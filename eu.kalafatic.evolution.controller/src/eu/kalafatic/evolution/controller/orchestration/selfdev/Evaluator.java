package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.tools.MavenTool;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;

public class Evaluator {
    private final File projectRoot;
    private final TaskContext context;
    private final MavenTool mavenTool = new MavenTool();

    public Evaluator(File projectRoot, TaskContext context) {
        this.projectRoot = projectRoot;
        this.context = context;
    }

    public EvaluationResult evaluate() throws Exception {
        return evaluateWithSnapshot().result;
    }

    public static class Evaluation {
        public EvaluationResult result;
        public StateSnapshot snapshot;
    }

    public Evaluation evaluateWithSnapshot() throws Exception {
        if (context != null) context.log("[EVALUATOR] Starting evaluation...");
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        StateSnapshot snapshot = new StateSnapshot();

        try {
            // Run build and tests
            String output = mavenTool.execute("clean install", projectRoot, context);

            // Basic parsing of Maven output
            boolean buildSuccess = output.contains("BUILD SUCCESS");
            result.setSuccess(buildSuccess);

            snapshot.build.status = buildSuccess ? StateSnapshot.BuildStatus.SUCCESS : StateSnapshot.BuildStatus.FAIL;
            snapshot.build.errorTypes = parseErrorTypes(output);
            snapshot.build.errorCount = snapshot.build.errorTypes.size();

            if (buildSuccess) {
                double passRate = parsePassRate(output);
                result.setTestPassRate(passRate);

                snapshot.tests.total = parseTotalTests(output);
                snapshot.tests.passed = (int) (snapshot.tests.total * passRate);
                snapshot.tests.failed = snapshot.tests.total - snapshot.tests.passed;
                snapshot.tests.failingTests = parseFailingTestNames(output);

                if (passRate < 1.0) {
                    if (context != null) context.log("[EVALUATOR] Tests failed despite build success. Rolling back.");
                    result.setDecision(SelfDevDecision.ROLLBACK);
                    result.getErrors().add("Tests failed (Pass rate: " + passRate + ")");
                } else {
                    result.setDecision(SelfDevDecision.CONTINUE);
                }
            } else {
                result.setDecision(SelfDevDecision.ROLLBACK);
                result.getErrors().add("Build failed.");
            }

            snapshot.coverage.percent = parseCoverage(output);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setDecision(SelfDevDecision.ROLLBACK);
            result.getErrors().add("Evaluation exception: " + e.getMessage());
        }

        if (context != null) context.log("[EVALUATOR] Evaluation finished. Success: " + result.isSuccess() + ", Decision: " + result.getDecision());
        Evaluation eval = new Evaluation();
        eval.result = result;
        eval.snapshot = snapshot;
        return eval;
    }

    private List<StateSnapshot.ErrorType> parseErrorTypes(String output) {
        List<StateSnapshot.ErrorType> types = new ArrayList<>();
        if (output.contains("COMPILATION ERROR")) {
            types.add(StateSnapshot.ErrorType.compiler);
        }
        if (output.contains("There are test failures")) {
            types.add(StateSnapshot.ErrorType.test);
        }
        // Simplified detection for runtime errors during build
        if (output.contains("java.lang.RuntimeException") || output.contains("java.lang.NullPointerException")) {
            types.add(StateSnapshot.ErrorType.runtime);
        }
        return types;
    }

    private int parseTotalTests(String output) {
        Pattern pattern = Pattern.compile("Tests run: (\\d+)");
        Matcher matcher = pattern.matcher(output);
        int total = 0;
        while (matcher.find()) {
            total += Integer.parseInt(matcher.group(1));
        }
        return total;
    }

    private List<String> parseFailingTestNames(String output) {
        List<String> failingTests = new ArrayList<>();
        Pattern pattern = Pattern.compile("(?m)^\\[ERROR\\]\\s+(?:Tests run:\\s+\\d+,\\s+Failures:\\s+\\d+,\\s+Errors:\\s+\\d+,\\s+Skipped:\\s+\\d+,\\s+Time elapsed:.*?<<<\\s+FAILURE!\\s+-\\s+in\\s+)(.*)$");
        Matcher matcher = pattern.matcher(output);
        while (matcher.find()) {
            failingTests.add(matcher.group(1).trim());
        }
        return failingTests;
    }

    private Double parseCoverage(String output) {
        // Placeholder for coverage parsing if JaCoCo is present in output
        return null;
    }

    public String generateFingerprint(String error) {
        if (error == null || error.isEmpty()) return "Unknown";
        String firstLine = error.split("\n")[0];
        String type = "Unknown";
        if (firstLine.contains("Compilation")) type = "compiler";
        else if (firstLine.contains("Test")) type = "test";
        else if (firstLine.contains("Exception")) type = "runtime";

        // Try to find a location
        String location = "Global";
        Pattern locPattern = Pattern.compile("([a-zA-Z0-9_]+\\.java:[0-9]+)");
        Matcher locMatcher = locPattern.matcher(error);
        if (locMatcher.find()) {
            location = locMatcher.group(1);
        }

        String cause = firstLine.replaceAll("@[a-f0-9]+", "").trim();
        return type + "@" + location + ":" + cause;
    }

    private double parsePassRate(String output) {
        // Look for "Tests run: 10, Failures: 0, Errors: 0, Skipped: 0"
        Pattern pattern = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+), Skipped: (\\d+)");
        Matcher matcher = pattern.matcher(output);
        int totalRun = 0;
        int totalFailures = 0;
        int totalErrors = 0;

        while (matcher.find()) {
            totalRun += Integer.parseInt(matcher.group(1));
            totalFailures += Integer.parseInt(matcher.group(2));
            totalErrors += Integer.parseInt(matcher.group(3));
        }

        if (totalRun == 0) return 1.0; // No tests run is considered success in pass rate terms
        return (double) (totalRun - totalFailures - totalErrors) / totalRun;
    }
}
