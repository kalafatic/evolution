package eu.kalafatic.evolution.controller.agents;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact;
import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalGene;
import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalUseCase;
import eu.kalafatic.evolution.controller.mediation.model.Hotspot;
import eu.kalafatic.evolution.controller.mediation.model.KnowledgeGap;
import eu.kalafatic.evolution.controller.mediation.model.SemanticEdge;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
import eu.kalafatic.evolution.controller.mediation.model.Subsystem;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;

/**
 * Reality Discovery Agent.
 * Synthesizes a formal TargetRealityModel from repository evidence and semantic snapshots.
 * Ensures evolution is grounded in discovered reality rather than generic assumptions.
 */
public class RealityDiscoveryAgent extends BaseAiAgent {

    public RealityDiscoveryAgent(SessionContainer container) {
        super("RealityDiscovery", "RealityDiscovery", container);
    }

    @Override
    protected String getAgentInstructions() {
        return "You are a Reality Discovery Agent specialized in Recursive Reality Reconstruction of any information landscape.\n" +
               "Your goal is deep comprehension of content, meaning, and relationships that remains executable on small local models.\n" +
               "Think about INFORMATION, not just code. Every carrier (Markdown, PDF, Image, Code) is just a knowledge container.\n" +
               "Small models participate in a recursive discovery process that gradually reconstructs the target reality.\n" +
               "Never ask to explain the entire system. Instead ask 'What is the most important fact or knowledge gap not yet resolved?'\n" +
               "Identify evidence, uncertainty, and knowledge gaps.\n" +
               "STRICT RULE: Only use discovered evidence. Do NOT compensate with invention.\n" +
               "MULTI-MODAL RULE: Images are first-class knowledge. Extract objects, UI screenshots, diagrams, architecture, tables, charts, logos, and text (OCR).";
    }

    public TargetRealityModel discover(String goal, TaskContext context, String targetPath) throws Exception {
        return discover(goal, context, targetPath, new TargetRealityModel());
    }

    public TargetRealityModel discover(String goal, TaskContext context, String targetPath, TargetRealityModel model) throws Exception {
        long startTime = System.currentTimeMillis();
        context.log("[DISCOVERY] Starting Recursive Reality Reconstruction for: " + goal);
        context.log("[ARCH] Target path: " + targetPath);

        // 1. Structural Scan to Snapshot
        eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner scanner = new eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner();
        File root = new File(targetPath);
        eu.kalafatic.evolution.controller.mediation.scanner.TargetClassificationDetector.ClassificationResult classResult =
                eu.kalafatic.evolution.controller.mediation.scanner.TargetClassificationDetector.detect(root);
        TargetSnapshot.TargetType type = classResult.getCategory();
        TargetSnapshot snapshot = scanner.scanToSnapshot(root, type);

        model.setTargetCategory(type);
        model.setClassificationConfidence(classResult.getConfidence());
        model.setTargetNature(classResult.getSummary());

        // Null-safety fallback for ExecutionProfile
        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile = context.getExecutionProfile();
        if (profile == null) {
            profile = eu.kalafatic.evolution.controller.kernel.EvolutionIntensityCalculator.calculate(context, null, null);
            context.getOrchestrationState().setExecutionProfile(profile);
        }
        int intensity = profile.getIntensity();

        // PASS 1 - METADATA LOADING
        context.log("[DISCOVERY] Pass 1: Metadata Loading.");
        try {
            loadAllMetadata(snapshot, context, model);
        } catch (Exception ex) {
            context.log("[DISCOVERY] Metadata loading note: " + ex.getMessage());
        }

        // PASS 2 - LOCAL RESPONSIBILITY DISCOVERY
        if (intensity >= 2 || !snapshot.getNodes().isEmpty()) {
            try {
                discoverLocalResponsibilities(snapshot, model, context);
            } catch (Exception ex) {
                context.log("[DISCOVERY] Pass 2 degraded: " + ex.getMessage() + ". Continuing with deterministic artifact knowledge.");
            }
        }

        // PASS 3 - RELATIONSHIP DISCOVERY & EVOLUTIONARY INFLUENCE
        if (intensity >= 3) {
            try {
                discoverRelationships(snapshot, model, context);
            } catch (Exception ex) {
                context.log("[DISCOVERY] Pass 3 degraded: " + ex.getMessage() + ". Continuing with topological relationships.");
            }
        }

        // PASS 4 - SUBSYSTEM DISCOVERY
        if (intensity >= 3) {
            try {
                discoverSubsystems(snapshot, model, context);
            } catch (Exception ex) {
                context.log("[DISCOVERY] Pass 4 degraded: " + ex.getMessage() + ". Deriving subsystems from directory structure.");
                deriveSubsystemsFromSnapshot(snapshot, model);
            }
        }

        // PASS 5 - REALITY DISCOVERY (Synthesis & Completeness Tracking)
        try {
            synthesizeReality(goal, snapshot, model, context);
        } catch (Exception ex) {
            context.log("[DISCOVERY] Pass 5 degraded: " + ex.getMessage() + ". Synthesizing reality from discovered evidence.");
            synthesizeRealityFromEvidence(goal, snapshot, model, context);
        }

        // PASS 6 - GENOME DISCOVERY (Portable Patterns)
        if (intensity >= 4) {
            try {
                discoverGenome(model, context);
            } catch (Exception ex) {
                context.log("[DISCOVERY] Pass 6 degraded: " + ex.getMessage());
            }
        }

        // PASS 7 - ARCHITECTURAL COMPRESSION
        if (intensity >= 2) {
            try {
                compressUnderstanding(model, context);
            } catch (Exception ex) {
                context.log("[DISCOVERY] Pass 7 degraded: " + ex.getMessage());
                if (!model.getDimensions().containsKey("information_essence")) {
                    model.getDimensions().put("information_essence", model.getContentLandscapeSummary() != null ? model.getContentLandscapeSummary() : model.getPurpose());
                }
            }
        }

        // PASS 8 - USE CASE DISCOVERY
        if (intensity >= 3) {
            try {
                discoverUseCases(model, context);
            } catch (Exception ex) {
                context.log("[DISCOVERY] Pass 8 degraded: " + ex.getMessage());
            }
        }

        // POPULATE CANONICAL PROJECTIONS
        populateCanonicalFields(snapshot, model, context, goal);

        // KNOWLEDGE GAP IDENTIFICATION
        identifyKnowledgeGaps(snapshot, model, context);

        // DETECT PARTIAL ANALYSIS
        detectPartialAnalysis(snapshot, model, context);

        // POPULATE DISCOVERY REPORT
        populateDiscoveryReport(snapshot, model, startTime, context);

        return model;
    }

