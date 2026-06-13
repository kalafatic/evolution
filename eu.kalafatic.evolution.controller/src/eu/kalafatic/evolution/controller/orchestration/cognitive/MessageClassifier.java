package eu.kalafatic.evolution.controller.orchestration.cognitive;

import java.util.regex.Pattern;

/**
 * Classifies messages into capability types and intents using rule-based logic.
 */
public class MessageClassifier {

    private static final Pattern ARCH_PATTERN = Pattern.compile("\\b(architecture|repository|module|dependency|graph|workflow|subsystem|use case|responsibility|codebase|analyze|investigate|report|summarize|discovery|landscape|artifact|mapping|structure|relationship|component)\\b");
    private static final Pattern CODE_PATTERN = Pattern.compile("\\b(java|class|method|compile|bug|exception|spring|maven|create|fix|add|run|test|generate|write|refactor|modify|delete|check|implement|build|improve|update|change|script|sql|xml|yaml|json|logic|function|variable|type|code)\\b");
    private static final Pattern EVO_PATTERN = Pattern.compile("\\b(evolve|self-dev|darwin|genome|iteration|reality model|recursive discovery|iterationmanager|darwinflow|kernel|mutation|fitness|selection|trajectory|gene|lineage|branch)\\b");
    private static final Pattern CHAT_PATTERN = Pattern.compile("^(hi|hello|hey|greetings|good morning|good afternoon|good evening|tell me a joke|thanks|thank you|tell me about|explain|help|status|who are you|who are you|what is your name|bye|goodbye|welcome)\\s*[!.]*$");

    public CapabilitySignal classify(String prompt) {
        if (prompt == null) prompt = "";
        String lower = prompt.toLowerCase().trim();

        // 1. EVOLUTION - Core Intelligence (Highest Priority for intent classification)
        if (EVO_PATTERN.matcher(lower).find()) {
            return new CapabilitySignal(CapabilityType.EVOLUTION, 8.0, SessionIntent.EVOLVING, "EVO_PATTERN");
        }

        // 2. ARCHITECTURE - Strategic Priority (High weight/precedence for unique value)
        if (ARCH_PATTERN.matcher(lower).find()) {
            SessionIntent intent = SessionIntent.ANALYZING;
            if (lower.contains("improve") || lower.contains("refactor")) intent = SessionIntent.BUILDING;
            if (lower.contains("discover") || lower.contains("explore")) intent = SessionIntent.LEARNING;
            return new CapabilitySignal(CapabilityType.ARCHITECTURE, 5.0, intent, "ARCH_PATTERN");
        }

        // 3. CODE - Implementation details
        if (CODE_PATTERN.matcher(lower).find()) {
            SessionIntent intent = SessionIntent.BUILDING;
            if (lower.contains("fix") || lower.contains("bug") || lower.contains("error") || lower.contains("exception")) {
                intent = SessionIntent.TROUBLESHOOTING;
            } else if (lower.contains("explain") || lower.contains("how to") || lower.contains("tell me about") || lower.contains("what is")) {
                intent = SessionIntent.LEARNING;
            }
            return new CapabilitySignal(CapabilityType.CODE, 3.0, intent, "CODE_PATTERN");
        }

        // 4. CHAT - Interactive overhead
        if (CHAT_PATTERN.matcher(lower).find() || lower.length() < 10) {
            return new CapabilitySignal(CapabilityType.CHAT, 1.0, SessionIntent.LEARNING, "CHAT_PATTERN");
        }

        return new CapabilitySignal(CapabilityType.CHAT, 1.0, SessionIntent.LEARNING, "FALLBACK");
    }
}
