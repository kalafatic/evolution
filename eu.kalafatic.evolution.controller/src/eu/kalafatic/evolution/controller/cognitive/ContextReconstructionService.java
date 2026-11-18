package eu.kalafatic.evolution.controller.cognitive;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONObject;

import eu.kalafatic.evolution.controller.cognitive.model.ContextAssessment;
import eu.kalafatic.evolution.controller.cognitive.model.ContextItem;
import eu.kalafatic.evolution.controller.cognitive.model.ContextPackage;
import eu.kalafatic.evolution.controller.cognitive.model.ContextRequest;
import eu.kalafatic.evolution.controller.cognitive.model.ContextSource;
import eu.kalafatic.evolution.controller.cognitive.model.ConversationRecord;
import eu.kalafatic.evolution.controller.cognitive.model.SessionDescriptor;
import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryRelationship;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.controller.memory.MemoryValidity;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;
import eu.kalafatic.evolution.servers.service.ValidatedPrincipal;
import eu.kalafatic.evolution.servers.validation.ValidationPolicyService;
import eu.kalafatic.evolution.servers.validation.ValidationResult;

/**
 * The context reconstruction engine: builds a budgeted, provenance-tagged
 * ContextPackage for a new or continuing session.
 *
 * <p>Pipeline: session resolution -> user context -> conversation history ->
 * memory retrieval -> decisions/issues -> project state -> conflict
 * detection -> freshness -> ranking -> dedup -> token budget -> assessment.
 * Explicit user preferences take precedence over inferred ones; a verified
 * current project state takes precedence over outdated history;
 * contradictions are surfaced, never silently dropped.</p>
 */
public class ContextReconstructionService {
    private static final Logger LOGGER = Logger.getLogger(ContextReconstructionService.class.getName());

    public static final String CATEGORY_USER = "user";
    public static final String CATEGORY_SESSION = "session";
    public static final String CATEGORY_PROJECT = "project";
    public static final String CATEGORY_MEMORY = "memory";
    public static final String CATEGORY_DECISIONS = "decisions";
    public static final String CATEGORY_ISSUES = "issues";
    public static final String CATEGORY_PROCEDURES = "procedures";
    public static final String CATEGORY_CONTINUITY = "continuity";
    public static final String CATEGORY_REPOSITORY = "repository";

    private static final long TTL_FACT_MS = 90L * 24 * 3600 * 1000;
    private static final long TTL_PROJECT_MS = 30L * 24 * 3600 * 1000;
    private static final long TTL_PROCEDURE_MS = 180L * 24 * 3600 * 1000;
    private static final long TTL_PREFERENCE_MS = 365L * 24 * 3600 * 1000;

    private static ContextReconstructionService instance;

    private final MemoryService memoryService;
    private final ConversationHistoryService conversationHistoryService;
    private final SessionContinuityService sessionContinuityService;
    private final UserContextService userContextService;
    private final ProjectKnowledgeService projectKnowledgeService;
    private final TokenEstimationService tokenEstimationService;
    private final ValidationPolicyService validationPolicyService;

    private ContextReconstructionService() {
        this.memoryService = MemoryService.getInstance();
        this.conversationHistoryService = ConversationHistoryService.getInstance();
        this.sessionContinuityService = SessionContinuityService.getInstance();
        this.userContextService = UserContextService.getInstance();
        this.projectKnowledgeService = ProjectKnowledgeService.getInstance();
        this.tokenEstimationService = TokenEstimationService.getInstance();
        this.validationPolicyService = ValidationPolicyService.getInstance();
    }

