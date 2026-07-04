package eu.kalafatic.evolution.controller.orchestration.intent;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.BaseAiAgent;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.diagnostics.CausalNode;
import eu.kalafatic.evolution.controller.orchestration.selfdev.AbstractionLevel;
import eu.kalafatic.evolution.controller.orchestration.selfdev.BranchVariant;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionDimension;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionaryPressureVector;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SemanticDomainResolver;
import eu.kalafatic.evolution.controller.orchestration.workspace.WorkspaceArtifact;
import eu.kalafatic.evolution.controller.parsers.JsonUtils;
import eu.kalafatic.evolution.controller.parsers.structured.StructuredResponsePipeline;
import eu.kalafatic.evolution.controller.trajectory.EvaluationSignal;
import eu.kalafatic.evolution.controller.trajectory.SignalSeverity;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventType;

/**
 * Engine for expanding user intent and exploring ambiguity before Darwin execution.
 */
public class IntentExpansionEngine extends BaseAiAgent {

    private final StructuredResponsePipeline pipeline = new StructuredResponsePipeline();

    public IntentExpansionEngine(eu.kalafatic.evolution.controller.orchestration.SessionContainer container) {
        super("IntentExpansionEngine", "IntentExpansionEngine", container);
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an Intent Expansion Engine. Your goal is to analyze user requests and CONSTRUCT THE EVOLUTIONARY SEARCH SPACE.\n" +
               "Identify implementation polymorphism - multiple valid ways to implement the intent.\n" +
               "\n" +
               "PHASE 0 - INTENT EXTRACTION:\n" +
               "Extract high-level metadata from the request:\n" +
               "- Goal: Concise engineering objective.\n" +
               "- Language/Framework/TargetPlatform: Detected technical stack.\n" +
               "- Constraints: Explicit constraints provided by user.\n" +
               "- Missing Information: Fields that need clarification.\n" +
               "- Ambiguities/Contradictions: Detected semantic conflicts.\n" +
               "\n" +
               "PHASE 1 - SEMANTIC DIMENSION DISCOVERY:\n" +
               "Analyze the goal to identify UNRESOLVED SEMANTIC DIMENSIONS.\n" +
               "A dimension is a decision point (e.g., Error Handling Strategy, Output Target, Persistence Model).\n" +
               "For SIMPLE ATOMIC TASKS (e.g., 'create a class', 'add a method'), avoid over-engineering but ENSURE SPECIFICITY. Identify simple dimensions like:\n" +
               "- Implementation Style (e.g., standard vs. optimized, static vs. instance)\n" +
               "- API Design (e.g., method signatures, return types, exception handling)\n" +
               "- Dependency Strategy (e.g., standard library vs. lightweight external utils)\n" +
               "- Structural Organization (e.g., inner classes vs. separate files, package layout)\n" +
               "\n" +
               "For each dimension, analyze:\n" +
               "- Abstraction Level: [PHILOSOPHY, STRATEGY, ARCHITECTURE, DESIGN, IMPLEMENTATION, SYNTAX]\n" +
               "- Semantic Domain: [EXECUTION, PERSISTENCE, RESILIENCE, COMMUNICATION, STRUCTURE, VALIDATION]\n" +
               "- Ambiguity Score: How much uncertainty exists?\n" +
               "- Evolutionary Pressure: [ambiguity, extensibility, scalability, failureExposure, implementationUncertainty, dependencyComplexity, integrationInstability, concurrencyPressure, performanceSensitivity]\n" +
               "\n" +
               "PHASE 2 - DIMENSION PRIORITIZATION:\n" +
               "Select exactly ONE dimension to evolve in this iteration.\n" +
               "Priority Rule: Higher abstraction levels (Philosophy/Strategy) MUST be resolved before lower levels (Architecture/Implementation).\n" +
               "For simple tasks, focus on IMPLEMENTATION or DESIGN dimensions immediately if Philosophy/Architecture is obvious.\n" +
               "STRICT: Avoid generic ID 'IMPLEMENTATION' if a more specific sub-dimension (e.g. 'API_DESIGN', 'OUTPUT_FORMAT', 'ERROR_STRATEGY') can be named. Specificity improves evolutionary diversity.\n" +
               "\n" +
               "PHASE 3 - CANDIDATE TRAJECTORIES:\n" +
               "For the selected dimension, define sibling branches (blueprints) that compete to resolve it.\n" +
               "MANDATORY: You MUST provide 'candidateBlueprints' with specific engineering strategies.\n" +
               "\n" +
               "STRICT RULES:\n" +
               "1. Do NOT generate code, tasks, or actual implementation branches.\n" +
               "2. Focus on structural and behavioral assumptions.\n" +
               "3. Output MUST be ONLY valid JSON.";
    }