    private void populateCanonicalFields(TargetSnapshot snapshot, TargetRealityModel model, TaskContext context, String goal) throws Exception {
        context.log("[DISCOVERY] Populating Canonical Reconstruction Fields.");
        if (snapshot == null) return;

        // 1. Influence Graph
        for (SemanticNode node : snapshot.getNodes().values()) {
            if (node.getEvolutionaryInfluenceScore() > 0) {
                model.getInfluenceGraph().put(node.getId(), node.getEvolutionaryInfluenceScore());
            }
        }

        // 2. Flows & Decision Centers (Derived from Pass 5 synthesis or explicit analysis)
        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile = context.getExecutionProfile();
        int intensity = (profile != null) ? profile.getIntensity() : 2;

        if (intensity >= 2) {
            StringBuilder sb = new StringBuilder();
            sb.append("FACTS: ").append(model.getArchitecturalFacts().size()).append("\n");
            sb.append("SUBSYSTEMS: ").append(model.getSubsystems().size()).append("\n");
            sb.append("\nIdentify core Execution Flows and Decision Flows (3-5 high-level steps each).");

            String prompt = sb.toString() + "\n\n" +
                    "Output a JSON object:\n" +
                    "{\n" +
                    "  \"execution_flows\": [\"string\"],\n" +
                    "  \"decision_flows\": [\"string\"]\n" +
                    "}";

            try {
                String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
                JSONObject obj = JsonUtils.extractJsonObject(response);
                if (obj != null) {
                    JSONArray execs = obj.optJSONArray("execution_flows");
                    if (execs != null) {
                        model.getExecutionFlows().clear();
                        for (int i = 0; i < execs.length(); i++) model.getExecutionFlows().add(execs.getString(i));
                    }
                    JSONArray decs = obj.optJSONArray("decision_flows");
                    if (decs != null) {
                        model.getDecisionFlows().clear();
                        for (int i = 0; i < decs.length(); i++) model.getDecisionFlows().add(decs.getString(i));
                    }
                }
            } catch (Exception ex) {
                context.log("[DISCOVERY] Flow analysis note: " + ex.getMessage());
                if (model.getExecutionFlows().isEmpty()) {
                    model.getExecutionFlows().add("Execution Flow: Discover -> Mediate -> Export");
                }
            }
        }

        // 3. Selected Files (Coverage-Driven via ContextCurator)
        context.log("[DISCOVERY] Running Coverage-Driven Context Selection.");
        eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator curator = new eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator();
        List<String> selected = curator.selectContext(snapshot, goal, 64, model);
        model.getSelectedFiles().clear();
        model.getSelectedFiles().addAll(selected);
    }

    private void discoverLocalResponsibilities(TargetSnapshot snapshot, TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 2: Local Information Discovery.");
        if (snapshot == null) return;

        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile = context.getExecutionProfile();
        int intensity = (profile != null) ? profile.getIntensity() : 2;
        int maxCandidates = intensity <= 3 ? 3 : 5;

        // Prioritize hotspots and high-centrality nodes
        List<SemanticNode> candidates = getTopCentralNodes(snapshot, maxCandidates);
        for (SemanticNode node : candidates) {
            // Check if we already have facts about this node
            boolean known = model.getArchitecturalFacts().stream().anyMatch(f -> f.getEvidence().contains(node.getPath()));
            if (known) continue;

            context.log("[DISCOVERY] Analyzing meaning of: " + node.getPath());

            if (node.getSummary() == null || node.getSummary().isEmpty()) {
                String sampleSummary = extractArtifactContentSummary(new File(context.getProjectRoot(), node.getPath()), context);
                if (sampleSummary != null && !sampleSummary.isEmpty()) {
                    node.setSummary(sampleSummary);
                }
            }

            // OPTIMIZATION: If we already have rich summary (e.g. from Pass 1 or file extraction),
            // reuse local summary as an architectural fact without issuing expensive LLM calls for every file
            if (!"image".equals(node.getType()) && node.getSummary() != null && !node.getSummary().isEmpty()
                    && ("model".equals(node.getType()) || node.getPath().endsWith(".json") || "dataset".equals(node.getType()))) {
                ArchitecturalFact fact = new ArchitecturalFact(node.getId() + "-summary", node.getPath(),
                        "content: " + limitString(node.getSummary(), 120), 0.85);
                fact.getEvidence().add(node.getPath());
                model.addArchitecturalFact(fact);
                continue;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("ARTIFACT: ").append(node.getPath()).append("\n");
            sb.append("TYPE: ").append(node.getType()).append("\n");
            sb.append("SUMMARY: ").append(node.getSummary()).append("\n");
            sb.append("DEPENDENCIES/RELATIONS: ").append(node.getDependencies()).append("\n\n");

            String prompt;
            if ("image".equals(node.getType())) {
                sb.append("Analyze this IMAGE artifact. Identify objects, diagrams, UI structures, charts, logos, and OCR text.");
                prompt = sb.toString() + "\n\n" +
                    "Output a JSON object:\n" +
                    "{\n" +
                    "  \"meaning\": [\"string\"],\n" +
                    "  \"extracted_text\": \"string\",\n" +
                    "  \"detected_elements\": [\"string\"],\n" +
                    "  \"concepts\": [\"string\"],\n" +
                    "  \"role_candidate\": \"string\"\n" +
                    "}";
            } else {
                sb.append("Identify responsibilities, meaning, decisions, and concepts for this artifact.");
                prompt = sb.toString() + "\n\n" +
                    "Output a JSON object:\n" +
                    "{\n" +
                    "  \"meaning\": [\"string\"],\n" +
                    "  \"decisions\": [\"string\"],\n" +
                    "  \"concepts\": [\"string\"],\n" +
                    "  \"role_candidate\": \"string\"\n" +
                    "}";
            }

            String response = null;
            try {
                if ("image".equals(node.getType())) {
                    String imagePath = new File(context.getProjectRoot(), node.getPath()).getAbsolutePath();
                    try {
                        response = aiService.sendImageRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, imagePath, context);
                    } catch (Exception ex) {
                        if (context != null) {
                            context.log("[DISCOVERY] Multi-modal image analysis failed for " + node.getPath() + " (" + ex.getMessage() + "). Falling back to text request.");
                        }
                        response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
                    }
                } else {
                    response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
                }
            } catch (Exception ex) {
                if (context != null) {
                    context.log("[DISCOVERY] LLM analysis for " + node.getPath() + " skipped (" + ex.getMessage() + "). Using local summary.");
                }
            }

            if (response != null) {
                JSONObject obj = JsonUtils.extractJsonObject(response);
                if (obj != null) {
                    JSONArray meanings = obj.optJSONArray("meaning");
                    if (meanings != null) {
                        for (int i = 0; i < meanings.length(); i++) {
                            ArchitecturalFact fact = new ArchitecturalFact(node.getId() + "-meaning-" + i, node.getPath(), "meaning: " + meanings.getString(i), 0.8);
                            fact.getEvidence().add(node.getPath());
                            model.addArchitecturalFact(fact);
                        }
                    }
                    String role = obj.optString("role_candidate");
                    if (!role.isEmpty()) {
                        ArchitecturalFact roleFact = new ArchitecturalFact(node.getId() + "-role", node.getPath(), "acts as " + role, 0.7);
                        roleFact.getEvidence().add(node.getPath());
                        model.addArchitecturalFact(roleFact);
                    }

                    String ocr = obj.optString("extracted_text");
                    if (!ocr.isEmpty()) {
                        node.getAttributes().put("ocr_text", ocr);
                        ArchitecturalFact ocrFact = new ArchitecturalFact(node.getId() + "-ocr", node.getPath(), "contains text: " + (ocr.length() > 50 ? ocr.substring(0, 50) + "..." : ocr), 0.9);
                        ocrFact.getEvidence().add(node.getPath());
                        model.addArchitecturalFact(ocrFact);
                    }
                }
            } else if (node.getSummary() != null && !node.getSummary().isEmpty()) {
                // Deterministic fact fallback from verified local summary
                ArchitecturalFact fallbackFact = new ArchitecturalFact(node.getId() + "-summary", node.getPath(), "content: " + limitString(node.getSummary(), 100), 0.85);
                fallbackFact.getEvidence().add(node.getPath());
                model.addArchitecturalFact(fallbackFact);
            }
        }
    }

