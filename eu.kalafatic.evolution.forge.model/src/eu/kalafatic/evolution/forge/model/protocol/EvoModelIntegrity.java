package eu.kalafatic.evolution.forge.model.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Diagnostic integrity validation result container for EVO Native Model Protocol files.
 */
public class EvoModelIntegrity {

    public enum Status {
        PASS,
        FAIL
    }

    public static class ValidationError {
        private final String section;
        private final String target;
        private final String message;

        public ValidationError(String section, String target, String message) {
            this.section = section;
            this.target = target;
            this.message = message;
        }

        public String getSection() { return section; }
        public String getTarget() { return target; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return "[" + section + "] Target: " + target + " - " + message;
        }
    }

    private Status status = Status.PASS;
    private final List<ValidationError> errors = new ArrayList<>();
    private final List<String> checksPassed = new ArrayList<>();

    public void addPass(String checkName) {
        checksPassed.add(checkName);
    }

    public void addError(String section, String target, String message) {
        this.status = Status.FAIL;
        errors.add(new ValidationError(section, target, message));
    }

    public boolean isValid() {
        return status == Status.PASS;
    }

    public Status getStatus() { return status; }
    public List<ValidationError> getErrors() { return Collections.unmodifiableList(errors); }
    public List<String> getChecksPassed() { return Collections.unmodifiableList(checksPassed); }

    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("EVO MODEL INTEGRITY REPORT\n");
        sb.append("===========================\n");
        sb.append("Overall Status: ").append(status).append("\n\n");

        sb.append("Passed Checks (").append(checksPassed.size()).append("):\n");
        for (String pass : checksPassed) {
            sb.append("  [PASS] ").append(pass).append("\n");
        }

        if (!errors.isEmpty()) {
            sb.append("\nFailed Checks (").append(errors.size()).append("):\n");
            for (ValidationError err : errors) {
                sb.append("  [FAIL] ").append(err).append("\n");
            }
        }
        return sb.toString();
    }
}
