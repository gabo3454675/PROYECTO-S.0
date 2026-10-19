package avilaos.config;

import avilaos.kernel.Kernel;
import avilaos.model.pcb.PCB;
import avilaos.model.enumeration.ProcessType;
import avilaos.kernel.sync.Buffer;
import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;
import avilaos.util.datastructures.Queue;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Gestor de persistencia en formato JSON para el estado del simulador ÁvilaOS.
 * Diseñado sin dependencias de java.util.
 */
public class ConfigManager {

    /**
     * Guarda la configuración de computadores, procesos y buffers en formato JSON.
     */
    public static void saveConfigToJson(String filePath, Kernel kernel) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        // 1. Configuración general (1 computador por instancia de Kernel)
        json.append("  \"numComputers\": 1,\n");

        // 2. Buffers
        json.append("  \"buffers\": [\n");
        if (kernel.getSyncManager() != null && kernel.getSyncManager().getAllBuffers() != null) {
            List<Buffer> buffers = kernel.getSyncManager().getAllBuffers();
            for (int i = 0; i < buffers.size(); i++) {
                Buffer b = buffers.get(i);
                json.append("    {\n");
                json.append("      \"id\": \"").append(b.getId()).append("\",\n");
                json.append("      \"hostComputerId\": ").append(b.getHostComputerId()).append(",\n");
                json.append("      \"capacity\": ").append(b.getCapacity()).append("\n");
                json.append("    }").append(i < buffers.size() - 1 ? ",\n" : "\n");
            }
        }
        json.append("  ],\n");

        // 3. Recopilar procesos de las colas New, Ready y Terminated
        List<PCB> allProcesses = new LinkedList<>();
        collectProcessesFromQueue(kernel.getNewQueue(), allProcesses);
        collectProcessesFromQueue(kernel.getReadyQueue(), allProcesses);
        collectProcessesFromQueue(kernel.getTerminatedQueue(), allProcesses);

        json.append("  \"processes\": [\n");
        for (int i = 0; i < allProcesses.size(); i++) {
            PCB p = allProcesses.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(p.getId()).append(",\n");
            json.append("      \"name\": \"").append(p.getName()).append("\",\n");
            json.append("      \"computerId\": ").append(p.getComputerId()).append(",\n");
            json.append("      \"priority\": ").append(p.getPriority()).append(",\n");
            json.append("      \"deadline\": ").append(p.getDeadline()).append("\n");
            json.append("    }").append(i < allProcesses.size() - 1 ? ",\n" : "\n");
        }
        json.append("  ]\n");
        json.append("}\n");

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(json.toString());
        }
    }

    /**
     * Carga una configuración desde un archivo JSON e inicializa el Kernel.
     */
    public static void loadConfigFromJson(String filePath, Kernel kernel) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line.trim());
            }
        }

        String jsonStr = content.toString();
        if (jsonStr.isEmpty()) {
            throw new IllegalArgumentException("El archivo JSON está vacío.");
        }

        // Extraer y agregar Buffers
        String buffersSection = extractJsonArray(jsonStr, "buffers");
        if (!buffersSection.isEmpty()) {
            String[] bufferObjects = splitJsonObjects(buffersSection);
            for (String bufObj : bufferObjects) {
                if (bufObj.isEmpty()) continue;
                int id = parseJsonInt(bufObj, "id", 0);
                int hostComputerId = parseJsonInt(bufObj, "hostComputerId", 0);
                int capacity = parseJsonInt(bufObj, "capacity", 5);
                if (kernel.getSyncManager() != null) {
                    kernel.getSyncManager().createBuffer(String.valueOf(id), hostComputerId, capacity);
                }
            }
        }

        // Extraer y agregar Procesos al Kernel
        String processesSection = extractJsonArray(jsonStr, "processes");
        if (!processesSection.isEmpty()) {
            String[] processObjects = splitJsonObjects(processesSection);
            for (String procObj : processObjects) {
                if (procObj.isEmpty()) continue;
                String name = parseJsonString(procObj, "name", "Proc");
                int computerId = parseJsonInt(procObj, "computerId", 0);
                int priority = parseJsonInt(procObj, "priority", 1);
                long deadline = parseJsonLong(procObj, "deadline", 20);

                ProcessType defaultType = (ProcessType.values().length > 0) ? ProcessType.values()[0] : null;
                PCB process = new PCB(name, computerId, defaultType, priority, 100L, deadline, deadline);
                kernel.submitProcess(process);
            }
        }
    }

    // --- Helpers auxiliares ---

    private static void collectProcessesFromQueue(Queue<PCB> queue, List<PCB> targetList) {
        if (queue == null) return;
        int size = queue.size();
        for (int i = 0; i < size; i++) {
            PCB pcb = queue.dequeue();
            if (pcb != null) {
                targetList.add(pcb);
                queue.enqueue(pcb); // Restaurar proceso a la cola
            }
        }
    }

    private static int parseJsonInt(String json, String key, int defaultValue) {
        String searchKey = "\"" + key + "\":";
        int index = json.indexOf(searchKey);
        if (index == -1) return defaultValue;

        int start = index + searchKey.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }

        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static long parseJsonLong(String json, String key, long defaultValue) {
        String searchKey = "\"" + key + "\":";
        int index = json.indexOf(searchKey);
        if (index == -1) return defaultValue;

        int start = index + searchKey.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }

        try {
            return Long.parseLong(json.substring(start, end).trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String parseJsonString(String json, String key, String defaultValue) {
        String searchKey = "\"" + key + "\":\"";
        int index = json.indexOf(searchKey);
        if (index == -1) return defaultValue;

        int start = index + searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return defaultValue;

        return json.substring(start, end);
    }

    private static String extractJsonArray(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int index = json.indexOf(searchKey);
        if (index == -1) return "";

        int start = json.indexOf("[", index);
        int end = json.indexOf("]", start);
        if (start == -1 || end == -1) return "";

        return json.substring(start + 1, end).trim();
    }

    private static String[] splitJsonObjects(String arrayContent) {
        LinkedList<String> objects = new LinkedList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < arrayContent.length(); i++) {
            char c = arrayContent.charAt(i);
            if (c == '{') {
                depth++;
            }
            if (depth > 0) {
                current.append(c);
            }
            if (c == '}') {
                depth--;
                if (depth == 0) {
                    objects.add(current.toString());
                    current = new StringBuilder();
                }
            }
        }

        String[] result = new String[objects.size()];
        for (int i = 0; i < objects.size(); i++) {
            result[i] = objects.get(i);
        }
        return result;
    }
}