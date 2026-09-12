package eu.kalafatic.evolution.controller.orchestration.cognitive.loop;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Proposed decision produced by runtime intelligence (LLM) or deterministic policies.
 */
public class CognitiveDecision {
    private final CognitiveDecisionType type;
    private final String targetCapability;
    private final String commandOrStrategy;
    private final String reasoning;
    private final Map<String, Object> parameters;

    public CognitiveDecision(CognitiveDecisionType type, String targetCapability, String commandOrStrategy, String reasoning, Map<String, Object> parameters) {
        this.type = type;
        this.targetCapability = targetCapability;
        this.commandOrStrategy = commandOrStrategy;
        this.reasoning = reasoning != null ? reasoning : "";
        this.parameters = parameters != null ? new HashMap<>(parameters) : new HashMap<>();
    }

    public static CognitiveDecision of(CognitiveDecisionType type, String reasoning) {
        return new CognitiveDecision(type, null, null, reasoning, null);
    }

    public static CognitiveDecision capability(String toolOrCapability, String command, String reasoning) {
        return new CognitiveDecision(CognitiveDecisionType.USE_CAPABILITY, toolOrCapability, command, reasoning, null);
    }

    public static CognitiveDecision adapt(String newStrategy, String reasoning) {
        return new CognitiveDecision(CognitiveDecisionType.ADAPT, null, newStrategy, reasoning, null);
    }

    public static CognitiveDecision darwin(String searchStrategy, String reasoning) {
        return new CognitiveDecision(CognitiveDecisionType.START_DARWIN, null, searchStrategy, reasoning, null);
    }

    public CognitiveDecisionType getType() {
        return type;
    }

    public String getTargetCapability() {
        return targetCapability;
    }

    public String getCommandOrStrategy() {
        return commandOrStrategy;
    }

    public String getReasoning() {
        return reasoning;
    }

    public Map<String, Object> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public String toString() {
        return "CognitiveDecision{" +
                "type=" + type +
                ", targetCapability='" + targetCapability + '\'' +
                ", commandOrStrategy='" + commandOrStrategy + '\'' +
                ", reasoning='" + reasoning + '\'' +
                '}';
    }
}
