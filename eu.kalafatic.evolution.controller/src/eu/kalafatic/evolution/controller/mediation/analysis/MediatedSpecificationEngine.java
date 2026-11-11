package eu.kalafatic.evolution.controller.mediation.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.mediation.model.ArchitecturalFact;
import eu.kalafatic.evolution.controller.mediation.model.Hotspot;
import eu.kalafatic.evolution.controller.mediation.model.Subsystem;
import eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;

/**
 * Transforms raw, incomplete, or ambiguous user requests into comprehensive,
 * professional engineering specifications using local Darwin intelligence.
 */
public class MediatedSpecificationEngine {

    /**
     * Transforms raw user request into a formal MediatedSpecification.
     */
    public MediatedSpecification transform(String rawRequest, TargetSnapshot snapshot,
            TargetRealityModel realityModel, TaskContext context, AiService aiService) {

        if (rawRequest == null || rawRequest.trim().isEmpty()) {
            rawRequest = "Perform architectural analysis and modernization review.";
        }

        // Try intelligent expansion with local AI service if available and not test mode without LLM
        if (aiService != null && context != null && context.getOrchestrator() != null
                && !context.getMetadata().containsKey("testMode")) {
            try {
                MediatedSpecification spec = attemptLlmTransformation(rawRequest, snapshot, realityModel, context, aiService);
                if (spec != null) {
                    return spec;
                }
            } catch (Exception e) {
                if (context != null) {
                    context.log("[SPECIFICATION_ENGINE] LLM transformation fallback: " + e.getMessage());
                }
            }
        }

        // Deterministic Darwin reasoning & heuristic expansion
        return buildDeterministicSpecification(rawRequest, snapshot, realityModel);
    }

    private MediatedSpecification attemptLlmTransformation(String rawRequest, TargetSnapshot snapshot,
            TargetRealityModel realityModel, TaskContext context, AiService aiService) throws Exception {

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are the Darwin Technical Reasoning Engine.\n");
        prompt.append("Transform the following RAW, potentially incomplete or simple user request into a comprehensive, professional engineering specification for an external senior software engineer or coding AI.\n\n");
        prompt.append("RAW USER REQUEST:\n> ").append(rawRequest).append("\n\n");

        if (realityModel != null) {
            prompt.append("PROJECT DOMAIN: ").append(realityModel.getDomain()).append("\n");
            prompt.append("ARCHITECTURE OVERVIEW: ").append(realityModel.getArchitectureSummary()).append("\n");
            if (!realityModel.getSubsystems().isEmpty()) {
                prompt.append("DETECTED SUBSYSTEMS:\n");
                for (Subsystem s : realityModel.getSubsystems()) {
                    prompt.append("- ").append(s.getName()).append(": ").append(s.getPurpose()).append("\n");
                }
            }
        }

        prompt.append("\nReturn a JSON object conforming STRICTLY to the following schema without any markdown formatting:\n");
        prompt.append("{\n");
        prompt.append("  \"primaryObjective\": \"...\",\n");
        prompt.append("  \"subObjectives\": [\"...\"],\n");
        prompt.append("  \"scopeDefinitions\": [\"...\"],\n");
        prompt.append("  \"resolvedAmbiguities\": [\"...\"],\n");
        prompt.append("  \"implicitRequirements\": [\"...\"],\n");
        prompt.append("  \"technicalConstraints\": [\"...\"],\n");
        prompt.append("  \"affectedAreas\": [\"...\"],\n");
        prompt.append("  \"technicalRisks\": [\"...\"],\n");
        prompt.append("  \"expectedBehavior\": [\"...\"],\n");
        prompt.append("  \"acceptanceCriteria\": [\"...\"]\n");
        prompt.append("}\n");

        String response = aiService.sendRequest(context.getOrchestrator(), prompt.toString(), context);
        if (response != null && !response.trim().isEmpty()) {
            String jsonStr = response.trim();
            if (jsonStr.contains("{") && jsonStr.contains("}")) {
                int start = jsonStr.indexOf('{');
                int end = jsonStr.lastIndexOf('}') + 1;
                jsonStr = jsonStr.substring(start, end);
                JSONObject obj = new JSONObject(jsonStr);

                MediatedSpecification spec = new MediatedSpecification(rawRequest);
                spec.setPrimaryObjective(obj.optString("primaryObjective", rawRequest));
                spec.setSubObjectives(jsonArrayToList(obj.optJSONArray("subObjectives")));
                spec.setScopeDefinitions(jsonArrayToList(obj.optJSONArray("scopeDefinitions")));
                spec.setResolvedAmbiguities(jsonArrayToList(obj.optJSONArray("resolvedAmbiguities")));
                spec.setImplicitRequirements(jsonArrayToList(obj.optJSONArray("implicitRequirements")));
                spec.setTechnicalConstraints(jsonArrayToList(obj.optJSONArray("technicalConstraints")));
                spec.setAffectedAreas(jsonArrayToList(obj.optJSONArray("affectedAreas")));
                spec.setTechnicalRisks(jsonArrayToList(obj.optJSONArray("technicalRisks")));
                spec.setExpectedBehavior(jsonArrayToList(obj.optJSONArray("expectedBehavior")));
                spec.setAcceptanceCriteria(jsonArrayToList(obj.optJSONArray("acceptanceCriteria")));

                if (spec.getAcceptanceCriteria().isEmpty()) {
                    spec.getAcceptanceCriteria().add("Verify changes compile and pass existing test suites without regressions.");
                }
                return spec;
            }
        }
        return null;
    }

