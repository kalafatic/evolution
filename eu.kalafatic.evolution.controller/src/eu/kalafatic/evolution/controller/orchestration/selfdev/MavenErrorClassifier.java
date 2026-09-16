package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MavenErrorClassifier {

    public static class ClassificationResult {
        private final MavenErrorCategory category;
        private final String failingModule;
        private final String failingPlugin;
        private final String failingGoal;
        private final String failedPhase;
        private final String rootCause;
        private final String reportDirectory;
        private final String testSummary;
        private final String lastRelevantOutput;

        public ClassificationResult(MavenErrorCategory category,
                                    String failingModule,
                                    String failingPlugin,
                                    String failingGoal,
                                    String failedPhase,
                                    String rootCause,
                                    String reportDirectory,
                                    String testSummary,
                                    String lastRelevantOutput) {
            this.category = category != null ? category : MavenErrorCategory.UNKNOWN;
            this.failingModule = failingModule != null ? failingModule : "UNKNOWN";
            this.failingPlugin = failingPlugin != null ? failingPlugin : "UNKNOWN";
            this.failingGoal = failingGoal != null ? failingGoal : "UNKNOWN";
            this.failedPhase = failedPhase != null ? failedPhase : "UNKNOWN";
            this.rootCause = rootCause != null ? rootCause : "Unknown root cause";
            this.reportDirectory = reportDirectory;
            this.testSummary = testSummary;
            this.lastRelevantOutput = lastRelevantOutput != null ? lastRelevantOutput : "";
        }

        public MavenErrorCategory getCategory() {
            return category;
        }

        public String getFailingModule() {
            return failingModule;
        }

        public String getFailingPlugin() {
            return failingPlugin;
        }

        public String getFailingGoal() {
            return failingGoal;
        }

        public String getFailedPhase() {
            return failedPhase;
        }

        public String getRootCause() {
            return rootCause;
        }

        public String getReportDirectory() {
            return reportDirectory;
        }

        public String getTestSummary() {
            return testSummary;
        }

        public String getLastRelevantOutput() {
            return lastRelevantOutput;
        }
    }

    private static final Pattern GOAL_FAIL_PATTERN = Pattern.compile(
            "(?:Failed to execute goal|\\[ERROR\\] Failed to execute goal)\\s+([^:\\s]+(?::[^:\\s]+)*):([^:\\s\\()]+)\\s*(?:\\([^)]*\\))?\\s+on project\\s+([^:]+):\\s*(.*)"
    );

    private static final Pattern REPORT_DIR_PATTERN = Pattern.compile(
            "(?:Please refer to|\\[ERROR\\] Please refer to)\\s+(.*?)\\s+for the individual test results"
    );

    public static ClassificationResult classify(String fullOutput) {
        return classify(fullOutput, null);
    }

    public static ClassificationResult classify(String fullOutput, File workingDir) {
        if (fullOutput == null || fullOutput.trim().isEmpty()) {
            return new ClassificationResult(
                    MavenErrorCategory.UNKNOWN,
                    "UNKNOWN", "UNKNOWN", "UNKNOWN", "UNKNOWN",
                    "Empty build output", null, null, ""
            );
        }

        String failingModule = "UNKNOWN";
        String failingPlugin = "UNKNOWN";
        String failingGoal = "UNKNOWN";
        String failedPhase = "UNKNOWN";
        String rootCause = "Build failed";
        String reportDirectory = null;
        MavenErrorCategory category = null;

        // 1. Extract explicit surefire report directory from log if present
        Matcher reportMatcher = REPORT_DIR_PATTERN.matcher(fullOutput);
        if (reportMatcher.find()) {
            reportDirectory = reportMatcher.group(1).trim();
        }

        // 2. Look for primary Maven error line: [ERROR] Failed to execute goal ...
        Matcher goalMatcher = GOAL_FAIL_PATTERN.matcher(fullOutput);
        if (goalMatcher.find()) {
            failingPlugin = goalMatcher.group(1).trim();
            failingGoal = goalMatcher.group(2).trim();
            failingModule = goalMatcher.group(3).trim();
            rootCause = goalMatcher.group(4).trim();
            failedPhase = failingPlugin + ":" + failingGoal;

            String lowerCause = rootCause.toLowerCase();
            String lowerGoal = failingGoal.toLowerCase();
            String lowerPlugin = failingPlugin.toLowerCase();

            if (lowerGoal.equals("test") || lowerCause.contains("test failure") || lowerCause.contains("test failures")) {
                category = MavenErrorCategory.TEST_FAILURE;
            } else if (lowerGoal.equals("compile") || lowerCause.contains("compilation failure") || lowerCause.contains("compilation error")) {
                category = MavenErrorCategory.COMPILATION;
            } else if (lowerCause.contains("cannot resolve") || lowerCause.contains("missing requirement") || lowerCause.contains("could not resolve dependencies")) {
                category = MavenErrorCategory.DEPENDENCY;
            } else if (lowerPlugin.contains("target-platform") || lowerCause.contains("target-platform") || lowerCause.contains("target platform")) {
                category = MavenErrorCategory.TARGET_PLATFORM;
            } else if (lowerPlugin.contains("tycho") || lowerCause.contains("tycho")) {
                category = MavenErrorCategory.TYCHO;
            } else if (lowerCause.contains("sockettimeout") || lowerCause.contains("connecttimeout") || lowerCause.contains("connection refused") || lowerCause.contains("429 too many requests")) {
                category = MavenErrorCategory.NETWORK;
            } else if (lowerCause.contains("no space left") || lowerCause.contains("permission denied") || lowerCause.contains("access is denied")) {
                category = MavenErrorCategory.FILESYSTEM;
            } else if (lowerCause.contains("java_home") || lowerCause.contains("unsupported major.minor") || lowerCause.contains("invalid flag")) {
                category = MavenErrorCategory.JAVA;
            } else if (lowerCause.contains("malformed pom") || lowerCause.contains("invalid pom") || lowerCause.contains("project build error")) {
                category = MavenErrorCategory.POM;
            } else if (lowerCause.contains("packaging") || lowerCause.contains("archive") || lowerCause.contains("zip") || lowerCause.contains("jar")) {
                category = MavenErrorCategory.PACKAGING;
            } else {
                category = MavenErrorCategory.MAVEN;
            }
        }

        // 3. Fallback scan if explicit goal failure line was not found
        if (category == null) {
            String[] lines = fullOutput.split("\r?\n");
            for (String line : lines) {
                String lower = line.toLowerCase();
                if (lower.contains("there are test failures") || lower.contains("there were test failures")) {
                    category = MavenErrorCategory.TEST_FAILURE;
                    rootCause = line.trim();
                } else if (lower.contains("java.lang.compilationerror") || lower.contains("compilation failure")) {
                    if (category == null) {
                        category = MavenErrorCategory.COMPILATION;
                        rootCause = line.trim();
                    }
                } else if (lower.contains("cannot resolve dependencies") || lower.contains("could not resolve dependencies") || lower.contains("missing requirement")) {
                    if (category == null) {
                        category = MavenErrorCategory.DEPENDENCY;
                        rootCause = line.trim();
                    }
                } else if (lower.contains("sockettimeoutexception") || lower.contains("connecttimeout") || lower.contains("connection refused") || lower.contains("429 too many requests")) {
                    if (category == null) {
                        category = MavenErrorCategory.NETWORK;
                        rootCause = line.trim();
                    }
                } else if (lower.contains("no space left on device") || lower.contains("access is denied") || lower.contains("permission denied")) {
                    if (category == null) {
                        category = MavenErrorCategory.FILESYSTEM;
                        rootCause = line.trim();
                    }
                }
            }
        }

        if (category == null && fullOutput.contains("[ERROR]")) {
            category = MavenErrorCategory.MAVEN;
        } else if (category == null) {
            category = MavenErrorCategory.UNKNOWN;
        }

        // 4. Resolve surefire report directory if not explicitly found in output
        if (reportDirectory == null && workingDir != null) {
            if (!"UNKNOWN".equals(failingModule)) {
                File candidateModuleDir = new File(workingDir, failingModule + "/target/surefire-reports");
                if (candidateModuleDir.exists() && candidateModuleDir.isDirectory()) {
                    reportDirectory = candidateModuleDir.getAbsolutePath();
                }
            }
            if (reportDirectory == null) {
                File candidateRootDir = new File(workingDir, "target/surefire-reports");
                if (candidateRootDir.exists() && candidateRootDir.isDirectory()) {
                    reportDirectory = candidateRootDir.getAbsolutePath();
                }
            }
        }

        // 5. Inspect surefire report directory for detailed test failure report
        String testSummary = null;
        if (reportDirectory != null) {
            File reportDirFile = new File(reportDirectory);
            if (reportDirFile.exists() && reportDirFile.isDirectory()) {
                testSummary = parseSurefireReports(reportDirFile);
            }
        }

        // 6. Extract relevant failure output block
        String relevantOutput = extractRelevantFailureBlock(fullOutput);

        return new ClassificationResult(
                category, failingModule, failingPlugin, failingGoal,
                failedPhase, rootCause, reportDirectory, testSummary, relevantOutput
        );
    }

    private static String parseSurefireReports(File reportDir) {
        if (reportDir == null || !reportDir.exists() || !reportDir.isDirectory()) {
            return null;
        }

        File[] xmlFiles = reportDir.listFiles((dir, name) -> name.startsWith("TEST-") && name.endsWith(".xml"));
        StringBuilder sb = new StringBuilder();

        int totalFailures = 0;
        List<String> failedTests = new ArrayList<>();

        if (xmlFiles != null && xmlFiles.length > 0) {
            for (File xmlFile : xmlFiles) {
                try {
                    String content = Files.readString(xmlFile.toPath());
                    Matcher tcMatcher = Pattern.compile(
                            "<testcase\\s+classname=\"([^\"]+)\"\\s+name=\"([^\"]+)\"[^>]*>\\s*<(failure|error)(?:\\s+message=\"([^\"]*)\")?[^>]*>",
                            Pattern.DOTALL
                    ).matcher(content);

                    while (tcMatcher.find()) {
                        totalFailures++;
                        String className = tcMatcher.group(1);
                        String methodName = tcMatcher.group(2);
                        String failureType = tcMatcher.group(3);
                        String msg = tcMatcher.group(4);
                        if (msg == null) {
                            msg = failureType;
                        }
                        failedTests.add(className + "#" + methodName + ": " + msg.trim());
                    }
                } catch (Exception ignored) {}
            }
        }

        if (failedTests.isEmpty()) {
            File[] txtFiles = reportDir.listFiles((dir, name) -> name.endsWith(".txt") && !name.endsWith("-output.txt"));
            if (txtFiles != null) {
                for (File txtFile : txtFiles) {
                    try {
                        List<String> lines = Files.readAllLines(txtFile.toPath());
                        for (String line : lines) {
                            if (line.contains("<<< FAILURE!") || line.contains("<<< ERROR!")) {
                                totalFailures++;
                                failedTests.add(txtFile.getName() + ": " + line.trim());
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        if (failedTests.isEmpty()) {
            return "Report directory: " + reportDir.getAbsolutePath() + " (no specific test failure entries parsed)";
        }

        sb.append("Report Directory: ").append(reportDir.getAbsolutePath()).append("\n");
        sb.append("Failing Test Count: ").append(totalFailures).append("\n");
        sb.append("Failing Test Details:\n");
        for (int i = 0; i < Math.min(failedTests.size(), 10); i++) {
            sb.append("  - ").append(failedTests.get(i)).append("\n");
        }
        if (failedTests.size() > 10) {
            sb.append("  ... and ").append(failedTests.size() - 10).append(" more failing test(s).\n");
        }
        return sb.toString().trim();
    }

    private static String extractRelevantFailureBlock(String fullOutput) {
        if (fullOutput == null || fullOutput.isEmpty()) return "";

        int idx = fullOutput.indexOf("[ERROR] Failed to execute goal");
        if (idx == -1) {
            idx = fullOutput.indexOf("BUILD FAILURE");
        }
        if (idx == -1) {
            idx = fullOutput.lastIndexOf("[ERROR]");
        }

        if (idx != -1) {
            // Include a bit of context before the error line if possible
            int start = Math.max(0, idx - 200);
            String block = fullOutput.substring(start);
            if (block.length() > 3000) {
                return block.substring(0, 3000) + "\n... (truncated)";
            }
            return block;
        }

        return getTail(fullOutput, 2000);
    }

    private static String getTail(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(text.length() - maxLength);
    }
}