    private void discoverRelationships(TargetSnapshot snapshot, TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 3: Relationship Discovery & Semantic Influence.");
        if (snapshot == null) return;

        eu.kalafatic.evolution.controller.kernel.EvolutionProfile profile = context.getExecutionProfile();
        int intensity = (profile != null) ? profile.getIntensity() : 2;
        int maxNodes = intensity <= 3 ? 3 : 5;

        List<SemanticNode> influentialNodes = getTopCentralNodes(snapshot, maxNodes);
        for (SemanticNode node : influentialNodes) {
            // Skip if relationships or influence for this node have already been analyzed
            boolean knownRel = model.getArchitecturalFacts().stream().anyMatch(f -> f.getEvidence().contains(node.getPath()) && f.getPredicate() != null && f.getPredicate().contains(":"));
            if (knownRel) continue;

            context.log("[DISCOVERY] Analyzing relationships and influence for: " + node.getPath());

            List<SemanticEdge> incoming = snapshot.getEdges().stream().filter(e -> e.getTargetId().equals(node.getId())).collect(Collectors.toList());
            List<SemanticEdge> outgoing = snapshot.getEdges().stream().filter(e -> e.getSourceId().equals(node.getId())).collect(Collectors.toList());

            StringBuilder sb = new StringBuilder();
            sb.append("NODE: ").append(node.getPath()).append("\n");
            sb.append("INCOMING RELATIONS: ").append(incoming.size()).append("\n");
            sb.append("OUTGOING RELATIONS: ").append(outgoing.size()).append("\n");
            sb.append("Identify control, ownership, orchestration, coordination, or semantic connection.\n");
            sb.append("Analyze Semantic Leverage: If this information changes, what knowledge is lost? What evolves? What behavior changes?");

            String prompt = sb.toString() + "\n\n" +
                    "Output a JSON object:\n" +
                    "{\n" +
                    "  \"relationships\": [\n" +
                    "    {\n" +
                    "      \"type\": \"control|ownership|orchestration|coordination|semantic_connection\",\n" +
                    "      \"subject\": \"string\",\n" +
                    "      \"description\": \"string\",\n" +
                    "      \"impact_if_removed\": \"string\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"semantic_leverage\": {\n" +
                    "    \"influence_score\": 0.0-1.0,\n" +
                    "    \"knowledge_loss_impacts\": [\"string\"],\n" +
                    "    \"evolution_potentials\": [\"string\"]\n" +
                    "  }\n" +
                    "}";

            String response = null;
            try {
                response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
            } catch (Exception ex) {
                context.log("[DISCOVERY] Relationship analysis for " + node.getPath() + " skipped: " + ex.getMessage());
            }

            if (response != null) {
                JSONObject obj = JsonUtils.extractJsonObject(response);
                if (obj != null) {
                    JSONArray rels = obj.optJSONArray("relationships");
                    if (rels != null) {
                        for (int i = 0; i < rels.length(); i++) {
                            JSONObject rObj = rels.optJSONObject(i);
                            if (rObj == null) continue;
                            ArchitecturalFact fact = new ArchitecturalFact(node.getId() + "-rel-" + i, node.getPath(), rObj.optString("type") + ": " + rObj.optString("description"), 0.85);
                            fact.setDescription("Impact if removed: " + rObj.optString("impact_if_removed"));
                            fact.getEvidence().add(node.getPath());
                            model.addArchitecturalFact(fact);
                        }
                    }

                    JSONObject lev = obj.optJSONObject("semantic_leverage");
                    if (lev != null) {
                        node.setEvolutionaryInfluenceScore(lev.optDouble("influence_score", 0.5));
                        JSONArray breaks = lev.optJSONArray("knowledge_loss_impacts");
                        if (breaks != null) for (int i = 0; i < breaks.length(); i++) node.addBreakImpact(breaks.getString(i));
                        JSONArray evs = lev.optJSONArray("evolution_potentials");
                        if (evs != null) for (int i = 0; i < evs.length(); i++) node.addEvolutionPotential(evs.getString(i));
                    }
                }
            } else if (!incoming.isEmpty() || !outgoing.isEmpty()) {
                // Deterministic topological relationship fact
                ArchitecturalFact topoFact = new ArchitecturalFact(node.getId() + "-topo", node.getPath(),
                        "relations: in=" + incoming.size() + ", out=" + outgoing.size(), 0.7);
                topoFact.getEvidence().add(node.getPath());
                model.addArchitecturalFact(topoFact);
            }
        }
    }