    @Override
    protected String getFooterInstructions() {
        return "OUTPUT SCHEMA:\n" +
               "{\n" +
               "  \"state\": \"CLEAR\",\n" +
               "  \"dominantIntent\": \"string\",\n" +
               "  \"metadata\": {\n" +
               "    \"language\": \"string\",\n" +
               "    \"framework\": \"string\",\n" +
               "    \"targetPlatform\": \"string\",\n" +
               "    \"expectedOutput\": \"string\",\n" +
               "    \"constraints\": [\"string\"],\n" +
               "    \"missingInformation\": [{ \"field\": \"string\", \"description\": \"string\" }],\n" +
               "    \"ambiguities\": [{ \"part\": \"string\", \"reason\": \"string\" }],\n" +
               "    \"contradictions\": [\"string\"],\n" +
               "    \"clarificationQuestion\": \"string\"\n" +
               "  },\n" +
               "  \"unresolvedDimensions\": [\n" +
               "    {\n" +
               "      \"id\": \"string\",\n" +
               "      \"description\": \"string\",\n" +
               "      \"abstractionLevel\": \"PHILOSOPHY|STRATEGY|ARCHITECTURE|DESIGN|IMPLEMENTATION|SYNTAX\",\n" +
               "      \"semanticDomain\": \"EXECUTION|PERSISTENCE|RESILIENCE|COMMUNICATION|STRUCTURE|VALIDATION\",\n" +
               "      \"significanceScore\": float,\n" +
               "      \"ambiguityScore\": float,\n" +
               "      \"pressure\": {\n" +
               "        \"ambiguity\": float, \"extensibility\": float, \"scalability\": float, \"failureExposure\": float,\n" +
               "        \"implementationUncertainty\": float, \"dependencyComplexity\": float, \"integrationInstability\": float,\n" +
               "        \"concurrencyPressure\": float, \"performanceSensitivity\": float\n" +
               "      },\n" +
               "      \"candidateBlueprints\": [\n" +
               "        {\n" +
               "          \"id\": \"string\",\n" +
               "          \"strategy\": \"string\",\n" +
               "          \"survivalArgument\": \"string\",\n" +
               "          \"tradeoffs\": \"string\"\n" +
               "        }\n" +
               "      ]\n" +
               "    }\n" +
               "  ],\n" +
               "  \"activeDimension\": \"id_of_selected_dimension\",\n" +
               "  \"confidence\": {\"overallConfidence\": float, \"rationale\": \"string\"}\n" +
               "}";
    }

