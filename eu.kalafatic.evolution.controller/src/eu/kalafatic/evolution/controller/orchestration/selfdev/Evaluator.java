package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.tools.MavenTool;
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
        context.log("[EVALUATOR] Starting evaluation...");
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();

        try {
            // Run build and tests
            String output = mavenTool.execute("clean install", projectRoot, context);

            // Basic parsing of Maven output
            boolean buildSuccess = output.contains("BUILD SUCCESS");
            result.setSuccess(buildSuccess);

            if (buildSuccess) {
                double passRate = parsePassRate(output);
                result.setTestPassRate(passRate);

                if (passRate < 1.0) {
                    context.log("[EVALUATOR] Tests failed despite build success. Rolling back.");
                    result.setDecision(SelfDevDecision.ROLLBACK);
                    result.getErrors().add("Tests failed (Pass rate: " + passRate + ")");
                } else {
                    result.setDecision(SelfDevDecision.CONTINUE);
                }
            } else {
                result.setDecision(SelfDevDecision.ROLLBACK);
                result.getErrors().add("Build failed.");
            }

        } catch (Exception e) {
            result.setSuccess(false);
            result.setDecision(SelfDevDecision.ROLLBACK);
            result.getErrors().add("Evaluation exception: " + e.getMessage());
        }

        context.log("[EVALUATOR] Evaluation finished. Success: " + result.isSuccess() + ", Decision: " + result.getDecision());
        return result;
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
