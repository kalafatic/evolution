package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import eu.kalafatic.evolution.controller.mediation.model.Hotspot;
import eu.kalafatic.evolution.controller.mediation.model.MediationResult;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Represents a single node in the persistent evolutionary tree.
 * Captures the full state, reasoning, and lineage of a specific mutation.
 */
public class EvolutionNode {
    private String id;
    private String parentId;
    private int iteration;
    private int generation;
    private int branchDepth;

    private String strategy;
    private String semanticPhilosophy;
    private String activeDimension;
    private Map<String, String> engineeringDimensions = new HashMap<>();
    private Map<String, String> codeSnapshots = new HashMap<>();

    private SemanticGenome genomeSnapshot;
    private MutationRecord mutationRecord;
    private ExecutionRecord executionRecord;
    private VerificationRecord verificationRecord;
    private FitnessRecord fitnessRecord;

    private double fitnessScore;
    private double verificationScore;

    // Repository Changes (Legacy/Shortcut)
    private List<String> createdFiles = new ArrayList<>();
    private List<String> modifiedFiles = new ArrayList<>();
    private List<String> deletedFiles = new ArrayList<>();

    // LLM Snapshots
    private String llmPrompt;
    private String llmResponse;

    // Reasoning and Results
    private String mutationReason;
    private String mutationIdentity;
    private String parentStrengths;
    private String parentWeaknesses;
    private String selectionReason;
    private String rejectionReason;
    private String executionResults;
    private String verificationResults;
    private String runtimeObservations;
    private List<String> mutationJournal = new ArrayList<>();

    // Lineage
    private List<String> childIds = new ArrayList<>();
    private List<String> ancestorIds = new ArrayList<>();

    private long timestamp;
    private String status; // ACTIVE, KEPT, REJECTED, ROOT
    private boolean winner;

