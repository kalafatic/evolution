package eu.kalafatic.evolution.controller.orchestration.mcp;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.cognitive.CognitiveContextService;
import eu.kalafatic.evolution.controller.cognitive.ConversationHistoryService;
import eu.kalafatic.evolution.controller.cognitive.PersonalizationService;
import eu.kalafatic.evolution.controller.cognitive.ProjectKnowledgeService;
import eu.kalafatic.evolution.controller.cognitive.SessionContinuityService;
import eu.kalafatic.evolution.controller.cognitive.model.ConversationRecord;
import eu.kalafatic.evolution.controller.cognitive.model.ContextPackage;
import eu.kalafatic.evolution.controller.cognitive.model.ContextRequest;
import eu.kalafatic.evolution.controller.cognitive.model.SessionDescriptor;
import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryRelationship;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemorySource;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.controller.memory.MemoryValidity;
import eu.kalafatic.evolution.servers.mcp.model.McpExecutionContext;
import eu.kalafatic.evolution.servers.mcp.model.Resource;
import eu.kalafatic.evolution.servers.mcp.model.Tool;
import eu.kalafatic.evolution.servers.mcp.resources.ResourceRegistry;
import eu.kalafatic.evolution.servers.mcp.resources.ResourceRegistry.ResourceProvider;
import eu.kalafatic.evolution.servers.mcp.tools.ToolRegistry;
import eu.kalafatic.evolution.servers.service.CognitiveScopes;
import eu.kalafatic.evolution.servers.service.ValidatedPrincipal;

/**
 * Cognitive Services MCP adapter. Thin, permission-aware facades over the
 * native cognitive services - this provider contains NO business logic.
 * Identity parameters in tool arguments are hints; the authenticated
 * bearer token establishes the principal, and every operation is scoped to
 * that principal's grants. All mutating operations are audited at the
 * dispatcher layer.
 */
public class CognitiveMcpToolProvider {
    private static final Logger LOGGER = Logger.getLogger(CognitiveMcpToolProvider.class.getName());

    private CognitiveMcpToolProvider() {
    }

    /** Registers all cognitive tools and their required scopes. */
    public static void registerCognitiveTools(ToolRegistry registry,
            eu.kalafatic.evolution.servers.mcp.server.JsonRpcDispatcher dispatcher) {
        if (registry == null) {
            return;
        }
        registerContextTools(registry);
        registerMemoryTools(registry);
        registerUserTools(registry);
        registerSessionTools(registry);
        registerDecisionTools(registry);
        registerProjectTools(registry);
        registerConversationTools(registry);

        if (dispatcher != null) {
            dispatcher.registerToolScope("context_get", CognitiveScopes.CONTEXT_GET);
            dispatcher.registerToolScope("context_search", CognitiveScopes.CONTEXT_SEARCH);
            dispatcher.registerToolScope("context_record", CognitiveScopes.CONTEXT_RECORD);
            dispatcher.registerToolScope("memory_store", CognitiveScopes.MEMORY_WRITE);
            dispatcher.registerToolScope("memory_update", CognitiveScopes.MEMORY_WRITE);
            dispatcher.registerToolScope("memory_retract", CognitiveScopes.MEMORY_WRITE);
            dispatcher.registerToolScope("memory_search", CognitiveScopes.MEMORY_READ);
            dispatcher.registerToolScope("user_get", CognitiveScopes.USER_READ);
            dispatcher.registerToolScope("user_preferences_get", CognitiveScopes.USER_PREFERENCES_READ);
            dispatcher.registerToolScope("user_preferences_update", CognitiveScopes.USER_PREFERENCES_WRITE);
            dispatcher.registerToolScope("session_create", CognitiveScopes.SESSION_CREATE);
            dispatcher.registerToolScope("session_get", CognitiveScopes.SESSION_READ);
            dispatcher.registerToolScope("session_search", CognitiveScopes.SESSION_READ);
            dispatcher.registerToolScope("session_snapshot", CognitiveScopes.SESSION_WRITE);
            dispatcher.registerToolScope("session_restore", CognitiveScopes.SESSION_WRITE);
            dispatcher.registerToolScope("decision_record", CognitiveScopes.DECISION_WRITE);
            dispatcher.registerToolScope("decision_search", CognitiveScopes.DECISION_READ);
            dispatcher.registerToolScope("decision_get", CognitiveScopes.DECISION_READ);
            dispatcher.registerToolScope("project_context_get", CognitiveScopes.PROJECT_READ);
            dispatcher.registerToolScope("project_state_get", CognitiveScopes.PROJECT_READ);
            dispatcher.registerToolScope("project_state_update", CognitiveScopes.PROJECT_WRITE);
            dispatcher.registerToolScope("project_knowledge_search", CognitiveScopes.PROJECT_READ);
            dispatcher.registerToolScope("conversation_history_get", CognitiveScopes.SESSION_READ);
            dispatcher.registerToolScope("conversation_history_append", CognitiveScopes.SESSION_WRITE);
        }
        LOGGER.info("[MCP] Cognitive Services tools registered.");
    }

    // ------------------------------------------------------------------
    // Principal handling: params never carry identity
    // ------------------------------------------------------------------

    private static ValidatedPrincipal principalOf(McpExecutionContext context) {
        return context != null ? context.getAuthenticatedPrincipal() : null;
    }

    /**
     * Resolves the effective user for an operation. The principal is
     * authoritative; a requested userId may only differ for ecosystem
     * admins or internal callers without a token.
     */
    private static String effectiveUser(ValidatedPrincipal principal, String requestedUserId) {
        if (principal == null) {
            return requestedUserId;
        }
        if (principal.isEcosystemAdmin() && requestedUserId != null && !requestedUserId.isBlank()) {
            return requestedUserId;
        }
        return principal.getUsername();
    }

    private static String effectiveApplication(ValidatedPrincipal principal, String requestedApplicationId) {
        if (principal == null) {
            return requestedApplicationId;
        }
        return principal.getApplicationId();
    }

