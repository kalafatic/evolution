package eu.kalafatic.evolution.controller.orchestration.mediation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator;
import eu.kalafatic.evolution.controller.mediation.analysis.SemanticExtractor;
import eu.kalafatic.evolution.controller.mediation.model.Hotspot;
import eu.kalafatic.evolution.controller.mediation.model.MediationCandidate;
import eu.kalafatic.evolution.controller.mediation.model.MediationDelta;
import eu.kalafatic.evolution.controller.mediation.model.MediationResult;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;

/**
 * Continuous Mediation Engine - Evolves understanding alongside implementation.
 * 
 * Unlike a one-time preprocessing step, Mediation is a continuous process
 * that runs every iteration and re-evaluates after each mutation.
 */
public class MediationEngine {
    
    private final TargetScanner scanner = new TargetScanner();
    private final ContextCurator curator = new ContextCurator();
    private final SemanticExtractor extractor = new SemanticExtractor();
    
    private TargetSnapshot lastSnapshot;
    private TargetRealityModel lastModel;
    private String lastRequest;
    
    /**
     * Performs a complete mediation cycle on the current state.
     * Called at the start of each iteration AND after each mutation.
     */
    public MediationResult mediate(TaskContext context, String request, BranchVariant currentVariant) {
        context.log("[MEDIATION] Starting mediation cycle...");
        
        long startTime = System.currentTimeMillis();
        
        // 1. SCAN: Get current repository state
        TargetSnapshot snapshot = scanRepository(context);
        
        // 2. EXTRACT: Semantic understanding
        List<String> candidates = curator.selectContext(snapshot, request, 32);
        extractor.extractToSnapshot(snapshot, candidates);
        
        // 3. COMPARE: What changed since last mediation?
        MediationDelta delta = calculateDelta(lastSnapshot, snapshot);
        if (delta.hasChanges()) {
            context.log("[MEDIATION] Detected changes: " + delta.getSummary());
        }
        
        // 4. UPDATE: Evolve the reality model
        TargetRealityModel model = buildOrUpdateModel(snapshot, request, delta, context);
        
        // 5. IDENTIFY: Hotspots based on current state
        List<Hotspot> hotspots = identifyHotspots(snapshot, model, currentVariant, context);
        
        // 6. GENERATE: Mediation candidates for this iteration
        List<MediationCandidate> candidatesList = generateCandidates(snapshot, model, hotspots, context);
        
        // 7. SELECT: Best candidate for current context
        MediationCandidate winner = selectCandidate(candidatesList, currentVariant, context);
        
        // Store for next iteration
        this.lastSnapshot = snapshot;
        this.lastModel = model;
        this.lastRequest = request;
        
        long duration = System.currentTimeMillis() - startTime;
        context.log("[MEDIATION] Mediation complete in " + duration + "ms. " + 
                   candidatesList.size() + " candidates generated.");
        
        return new MediationResult(snapshot, model, hotspots, candidatesList, winner, delta, duration);
    }
    
    /**
     * Quick mediation for self-dev mode - faster, focused on build results.
     */
    public MediationResult quickMediate(TaskContext context, String request, BranchVariant currentVariant) {
        context.log("[MEDIATION] Quick mediation cycle for self-dev...");
        
        // Use cached snapshot if available, otherwise scan
        TargetSnapshot snapshot = lastSnapshot != null ? lastSnapshot : scanRepository(context);
        
        // Only extract if needed
        if (snapshot.getNodes().isEmpty()) {
            List<String> candidates = curator.selectContext(snapshot, request, 16);
            extractor.extractToSnapshot(snapshot, candidates);
        }
        
        // Update model incrementally
        TargetRealityModel model = buildOrUpdateModel(snapshot, request, null, context);
        
        // Focus on hotspots related to build failures
        List<Hotspot> hotspots = identifyHotspots(snapshot, model, currentVariant, context);
        
        MediationCandidate winner = createQuickCandidate(snapshot, model, hotspots);
        
        return new MediationResult(snapshot, model, hotspots, List.of(winner), winner, null, 0);
    }
    
    private TargetSnapshot scanRepository(TaskContext context) {
        File projectRoot = context.getProjectRoot();
        TargetSnapshot.TargetType type = projectRoot.getAbsolutePath().contains("evolution") ? 
            TargetSnapshot.TargetType.SELF : TargetSnapshot.TargetType.PROJECT;
        return scanner.scanToSnapshot(projectRoot, type);
    }
    
    private TargetRealityModel buildOrUpdateModel(TargetSnapshot snapshot, String request, 
            MediationDelta delta, TaskContext context) {
        
        TargetRealityModel model = lastModel != null ? lastModel : new TargetRealityModel();
        
        // Update with new snapshot data
        model.setFilesScanned(snapshot.getNodes().size());
        model.setRealityCompleteness(calculateCompleteness(snapshot));
        
        // Infer domain from request if not set
        if (model.getDomain() == null || model.getDomain().isEmpty()) {
            model.setDomain(inferDomain(request, snapshot));
        }
        
        // Update architecture summary
        if (delta != null && delta.hasChanges()) {
            model.setArchitectureSummary(updateArchitectureSummary(model, delta));
        }
        
        return model;
    }
    
