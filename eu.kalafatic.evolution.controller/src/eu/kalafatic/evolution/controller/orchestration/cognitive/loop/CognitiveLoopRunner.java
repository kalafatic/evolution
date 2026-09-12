package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Headless CLI / programmatic execution harness for the Cognitive Loop.
 */
public class CognitiveLoopRunner {

    public static void main(String[] args) {
        String goalText = "Acquire training data and verify build stability.";
        String sessionId = "CognitiveHeadlessSession";
        String domain = "GENERAL";
        File projectRoot = new File(".");

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if ("--goal".equals(args[i]) && i + 1 < args.length) {
                    goalText = args[++i];
                } else if ("--session".equals(args[i]) && i + 1 < args.length) {
                    sessionId = args[++i];
                } else if ("--domain".equals(args[i]) && i + 1 < args.length) {
                    domain = args[++i];
                } else if ("--project-root".equals(args[i]) && i + 1 < args.length) {
                    projectRoot = new File(args[++i]);
                }
            }
        }

        System.out.println("Starting Headless Cognitive Loop...");
        System.out.println("Session ID: " + sessionId);
        System.out.println("Goal: " + goalText);
        System.out.println("Domain: " + domain);

        CognitiveGoal goal = new CognitiveGoal(goalText, domain, null);
        CognitiveResult result = runHeadless(sessionId, projectRoot, goal);

        System.out.println("Cognitive Loop Finished.");
        System.out.println("Final State: " + result.getFinalState());
        System.out.println("Attempts: " + result.getAttempts());
        System.out.println("Darwin Invocations: " + result.getDarwinInvocations());
        System.out.println("Summary: " + result.getSummary());

        if (result.getFinalState() != CognitiveState.SUCCESS) {
            System.exit(1);
        }
    }

    public static CognitiveResult runHeadless(String sessionId, File projectRoot, CognitiveGoal goal) {
        SessionContainer session = SessionManager.getInstance().getOrCreateSession(sessionId);
        TaskContext taskContext = new TaskContext(null, projectRoot != null ? projectRoot : new File("."));
        taskContext.setSessionId(sessionId);

        CognitiveLoopEngine engine = new CognitiveLoopEngine();
        return engine.solve(session, taskContext, goal);
    }
}