    public static synchronized ContextReconstructionService getInstance() {
        if (instance == null) {
            instance = new ContextReconstructionService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    /**
     * Reconstructs context for the given request. The principal (when
     * present) is authoritative for identity; request params are hints.
     */
    public ContextPackage reconstruct(ContextRequest request, ValidatedPrincipal principal) {
        long start = System.currentTimeMillis();
        ContextPackage pkg = new ContextPackage();
        pkg.setContextId(SessionIdService.newContextId());
        pkg.setBudgetTokens(request.getBudgetTokens() > 0 ? request.getBudgetTokens() : 12000);

        String username = userContextService.resolveUsername(principal, request.getUserId());
        String applicationId = principal != null ? principal.getApplicationId() : request.getApplicationId();
        String projectRef = request.getProject();
        String sessionId = request.getSessionId();

        List<ContextItem> candidates = new ArrayList<>();
        List<ContextSource> sources = new ArrayList<>();

        // 1. Session resolution (continuity)
        SessionDescriptor session = resolveSession(sessionId, username, applicationId, projectRef);
        pkg.setSessionContext(buildSessionContext(session, request));
        if (session != null && request.wantsCategory(CATEGORY_CONTINUITY)) {
            JSONObject continuity = new JSONObject();
            JSONObject snapshot = sessionContinuityService.getLatestSnapshot(session.getId());
            if (snapshot != null) {
                continuity.put("resumedFrom", snapshot.optString("snapshotId"));
            }
            continuity.put("gaps", new org.json.JSONArray());
            pkg.setContinuity(continuity);
            sources.add(new ContextSource("SESSION", session.getId(), "session continuity state", 1.0, 0));
        }

        // 2. User context
        if (request.wantsCategory(CATEGORY_USER)) {
            JSONObject userContext = userContextService.buildUserContext(username, projectRef, sources);
            pkg.setUserContext(userContext);
            candidates.add(newItem("USER_PROFILE", userContext.toString(), "USER", 1.0, 0, false));
        }

        // 3. Conversation history (recent)
        if (request.wantsCategory(CATEGORY_SESSION) && session != null) {
            String conversationId = session.getId();
            List<ConversationRecord> recent = conversationHistoryService.list(conversationId, 0, 30);
            if (!recent.isEmpty()) {
                StringBuilder history = new StringBuilder();
                for (ConversationRecord r : recent) {
                    history.append(r.getRole()).append(": ").append(r.getContent()).append('\n');
                }
                candidates.add(newItem("CONVERSATION_HISTORY", history.toString(), "SESSION", 1.0, 0, false));
                sources.add(new ContextSource("CONVERSATION", conversationId, "recent conversation history", 1.0, 0));
            }
        }

        // 4. Memory retrieval with relevance ranking + conflict detection
        if (request.wantsCategory(CATEGORY_MEMORY)) {
            gatherMemory(candidates, sources, request, username, applicationId, projectRef, sessionId, pkg);
        }

        // 5. Decisions
        if (request.wantsCategory(CATEGORY_DECISIONS) && projectRef != null) {
            List<JSONObject> decisions = projectKnowledgeService.searchDecisions(projectRef, request.getIntent(), "ACCEPTED", 15);
            for (JSONObject decision : decisions) {
                pkg.getDecisions().add(decision);
                candidates.add(newItem("DECISION", decision.optString("title") + ": " + decision.optString("decision"),
                        "DECISION", 1.0, 0, false));
                sources.add(new ContextSource("DECISION", decision.optString("decisionId"), "architectural decision", 1.0, 0));
            }
        }

        // 6. Known issues
        if (request.wantsCategory(CATEGORY_ISSUES) && projectRef != null) {
            List<JSONObject> issues = projectKnowledgeService.listKnownIssues(projectRef, null, 15);
            for (JSONObject issue : issues) {
                pkg.getKnownIssues().add(issue);
                candidates.add(newItem("KNOWN_ISSUE", issue.optString("title") + ": " + issue.optString("description"),
                        "ISSUE", 1.0, 0, false));
                sources.add(new ContextSource("KNOWN_ISSUE", issue.optString("title"), "known issue", 1.0, 0));
            }
        }

        // 7. Project state
        if (request.wantsCategory(CATEGORY_PROJECT) && projectRef != null) {
            JSONObject state = projectKnowledgeService.getState(projectRef);
            pkg.setProjectContext(state);
            candidates.add(newItem("PROJECT_STATE", state.toString(), "PROJECT", 1.0, 0, false));
            sources.add(new ContextSource("PROJECT", projectRef, "project state", 1.0, 0));
            if (!state.optBoolean("known", false)) {
                pkg.addMissing("no stored knowledge for project " + projectRef);
            }
        }

        // 8. Budget optimization: rank, dedup, trim
        List<ContextItem> selected = optimizeBudget(candidates, request.getBudgetTokens());
        pkg.setMemories(selected.stream()
                .filter(i -> isMemoryCategory(i.getCategory()))
                .collect(Collectors.toList()));
        pkg.setProcedures(selected.stream()
                .filter(i -> "PROCEDURE".equals(i.getCategory()))
                .collect(Collectors.toList()));

        // 9. Provenance + measured token estimate
        pkg.setSources(sources);
        pkg.setTokenEstimate(measure(pkg));
        pkg.setFreshness(start);
        pkg.setConfidence(computeConfidence(pkg));

        // 9b. Final budget enforcement: trim lowest-relevance items until the
        // assembled package fits. When trimming is not enough, the overflow
        // is reported explicitly - never returned silently.
        trimToBudget(pkg);

        // 10. Assessment
        pkg.setAssessment(assess(request, pkg, candidates.size(), selected.size()));

        publishEvent(session != null ? session.getId() : (sessionId != null ? sessionId : null));
        return pkg;
    }

    // ------------------------------------------------------------------
    // Memory gathering
    // ------------------------------------------------------------------

    private void gatherMemory(List<ContextItem> candidates, List<ContextSource> sources,
            ContextRequest request, String username, String applicationId, String projectRef,
            String sessionId, ContextPackage pkg) {
        List<MemoryEntry> entries = memoryService.search(
                request.getIntent(), null, null, projectRef, 60);
        if (entries.isEmpty()) {
            // No intent match: fall back to recent active entries in scope
            entries = memoryService.getAllEntries().stream()
                    .filter(e -> e.getValidity() == MemoryValidity.VALID)
                    .filter(e -> inScope(e, username, applicationId, projectRef, sessionId))
                    .sorted(Comparator.comparingLong(MemoryEntry::getUpdatedAt).reversed())
                    .limit(40)
                    .collect(Collectors.toList());
        }
        for (MemoryEntry entry : entries) {
            if (entry.getValidity() == MemoryValidity.RETRACTED) {
                continue;
            }
            if (!inScope(entry, username, applicationId, projectRef, sessionId)) {
                continue;
            }
            if (entry.getMetadata().containsKey("sensitive") && "true".equalsIgnoreCase(entry.getMetadata().get("sensitive"))) {
                continue; // sensitive entries are excluded unless memory.read.sensitive scope
            }
            boolean stale = isStale(entry);
            double relevance = score(entry, request.getIntent(), stale);
            ContextItem item = newItem("MEMORY", entry.getContent(), "MEMORY", entry.getConfidence(),
                    entry.getFreshnessMs(), stale);
            item.setId(entry.getId());
            item.setExtra(new JSONObject()
                    .put("memoryType", entry.getType() != null ? entry.getType().name() : null)
                    .put("informationStatus", entry.getInformationStatus().name())
                    .put("validity", entry.getValidity().name())
                    .put("relevance", relevance)
                    .put("scope", entry.getScope() != null ? entry.getScope().name() : null));
            candidates.add(item);
            sources.add(new ContextSource("MEMORY", entry.getId(),
                    entry.getType() != null ? entry.getType().name() : "MEMORY",
                    entry.getConfidence(), entry.getFreshnessMs()));
            if (stale) {
                pkg.addWarning("memory entry " + entry.getId() + " is stale (last verified "
                        + entry.getFreshnessMs() / 86400000L + " days ago)");
            }
        }

        // Conflicts: CONTRADICTS relationships are surfaced, never hidden
        List<MemoryRelationship> conflicts = new ArrayList<>();
        for (MemoryEntry entry : entries) {
            conflicts.addAll(memoryService.findConflicts(entry.getId()));
        }
        Map<String, Integer> seen = new HashMap<>();
        for (MemoryRelationship conflict : conflicts) {
            String key = conflict.getFromEntryId() + "|" + conflict.getToEntryId();
            if (seen.putIfAbsent(key, 1) != null) {
                continue;
            }
            MemoryEntry from = memoryService.getEntryById(conflict.getFromEntryId());
            MemoryEntry to = memoryService.getEntryById(conflict.getToEntryId());
            pkg.addWarning("conflicting memory entries: " + conflict.getFromEntryId()
                    + " <-> " + conflict.getToEntryId()
                    + (from != null && to != null ? " (" + entryLabel(from) + " vs " + entryLabel(to) + ")" : ""));
        }
    }

    private String entryLabel(MemoryEntry entry) {
        String content = entry.getContent();
        return content != null && content.length() > 40 ? content.substring(0, 40) + "..." : String.valueOf(content);
    }

    private boolean inScope(MemoryEntry entry, String username, String applicationId, String projectRef, String sessionId) {
        MemoryScope scope = entry.getScope();
        if (scope == null) {
            return true;
        }
        switch (scope) {
            case USER:
                return username == null || entry.getUserId() == null || username.equals(entry.getUserId());
            case PROJECT:
                return projectRef == null || entry.getProjectRef() == null || projectRef.equals(entry.getProjectRef());
            case SESSION:
                // Session-scoped entries are tied to a specific session and
                // are reconstructable only by a request carrying that exact
                // session. Unbound entries remain accessible via memory
                // search/administration, never via context reconstruction.
                return sessionId != null && entry.getSessionRef() != null
                        && sessionId.equals(entry.getSessionRef());
            case TASK:
                return true;
            default:
                return true;
        }
    }

    private double score(MemoryEntry entry, String intent, boolean stale) {
        double score = 0.0;
        if (intent != null && entry.getContent() != null) {
            String lowerContent = entry.getContent().toLowerCase();
            for (String token : intent.toLowerCase().split("\\s+")) {
                if (token.length() > 2 && lowerContent.contains(token)) {
                    score += 0.15;
                }
            }
        }
        score += entry.getConfidence() * 0.4;
        score += entry.getImportance() != null ? entry.getImportance().getLevel() * 0.1 : 0.1;
        double freshnessFactor = Math.exp(-entry.getFreshnessMs() / (30.0 * 86400000.0));
        score += freshnessFactor * 0.3;
        if (stale) {
            score *= 0.5;
        }
        return Math.min(1.0, score);
    }

    private boolean isStale(MemoryEntry entry) {
        long ttl;
        switch (entry.getType() != null ? entry.getType() : MemoryType.FACT) {
            case PROJECT_CONTEXT:
            case PROJECT:
                ttl = TTL_PROJECT_MS;
                break;
            case PROCEDURAL:
            case SOLUTION:
            case EXPERIENCE:
            case RULE:
                ttl = TTL_PROCEDURE_MS;
                break;
            case PREFERENCE:
                ttl = TTL_PREFERENCE_MS;
                break;
            default:
                ttl = TTL_FACT_MS;
        }
        return entry.getFreshnessMs() > ttl;
    }

    private boolean isMemoryCategory(String category) {
        return "MEMORY".equals(category) || "PROCEDURE".equals(category);
    }

    // ------------------------------------------------------------------
    // Session resolution
    // ------------------------------------------------------------------

    private SessionDescriptor resolveSession(String sessionId, String username, String applicationId, String projectRef) {
        if (sessionId != null && !sessionId.isEmpty()) {
            SessionDescriptor existing = sessionContinuityService.resolve(sessionId);
            if (existing != null) {
                sessionContinuityService.touch(existing.getId());
                return existing;
            }
            // Register the ad-hoc id as a legacy alias of a new canonical session
            return sessionContinuityService.registerSession(SessionIdService.newSessionId(), sessionId,
                    SessionDescriptor.KIND_CONVERSATION, username, applicationId, projectRef, null, null);
        }
        return null;
    }

    private JSONObject buildSessionContext(SessionDescriptor session, ContextRequest request) {
        if (session == null) {
            return new JSONObject().put("sessionId", JSONObject.NULL);
        }
        JSONObject context = session.toJsonObject();
        org.json.JSONArray related = new org.json.JSONArray();
        for (JSONObject rel : sessionContinuityService.listRelationships(session.getId())) {
            related.put(rel);
        }
        context.put("relatedSessions", related);
        return context;
    }

    // ------------------------------------------------------------------
    // Budget, ranking, dedup, assessment
    // ------------------------------------------------------------------

    private ContextItem newItem(String category, String content, String sourceRef, double confidence,
            long freshness, boolean stale) {
        ContextItem item = new ContextItem();
        item.setCategory(category);
        item.setContent(content != null ? content : "");
        item.setSourceRef(sourceRef);
        item.setConfidence(confidence);
        item.setFreshness(freshness);
        item.setTokens(tokenEstimationService.estimateTokens(content));
        item.setStale(stale);
        return item;
    }

    private List<ContextItem> optimizeBudget(List<ContextItem> candidates, int budgetTokens) {
        List<ContextItem> deduped = deduplicate(candidates);
        // Essentials (identity/profile, project state) always enter first;
        // remaining items are ranked by confidence and only kept while the
        // budget allows.
        List<ContextItem> sorted = new ArrayList<>(deduped);
        sorted.sort(Comparator.comparingInt((ContextItem i) -> isEssential(i.getCategory()) ? 0 : 1)
                .thenComparing(Comparator.comparingDouble(ContextItem::getConfidence).reversed()));
        List<ContextItem> selected = new ArrayList<>();
        int used = 0;
        for (ContextItem item : sorted) {
            int cost = item.getTokens();
            if (!isEssential(item.getCategory()) && budgetTokens > 0
                    && used + cost > budgetTokens && !selected.isEmpty()) {
                continue;
            }
            selected.add(item);
            used += cost;
        }
        selected.sort(Comparator.comparingInt(ContextItem::getTokens).reversed());
        return selected;
    }

    private boolean isEssential(String category) {
        return "USER_PROFILE".equals(category) || "PROJECT_STATE".equals(category);
    }

    private List<ContextItem> deduplicate(List<ContextItem> candidates) {
        Map<String, ContextItem> unique = new HashMap<>();
        for (ContextItem item : candidates) {
            String normalized = normalize(item.getContent());
            if (normalized.isEmpty()) {
                continue;
            }
            ContextItem existing = unique.get(normalized);
            if (existing == null || item.getConfidence() > existing.getConfidence()) {
                unique.put(normalized, item);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private String normalize(String content) {
        if (content == null) {
            return "";
        }
        return content.toLowerCase().replaceAll("\\s+", " ").trim();
    }

    private int measure(ContextPackage pkg) {
        JSONObject json = pkg.toJsonObject();
        // Avoid recursion into assessment for the estimate
        json.remove("assessment");
        return tokenEstimationService.estimateTokens(json.toString());
    }

    private double computeConfidence(ContextPackage pkg) {
        if (pkg.getSources().isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (ContextSource source : pkg.getSources()) {
            sum += source.getConfidence();
        }
        double avg = sum / pkg.getSources().size();
        return Math.min(1.0, avg);
    }

    private ContextAssessment assess(ContextRequest request, ContextPackage pkg, int candidateCount, int selectedCount) {
        ContextQualityService qualityService = ContextQualityService.getInstance();
        return qualityService.assess(request, pkg, candidateCount, selectedCount);
    }

    // ------------------------------------------------------------------
    // Validation policy integration (domain completeness, opt-in)
    // ------------------------------------------------------------------

    /**
     * Runs the configurable domain validation against the assembled package.
     * With the default DISABLED mode these checks are skipped; WARN records
     * warnings; STRICT fails block the operation. Security checks are never
     * governed by this policy.
     */
    public List<ValidationResult> runDomainValidation(ContextRequest request, ContextPackage pkg) {
        List<ValidationResult> results = new ArrayList<>();

        ValidationResult categories = validationPolicyService.evaluate("CONTEXT.categoriesComplete", () -> {
            if (pkg.getContextId() == null) {
                return ValidationResult.failed("CONTEXT.categoriesComplete",
                        "Context package missing contextId", "WARN", false);
            }
            if (request.wantsCategory(CATEGORY_PROJECT) && request.getProject() == null) {
                return ValidationResult.failed("CONTEXT.categoriesComplete",
                        "Project category requested without a project reference", "WARN", false);
            }
            return ValidationResult.ok("CONTEXT.categoriesComplete");
        });
        results.add(categories);

        ValidationResult budget = validationPolicyService.evaluate("CONTEXT.budgetFits", () -> {
            if (pkg.getBudgetTokens() > 0 && pkg.getTokenEstimate() > pkg.getBudgetTokens()) {
                return ValidationResult.failed("CONTEXT.budgetFits",
                        "Context package exceeds token budget (" + pkg.getTokenEstimate()
                                + " > " + pkg.getBudgetTokens() + ")", "ERROR", false);
            }
            return ValidationResult.ok("CONTEXT.budgetFits");
        });
        results.add(budget);

        ValidationResult sources = validationPolicyService.evaluate("CONTEXT.sourcesAnnotated", () -> {
            if (pkg.getSources() == null || pkg.getSources().isEmpty()) {
                return ValidationResult.failed("CONTEXT.sourcesAnnotated",
                        "Context package has no source annotations", "WARN", false);
            }
            return ValidationResult.ok("CONTEXT.sourcesAnnotated");
        });
        results.add(sources);

        for (ValidationResult result : results) {
            if (result.isBlocking()) {
                throw new eu.kalafatic.evolution.servers.validation.ValidationException(
                        result.getRuleId(), result.getMessage());
            }
        }
        return results;
    }

    private void trimToBudget(ContextPackage pkg) {
        int budget = pkg.getBudgetTokens();
        if (budget <= 0) {
            return;
        }
        for (int iteration = 0; iteration < 40 && pkg.getTokenEstimate() > budget; iteration++) {
            ContextItem lowest = null;
            for (ContextItem item : pkg.getMemories()) {
                if (lowest == null || relevanceOf(item) < relevanceOf(lowest)) {
                    lowest = item;
                }
            }
            if (lowest != null) {
                pkg.getMemories().remove(lowest);
            } else if (!pkg.getProcedures().isEmpty()) {
                ContextItem lowestProcedure = null;
                for (ContextItem item : pkg.getProcedures()) {
                    if (lowestProcedure == null || relevanceOf(item) < relevanceOf(lowestProcedure)) {
                        lowestProcedure = item;
                    }
                }
                pkg.getProcedures().remove(lowestProcedure);
            } else {
                break;
            }
            pkg.setTokenEstimate(measure(pkg));
        }
        if (pkg.getTokenEstimate() > budget) {
            pkg.addWarning("context package exceeds token budget after trimming: "
                    + pkg.getTokenEstimate() + " > " + budget);
        }
    }

    private double relevanceOf(ContextItem item) {
        if (item.getExtra() != null && item.getExtra().has("relevance")) {
            return item.getExtra().optDouble("relevance", 0.0);
        }
        return item.getConfidence();
    }

    private void publishEvent(String sessionId) {
        try {
            if (sessionId == null) {
                return;
            }
            var container = SessionManager.getInstance().getSession(sessionId);
            if (container != null && container.getEventBus() != null) {
                container.getEventBus().publish(new RuntimeEvent(RuntimeEventType.CONTEXT_RECONSTRUCTED,
                        sessionId, "ContextReconstructionService", sessionId));
            }
        } catch (Exception ignored) {
        }
    }
}