    private List<String> jsonArrayToList(JSONArray arr) {
        List<String> list = new ArrayList<>();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                String val = arr.optString(i);
                if (val != null && !val.trim().isEmpty()) {
                    list.add(val.trim());
                }
            }
        }
        return list;
    }

    /**
     * Fallback and deterministic Darwin specification generator that extracts
     * domain, technology constraints, and requirements grounded in the target model.
     */
    public MediatedSpecification buildDeterministicSpecification(String rawRequest, TargetSnapshot snapshot,
            TargetRealityModel realityModel) {

        MediatedSpecification spec = new MediatedSpecification(rawRequest);
        String lower = rawRequest.toLowerCase(Locale.ROOT);

        // 1. Primary Objective
        spec.setPrimaryObjective("Implement requested improvement: " + rawRequest.trim());

        // 2. Identify implicit requirements & resolved ambiguities from keywords
        if (lower.contains("database") || lower.contains("connection") || lower.contains("sql") || lower.contains("dao")) {
            spec.getSubObjectives().add("Establish robust connection lifecycle management with proper pooling, lease timeouts, and clean resource closure.");
            spec.getResolvedAmbiguities().add("Interpreted 'improve database handling' as enforcing connection leak prevention, statement caching, and transactional boundaries.");
            spec.getImplicitRequirements().add("Thread safety across concurrent database operations.");
            spec.getImplicitRequirements().add("Ensure all Statement and ResultSet resources are closed via try-with-resources.");
            spec.getTechnicalConstraints().add("Do not break existing database schemas or table definitions.");
            spec.getTechnicalRisks().add("Connection pool starvation under high concurrent load.");
            spec.getExpectedBehavior().add("Graceful connection retry with backoff and detailed logging on connection acquisition failure.");
            spec.getAcceptanceCriteria().add("Database connections and prepared statements close deterministically without resource leaks.");
        }

        if (lower.contains("performance") || lower.contains("slow") || lower.contains("optimize") || lower.contains("cache")) {
            spec.getSubObjectives().add("Identify throughput bottlenecks, minimize unnecessary object allocations, and introduce localized caching.");
            spec.getResolvedAmbiguities().add("Interpreted performance improvement as reducing latency and eliminating redundant I/O operations without modifying public API contracts.");
            spec.getImplicitRequirements().add("Benchmark or verify throughput before and after optimization.");
            spec.getTechnicalConstraints().add("Maintain memory footprint within host runtime limits; prevent cache memory leaks.");
            spec.getTechnicalRisks().add("Cache invalidation staleness or concurrency race conditions.");
            spec.getExpectedBehavior().add("Measurable latency reduction with thread-safe access.");
            spec.getAcceptanceCriteria().add("Performance optimizations verify zero regression in functional correctness.");
        }

        if (lower.contains("fix") || lower.contains("bug") || lower.contains("error") || lower.contains("exception")) {
            spec.getSubObjectives().add("Locate root cause of defect and implement targeted defensive error handling.");
            spec.getResolvedAmbiguities().add("Targeted fix without disruptive architectural rewrites.");
            spec.getImplicitRequirements().add("Add regression test verifying the fix.");
            spec.getTechnicalConstraints().add("Preserve backward compatibility with existing callers.");
            spec.getTechnicalRisks().add("Unintended side effects on dependent callers.");
            spec.getExpectedBehavior().add("Deterministic handling of invalid inputs and failure states.");
            spec.getAcceptanceCriteria().add("Demonstrate failing test case succeeds after modification.");
        }

        if (lower.contains("refactor") || lower.contains("clean") || lower.contains("modular")) {
            spec.getSubObjectives().add("Improve modularity, reduce cyclomatic complexity, and adhere to SOLID principles.");
            spec.getResolvedAmbiguities().add("Structural refactoring preserving exact external behavior and signatures.");
            spec.getImplicitRequirements().add("Existing unit tests must continue to pass without modification.");
            spec.getTechnicalConstraints().add("Maintain module boundary integrity; avoid cyclic package dependencies.");
            spec.getTechnicalRisks().add("Subtle regression in unmocked edge cases.");
            spec.getAcceptanceCriteria().add("100% pass rate on existing test suites with cleaner abstractions.");
        }

        // Generic fallback items if specific domains weren't matched
        if (spec.getSubObjectives().isEmpty()) {
            spec.getSubObjectives().add("Execute architectural and functional improvements aligned with: " + rawRequest);
            spec.getResolvedAmbiguities().add("Focus on minimal surgical changes adhering to established project architecture.");
        }

        if (spec.getImplicitRequirements().isEmpty()) {
            spec.getImplicitRequirements().add("Preserve consistent code conventions, formatting, and idiom usage.");
            spec.getImplicitRequirements().add("Include appropriate automated unit tests for modified components.");
        }

        if (spec.getTechnicalConstraints().isEmpty()) {
            spec.getTechnicalConstraints().add("Preserve existing public API and binary interface compatibility.");
            spec.getTechnicalConstraints().add("Do not introduce unauthorized external dependencies or license conflicts.");
        }

        // 3. Grounding from TargetRealityModel
        if (realityModel != null) {
            if (realityModel.getDomain() != null && !realityModel.getDomain().isEmpty()) {
                spec.getScopeDefinitions().add("Target Project Domain: " + realityModel.getDomain());
            }
            if (!realityModel.getSubsystems().isEmpty()) {
                for (Subsystem sub : realityModel.getSubsystems()) {
                    spec.getAffectedAreas().add("Subsystem [" + sub.getName() + "]: " + sub.getPurpose());
                }
            }
            if (!realityModel.getHotspots().isEmpty()) {
                for (Hotspot h : realityModel.getHotspots()) {
                    spec.getAffectedAreas().add("Hotspot [" + h.getName() + "] in " + h.getFile() + ": " + h.getDescription());
                }
            }
            if (!realityModel.getArchitecturalFacts().isEmpty()) {
                for (ArchitecturalFact f : realityModel.getArchitecturalFacts()) {
                    spec.getTechnicalConstraints().add("Architectural Fact: " + f.getSubject() + " " + f.getPredicate() + " " + f.getDescription());
                }
            }
        }

        // 4. Grounding from TargetSnapshot
        if (snapshot != null) {
            Object tech = snapshot.getMetadata().get("detectedTechnologies");
            if (tech != null) {
                spec.getTechnicalConstraints().add("Technology Stack Grounding: " + tech);
            }
        }

        // 5. Acceptance criteria guarantees
        if (spec.getAcceptanceCriteria().isEmpty()) {
            spec.getAcceptanceCriteria().add("Verify changes compile successfully against target build configuration.");
            spec.getAcceptanceCriteria().add("Ensure all existing automated tests pass without regressions.");
            spec.getAcceptanceCriteria().add("New or modified logic is accompanied by unit tests.");
            spec.getAcceptanceCriteria().add("Target code modifications strictly satisfy: " + rawRequest);
        }

        return spec;
    }
}
