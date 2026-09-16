package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class MavenErrorClassifier {

    public static class ClassificationResult {
        private final MavenErrorCategory category;
        private final String failedPhase;
        private final String rootCause;
        private final String lastRelevantOutput;

        public ClassificationResult(MavenErrorCategory category, String failedPhase, String rootCause, String lastRelevantOutput) {
            this.category = category;
            this.failedPhase = failedPhase != null ? failedPhase : "UNKNOWN";
            this.rootCause = rootCause != null ? rootCause : "Unknown root cause";
            this.lastRelevantOutput = lastRelevantOutput != null ? lastRelevantOutput : "";
        }

        public MavenErrorCategory getCategory() {
            return category;
        }

        public String getFailedPhase() {
            return failedPhase;
        }

        public String getRootCause() {
            return rootCause;
        }

        public String getLastRelevantOutput() {
            return lastRelevantOutput;
        }
    }

    public static ClassificationResult classify(String fullOutput) {
        if (fullOutput == null || fullOutput.trim().isEmpty()) {
            return new ClassificationResult(MavenErrorCategory.UNKNOWN, "UNKNOWN", "Empty build output", "");
        }

        String[] lines = fullOutput.split("\r?\n");
        String failedPhase = "UNKNOWN";
        String rootCause = "Build failed";
        MavenErrorCategory category = MavenErrorCategory.UNKNOWN;

        for (String line : lines) {
            if (line.contains("Failed to execute goal") || line.contains("--- ")) {
                if (line.contains("--- ")) {
                    int start = line.indexOf("--- ");
                    int end = line.indexOf(" ---", start);
                    if (end > start) {
                        failedPhase = line.substring(start + 4, end).trim();
                    }
                } else if (line.contains("Failed to execute goal")) {
                    failedPhase = line.trim();
                }
            }

            String lower = line.toLowerCase();
            if (lower.contains("java.lang.compilationerror") || lower.contains("compilation failure") || lower.contains("compilationerror")) {
                category = MavenErrorCategory.COMPILATION;
                rootCause = line.trim();
            } else if (lower.contains("cannot resolve dependencies") || lower.contains("could not resolve dependencies") || lower.contains("missing requirement")) {
                category = MavenErrorCategory.DEPENDENCY;
                rootCause = line.trim();
            } else if (lower.contains("target-platform") || lower.contains("target platform")) {
                category = MavenErrorCategory.TARGET_PLATFORM;
                rootCause = line.trim();
            } else if (lower.contains("tycho") || lower.contains("org.eclipse.tycho")) {
                if (category == MavenErrorCategory.UNKNOWN) {
                    category = MavenErrorCategory.TYCHO;
                    rootCause = line.trim();
                }
            } else if (lower.contains("sockettimeoutexception") || lower.contains("connecttimeout") || lower.contains("connection refused") || lower.contains("429 too many requests") || lower.contains("failed to transfer")) {
                category = MavenErrorCategory.NETWORK;
                rootCause = line.trim();
            } else if (lower.contains("no space left on device") || lower.contains("access is denied") || lower.contains("permission denied")) {
                category = MavenErrorCategory.FILESYSTEM;
                rootCause = line.trim();
            } else if (lower.contains("java_home") || lower.contains("unsupported major.minor version") || lower.contains("invalid flag:")) {
                category = MavenErrorCategory.JAVA;
                rootCause = line.trim();
            } else if (lower.contains("malformed pom") || lower.contains("invalid pom") || lower.contains("project build error")) {
                category = MavenErrorCategory.POM;
                rootCause = line.trim();
            } else if (lower.contains("packaging") || lower.contains("archive") || lower.contains("zip") || lower.contains("jar")) {
                if (category == MavenErrorCategory.UNKNOWN) {
                    category = MavenErrorCategory.PACKAGING;
                    rootCause = line.trim();
                }
            }
        }

        if (category == MavenErrorCategory.UNKNOWN && fullOutput.contains("[ERROR]")) {
            category = MavenErrorCategory.MAVEN;
        }

        String tail = getTail(fullOutput, 2000);
        return new ClassificationResult(category, failedPhase, rootCause, tail);
    }

    private static String getTail(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(text.length() - maxLength);
    }
}
