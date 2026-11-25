package avilaos.config;

import avilaos.core.disk.DiskBlockMap;
import avilaos.core.filesystem.FileSystemManager;
import avilaos.kernel.disk.DiskScheduler;
import avilaos.model.enumeration.DiskPolicyType;
import avilaos.model.enumeration.HeadDirection;
import avilaos.model.enumeration.IOOperationType;
import avilaos.model.enumeration.SchedulingPolicyType;
import avilaos.model.pcb.PCB;
import avilaos.simulation.SimulationCluster;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;

/** Carga conjunta de los dos archivos requeridos por el Proyecto 2. */
public final class Project2ConfigLoader {
    private static final ObjectMapper JSON = new ObjectMapper();

    private Project2ConfigLoader() { }

    public static SimulationCluster load(Path diskFile, Path processFile) throws IOException {
        if (diskFile == null || processFile == null) {
            throw new IllegalArgumentException("Se requieren ambos archivos JSON");
        }
        JsonNode diskRoot = JSON.readTree(diskFile.toFile());
        JsonNode processRoot = JSON.readTree(processFile.toFile());
        JsonNode diskConfig = required(diskRoot, "diskConfig");
        JsonNode fileSystem = required(diskRoot, "fileSystem");
        JsonNode global = required(processRoot, "globalConfig");
        JsonNode computerConfig = required(processRoot, "computer");
        JsonNode processes = required(processRoot, "processes");
        if (!fileSystem.isArray() || !processes.isArray()) {
            throw new IllegalArgumentException("fileSystem y processes deben ser arreglos");
        }

        int totalBlocks = integer(diskConfig, "totalBlocks", 100, 500);
        int initialHead = integer(diskConfig, "initialHead", 0, totalBlocks - 1);
        int speed = diskConfig.has("positionsPerCycle")
                ? integer(diskConfig, "positionsPerCycle", 1, totalBlocks) : 1;
        int swapBlocks = integer(global, "swapSizeBlocks", 0, totalBlocks);
        int cycleMs = integer(global, "cycleDurationMs", 1, Integer.MAX_VALUE);
        int ram = integer(computerConfig, "ramCapacity", 1, Integer.MAX_VALUE);
        SchedulingPolicyType cpuPolicy = SchedulingPolicyType.valueOf(
                string(computerConfig, "policy").replace('-', '_').toUpperCase());
        int quantum = integer(computerConfig, "quantum", 0, Integer.MAX_VALUE);
        if (cpuPolicy == SchedulingPolicyType.ROUND_ROBIN && quantum == 0) {
            throw new IllegalArgumentException("Round Robin requiere quantum positivo");
        }
        DiskPolicyType diskPolicy = DiskPolicyType.fromString(string(global, "initialDiskPolicy"));

        FileSystemManager files = new FileSystemManager(new DiskBlockMap(totalBlocks));
        loadFiles(fileSystem, files);
        SimulationCluster simulation = new SimulationCluster();
        try {
            simulation.setCycleDurationMs(cycleMs);
            simulation.setConfiguredSwapSizeBlocks(swapBlocks);
            var computer = simulation.addComputer(0, ram, cpuPolicy, Math.max(1, quantum));
            computer.attachDisk(new DiskScheduler(diskPolicy), files, initialHead, HeadDirection.UP, speed);
            for (JsonNode item : processes) {
                simulation.submitProcess(process(item));
            }
            return simulation;
        } catch (RuntimeException ex) {
            simulation.shutdown();
            throw ex;
        }
    }

