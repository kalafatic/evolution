package eu.kalafatic.evolution.controller.orchestration.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import eu.kalafatic.evolution.controller.orchestration.SystemState;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.cognitive.CapabilityType;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;

/**
 * Dynamically classifies a user prompt into CHAT, TASK, or CONTROL.
 *
 * <p>Core principle: <b>Everything is evolution EXCEPT pure conversational chat.</b></p>
 *
 * <p>Classification flow:</p>
 * <ol>
 *   <li><b>Exact match cache</b> – fast path.</li>
 *   <li><b>State‑aware control detection</b> – e.g., awaiting branch selection.</li>
 *   <li><b>Heuristic scoring</b> – 0.0 (CHAT) to 1.0 (TASK).</li>
 *   <li><b>LLM fallback</b> – only for ambiguous scores.</li>
 *   <li><b>Conservative default</b> – TASK if still uncertain.</li>
 * </ol>
 *
 * <p>Single‑class, zero external dependencies beyond standard Java and EVO context.</p>
 */
public final class PromptClassifier {

    // ======================== CONSTANTS ========================

    /** Minimum length for a prompt to be considered a task. */
    private static final int MIN_TASK_LENGTH = 5;

    /** Score threshold below which we treat as CHAT (0.0–1.0). */
    private static final double CHAT_THRESHOLD = 0.25;

    /** Score threshold above which we treat as TASK (0.0–1.0). */
    private static final double TASK_THRESHOLD = 0.70;

    /** Confidence threshold for LLM fallback (0.0–1.0). */
    private static final double AMBIGUITY_THRESHOLD = 0.45;

    // ======================== PATTERNS ========================

    // ---- Greetings / small talk (strong CHAT signal) ----
    private static final Pattern CHAT_GREETING = Pattern.compile(
        "^(?i)(hi|hello|hey|good morning|good afternoon|good evening|thanks?|thank you|"
        + "how are you|what'?s up|yo|sup|okay|ok|got it|i see|understood|"
        + "great|awesome|nice|sure|fine|goodbye|bye)$"
    );

    // ---- Control commands (Darwin UI interactions) ----
    private static final Pattern CONTROL_SELECT = Pattern.compile(
        "^(?i)(select|approve|choose|pick)\\s+(variant|trajectory|branch|v)?\\s*([a-zA-Z0-9_\\-]+)$"
    );
    private static final Pattern CONTROL_KEEP = Pattern.compile(
        "^(?i)keep\\s+(variant|trajectory|branch)?\\s*([a-zA-Z0-9_\\-]+)$"
    );
    private static final Pattern CONTROL_REJECT = Pattern.compile(
        "^(?i)(reject|rejected|no|stop|cancel|dismiss)\\s*(variant|trajectory|branch)?\\s*([a-zA-Z0-9_\\-]+)?$"
    );
    private static final Pattern CONTROL_FORCE = Pattern.compile(
        "^(?i)force\\s+solution$"
    );
    private static final Pattern CONTROL_PROCEED = Pattern.compile(
        "^(?i)(yes|y|proceed|go ahead|approved|confirm|continue|ok|okay)$"
    );

    // ---- Task indicators (code, architecture, changes) ----
    private static final Pattern TASK_CODE_KEYWORD = Pattern.compile(
        "(?i)\\b(class|interface|enum|method|function|implement|extends|override|"
        + "public|private|protected|static|void|return|throw|try|catch|finally|"
        + "import|package|new|this|super|synchronized|volatile|transient|"
        + "abstract|final|native|strictfp|transient|volatile|synchronized)\\b"
    );
    private static final Pattern TASK_SYNTAX = Pattern.compile(
        "[{}()\\[\\];=<>]|//|/\\*|\\*/|@[a-zA-Z]+|\\.\\w+\\s*\\("
    );
    private static final Pattern TASK_PATH = Pattern.compile(
        "[a-zA-Z0-9_\\-./]+\\.[a-zA-Z]+"  // file paths like src/main/java/App.java
    );
    private static final Pattern TASK_ACTION = Pattern.compile(
        "(?i)\\b(fix|refactor|implement|create|generate|build|compile|test|debug|"
        + "optimize|migrate|upgrade|restructure|design|architect|add|remove|"
        + "rename|move|extract|inline|consolidate|reorganize|simplify)\\b"
    );

