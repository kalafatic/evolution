package eu.kalafatic.evolution.controller.agents;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact;
import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalGene;
import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalUseCase;
import eu.kalafatic.evolution.controller.mediation.model.Hotspot;
import eu.kalafatic.evolution.controller.mediation.model.KnowledgeGap;
import eu.kalafatic.evolution.controller.mediation.model.Subsystem;
import eu.kalafatic.evolution.controller.mediation.model.SemanticEdge;
import eu.kalafatic.evolution.controller.mediation.model.SemanticNode;
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
        return "You are a Reality Discovery Agent specialized in Recursive Reality Reconstruction.\n" +
               "Your goal is deep architectural comprehension that remains executable on small local models.\n" +
               "Small models participate in a recursive discovery process that gradually reconstructs the target reality.\n" +
               "Never ask to explain the entire system. Instead ask 'What is the most important architectural fact not yet known?'\n" +
               "Identify evidence, uncertainty, and knowledge gaps.\n" +
               "STRICT RULE: Only use discovered evidence. Do NOT compensate with invention.";
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
        TargetSnapshot.TargetType type = targetPath.contains("evolution") ? TargetSnapshot.TargetType.SELF : TargetSnapshot.TargetType.PROJECT;
        TargetSnapshot snapshot = scanner.scanToSnapshot(root, type);

        int intensity = context.getExecutionProfile().getIntensity();

        // PASS 1 - METADATA LOADING
        context.log("[DISCOVERY] Pass 1: Metadata Loading.");
        loadAllMetadata(snapshot, context, model);

        // PASS 2 - LOCAL RESPONSIBILITY DISCOVERY
        if (intensity >= 2) {
            discoverLocalResponsibilities(snapshot, model, context);
        }

        // PASS 3 - RELATIONSHIP DISCOVERY & EVOLUTIONARY INFLUENCE
        if (intensity >= 3) {
            discoverRelationships(snapshot, model, context);
        }

        // PASS 4 - SUBSYSTEM DISCOVERY
        if (intensity >= 3) {
            discoverSubsystems(snapshot, model, context);
        }

        // PASS 5 - REALITY DISCOVERY (Synthesis & Completeness Tracking)
        synthesizeReality(goal, snapshot, model, context);

        // PASS 6 - GENOME DISCOVERY (Portable Patterns)
        if (intensity >= 4) {
            discoverGenome(model, context);
        }

        // PASS 7 - ARCHITECTURAL COMPRESSION
        if (intensity >= 2) {
            compressUnderstanding(model, context);
        }

        // PASS 8 - USE CASE DISCOVERY
        if (intensity >= 3) {
            discoverUseCases(model, context);
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
        if (context.getExecutionProfile().getIntensity() >= 2) {
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
        }

        // 3. Selected Files (Coverage-Driven via ContextCurator)
        context.log("[DISCOVERY] Running Coverage-Driven Context Selection.");
        eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator curator = new eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator();
        List<String> selected = curator.selectContext(snapshot, goal, 64, model);
        model.getSelectedFiles().clear();
        model.getSelectedFiles().addAll(selected);
    }

    private void discoverLocalResponsibilities(TargetSnapshot snapshot, TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 2: Local Responsibility Discovery.");
        if (snapshot == null) return;

        // Prioritize hotspots and high-centrality nodes
        List<SemanticNode> candidates = getTopCentralNodes(snapshot, 10);
        for (SemanticNode node : candidates) {
            // Check if we already have facts about this node
            boolean known = model.getArchitecturalFacts().stream().anyMatch(f -> f.getEvidence().contains(node.getPath()));
            if (known) continue;

            context.log("[DISCOVERY] Analyzing responsibility of: " + node.getPath());

            StringBuilder sb = new StringBuilder();
            sb.append("ARTIFACT: ").append(node.getPath()).append("\n");
            sb.append("SUMMARY: ").append(node.getSummary()).append("\n");
            sb.append("DEPENDENCIES: ").append(node.getDependencies()).append("\n\n");
            sb.append("Identify responsibilities, inputs, outputs, decisions, and concepts for this artifact.");

            String prompt = sb.toString() + "\n\n" +
                    "Output a JSON object:\n" +
                    "{\n" +
                    "  \"responsibilities\": [\"string\"],\n" +
                    "  \"inputs\": [\"string\"],\n" +
                    "  \"outputs\": [\"string\"],\n" +
                    "  \"decisions\": [\"string\"],\n" +
                    "  \"concepts\": [\"string\"],\n" +
                    "  \"role_candidate\": \"string\"\n" +
                    "}";

            String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
            JSONObject obj = JsonUtils.extractJsonObject(response);
            if (obj != null) {
                JSONArray resps = obj.optJSONArray("responsibilities");
                if (resps != null) {
                    for (int i = 0; i < resps.length(); i++) {
                        ArchitecturalFact fact = new ArchitecturalFact(node.getId() + "-resp-" + i, node.getPath(), "provides responsibility: " + resps.getString(i), 0.8);
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
            }
        }
    }

    private void discoverRelationships(TargetSnapshot snapshot, TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 3: Relationship Discovery & Evolutionary Influence.");
        if (snapshot == null) return;

        List<SemanticNode> influentialNodes = getTopCentralNodes(snapshot, 15);
        for (SemanticNode node : influentialNodes) {
            context.log("[DISCOVERY] Analyzing relationships and influence for: " + node.getPath());

            List<SemanticEdge> incoming = snapshot.getEdges().stream().filter(e -> e.getTargetId().equals(node.getId())).collect(Collectors.toList());
            List<SemanticEdge> outgoing = snapshot.getEdges().stream().filter(e -> e.getSourceId().equals(node.getId())).collect(Collectors.toList());

            StringBuilder sb = new StringBuilder();
            sb.append("NODE: ").append(node.getPath()).append("\n");
            sb.append("INCOMING DEPENDENCIES: ").append(incoming.size()).append("\n");
            sb.append("OUTGOING DEPENDENCIES: ").append(outgoing.size()).append("\n");
            sb.append("Identify control, ownership, orchestration, and coordination responsibilities.\n");
            sb.append("Analyze Evolutionary Leverage: If this file changes, what breaks? What evolves? What behavior changes?");

            String prompt = sb.toString() + "\n\n" +
                    "Output a JSON object:\n" +
                    "{\n" +
                    "  \"relationships\": [\n" +
                    "    {\n" +
                    "      \"type\": \"control|ownership|orchestration|coordination\",\n" +
                    "      \"subject\": \"string\",\n" +
                    "      \"description\": \"string\",\n" +
                    "      \"impact_if_removed\": \"string\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"evolutionary_leverage\": {\n" +
                    "    \"influence_score\": 0.0-1.0,\n" +
                    "    \"break_impacts\": [\"string\"],\n" +
                    "    \"evolution_potentials\": [\"string\"]\n" +
                    "  }\n" +
                    "}";

            String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
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

                JSONObject lev = obj.optJSONObject("evolutionary_leverage");
                if (lev != null) {
                    node.setEvolutionaryInfluenceScore(lev.optDouble("influence_score", 0.5));
                    JSONArray breaks = lev.optJSONArray("break_impacts");
                    if (breaks != null) for (int i = 0; i < breaks.length(); i++) node.addBreakImpact(breaks.getString(i));
                    JSONArray evs = lev.optJSONArray("evolution_potentials");
                    if (evs != null) for (int i = 0; i < evs.length(); i++) node.addEvolutionPotential(evs.getString(i));
                }
            }
        }
    }

    private void discoverSubsystems(TargetSnapshot snapshot, TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 4: Subsystem Discovery.");

        StringBuilder sb = new StringBuilder();
        sb.append("EXISTING ARCHITECTURAL FACTS:\n");
        model.getArchitecturalFacts().stream().limit(20).forEach(f -> sb.append("- ").append(f.toString()).append("\n"));
        sb.append("\nIdentify strongly interacting clusters and define Subsystems.");

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

        String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
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

    private void synthesizeReality(String goal, TargetSnapshot snapshot, TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 5: Reality Discovery & Completeness Tracking.");

        StringBuilder sb = new StringBuilder();
        sb.append("GOAL: ").append(goal).append("\n");
        sb.append("FACTS: ").append(model.getArchitecturalFacts().size()).append("\n");
        sb.append("SUBSYSTEMS: ").append(model.getSubsystems().size()).append("\n");
        sb.append("KNOWLEDGE GAPS: ").append(model.getKnowledgeGaps().size()).append("\n");
        sb.append("\nSynthesize Reality Model, discover core facts/hotspots, and estimate Completeness (0.0-1.0).\n");
        sb.append("Also organize the model into ArchitectureView, ImplementationView, and GenomeView.");

        String prompt = sb.toString() + "\n\n" +
                "Output a JSON object:\n" +
                "{\n" +
                "  \"domain\": \"string\",\n" +
                "  \"purpose\": \"string\",\n" +
                "  \"architecture_summary\": \"string\",\n" +
                "  \"completeness_score\": 0.0-1.0,\n" +
                "  \"architectural_facts\": [{ \"id\": \"string\", \"subject\": \"string\", \"predicate\": \"string\", \"description\": \"string\" }],\n" +
                "  \"hotspots\": [{ \"id\": \"string\", \"name\": \"string\", \"description\": \"string\", \"significance\": 0.0-1.0 }],\n" +
                "  \"views\": {\n" +
                "    \"architecture\": {\"hubs\": [\"string\"], \"orchestration\": \"string\"},\n" +
                "    \"implementation\": {\"entry_points\": [\"string\"], \"critical_path\": [\"string\"]},\n" +
                "    \"genome\": {\"core_patterns\": [\"string\"]}\n" +
                "  }\n" +
                "}";

        String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
        JSONObject obj = JsonUtils.extractJsonObject(response);
        if (obj != null) {
            model.setDomain(obj.optString("domain"));
            model.setPurpose(obj.optString("purpose"));
            model.setArchitectureSummary(obj.optString("architecture_summary"));
            model.setRealityCompleteness(obj.optDouble("completeness_score", 0.5));

            // Extract additional facts if discovered
            JSONArray facts = obj.optJSONArray("architectural_facts");
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
                    model.addHotspot(hotspot);
                }
            }

            JSONObject views = obj.optJSONObject("views");
            if (views != null) {
                JSONObject arch = views.optJSONObject("architecture");
                if (arch != null) {
                    java.util.Iterator<String> keys = arch.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        model.getArchitectureView().put(key, arch.get(key));
                    }
                }
                JSONObject impl = views.optJSONObject("implementation");
                if (impl != null) {
                    java.util.Iterator<String> keys = impl.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        model.getImplementationView().put(key, impl.get(key));
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

    private void discoverGenome(TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 6: Genome Discovery (Portable Patterns).");

        StringBuilder sb = new StringBuilder();
        sb.append("REALITY MODEL: ").append(model.getArchitectureSummary()).append("\n");
        sb.append("Identify portable patterns and their required artifacts.");

        String prompt = sb.toString() + "\n\n" +
                "Output a JSON object:\n" +
                "{\n" +
                "  \"genes\": [\n" +
                "    {\n" +
                "      \"pattern\": \"string\",\n" +
                "      \"purpose\": \"string\",\n" +
                "      \"rationale\": \"string\",\n" +
                "      \"required_artifacts\": [\"string\"],\n" +
                "      \"transferability\": \"string\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
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
                    JSONArray reqs = gObj.optJSONArray("required_artifacts");
                    if (reqs != null) for (int j = 0; j < reqs.length(); j++) gene.getRequiredArtifacts().add(reqs.getString(j));
                    gene.setTransferability(gObj.optString("transferability"));
                    model.addGene(gene);
                }
            }
        }
    }

    private void discoverUseCases(TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 8: Use Case Discovery.");

        StringBuilder sb = new StringBuilder();
        sb.append("SUBSYSTEMS:\n");
        model.getSubsystems().forEach(s -> sb.append("- ").append(s.getName()).append(": ").append(s.getPurpose()).append("\n"));
        sb.append("\nARCHITECTURAL FACTS:\n");
        model.getArchitecturalFacts().stream().limit(15).forEach(f -> sb.append("- ").append(f.toString()).append("\n"));
        sb.append("\nIdentify major repository-wide Use Cases derived from these architectural structures.\n");
        sb.append("Every use case MUST have supporting components, supporting files (evidence), and a confidence score.");

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

        String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
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

    private void compressUnderstanding(TargetRealityModel model, TaskContext context) throws Exception {
        context.log("[DISCOVERY] Pass 7: Architectural Compression.");

        StringBuilder sb = new StringBuilder();
        sb.append("TARGET REALITY MODEL:\n");
        sb.append("- Domain: ").append(model.getDomain()).append("\n");
        sb.append("- Subsystems: ").append(model.getSubsystems().size()).append("\n");
        sb.append("- Facts: ").append(model.getArchitecturalFacts().size()).append("\n");
        sb.append("- Genes: ").append(model.getGenes().size()).append("\n");
        sb.append("\nCompress these observations into high-signal architectural facts and genes while preserving understanding.");

        String prompt = sb.toString() + "\n\n" +
                "Summarize the core architectural essence in 3 paragraphs.";

        String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
        model.getDimensions().put("architectural_essence", response);
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
        int metadataNodes = model.getMetadataEntries();
        double coverage = totalNodes > 0 ? (double) metadataNodes / totalNodes : 0;

        if (coverage < 0.2) {
            String warning = "Analysis covered only " + metadataNodes + " of " + totalNodes + " source files. Architecture model incomplete.";
            model.setAnalysisWarning(warning);
            context.log("[DISCOVERY] WARNING: " + warning);
            // Reduce confidence of all use cases
            model.getUseCases().forEach(uc -> uc.setConfidence(uc.getConfidence() * 0.5));
            model.setRealityCompleteness(model.getRealityCompleteness() * 0.5);
        } else if (coverage < 0.5) {
            String warning = "Analysis covered " + String.format("%.1f", coverage * 100) + "% of source files. Results may be partial.";
            model.setAnalysisWarning(warning);
            context.log("[DISCOVERY] WARNING: " + warning);
        }
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
                KnowledgeGap gap = new KnowledgeGap("gap-" + node.getId(), "CRITICAL UNKNOWN: High-influence component unmapped: " + node.getPath(), KnowledgeGap.GapType.UNKNOWN_FACT);
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
                return Integer.compare(d2, d1);
            })
            .limit(limit)
            .collect(Collectors.toList());
    }
}
