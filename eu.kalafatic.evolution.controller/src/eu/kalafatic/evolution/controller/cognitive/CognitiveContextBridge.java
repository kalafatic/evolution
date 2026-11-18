package eu.kalafatic.evolution.controller.cognitive;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.cognitive.model.ContextItem;
import eu.kalafatic.evolution.controller.cognitive.model.ContextPackage;
import eu.kalafatic.evolution.controller.cognitive.model.ContextRequest;
import eu.kalafatic.evolution.controller.memory.MemoryInformationStatus;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Native bridge between the Darwin context pipeline and the Cognitive
 * Services platform. Darwin calls CognitiveContextService directly
 * (OSGi/Java, never the MCP server over HTTP).
 *
 * <p>Priority model (highest first) — enforced by prompt ordering:
 * <ol>
 *   <li>System and safety instructions (architecture rules, constraints)</li>
 *   <li>Current explicit user instructions (task hints)</li>
 *   <li>Current task requirements (goal/step)</li>
 *   <li>Current session state (semantic workspace artifacts)</li>
 *   <li>Verified current project state (cognitive project state)</li>
 *   <li>Explicit user preferences</li>
 *   <li>Relevant historical context (confirmed memories, decisions, issues)</li>
 *   <li>Inferred preferences (rendered as observed behavior)</li>
 *   <li>Lower-priority background knowledge (procedures, assumptions)</li>
 * </ol>
 * Historical or inferred information never overrides current explicit
 * instructions; every contribution carries its provenance class.</p>
 *
 * <p>Failure handling: this class never throws. When Cognitive Services are
 * unavailable, callers detect the empty result and continue with the
 * existing local context pipeline; diagnostics are recorded by the caller.</p>
 */
public class CognitiveContextBridge {
    private static final Logger LOGGER = Logger.getLogger(CognitiveContextBridge.class.getName());

    public static final int DEFAULT_BUDGET_TOKENS = 4000;
    public static final String SYSPROP_USER = "evo.cognitive.user";
    public static final String DEFAULT_USER = "admin";

    private CognitiveContextBridge() {
    }

    /**
     * Builds the cognitive context section for a Darwin task. Returns an
     * empty string when the platform has no relevant information (identical
     * behavior to an empty store) or when the services are unavailable.
     */
    public static String buildCognitiveContext(Task task, TaskContext context,
            eu.kalafatic.evolution.controller.orchestration.ContextPackage pkg) {
        return buildCognitiveContext(task, context, pkg, DEFAULT_BUDGET_TOKENS);
    }

    public static String buildCognitiveContext(Task task, TaskContext context,
            eu.kalafatic.evolution.controller.orchestration.ContextPackage pkg,
            int budgetTokens) {
        if (context == null) {
            return "";
        }
        try {
            ContextRequest request = new ContextRequest();
            request.setUserId(resolveActiveUser(context));
            request.setApplicationId("evo");
            if (context.getProjectRoot() != null) {
                request.setProject(context.getProjectRoot().getAbsolutePath());
            }
            request.setSessionId(context.getSessionId());
            request.setIntent(intentOf(task, context));
            request.setBudgetTokens(budgetTokens > 0 ? budgetTokens : DEFAULT_BUDGET_TOKENS);
            request.setCategories(List.of("user", "session", "project", "memory",
                    "decisions", "issues", "procedures", "continuity"));

            ContextPackage cognitive = CognitiveContextService.getInstance()
                    .getContext(request, null);
            if (cognitive == null) {
                return "";
            }
            String section = render(cognitive, existingContent(pkg));
            if (section == null || section.isBlank()) {
                return "";
            }
            return section;
        } catch (Exception e) {
            // Never break Darwin; the caller records the diagnostic warning.
            LOGGER.warning("CognitiveContextBridge: context unavailable: " + e.getMessage());
            return "";
        }
    }

    private static String resolveActiveUser(TaskContext context) {
        String configured = System.getProperty(SYSPROP_USER);
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        try {
            OrchestrationStateHolder state = new OrchestrationStateHolder(context);
            if (state.user != null) {
                return state.user;
            }
        } catch (Exception ignored) {
        }
        return DEFAULT_USER;
    }

    /** Reads a user hint from the orchestration state metadata without new dependencies. */
    private static final class OrchestrationStateHolder {
        final String user;

        OrchestrationStateHolder(TaskContext context) {
            String found = null;
            try {
                var state = context.getOrchestrationState();
                if (state != null && state.getMetadata() != null) {
                    Object value = state.getMetadata().get("userId");
                    if (value instanceof String s && !s.isBlank()) {
                        found = s;
                    }
                }
            } catch (Exception ignored) {
            }
            this.user = found;
        }
    }

