package avilaos.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import avilaos.model.dto.SimulationConfig;

import java.io.File;
import java.io.IOException;

/**
 * Implementación robusta de persistencia en formato JSON utilizando Jackson.
 * Soporta records inmutables, enums y formateo con sangría (pretty-print).
 */
public class JsonConfigSerializer implements ConfigSerializer {

    private final ObjectMapper mapper;

    public JsonConfigSerializer() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    @Override
    public void serialize(SimulationConfig config, File file) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("La configuración no puede ser null.");
        }
        if (file == null) {
            throw new IllegalArgumentException("El archivo de destino no puede ser null.");
        }
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        mapper.writeValue(file, config);
    }

    @Override
    public String serializeToString(SimulationConfig config) throws IOException {
        if (config == null) {
            throw new IllegalArgumentException("La configuración no puede ser null.");
        }
        return mapper.writeValueAsString(config);
    }

    @Override
    public SimulationConfig deserialize(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("El archivo fuente no puede ser null.");
        }
        if (!file.exists() || !file.isFile()) {
            throw new IOException("El archivo no existe o no es un archivo válido: " + file.getAbsolutePath());
        }
        return mapper.readValue(file, SimulationConfig.class);
    }

    @Override
    public SimulationConfig deserializeFromString(String json) throws IOException {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("El contenido JSON no puede estar vacío.");
        }
        return mapper.readValue(json, SimulationConfig.class);
    }
}
