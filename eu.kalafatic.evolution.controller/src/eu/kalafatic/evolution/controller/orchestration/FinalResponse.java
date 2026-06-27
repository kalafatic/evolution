package eu.kalafatic.evolution.controller.orchestration;

import java.util.List;

/**
 * Immutable DTO representing the final user-oriented response.
 */
public class FinalResponse {
    private final String summary;
    private final List<String> proposals;
    private final List<FileReference> files;
    private final boolean success;
    private final String selectedVariantId;
    private final String executionSummary;
    private final ExecutionMetrics metrics;

    public FinalResponse(String summary, List<String> proposals, List<FileReference> files,
                         boolean success, String selectedVariantId, String executionSummary,
                         ExecutionMetrics metrics) {
        this.summary = summary;
        this.proposals = proposals;
        this.files = files;
        this.success = success;
        this.selectedVariantId = selectedVariantId;
        this.executionSummary = executionSummary;
        this.metrics = metrics;
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getProposals() {
        return proposals;
    }

    public List<FileReference> getFiles() {
        return files;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getSelectedVariantId() {
        return selectedVariantId;
    }

    public String getExecutionSummary() {
        return executionSummary;
    }

    public ExecutionMetrics getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("### TASK RESULT\n\n");

        if (executionSummary != null && !executionSummary.isEmpty()) {
            sb.append(executionSummary).append("\n\n");
        }

        if (summary != null && !summary.isEmpty() && !summary.equals("{}")) {
            sb.append("**Summary**\n").append(summary).append("\n\n");
        }

        if (proposals != null && !proposals.isEmpty()) {
            sb.append("**Suggestions**\n");
            for (String p : proposals) {
                sb.append("- ").append(p).append("\n");
            }
            sb.append("\n");
        }

        if (files != null && !files.isEmpty()) {
            sb.append("#### 📂 Modified Files\n\n");
            for (FileReference f : files) {
                sb.append("- [").append(f.getPath()).append("](").append(f.getEclipseUri()).append(")\n");
            }
            sb.append("\n");
        }

        if (metrics != null) {
            sb.append("**Execution time**: ").append(metrics.formatDuration()).append("\n");
        }

        return sb.toString();
    }
}