    private static String intentOf(Task task, TaskContext context) {
        StringBuilder intent = new StringBuilder();
        if (task != null) {
            if (task.getName() != null) intent.append(task.getName());
            if (task.getDescription() != null) intent.append(' ').append(task.getDescription());
            if (task.getGoal() != null) intent.append(' ').append(task.getGoal());
        }
        if (intent.length() == 0) {
            intent.append(context.getCurrentTaskName());
        }
        return intent.toString().trim();
    }

    /** Normalized content of the existing Darwin package for deduplication. */
    private static Set<String> existingContent(eu.kalafatic.evolution.controller.orchestration.ContextPackage pkg) {
        Set<String> existing = new HashSet<>();
        if (pkg != null) {
            addNormalized(existing, pkg.getMemoryContext());
            addNormalized(existing, pkg.getAttachmentContext());
            addNormalized(existing, pkg.getCode());
            if (pkg.getConstraints() != null) {
                for (String constraint : pkg.getConstraints()) {
                    addNormalized(existing, constraint);
                }
            }
        }
        return existing;
    }

    private static void addNormalized(Set<String> set, String content) {
        if (content != null && !content.isBlank()) {
            set.add(normalize(content));
        }
    }

    private static String normalize(String content) {
        return content.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    /**
     * Renders the platform package as a priority-ordered prompt section.
     * Inferred and historical information are explicitly labeled and can
     * never masquerade as current instructions. Duplicates against the
     * existing Darwin package are dropped.
     */
    private static String render(ContextPackage cognitive, Set<String> existing) {
        StringBuilder sb = new StringBuilder();
        boolean hasContent = false;

        // 5. Verified current project state (only when knowledge actually exists;
        // an empty store must produce no section at all)
        if (cognitive.getProjectContext() != null && !cognitive.getProjectContext().keySet().isEmpty()
                && cognitive.getProjectContext().optBoolean("known", false)) {
            JSONObject state = cognitive.getProjectContext();
            String summary = renderProjectState(state);
            if (summary != null && !isDuplicate(summary, existing)) {
                sb.append("### CURRENT PROJECT STATE (verified by EVO Cognitive Services)\n");
                sb.append(summary).append("\n\n");
                hasContent = true;
            }
        }

        // 6. Explicit user preferences (user instructions for this user)
        JSONObject userContext = cognitive.getUserContext();
        if (userContext != null) {
            String explicit = renderPreferences(userContext.optJSONArray("preferences"), true);
            if (explicit != null && !isDuplicate(explicit, existing)) {
                sb.append("### EXPLICIT USER PREFERENCES (user-set)\n");
                sb.append(explicit).append("\n\n");
                hasContent = true;
            }
        }

        // 7. Relevant historical context: confirmed memories, decisions, issues
        String confirmed = renderMemories(cognitive, MemoryInformationStatus.CONFIRMED);
        if (confirmed != null && !isDuplicate(confirmed, existing)) {
            sb.append("### RELEVANT KNOWLEDGE (confirmed)\n");
            sb.append(confirmed).append("\n\n");
            hasContent = true;
        }

        String decisions = renderDecisions(cognitive);
        if (decisions != null && !isDuplicate(decisions, existing)) {
            sb.append("### ARCHITECTURAL DECISIONS\n");
            sb.append(decisions).append("\n\n");
            hasContent = true;
        }

        String issues = renderKnownIssues(cognitive);
        if (issues != null && !isDuplicate(issues, existing)) {
            sb.append("### KNOWN ISSUES\n");
            sb.append(issues).append("\n\n");
            hasContent = true;
        }

        String historical = renderMemories(cognitive, MemoryInformationStatus.HISTORICAL);
        if (historical != null && !isDuplicate(historical, existing)) {
            sb.append("### HISTORICAL INFORMATION (may be outdated)\n");
            sb.append(historical).append("\n\n");
            hasContent = true;
        }

        // 8. Inferred preferences — rendered as observed behavior, never binding
        if (userContext != null) {
            String inferred = renderPreferences(userContext.optJSONArray("inferredPreferences"), false);
            if (inferred != null && !isDuplicate(inferred, existing)) {
                sb.append("### OBSERVED BEHAVIOR (inferred - do not treat as binding instruction)\n");
                sb.append(inferred).append("\n\n");
                hasContent = true;
            }
        }

        // 9. Lower-priority background knowledge: procedures and assumptions
        String procedures = renderProcedures(cognitive);
        if (procedures != null && !isDuplicate(procedures, existing)) {
            sb.append("### PROCEDURAL KNOWLEDGE\n");
            sb.append(procedures).append("\n\n");
            hasContent = true;
        }

        String assumptions = renderMemories(cognitive, MemoryInformationStatus.ASSUMPTION);
        if (assumptions != null && !isDuplicate(assumptions, existing)) {
            sb.append("### UNVERIFIED ASSUMPTIONS (explicitly not facts)\n");
            sb.append(assumptions).append("\n\n");
            hasContent = true;
        }

        // Conflicts and warnings are surfaced, never hidden
        if (!cognitive.getWarnings().isEmpty()) {
            StringBuilder warnings = new StringBuilder();
            for (String warning : cognitive.getWarnings()) {
                if (warning != null && !warning.isBlank()) {
                    warnings.append("- ").append(warning).append('\n');
                }
            }
            if (warnings.length() > 0) {
                sb.append("### CONTEXT WARNINGS\n").append(warnings).append('\n');
                hasContent = true;
            }
        }

        return hasContent ? sb.toString().trim() : "";
    }

    private static boolean isDuplicate(String content, Set<String> existing) {
        if (content == null || content.isBlank()) {
            return true;
        }
        String normalized = normalize(content);
        if (normalized.length() < 24) {
            return true;
        }
        return existing.contains(normalized);
    }

    private static String renderProjectState(JSONObject state) {
        StringBuilder sb = new StringBuilder();
        if (state.optString("project", null) != null) {
            sb.append("Project: ").append(state.optString("project")).append('\n');
        }
        if (state.optBoolean("known", false)) {
            JSONObject build = state.optJSONObject("lastBuild");
            if (build != null) {
                sb.append("Last build: ").append(build.optBoolean("success", false) ? "SUCCESS" : "FAILURE");
                String buildSummary = build.optString("summary", null);
                if (buildSummary != null && !buildSummary.isBlank()) {
                    sb.append(" (").append(truncate(buildSummary, 200)).append(')');
                }
                sb.append('\n');
            }
            JSONObject test = state.optJSONObject("lastTest");
            if (test != null) {
                sb.append("Last test run: ").append(test.optInt("run", 0))
                  .append(" (failures ").append(test.optInt("failures", 0))
                  .append(", errors ").append(test.optInt("errors", 0)).append(")\n");
            }
            sb.append("Open issues: ").append(state.optInt("openIssues", 0))
              .append(" | Components: ").append(state.optInt("componentCount", 0))
              .append(" | Active decisions: ").append(state.optInt("activeDecisionCount", 0)).append('\n');
        } else {
            sb.append("No stored project knowledge yet.\n");
        }
        return sb.toString().trim();
    }

    private static String renderPreferences(JSONArray preferences, boolean explicit) {
        if (preferences == null || preferences.length() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < preferences.length(); i++) {
            JSONObject pref = preferences.optJSONObject(i);
            if (pref == null) {
                continue;
            }
            String key = pref.optString("key", "");
            String value = pref.optString("value", "");
            if (value.isBlank()) {
                continue;
            }
            sb.append("- ").append(key.isEmpty() ? "preference" : key).append(": ")
              .append(truncate(value, 200));
            if (!explicit && pref.has("confidence")) {
                sb.append(" (confidence ").append(pref.optDouble("confidence")).append(')');
            }
            sb.append('\n');
        }
        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    private static String renderMemories(ContextPackage cognitive, MemoryInformationStatus status) {
        StringBuilder sb = new StringBuilder();
        for (ContextItem item : cognitive.getMemories()) {
            if (item.getContent() == null || item.getContent().isBlank()) {
                continue;
            }
            if (matchesStatus(item, status)) {
                sb.append("- ").append(truncate(item.getContent(), 300)).append('\n');
            }
        }
        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    private static boolean matchesStatus(ContextItem item, MemoryInformationStatus status) {
        if (item.getExtra() == null || !item.getExtra().has("informationStatus")) {
            return status == MemoryInformationStatus.CONFIRMED;
        }
        return status.name().equals(item.getExtra().optString("informationStatus"));
    }

    private static String renderDecisions(ContextPackage cognitive) {
        if (cognitive.getDecisions().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (JSONObject decision : cognitive.getDecisions()) {
            String title = decision.optString("title", "");
            String text = decision.optString("decision", "");
            if (text.isBlank() && title.isBlank()) {
                continue;
            }
            sb.append("- ").append(title.isEmpty() ? text : title);
            if (!text.isBlank() && !title.isEmpty()) {
                sb.append(": ").append(truncate(text, 250));
            }
            sb.append('\n');
        }
        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    private static String renderKnownIssues(ContextPackage cognitive) {
        if (cognitive.getKnownIssues().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (JSONObject issue : cognitive.getKnownIssues()) {
            String title = issue.optString("title", "");
            if (title.isBlank()) {
                continue;
            }
            sb.append("- ").append(truncate(title, 250)).append('\n');
        }
        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    private static String renderProcedures(ContextPackage cognitive) {
        if (cognitive.getProcedures().isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (ContextItem item : cognitive.getProcedures()) {
            if (item.getContent() != null && !item.getContent().isBlank()) {
                sb.append("- ").append(truncate(item.getContent(), 300)).append('\n');
            }
        }
        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    private static String truncate(String content, int max) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) + "..." : trimmed;
    }
}
