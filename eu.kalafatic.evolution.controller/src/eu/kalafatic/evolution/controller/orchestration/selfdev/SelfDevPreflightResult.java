package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result object produced by SelfDevPreflight.
 */
public class SelfDevPreflightResult {

    public enum PreflightStatus {
        SUCCESS,
        RECOVERED,
        BLOCKED,
        FAILED
    }

    public static class CheckDetail {
        private final String checkId;
        private final String status;
        private final String message;
        private final String path;
        private final String expected;
        private final String actual;
        private final String recoveryAction;

        public CheckDetail(String checkId, String status, String message, String path,
                           String expected, String actual, String recoveryAction) {
            this.checkId = checkId;
            this.status = status;
            this.message = message;
            this.path = path;
            this.expected = expected;
            this.actual = actual;
            this.recoveryAction = recoveryAction;
        }

        public String getCheckId() { return checkId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public String getPath() { return path; }
        public String getExpected() { return expected; }
        public String getActual() { return actual; }
        public String getRecoveryAction() { return recoveryAction; }
    }

    private final PreflightStatus status;
    private final ResolvedSelfDevResources resolvedResources;
    private final List<CheckDetail> checks;
    private final List<String> conflicts;
    private final List<String> recoveries;
    private final List<String> errors;
    private final List<String> warnings;

    public SelfDevPreflightResult(PreflightStatus status, ResolvedSelfDevResources resolvedResources,
                                  List<CheckDetail> checks, List<String> conflicts,
                                  List<String> recoveries, List<String> errors, List<String> warnings) {
        this.status = status;
        this.resolvedResources = resolvedResources;
        this.checks = new ArrayList<>(checks);
        this.conflicts = new ArrayList<>(conflicts);
        this.recoveries = new ArrayList<>(recoveries);
        this.errors = new ArrayList<>(errors);
        this.warnings = new ArrayList<>(warnings);
    }

    public boolean isSuccess() {
        return status == PreflightStatus.SUCCESS || status == PreflightStatus.RECOVERED;
    }

    public PreflightStatus getStatus() { return status; }
    public ResolvedSelfDevResources getResolvedResources() { return resolvedResources; }
    public List<CheckDetail> getChecks() { return Collections.unmodifiableList(checks); }
    public List<String> getConflicts() { return Collections.unmodifiableList(conflicts); }
    public List<String> getRecoveries() { return Collections.unmodifiableList(recoveries); }
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }
    public List<String> getWarnings() { return Collections.unmodifiableList(warnings); }

    public String generateSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append("SELF-DEV PREFLIGHT\n");
        sb.append("============================================================\n\n");
        sb.append("RESULT: ").append(status).append("\n\n");

        if (resolvedResources != null) {
            sb.append("SOURCE:\n");
            sb.append("  repository : ").append(resolvedResources.getRepositoryRoot()).append("\n");
            sb.append("  project    : ").append(resolvedResources.getProjectRoot()).append("\n");
            sb.append("  sourceDir  : ").append(resolvedResources.getSourceDirectory()).append("\n");
            sb.append("  reactorDir : ").append(resolvedResources.getReactorDirectory()).append("\n\n");

            sb.append("BUILD:\n");
            sb.append("  directory  : ").append(resolvedResources.getBuildDirectory()).append("\n\n");

            sb.append("EXPORT:\n");
            sb.append("  directory  : ").append(resolvedResources.getExportDirectory()).append("\n\n");

            sb.append("JAVA:\n");
            sb.append("  executable : ").append(resolvedResources.getJavaExecutable()).append("\n\n");

            sb.append("MAVEN:\n");
            sb.append("  executable : ").append(resolvedResources.getMavenExecutable()).append("\n\n");
        }

        if (!recoveries.isEmpty()) {
            sb.append("RECOVERIES:\n");
            for (String r : recoveries) {
                sb.append("  - ").append(r).append("\n");
            }
            sb.append("\n");
        }

        if (!conflicts.isEmpty()) {
            sb.append("PATH CONFLICTS:\n");
            for (String c : conflicts) {
                sb.append("  - ").append(c).append("\n");
            }
            sb.append("\n");
        }

        if (!errors.isEmpty()) {
            sb.append("ERRORS:\n");
            for (String e : errors) {
                sb.append("  - ").append(e).append("\n");
            }
            sb.append("\n");
        }

        if (!warnings.isEmpty()) {
            sb.append("WARNINGS:\n");
            for (String w : warnings) {
                sb.append("  - ").append(w).append("\n");
            }
            sb.append("\n");
        }

        sb.append("============================================================\n");
        return sb.toString();
    }
}