    private void discoverSubsystems(TargetSnapshot snapshot, TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 4: Subsystem / Component Cluster Discovery.");

        StringBuilder sb = new StringBuilder();
        sb.append("TARGET CATEGORY: ").append(model.getTargetCategory()).append("\n");
        sb.append("EXISTING ARCHITECTURAL FACTS:\n");
        model.getArchitecturalFacts().stream().limit(20).forEach(f -> sb.append("- ").append(f.toString()).append("\n"));

        if (model.getTargetCategory() == TargetSnapshot.TargetType.DATASET) {
            sb.append("\nIdentify major Data Categories, Splits, or Feature Clusters for this dataset.");
        } else if (model.getTargetCategory() == TargetSnapshot.TargetType.KNOWLEDGE_BASE) {
            sb.append("\nIdentify major Topic Areas, Document Sections, or Knowledge Hubs.");
        } else if (model.getTargetCategory() == TargetSnapshot.TargetType.AI_MODEL) {
            sb.append("\nIdentify Model Sub-Components, Layers, or Configuration Groups.");
        } else {
            sb.append("\nIdentify strongly interacting software clusters and define Subsystems.");
        }

        String prompt = sb.toString() + "\n\n" +
                "Output a JSON object:\n" +
                "{\n" +
                "  \"subsystems\": [\n" +
                "    {\n" +
                "      \"name\": \"string\",\n" +
                "      \"purpose\": \"string\",\n" +
                "      \"boundaries\": [\"string\"],\n" +
                "      \"critical_files\": [\"string\"],\n" +
                "      \"responsibilities\": [\"string\"]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String response = null;
        try {
            response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
        } catch (Exception ex) {
            context.log("[DISCOVERY] Subsystem analysis note: " + ex.getMessage());
        }

        if (response != null) {
            JSONObject obj = JsonUtils.extractJsonObject(response);
            if (obj != null) {
                JSONArray subs = obj.optJSONArray("subsystems");
                if (subs != null) {
                    for (int i = 0; i < subs.length(); i++) {
                        JSONObject sObj = subs.optJSONObject(i);
                        if (sObj == null) continue;
                        Subsystem sub = new Subsystem("sub-" + i, sObj.optString("name"), 0.8);
                        sub.setPurpose(sObj.optString("purpose"));
                        JSONArray bounds = sObj.optJSONArray("boundaries");
                        if (bounds != null) for (int j = 0; j < bounds.length(); j++) sub.getBoundaries().add(bounds.getString(j));
                        JSONArray files = sObj.optJSONArray("critical_files");
                        if (files != null) for (int j = 0; j < files.length(); j++) sub.getCriticalFiles().add(files.getString(j));
                        JSONArray resps = sObj.optJSONArray("responsibilities");
                        if (resps != null) for (int j = 0; j < resps.length(); j++) sub.getResponsibilities().add(resps.getString(j));
                        model.addSubsystem(sub);
                    }
                }
            }
        }
        if (model.getSubsystems().isEmpty()) {
            deriveSubsystemsFromSnapshot(snapshot, model);
        }
    }

    private void deriveSubsystemsFromSnapshot(TargetSnapshot snapshot, TargetRealityModel model) {
        if (snapshot == null || snapshot.getNodes().isEmpty()) return;
        java.util.Map<String, List<String>> folderClusters = new java.util.HashMap<>();
        for (SemanticNode node : snapshot.getNodes().values()) {
            String path = node.getPath().replace('\\', '/');
            int idx = path.indexOf('/');
            String top = (idx > 0) ? path.substring(0, idx) : "root";
            folderClusters.computeIfAbsent(top, k -> new ArrayList<>()).add(path);
        }

        int subIdx = 0;
        for (java.util.Map.Entry<String, List<String>> entry : folderClusters.entrySet()) {
            Subsystem sub = new Subsystem("sub-" + (subIdx++), entry.getKey(), 0.75);
            sub.setPurpose("Cluster for " + entry.getKey() + " artifacts (" + entry.getValue().size() + " files)");
            sub.getCriticalFiles().addAll(entry.getValue().stream().limit(5).collect(Collectors.toList()));
            sub.getBoundaries().addAll(entry.getValue().stream().limit(2).collect(Collectors.toList()));
            model.addSubsystem(sub);
        }
    }