    // ---- Mediated mode indicators ----
    private static final Pattern MEDIATED_SELECT = Pattern.compile(
        "(?i)(mediate|zip|export|package|prompt|context)\\s+(prepare|generate|create|analyze)"
    );

    // ======================== CACHE ========================

    /** Cache for exact prompt matches to avoid re‑computation. */
    private final Map<String, PromptClassification> exactMatchCache = new ConcurrentHashMap<>();

    /** Cache for LLM fallback results (keyed by prompt + state signature). */
    private final Map<String, PromptClassification> llmCache = new ConcurrentHashMap<>();

    // ======================== DEPENDENCIES ========================

    private final PromptInstructions promptInstructions;
    private final boolean isMediatedMode;
    private final boolean isStepMode;

    /**
     * Constructor.
     *
     * @param promptInstructions the user's explicit instructions (may be null)
     * @param isMediatedMode     true if the platform is in mediated export mode
     */
    public PromptClassifier(PromptInstructions promptInstructions, boolean isMediatedMode) {
        this.promptInstructions = promptInstructions;
        this.isMediatedMode = isMediatedMode;
        this.isStepMode = promptInstructions != null && promptInstructions.isStepMode();
    }

    // ======================== PUBLIC API ========================

    /**
     * Classifies a user prompt based on content, context, and system state.
     *
     * @param prompt  the raw user input (never null)
     * @param context the current execution context (provides state, history, capability)
     * @return a rich classification result
     */
    public PromptClassification classify(String prompt, TaskContext context) {
        // 1. Sanitize input
        String trimmed = prompt == null ? "" : prompt.trim();
        if (trimmed.isEmpty()) {
            return new PromptClassification(PromptCategory.CHAT, 0.0, "Empty input", false);
        }

        // 2. Exact match cache (fast path)
        String cacheKey = trimmed.toLowerCase();
        if (exactMatchCache.containsKey(cacheKey)) {
            return exactMatchCache.get(cacheKey);
        }

        // 3. State‑aware control detection
        PromptClassification stateControl = detectStateControl(trimmed, context);
        if (stateControl != null) {
            exactMatchCache.put(cacheKey, stateControl);
            return stateControl;
        }

        // 4. Heuristic scoring
        double score = heuristicScore(trimmed, context);
        double confidence = computeConfidence(score);

        // 5. If highly confident, return directly
        if (confidence > 0.45) {
            PromptCategory category = resolveCategory(score, trimmed, context);
            PromptClassification result = new PromptClassification(
                category,
                score,
                String.format("Heuristic: score=%.2f, confidence=%.2f", score, confidence),
                false
            );
            exactMatchCache.put(cacheKey, result);
            return result;
        }

        // 6. AMBIGUOUS → LLM fallback (if enabled and not pure chat capability)
        if (shouldUseLlmFallback(context)) {
            PromptClassification llmResult = classifyWithLlm(trimmed, context);
            exactMatchCache.put(cacheKey, llmResult);
            return llmResult;
        }

        // 7. Conservative default: TASK (better to evolve than bypass)
        PromptClassification fallback = new PromptClassification(
            PromptCategory.TASK,
            score,
            String.format("Conservative fallback (ambiguous score=%.2f)", score),
            false
        );
        exactMatchCache.put(cacheKey, fallback);
        return fallback;
    }

    // ======================== STRATEGIES ========================