    public IntentExpansionResult expand(String prompt, TaskContext context) throws Exception {
        context.consoleLog("[INTENT EXPANSION] Analyzing intent space for: " + prompt);

        // FAST BYPASS: Detect direct variant selection or force solution commands
        String lower = prompt.toLowerCase().trim();
        if (lower.startsWith("select ") || lower.startsWith("approve variant ") || lower.equalsIgnoreCase("force solution")) {
            context.consoleLog("[INTENT EXPANSION] Detected bypass command: " + lower + ". Bypassing LLM expansion.");
            IntentExpansionResult result = new IntentExpansionResult();
            result.setOriginalPrompt(prompt);
            result.setState(InterpretationState.CLEAR);
            result.setDominantIntent(prompt);
            result.setDominantConfidence(1.0);

            IntentConfidence c = new IntentConfidence();
            c.setOverallConfidence(1.0);
            c.setRationale("Bypass command triggered: " + lower);
            result.setConfidence(c);
            return result;
        }

        // PERSISTENCE: Check Semantic Workspace for prior resolutions
        List<WorkspaceArtifact> priorConclusions = context.getSemanticWorkspace().findArtifactsByType("clarification-conclusion");
        String priorContext = "";
        if (!priorConclusions.isEmpty()) {
            StringBuilder sb = new StringBuilder("\nPRIOR CLARIFICATIONS:\n");
            for (WorkspaceArtifact a : priorConclusions) {
                sb.append("- ").append(a.getContent()).append("\n");
            }
            priorContext = sb.toString();
            context.consoleLog("[INTENT EXPANSION] Found " + priorConclusions.size() + " prior clarifications in workspace.");
        }

        String systemPrompt = getAgentInstructions() + "\n\n" + getFooterInstructions();

        StringBuilder userPromptBuilder = new StringBuilder();
        userPromptBuilder.append("Analyze the following user request and expand the intent space:\n\n").append(prompt).append(priorContext);

        eu.kalafatic.evolution.controller.orchestration.selfdev.AbstractionLevel lockedLevel = context.getOrchestrationState().getLockedAbstractionLevel();
        if (lockedLevel != null) {
            userPromptBuilder.append("\n\n[LOCKED_ABSTRACTION_LEVEL] This task is locked to the ").append(lockedLevel).append(" level.\n");
            userPromptBuilder.append("Ensure identified dimensions and blueprints do not exceed this level of abstraction.");
        }

        String userPrompt = userPromptBuilder.toString();

        String response = aiService.sendRequest(context.getOrchestrator(), systemPrompt + "\n\n" + userPrompt, context);

        // Define Schema for Intent Expansion
        java.util.Map<String, Class<?>> schema = new java.util.HashMap<>();
        schema.put("state", String.class);
        schema.put("dominantIntent", String.class);
        schema.put("confidence", JSONObject.class);

        JSONObject json = pipeline.process(response, schema, context);

        int currentIteration = context.getOrchestrationState().getIterationCount();
        json.put("iteration", currentIteration);
        context.log("[DARWIN_BRANCHES] " + json.toString());

        IntentExpansionResult result = new IntentExpansionResult();
        result.setOriginalPrompt(prompt);

        // Parse Intent Resolution fields
        String stateStr = json.optString("state", "CLEAR");
        result.setState(parseState(stateStr, context));

        result.setDominantIntent(json.optString("dominantIntent"));
        result.setDominantConfidence(json.optDouble("dominantConfidence", 0.5));

        // Parse Metadata
        JSONObject metadata = json.optJSONObject("metadata");
        if (metadata != null) {
            result.setLanguage(metadata.optString("language"));
            result.setFramework(metadata.optString("framework"));
            result.setTargetPlatform(metadata.optString("targetPlatform"));
            result.setExpectedOutput(metadata.optString("expectedOutput"));
            result.setClarificationQuestion(metadata.optString("clarificationQuestion"));
            result.setConstraints(JsonUtils.toStringList(metadata.optJSONArray("constraints")));
            result.setContradictions(JsonUtils.toStringList(metadata.optJSONArray("contradictions")));

            JSONArray missing = metadata.optJSONArray("missingInformation");
            if (missing != null) {
                for (int i = 0; i < missing.length(); i++) {
                    JSONObject m = missing.optJSONObject(i);
                    if (m != null) {
                        result.getMissingInformation().add(new MissingRequirement(
                            m.optString("field", "unknown"),
                            m.optString("description", m.toString())
                        ));
                    }
                }
            }

            JSONArray ambiguities = metadata.optJSONArray("ambiguities");
            if (ambiguities != null) {
                for (int i = 0; i < ambiguities.length(); i++) {
                    JSONObject a = ambiguities.optJSONObject(i);
                    if (a != null) {
                        result.getAmbiguities().add(new Ambiguity(
                            a.optString("part", "unknown"),
                            a.optString("reason", a.toString())
                        ));
                    }
                }
            }
        }

        // Parse Engineering Dimensions
        JSONObject dimensions = json.optJSONObject("engineeringDimensions");
        if (dimensions != null) {
            java.util.Map<String, Object> dimMap = new java.util.HashMap<>();
            java.util.Iterator<String> keys = dimensions.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                dimMap.put(key, dimensions.get(key));
            }
            context.getOrchestrationState().getMetadata().put("engineeringDimensions", dimMap);
            context.consoleLog("[INTENT EXPANSION] Derived 9 engineering dimensions.");
        }

