package eu.kalafatic.evolution.controller.cognitive;

import java.util.List;

import eu.kalafatic.evolution.controller.cognitive.model.ContextPackage;
import eu.kalafatic.evolution.controller.cognitive.model.ContextRequest;
import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.servers.service.ValidatedPrincipal;
import eu.kalafatic.evolution.servers.validation.ValidationResult;

/**
 * Facade for the native cognitive services: the single entry point used by
 * MCP adapters and internal EVO components. One implementation, two
 * interfaces (OSGi/Java and MCP). Internal EVO components call this facade
 * directly - never the MCP server over HTTP.
 */
public class CognitiveContextService {

    private static CognitiveContextService instance;

    private final ContextReconstructionService reconstructionService;
    private final MemoryService memoryService;

    private CognitiveContextService() {
        this.reconstructionService = ContextReconstructionService.getInstance();
        this.memoryService = MemoryService.getInstance();
    }

    public static synchronized CognitiveContextService getInstance() {
        if (instance == null) {
            instance = new CognitiveContextService();
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
    }

    /**
     * Reconstructs a ContextPackage for the request. When a principal is
     * supplied, its identity and scopes are authoritative; request params are
     * hints only. Domain validation runs under the central policy (default
     * DISABLED; security checks always active at the service boundaries).
     */
    public ContextPackage getContext(ContextRequest request, ValidatedPrincipal principal) {
        if (request == null) {
            throw new IllegalArgumentException("ContextRequest is required");
        }
        ContextPackage pkg = reconstructionService.reconstruct(request, principal);
        List<ValidationResult> validationResults = reconstructionService.runDomainValidation(request, pkg);
        for (ValidationResult result : validationResults) {
            if (!result.isPassed() && result.getMessage() != null) {
                pkg.addWarning(result.getMessage());
            }
        }
        return pkg;
    }

    /** Convenience overload for internal callers without a token principal. */
    public ContextPackage getContext(ContextRequest request) {
        return getContext(request, null);
    }

    /** Cross-section search: memory + decisions + conversations. */
    public java.util.List<Object> searchContext(String query, String project, List<String> types, int limit) {
        java.util.List<Object> results = new java.util.ArrayList<>();
        boolean includeMemory = types == null || types.isEmpty() || types.contains("memory");
        boolean includeDecisions = types == null || types.isEmpty() || types.contains("decisions");
        boolean includeConversations = types == null || types.isEmpty() || types.contains("conversations");
        if (includeMemory) {
            for (MemoryEntry entry : memoryService.search(query, null, null, project, limit > 0 ? limit : 25)) {
                results.add(entry.toJsonObject());
            }
        }
        if (includeDecisions) {
            for (org.json.JSONObject decision : ProjectKnowledgeService.getInstance()
                    .searchDecisions(project, query, null, limit > 0 ? limit : 25)) {
                results.add(decision);
            }
        }
        if (includeConversations) {
            for (var record : ConversationHistoryService.getInstance().search(query, limit > 0 ? limit : 25)) {
                results.add(record.toJsonObject());
            }
        }
        return results;
    }

    /** Stores one memory record through the unified memory API. */
    public MemoryEntry storeMemory(MemoryEntry entry) {
        return memoryService.addEntry(entry);
    }

    /** Retracts a memory record (auditable; never hard-deleted). */
    public MemoryEntry retractMemory(String entryId, String reason, String actor) {
        return memoryService.retractEntry(entryId, reason, actor);
    }

    /** Persists a ContextSnapshot reference for replay/audit. */
    public void recordContext(String contextId, String schemaVersion, String requestedBy, String packageRef) {
        org.json.JSONObject row = new org.json.JSONObject();
        row.put("contextId", contextId);
        row.put("schemaVersion", schemaVersion);
        row.put("packageRef", packageRef);
        row.put("requestedBy", requestedBy);
        row.put("createdAt", System.currentTimeMillis());
        try (java.sql.Connection conn = SessionContinuityService.getInstance().getDatabaseManager().getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO context_snapshots (id, context_id, schema_version, package_ref, requested_by, created_at) " +
                     "VALUES (?,?,?,?,?,?)")) {
            ps.setString(1, SessionIdService.newSnapshotId());
            ps.setString(2, contextId);
            ps.setString(3, schemaVersion);
            ps.setString(4, packageRef);
            ps.setString(5, requestedBy);
            ps.setLong(6, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist context snapshot reference", e);
        }
    }
}