    /**
     * Detects control commands based on the current system state.
     *
     * @return classification or null if no control match
     */
    private PromptClassification detectStateControl(String prompt, TaskContext context) {
        String lower = prompt.toLowerCase();

        // ---- If we are awaiting a branch selection ----
        SystemState currentState = context.getStateHolder().getState();
        if (currentState == SystemState.AWAITING_BRANCH_SELECTION) {

            // "Force Solution" always overrides
            if (CONTROL_FORCE.matcher(lower).matches()) {
                return new PromptClassification(PromptCategory.CONTROL, 1.0, "Force Solution command", true);
            }

            // "Select v0" or "Approve variant direct_minimal"
            java.util.regex.Matcher selectMatcher = CONTROL_SELECT.matcher(lower);
            if (selectMatcher.matches()) {
                String id = selectMatcher.group(3);
                if (id != null) {
                    return new PromptClassification(PromptCategory.CONTROL, 1.0, "Variant selection: " + id, true);
                }
            }

            // "Keep variant v1"
            java.util.regex.Matcher keepMatcher = CONTROL_KEEP.matcher(lower);
            if (keepMatcher.matches()) {
                String id = keepMatcher.group(2);
                if (id != null) {
                    return new PromptClassification(PromptCategory.CONTROL, 1.0, "Keep variant: " + id, true);
                }
            }

            // "Reject variant v2" or just "Reject"
            java.util.regex.Matcher rejectMatcher = CONTROL_REJECT.matcher(lower);
            if (rejectMatcher.matches() || lower.equals("reject") || lower.equals("no")) {
                return new PromptClassification(PromptCategory.CONTROL, 1.0, "Rejection command", true);
            }

            // "Yes" / "Proceed" — during awaiting selection, often "proceed with best"
            if (CONTROL_PROCEED.matcher(lower).matches()) {
                return new PromptClassification(PromptCategory.CONTROL, 0.9, "Proceed with selection", true);
            }
        }

        // ---- Generic control outside selection ----
        if (CONTROL_FORCE.matcher(lower).matches()) {
            return new PromptClassification(PromptCategory.CONTROL, 1.0, "Force Solution command", true);
        }

        // Proceed commands (not in AWAITING state → still control)
        if (CONTROL_PROCEED.matcher(lower).matches()) {
            return new PromptClassification(PromptCategory.CONTROL, 0.8, "Proceed command", true);
        }

        return null;
    }