    private static void loadFiles(JsonNode entries, FileSystemManager files) {
        // Crea directorios por profundidad para aceptar cualquier orden en el JSON.
        int remaining = 0;
        for (JsonNode item : entries) if ("DIRECTORY".equals(string(item, "type"))) remaining++;
        while (remaining > 0) {
            int created = 0;
            for (JsonNode item : entries) {
                if (!"DIRECTORY".equals(string(item, "type"))) continue;
                String path = string(item, "path");
                if (files.resolve(path) != null) continue;
                String parent = parent(path);
                if (files.resolve(parent) == null) continue;
                files.createDirectory(parent, name(path), string(item, "owner"), isPublic(item));
                created++;
            }
            if (created == 0) throw new IllegalArgumentException("Directorio padre ausente o ruta duplicada");
            remaining -= created;
        }
        for (JsonNode item : entries) {
            String type = string(item, "type");
            if ("DIRECTORY".equals(type)) continue;
            if (!"FILE".equals(type)) throw new IllegalArgumentException("Tipo de nodo desconocido: " + type);
            String path = string(item, "path");
            int start = integer(item, "startBlock", 0, files.getBlockMap().getTotalBlocks() - 1);
            var file = files.createFile(parent(path), name(path),
                    integer(item, "blocks", 1, files.getBlockMap().getTotalBlocks()),
                    string(item, "owner"), isPublic(item), "SYSTEM",
                    start);
            if (file.getFirstBlock() != start) {
                throw new IllegalArgumentException("Bloque inicial ocupado para " + path + ": " + start);
            }
        }
    }

    private static PCB process(JsonNode item) {
        String name = string(item, "name");
        long memory = integer(item, "memory", 1, Integer.MAX_VALUE);
        long deadline = integer(item, "deadline", 0, Integer.MAX_VALUE);
        int instructions = integer(item, "instructions", 1, Integer.MAX_VALUE);
        String path = string(item, "targetPath");
        IOOperationType operation = IOOperationType.valueOf(string(item, "operation").toUpperCase());
        return switch (operation) {
            case CREATE -> PCB.forFileCreate(name, 0, 0, memory, deadline, instructions, path,
                    integer(item, "blocks", 1, 500), optional(item, "owner", "admin"),
                    !"private".equalsIgnoreCase(optional(item, "permissions", "public")));
            case READ -> PCB.forFileRead(name, 0, 0, memory, deadline, instructions, path);
            case UPDATE -> PCB.forFileUpdate(name, 0, 0, memory, deadline, instructions, path,
                    optional(item, "newName", name(path)));
            case DELETE -> PCB.forFileDelete(name, 0, 0, memory, deadline, instructions, path);
            default -> throw new IllegalArgumentException("SUSPEND/RESUME se generan por el planificador");
        };
    }

    private static JsonNode required(JsonNode node, String key) {
        JsonNode value = node == null ? null : node.get(key);
        if (value == null || value.isNull()) throw new IllegalArgumentException("Falta " + key);
        return value;
    }

    private static String string(JsonNode node, String key) {
        JsonNode value = required(node, key);
        if (!value.isTextual() || value.asText().isBlank()) {
            throw new IllegalArgumentException(key + " debe ser texto no vacío");
        }
        return value.asText();
    }

    private static String optional(JsonNode node, String key, String fallback) {
        return node.hasNonNull(key) ? string(node, key) : fallback;
    }

    private static int integer(JsonNode node, String key, int min, int max) {
        JsonNode value = required(node, key);
        if (!value.isIntegralNumber() || !value.canConvertToInt()
                || value.asInt() < min || value.asInt() > max) {
            throw new IllegalArgumentException(key + " debe estar entre " + min + " y " + max);
        }
        return value.asInt();
    }

    private static boolean isPublic(JsonNode node) {
        String permission = string(node, "permissions");
        if (!permission.equalsIgnoreCase("public") && !permission.equalsIgnoreCase("private")) {
            throw new IllegalArgumentException("Permiso desconocido: " + permission);
        }
        return permission.equalsIgnoreCase("public");
    }

    private static String parent(String path) {
        int slash = path.lastIndexOf('/');
        if (slash < 0 || slash == path.length() - 1) throw new IllegalArgumentException("Ruta inválida: " + path);
        return slash == 0 ? "/" : path.substring(0, slash);
    }

    private static String name(String path) {
        parent(path);
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