    public EvolutionNode() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }

    public int getIteration() { return iteration; }
    public void setIteration(int iteration) { this.iteration = iteration; }

    public int getGeneration() { return generation; }
    public void setGeneration(int generation) { this.generation = generation; }

    public int getBranchDepth() { return branchDepth; }
    public void setBranchDepth(int branchDepth) { this.branchDepth = branchDepth; }

    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }

    public String getSemanticPhilosophy() { return semanticPhilosophy; }
    public void setSemanticPhilosophy(String semanticPhilosophy) { this.semanticPhilosophy = semanticPhilosophy; }

    public String getActiveDimension() { return activeDimension; }
    public void setActiveDimension(String activeDimension) { this.activeDimension = activeDimension; }

    public Map<String, String> getEngineeringDimensions() { return engineeringDimensions; }
    public void setEngineeringDimensions(Map<String, String> engineeringDimensions) { this.engineeringDimensions = engineeringDimensions; }

    public Map<String, String> getCodeSnapshots() { return codeSnapshots; }
    public void setCodeSnapshots(Map<String, String> codeSnapshots) { this.codeSnapshots = codeSnapshots; }

    public SemanticGenome getGenomeSnapshot() { return genomeSnapshot; }
    public void setGenomeSnapshot(SemanticGenome genomeSnapshot) { this.genomeSnapshot = genomeSnapshot; }

    public double getFitnessScore() { return fitnessScore; }
    public void setFitnessScore(double fitnessScore) { this.fitnessScore = fitnessScore; }

    public double getVerificationScore() { return verificationScore; }
    public void setVerificationScore(double verificationScore) { this.verificationScore = verificationScore; }

    public List<String> getCreatedFiles() { return createdFiles; }
    public void setCreatedFiles(List<String> createdFiles) { this.createdFiles = createdFiles; }

    public List<String> getModifiedFiles() { return modifiedFiles; }
    public void setModifiedFiles(List<String> modifiedFiles) { this.modifiedFiles = modifiedFiles; }

    public List<String> getDeletedFiles() { return deletedFiles; }
    public void setDeletedFiles(List<String> deletedFiles) { this.deletedFiles = deletedFiles; }

    public MutationRecord getMutationRecord() { return mutationRecord; }
    public void setMutationRecord(MutationRecord mutationRecord) { this.mutationRecord = mutationRecord; }

    public ExecutionRecord getExecutionRecord() { return executionRecord; }
    public void setExecutionRecord(ExecutionRecord executionRecord) { this.executionRecord = executionRecord; }

    public VerificationRecord getVerificationRecord() { return verificationRecord; }
    public void setVerificationRecord(VerificationRecord verificationRecord) { this.verificationRecord = verificationRecord; }

    public FitnessRecord getFitnessRecord() { return fitnessRecord; }
    public void setFitnessRecord(FitnessRecord fitnessRecord) { this.fitnessRecord = fitnessRecord; }

    public String getLlmPrompt() { return llmPrompt; }
    public void setLlmPrompt(String llmPrompt) { this.llmPrompt = llmPrompt; }

    public String getLlmResponse() { return llmResponse; }
    public void setLlmResponse(String llmResponse) { this.llmResponse = llmResponse; }

    public String getMutationReason() { return mutationReason; }
    public void setMutationReason(String mutationReason) { this.mutationReason = mutationReason; }

    public String getMutationIdentity() { return mutationIdentity; }
    public void setMutationIdentity(String mutationIdentity) { this.mutationIdentity = mutationIdentity; }

    public String getParentStrengths() { return parentStrengths; }
    public void setParentStrengths(String parentStrengths) { this.parentStrengths = parentStrengths; }

    public String getParentWeaknesses() { return parentWeaknesses; }
    public void setParentWeaknesses(String parentWeaknesses) { this.parentWeaknesses = parentWeaknesses; }

    public String getSelectionReason() { return selectionReason; }
    public void setSelectionReason(String selectionReason) { this.selectionReason = selectionReason; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getExecutionResults() { return executionResults; }
    public void setExecutionResults(String executionResults) { this.executionResults = executionResults; }

    public String getVerificationResults() { return verificationResults; }
    public void setVerificationResults(String verificationResults) { this.verificationResults = verificationResults; }

    public String getRuntimeObservations() { return runtimeObservations; }
    public void setRuntimeObservations(String runtimeObservations) { this.runtimeObservations = runtimeObservations; }

    public List<String> getMutationJournal() { return mutationJournal; }
    public void setMutationJournal(List<String> mutationJournal) { this.mutationJournal = mutationJournal; }

    public List<String> getChildIds() { return childIds; }
    public void setChildIds(List<String> childIds) { this.childIds = childIds; }

    public List<String> getAncestorIds() { return ancestorIds; }
    public void setAncestorIds(List<String> ancestorIds) { this.ancestorIds = ancestorIds; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isWinner() { return winner; }
    public void setWinner(boolean winner) { this.winner = winner; }

	/**
 * Merges mediation insights into the evolutionary context.
 */
private void mergeMediationInsights(MediationResult mediation, TaskContext context, 
        IterationManager manager) {
    
    context.log("[DARWIN] Merging mediation insights...");
    
    // Store hotspots in the evolution tree for future reference
    EvolutionTree tree = context.getKernelContext().getMemoryService().getEvolutionTree();
    
    if (mediation.getHotspots() != null) {
        for (Hotspot hotspot : mediation.getHotspots()) {
            String filePath = hotspot.getFile();
            if (filePath != null && !filePath.isEmpty()) {
                String nodeId = "hotspot-" + Math.abs(filePath.hashCode());
                EvolutionNode node = tree.getNode(nodeId);
                if (node == null) {
                    node = new EvolutionNode();
                    node.setId(nodeId);
                    node.setStrategy("Hotspot: " + hotspot.getName());
                    node.setStatus("MEDIATED");
                    tree.addNode(node);
                }
                // Store hotspot data in engineering dimensions (Map<String, String>)
                Map<String, String> dims = node.getEngineeringDimensions();
                if (dims == null) {
                    dims = new java.util.HashMap<>();
                    // Need to check if EvolutionNode has a setter for engineeringDimensions
                    // If not, we need to use the existing map
                }
                dims.put("hotspot_score", String.valueOf(hotspot.getSignificance()));
                dims.put("hotspot_type", hotspot.getType() != null ? hotspot.getType() : "UNKNOWN");
                if (hotspot.getName() != null) {
                    dims.put("hotspot_name", hotspot.getName());
                }
                if (hotspot.getDescription() != null) {
                    dims.put("hotspot_description", hotspot.getDescription());
                }
                // Note: We cannot call setEngineeringDimensions if it doesn't exist
                // The map should already be accessible via getEngineeringDimensions()
            }
        }
    }
    
    // Store mediation candidate if available
    if (mediation.getWinner() != null) {
        context.getOrchestrationState().getMetadata().put("currentMediationWinner", 
            mediation.getWinner());
    }
    
    // Persist changes
    context.getKernelContext().getMemoryService().saveEvolutionTree();
}
}
