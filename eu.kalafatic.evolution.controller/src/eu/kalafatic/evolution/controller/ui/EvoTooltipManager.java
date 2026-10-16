package eu.kalafatic.evolution.controller.ui;

import java.util.Map;

/**
 * Manager for generating dynamic, human-readable UI control tooltips.
 */
public class EvoTooltipManager {

    private static final EvoTooltipManager INSTANCE = new EvoTooltipManager();

    public static EvoTooltipManager getInstance() {
        return INSTANCE;
    }

    private EvoTooltipManager() {
    }

    public String formatTooltip(EvoUiActionDescriptor descriptor) {
        if (descriptor == null) return "";

        StringBuilder sb = new StringBuilder();
        String label = descriptor.getLabel();
        if (label != null && !label.isBlank()) {
            sb.append(label).append("\n\n");
        }

        if (descriptor.getOperationName() != null && !descriptor.getOperationName().isBlank()) {
            sb.append("Operation: ").append(descriptor.getOperationName());
            if (descriptor.getMethodName() != null && !descriptor.getMethodName().isBlank()) {
                sb.append(" (").append(descriptor.getMethodName()).append(")");
            }
            sb.append("\n");
        }

        if (descriptor.getTargetPath() != null && !descriptor.getTargetPath().isBlank()) {
            sb.append("Target: ").append(descriptor.getTargetPath()).append("\n");
        }

        if (descriptor.getComponentName() != null && !descriptor.getComponentName().isBlank()) {
            sb.append("Component: ").append(descriptor.getComponentName()).append("\n");
        }

        if (!descriptor.getParameters().isEmpty()) {
            sb.append("Parameters: ").append(formatParametersSummary(descriptor.getParameters())).append("\n");
        }

        if (descriptor.getOldValue() != null || descriptor.getNewValue() != null) {
            sb.append("Value: ").append(descriptor.getNewValue() != null ? descriptor.getNewValue() : "").append("\n");
        }

        String statusStr = formatStatus(descriptor);
        sb.append("Status: ").append(statusStr);

        if (descriptor.getResultSummary() != null && !descriptor.getResultSummary().isBlank()) {
            sb.append("\nResult: ").append(descriptor.getResultSummary());
        }

        return sb.toString();
    }

    private String formatStatus(EvoUiActionDescriptor descriptor) {
        if (descriptor.getStatus() == null) return "Not started";
        return switch (descriptor.getStatus()) {
            case NOT_STARTED -> "Not started";
            case ACTION_START, IN_PROGRESS -> "In progress...";
            case ACTION_SUCCESS -> "Completed successfully";
            case ACTION_FAILED -> "Failed";
            case ACTION_CANCELLED -> "Cancelled";
        };
    }

    private String formatParametersSummary(Map<String, String> params) {
        if (params.isEmpty()) return "None";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
            if (sb.length() > 80) {
                sb.append("...");
                break;
            }
        }
        return sb.toString();
    }
}
