package eu.kalafatic.evolution.tests.iterative;

import java.util.Random;

/**
 * Simulation for the manual orchestration flow.
 * Lifecycle: OBSERVE -> ANALYZE -> PLAN -> VALIDATE -> EXECUTE -> TEST -> EVALUATE
 */
public class ManualOrchestrationTest implements ISimulationTest {

    private final ITestListener listener;
    private final Random random = new Random();
    private boolean stop = false;

    public ManualOrchestrationTest(ITestListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        stop = false;
        listener.reset();
        executeStep("observe");
    }

    @Override
    public void stop() {
        this.stop = true;
    }

    private void executeStep(String step) {
        if (stop) return;

        listener.stepStarted(step);
        sleep(800 + random.nextInt(400));

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
                listener.stepSuccess("evaluate");
                return;
        }

        if (nextStep != null) {
            listener.stepSuccess(step);
            listener.transitionActive(edgeId);
            sleep(500);
            executeStep(nextStep);
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