    private List<Hotspot> identifyHotspots(TargetSnapshot snapshot, TargetRealityModel model,
            BranchVariant currentVariant, TaskContext context) {
        
        List<Hotspot> hotspots = new ArrayList<>();
        
        // 1. Files with high change frequency from repository
        // 2. Areas with compilation errors
        // 3. Files that are part of the current variant
        // 4. Files with high complexity
        
        // Add some default hotspots for demonstration
        Hotspot defaultHotspot = new Hotspot();
        defaultHotspot.setName("Main Implementation");
        defaultHotspot.setFile("src/main/java/com/example/App.java");
        defaultHotspot.setType("CORE");
        defaultHotspot.setDescription("Primary implementation area");
        defaultHotspot.setSignificance(0.8);
        hotspots.add(defaultHotspot);
        
        return hotspots;
    }
    
    private List<MediationCandidate> generateCandidates(TargetSnapshot snapshot, 
            TargetRealityModel model, List<Hotspot> hotspots, TaskContext context) {
        
        List<MediationCandidate> candidates = new ArrayList<>();
        
        // Generate candidates based on hotspots and model
        // This should use the LLM to generate contextual candidates
        
        // Create default candidate
        MediationCandidate defaultCandidate = new MediationCandidate();
        defaultCandidate.setPrompt("Analyze repository structure and identify key components");
        defaultCandidate.setArchitectureSummary(model.getArchitectureSummary());
        defaultCandidate.setDependencies("Java 8+, Maven");
        defaultCandidate.setExecutionInstructions("Run with: mvn clean install");
        defaultCandidate.setEvaluation("Expected to compile successfully");
        
        // Add selected files from hotspots
        for (Hotspot hotspot : hotspots) {
            if (hotspot.getFile() != null && !hotspot.getFile().isEmpty()) {
                defaultCandidate.getSelectedFiles().add(hotspot.getFile());
            }
        }
        
        candidates.add(defaultCandidate);
        
        // Add alternative candidate
        MediationCandidate altCandidate = new MediationCandidate();
        altCandidate.setPrompt("Focus on test coverage and error handling");
        altCandidate.setArchitectureSummary("Test-first approach with comprehensive error handling");
        altCandidate.setDependencies("Java 8+, JUnit 5");
        altCandidate.setExecutionInstructions("Run tests with: mvn test");
        altCandidate.setEvaluation("Expected tests to pass");
        candidates.add(altCandidate);
        
        return candidates;
    }
    
    private MediationCandidate selectCandidate(List<MediationCandidate> candidates,
            BranchVariant currentVariant, TaskContext context) {
        
        if (candidates == null || candidates.isEmpty()) {
            return createDefaultCandidate();
        }
        
        // Simple selection: return first candidate
        // In production, this would use the LLM to rank candidates
        return candidates.get(0);
    }
    
    private MediationDelta calculateDelta(TargetSnapshot old, TargetSnapshot current) {
        MediationDelta delta = new MediationDelta();
        if (old == null || current == null) {
            delta.setHasChanges(true);
            delta.setSummary("Initial mediation");
            return delta;
        }
        
        // Compare snapshots
        // Track added/modified/removed files
        // Track semantic changes
        
        return delta;
    }
    
    private MediationCandidate createDefaultCandidate() {
        MediationCandidate candidate = new MediationCandidate();
        candidate.setPrompt("Analyze repository structure and identify key components");
        candidate.setArchitectureSummary("Standard Java application structure");
        candidate.setDependencies("Java 8+, Maven");
        candidate.setExecutionInstructions("mvn clean install");
        return candidate;
    }
    
    private MediationCandidate createQuickCandidate(TargetSnapshot snapshot, 
            TargetRealityModel model, List<Hotspot> hotspots) {
        
        MediationCandidate candidate = new MediationCandidate();
        candidate.setArchitectureSummary(model.getArchitectureSummary());
        candidate.setPrompt("Quick mediation for self-dev: " + model.getDomain());
        candidate.setDependencies("Standard dependencies");
        candidate.setExecutionInstructions("mvn clean compile");
        
        // Add top hotspots as selected files
        if (hotspots != null) {
            hotspots.stream()
                .limit(8)
                .forEach(h -> {
                    if (h.getFile() != null && !h.getFile().isEmpty()) {
                        candidate.getSelectedFiles().add(h.getFile());
                    }
                });
        }
        
        return candidate;
    }
    
    private double calculateCompleteness(TargetSnapshot snapshot) {
        // Calculate based on node coverage
        return Math.min(1.0, snapshot.getNodes().size() / 100.0);
    }
    
    private String inferDomain(String request, TargetSnapshot snapshot) {
        // Simple heuristic
        if (request.toLowerCase().contains("java") || 
            snapshot.getMetadata().containsKey("detectedTechnologies")) {
            return "JAVA";
        }
        return "UNKNOWN";
    }
    
    private String updateArchitectureSummary(TargetRealityModel model, MediationDelta delta) {
        String current = model.getArchitectureSummary();
        if (current == null || current.isEmpty()) {
            return "Initial architecture discovery";
        }
        return current + " (Updated with " + delta.getChangedFiles().size() + " changes)";
    }
}