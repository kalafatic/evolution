package eu.kalafatic.evolution.tests.iterative;

import java.util.Random;

/**
 * Implementation of the Iterative Development lifecycle test.
 */
public class IterativeDevelopmentTest {

    private final ITestListener listener;
    private final Random random = new Random();
    private boolean stop = false;

    public IterativeDevelopmentTest(ITestListener listener) {
        this.listener = listener;
    }

    public void run() {
        stop = false;
        listener.reset();
        executeStep("prompt", 0);
    }

    public void stop() {
        this.stop = true;
    }

    private void executeStep(String step, int iterationCount) {
        if (stop) return;

        listener.stepStarted(step);

        // Simulate work
        sleep(800 + random.nextInt(400));

        if (stop) return;

        String nextStep = null;
        String edgeId = null;

        boolean success = true;

        // Custom logic for each step
        switch (step) {
            case "prompt":
                nextStep = "plan"; edgeId = "prompt_plan"; break;
            case "plan":
                nextStep = "implement"; edgeId = "plan_implement"; break;
            case "implement":
                nextStep = "compile"; edgeId = "implement_compile"; break;
            case "compile":
                if (random.nextDouble() < 0.1) success = false;
                else { nextStep = "test"; edgeId = "compile_test"; }
                break;
            case "test":
                if (random.nextDouble() < 0.15) success = false;
                else { nextStep = "evaluate"; edgeId = "test_evaluate"; }
                break;
            case "evaluate":
                nextStep = "iterate"; edgeId = "evaluate_iterate"; break;
            case "iterate":
                if (iterationCount < 1 && random.nextDouble() < 0.4) {
                    listener.stepSuccess("iterate");
                    listener.transitionActive("iterate_plan");
                    sleep(500);
                    executeStep("plan", iterationCount + 1);
                    return;
                } else {
                    nextStep = "commit"; edgeId = "iterate_commit";
                }
                break;
            case "commit":
                nextStep = "PR"; edgeId = "commit_PR"; break;
            case "PR":
                nextStep = "feedback"; edgeId = "PR_feedback"; break;
            case "feedback":
                nextStep = "refine"; edgeId = "feedback_refine"; break;
            case "refine":
                listener.stepSuccess("refine");
                return;
        }

        if (success) {
            listener.stepSuccess(step);
            if (nextStep != null && edgeId != null) {
                listener.transitionActive(edgeId);
                sleep(500);
                executeStep(nextStep, iterationCount);
            }
        } else {
            listener.stepFailed(step);
        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
