package avilaos.persistence;

import avilaos.util.datastructures.LinkedList;
import avilaos.util.datastructures.List;

/**
 * Resultado estructurado de una validación de configuración.
 * Contiene listas de errores y advertencias utilizando estructuras de datos propias.
 */
public class ValidationResult {

    private final List<String> errors;
    private final List<String> warnings;

    public ValidationResult() {
        this.errors = new LinkedList<>();
        this.warnings = new LinkedList<>();
    }

    public void addError(String error) {
        if (error != null && !error.isBlank()) {
            errors.add(error);
        }
    }

    public void addWarning(String warning) {
        if (warning != null && !warning.isBlank()) {
            warnings.add(warning);
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public String getFormattedErrors() {
        if (isValid()) {
            return "Sin errores.";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < errors.size(); i++) {
            sb.append("• ").append(errors.get(i)).append("\n");
        }
        return sb.toString().trim();
    }

    public String getFormattedWarnings() {
        if (!hasWarnings()) {
            return "Sin advertencias.";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < warnings.size(); i++) {
            sb.append("• ").append(warnings.get(i)).append("\n");
        }
        return sb.toString().trim();
    }
}
