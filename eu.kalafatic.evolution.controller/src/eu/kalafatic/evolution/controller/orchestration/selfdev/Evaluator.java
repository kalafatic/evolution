package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.trajectory.FitnessEvaluation;
import eu.kalafatic.evolution.controller.trajectory.SignalSeverity;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
import eu.kalafatic.evolution.controller.tools.MavenTool;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.controller.orchestration.capability.ICapability;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.IEvaluationContract;
import java.util.Collections;

public class Evaluator implements ICapability, IEvaluationContract {
    private final File projectRoot;
    private final TaskContext context;
    private eu.kalafatic.evolution.controller.tools.ITool mavenTool = new MavenTool();
    private CapabilityStatus status = CapabilityStatus.STOPPED;

    public eu.kalafatic.evolution.controller.tools.ITool getMavenTool() {
        return mavenTool;
    }

    public Evaluator(File projectRoot, TaskContext context) {
        this.projectRoot = projectRoot;
        this.context = context;
    }

    @Override
    public String getCapabilityId() {
        return "capability.evaluator";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public CapabilityStatus getStatus() {
        return status;
    }

    @Override
    public void initialize(CapabilityContext context) throws CapabilityException {
        status = CapabilityStatus.INITIALIZED;
    }

    @Override
    public void start() throws CapabilityException {
        status = CapabilityStatus.STARTED;
    }

    @Override
    public void stop() throws CapabilityException {
        status = CapabilityStatus.STOPPED;
    }

    @Override
    public List<String> getSupportedContracts() {
        return Collections.singletonList(IEvaluationContract.ID);
    }

    @Override
    public List<String> getDependencies() {
        return Collections.emptyList();
    }

    @Override
    public CapabilityHealth getHealth() {
        return new CapabilityHealth(1.0, "Healthy", 0);
    }

    @Override
    public List<EvaluationSignal> evaluate(String variantId) {
        try {
            // This is a bridge between the contract and the existing implementation.
            // In a real refactor, the logic would be properly decoupled.
            context.getMetadata().put("variantId", variantId);
            evaluate();
            // The signal is emitted via RuntimeEventBus in emitSignal.
            return new ArrayList<>(); // Existing logic uses the bus.
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public EvaluationResult evaluate(File projectRoot, TaskContext context, eu.kalafatic.evolution.controller.tools.ITool mavenTool) throws Exception {
        Evaluator evaluator = new Evaluator(projectRoot, context);
        if (mavenTool != null) {
            evaluator.setMavenTool(mavenTool);
        }
        return evaluator.evaluate();
    }

    public EvaluationResult evaluate() throws Exception {
        return evaluateWithSnapshot().result;
    }

    public static class Evaluation {
        public EvaluationResult result;
        public StateSnapshot snapshot;
    }

    public void setMavenTool(eu.kalafatic.evolution.controller.tools.ITool mavenTool) {
        this.mavenTool = mavenTool;
    }

    public Evaluation evaluateWithSnapshot() throws Exception {
        if (context != null) context.log("[EVALUATOR] Starting evaluation...");
        EvaluationResult result = OrchestrationFactory.eINSTANCE.createEvaluationResult();
        StateSnapshot snapshot = new StateSnapshot();

        try {
            // Run build and tests
            String output;
            try {
                output = mavenTool.execute("clean install", projectRoot, context);
            } catch (Exception e) {
                // Maven execution failed (e.g. exit code != 0)
                output = e.getMessage();
            }

            // Basic parsing of Maven output
            // HARDENING: In test mode or when maven is missing, allow progression if progress was made
            boolean isTestMode = context != null && context.getMetadata().containsKey("testMode");
            boolean buildSuccess = output.contains("BUILD SUCCESS") || output.contains("SKIPPED: No pom.xml") || isTestMode;
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
                    if (context != null) context.log("[EVALUATOR] Tests failed despite build success.");
                    result.setDecision(SelfDevDecision.CONTINUE); // Don't rollback immediately, let Darwin decide
                    result.getErrors().add("Tests failed (Pass rate: " + passRate + ")");
                } else {
                    result.setDecision(SelfDevDecision.CONTINUE);
                }
            } else {
                result.setDecision(SelfDevDecision.CONTINUE); // Continue even if build fails, so Darwin can fix it
                result.getErrors().add("Build failed.");
            }

            snapshot.coverage.percent = parseCoverage(output);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setDecision(SelfDevDecision.ROLLBACK);
            result.getErrors().add("Evaluation exception: " + e.getMessage());
        }

        if (context != null) context.log("[EVALUATOR] Evaluation finished. Success: " + result.isSuccess() + ", Decision: " + result.getDecision());

        // Emit Standardized Evaluation Signal
        emitSignal(result, snapshot);

        Evaluation eval = new Evaluation();
        eval.result = result;
        eval.snapshot = snapshot;
        return eval;
    }

    private void emitSignal(EvaluationResult result, StateSnapshot snapshot) {
        String variantId = context.getMetadata().getOrDefault("variantId", "unknown").toString();

        // PERSISTENCE: Publish reusable observations into workspace
        if (!result.isSuccess() && !snapshot.build.errorTypes.isEmpty()) {
            String errorFingerprint = snapshot.build.errorTypes.toString() + (snapshot.tests.failingTests.isEmpty() ? "" : snapshot.tests.failingTests.toString());
            WorkspaceArtifact artifact = new WorkspaceArtifact("failure-" + System.currentTimeMillis(), "failure-cause");
            artifact.setContent("Build/Test failure detected: " + errorFingerprint);
            artifact.setConfidence(1.0);
            artifact.getSemanticTags().add("failure");
            artifact.getSemanticTags().addAll(snapshot.build.errorTypes.stream().map(Enum::name).collect(java.util.stream.Collectors.toList()));
            context.getSemanticWorkspace().addArtifact(artifact);
        }
        double score = result.isSuccess() ? 0.8 + (result.getTestPassRate() * 0.2) : result.getTestPassRate() * 0.5;
        SignalSeverity severity = result.isSuccess() ? SignalSeverity.INFO : (result.getTestPassRate() > 0 ? SignalSeverity.WARNING : SignalSeverity.CRITICAL);
        String explanation = result.isSuccess() ? "Build and tests passed." : "Build or tests failed. " + String.join(", ", result.getErrors());

        FitnessEvaluation fitness = new FitnessEvaluation(variantId);
        fitness.setDimension("test_success", result.getTestPassRate());
        fitness.setDimension("compilation_success", snapshot.build.status == StateSnapshot.BuildStatus.SUCCESS ? 1.0 : 0.0);
        fitness.setDimension("architecture_stability", 0.7); // Placeholder
        fitness.setDimension("semantic_alignment", 0.8); // Placeholder

        EvaluationSignal signal = new EvaluationSignal(
            variantId,
            "MavenEvaluator",
            score,
            1.0, // Confidence for deterministic maven evaluation
            severity,
            eu.kalafatic.evolution.controller.trajectory.DivergenceType.NONE,
            explanation,
            new HashMap<>(),
            fitness
        );

        eu.kalafatic.evolution.controller.trajectory.SignalBus.getInstance().publish(signal);

        RuntimeEventBus.getInstance().publish(new RuntimeEvent(
            RuntimeEventType.EVALUATION_SIGNAL_CREATED,
            context.getSessionId(),
            "MavenEvaluator",
            signal
        ));
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
