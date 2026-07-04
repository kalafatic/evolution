package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.EvolutionProgressPublisher;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.intent.IntentExpansionResult;
import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
import eu.kalafatic.evolution.controller.orchestration.util.ModelCapability;
import eu.kalafatic.evolution.controller.orchestration.util.ModelCapabilityDetector;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class DynamicSiblingGenerator {

	private final SessionContainer container;
	private final AiService aiService;

	private final PromptOptimizer promptOptimizer;
	private final ModelCapabilityDetector capabilityDetector;

	public DynamicSiblingGenerator(SessionContainer container, AiService aiService) {
		this.container = container;
		this.aiService = aiService;
		this.promptOptimizer = new PromptOptimizer(container);
		this.promptOptimizer.setAiService(aiService);

		this.capabilityDetector = new ModelCapabilityDetector();
	}

	// ============================================================
	// PRIMARY METHOD: 5 parameters (the one with all the logic)
	// ============================================================
	public List<JSONObject> generateSiblings(String userRequest, GoalModel goal, TaskContext context,
			EvolutionDimension activeDimension, int targetPopulation) throws Exception {

		boolean isMediated = ModeRecognizer.isMediatedMode(context);

		// STEP 1: Analyze intent
		context.log("[DYNAMIC] Analyzing user intent...");
		File projectRoot = context.getProjectRoot();
		PromptIntentAnalyzer intentAnalyzer = new PromptIntentAnalyzer(container, projectRoot);
		intentAnalyzer.setAiService(aiService);

		PromptIntentAnalyzer.IntentResult intentResult = intentAnalyzer.analyze(userRequest, context);
		context.log("[DYNAMIC] Intent: " + intentResult.getCategory() + " | SubIntent: " + intentResult.getSubIntent());

		// Map to IntentProfile
		IntentProfile profile = new IntentProfile();

		// GROUNDING IMPROVEMENT: Combine action and artifact for better LLM grounding
		String combinedGoal = goal.getPrimaryAction();
		if (goal.getRequestedArtifact() != null && !combinedGoal.toLowerCase().contains(goal.getRequestedArtifact().toLowerCase())) {
			combinedGoal += " (" + goal.getRequestedArtifact() + ")";
		}

		profile.primaryGoal = combinedGoal;
		profile.complexity = goal.getComplexity();
		profile.domain = goal.getDomain();
		profile.artifactType = intentResult.getTargetArtifact() != null ? intentResult.getTargetArtifact()
				: goal.getRequestedArtifact();
		profile.abstractionLevel = context.getOrchestrationState().getLockedAbstractionLevel() != null
				? context.getOrchestrationState().getLockedAbstractionLevel().name()
				: "IMPLEMENTATION";

		// STEP 2: Optimize prompt based on intent
		context.log("[DYNAMIC] Optimizing prompt for intent...");
		PromptStrategy strategy = promptOptimizer.optimizePrompt(profile, context);

		// MANDATE: Enforce target population for Darwinian search
		int siblingCount = targetPopulation;
		context.log("[DYNAMIC] Using format: " + strategy.format + " | Siblings: " + siblingCount);
		
		
		if (isMediated) {
		    strategy.format = "MEDIATED";
		}

		// STEP 3: Detect model capability
		String modelName = getModelName(context);
		ModelCapability capability = capabilityDetector.getModelCapability(modelName);
		context.log("[DYNAMIC] Model: " + modelName + " | Capability: " + capability.size);

		// ============================================================
		// STEP 4: ITERATIVE SIBLING GENERATION
		// Each sibling learns from all previous siblings
		// ============================================================
		List<JSONObject> variants = new ArrayList<>();
		List<String> previousSolutions = new ArrayList<>();
		List<String> previousStrategies = new ArrayList<>();
		List<String> previousClassNames = new ArrayList<>();

		for (int i = 0; i < siblingCount; i++) {
			String prompt = buildIterativePrompt(strategy, i, previousSolutions, previousStrategies, previousClassNames,
					context, activeDimension);

			context.log("[DYNAMIC] Generating sibling " + (i + 1) + "/" + siblingCount + " (learning from "
					+ previousSolutions.size() + " previous siblings)");

			JSONObject variant = generateSingleVariant(prompt, strategy, i, context);
			if (variant != null && validateVariant(variant, strategy)) {
				// Extract key information for the next iteration
				String code = variant.optString("implementation", "");
				String strategyName = variant.optString("strategy", "unknown");
				String className = variant.optString("class_name", "Unknown");

				previousSolutions.add(code);
				previousStrategies.add(strategyName);
				previousClassNames.add(className);

				variants.add(variant);

				// Update UI with progress
				EvolutionProgressPublisher.updateBranchStatus(context, variant.optString("id"),
						variant.optString("strategy"), "generated", null);
			}
		}

		// Ensure we have at least one variant
		if (variants.isEmpty()) {
			context.log("[DYNAMIC] No valid variants generated. Creating fallback.");
			JSONObject fallback = isMediated ? createMediatedFallbackVariant(profile, context) : createFallbackVariant(profile, context);
			variants.add(fallback);
		}

		return variants;
	}

	// ============================================================
	// SECONDARY METHOD: 16 parameters (calls the 5-parameter version)
	// ============================================================
	public List<JSONObject> generateSiblings(GoalModel goal, EvolutionDimension activeDimension, int branchingLimit,
			String basePrompt, String lineageContext, List<String> rejectedSiblings, TaskContext context,
			SemanticGenome genome, EvolutionTree tree, String currentParentId, int generation,
			BranchVariant.ReasoningLevel reasoningLevel, boolean architectureEnabled, boolean implementationEnabled,
			IntentExpansionResult expansion, Orchestrator orchestrator) throws Exception {

		List<JSONObject> uniqueVariants = new ArrayList<>();
		List<TrajectoryBlueprint> generatedBlueprints = new ArrayList<>();

		// ============================================================
		// 1. ITERATIVE BLUEPRINT GENERATION using TrajectoryTerritoryMapper
		// ============================================================
		TrajectoryTerritoryMapper mapper = new TrajectoryTerritoryMapper(container);
		mapper.setAiService(aiService);

		// Track what has been generated
		List<TrajectoryBlueprint> siblings = new ArrayList<>();
		TrajectoryBlueprint parentBlueprint = null; // Could be derived from currentParentId

		// Initialize SiblingGenerationContext for each iteration
		for (int i = 0; i < branchingLimit; i++) {
			context.log("[TERRITORY] Discovering sibling " + (i + 1) + "/" + branchingLimit);

			SiblingGenerationContext ctx = new SiblingGenerationContext();
			ctx.setGoal(goal);
			ctx.setDimension(activeDimension);
			ctx.setSiblingIndex(i);
			ctx.setParent(parentBlueprint);
			ctx.setOlderSiblings(new ArrayList<>(siblings)); // Pass only previous siblings
			ctx.setGenome(genome);
			ctx.setTree(tree);
			ctx.setCurrentParentId(currentParentId);
			ctx.setGeneration(generation);
			ctx.setExpansion(expansion);

			// Discover the next sibling using the mapper
			TrajectoryBlueprint bp = mapper.discoverNextSibling(ctx, context);

			if (bp != null) {
				// Add to list for future iterations
				siblings.add(bp);
				generatedBlueprints.add(bp);
				context.log("[TERRITORY] Discovered sibling " + (i + 1) + ": " + bp.getStrategy());
			} else {
				context.log("[TERRITORY] Failed to discover sibling " + (i + 1) + ". Breaking.");
				break;
			}
		}

		// ============================================================
		// 2. MATERIALIZE BLUEPRINTS INTO ACTUAL VARIANTS
		// ============================================================
		DarwinVariantSpawner spawner = new DarwinVariantSpawner(aiService);

		for (TrajectoryBlueprint bp : generatedBlueprints) {
			JSONObject variant = spawner.spawnSingleBlueprint(goal, bp, basePrompt, lineageContext, rejectedSiblings,
					null, // mutationContext
					false, // isMediated
					context, activeDimension, genome);

			if (variant != null) {
				// Mark which sibling this is
				variant.put("siblingIndex", generatedBlueprints.indexOf(bp));
				uniqueVariants.add(variant);
			}
		}

		// ============================================================
		// 3. FALLBACK: If not enough blueprints, call the 5-parameter version
		// ✅ THIS NOW CALLS THE OTHER METHOD, NOT ITSELF
		// ============================================================
		if (uniqueVariants.size() < 2) {
			context.log("[TERRITORY] Only " + uniqueVariants.size()
					+ " blueprints generated. Falling back to DynamicSiblingGenerator.");

			// ✅ Call the 5-parameter version
			List<JSONObject> fallbackVariants = this.generateSiblings(goal.getPrimaryAction(), // userRequest
					goal, // goal
					context, // context
					activeDimension, // activeDimension
					branchingLimit // targetPopulation
			);

			uniqueVariants.addAll(fallbackVariants);
		}

		// ============================================================
		// 4. DEDUPLICATE using semantic similarity
		// ============================================================
		uniqueVariants = deduplicateVariants(uniqueVariants, context);

		return uniqueVariants;
	}

	private String buildIterativePrompt(PromptStrategy strategy, int siblingIndex, List<String> previousSolutions,
			List<String> previousStrategies, List<String> previousClassNames, TaskContext context,
			EvolutionDimension activeDimension) throws Exception {

		StringBuilder prompt = new StringBuilder();
		boolean isMediated = ModeRecognizer.isMediatedMode(context);

	    // ============================================================
	    // 1. BASE INSTRUCTION — DIFFERENT FOR MEDIATED MODE
	    // ============================================================
	    if (isMediated) {
	    	strategy.format = "MEDIATED";
	        
	        prompt.append("You are performing a REPOSITORY ANALYSIS task.\n");
	        prompt.append("Your goal is to produce an ANALYSIS PACKAGE (Genome A/B) for the goal: ")
	              .append(strategy.intent.primaryGoal)
	              .append("\n\n");
	        prompt.append("You MUST NOT generate code or create files.\n");
	        prompt.append("You MUST analyze the existing repository structure and produce:\n");
	        prompt.append("  - Genome A: An optimized prompt for an external LLM\n");
	        prompt.append("  - Genome B: A curated set of files and architectural summary\n\n");
	    } else {
	        prompt.append("Generate a Java solution for: ")
	              .append(strategy.intent.primaryGoal)
	              .append("\n\n");
	    }

		// ============================================================
		// 2. CONTEXT: What has already been generated?
		// ============================================================
		if (!previousSolutions.isEmpty()) {
			prompt.append("PREVIOUS SOLUTIONS ALREADY GENERATED:\n");
			prompt.append("=========================================\n");

			for (int i = 0; i < previousSolutions.size(); i++) {
				String strategyName = previousStrategies.get(i);
				String className = previousClassNames.get(i);
				String code = previousSolutions.get(i);

				prompt.append("Sibling ").append(i + 1).append(": ").append(strategyName).append("\n");
				prompt.append("Class Name: ").append(className).append("\n");

				// Extract key characteristics to show what was done
				String summary = summarizeSolution(code);
				prompt.append("Approach: ").append(summary).append("\n");
				prompt.append("---\n");
			}

			prompt.append("\n");
			prompt.append("MANDATE: Your solution MUST be DIFFERENT from ALL previous siblings.\n");
			prompt.append("You MUST use a DIFFERENT class name, DIFFERENT structure, and DIFFERENT approach.\n\n");

			// List forbidden class names
			prompt.append("FORBIDDEN CLASS NAMES (already used):\n");
			for (String cn : previousClassNames) {
				prompt.append("- ").append(cn).append("\n");
			}
			prompt.append("\n");

			// List forbidden approaches
			prompt.append("ALREADY EXPLORED APPROACHES:\n");
			for (int i = 0; i < previousStrategies.size(); i++) {
				String approach = extractApproach(previousSolutions.get(i));
				prompt.append("- ").append(approach).append("\n");
			}
			prompt.append("\n");
		} else {
			// First sibling: any approach is valid
			prompt.append("This is the FIRST sibling. Any valid Java class is acceptable.\n\n");
		}

		// ============================================================
		// 3. SPECIFIC INSTRUCTION FOR THIS SIBLING
		// ============================================================
		if (previousSolutions.isEmpty()) {
			prompt.append("Create a simple Java class that prints text to the console.\n");
			prompt.append("Focus on correctness and clarity.\n");
		} else {
			// Force diversity by specifying what to do differently
			prompt.append("YOUR CHALLENGE: Create a solution that is DIFFERENT from all above.\n");

			if (activeDimension != null) {
				prompt.append("\n[EVOLUTIONARY_AXIS] Focus on varying this specific dimension:\n");
				prompt.append("Dimension: ").append(activeDimension.getId()).append("\n");
				prompt.append("Description: ").append(activeDimension.getDescription()).append("\n\n");
			}

			prompt.append("Consider these dimensions to vary:\n");
			prompt.append("- Class name (must be unique)\n");
			prompt.append("- Method organization (main only vs separate methods vs interface)\n");
			prompt.append("- Programming style (procedural vs OOP vs functional)\n");
			prompt.append("- Internal structure (single class vs multiple classes vs nested classes)\n");
			prompt.append("- Design pattern (factory vs strategy vs simple)\n");
			prompt.append("- Implementation approach (direct vs utility vs interface)\n\n");

			prompt.append("Choose an approach that is MEANINGFULLY DIFFERENT from what exists.\n");
		}

		// ============================================================
		// 4. FORMAT INSTRUCTION
		// ============================================================
		if ("MEDIATED".equals(strategy.format) || isMediated) {
			prompt.append(buildMediatedPrompt(strategy, siblingIndex, context));
		} else {
			switch (strategy.format) {
			case "STEP_BY_STEP":
				prompt.append(buildStepByStepPrompt(strategy, siblingIndex));
				break;
			case "JSON_SCHEMA":
				prompt.append(buildJsonSchemaPrompt(strategy, siblingIndex));
				break;
			case "SIMPLE_TEXT":
				prompt.append(buildSimpleTextPrompt(strategy, siblingIndex));
				break;
			case "CODE_ONLY":
				prompt.append(buildCodeOnlyPrompt(strategy, siblingIndex));
				break;
			default:
				prompt.append(buildSimpleTextPrompt(strategy, siblingIndex));
			}
		}

		// ============================================================
		// 5. CONSTRAINTS & VALIDATION
		// ============================================================
		if (!strategy.constraints.isEmpty()) {
			prompt.append("\n\nConstraints:\n");
			for (String constraint : strategy.constraints) {
				prompt.append("- ").append(constraint).append("\n");
			}
		}

		// Add uniqueness validation
		if (!previousClassNames.isEmpty()) {
			prompt.append("\n\n⚠️ UNIQUENESS REQUIREMENT:\n");
			prompt.append("Your class name MUST NOT be: ").append(String.join(", ", previousClassNames)).append("\n");
			prompt.append("Your approach MUST NOT be identical to any previous approach.\n");
		}

		return prompt.toString();
	}

	/**
	 * Extracts a summary of the solution approach.
	 */
	private String summarizeSolution(String code) {
		if (code == null || code.isEmpty())
			return "Unknown approach";

		StringBuilder summary = new StringBuilder();

		// Check for interface
		if (code.contains("interface")) {
			summary.append("Interface-driven design");
		} else if (code.contains("final class") || code.contains("static")) {
			summary.append("Utility/static approach");
		} else if (code.contains("implements")) {
			summary.append("Implementation of interface");
		} else if (code.contains("extends")) {
			summary.append("Inheritance approach");
		} else if (code.contains("private ") && !code.contains("public")) {
			summary.append("Encapsulated/private approach");
		} else {
			summary.append("Simple class approach");
		}

		// Check method organization
		if (code.contains("main") && code.contains("print(") && !code.contains("main")) {
			summary.append(" with separate print method");
		} else if (code.contains("main")) {
			summary.append(" with main-only");
		} else {
			summary.append(" without main");
		}

		// Extract class name
		Pattern pattern = Pattern.compile("(?:public\\s+)?class\\s+(\\w+)");
		Matcher matcher = pattern.matcher(code);
		if (matcher.find()) {
			summary.append(" (class: ").append(matcher.group(1)).append(")");
		}

		return summary.toString();
	}

	/**
	 * Extracts the approach from a solution.
	 */
	private String extractApproach(String code) {
		if (code == null || code.isEmpty())
			return "Unknown";

		if (code.contains("interface") && code.contains("implements")) {
			return "Interface + implementation";
		} else if (code.contains("final class") || code.contains("private ")) {
			return "Utility/static class";
		} else if (code.contains("extends")) {
			return "Inheritance";
		} else if (code.contains("print(") && !code.contains("static") && code.contains("new")) {
			return "Instance method approach";
		} else if (code.contains("static") && code.contains("print(")) {
			return "Static method approach";
		} else if (code.contains("main") && !code.contains("print(")) {
			return "Main-only approach";
		} else {
			return "Simple class";
		}
	}

	/**
	 * Gets the model name from the context.
	 */
	private String getModelName(TaskContext context) {
		if (context != null && context.getOrchestrator() != null) {
			var orchestrator = context.getOrchestrator();
			// Try Ollama first
			if (orchestrator.getOllama() != null) {
				String model = orchestrator.getOllama().getModel();
				if (model != null && !model.isEmpty()) {
					return model;
				}
			}
			// Try provider
//            if (orchestrator.getProvider() != null) {
//                String model = orchestrator.getProvider().getModel();
//                if (model != null && !model.isEmpty()) {
//                    return model;
//                }
//            }
//            // Try AI service
//            if (orchestrator.getAiService() != null) {
//                String model = orchestrator.getAiService().getModel();
//                if (model != null && !model.isEmpty()) {
//                    return model;
//                }
//            }
		}
		// Fallback: check system property or environment
		String modelFromProperty = System.getProperty("ollama.model");
		if (modelFromProperty != null && !modelFromProperty.isEmpty()) {
			return modelFromProperty;
		}
		return "unknown";
	}

	private String buildDynamicPrompt(PromptStrategy strategy, int siblingIndex, TaskContext context) {
		StringBuilder prompt = new StringBuilder();

		boolean isMediated = ModeRecognizer.isMediatedMode(context);

		// Base instruction
		if (isMediated) {
			prompt.append("Perform a repository-grounded architectural analysis for: ")
					.append(strategy.intent.primaryGoal).append("\n\n");
		} else {
			prompt.append("Generate a Java solution for: ").append(strategy.intent.primaryGoal).append("\n\n");
		}

		// Tone
		if ("instructional".equals(strategy.tone)) {
			prompt.append("Follow these instructions carefully:\n");
		} else if ("conversational".equals(strategy.tone)) {
			prompt.append("Let's create a Java class together.\n");
		}

		// Format-specific prompt construction
		if (isMediated) {
			prompt.append(buildMediatedPrompt(strategy, siblingIndex, context));
		} else {
			switch (strategy.format) {
			case "STEP_BY_STEP":
				prompt.append(buildStepByStepPrompt(strategy, siblingIndex));
				break;
			case "JSON_SCHEMA":
				prompt.append(buildJsonSchemaPrompt(strategy, siblingIndex));
				break;
			case "SIMPLE_TEXT":
				prompt.append(buildSimpleTextPrompt(strategy, siblingIndex));
				break;
			case "CODE_ONLY":
				prompt.append(buildCodeOnlyPrompt(strategy, siblingIndex));
				break;
			default:
				prompt.append(buildSimpleTextPrompt(strategy, siblingIndex));
			}
		}

		// Add examples
		if (!strategy.examples.isEmpty()) {
			prompt.append("\n\nExamples:\n");
			for (String example : strategy.examples) {
				prompt.append("- ").append(example).append("\n");
			}
		}

		// Add constraints
		if (!strategy.constraints.isEmpty()) {
			prompt.append("\n\nConstraints:\n");
			for (String constraint : strategy.constraints) {
				prompt.append("- ").append(constraint).append("\n");
			}
		}

		// Add validation expectations
		if (!strategy.validationRules.isEmpty()) {
			prompt.append("\n\nYour solution must:\n");
			for (String rule : strategy.validationRules) {
				prompt.append("- ").append(rule).append("\n");
			}
		}

		// Sibling variation hint
		if (siblingIndex > 0) {
			prompt.append("\n\nMake this solution DIFFERENT from the previous one.\n");
			prompt.append("Focus on a different approach or style.\n");
		}

		return prompt.toString();
	}

	private String buildStepByStepPrompt(PromptStrategy strategy, int index) {
		StringBuilder sb = new StringBuilder();
		sb.append("Step 1: What should the class be named?\n");
		sb.append("Step 2: What should the main method do?\n");
		sb.append("Step 3: Write the complete Java code.\n\n");
		sb.append("Return your answer in this format:\n");
		sb.append("CLASS_NAME: [name]\n");
		sb.append("METHOD: [description]\n");
		sb.append("CODE:\n");
		sb.append("```java\n");
		sb.append("[code here]\n");
		sb.append("```\n");
		return sb.toString();
	}

	private String buildJsonSchemaPrompt(PromptStrategy strategy, int index) {
		return """
				Return a JSON object with this structure:

				{
				    "class_name": "Name of the class",
				    "fields": ["field1", "field2"],
				    "methods": [
				        {
				            "name": "methodName",
				            "returnType": "void",
				            "parameters": ["String text"],
				            "body": "System.out.println(text);"
				        }
				    ],
				    "main_method": {
				        "body": "Printer printer = new Printer(); printer.print(\"Hello\");"
				    }
				}
				""";
	}

	private String buildSimpleTextPrompt(PromptStrategy strategy, int index) {
		return """
				Write a complete Java class that prints text to the console.

				Include:
				1. The class definition
				2. A method that prints text
				3. A main method that demonstrates it

				Return ONLY the Java code, no explanation.
				""";
	}

	private String buildCodeOnlyPrompt(PromptStrategy strategy, int index) {
		return """
				Write a Java class that prints text.
				Only output the code, no markdown, no explanation.
				""";
	}

	private String buildMediatedPrompt(PromptStrategy strategy, int index, TaskContext context) {
	    StringBuilder sb = new StringBuilder();
	    
	    sb.append("⚠️ CRITICAL: You are in MEDIATED ANALYSIS MODE.\n");
	    sb.append("DO NOT generate code. DO NOT create files. DO NOT write classes.\n\n");
	    
	    sb.append("TASK: Produce a comprehensive architectural analysis package for the goal: ")
	      .append(strategy.intent.primaryGoal)
	      .append("\n\n");
	    
	    sb.append("GENOME A (The Optimized Prompt for External LLM):\n");
	    sb.append("  Create a concise, context-rich prompt that an external LLM can use to analyze this codebase.\n");
	    sb.append("  The prompt should:\n");
	    sb.append("  - State the purpose of the analysis\n");
	    sb.append("  - List the key files that need to be examined\n");
	    sb.append("  - Ask specific questions about the architecture\n");
	    sb.append("  - Request identification of patterns, anti-patterns, and improvement opportunities\n\n");
	    
	    sb.append("GENOME B (The Context Package):\n");
	    sb.append("  Identify 8-16 critical files from the repository that are essential for understanding the architecture.\n");
	    sb.append("  For each file, explain its role in the system (1-2 sentences).\n");
	    sb.append("  Provide a concise architecture summary (2-3 sentences).\n");
	    sb.append("  List key dependencies and their relationships.\n\n");
	    
	    sb.append("OUTPUT FORMAT (EXACTLY):\n");
	    sb.append("=========================================\n");
	    sb.append("ARCHITECTURE_SUMMARY: [2-3 sentences describing the overall architecture]\n");
	    sb.append("\n");
	    sb.append("CRITICAL_FILES:\n");
	    sb.append("  - path/to/file1.java: [role and importance, 1-2 sentences]\n");
	    sb.append("  - path/to/file2.java: [role and importance, 1-2 sentences]\n");
	    sb.append("  ... (8-16 files total)\n");
	    sb.append("\n");
	    sb.append("DEPENDENCIES:\n");
	    sb.append("  - [dependency name]: [purpose]\n");
	    sb.append("\n");
	    sb.append("OPTIMIZED_PROMPT:\n");
	    sb.append("[The complete prompt for the external LLM]\n");
	    sb.append("\n");
	    sb.append("EXECUTION_INSTRUCTIONS:\n");
	    sb.append("[Specific instructions for using the analysis package]\n");
	    sb.append("=========================================\n\n");
	    
	    sb.append("⚠️ REMINDER: This is an ANALYSIS task. Your output should be the analysis package above.\n");
	    sb.append("⚠️ DO NOT include code. DO NOT include class definitions. ONLY analysis.\n");
	    
	    return sb.toString();
	}

	private JSONObject generateSingleVariant(String prompt, PromptStrategy strategy, int index, TaskContext context) {
		String variantId = "variant-" + System.currentTimeMillis() + "-" + index;
		String variantStrategy = strategy.intent.primaryGoal + " - Variant " + (index + 1);
		boolean isMediated = ModeRecognizer.isMediatedMode(context);

		try {
			EvolutionProgressPublisher.updateBranchStatus(context, variantId, variantStrategy, "analyzing", null);
			String response = aiService.sendRequest(context.getOrchestrator(), prompt, context);

			// Parse based on format
			JSONObject variant = new JSONObject();
			variant.put("id", variantId);
			variant.put("strategy", variantStrategy);

			if (isMediated) {
				populateMediatedVariant(variant, response, strategy, context);
			} else {
				// Extract code based on format
				String code = extractCode(response, strategy.format);
				variant.put("implementation", code);

				// Extract class name
				String className = extractClassName(code);
				variant.put("class_name", className);

				// Build action
				JSONObject action = new JSONObject();
				action.put("domain", "file");
				action.put("operation", "WRITE");
				action.put("target", "src/main/java/com/example/" + className + ".java");
				action.put("implementation", code);
				JSONArray actions = new JSONArray();
				actions.put(action);
				variant.put("actions", actions);
			}

			// Fill required fields
			variant.put("strategy_type", "PROBABLE_SURVIVOR");
			variant.put("semantic_anchor", strategy.intent.primaryGoal);
			variant.put("survival_argument", "Generated for: " + strategy.intent.primaryGoal);
			variant.put("tradeoffs", isMediated ? "High-signal discovery" : "Simple implementation");
			variant.put("reasoning_level", "MINIMAL");
			variant.put("architecture_enabled", isMediated);
			variant.put("implementation_enabled", !isMediated);

			// Engineering dimensions
			JSONObject dims = new JSONObject();
			dims.put("active_dimension", isMediated ? "ARCHITECTURE" : "IMPLEMENTATION");
			dims.put("active_dimension_description",
					isMediated ? "Establish architectural grounding" : "Define the core functionality");
			dims.put("execution_model", isMediated ? "mediated" : "atomic");
			dims.put("abstraction_depth", isMediated ? "high" : "low");
			variant.put("engineering_dimensions", dims);

			// Projected steps
			JSONArray steps = new JSONArray();
			if (isMediated) {
				steps.put("Discovery repository structure");
				steps.put("Synthesize optimized prompt");
			} else {
				steps.put("Implement logic");
			}
			variant.put("projected_steps", steps);

			EvolutionProgressPublisher.updateBranchStatus(context, variantId, variantStrategy, "planned", null);
			return variant;

		} catch (Exception e) {
			context.log("[DYNAMIC] Failed to generate variant: " + e.getMessage());
			EvolutionProgressPublisher.updateBranchStatus(context, variantId, variantStrategy, "failed", null);
			return null;
		}
	}

	private void populateMediatedVariant(JSONObject variant, String response, PromptStrategy strategy,
			TaskContext context) {
		String optimizedPrompt = extractField(response, "PROMPT:");
		String architecture = extractField(response, "ARCHITECTURE:");
		String filesStr = extractField(response, "FILES:");
		String instructions = extractField(response, "INSTRUCTIONS:");

		JSONObject med = new JSONObject();
		med.put("prompt", optimizedPrompt);
		med.put("architecture_summary", architecture);
		med.put("execution_instructions", instructions);

		JSONArray selectedFiles = new JSONArray();
		if (filesStr != null && !filesStr.isEmpty()) {
			String[] parts = filesStr.split(",");
			for (String p : parts) {
				selectedFiles.put(p.trim());
			}
		}
		med.put("selected_files", selectedFiles);
		variant.put("mediation_candidate", med);

		// ACTION: Mediated mode uses ANALYZE workspace
		JSONObject action = new JSONObject();
		action.put("domain", "kernel");
		action.put("operation", "ANALYZE");
		action.put("target", "workspace");
		action.put("description", "Perform cognitive analysis of the repository");

		JSONArray actions = new JSONArray();
		actions.put(action);
		variant.put("actions", actions);
	}

	private String extractField(String response, String label) {
		if (response == null)
			return "";
		int start = response.indexOf(label);
		if (start == -1)
			return "";

		start += label.length();

		// Find the next label to determine the end of the current field
		String[] labels = { "PROMPT:", "ARCHITECTURE:", "FILES:", "INSTRUCTIONS:" };
		int end = response.length();

		for (String l : labels) {
			int nextLabelStart = response.indexOf(l, start);
			if (nextLabelStart != -1 && nextLabelStart < end) {
				end = nextLabelStart;
			}
		}

		return response.substring(start, end).trim();
	}

	private String extractCode(String response, String format) {
		if (response == null) return "";

		// 0. Handle "Implementation:" or "CODE:" prefix explicitly before code blocks
		Pattern labelPattern = Pattern.compile("(?:Implementation|CODE|Java Code):\\s*\\n?(.*)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		Matcher labelMatcher = labelPattern.matcher(response);
		String workingResponse = response;
		if (labelMatcher.find()) {
			workingResponse = labelMatcher.group(1).trim();
		}

		// 1. Try to extract from specifically marked code blocks first
		Pattern codeBlockPattern = Pattern.compile("```(?:java|json)?\\s*\\n(.*?)\\n```", Pattern.DOTALL);
		Matcher matcher = codeBlockPattern.matcher(workingResponse);

		while (matcher.find()) {
			String block = matcher.group(1).trim();

			// If it's a JSON block, it might contain the code inside
			if (block.startsWith("{")) {
				try {
					JSONObject obj = new JSONObject(block);
					if (obj.has("code")) return obj.getString("code");
					if (obj.has("implementation")) return obj.getString("implementation");
				} catch (Exception e) {
					// Fall through to returning the block if it looks like Java
				}
			}

			// If it contains 'class ' it's probably the Java code we want
			if (block.contains("class ") || block.contains("interface ") || block.contains("enum ")) {
				return block;
			}
		}

		// 2. If it's CODE_ONLY format, return as-is (but try to strip markdown)
		if ("CODE_ONLY".equals(format)) {
			return workingResponse.replaceAll("```(?:java)?", "").replaceAll("```", "").trim();
		}

		// 3. Try to extract from STEP_BY_STEP format
		if ("STEP_BY_STEP".equals(format)) {
			Pattern stepPattern = Pattern.compile("CODE:\\s*\\n?```(?:java|json)?\\s*\\n(.*?)\\n```", Pattern.DOTALL);
			matcher = stepPattern.matcher(workingResponse);
			if (matcher.find()) {
				String block = matcher.group(1).trim();
				if (block.startsWith("{")) {
					try {
						JSONObject obj = new JSONObject(block);
						if (obj.has("code")) return obj.getString("code");
						if (obj.has("implementation")) return obj.getString("implementation");
					} catch (Exception e) {}
				}
				return block;
			}
		}

		// 4. Try to extract from the root response if it's JSON
		try {
			String trimmedResponse = workingResponse.trim();
			if (trimmedResponse.startsWith("{")) {
				JSONObject obj = new JSONObject(trimmedResponse);
				if (obj.has("code")) return obj.getString("code");
				if (obj.has("implementation")) return obj.getString("implementation");
			}
		} catch (Exception e) {}

		// 5. SEARCH FALLBACK: If we still don't have a clean class, search for the first occurrence of "class "
		if (workingResponse.contains("public class ") || workingResponse.contains("class ")) {
			int start = workingResponse.indexOf("public class ");
			if (start == -1) start = workingResponse.indexOf("class ");

			if (start != -1) {
				// Try to find the end of the class (very naive, but better than junk text)
				String subset = workingResponse.substring(start);
				// If there's a closing ``` after it, stop there
				int end = subset.indexOf("```");
				if (end != -1) {
					return subset.substring(0, end).trim();
				}
				return subset.trim();
			}
		}

		// 6. Final fallback: return the first code block if anything was found, or the raw response
		matcher.reset();
		if (matcher.find()) {
			return matcher.group(1).trim();
		}

		return workingResponse.trim();
	}

	private boolean validateVariant(JSONObject variant, PromptStrategy strategy) {
		// Mediated mode validation
		if (variant.has("mediation_candidate")) {
			JSONObject med = variant.getJSONObject("mediation_candidate");
			return med.has("prompt") && !med.optString("prompt").isEmpty();
		}

		// Check required fields
		if (!variant.has("implementation") || variant.optString("implementation").isEmpty()) {
			return false;
		}

		String code = variant.optString("implementation");

		// Check for class definition
		if (!code.contains("class")) {
			return false;
		}

		// Check for main method if required
		if (strategy.validationRules.stream().anyMatch(r -> r.contains("main"))) {
			if (!code.contains("main")) {
				return false;
			}
		}

		// Check for System.out.println if required (for print tasks)
		if (strategy.intent.primaryGoal.toLowerCase().contains("print")
				|| strategy.intent.primaryGoal.toLowerCase().contains("text")) {
			if (!code.contains("System.out.println") && !code.contains("System.out.print")) {
				// Allow fallback, but log warning
				return true; // Don't reject, just warn
			}
		}

		return true;
	}

	private JSONObject createFallbackVariant(IntentProfile intent, TaskContext context) {
		JSONObject variant = new JSONObject();
		String className = "Printer";
		String code = "public class Printer {\n" + "    public void print(String text) {\n"
				+ "        System.out.println(text);\n" + "    }\n" + "    \n"
				+ "    public static void main(String[] args) {\n" + "        Printer printer = new Printer();\n"
				+ "        printer.print(\"Hello, World!\");\n" + "    }\n" + "}";

		variant.put("id", "variant-fallback-" + System.currentTimeMillis());
		variant.put("strategy", "Fallback: " + intent.primaryGoal);
		variant.put("implementation", code);
		variant.put("class_name", className);

		JSONObject action = new JSONObject();
		action.put("domain", "file");
		action.put("operation", "WRITE");
		action.put("target", "src/main/java/com/example/" + className + ".java");
		action.put("implementation", code);
		JSONArray actions = new JSONArray();
		actions.put(action);
		variant.put("actions", actions);

		variant.put("strategy_type", "PROBABLE_SURVIVOR");
		variant.put("semantic_anchor", "Fallback implementation");
		variant.put("survival_argument", "Generated as fallback");
		variant.put("tradeoffs", "Simple implementation");

		JSONObject dims = new JSONObject();
		dims.put("active_dimension", "IMPLEMENTATION");
		variant.put("engineering_dimensions", dims);

		return variant;
	}
	
	private JSONObject createMediatedFallbackVariant(IntentProfile intent, TaskContext context) {
	    JSONObject variant = new JSONObject();
	    
	    // Create a mediated analysis package instead of code
	    String analysisPackage = 
	        "ARCHITECTURE_SUMMARY: Analysis of the repository structure.\n\n" +
	        "CRITICAL_FILES:\n" +
	        "  - pom.xml: Build configuration\n" +
	        "  - src/main/java/com/example/App.java: Main entry point\n\n" +
	        "OPTIMIZED_PROMPT: Analyze this Java application's architecture...\n\n" +
	        "EXECUTION_INSTRUCTIONS: Review the files and identify architectural patterns.";
	    
	    variant.put("id", "mediated-fallback-" + System.currentTimeMillis());
	    variant.put("strategy", "Analysis: " + intent.primaryGoal);
	    variant.put("strategy_type", "PHILOSOPHY_MAPPING");
	    variant.put("semantic_anchor", "Architectural analysis package");
	    variant.put("survival_argument", "Generated as fallback analysis package");
	    variant.put("tradeoffs", "High-signal discovery, no code generation");
	    variant.put("architecture_enabled", true);
	    variant.put("implementation_enabled", false);
	    
	    // Store analysis in mediation_candidate
	    JSONObject med = new JSONObject();
	    med.put("prompt", "Analyze the repository structure and identify key architectural patterns.");
	    med.put("architecture_summary", "Repository analysis package");
	    med.put("execution_instructions", "Review the provided analysis package.");
	    med.put("selected_files", new JSONArray());
	    variant.put("mediation_candidate", med);
	    
	    return variant;
	}

	// In DynamicSiblingGenerator.java

	/**
	 * Removes semantically duplicate variants from the list. Compares
	 * implementations for similarity.
	 */
	private List<JSONObject> deduplicateVariants(List<JSONObject> variants, TaskContext context) {
		if (variants == null || variants.size() <= 1) {
			return variants;
		}

		List<JSONObject> unique = new ArrayList<>();

		for (JSONObject variant : variants) {
			String code = variant.optString("implementation", "");
			String strategy = variant.optString("strategy", "");

			// Skip empty code
			if (code.isEmpty() || code.length() < 10) {
				context.log("[DEDUPE] Skipping empty variant: " + strategy);
				continue;
			}

			// Check if this variant is a duplicate of any existing one
			boolean isDuplicate = false;
			for (JSONObject existing : unique) {
				String existingCode = existing.optString("implementation", "");

				// Calculate similarity
				double similarity = calculateSemanticSimilarity(code, existingCode);

				if (similarity > 0.85) {
					isDuplicate = true;
					context.log("[DEDUPE] Duplicate detected: '" + strategy + "' is "
							+ String.format("%.2f", similarity * 100) + "% similar to '"
							+ existing.optString("strategy", "unknown") + "'");
					break;
				}
			}

			if (!isDuplicate) {
				unique.add(variant);
				context.log("[DEDUPE] Unique variant: " + strategy);
			}
		}

		context.log("[DEDUPE] Reduced from " + variants.size() + " to " + unique.size() + " unique variants");

		// If we lost too many, inject synthetic alternatives
		if (unique.size() < 2 && variants.size() > 1) {
			context.log("[DEDUPE] Too few unique variants (" + unique.size() + "). Attempting to salvage...");

			// Try to keep at least the first variant and one more
			if (unique.isEmpty() && !variants.isEmpty()) {
				unique.add(variants.get(0));
			}
			if (unique.size() < 2 && variants.size() > 1) {
				// Find the most different from the first
				JSONObject first = unique.get(0);
				String firstCode = first.optString("implementation", "");

				JSONObject mostDifferent = null;
				double lowestSimilarity = 1.0;

				for (JSONObject v : variants) {
					if (v == first)
						continue;
					String vCode = v.optString("implementation", "");
					double sim = calculateSemanticSimilarity(firstCode, vCode);
					if (sim < lowestSimilarity) {
						lowestSimilarity = sim;
						mostDifferent = v;
					}
				}

				if (mostDifferent != null && lowestSimilarity < 0.95) {
					unique.add(mostDifferent);
					context.log("[DEDUPE] Salvaged most different variant: "
							+ mostDifferent.optString("strategy", "unknown") + " (similarity: "
							+ String.format("%.2f", lowestSimilarity * 100) + "%)");
				}
			}
		}

		return unique;
	}

	/**
	 * Calculates semantic similarity between two code snippets. Uses multiple
	 * heuristics for robust comparison.
	 */
	private double calculateSemanticSimilarity(String code1, String code2) {
		if (code1 == null || code2 == null)
			return 0.0;
		if (code1.equals(code2))
			return 1.0;
		if (code1.isEmpty() || code2.isEmpty())
			return 0.0;

		// Normalize both
		String norm1 = normalizeCode(code1);
		String norm2 = normalizeCode(code2);

		// 1. Jaccard similarity on normalized code
		double jaccard = calculateJaccard(norm1, norm2);

		// 2. Class name similarity
		double classSimilarity = compareClassNames(code1, code2);

		// 3. Method structure similarity
		double methodSimilarity = compareMethodStructures(code1, code2);

		// 4. String content similarity (what they print)
		double stringSimilarity = comparePrintContent(code1, code2);

		// Weighted average
		double result = (jaccard * 0.35) + (classSimilarity * 0.25) + (methodSimilarity * 0.25)
				+ (stringSimilarity * 0.15);

		return Math.min(1.0, result);
	}

	/**
	 * Normalizes code for comparison.
	 */
	private String normalizeCode(String code) {
		if (code == null)
			return "";

		// Remove comments
		String normalized = code.replaceAll("//.*?\\n", "\n").replaceAll("/\\*.*?\\*/", "");

		// Remove whitespace
		normalized = normalized.replaceAll("\\s+", " ");

		// Remove specific string literals (but keep their pattern)
		normalized = normalized.replaceAll("\"[^\"]*\"", "\"TEXT\"");

		// Remove numbers
		normalized = normalized.replaceAll("\\d+", "0");

		return normalized.trim();
	}

	/**
	 * Calculates Jaccard similarity between two strings.
	 */
	private double calculateJaccard(String s1, String s2) {
		if (s1.isEmpty() && s2.isEmpty())
			return 1.0;
		if (s1.isEmpty() || s2.isEmpty())
			return 0.0;

		// Split into tokens
		String[] tokens1 = s1.split("\\s+|(?=[{}()])|(?<=[{}()])");
		String[] tokens2 = s2.split("\\s+|(?=[{}()])|(?<=[{}()])");

		java.util.Set<String> set1 = new java.util.HashSet<>(Arrays.asList(tokens1));
		java.util.Set<String> set2 = new java.util.HashSet<>(Arrays.asList(tokens2));

		// Intersection
		java.util.Set<String> intersection = new java.util.HashSet<>(set1);
		intersection.retainAll(set2);

		// Union
		java.util.Set<String> union = new java.util.HashSet<>(set1);
		union.addAll(set2);

		if (union.isEmpty())
			return 0.0;
		return (double) intersection.size() / union.size();
	}

	/**
	 * Compares class names from two code snippets.
	 */
	private double compareClassNames(String code1, String code2) {
		String className1 = extractClassName(code1);
		String className2 = extractClassName(code2);

		if (className1 == null || className2 == null)
			return 0.0;
		if (className1.equals(className2))
			return 1.0;

		// If one contains the other or they're similar
		String lower1 = className1.toLowerCase();
		String lower2 = className2.toLowerCase();

		if (lower1.contains(lower2) || lower2.contains(lower1)) {
			return 0.5;
		}

		// Levenshtein distance for similar names
		int distance = getLevenshteinDistance(lower1, lower2);
		int maxLen = Math.max(lower1.length(), lower2.length());
		if (maxLen == 0)
			return 0.0;

		return 1.0 - ((double) distance / maxLen);
	}

	/**
	 * Compares method structures between two code snippets.
	 */
	private double compareMethodStructures(String code1, String code2) {
		List<String> methods1 = extractMethodSignatures(code1);
		List<String> methods2 = extractMethodSignatures(code2);

		if (methods1.isEmpty() && methods2.isEmpty())
			return 1.0;
		if (methods1.isEmpty() || methods2.isEmpty())
			return 0.0;

		// Count common method signatures
		int common = 0;
		for (String m1 : methods1) {
			for (String m2 : methods2) {
				if (m1.equals(m2) || m1.contains(m2) || m2.contains(m1)) {
					common++;
					break;
				}
			}
		}

		return (double) common / Math.max(methods1.size(), methods2.size());
	}

	/**
	 * Compares the text being printed.
	 */
	private double comparePrintContent(String code1, String code2) {
		String content1 = extractPrintContent(code1);
		String content2 = extractPrintContent(code2);

		if (content1.isEmpty() && content2.isEmpty())
			return 1.0;
		if (content1.isEmpty() || content2.isEmpty())
			return 0.0;

		// If they contain the same phrase or similar
		if (content1.equals(content2))
			return 1.0;
		if (content1.contains(content2) || content2.contains(content1))
			return 0.7;

		// Word overlap
		String[] words1 = content1.split("\\s+");
		String[] words2 = content2.split("\\s+");

		int common = 0;
		for (String w1 : words1) {
			for (String w2 : words2) {
				if (w1.equalsIgnoreCase(w2)) {
					common++;
					break;
				}
			}
		}

		return (double) common / Math.max(words1.length, words2.length);
	}

	/**
	 * Extracts method signatures from code.
	 */
	private List<String> extractMethodSignatures(String code) {
		List<String> methods = new ArrayList<>();
		Pattern pattern = Pattern
				.compile("(?:public|private|protected)?\\s*(?:static)?\\s*\\w+\\s+(\\w+)\\s*\\([^)]*\\)");
		Matcher matcher = pattern.matcher(code);
		while (matcher.find()) {
			methods.add(matcher.group(1));
		}
		return methods;
	}

	/**
	 * Extracts the text being printed.
	 */
	private String extractPrintContent(String code) {
		Pattern pattern = Pattern.compile("System\\.out\\.println\\s*\\(\"([^\"]*)\"\\)\\s*;");
		Matcher matcher = pattern.matcher(code);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "";
	}

	/**
	 * Simple Levenshtein distance implementation.
	 */
	private int getLevenshteinDistance(String s1, String s2) {
		int len1 = s1.length();
		int len2 = s2.length();

		int[][] dp = new int[len1 + 1][len2 + 1];

		for (int i = 0; i <= len1; i++) {
			dp[i][0] = i;
		}
		for (int j = 0; j <= len2; j++) {
			dp[0][j] = j;
		}

		for (int i = 1; i <= len1; i++) {
			for (int j = 1; j <= len2; j++) {
				int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
				dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
			}
		}

		return dp[len1][len2];
	}

	/**
	 * Extracts class name from code.
	 */
	private String extractClassName(String code) {
		Pattern pattern = Pattern.compile("(?:public\\s+)?class\\s+(\\w+)");
		Matcher matcher = pattern.matcher(code);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}
}