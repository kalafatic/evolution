package eu.kalafatic.evolution.controller.agents;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact;
import eu.kalafatic.evolution.controller.mediation.model.Hotspot;
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
        return "You are a Reality Discovery Agent.\n" +
               "Your goal is to construct a formal Target Reality Model from the provided repository evidence.\n" +
               "Identify the domain, purpose, architecture, hotspots, subsystems, architectural facts, objectives, and risks.\n" +
               "STRICT RULE: Only use discovered evidence. Do NOT compensate with invention or fashionable patterns.\n" +
               "Identify hotspots based on connectivity, information density, and entry points.\n" +
               "Discover subsystems based on dependencies, communication patterns, and shared responsibilities.\n" +
               "Accumulate reusable architectural facts (e.g., 'Component A coordinates execution').";
    }

    public TargetRealityModel discover(String goal, TaskContext context, TargetSnapshot snapshot) throws Exception {
        context.log("[DISCOVERY] Formalizing Target Reality Model for: " + goal);

        StringBuilder sb = new StringBuilder();
        sb.append("GOAL: ").append(goal).append("\n\n");

        if (snapshot != null) {
            sb.append("REPOSITORY STRUCTURE SUMMARY:\n");
            sb.append("- Root: ").append(snapshot.getRootPath()).append("\n");
            sb.append("- Technologies: ").append(snapshot.getMetadata().get("detectedTechnologies")).append("\n");
            sb.append("- Architecture Inference: ").append(snapshot.getMetadata().get("architectureInference")).append("\n");
            sb.append("- Node Count: ").append(snapshot.getNodes().size()).append("\n\n");

            // Extract high-signal hotspots from graph centrality
            List<SemanticNode> topNodes = getTopCentralNodes(snapshot, 10);
            sb.append("HIGH-CENTRALITY CANDIDATES:\n");
            for (SemanticNode node : topNodes) {
                sb.append("- ").append(node.getPath()).append(" (Summary: ").append(node.getSummary()).append(")\n");
            }
        }

        String prompt = sb.toString() + "\n\n" +
               "Output a JSON object for the Target Reality Model with the following schema:\n" +
               "{\n" +
               "  \"domain\": \"string\",\n" +
               "  \"purpose\": \"string\",\n" +
               "  \"architecture_summary\": \"string\",\n" +
               "  \"technologies\": [\"string\"],\n" +
               "  \"hotspots\": [\n" +
               "    {\n" +
               "      \"id\": \"string\",\n" +
               "      \"name\": \"string\",\n" +
               "      \"type\": \"entry_point|core_module|bottleneck|critical_query\",\n" +
               "      \"description\": \"string\",\n" +
               "      \"significance\": 0.0-1.0,\n" +
               "      \"evidence\": [\"string\"],\n" +
               "      \"related_artifacts\": [\"path/to/artifact\"]\n" +
               "    }\n" +
               "  ],\n" +
               "  \"subsystems\": [\n" +
               "    {\n" +
               "      \"id\": \"string\",\n" +
               "      \"name\": \"string\",\n" +
               "      \"purpose\": \"string\",\n" +
               "      \"description\": \"string\",\n" +
               "      \"boundaries\": [\"string\"],\n" +
               "      \"critical_files\": [\"string\"],\n" +
               "      \"responsibilities\": [\"string\"]\n" +
               "    }\n" +
               "  ],\n" +
               "  \"architectural_facts\": [\n" +
               "    {\n" +
               "      \"id\": \"string\",\n" +
               "      \"subject\": \"string\",\n" +
               "      \"predicate\": \"string\",\n" +
               "      \"description\": \"string\",\n" +
               "      \"confidence\": 0.0-1.0,\n" +
               "      \"evidence\": [\"string\"]\n" +
               "    }\n" +
               "  ],\n" +
               "  \"objectives\": [\"string\"],\n" +
               "  \"risks\": [\"string\"],\n" +
               "  \"dimensions\": {\"key\": \"value\"}\n" +
               "}";

        String response = aiService.sendRequest(context.getOrchestrator(), getAgentInstructions() + "\n\n" + prompt, context);
        JSONObject obj = JsonUtils.extractJsonObject(response);

        TargetRealityModel model = new TargetRealityModel();
        if (obj != null) {
            model.setDomain(obj.optString("domain"));
            model.setPurpose(obj.optString("purpose"));
            model.setArchitectureSummary(obj.optString("architecture_summary"));

            JSONArray techs = obj.optJSONArray("technologies");
            if (techs != null) {
                for (int i = 0; i < techs.length(); i++) model.getTechnologies().add(techs.getString(i));
            }

            JSONArray hotspots = obj.optJSONArray("hotspots");
            if (hotspots != null) {
                for (int i = 0; i < hotspots.length(); i++) {
                    JSONObject hObj = hotspots.getJSONObject(i);
                    Hotspot hotspot = new Hotspot();
                    hotspot.setId(hObj.optString("id"));
                    hotspot.setName(hObj.optString("name"));
                    hotspot.setType(hObj.optString("type"));
                    hotspot.setDescription(hObj.optString("description"));
                    hotspot.setSignificance(hObj.optDouble("significance", 0.5));

                    JSONArray ev = hObj.optJSONArray("evidence");
                    if (ev != null) {
                        for (int j = 0; j < ev.length(); j++) hotspot.getEvidence().add(ev.getString(j));
                    }

                    JSONArray rel = hObj.optJSONArray("related_artifacts");
                    if (rel != null) {
                        for (int j = 0; j < rel.length(); j++) hotspot.getRelatedArtifacts().add(rel.getString(j));
                    }
                    model.addHotspot(hotspot);
                }
            }

            JSONArray subsystems = obj.optJSONArray("subsystems");
            if (subsystems != null) {
                for (int i = 0; i < subsystems.length(); i++) {
                    JSONObject sObj = subsystems.getJSONObject(i);
                    Subsystem subsystem = new Subsystem();
                    subsystem.setId(sObj.optString("id"));
                    subsystem.setName(sObj.optString("name"));
                    subsystem.setPurpose(sObj.optString("purpose"));
                    subsystem.setDescription(sObj.optString("description"));

                    JSONArray bounds = sObj.optJSONArray("boundaries");
                    if (bounds != null) {
                        for (int j = 0; j < bounds.length(); j++) subsystem.getBoundaries().add(bounds.getString(j));
                    }
                    JSONArray crit = sObj.optJSONArray("critical_files");
                    if (crit != null) {
                        for (int j = 0; j < crit.length(); j++) subsystem.getCriticalFiles().add(crit.getString(j));
                    }
                    JSONArray resp = sObj.optJSONArray("responsibilities");
                    if (resp != null) {
                        for (int j = 0; j < resp.length(); j++) subsystem.getResponsibilities().add(resp.getString(j));
                    }
                    model.addSubsystem(subsystem);
                }
            }

            JSONArray facts = obj.optJSONArray("architectural_facts");
            if (facts != null) {
                for (int i = 0; i < facts.length(); i++) {
                    JSONObject fObj = facts.getJSONObject(i);
                    ArchitecturalFact fact = new ArchitecturalFact();
                    fact.setId(fObj.optString("id"));
                    fact.setSubject(fObj.optString("subject"));
                    fact.setPredicate(fObj.optString("predicate"));
                    fact.setDescription(fObj.optString("description"));
                    fact.setConfidence(fObj.optDouble("confidence", 0.5));

                    JSONArray ev = fObj.optJSONArray("evidence");
                    if (ev != null) {
                        for (int j = 0; j < ev.length(); j++) fact.getEvidence().add(ev.getString(j));
                    }
                    model.addArchitecturalFact(fact);
                }
            }

            JSONArray objectives = obj.optJSONArray("objectives");
            if (objectives != null) {
                for (int i = 0; i < objectives.length(); i++) model.getObjectives().add(objectives.getString(i));
            }

            JSONArray risks = obj.optJSONArray("risks");
            if (risks != null) {
                for (int i = 0; i < risks.length(); i++) model.getRisks().add(risks.getString(i));
            }

            JSONObject dims = obj.optJSONObject("dimensions");
            if (dims != null) {
                for (Object keyObj : dims.keySet()) {
                    String key = (String) keyObj;
                    model.getDimensions().put(key, dims.optString(key));
                }
            }
        }

        return model;
    }

    private List<SemanticNode> getTopCentralNodes(TargetSnapshot snapshot, int limit) {
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
