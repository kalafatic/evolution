package eu.kalafatic.evolution.forge.agent.gguf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GGUFValidationReport {
    private boolean valid = true;
    private boolean structureValid = true;
    private boolean metadataValid = true;
    private boolean tensorsValid = true;
    private boolean semanticsValid = true;

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> diagnostics = new ArrayList<>();

    public void addError(String section, String field, String expected, String actual, long fileOffset, String message) {
        valid = false;
        if ("header".equals(section) || "alignment".equals(section) || "tensor_offset".equals(section) || "tensor_payload".equals(section) || "tensor_overlap".equals(section)) {
            structureValid = false;
        } else if ("metadata".equals(section) || "tokenizer".equals(section)) {
            metadataValid = false;
        } else if ("tensor_info".equals(section) || "tensor_names".equals(section) || "tensor_dims".equals(section)) {
            tensorsValid = false;
        } else if ("semantics".equals(section)) {
            semanticsValid = false;
        }

        String errStr;
        if (fileOffset >= 0) {
            errStr = String.format("GGUF validation error [section: %s, field: %s, fileOffset: 0x%X (%d)]: expected='%s', actual='%s' -> %s",
                section, field, fileOffset, fileOffset, expected, actual, message);
        } else {
            errStr = String.format("GGUF validation error [section: %s, field: %s]: expected='%s', actual='%s' -> %s",
                section, field, expected, actual, message);
        }
        errors.add(errStr);
    }

    public void addWarning(String message) {
        warnings.add(message);
    }

    public void addDiagnostic(String message) {
        diagnostics.add(message);
    }

    public boolean isValid() { return valid; }
    public boolean isStructureValid() { return structureValid; }
    public boolean isMetadataValid() { return metadataValid; }
    public boolean isTensorsValid() { return tensorsValid; }
    public boolean isSemanticsValid() { return semanticsValid; }

    public List<String> getErrors() { return Collections.unmodifiableList(errors); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }
    public List<String> getDiagnostics() { return Collections.unmodifiableList(diagnostics); }

    public String generateSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== GGUF VALIDATION REPORT ===\n");
        sb.append("Overall Status   : ").append(valid ? "PASS ✅" : "FAIL ❌").append("\n");
        sb.append("Structure        : ").append(structureValid ? "PASS ✅" : "FAIL ❌").append("\n");
        sb.append("Metadata         : ").append(metadataValid ? "PASS ✅" : "FAIL ❌").append("\n");
        sb.append("Tensors          : ").append(tensorsValid ? "PASS ✅" : "FAIL ❌").append("\n");
        sb.append("Semantics        : ").append(semanticsValid ? "PASS ✅" : "FAIL ❌").append("\n");

        if (!errors.isEmpty()) {
            sb.append("\nErrors (").append(errors.size()).append("):\n");
            for (String err : errors) {
                sb.append("  - ").append(err).append("\n");
            }
        }
        if (!warnings.isEmpty()) {
            sb.append("\nWarnings (").append(warnings.size()).append("):\n");
            for (String w : warnings) {
                sb.append("  - ").append(w).append("\n");
            }
        }
        return sb.toString();
    }
}
