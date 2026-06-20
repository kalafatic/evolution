package eu.kalafatic.evolution.controller.kernel;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
import eu.kalafatic.evolution.controller.orchestration.cognitive.SessionCognitiveState;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.utils.semantic.EvolutionComponent;

/**
 * Calculates the intensity of evolutionary search based on multiple signals.
 */
@EvolutionComponent(domain = "kernel", role = "intensity-authority")
public class EvolutionIntensityCalculator {

    public static int calculate(TaskContext context, Trajectory trajectory, EvolutionaryPressureVector pressure) {
        double intensity = 1.0;

        // 1. Capability & Depth Signal
        CapabilityType cap = CapabilityType.CHAT;
        int depth = 1;
        eu.kalafatic.evolution.controller.orchestration.SessionContainer session =
            eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(context.getSessionId());
        if (session instanceof eu.kalafatic.evolution.controller.orchestration.SessionContext) {
            var cogState = ((eu.kalafatic.evolution.controller.orchestration.SessionContext)session).getCognitiveState();
            cap = cogState.getCurrentCapability();
            depth = cogState.getCognitiveDepth();
        }

        switch (cap) {
            case EVOLUTION: intensity += 2.0; break;
            case ARCHITECTURE: intensity += 1.5; break;
            case CODE: intensity += 1.0; break;
            default: break;
        }

        intensity += (depth / 10.0) * 1.5;

        // 2. Pressure Signal
        if (pressure != null) {
            intensity += (pressure.getTotalPressure() * 2.0);
        }

        // 3. Goal Complexity (Heuristic)
        String goal = context.getOrchestrationState().getRawInput();
        if (goal != null) {
            if (goal.length() > 500) intensity += 1.0;
            if (goal.contains("{") || goal.contains("class") || goal.contains("interface")) intensity += 1.0;
        }

        // 4. Trajectory Stability (Inverse)
        if (trajectory != null) {
            double fitness = trajectory.getFitnessScore();
            if (fitness < 0.3) intensity += 1.5;
            else if (fitness < 0.6) intensity += 0.5;
        }

        // 5. Expansion Settings
        int expansion = 5;
        if (context.getOrchestrator() != null && context.getOrchestrator().getAiChat() != null) {
            String sessionId = context.getSessionId();
            var chatSession = context.getOrchestrator().getAiChat().getSessions().stream()
                .filter(s -> s.getId().equals(sessionId)).findFirst().orElse(null);
            if (chatSession != null) {
                expansion = chatSession.getExpansion();
            }
        }
        intensity += (expansion - 5) * 0.2;

        return (int) Math.max(1, Math.min(4, Math.round(intensity)));
    }
}
