package avilaos.persistence;

import avilaos.model.dto.SimulationConfig;
import java.io.File;
import java.io.IOException;

/**
 * Contrato para la serialización y deserialización de la configuración de la simulación.
 */
public interface ConfigSerializer {
    void serialize(SimulationConfig config, File file) throws IOException;
    String serializeToString(SimulationConfig config) throws IOException;
    SimulationConfig deserialize(File file) throws IOException;
    SimulationConfig deserializeFromString(String json) throws IOException;
}