    private static boolean isOwnSession(ValidatedPrincipal principal, SessionDescriptor descriptor) {
        if (principal == null || descriptor == null) {
            return true;
        }
        if (principal.hasScope(CognitiveScopes.SESSION_READ_ANY)) {
            return true;
        }
        // Sessions tagged with an application are visible only to that
        // application's token AND the owning user - the token's user is
        // never sufficient on its own.
        if (descriptor.getApplicationId() != null && !descriptor.getApplicationId().isBlank()) {
            return descriptor.getApplicationId().equals(principal.getApplicationId())
                    && (descriptor.getOwnerUserId() == null || descriptor.getOwnerUserId().isBlank()
                            || descriptor.getOwnerUserId().equals(principal.getUsername()));
        }
        // Unscoped (internal) sessions: owner user only.
        return descriptor.getOwnerUserId() == null || descriptor.getOwnerUserId().isBlank()
                || descriptor.getOwnerUserId().equals(principal.getUsername());
    }

    private static boolean canReadMemory(ValidatedPrincipal principal, MemoryEntry entry) {
        if (principal == null) {
            return true;
        }
        if (principal.hasScope(CognitiveScopes.MEMORY_READ_ANY)) {
            return true;
        }
        String entryUser = entry.getUserId();
        String entryApp = entry.getApplicationId();
        if (entryUser == null && entryApp == null) {
            return true;
        }
        if (entryApp != null && entryApp.equals(principal.getApplicationId())) {
            return true;
        }
        if (entryUser != null && entryUser.equals(principal.getUsername())) {
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Context tools
    // ------------------------------------------------------------------

    private static void registerContextTools(ToolRegistry registry) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> props = new HashMap<>();
        props.put("userId", Map.of("type", "string", "description", "Hint: target user (validated against token)"));
        props.put("applicationId", Map.of("type", "string", "description", "Hint: calling application (validated against token)"));
        props.put("project", Map.of("type", "string", "description", "Project path or reference"));
        props.put("sessionId", Map.of("type", "string", "description", "Optional session id for continuation"));
        props.put("taskId", Map.of("type", "string", "description", "Optional task id"));
        props.put("intent", Map.of("type", "string", "description", "Task description used for relevance ranking"));
        props.put("budgetTokens", Map.of("type", "integer", "description", "Token budget (default 12000)"));
        props.put("categories", Map.of("type", "array", "description", "Requested context categories"));
        props.put("timeWindow", Map.of("type", "object", "description", "Optional time window (from/to epoch millis)"));
        props.put("accessScope", Map.of("type", "array", "description", "Requested access scopes"));
        schema.put("properties", props);

        registry.register(new Tool("context_get",
                "Reconstructs relevant cognitive context (user, session, project, memory, decisions, issues) as a budgeted ContextPackage",
                schema),
                (args, context) -> {
                    ContextRequest request = requestFromJson(args);
                    ValidatedPrincipal principal = principalOf(context);
                    request.setUserId(effectiveUser(principal, request.getUserId()));
                    request.setApplicationId(effectiveApplication(principal, request.getApplicationId()));
                    ContextPackage pkg = CognitiveContextService.getInstance().getContext(request, principal);
                    return jsonResult(pkg.toJsonObject());
                });

        Map<String, Object> searchSchema = new HashMap<>();
        searchSchema.put("type", "object");
        searchSchema.put("properties", Map.of(
                "query", Map.of("type", "string", "description", "Search query"),
                "project", Map.of("type", "string", "description", "Optional project filter"),
                "types", Map.of("type", "array", "description", "Result kinds: memory, decisions, conversations"),
                "limit", Map.of("type", "integer", "description", "Maximum results")));
        registry.register(new Tool("context_search", "Searches memory, decisions and conversations in one call", searchSchema),
                (args, context) -> {
                    String query = text(args, "query");
                    String project = text(args, "project");
                    List<String> types = stringList(args, "types");
                    int limit = intArg(args, "limit", 25);
                    List<Object> results = CognitiveContextService.getInstance().searchContext(query, project, types, limit);
                    Map<String, Object> result = new HashMap<>();
                    result.put("count", results.size());
                    result.put("results", toSerializable(results));
                    return result;
                });

        Map<String, Object> recordSchema = new HashMap<>();
        recordSchema.put("type", "object");
        recordSchema.put("properties", Map.of(
                "sessionId", Map.of("type", "string", "description", "Optional session id"),
                "summary", Map.of("type", "string", "description", "Context snapshot summary"),
                "metadata", Map.of("type", "object", "description", "Additional metadata")));
        registry.register(new Tool("context_record", "Stores a ContextSnapshot reference for replay and audit", recordSchema),
                (args, context) -> {
                    ValidatedPrincipal principal = principalOf(context);
                    String requester = principal != null ? principal.getUsername() : "internal";
                    String contextId = eu.kalafatic.evolution.controller.cognitive.SessionIdService.newContextId();
                    String summary = text(args, "summary");
                    CognitiveContextService.getInstance().recordContext(contextId, "1.0", requester,
                            summary != null ? summary : "");
                    Map<String, Object> result = new HashMap<>();
                    result.put("contextId", contextId);
                    return result;
                });
    }

    private static ContextRequest requestFromJson(JsonNode args) {
        ContextRequest request = new ContextRequest();
        if (args == null) {
            return request;
        }
        if (args.hasNonNull("userId")) request.setUserId(args.get("userId").asText());
        if (args.hasNonNull("applicationId")) request.setApplicationId(args.get("applicationId").asText());
        if (args.hasNonNull("project")) request.setProject(args.get("project").asText());
        if (args.hasNonNull("sessionId")) request.setSessionId(args.get("sessionId").asText());
        if (args.hasNonNull("taskId")) request.setTaskId(args.get("taskId").asText());
        if (args.hasNonNull("intent")) request.setIntent(args.get("intent").asText());
        if (args.hasNonNull("budgetTokens")) request.setBudgetTokens(args.get("budgetTokens").asInt());
        request.setCategories(stringList(args, "categories"));
        request.setAccessScope(stringList(args, "accessScope"));
        if (args.hasNonNull("timeWindow")) {
            JsonNode window = args.get("timeWindow");
            if (window.hasNonNull("from")) request.setTimeWindowFrom(window.get("from").asLong());
            if (window.hasNonNull("to")) request.setTimeWindowTo(window.get("to").asLong());
        }
        return request;
    }

    // ------------------------------------------------------------------
    // Memory tools
    // ------------------------------------------------------------------

    private static void registerMemoryTools(ToolRegistry registry) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> props = new HashMap<>();
        props.put("content", Map.of("type", "string", "description", "Memory content"));
        props.put("type", Map.of("type", "string", "description", "Memory type (FACT/PREFERENCE/DECISION/RULE/CONSTRAINT/GOAL/PROJECT_CONTEXT/OBSERVATION/EXPERIENCE/SOLUTION/WORKING/EPISODIC/SEMANTIC/PROCEDURAL/PROJECT/RELATIONAL)"));
        props.put("scope", Map.of("type", "string", "description", "Scope (USER/PROJECT/SESSION/TASK/MODE)"));
        props.put("project", Map.of("type", "string", "description", "Project reference"));
        props.put("sessionId", Map.of("type", "string", "description", "Cognitive session the entry belongs to (validated against the token)"));
        props.put("importance", Map.of("type", "string", "description", "Importance (LOW..CRITICAL)"));
        props.put("confidence", Map.of("type", "number", "description", "Confidence 0..1"));
        props.put("source", Map.of("type", "string", "description", "Source (USER/SESSION/INFERENCE/TOOL/...)"));
        props.put("associatedId", Map.of("type", "string", "description", "Associated entity id"));
        props.put("metadata", Map.of("type", "object", "description", "Additional metadata"));
        props.put("relationships", Map.of("type", "array", "description", "Memory relationships"));
        schema.put("properties", props);
        schema.put("required", List.of("content"));

        registry.register(new Tool("memory_store", "Stores a memory record through the unified memory API", schema),
                (args, context) -> {
                    ValidatedPrincipal principal = principalOf(context);
                    String content = text(args, "content");
                    if (content == null || content.isBlank()) {
                        throw new IllegalArgumentException("content is required");
                    }
                    MemoryEntry entry = new MemoryEntry();
                    entry.setContent(content);
                    entry.setType(enumArg(args, "type", MemoryType.class, MemoryType.FACT));
                    entry.setScope(enumArg(args, "scope", MemoryScope.class, MemoryScope.USER));
                    entry.setImportance(enumArg(args, "importance", MemoryImportance.class, MemoryImportance.HIGH));
                    entry.setSource(enumArg(args, "source", MemorySource.class, MemorySource.USER));
                    entry.setConfidence(numberArg(args, "confidence", 1.0));
                    entry.setProjectRef(text(args, "project"));
                    entry.setAssociatedId(text(args, "associatedId"));
                    if (args != null && args.hasNonNull("metadata")) {
                        JsonNode meta = args.get("metadata");
                        meta.fieldNames().forEachRemaining(k -> entry.getMetadata().put(k, meta.get(k).asText()));
                    }
                    // Identity always comes from the token, never from params
                    if (principal != null) {
                        entry.setUserId(principal.getUsername());
                        entry.setApplicationId(principal.getApplicationId());
                    }
                    // Session binding: an explicitly supplied cognitive session
                    // id must belong to the caller; never the ephemeral MCP
                    // request context id.
                    String sessionId = text(args, "sessionId");
                    if (sessionId != null && !sessionId.isBlank()) {
                        SessionDescriptor descriptor = SessionContinuityService.getInstance().resolve(sessionId);
                        if (descriptor == null) {
                            throw new IllegalArgumentException("Unknown session: " + sessionId);
                        }
                        if (!isOwnSession(principal, descriptor)) {
                            throw new SecurityException("Access denied: session belongs to another user or application");
                        }
                        entry.setSessionRef(descriptor.getId());
                    }
                    MemoryEntry stored = CognitiveContextService.getInstance().storeMemory(entry);
                    Map<String, Object> result = new HashMap<>();
                    result.put("entryId", stored.getId());
                    result.put("version", stored.getVersion());
                    return result;
                });

        Map<String, Object> searchSchema = new HashMap<>();
        searchSchema.put("type", "object");
        Map<String, Object> searchProps = new HashMap<>();
        searchProps.put("query", Map.of("type", "string", "description", "Search query"));
        searchProps.put("type", Map.of("type", "string", "description", "Memory type filter"));
        searchProps.put("scope", Map.of("type", "string", "description", "Scope filter"));
        searchProps.put("project", Map.of("type", "string", "description", "Project filter"));
        searchProps.put("minConfidence", Map.of("type", "number", "description", "Minimum confidence"));
        searchProps.put("limit", Map.of("type", "integer", "description", "Maximum results"));
        searchSchema.put("properties", searchProps);

        registry.register(new Tool("memory_search", "Searches memory records (FTS with fallback)", searchSchema),
                (args, context) -> {
                    ValidatedPrincipal principal = principalOf(context);
                    String query = text(args, "query");
                    MemoryType type = enumArg(args, "type", MemoryType.class, null);
                    MemoryScope scope = enumArg(args, "scope", MemoryScope.class, null);
                    String project = text(args, "project");
                    double minConfidence = numberArg(args, "minConfidence", 0.0);
                    int limit = intArg(args, "limit", 25);
                    List<MemoryEntry> entries = MemoryService.getInstance().search(query, scope, type, project, limit);
                    List<JSONObject> results = new ArrayList<>();
                    for (MemoryEntry entry : entries) {
                        if (entry.getConfidence() < minConfidence) {
                            continue;
                        }
                        if (!canReadMemory(principal, entry)) {
                            continue;
                        }
                        JSONObject item = entry.toJsonObject();
                        item.put("freshnessMs", entry.getFreshnessMs());
                        results.add(item);
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("count", results.size());
                    result.put("results", toSerializable(results));
                    return result;
                });

        Map<String, Object> updateSchema = new HashMap<>();
        updateSchema.put("type", "object");
        Map<String, Object> updateProps = new HashMap<>();
        updateProps.put("entryId", Map.of("type", "string", "description", "Entry id"));
        updateProps.put("content", Map.of("type", "string", "description", "New content"));
        updateProps.put("confidence", Map.of("type", "number", "description", "New confidence"));
        updateProps.put("importance", Map.of("type", "string", "description", "New importance"));
        updateProps.put("validity", Map.of("type", "string", "description", "New validity"));
        updateSchema.put("properties", updateProps);
        updateSchema.put("required", List.of("entryId"));

        registry.register(new Tool("memory_update", "Updates a memory record (creates a new version)", updateSchema),
                (args, context) -> {
                    String entryId = text(args, "entryId");
                    MemoryEntry entry = MemoryService.getInstance().getEntryById(entryId);
                    if (entry == null) {
                        throw new java.util.NoSuchElementException("Memory entry not found: " + entryId);
                    }
                    if (!canReadMemory(principalOf(context), entry)) {
                        throw new SecurityException("Access denied: memory entry belongs to another user or application");
                    }
                    if (args != null && args.hasNonNull("content")) entry.setContent(args.get("content").asText());
                    if (args != null && args.hasNonNull("confidence")) entry.setConfidence(args.get("confidence").asDouble());
                    if (args != null && args.hasNonNull("importance")) {
                        entry.setImportance(enumArg(args, "importance", MemoryImportance.class, entry.getImportance()));
                    }
                    if (args != null && args.hasNonNull("validity")) {
                        entry.setValidity(MemoryValidity.fromString(args.get("validity").asText()));
                    }
                    MemoryEntry updated = MemoryService.getInstance().updateEntry(entry);
                    Map<String, Object> result = new HashMap<>();
                    result.put("entryId", updated.getId());
                    result.put("version", updated.getVersion());
                    return result;
                });

        Map<String, Object> retractSchema = new HashMap<>();
        retractSchema.put("type", "object");
        Map<String, Object> retractProps = new HashMap<>();
        retractProps.put("entryId", Map.of("type", "string", "description", "Entry id"));
        retractProps.put("reason", Map.of("type", "string", "description", "Retraction reason"));
        retractSchema.put("properties", retractProps);
        retractSchema.put("required", List.of("entryId"));

        registry.register(new Tool("memory_retract", "Retracts a memory record (auditable, never hard-deleted)", retractSchema),
                (args, context) -> {
                    String entryId = text(args, "entryId");
                    MemoryEntry entry = MemoryService.getInstance().getEntryById(entryId);
                    if (entry == null) {
                        throw new java.util.NoSuchElementException("Memory entry not found: " + entryId);
                    }
                    if (!canReadMemory(principalOf(context), entry)) {
                        throw new SecurityException("Access denied: memory entry belongs to another user or application");
                    }
                    String reason = text(args, "reason");
                    ValidatedPrincipal principal = principalOf(context);
                    MemoryEntry retracted = CognitiveContextService.getInstance().retractMemory(entryId, reason,
                            principal != null ? principal.getUsername() : null);
                    Map<String, Object> result = new HashMap<>();
                    result.put("entryId", retracted.getId());
                    result.put("validity", retracted.getValidity().name());
                    return result;
                });
    }

    // ------------------------------------------------------------------
    // User tools
    // ------------------------------------------------------------------

    private static void registerUserTools(ToolRegistry registry) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of("userId", Map.of("type", "string", "description", "Hint: target user (self unless authorized)")));
        registry.register(new Tool("user_get", "Returns the user profile and preferences (scope-filtered)", schema),
                (args, context) -> {
                    ValidatedPrincipal principal = principalOf(context);
                    String username = effectiveUser(principal, text(args, "userId"));
                    JSONObject profile = eu.kalafatic.evolution.controller.cognitive.UserContextService.getInstance()
                            .getProfile(username);
                    Map<String, Object> result = new HashMap<>();
                    result.put("profile", toSerializable(profile));
                    return result;
                });

