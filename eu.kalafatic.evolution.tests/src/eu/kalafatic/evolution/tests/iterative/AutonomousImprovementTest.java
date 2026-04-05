package eu.kalafatic.evolution.tests.iterative;

import java.util.Random;

/**
 * Simulation for the full 12-phase autonomous improvement lifecycle.
 * Lifecycle: OBSERVE -> ANALYZE -> PLAN -> VALIDATE -> EXECUTE -> TEST -> EVALUATE -> COMMIT -> PR -> FEEDBACK -> REFINE -> LEARN (-> back to PLAN)
 */
public class AutonomousImprovementTest implements ISimulationTest {

    private final ITestListener listener;
    private final Random random = new Random();
    private boolean stop = false;

    public AutonomousImprovementTest(ITestListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        stop = false;
        listener.reset();
        executeStep("observe", 0);
    }

    @Override
    public void stop() {
        this.stop = true;
    }

    private void executeStep(String step, int iterationCount) {
        if (stop) return;

        listener.stepStarted(step);
        sleep(600 + random.nextInt(400));

        if (stop) return;

        String nextStep = null;
        String edgeId = null;

        switch (step) {
            case "observe":
                nextStep = "analyze"; edgeId = "observe_analyze"; break;
            case "analyze":
                nextStep = "plan"; edgeId = "analyze_plan"; break;
            case "plan":
                nextStep = "validate"; edgeId = "plan_validate"; break;
            case "validate":
                nextStep = "execute"; edgeId = "validate_execute"; break;
            case "execute":
                nextStep = "test"; edgeId = "execute_test"; break;
            case "test":
                nextStep = "evaluate"; edgeId = "test_evaluate"; break;
            case "evaluate":
                nextStep = "commit"; edgeId = "evaluate_commit"; break;
            case "commit":
                nextStep = "PR"; edgeId = "commit_PR"; break;
            case "PR":
                nextStep = "feedback"; edgeId = "PR_feedback"; break;
            case "feedback":
                nextStep = "refine"; edgeId = "feedback_refine"; break;
            case "refine":
                nextStep = "learn"; edgeId = "refine_learn"; break;
            case "learn":
                if (iterationCount < 1 && random.nextDouble() < 0.5) {
                    listener.stepSuccess("learn");
                    listener.transitionActive("learn_plan");
                    sleep(500);
                    executeStep("plan", iterationCount + 1);
                    return;
                } else {
                    listener.stepSuccess("learn");
                    return;
                }
        }

        if (nextStep != null) {
            listener.stepSuccess(step);
            listener.transitionActive(edgeId);
            sleep(500);
            executeStep(nextStep, iterationCount);
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