        context.consoleLog("[INTENT EXPANSION] Interpretation State: " + result.getState());
        context.consoleLog("[INTENT EXPANSION] Dominant Intent: " + result.getDominantIntent());

        // Parse Unresolved Dimensions
        JSONArray unresolved = json.optJSONArray("unresolvedDimensions");
        SemanticDomainResolver domainResolver = new SemanticDomainResolver();
        if (unresolved != null) {
            for (int i = 0; i < unresolved.length(); i++) {
                Object item = unresolved.opt(i);
                if (item == null) continue;

                EvolutionDimension dim;
                if (item instanceof JSONObject) {
                    JSONObject dimObj = (JSONObject) item;
                    String levelStr = dimObj.optString("abstractionLevel", "IMPLEMENTATION");
                    AbstractionLevel level = AbstractionLevel.IMPLEMENTATION;
                    try {
                        level = AbstractionLevel.valueOf(levelStr.trim().toUpperCase());
                    } catch (Exception e) {}

                    dim = new EvolutionDimension(
                        dimObj.optString("id"),
                        dimObj.optString("description"),
                        level,
                        domainResolver.resolve(dimObj.optString("semanticDomain", "EXECUTION"))
                    );
                    dim.setAmbiguityScore(dimObj.optDouble("ambiguityScore", 0.0));
                    dim.setSignificanceScore(dimObj.optDouble("significanceScore", 0.5));

                    JSONObject pressureObj = dimObj.optJSONObject("pressure");
                    if (pressureObj != null) {
                        EvolutionaryPressureVector pressure = new EvolutionaryPressureVector();
                        pressure.ambiguity = pressureObj.optDouble("ambiguity", 0.0);
                        pressure.extensibility = pressureObj.optDouble("extensibility", 0.0);
                        pressure.scalability = pressureObj.optDouble("scalability", 0.0);
                        pressure.failureExposure = pressureObj.optDouble("failureExposure", 0.0);
                        pressure.implementationUncertainty = pressureObj.optDouble("implementationUncertainty", 0.0);
                        pressure.dependencyComplexity = pressureObj.optDouble("dependencyComplexity", 0.0);
                        pressure.integrationInstability = pressureObj.optDouble("integrationInstability", 0.0);
                        pressure.concurrencyPressure = pressureObj.optDouble("concurrencyPressure", 0.0);
                        pressure.performanceSensitivity = pressureObj.optDouble("performanceSensitivity", 0.0);
                        dim.setEvolutionaryPressure(pressure.getTotalPressure());
                    }

                    JSONArray blueprints = dimObj.optJSONArray("candidateBlueprints");
                    if (blueprints != null) {
                        for (int j = 0; j < blueprints.length(); j++) {
                            Object bpItem = blueprints.opt(j);
                            if (bpItem instanceof JSONObject) {
                                JSONObject bpObj = (JSONObject) bpItem;
                                BranchVariant bv = new BranchVariant();
                                bv.setId(bpObj.optString("id"));
                                bv.setStrategy(bpObj.optString("strategy"));
                                bv.setSurvivalArgument(bpObj.optString("survival_argument"));
                                bv.setTradeoffs(bpObj.optString("tradeoffs"));
                                dim.getCandidateBranches().add(bv);
                            } else if (bpItem instanceof String) {
                                BranchVariant bv = new BranchVariant();
                                bv.setId("v-" + j);
                                bv.setStrategy((String) bpItem);
                                bv.setSurvivalArgument("Inferred variant");
                                dim.getCandidateBranches().add(bv);
                            }
                        }
                    }
                } else if (item instanceof String) {
                    dim = new EvolutionDimension(
                        "dim-" + i,
                        (String) item,
                        AbstractionLevel.IMPLEMENTATION,
                        domainResolver.resolve("EXECUTION")
                    );
                    dim.setAmbiguityScore(0.5);
                    dim.setSignificanceScore(0.5);
                } else {
                    continue;
                }
                result.getUnresolvedDimensions().add(dim);
            }
        }
        result.setActiveDimensionId(json.optString("activeDimension"));