        Map<String, Object> prefSchema = new HashMap<>();
        prefSchema.put("type", "object");
        prefSchema.put("properties", Map.of(
                "userId", Map.of("type", "string", "description", "Hint: target user"),
                "scope", Map.of("type", "string", "description", "Scope filter"),
                "project", Map.of("type", "string", "description", "Project filter")));
        registry.register(new Tool("user_preferences_get", "Returns effective preferences (explicit beats inferred)", prefSchema),
                (args, context) -> {
                    ValidatedPrincipal principal = principalOf(context);
                    String username = effectiveUser(principal, text(args, "userId"));
                    String project = text(args, "project");
                    List<JSONObject> preferences = PersonalizationService.getInstance().getEffectivePreferences(username, project);
                    Map<String, Object> result = new HashMap<>();
                    result.put("preferences", toSerializable(preferences));
                    return result;
                });

        Map<String, Object> updatePrefSchema = new HashMap<>();
        updatePrefSchema.put("type", "object");
        Map<String, Object> updateProps = new HashMap<>();
        updateProps.put("userId", Map.of("type", "string", "description", "Hint: target user (self unless admin)"));
        updateProps.put("scope", Map.of("type", "string", "description", "Preference scope"));
        updateProps.put("project", Map.of("type", "string", "description", "Project reference"));
        updateProps.put("key", Map.of("type", "string", "description", "Preference key"));
        updateProps.put("value", Map.of("type", "string", "description", "Preference value (JSON string)"));
        updateProps.put("overwriteExplicit", Map.of("type", "boolean", "description", "Allow inferred writes to overwrite explicit (default false)"));
        updatePrefSchema.put("properties", updateProps);
        updatePrefSchema.put("required", List.of("key", "value"));

