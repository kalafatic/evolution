package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.regex.Pattern;

/**
 * Classifies messages into capability types and intents using rule-based logic.
 */
public class MessageClassifier {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\b(java|class|method|compile|bug|exception|spring|maven|create|fix|add|run|test|generate|write|refactor|modify|delete|check|implement|build|improve|update|change|approve|select)\\b");
    private static final Pattern ARCH_PATTERN = Pattern.compile("\\b(architecture|repository|module|dependency|graph|workflow|subsystem|use case|responsibility|codebase|analyze|investigate|report|summarize)\\b");
    private static final Pattern EVO_PATTERN = Pattern.compile("\\b(evolve|self-dev|darwin|genome|iteration|reality model|recursive discovery|iterationmanager|darwinflow|kernel)\\b");
    private static final Pattern CHAT_PATTERN = Pattern.compile("^(hi|hello|hey|greetings|good morning|good afternoon|good evening|tell me a joke|thanks|tell me about|explain)\\s*[!.]*$");

    public CapabilitySignal classify(String prompt) {
        if (prompt == null) prompt = "";
        String lower = prompt.toLowerCase().trim();

        if (EVO_PATTERN.matcher(lower).find()) {
            return new CapabilitySignal(CapabilityType.EVOLUTION, 8.0, SessionIntent.EVOLVING, "EVO_PATTERN");
        }

        if (ARCH_PATTERN.matcher(lower).find()) {
            return new CapabilitySignal(CapabilityType.ARCHITECTURE, 5.0, SessionIntent.ANALYZING, "ARCH_PATTERN");
        }

        if (CODE_PATTERN.matcher(lower).find()) {
            return new CapabilitySignal(CapabilityType.CODE, 3.0, SessionIntent.BUILDING, "CODE_PATTERN");
        }

        if (CHAT_PATTERN.matcher(lower).find()) {
            return new CapabilitySignal(CapabilityType.CHAT, 1.0, SessionIntent.LEARNING, "CHAT_PATTERN");
        }

        return new CapabilitySignal(CapabilityType.CHAT, 1.0, SessionIntent.LEARNING, "FALLBACK");
    }
}