    /**
     * Heuristic scoring: 0.0 = definitely CHAT, 1.0 = definitely TASK.
     */
    private double heuristicScore(String prompt, TaskContext context) {
        String lower = prompt.toLowerCase();
        String raw = prompt;

        double score = 0.0;

        // ---- Factor 1: Length (short prompts tend to be chat/control) ----
        int length = raw.length();
        if (length < MIN_TASK_LENGTH) {
            score -= 0.15;
        } else if (length > 50) {
            score += 0.1;
        } else if (length > 100) {
            score += 0.2;
        }

        // ---- Factor 2: Greetings / small talk (strong CHAT signal) ----
        if (CHAT_GREETING.matcher(lower).matches()) {
            return 0.0; // strong override
        }

        // ---- Factor 3: Code keywords (strong TASK signal) ----
        if (TASK_CODE_KEYWORD.matcher(raw).find()) {
            score += 0.35;
        }

        // ---- Factor 4: Code syntax (brackets, semicolons, annotations) ----
        if (TASK_SYNTAX.matcher(raw).find()) {
            score += 0.25;
        }

        // ---- Factor 5: File paths (src/main/java/...) ----
        if (TASK_PATH.matcher(raw).find()) {
            score += 0.2;
        }

        // ---- Factor 6: Action verbs (fix, implement, refactor) ----
        if (TASK_ACTION.matcher(lower).find()) {
            score += 0.2;
        }

        // ---- Factor 7: Mediated mode detection ----
        if (isMediatedMode && MEDIATED_SELECT.matcher(lower).find()) {
            score += 0.2;
        }

        // ---- Factor 8: Step mode — short confirmations are CHAT, not TASK ----
        if (isStepMode) {
            if (CONTROL_PROCEED.matcher(lower).matches() || lower.equals("ok")) {
                score -= 0.3;
            }
        }

        // ---- Factor 9: Active evolution session — "yes"/"ok" is control, not task ----
        if (context.getOrchestrationState().getIterationCount() > 0) {
            if (length < 10 && !TASK_CODE_KEYWORD.matcher(raw).find()) {
                score -= 0.2;
            }
        }

        // Clamp to [0.0, 1.0]
        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Computes confidence from the raw score. Higher when score is near 0 or 1.
     */
    private double computeConfidence(double score) {
        return 1.0 - Math.abs(score - 0.5) * 2.0;
    }

    /**
     * Resolves the final category from the heuristic score.
     */
    private PromptCategory resolveCategory(double score, String prompt, TaskContext context) {
        if (score <= CHAT_THRESHOLD) {
            return PromptCategory.CHAT;
        }
        if (score >= TASK_THRESHOLD) {
            return PromptCategory.TASK;
        }

        // If in mediated mode, bias toward TASK (exports need evolution)
        if (isMediatedMode) {
            return PromptCategory.TASK;
        }

        // For ambiguous, check length + action verbs one more time
        if (prompt.length() > 30 && TASK_ACTION.matcher(prompt.toLowerCase()).find()) {
            return PromptCategory.TASK;
        }

        // Conservative: TASK over CHAT (better to evolve than to skip)
        return PromptCategory.TASK;
    }

    // ======================== LLM FALLBACK ========================

    /**
     * Determines whether to use the LLM fallback.
     * Disabled in CHAT capability mode or mediated mode (to avoid cost).
     */
    private boolean shouldUseLlmFallback(TaskContext context) {
        // If the execution profile says we are in pure CHAT mode, skip LLM fallback.
        CapabilityType capability = context.getExecutionProfile().getCapability();
        if (capability == CapabilityType.CHAT) {
            return false;
        }
        // Mediated mode often has expensive prompts; prefer heuristics.
        if (isMediatedMode) {
            return false;
        }
        return true;
    }

    /**
     * Uses a lightweight LLM to classify the prompt.
     *
     * <p>Cache key is prompt + a signature of the current system state.</p>
     */
    private PromptClassification classifyWithLlm(String prompt, TaskContext context) {
        String cacheKey = prompt.toLowerCase() + "|" + context.getStateHolder().getState().toString();
        if (llmCache.containsKey(cacheKey)) {
            return llmCache.get(cacheKey);
        }

        try {
            // Build a minimal classification prompt
            String llmPrompt = String.format(
                "Classify the following user input as exactly one of: CHAT, TASK, CONTROL.\n\n" +
                "- CHAT: casual conversation, greetings, explanations, non‑executable questions.\n" +
                "- TASK: code creation, refactoring, fixing, architecture analysis, implementation.\n" +
                "- CONTROL: UI commands like 'select v0', 'approve', 'reject', 'force solution'.\n\n" +
                "Input: \"%s\"\n\n" +
                "Output only the category name in uppercase.",
                prompt.replace("\"", "\\\"")
            );

            String response = context.getAiService().sendRequest(
                context.getOrchestrator(),
                llmPrompt,
                context
            ).trim().toUpperCase();

            PromptCategory category;
            if (response.contains("CHAT")) {
                category = PromptCategory.CHAT;
            } else if (response.contains("CONTROL")) {
                category = PromptCategory.CONTROL;
            } else if (response.contains("TASK")) {
                category = PromptCategory.TASK;
            } else {
                // Fallback: use heuristic
                double score = heuristicScore(prompt, context);
                category = resolveCategory(score, prompt, context);
            }

            PromptClassification result = new PromptClassification(
                category,
                0.5,
                "LLM fallback classification",
                true
            );
            llmCache.put(cacheKey, result);
            return result;

        } catch (Exception e) {
            // If LLM fails, fall back to conservative TASK
            return new PromptClassification(
                PromptCategory.TASK,
                0.5,
                "LLM failed, conservative fallback to TASK: " + e.getMessage(),
                false
            );
        }
    }

    // ======================== INNER CLASSES ========================

    /**
     * Classification categories.
     */
    public enum PromptCategory {
        /** Pure conversation — bypass Darwin, respond directly. */
        CHAT,

        /** Evolution task — must use Darwin iterative flow. */
        TASK,

        /** UI control command — route to selection engine. */
        CONTROL
    }

    /**
     * Rich classification result containing metadata for downstream decision-making.
     */
    public static final class PromptClassification {

        private final PromptCategory category;
        private final double confidence;
        private final String rationale;
        private final boolean requiresUserAction;

        public PromptClassification(PromptCategory category, double confidence, String rationale, boolean requiresUserAction) {
            this.category = category;
            this.confidence = Math.max(0.0, Math.min(1.0, confidence));
            this.rationale = rationale;
            this.requiresUserAction = requiresUserAction;
        }

        public PromptCategory getCategory() {
            return category;
        }

        public double getConfidence() {
            return confidence;
        }

        public String getRationale() {
            return rationale;
        }

        public boolean isRequiresUserAction() {
            return requiresUserAction;
        }

        public boolean isChat() {
            return category == PromptCategory.CHAT;
        }

        public boolean isTask() {
            return category == PromptCategory.TASK;
        }

        public boolean isControl() {
            return category == PromptCategory.CONTROL;
        }

        @Override
        public String toString() {
            return String.format("PromptClassification{category=%s, confidence=%.2f, rationale='%s'}",
                category, confidence, rationale);
        }
    }
}