        registry.register(new Tool("user_preferences_update", "Sets an explicit user preference (explicit always wins)", updatePrefSchema),
                (args, context) -> {
                    ValidatedPrincipal principal = principalOf(context);
                    String username = effectiveUser(principal, text(args, "userId"));
                    String scope = text(args, "scope") != null ? text(args, "scope") : "USER";
                    String project = text(args, "project");
                    String key = text(args, "key");
                    String value = text(args, "value");
                    JSONObject profile = eu.kalafatic.evolution.controller.cognitive.UserContextService.getInstance()
                            .getProfile(username);
                    if (!profile.has("userId")) {
                        throw new IllegalArgumentException("Unknown user: " + username);
                    }
                    long userId = profile.getLong("userId");
                    JSONObject result = PersonalizationService.getInstance()
                            .updateExplicit(userId, scope, project, key, value, 1.0);
                    Map<String, Object> response = new HashMap<>();
                    response.put("accepted", result.optBoolean("accepted"));
                    response.put("preference", toSerializable(result.optJSONObject("preference")));
                    return response;
                });
    }

    // ------------------------------------------------------------------
    // Session tools
    // ------------------------------------------------------------------

    private static void registerSessionTools(ToolRegistry registry) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> props = new HashMap<>();
        props.put("kind", Map.of("type", "string", "description", "Session kind (USER/APPLICATION/CONVERSATION/TASK)"));
        props.put("project", Map.of("type", "string", "description", "Project reference"));
        props.put("correlationId", Map.of("type", "string", "description", "Caller-side correlation id"));
        props.put("metadata", Map.of("type", "object", "description", "Additional metadata"));
        schema.put("properties", props);

        registry.register(new Tool("session_create", "Registers a new cognitive session", schema),
                (args, context) -> {
                    ValidatedPrincipal principal = principalOf(context);
                    String kind = text(args, "kind") != null ? text(args, "kind") : SessionDescriptor.KIND_CONVERSATION;
                    String project = text(args, "project");
                    String correlationId = text(args, "correlationId");
                    JSONObject metadata = new JSONObject();
                    if (correlationId != null) {
                        metadata.put("correlationId", correlationId);
                    }
                    SessionDescriptor descriptor = SessionContinuityService.getInstance().beginSession(
                            kind,
                            principal != null ? principal.getUsername() : null,
                            principal != null ? principal.getApplicationId() : null,
                            project, null,
                            metadata.keySet().isEmpty() ? null : metadata.toString());
                    Map<String, Object> result = new HashMap<>();
                    result.put("sessionId", descriptor.getId());
                    result.put("kind", descriptor.getKind());
                    result.put("status", descriptor.getStatus());
                    return result;
                });

        Map<String, Object> getSchema = new HashMap<>();
        getSchema.put("type", "object");
        getSchema.put("properties", Map.of("sessionId", Map.of("type", "string", "description", "Session id")));
        getSchema.put("required", List.of("sessionId"));
        registry.register(new Tool("session_get", "Returns a session descriptor with relationships and latest snapshot", getSchema),
                (args, context) -> {
                    String sessionId = text(args, "sessionId");
                    SessionDescriptor descriptor = SessionContinuityService.getInstance().resolve(sessionId);
                    if (descriptor == null) {
                        throw new java.util.NoSuchElementException("Session not found: " + sessionId);
                    }
                    if (!isOwnSession(principalOf(context), descriptor)) {
                        throw new SecurityException("Access denied: session belongs to another user or application");
                    }
                    JSONObject result = descriptor.toJsonObject();
                    JSONArray relationships = new JSONArray();
                    for (JSONObject rel : SessionContinuityService.getInstance().listRelationships(descriptor.getId())) {
                        relationships.put(rel);
                    }
                    result.put("relationships", relationships);
                    JSONObject snapshot = SessionContinuityService.getInstance().getLatestSnapshot(descriptor.getId());
                    result.put("latestSnapshot", snapshot != null ? snapshot : JSONObject.NULL);
                    return jsonResult(result);
                });

        Map<String, Object> searchSchema = new HashMap<>();
        searchSchema.put("type", "object");
        Map<String, Object> searchProps = new HashMap<>();
        searchProps.put("project", Map.of("type", "string", "description", "Project filter"));
        searchProps.put("kind", Map.of("type", "string", "description", "Kind filter"));
        searchProps.put("status", Map.of("type", "string", "description", "Status filter"));
        searchProps.put("limit", Map.of("type", "integer", "description", "Maximum results"));
        searchSchema.put("properties", searchProps);
        registry.register(new Tool("session_search", "Lists sessions visible to the caller", searchSchema),
                (args, context) -> {
                    ValidatedPrincipal principal = principalOf(context);
                    String project = text(args, "project");
                    String status = text(args, "status");
                    int limit = intArg(args, "limit", 50);
                    List<SessionDescriptor> sessions = SessionContinuityService.getInstance()
                            .listSessions(null, null, project, status, limit);
                    List<JSONObject> results = new ArrayList<>();
                    for (SessionDescriptor descriptor : sessions) {
                        if (!isOwnSession(principal, descriptor)) {
                            continue;
                        }
                        results.add(descriptor.toJsonObject());
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("count", results.size());
                    result.put("sessions", toSerializable(results));
                    return result;
                });

        Map<String, Object> snapSchema = new HashMap<>();
        snapSchema.put("type", "object");
        Map<String, Object> snapProps = new HashMap<>();
        snapProps.put("sessionId", Map.of("type", "string", "description", "Session id"));
        snapProps.put("kind", Map.of("type", "string", "description", "Snapshot kind (KERNEL/CONTEXT/FULL)"));
        snapSchema.put("properties", snapProps);
        snapSchema.put("required", List.of("sessionId"));
        registry.register(new Tool("session_snapshot", "Creates a layered snapshot manifest for a session", snapSchema),
                (args, context) -> {
                    String sessionId = text(args, "sessionId");
                    String kind = text(args, "kind") != null ? text(args, "kind") : "FULL";
                    SessionDescriptor descriptor = SessionContinuityService.getInstance().resolve(sessionId);
                    if (descriptor == null) {
                        throw new java.util.NoSuchElementException("Session not found: " + sessionId);
                    }
                    if (!isOwnSession(principalOf(context), descriptor)) {
                        throw new SecurityException("Access denied: session belongs to another user or application");
                    }
                    ValidatedPrincipal principal = principalOf(context);
                    JSONObject manifest = SessionContinuityService.getInstance().snapshot(descriptor.getId(), kind,
                            descriptor.getId(), null, null,
                            principal != null ? principal.getUsername() : null);
                    Map<String, Object> result = new HashMap<>();
                    result.put("snapshotId", manifest.optString("snapshotId"));
                    result.put("ref", manifest.optString("ref"));
                    return result;
                });

        Map<String, Object> restoreSchema = new HashMap<>();
        restoreSchema.put("type", "object");
        restoreSchema.put("properties", Map.of("sessionId", Map.of("type", "string", "description", "Session id")));
        restoreSchema.put("required", List.of("sessionId"));
        registry.register(new Tool("session_restore", "Restores supported session layers (conversation/context/kernel references)", restoreSchema),
                (args, context) -> {
                    String sessionId = text(args, "sessionId");
                    SessionDescriptor descriptor = SessionContinuityService.getInstance().resolve(sessionId);
                    if (descriptor == null) {
                        throw new java.util.NoSuchElementException("Session not found: " + sessionId);
                    }
                    if (!isOwnSession(principalOf(context), descriptor)) {
                        throw new SecurityException("Access denied: session belongs to another user or application");
                    }
                    JSONObject restored = SessionContinuityService.getInstance().restore(descriptor.getId());
                    return jsonResult(restored);
                });
    }

    // ------------------------------------------------------------------
    // Decision tools
    // ------------------------------------------------------------------

    private static void registerDecisionTools(ToolRegistry registry) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        Map<String, Object> props = new HashMap<>();
        props.put("project", Map.of("type", "string", "description", "Project reference"));
        props.put("decisionId", Map.of("type", "string", "description", "Optional decision id (ADR-xxx)"));
        props.put("title", Map.of("type", "string", "description", "Decision title"));
        props.put("context", Map.of("type", "string", "description", "Decision context"));
        props.put("decision", Map.of("type", "string", "description", "The decision"));
        props.put("rationale", Map.of("type", "string", "description", "Rationale"));
        props.put("status", Map.of("type", "string", "description", "Status (PROPOSED/ACCEPTED/SUPERSEDED/REJECTED)"));
        schema.put("properties", props);
        schema.put("required", List.of("project", "title", "decision"));

        registry.register(new Tool("decision_record", "Records an architectural decision for a project", schema),
                (args, context) -> {
                    String project = text(args, "project");
                    JSONObject result = ProjectKnowledgeService.getInstance().recordDecision(
                            project, text(args, "decisionId"), text(args, "title"), text(args, "context"),
                            text(args, "decision"), text(args, "rationale"), text(args, "status"));
                    Map<String, Object> response = new HashMap<>();
                    response.put("decisionId", result.optString("decisionId"));
                    response.put("project", result.optString("project"));
                    response.put("status", result.optString("status"));
                    return response;
                });

        Map<String, Object> searchSchema = new HashMap<>();
        searchSchema.put("type", "object");
        searchSchema.put("properties", Map.of(
                "project", Map.of("type", "string", "description", "Project reference"),
                "query", Map.of("type", "string", "description", "Search query"),
                "status", Map.of("type", "string", "description", "Status filter"),
                "limit", Map.of("type", "integer", "description", "Maximum results")));
        registry.register(new Tool("decision_search", "Searches architectural decisions", searchSchema),
                (args, context) -> {
                    String project = text(args, "project");
                    String query = text(args, "query");
                    String status = text(args, "status");
                    int limit = intArg(args, "limit", 25);
                    List<JSONObject> decisions = ProjectKnowledgeService.getInstance()
                            .searchDecisions(project, query, status, limit);
                    Map<String, Object> result = new HashMap<>();
                    result.put("count", decisions.size());
                    result.put("decisions", toSerializable(decisions));
                    return result;
                });

        Map<String, Object> getSchema = new HashMap<>();
        getSchema.put("type", "object");
        getSchema.put("properties", Map.of(
                "project", Map.of("type", "string", "description", "Project reference"),
                "decisionId", Map.of("type", "string", "description", "Decision id")));
        getSchema.put("required", List.of("project", "decisionId"));
        registry.register(new Tool("decision_get", "Returns a decision with its supersession chain", getSchema),
                (args, context) -> {
                    JSONObject result = ProjectKnowledgeService.getInstance()
                            .getDecision(text(args, "project"), text(args, "decisionId"));
                    return jsonResult(result);
                });
    }

    // ------------------------------------------------------------------
    // Project tools
    // ------------------------------------------------------------------

    private static void registerProjectTools(ToolRegistry registry) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of("project", Map.of("type", "string", "description", "Project reference")));
        schema.put("required", List.of("project"));
        registry.register(new Tool("project_context_get", "Returns project components, active decisions, known issues and state", schema),
                (args, context) -> {
                    String project = text(args, "project");
                    ProjectKnowledgeService service = ProjectKnowledgeService.getInstance();
                    JSONObject result = new JSONObject();
                    result.put("project", project);
                    JSONArray components = new JSONArray();
                    for (JSONObject component : service.listComponents(project, 200)) {
                        components.put(component);
                    }
                    result.put("components", toSerializable(components));
                    JSONArray decisions = new JSONArray();
                    for (JSONObject decision : service.searchDecisions(project, null, "ACCEPTED", 50)) {
                        decisions.put(decision);
                    }
                    result.put("decisions", toSerializable(decisions));
                    JSONArray issues = new JSONArray();
                    for (JSONObject issue : service.listKnownIssues(project, null, 50)) {
                        issues.put(issue);
                    }
                    result.put("knownIssues", issues);
                    result.put("state", service.getState(project));
                    return jsonResult(result);
                });

        Map<String, Object> stateSchema = new HashMap<>();
        stateSchema.put("type", "object");
        stateSchema.put("properties", Map.of("project", Map.of("type", "string", "description", "Project reference")));
        stateSchema.put("required", List.of("project"));
        registry.register(new Tool("project_state_get", "Returns the current project state (last scan, build, test, issues)", stateSchema),
                (args, context) -> jsonResult(ProjectKnowledgeService.getInstance().getState(text(args, "project"))));

        Map<String, Object> updateSchema = new HashMap<>();
        updateSchema.put("type", "object");
        Map<String, Object> updateProps = new HashMap<>();
        updateProps.put("project", Map.of("type", "string", "description", "Project reference"));
        updateProps.put("key", Map.of("type", "string", "description", "State key (build/test summary supported)"));
        updateProps.put("value", Map.of("type", "string", "description", "State value"));
        updateSchema.put("properties", updateProps);
        updateSchema.put("required", List.of("project", "key", "value"));
        registry.register(new Tool("project_state_update", "Updates restricted project state keys", updateSchema),
                (args, context) -> {
                    String project = text(args, "project");
                    String key = text(args, "key");
                    String value = text(args, "value");
                    if ("build".equalsIgnoreCase(key)) {
                        ProjectKnowledgeService.getInstance().recordBuild(project, "mcp", true, 0, value, null);
                    } else if ("implementation".equalsIgnoreCase(key)) {
                        ProjectKnowledgeService.getInstance().recordImplementation(project, value, null, null, null);
                    } else {
                        throw new IllegalArgumentException("Unsupported state key: " + key);
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("accepted", true);
                    return result;
                });

        Map<String, Object> searchSchema = new HashMap<>();
        searchSchema.put("type", "object");
        searchSchema.put("properties", Map.of(
                "project", Map.of("type", "string", "description", "Project reference"),
                "query", Map.of("type", "string", "description", "Search query"),
                "kinds", Map.of("type", "array", "description", "Kinds: components, decisions, issues")));
        searchSchema.put("required", List.of("project"));
        registry.register(new Tool("project_knowledge_search", "Searches project components, decisions and issues", searchSchema),
                (args, context) -> {
                    String project = text(args, "project");
                    String query = text(args, "query");
                    List<String> kinds = stringList(args, "kinds");
                    JSONObject result = new JSONObject();
                    if (kinds == null || kinds.isEmpty() || kinds.contains("decisions")) {
                        JSONArray decisions = new JSONArray();
                        for (JSONObject decision : ProjectKnowledgeService.getInstance()
                                .searchDecisions(project, query, null, 25)) {
                            decisions.put(decision);
                        }
                        result.put("decisions", toSerializable(decisions));
                    }
                    if (kinds == null || kinds.isEmpty() || kinds.contains("issues")) {
                        JSONArray issues = new JSONArray();
                        for (JSONObject issue : ProjectKnowledgeService.getInstance()
                                .listKnownIssues(project, null, 25)) {
                            if (query == null || issue.toString().toLowerCase().contains(query.toLowerCase())) {
                                issues.put(issue);
                            }
                        }
                        result.put("issues", toSerializable(issues));
                    }
                    if (kinds == null || kinds.isEmpty() || kinds.contains("components")) {
                        JSONArray components = new JSONArray();
                        for (JSONObject component : ProjectKnowledgeService.getInstance()
                                .listComponents(project, 100)) {
                            if (query == null || component.toString().toLowerCase().contains(query.toLowerCase())) {
                                components.put(component);
                            }
                        }
                        result.put("components", toSerializable(components));
                    }
                    return jsonResult(result);
                });
    }

    // ------------------------------------------------------------------
    // Conversation history tools
    // ------------------------------------------------------------------

    private static void registerConversationTools(ToolRegistry registry) {
        Map<String, Object> getSchema = new HashMap<>();
        getSchema.put("type", "object");
        Map<String, Object> getProps = new HashMap<>();
        getProps.put("conversationId", Map.of("type", "string", "description", "Conversation/session id"));
        getProps.put("from", Map.of("type", "integer", "description", "Starting sequence"));
        getProps.put("limit", Map.of("type", "integer", "description", "Maximum records"));
        getSchema.put("properties", getProps);
        getSchema.put("required", List.of("conversationId"));
        registry.register(new Tool("conversation_history_get", "Loads durable conversation history records", getSchema),
                (args, context) -> {
                    String conversationId = text(args, "conversationId");
                    SessionDescriptor descriptor = SessionContinuityService.getInstance().resolve(conversationId);
                    if (descriptor != null && !isOwnSession(principalOf(context), descriptor)) {
                        throw new SecurityException("Access denied: conversation belongs to another user or application");
                    }
                    List<ConversationRecord> records = ConversationHistoryService.getInstance()
                            .list(conversationId, intArg(args, "from", 0), intArg(args, "limit", 100));
                    List<JSONObject> results = new ArrayList<>();
                    for (ConversationRecord record : records) {
                        results.add(record.toJsonObject());
                    }
                    Map<String, Object> result = new HashMap<>();
                    result.put("count", results.size());
                    result.put("records", toSerializable(results));
                    return result;
                });

        Map<String, Object> appendSchema = new HashMap<>();
        appendSchema.put("type", "object");
        Map<String, Object> appendProps = new HashMap<>();
        appendProps.put("conversationId", Map.of("type", "string", "description", "Conversation/session id"));
        appendProps.put("role", Map.of("type", "string", "description", "Record role (USER/ASSISTANT/SYSTEM/TOOL_CALL/TOOL_RESULT/DECISION/EVENT/ERROR)"));
        appendProps.put("toolName", Map.of("type", "string", "description", "Tool name for tool records"));
        appendProps.put("content", Map.of("type", "string", "description", "Record content"));
        appendSchema.put("properties", appendProps);
        appendSchema.put("required", List.of("conversationId", "content"));
        registry.register(new Tool("conversation_history_append", "Appends one durable conversation history record", appendSchema),
                (args, context) -> {
                    String conversationId = text(args, "conversationId");
                    SessionDescriptor descriptor = SessionContinuityService.getInstance().resolve(conversationId);
                    if (descriptor != null && !isOwnSession(principalOf(context), descriptor)) {
                        throw new SecurityException("Access denied: conversation belongs to another user or application");
                    }
                    String role = text(args, "role") != null ? text(args, "role") : "EVENT";
                    String toolName = text(args, "toolName");
                    ConversationRecord record = ConversationHistoryService.getInstance()
                            .append(conversationId, role, null, toolName, text(args, "content"), null);
                    Map<String, Object> result = new HashMap<>();
                    result.put("sequence", record.getSequence());
                    result.put("recordId", record.getId());
                    return result;
                });
    }

    // ------------------------------------------------------------------
    // Resources (evo://)
    // ------------------------------------------------------------------

    /** Registers evo:// cognitive resources. */
    public static void registerCognitiveResources(ResourceRegistry registry) {
        if (registry == null) {
            return;
        }
        // evo://sessions/{id}/history - paginated conversation transcript
        registry.register(new Resource("evo://sessions/history", "Session conversation history",
                "Conversation transcript of a session (pass sessionId as query param)",
                "application/json"), historyResourceProvider());
        // evo://memory/{memoryId} - single memory entry
        registry.register(new Resource("evo://memory/entry", "Memory entry",
                "Single memory entry (pass entryId as query param)",
                "application/json"), memoryResourceProvider());
        // evo://users/profile - user profile
        registry.register(new Resource("evo://users/profile", "User profile",
                "User profile (pass userId as query param)",
                "application/json"), profileResourceProvider());
        // evo://projects/state - project state
        registry.register(new Resource("evo://projects/state", "Project state",
                "Current project state (pass project as query param)",
                "application/json"), projectStateResourceProvider());
        LOGGER.info("[MCP] Cognitive Services resources registered.");
    }

    private static ResourceProvider historyResourceProvider() {
        return uri -> {
            String sessionId = queryParam(uri, "sessionId");
            if (sessionId == null) {
                throw new IllegalArgumentException("sessionId query param is required");
            }
            List<ConversationRecord> records = ConversationHistoryService.getInstance().list(sessionId, 0, 200);
            JSONObject result = new JSONObject();
            result.put("sessionId", sessionId);
            JSONArray arr = new JSONArray();
            for (ConversationRecord record : records) {
                arr.put(record.toJsonObject());
            }
            result.put("records", arr);
            return result.toString();
        };
    }

    private static ResourceProvider memoryResourceProvider() {
        return uri -> {
            String entryId = queryParam(uri, "entryId");
            if (entryId == null) {
                throw new IllegalArgumentException("entryId query param is required");
            }
            MemoryEntry entry = MemoryService.getInstance().getEntryById(entryId);
            if (entry == null) {
                throw new java.util.NoSuchElementException("Memory entry not found: " + entryId);
            }
            return entry.toJsonObject().toString();
        };
    }

    private static ResourceProvider profileResourceProvider() {
        return uri -> {
            String userId = queryParam(uri, "userId");
            // Resources on the 28280 server are authorized at the dispatcher
            // level (RESOURCE READ); per-URI principal filtering is applied
            // by the native services when a principal context is available.
            JSONObject profile = eu.kalafatic.evolution.controller.cognitive.UserContextService.getInstance()
                    .getProfile(userId);
            return profile.toString();
        };
    }

    private static ResourceProvider projectStateResourceProvider() {
        return uri -> {
            String project = queryParam(uri, "project");
            if (project == null) {
                throw new IllegalArgumentException("project query param is required");
            }
            return ProjectKnowledgeService.getInstance().getState(project).toString();
        };
    }

    private static String queryParam(String uri, String name) {
        if (uri == null || !uri.contains("?")) {
            return null;
        }
        String query = uri.substring(uri.indexOf('?') + 1);
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && name.equals(pair.substring(0, eq))) {
                return java.net.URLDecoder.decode(pair.substring(eq + 1), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Converts the result of a tool into a Jackson-serializable structure.
     * org.json types (JSONObject/JSONArray/JSONObject.NULL) are converted
     * recursively to plain Maps/Lists/null so the JSON-RPC response can be
     * serialized by the dispatcher's ObjectMapper.
     */
    public static Object toSerializable(Object value) {
        if (value instanceof JSONObject json) {
            Map<String, Object> map = new HashMap<>();
            for (Object key : json.keySet()) {
                map.put(String.valueOf(key), toSerializable(json.opt(String.valueOf(key))));
            }
            return map;
        }
        if (value instanceof JSONArray array) {
            List<Object> list = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                list.add(toSerializable(array.opt(i)));
            }
            return list;
        }
        if (value == null || JSONObject.NULL.equals(value)) {
            return null;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                converted.put(String.valueOf(entry.getKey()), toSerializable(entry.getValue()));
            }
            return converted;
        }
        if (value instanceof List<?> list) {
            List<Object> converted = new ArrayList<>();
            for (Object item : list) {
                converted.add(toSerializable(item));
            }
            return converted;
        }
        return value;
    }

    private static Map<String, Object> jsonResult(JSONObject json) {
        Object converted = toSerializable(json);
        return (converted instanceof Map) ? (Map<String, Object>) converted : new HashMap<>();
    }

    private static String text(JsonNode args, String field) {
        return args != null && args.hasNonNull(field) ? args.get(field).asText() : null;
    }

    private static int intArg(JsonNode args, String field, int defaultValue) {
        return args != null && args.hasNonNull(field) ? args.get(field).asInt(defaultValue) : defaultValue;
    }

    private static double numberArg(JsonNode args, String field, double defaultValue) {
        return args != null && args.hasNonNull(field) ? args.get(field).asDouble(defaultValue) : defaultValue;
    }

    private static <T extends Enum<T>> T enumArg(JsonNode args, String field, Class<T> type, T defaultValue) {
        String value = text(args, field);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    private static List<String> stringList(JsonNode args, String field) {
        List<String> list = new ArrayList<>();
        if (args != null && args.hasNonNull(field) && args.get(field).isArray()) {
            for (JsonNode node : args.get(field)) {
                if (node.isTextual()) {
                    list.add(node.asText());
                }
            }
        }
        return list;
    }
}