        // Parse Confidence
        JSONObject confObj = json.optJSONObject("confidence");
        IntentConfidence c = new IntentConfidence();
        if (confObj != null) {
            c.setOverallConfidence(confObj.optDouble("overallConfidence", 0.5));
            c.setRationale(confObj.optString("rationale", "Inferred from content"));
        } else {
            c.setOverallConfidence(0.5);
            c.setRationale("No confidence data provided by AI");
        }

        // Final sanity check/adjustment of confidence if needed
        if (c.getOverallConfidence() == 0 && result.getDominantIntent() != null) {
            c.setOverallConfidence(ConfidenceEvaluator.evaluate(result));
        }
        result.setConfidence(c);

        emitIntentSignal(result, context);

        // DIAGNOSTICS: Emit causal node for intent expansion
        context.getOrchestrationState().getCognitiveTrace().addNode(new CausalNode(
            "intent-expansion-" + System.currentTimeMillis(),
            "INTENT_EXPANSION",
            "IntentExpansionEngine",
            List.of(prompt),
            result.getEvolutionaryAxes().stream().map(a -> a.getName()).collect(java.util.stream.Collectors.toList()),
            result.getConfidence().getOverallConfidence(),
            result.getConfidence().getRationale()
        ));

        return result;
    }

    private void emitIntentSignal(IntentExpansionResult result, TaskContext context) {
        double confidence = result.getConfidence().getOverallConfidence();
        EvaluationSignal signal = new EvaluationSignal(
            "intent-analysis",
            "IntentExpansionEngine",
            confidence,
            1.0,
            confidence > 0.7 ? SignalSeverity.INFO : SignalSeverity.WARNING,
            "Intent confidence: " + confidence
        );

        SessionContainer session = getSessionContainer();
        if (session == null) {
            session = SessionManager.getInstance().getSession(context.getSessionId());
        }
        if (session != null) {
            RuntimeEventBus bus = session.getEventBus();
            bus.publish(new RuntimeEvent(
                RuntimeEventType.EVALUATION_SIGNAL_CREATED,
                context.getSessionId(),
                "IntentExpansionEngine",
                signal
            ));
        }
    }

    public static InterpretationState parseState(String stateStr, TaskContext context) {
        if (stateStr == null || stateStr.trim().isEmpty()) return InterpretationState.CLEAR;
        String cleanState = stateStr.trim().toUpperCase();

        // HARDENING: If the model echoes the whole enum list or example comments, it's likely CLEAR/EVOLVABLE
        // Only trigger if it looks like a generic template/list rather than a set of specific states
        if ((cleanState.contains("|") || cleanState.contains("[") || cleanState.contains("/")) &&
           (cleanState.contains("CLEAR") && cleanState.contains("NEEDS_CLARIFICATION") && cleanState.contains("BLOCKED"))) {
            if (context != null) {
                context.consoleLog("[INTENT EXPANSION] Detected placeholder or list echo in state: " + stateStr + ". Resolving to CLEAR.");
            }
            return InterpretationState.CLEAR;
        }

        try {
            // Exact match
            return InterpretationState.valueOf(cleanState);
        } catch (IllegalArgumentException e) {
            // Robust substring match for noisy LLM output (e.g., "State: CLEAR")
            for (InterpretationState s : InterpretationState.values()) {
                if (cleanState.contains(s.name())) {
                    if (context != null) {
                        context.consoleLog("[INTENT EXPANSION] Noisy state string '" + stateStr + "' resolved to " + s);
                    }
                    return s;
                }
            }
            if (context != null) {
                context.consoleLog("[INTENT EXPANSION] WARNING: Unrecognized state '" + stateStr + "'. Defaulting to CLEAR.");
            }
            return InterpretationState.CLEAR;
        }
    }

}