    private void synthesizeReality(String goal, TargetSnapshot snapshot, TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 5: Reality Synthesis & Completeness Tracking.");

        StringBuilder sb = new StringBuilder();
        sb.append("GOAL: ").append(goal).append("\n");
        sb.append("FACTS: ").append(model.getArchitecturalFacts().size()).append("\n");
        sb.append("SUBSYSTEMS: ").append(model.getSubsystems().size()).append("\n");
        sb.append("KNOWLEDGE GAPS: ").append(model.getKnowledgeGaps().size()).append("\n");
        sb.append("\nSynthesize Information Reality Model, discover core facts/hotspots, and estimate Completeness (0.0-1.0).\n");
        sb.append("Also organize the model into KnowledgeView, StructureView, and GenomeView.");

        String prompt = sb.toString() + "\n\n" +
                "Output a JSON object:\n" +
                "{\n" +
                "  \"domain\": \"string\",\n" +
                "  \"purpose\": \"string\",\n" +
                "  \"landscape_summary\": \"string\",\n" +
                "  \"completeness_score\": 0.0-1.0,\n" +
                "  \"information_facts\": [{ \"id\": \"string\", \"subject\": \"string\", \"predicate\": \"string\", \"description\": \"string\" }],\n" +
                "  \"hotspots\": [{ \"id\": \"string\", \"name\": \"string\", \"description\": \"string\", \"significance\": 0.0-1.0, \"related_artifacts\": [\"string\"] }],\n" +
                "  \"views\": {\n" +
                "    \"knowledge\": {\"hubs\": [\"string\"], \"concepts\": [\"string\"]},\n" +
                "    \"structure\": {\"entry_points\": [\"string\"], \"critical_path\": [\"string\"]},\n" +
                "    \"genome\": {\"core_patterns\": [\"string\"]}\n" +
                "  }\n" +
                "}";

        String response = null;
        try {
            response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
        } catch (Exception ex) {
            context.log("[DISCOVERY] Pass 5 LLM request note: " + ex.getMessage());
        }

        if (response != null) {
            JSONObject obj = JsonUtils.extractJsonObject(response);
            if (obj != null) {
                model.setDomain(obj.optString("domain"));
                model.setPurpose(obj.optString("purpose"));
                model.setContentLandscapeSummary(obj.optString("landscape_summary"));
                model.setRealityCompleteness(obj.optDouble("completeness_score", 0.5));

                // Extract additional facts if discovered (supporting both information_facts and architectural_facts)
                JSONArray facts = obj.optJSONArray("information_facts");
                if (facts == null) facts = obj.optJSONArray("architectural_facts");
                if (facts != null) {
                    for (int i = 0; i < facts.length(); i++) {
                        JSONObject fObj = facts.optJSONObject(i);
                        if (fObj == null) continue;
                        ArchitecturalFact fact = new ArchitecturalFact(fObj.optString("id"), fObj.optString("subject"), fObj.optString("predicate"), 0.9);
                        fact.setDescription(fObj.optString("description"));
                        model.addArchitecturalFact(fact);
                    }
                }

                // Extract hotspots
                JSONArray hotspots = obj.optJSONArray("hotspots");
                if (hotspots != null) {
                    for (int i = 0; i < hotspots.length(); i++) {
                        JSONObject hObj = hotspots.optJSONObject(i);
                        if (hObj == null) continue;

                        Hotspot hotspot = new Hotspot(hObj.optString("id"), hObj.optString("name"), response, i);
                        hotspot.setDescription(hObj.optString("description"));
                        hotspot.setSignificance(hObj.optDouble("significance", 0.5));

                        JSONArray related = hObj.optJSONArray("related_artifacts");
                        if (related != null) {
                            for (int j = 0; j < related.length(); j++) {
                                hotspot.getRelatedArtifactsList().add(related.getString(j));
                            }
                        }
                        model.addHotspot(hotspot);
                    }
                }

                JSONObject views = obj.optJSONObject("views");
                if (views != null) {
                    JSONObject kno = views.optJSONObject("knowledge");
                    if (kno != null) {
                        java.util.Iterator<String> keys = kno.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            model.getKnowledgeView().put(key, kno.get(key));
                        }
                    }
                    JSONObject str = views.optJSONObject("structure");
                    if (str != null) {
                        java.util.Iterator<String> keys = str.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            model.getStructureView().put(key, str.get(key));
                        }
                    }
                    JSONObject gen = views.optJSONObject("genome");
                    if (gen != null) {
                        java.util.Iterator<String> keys = gen.keys();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            model.getGenomeView().put(key, gen.get(key));
                        }
                    }
                }
            }
        }

        if (model.getDomain() == null || model.getDomain().isEmpty() || model.getPurpose() == null || model.getPurpose().isEmpty()) {
            synthesizeRealityFromEvidence(goal, snapshot, model, context);
        }
    }

    private void synthesizeRealityFromEvidence(String goal, TargetSnapshot snapshot, TargetRealityModel model, TaskContext context) {
        if (model.getDomain() == null || model.getDomain().isEmpty()) {
            String domain = model.getTargetCategory() != null ? model.getTargetCategory().name() : "SOFTWARE";
            model.setDomain(domain);
        }

        if (model.getPurpose() == null || model.getPurpose().isEmpty()) {
            String nature = model.getTargetNature();
            if (nature != null && !nature.isEmpty()) {
                model.setPurpose(nature);
            } else {
                model.setPurpose("Discovered target repository for goal: " + goal);
            }
        }

        if (model.getContentLandscapeSummary() == null || model.getContentLandscapeSummary().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Target category: ").append(model.getTargetCategory()).append(". ");
            sb.append("Files scanned: ").append(snapshot != null ? snapshot.getNodes().size() : 0).append(". ");
            sb.append("Subsystems: ").append(model.getSubsystems().size()).append(". ");
            sb.append("Discovered facts: ").append(model.getArchitecturalFacts().size()).append(".");
            model.setContentLandscapeSummary(sb.toString());
        }

        if (model.getHotspots().isEmpty() && snapshot != null) {
            List<SemanticNode> top = getTopCentralNodes(snapshot, 5);
            int idx = 0;
            for (SemanticNode node : top) {
                Hotspot h = new Hotspot("hotspot-" + (idx++), node.getPath(), "Discovered node " + node.getPath(), idx);
                h.setDescription("Key artifact: " + node.getPath() + " (" + node.getType() + ")");
                h.setSignificance(0.8);
                h.getRelatedArtifactsList().add(node.getPath());
                model.addHotspot(h);
            }
        }
    }

    private void discoverGenome(TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 6: Semantic Genome Discovery (Portable Patterns/Knowledge).");

        StringBuilder sb = new StringBuilder();
        sb.append("REALITY MODEL: ").append(model.getContentLandscapeSummary()).append("\n");
        sb.append("Identify portable patterns, recurring concepts, and their required carriers.");

        String prompt = sb.toString() + "\n\n" +
                "Output a JSON object:\n" +
                "{\n" +
                "  \"genes\": [\n" +
                "    {\n" +
                "      \"pattern\": \"string\",\n" +
                "      \"purpose\": \"string\",\n" +
                "      \"rationale\": \"string\",\n" +
                "      \"required_carriers\": [\"string\"],\n" +
                "      \"transferability\": \"string\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String response = null;
        try {
            response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
        } catch (Exception ex) {
            context.log("[DISCOVERY] Pass 6 note: " + ex.getMessage());
        }

        if (response != null) {
            JSONObject obj = JsonUtils.extractJsonObject(response);
            if (obj != null) {
                JSONArray genes = obj.optJSONArray("genes");
                if (genes != null) {
                    for (int i = 0; i < genes.length(); i++) {
                        JSONObject gObj = genes.optJSONObject(i);
                        if (gObj == null) continue;
                        ArchitecturalGene gene = new ArchitecturalGene("gene-" + i, gObj.optString("purpose"));
                        gene.setPattern(gObj.optString("pattern"));
                        gene.setRationale(gObj.optString("rationale"));
                        JSONArray reqs = gObj.optJSONArray("required_carriers");
                        if (reqs == null) reqs = gObj.optJSONArray("required_artifacts");
                        if (reqs != null) for (int j = 0; j < reqs.length(); j++) gene.getRequiredArtifacts().add(reqs.getString(j));
                        gene.setTransferability(gObj.optString("transferability"));
                        model.addGene(gene);
                    }
                }
            }
        }
    }

    private void discoverUseCases(TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 8: Capability & Use Case Discovery.");

        StringBuilder sb = new StringBuilder();
        sb.append("TARGET CATEGORY: ").append(model.getTargetCategory()).append("\n");
        sb.append("SUBSYSTEMS / CLUSTERS:\n");
        model.getSubsystems().forEach(s -> sb.append("- ").append(s.getName()).append(": ").append(s.getPurpose()).append("\n"));
        sb.append("\nARCHITECTURAL FACTS:\n");
        model.getArchitecturalFacts().stream().limit(15).forEach(f -> sb.append("- ").append(f.toString()).append("\n"));

        if (model.getTargetCategory() == TargetSnapshot.TargetType.DATASET) {
            sb.append("\nIdentify primary Data Workflows, Model Training/Evaluation Uses, or Feature Transformations derived from this dataset.\n");
        } else if (model.getTargetCategory() == TargetSnapshot.TargetType.KNOWLEDGE_BASE) {
            sb.append("\nIdentify primary Information Needs, Query Intents, or Knowledge Workflows.\n");
        } else {
            sb.append("\nIdentify major repository-wide Use Cases derived from these architectural structures.\n");
        }
        sb.append("Every use case / capability MUST have supporting components, supporting files (evidence), and a confidence score.");

        String prompt = sb.toString() + "\n\n" +
                "Output a JSON object:\n" +
                "{\n" +
                "  \"use_cases\": [\n" +
                "    {\n" +
                "      \"name\": \"string\",\n" +
                "      \"description\": \"string\",\n" +
                "      \"supporting_components\": [\"string\"],\n" +
                "      \"supporting_files\": [\"string\"],\n" +
                "      \"confidence\": 0.0-1.0,\n" +
                "      \"rationale\": \"string\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String response = null;
        try {
            response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
        } catch (Exception ex) {
            context.log("[DISCOVERY] Pass 8 note: " + ex.getMessage());
        }

        if (response != null) {
            JSONObject obj = JsonUtils.extractJsonObject(response);
            if (obj != null) {
                JSONArray ucs = obj.optJSONArray("use_cases");
                if (ucs != null) {
                    for (int i = 0; i < ucs.length(); i++) {
                        JSONObject uObj = ucs.optJSONObject(i);
                        if (uObj == null) continue;
                        ArchitecturalUseCase uc = new ArchitecturalUseCase("uc-" + i, uObj.optString("name"), uObj.optString("description"), uObj.optDouble("confidence", 0.5));
                        JSONArray comps = uObj.optJSONArray("supporting_components");
                        if (comps != null) for (int j = 0; j < comps.length(); j++) uc.getSupportingComponents().add(comps.getString(j));
                        JSONArray files = uObj.optJSONArray("supporting_files");
                        if (files != null) for (int j = 0; j < files.length(); j++) uc.getSupportingFiles().add(files.getString(j));
                        uc.setRationale(uObj.optString("rationale"));
                        model.addUseCase(uc);
                    }
                }
            }
        }
        if (model.getUseCases().isEmpty() && model.getTargetCategory() == TargetSnapshot.TargetType.AI_MODEL) {
            ArchitecturalUseCase uc = new ArchitecturalUseCase("uc-model-inference", "Local Model Inference & Evaluation",
                    "Execute inference using the model definition and trained weights.", 0.9);
            uc.getSupportingComponents().add("Modelfile");
            uc.getSupportingFiles().add("Modelfile");
            uc.setRationale("Model configuration and weights discovered.");
            model.addUseCase(uc);
        }
    }

    private void compressUnderstanding(TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 7: Information Compression.");

        StringBuilder sb = new StringBuilder();
        sb.append("TARGET REALITY MODEL:\n");
        sb.append("- Domain: ").append(model.getDomain()).append("\n");
        sb.append("- Subsystems/Clusters: ").append(model.getSubsystems().size()).append("\n");
        sb.append("- Facts: ").append(model.getArchitecturalFacts().size()).append("\n");
        sb.append("- Genes: ").append(model.getGenes().size()).append("\n");
        sb.append("\nCompress these observations into high-signal information facts and genes while preserving understanding.");

        String prompt = sb.toString() + "\n\n" +
                "Summarize the core essence of this information landscape in 3 paragraphs.";

        String response = null;
        try {
            response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
        } catch (Exception ex) {
            context.log("[DISCOVERY] Pass 7 note: " + ex.getMessage());
        }
        if (response != null && !response.isBlank()) {
            model.getDimensions().put("information_essence", response);
        } else if (model.getContentLandscapeSummary() != null) {
            model.getDimensions().put("information_essence", model.getContentLandscapeSummary());
        }
    }

    private void populateDiscoveryReport(TargetSnapshot snapshot, TargetRealityModel model, long startTime, TaskContext context) {
        if (snapshot != null) {
            model.setFilesScanned(snapshot.getNodes().size());
            model.setArchitectureNodes(model.getHotspots().size() + model.getSubsystems().size() + model.getUseCases().size());
            // Rough estimate of relationships
            int rels = model.getArchitecturalFacts().size();
            model.setArchitectureRelationships(rels);
        }
        model.setAnalysisDurationMs(System.currentTimeMillis() - startTime);

        context.log("[DISCOVERY] Discovery Report generated.");
        context.log("[DISCOVERY] Files Scanned: " + model.getFilesScanned());
        context.log("[DISCOVERY] Metadata Entries: " + model.getMetadataEntries());
        context.log("[DISCOVERY] Architecture Nodes: " + model.getArchitectureNodes());
        context.log("[DISCOVERY] Use Cases: " + model.getUseCases().size());
        context.log("[DISCOVERY] Duration: " + model.getAnalysisDurationMs() + "ms");
    }

    private void detectPartialAnalysis(TargetSnapshot snapshot, TargetRealityModel model, TaskContext context) {
        if (snapshot == null) return;
        int totalNodes = snapshot.getNodes().size();
        if (totalNodes == 0) return;

        int metadataNodes = model.getMetadataEntries();
        long analyzedNodesCount = snapshot.getNodes().values().stream()
                .filter(node -> (node.getSummary() != null && !node.getSummary().isEmpty())
                        || model.getArchitecturalFacts().stream().anyMatch(f -> f.getEvidence().contains(node.getPath())))
                .count();

        int effectiveCovered = (int) Math.max(metadataNodes, analyzedNodesCount);
        double coverage = (double) effectiveCovered / totalNodes;

        if (coverage < 0.2 && totalNodes > 1) {
            String warning = "Analysis covered only " + effectiveCovered + " of " + totalNodes + " source files. Architecture model incomplete.";
            model.setAnalysisWarning(warning);
            context.log("[DISCOVERY] WARNING: " + warning);
            // Reduce confidence of all use cases
            model.getUseCases().forEach(uc -> uc.setConfidence(uc.getConfidence() * 0.5));
            model.setRealityCompleteness(model.getRealityCompleteness() * 0.5);
        } else if (coverage < 0.5 && totalNodes > 1) {
            String warning = "Analysis covered " + String.format("%.1f", coverage * 100) + "% of source files. Results may be partial.";
            model.setAnalysisWarning(warning);
            context.log("[DISCOVERY] WARNING: " + warning);
        }
    }

    private String extractArtifactContentSummary(File file, TaskContext context) {
        if (file == null || !file.exists() || !file.isFile()) return null;
        String name = file.getName().toLowerCase();

        // 1. Native EVO Model file (.evo)
        if (name.endsWith(".evo")) {
            try {
                eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact artifact =
                        eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact.load(file.toPath());
                return String.format("Native EVO LLM Model '%s': %s parameters, Vocab: %d, %d layers, %d heads, embed: %d, dff: %d, maxSeq: %d",
                        artifact.getModelName(), formatParamCount(artifact.getParameterCount()), artifact.getVocabSize(),
                        artifact.getLayers(), artifact.getHeads(), artifact.getEmbeddingSize(), artifact.getDff(), artifact.getMaxSeqLen());
            } catch (Exception ex) {
                return "Native EVO Model Container (" + file.length() + " bytes)";
            }
        }

        // 2. Training report / Model config JSON
        if (name.equals("training-report.json") || name.equals("model.json") || name.equals("config.json")) {
            try {
                String content = java.nio.file.Files.readString(file.toPath());
                org.json.JSONObject obj = new org.json.JSONObject(content);
                StringBuilder sb = new StringBuilder();
                sb.append("EVO Model Configuration / Training Report: ");
                if (obj.has("modelName")) sb.append("Model: ").append(obj.optString("modelName")).append(" | ");
                if (obj.has("parameterCount")) sb.append("Params: ").append(formatParamCount(obj.optLong("parameterCount"))).append(" | ");
                if (obj.has("finalLoss")) sb.append("Loss: ").append(String.format(java.util.Locale.US, "%.4f", obj.optDouble("finalLoss"))).append(" | ");
                if (obj.has("vocabSize")) sb.append("Vocab: ").append(obj.optInt("vocabSize")).append(" | ");
                if (obj.has("numBlocks")) sb.append("Layers: ").append(obj.optInt("numBlocks")).append(" | ");
                if (obj.has("dModel")) sb.append("Embed: ").append(obj.optInt("dModel")).append(" | ");
                return sb.toString().replaceAll(" \\| $", "");
            } catch (Exception ignored) {}
        }

        // 3. Modelfile
        if (name.equals("modelfile") || name.endsWith(".modelfile")) {
            try {
                List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
                StringBuilder sb = new StringBuilder("Ollama Model Definition: ");
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("FROM") || trimmed.startsWith("PARAMETER") || trimmed.startsWith("SYSTEM")) {
                        sb.append(trimmed).append(" | ");
                    }
                }
                return sb.toString().replaceAll(" \\| $", "");
            } catch (Exception ignored) {}
        }

        // 4. GGUF file
        if (name.endsWith(".gguf")) {
            return String.format("Exported GGUF Model weights artifact (%s MB)", file.length() / (1024 * 1024));
        }

        // 5. Dataset adapters via SourceAdapterRegistry (.evodata, .parquet, .jsonl, .csv, etc.)
        try {
            eu.kalafatic.evolution.forge.data.api.source.SourceAdapterRegistry registry =
                    eu.kalafatic.evolution.forge.data.api.source.SourceAdapterRegistry.createDefaultRegistry();
            eu.kalafatic.evolution.forge.data.api.source.DatasetItem item =
                    new eu.kalafatic.evolution.forge.data.api.source.DatasetItem(true, file.getAbsolutePath(), "FILE");
            eu.kalafatic.evolution.forge.data.api.source.DatasetSourceAdapter adapter = registry.findAdapter(item);

            if (adapter != null && !(adapter instanceof eu.kalafatic.evolution.forge.data.impl.source.adapters.FolderAdapter)) {
                eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext prepCtx =
                        new eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext();
                List<eu.kalafatic.evolution.forge.data.api.NormalizedSample> samples = adapter.convert(item, prepCtx);
                if (samples != null && !samples.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Dataset file containing ").append(samples.size()).append("+ extracted samples. Sample records:\n");
                    for (int i = 0; i < Math.min(3, samples.size()); i++) {
                        sb.append(" Sample ").append(i + 1).append(": ").append(limitString(samples.get(i).toFullText(), 200)).append("\n");
                    }
                    return sb.toString();
                }
            }

            // Fallback for general text or readable files
            List<String> lines = java.nio.file.Files.readAllLines(file.toPath());
            if (lines != null && !lines.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append("File containing ").append(lines.size()).append(" records/lines. Sample:\n");
                for (int i = 0; i < Math.min(3, lines.size()); i++) {
                    if (!lines.get(i).trim().isEmpty()) {
                        sb.append(" - ").append(limitString(lines.get(i).trim(), 150)).append("\n");
                    }
                }
                return sb.toString();
            }
        } catch (Exception ex) {
            if (context != null) {
                context.log("[DISCOVERY] Dataset sample extraction note for " + file.getName() + ": " + ex.getMessage());
            }
        }
        return null;
    }

    private static String formatParamCount(long params) {
        if (params >= 1_000_000_000) {
            return String.format("%.2fB", params / 1_000_000_000.0);
        } else if (params >= 1_000_000) {
            return String.format("%.2fM", params / 1_000_000.0);
        } else if (params >= 1_000) {
            return String.format("%.1fK", params / 1_000.0);
        } else {
            return String.valueOf(params);
        }
    }

    private String limitString(String str, int max) {
        if (str == null) return "";
        String trimmed = str.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= max) return trimmed;
        return trimmed.substring(0, max) + "...";
    }

    private void identifyKnowledgeGaps(TargetSnapshot snapshot, TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Identifying Knowledge Gaps (Knowledge Acquisition Focus).");
        model.getKnowledgeGaps().clear();
        if (snapshot == null) return;

        // 1. Identify high-centrality nodes not covered by subsystems or facts (STRICTER)
        List<SemanticNode> topNodes = getTopCentralNodes(snapshot, 32);
        for (SemanticNode node : topNodes) {
            boolean coveredBySubsystem = model.getSubsystems().stream().anyMatch(s -> s.getCriticalFiles().contains(node.getPath()));
            boolean coveredByFact = model.getArchitecturalFacts().stream().anyMatch(f -> f.getEvidence().contains(node.getPath()));

            if (!coveredBySubsystem && !coveredByFact) {
                KnowledgeGap gap = new KnowledgeGap("gap-" + node.getId(), "CRITICAL UNKNOWN: High-influence carrier unmapped: " + node.getPath(), KnowledgeGap.GapType.UNKNOWN_FACT);
                gap.getRelatedArtifacts().add(node.getPath());
                gap.setSignificance(0.95); // Extremely high significance to force discovery
                model.addKnowledgeGap(gap);
            }
        }

        // 2. Detect "Empty" Subsystems (Knowledge Gap in Structure)
        for (Subsystem s : model.getSubsystems()) {
            if (s.getBoundaries().isEmpty() || s.getCriticalFiles().isEmpty()) {
                KnowledgeGap gap = new KnowledgeGap("gap-" + s.getId() + "-structure", "Structural Void: Subsystem '" + s.getName() + "' exists but its files/boundaries are unknown.", KnowledgeGap.GapType.MISSING_EVIDENCE);
                gap.setSignificance(0.8);
                model.addKnowledgeGap(gap);
            }
        }

        // 3. Low-Confidence Pattern Recognition
        for (ArchitecturalGene gene : model.getGenes()) {
            if (gene.getEvidence().isEmpty()) {
                 KnowledgeGap gap = new KnowledgeGap("gap-gene-" + gene.getId(), "Unverified Pattern: Gene '" + gene.getPattern() + "' inferred but lacks supporting file evidence.", KnowledgeGap.GapType.WEAK_FACT);
                 gap.setSignificance(0.75);
                 model.addKnowledgeGap(gap);
            }
        }

        // 4. Global Uncertainty (Incompleteness Gap)
        if (model.getRealityCompleteness() < 0.5) {
             KnowledgeGap gap = new KnowledgeGap("gap-global-completeness", "Knowledge Vacuum: Architecture model completeness is critically low (" + String.format("%.0f%%", model.getRealityCompleteness()*100) + "). Massive recursive discovery required.", KnowledgeGap.GapType.UNKNOWN_FACT);
             gap.setSignificance(1.0);
             model.addKnowledgeGap(gap);
        }

        context.log("[DISCOVERY] Identified " + model.getKnowledgeGaps().size() + " knowledge gaps.");
    }

    private void loadAllMetadata(TargetSnapshot snapshot, TaskContext context, TargetRealityModel model) {
        if (snapshot == null) return;
        eu.kalafatic.utils.semantic.AIContextTool tool = new eu.kalafatic.utils.semantic.AIContextTool();
        File root = new File(snapshot.getRootPath());
        int metadataCount = 0;

        for (SemanticNode node : snapshot.getNodes().values()) {
            File f = new File(root, node.getPath());
            if (f.exists()) {
                eu.kalafatic.utils.semantic.EvoMetadata meta = tool.loadMetadata(f);
                if (meta != null) {
                    node.setSummary(meta.getSummary());
                    node.setArchitecturalAuthority(meta.getImportanceScore());
                    node.setEvolutionaryInfluenceScore(meta.getMediatedRelevanceScore());
                    if (meta.getDependencyLinks() != null) {
                        node.getDependencies().addAll(meta.getDependencyLinks());
                    }
                    metadataCount++;
                } else {
                    // Extract rich deterministic metadata directly from model/dataset/code artifacts
                    String contentSummary = extractArtifactContentSummary(f, context);
                    if (contentSummary != null && !contentSummary.isBlank()) {
                        node.setSummary(contentSummary);
                        boolean isExport = "true".equals(node.getAttributes().get("isExport"));
                        double authority = isExport ? 0.4 : 0.85;
                        double influence = isExport ? 0.3 : 0.8;
                        node.setArchitecturalAuthority(authority);
                        node.setEvolutionaryInfluenceScore(influence);
                        metadataCount++;
                    }
                }
            }
        }
        model.setMetadataEntries(metadataCount);
        context.log("[DISCOVERY] Loaded metadata for " + metadataCount + " / " + snapshot.getNodes().size() + " nodes.");
    }

    private List<SemanticNode> getTopCentralNodes(TargetSnapshot snapshot, int limit) {
        if (snapshot == null) return new ArrayList<>();
        java.util.Map<String, Integer> inDegree = new java.util.HashMap<>();
        java.util.Map<String, Integer> outDegree = new java.util.HashMap<>();
        for (SemanticEdge edge : snapshot.getEdges()) {
            outDegree.merge(edge.getSourceId(), 1, Integer::sum);
            inDegree.merge(edge.getTargetId(), 1, Integer::sum);
        }

        return snapshot.getNodes().values().stream()
            .sorted((n1, n2) -> {
                int d1 = inDegree.getOrDefault(n1.getId(), 0) + outDegree.getOrDefault(n1.getId(), 0);
                int d2 = inDegree.getOrDefault(n2.getId(), 0) + outDegree.getOrDefault(n2.getId(), 0);
                if (d1 != d2) {
                    return Integer.compare(d2, d1);
                }
                double s1 = scoreNodeSignificance(n1);
                double s2 = scoreNodeSignificance(n2);
                if (Double.compare(s2, s1) != 0) {
                    return Double.compare(s2, s1);
                }
                return n1.getPath().compareTo(n2.getPath());
            })
            .limit(limit)
            .collect(Collectors.toList());
    }

    private double scoreNodeSignificance(SemanticNode node) {
        if (node == null) return 0.0;
        double score = node.getArchitecturalAuthority() + (node.getEvolutionaryInfluenceScore() * 10.0);
        String p = node.getPath().toLowerCase();
        if (p.endsWith("modelfile") || p.endsWith(".modelfile")) score += 50.0;
        else if (p.endsWith("model.json") || p.endsWith("config.json") || p.endsWith("training-report.json")) score += 40.0;
        else if (p.endsWith(".evo") || p.endsWith(".evodata") || p.endsWith(".parquet") || p.endsWith(".gguf")) score += 35.0;
        else if (p.endsWith("pom.xml") || p.endsWith("build.gradle") || p.endsWith("package.json") || p.endsWith("readme.md")) score += 30.0;
        else if ("source_code".equals(node.getType())) score += 20.0;
        else if ("dataset".equals(node.getType()) || "model".equals(node.getType())) score += 25.0;

        // Generated exports (e.g. exports/ollama/Modelfile) should rank lower than primary root artifacts
        if ("true".equals(node.getAttributes().get("isExport")) || p.startsWith("exports/") || p.contains("/exports/")) {
            score -= 25.0;
        }

        if (node.getSummary() != null && !node.getSummary().isEmpty()) score += 10.0;
        return score;
    }
}
