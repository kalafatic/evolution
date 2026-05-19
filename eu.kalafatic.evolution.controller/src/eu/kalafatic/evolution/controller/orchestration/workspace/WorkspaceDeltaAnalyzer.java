package eu.kalafatic.evolution.controller.orchestration.workspace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.tools.GitTool;

/**
 * Analyzer for workspace deltas to perform semantic reality validation.
 * Goes beyond simple diff length to understand the nature of changes.
 */
public class WorkspaceDeltaAnalyzer {
    private final File projectRoot;
    private final TaskContext context;
    private final GitTool gitTool = new GitTool();

    public WorkspaceDeltaAnalyzer(File projectRoot, TaskContext context) {
        this.projectRoot = projectRoot;
        this.context = context;
    }

    /**
     * Performs a comprehensive reality check on the current workspace state compared to a base commit.
     */
    public DeltaAnalysis analyze(String baseCommit) {
        DeltaAnalysis analysis = new DeltaAnalysis();
        try {
            String diff = gitTool.execute("diff " + baseCommit + " HEAD", projectRoot, context);
            analysis.setRawDiff(diff);

            String status = gitTool.execute("status --porcelain", projectRoot, context);
            analysis.setChangedFiles(parseChangedFiles(status));

            performSemanticAnalysis(diff, analysis);

        } catch (Exception e) {
            context.log("[DELTA-ANALYZER] Analysis failed: " + e.getMessage());
        }
        return analysis;
    }

    private List<String> parseChangedFiles(String status) {
        List<String> files = new ArrayList<>();
        if (status == null || status.isEmpty()) return files;

        String[] lines = status.split("\n");
        for (String line : lines) {
            if (line.length() > 3) {
                files.add(line.substring(3).trim());
            }
        }
        return files;
    }

    private void performSemanticAnalysis(String diff, DeltaAnalysis analysis) {
        // P1 implementation: basic semantic signals
        if (diff == null || diff.isEmpty()) return;

        int addedLines = 0;
        int removedLines = 0;
        for (String line : diff.split("\n")) {
            if (line.startsWith("+") && !line.startsWith("+++")) addedLines++;
            if (line.startsWith("-") && !line.startsWith("---")) removedLines++;
        }

        analysis.setAddedLines(addedLines);
        analysis.setRemovedLines(removedLines);

        // Detect impact
        if (diff.contains("import ")) analysis.addSignal("DEPENDENCY_CHANGE");
        if (diff.contains("@Test")) analysis.addSignal("TEST_IMPACT");
        if (diff.contains("public interface")) analysis.addSignal("CONTRACT_CHANGE");
    }

    public static class DeltaAnalysis {
        private String rawDiff;
        private List<String> changedFiles = new ArrayList<>();
        private int addedLines;
        private int removedLines;
        private List<String> signals = new ArrayList<>();

        public String getRawDiff() { return rawDiff; }
        public void setRawDiff(String rawDiff) { this.rawDiff = rawDiff; }

        public List<String> getChangedFiles() { return changedFiles; }
        public void setChangedFiles(List<String> changedFiles) { this.changedFiles = changedFiles; }

        public int getAddedLines() { return addedLines; }
        public void setAddedLines(int addedLines) { this.addedLines = addedLines; }

        public int getRemovedLines() { return removedLines; }
        public void setRemovedLines(int removedLines) { this.removedLines = removedLines; }

        public void addSignal(String signal) { signals.add(signal); }
        public List<String> getSignals() { return signals; }

        public boolean isSignificant() {
            return !changedFiles.isEmpty() && (addedLines + removedLines > 0);
        }

        @Override
        public String toString() {
            return String.format("DeltaAnalysis[files=%d, added=%d, removed=%d, signals=%s]",
                changedFiles.size(), addedLines, removedLines, signals);
        }
    }
}
