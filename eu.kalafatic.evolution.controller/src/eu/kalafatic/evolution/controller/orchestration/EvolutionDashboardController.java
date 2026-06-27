package eu.kalafatic.evolution.controller.orchestration;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionNode;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionTree;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;

/**
 * Controller for the Evolution Dashboard, exposing the EvolutionTree.
 */
public class EvolutionDashboardController {

    public String getEvolutionTreeJson(String sessionId) {
        SessionContainer session = SessionManager.getInstance().getSession(sessionId);
        if (session == null || !(session instanceof SessionContext)) return "{}";

        SessionContext context = (SessionContext) session;
        IterationMemoryService memory = context.getTaskContext().getKernelContext().getMemoryService();
        EvolutionTree tree = memory.getEvolutionTree();

        JSONObject result = new JSONObject();
        result.put("rootId", tree.getRootId());
        result.put("currentWinnerId", tree.getCurrentWinnerId());

        JSONArray nodes = new JSONArray();
        for (EvolutionNode node : tree.getNodes().values()) {
            JSONObject n = new JSONObject();
            n.put("id", node.getId());
            n.put("parentId", node.getParentId());
            n.put("iteration", node.getIteration());
            n.put("generation", node.getGeneration());
            n.put("depth", node.getBranchDepth());
            n.put("strategy", node.getStrategy());
            n.put("philosophy", node.getSemanticPhilosophy());
            n.put("status", node.getStatus());
            n.put("fitness", node.getFitnessScore());
            n.put("timestamp", node.getTimestamp());

            // Add detailed info for inspection
            JSONObject details = new JSONObject();
            details.put("mutationReason", node.getMutationReason());
            details.put("selectionReason", node.getSelectionReason());
            details.put("rejectionReason", node.getRejectionReason());
            details.put("llmPrompt", node.getLlmPrompt());
            details.put("llmResponse", node.getLlmResponse());

            JSONArray created = new JSONArray(node.getCreatedFiles());
            JSONArray modified = new JSONArray(node.getModifiedFiles());
            JSONArray deleted = new JSONArray(node.getDeletedFiles());

            details.put("createdFiles", created);
            details.put("modifiedFiles", modified);
            details.put("deletedFiles", deleted);

            n.put("details", details);
            nodes.put(n);
        }
        result.put("nodes", nodes);

        return result.toString();
    }
}
