package eu.kalafatic.evolution.controller.orchestration.intent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.PromptIntentAnalyzer;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetClassificationDetector;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetClassificationDetector.ClassificationResult;
import eu.kalafatic.evolution.controller.orchestration.ConversationState;
import eu.kalafatic.evolution.controller.orchestration.PlatformType;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.intent.model.ExecutionBranch;
import eu.kalafatic.evolution.controller.orchestration.intent.model.ReconstructedIntent;
import eu.kalafatic.evolution.controller.orchestration.intent.model.ReconstructedIntent.ActionType;
import eu.kalafatic.evolution.controller.orchestration.intent.model.ReconstructedIntent.TargetRole;
import eu.kalafatic.evolution.controller.orchestration.selfdev.DeveloperContextProvider;
import eu.kalafatic.evolution.controller.parsers.structured.StructuredResponsePipeline;

/**
 * Foundational service for Intent Understanding and Intent Reconstruction.
 * <p>
 * Evaluates:
 * 1. User intention (explicit vs implicit, action type, conversational context).
 * 2. Target data (project, folder, repository, docs, models, datasets; role as target, context, or ambiguous).
 * 3. Technical context (leveraging DeveloperContextProvider for Java/development requests).
 * 4. Local AI execution branch generation: constructs 3-4 distinct Darwin execution branches.
 * 5. Deterministic fallback handling when AI is offline, timed out, or malformed.
 */
public class IntentUnderstandingService {

    private final StructuredResponsePipeline pipeline = new StructuredResponsePipeline();
    private final DeveloperContextProvider devContextProvider = new DeveloperContextProvider();

    private static final String SYSTEM_PROMPT =
        "Role: Darwin Foundational Intent Understanding & Reconstruction Engine.\n" +
        "Goal: Deeply understand what the user wants to achieve, interpret supplied target data, and generate 3 to 4 meaningful Darwin execution branches.\n\n" +
        "CRITICAL RESPONSIBILITIES:\n" +
        "1. Understand what the user explicitly requests vs implicitly expects.\n" +
        "2. Identify the action type: QUESTION, CONVERSATION, PROGRAMMING, ANALYSIS, MODIFICATION, INVESTIGATION, or CLARIFICATION_NEEDED.\n" +
        "3. Interpret target data: distinguish whether target is to be MODIFIED, merely ANALYZED, used as CONTEXT, or is AMBIGUOUS.\n" +
        "   - E.g. 'Explain what this application does' on a repo is ANALYSIS_ONLY, NOT code modification.\n" +
        "   - E.g. 'Improve this' with no specific goal is AMBIGUOUS and requires clarification.\n" +
        "4. Generate 3 to 4 MEANINGFUL Darwin execution branches (e.g. DEVELOPER with project context, DEVELOPER new component, ASSISTED_CODING, CLARIFICATION, or ANALYTICAL/EXPLANATION).\n" +
        "   Each branch MUST have: interpretation, proposedMode (DEVELOPER, SIMPLE_CHAT, ASSISTED_CODING, HYBRID_MANUAL_EXPORT), expectedResult, advantages, limitations, risks, confidence (0.0-1.0), and selectionRationale.\n" +
        "5. Distinguish confirmed facts from assumptions. Never fabricate missing requirements.\n" +
        "6. Select exactly ONE winning branch based on evidence.\n\n" +
        "OUTPUT SCHEMA (JSON ONLY):\n" +
        "{\n" +
        "  \"actionType\": \"PROGRAMMING|QUESTION|CONVERSATION|ANALYSIS|MODIFICATION|INVESTIGATION|CLARIFICATION_NEEDED\",\n" +
        "  \"primaryObjective\": \"string\",\n" +
        "  \"secondaryObjectives\": [\"string\"],\n" +
        "  \"expectedResult\": \"string\",\n" +
        "  \"targetRole\": \"NONE|MODIFICATION_TARGET|ANALYSIS_ONLY|CONTEXTUAL_REFERENCE|AMBIGUOUS\",\n" +
        "  \"targetDescription\": \"string\",\n" +
        "  \"technicalConstraints\": [\"string\"],\n" +
        "  \"confirmedFacts\": [\"string\"],\n" +
        "  \"assumptions\": [\"string\"],\n" +
        "  \"missingInformation\": [\"string\"],\n" +
        "  \"ambiguities\": [\"string\"],\n" +
        "  \"clarificationRequired\": false,\n" +
        "  \"clarificationQuestion\": \"string\",\n" +
        "  \"branches\": [\n" +
        "    {\n" +
        "      \"id\": \"branch-1\",\n" +
        "      \"name\": \"string\",\n" +
        "      \"interpretation\": \"string\",\n" +
        "      \"proposedMode\": \"DEVELOPER|SIMPLE_CHAT|ASSISTED_CODING|HYBRID_MANUAL_EXPORT\",\n" +
        "      \"expectedResult\": \"string\",\n" +
        "      \"requiredCapabilities\": [\"string\"],\n" +
        "      \"advantages\": [\"string\"],\n" +
        "      \"limitations\": [\"string\"],\n" +
        "      \"risks\": [\"string\"],\n" +
        "      \"confidence\": 0.9,\n" +
        "      \"selected\": true,\n" +
        "      \"selectionRationale\": \"string\"\n" +
        "    }\n" +
        "  ],\n" +
        "  \"selectedBranchId\": \"branch-1\",\n" +
        "  \"decisionRationale\": \"string\"\n" +
        "}";

    public ReconstructedIntent reconstructIntent(String prompt, TaskContext context) {
        if (prompt == null) {
            prompt = "";
        }
        String trimmedPrompt = prompt.trim();

        ReconstructedIntent intent = new ReconstructedIntent("intent-" + System.currentTimeMillis(), trimmedPrompt);

        // 1. Check for pure conversational greeting
        if (PromptIntentAnalyzer.isSimpleGreeting(trimmedPrompt)) {
            buildGreetingIntent(intent, trimmedPrompt);
            return intent;
        }

        // 2. Inspect Target Data
        File targetRoot = context != null ? context.getProjectRoot() : null;
        inspectTarget(targetRoot, intent, context, trimmedPrompt);

        // 3. Inspect Conversation Context
        String convContext = inspectConversationContext(context, intent);

        // 4. Inspect Technical Context if programming or code-related
        boolean isCodeRelated = isProgrammingPrompt(trimmedPrompt) || intent.getActionType() == ActionType.PROGRAMMING
                || (!convContext.isEmpty() && isProgrammingPrompt(convContext) && isCodeContinuation(trimmedPrompt));
        String devContext = "";
        if (isCodeRelated && targetRoot != null && targetRoot.exists()) {
            intent.getConfirmedFacts().add("Target build system: " + devContextProvider.detectBuildSystem(targetRoot));
            String jv = devContextProvider.detectJavaVersion(targetRoot);
            if (jv != null) {
                intent.getTechnicalConstraints().add("Target Java Compliance: " + jv);
            }
            try {
                devContext = devContextProvider.buildInitialContext(targetRoot, context, trimmedPrompt);
            } catch (Throwable t) {
                if (context != null) context.log("[INTENT] Target technical context warning: " + t.getMessage());
            }
        }

        // 5. Check for clear Ambiguity upfront (e.g. "Improve this", "Fix it" without details)
        if (isAmbiguousPrompt(trimmedPrompt)) {
            buildAmbiguousIntent(intent, trimmedPrompt, targetRoot);
            return intent;
        }

        // 6. Invoke Local AI for Deep Understanding & Branch Generation
        boolean aiSuccess = false;
        if (context != null && context.getAiService() != null && context.getOrchestrator() != null) {
            try {
                StringBuilder userPrompt = new StringBuilder();
                userPrompt.append("User Request: \"").append(trimmedPrompt).append("\"\n\n");
                if (intent.getTargetPath() != null) {
                    userPrompt.append("Target Path: ").append(intent.getTargetPath()).append("\n");
                    userPrompt.append("Target Category: ").append(intent.getTargetCategory()).append("\n");
                    userPrompt.append("Target Initial Role: ").append(intent.getTargetRole()).append("\n");
                }
                if (!convContext.isEmpty()) {
                    userPrompt.append("\nRecent Conversation Context:\n").append(convContext).append("\n");
                }
                if (!devContext.isEmpty()) {
                    userPrompt.append("\nTarget Technical Context:\n").append(devContext).append("\n");
                }
                userPrompt.append("\nAnalyze the user intent, evaluate the target, and generate 3 to 4 distinct Darwin execution branches.");

                context.log("[INTENT] Sending prompt to local AI for Intent Reconstruction...");
                String response = context.getAiService().sendRequest(
                        context.getOrchestrator(),
                        SYSTEM_PROMPT + "\n\n" + userPrompt.toString(),
                        context
                );

                if (response != null && !response.trim().isEmpty()) {
                    JSONObject json = pipeline.process(response, new java.util.HashMap<>(), context);
                    if (json != null) {
                        applyAiAnalysis(json, intent, trimmedPrompt, targetRoot);
                        aiSuccess = true;
                        context.log("[INTENT] Local AI intent reconstruction successful. Branches generated: "
                                + intent.getCandidateBranches().size());
                    }
                }
            } catch (Exception e) {
                if (context != null) {
                    context.log("[INTENT] Local AI intent reconstruction failed or unavailable: " + e.getMessage()
                            + ". Using deterministic fallback branches.");
                }
            }
        }

        // 7. Fallback: If AI is unavailable, failed, or produced malformed response
        if (!aiSuccess) {
            buildFallbackIntent(intent, trimmedPrompt, targetRoot, isCodeRelated);
        }

        // Ensure 3 to 4 branches always exist
        ensureBranchCount(intent, trimmedPrompt, targetRoot);

        // Resolve winning branch selection
        resolveWinningBranch(intent);

        return intent;
    }

    private void inspectTarget(File targetRoot, ReconstructedIntent intent, TaskContext context, String prompt) {
        if (targetRoot == null || !targetRoot.exists()) {
            intent.setTargetRole(TargetRole.NONE);
            intent.setTargetCategory("NONE");
            return;
        }

        intent.setTargetPath(targetRoot.getAbsolutePath());
        ClassificationResult classification = TargetClassificationDetector.detect(targetRoot);
        intent.setTargetCategory(classification.getCategory().name());
        intent.setTargetDescription("Target: " + targetRoot.getName() + " (" + classification.getCategory() + ")");

        String lowerPrompt = prompt.toLowerCase(Locale.US);

        // Distinguish modification intent vs analysis intent on target
        if (lowerPrompt.contains("explain") || lowerPrompt.contains("what does") || lowerPrompt.contains("understand")
                || lowerPrompt.contains("inspect") || lowerPrompt.contains("analyze") || lowerPrompt.contains("read")) {
            intent.setTargetRole(TargetRole.ANALYSIS_ONLY);
            intent.setTargetRelationship("Target is provided as a read-only source for inspection and analysis.");
        } else if (lowerPrompt.contains("write") || lowerPrompt.contains("create") || lowerPrompt.contains("implement")
                || lowerPrompt.contains("refactor") || lowerPrompt.contains("add") || lowerPrompt.contains("fix")
                || lowerPrompt.contains("modify") || lowerPrompt.contains("update")) {
            intent.setTargetRole(TargetRole.MODIFICATION_TARGET);
            intent.setTargetRelationship("Target represents the workspace or project to be extended or modified.");
        } else {
            intent.setTargetRole(TargetRole.CONTEXTUAL_REFERENCE);
            intent.setTargetRelationship("Target provides background context for the user inquiry.");
        }
    }

    private String inspectConversationContext(TaskContext context, ReconstructedIntent intent) {
        if (context == null || context.getSharedMemory() == null) {
            return "";
        }
        try {
            ConversationState convState = ConversationState.load(context.getSharedMemory(), context.getSessionId());
            if (convState != null && convState.getLastMessages() != null && !convState.getLastMessages().isEmpty()) {
                List<String> lastMessages = convState.getLastMessages();
                int start = Math.max(0, lastMessages.size() - 5);
                StringBuilder sb = new StringBuilder();
                for (int i = start; i < lastMessages.size(); i++) {
                    sb.append("- ").append(lastMessages.get(i)).append("\n");
                }
                String summary = sb.toString();
                intent.setConversationSummary(summary);
                intent.getConfirmedFacts().add("Conversation context present (" + (lastMessages.size() - start) + " recent turns).");
                return summary;
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private boolean isProgrammingPrompt(String prompt) {
        String lower = prompt.toLowerCase(Locale.US);
        return lower.contains("java") || lower.contains("class") || lower.contains("code")
                || lower.contains("implement") || lower.contains("refactor") || lower.contains("rest client")
                || lower.contains("http") || lower.contains("api") || lower.contains("bug") || lower.contains("method")
                || lower.contains("function") || lower.contains("package") || lower.contains("compile")
                || lower.contains("library") || lower.contains("endpoint") || lower.contains("service")
                || lower.contains("logging") || lower.contains("logger");
    }

    private boolean isCodeContinuation(String prompt) {
        String lower = prompt.toLowerCase(Locale.US);
        return lower.startsWith("now ") || lower.contains("add ") || lower.contains("also ")
                || lower.contains("next ") || lower.contains("logging") || lower.contains("tests");
    }

    private boolean isAmbiguousPrompt(String prompt) {
        String lower = prompt.trim().toLowerCase(Locale.US).replaceAll("[^a-z0-9\\s]", "");
        return lower.equals("improve this") || lower.equals("fix this") || lower.equals("make it better")
                || lower.equals("update this") || lower.equals("change this") || lower.equals("do it")
                || lower.equals("fix it") || lower.equals("check this");
    }

    private void buildGreetingIntent(ReconstructedIntent intent, String prompt) {
        intent.setActionType(ActionType.CONVERSATION);
        intent.setPrimaryObjective("Engage in natural, friendly conversation with user");
        intent.setExpectedResult("Conversational response without technical workflow invocation");
        intent.setTargetRole(TargetRole.NONE);
        intent.setRecommendedExecutionMode(PlatformType.SIMPLE_CHAT);
        intent.setConfidence(1.0);
        intent.setDecisionRationale("Pure greeting detected. No technical, code, or workflow branches needed.");

        ExecutionBranch branch = new ExecutionBranch("branch-chat", "Conversational Chat", PlatformType.SIMPLE_CHAT,
                "Respond naturally and politely without invoking developer or evolutionary engines.");
        branch.setExpectedResult("Friendly conversation message.");
        branch.setConfidence(1.0);
        branch.setSelected(true);
        branch.setSelectionRationale("Greetings map directly to conversational mode.");
        intent.getCandidateBranches().add(branch);
        intent.setSelectedBranch(branch);
    }

    private void buildAmbiguousIntent(ReconstructedIntent intent, String prompt, File targetRoot) {
        intent.setActionType(ActionType.CLARIFICATION_NEEDED);
        intent.setPrimaryObjective("Clarify ambiguous user instruction '" + prompt + "'");
        intent.setExpectedResult("User clarification on specific goal and desired scope of changes");
        intent.setClarificationRequired(true);
        intent.setConfidence(0.4);

        String targetInfo = targetRoot != null ? " on target " + targetRoot.getName() : "";
        intent.setClarificationQuestion("Could you please specify what improvements or changes you would like to make" + targetInfo + "? (e.g., performance optimization, code refactoring, bug fixes, or documentation)");
        intent.getAmbiguities().add("Instruction '" + prompt + "' does not define target aspect, metric, or desired behavior.");
        intent.getMissingInformation().add("Specific change goal, target files or features, and acceptance criteria.");

        // 3-4 Branches for Ambiguous request
        ExecutionBranch b1 = new ExecutionBranch("branch-clarify", "Request Clarification", PlatformType.SIMPLE_CHAT,
                "Ask user to specify whether they intend code refactoring, performance optimization, or documentation.");
        b1.getAdvantages().add("Prevents unwanted or incorrect modifications to codebase");
        b1.getLimitations().add("Requires user to reply before work begins");
        b1.setConfidence(0.95);
        b1.setSelected(true);
        b1.setSelectionRationale("Safest and most constructive strategy for highly ambiguous prompts.");

        ExecutionBranch b2 = new ExecutionBranch("branch-code-review", "Code Quality & Diagnostic Review", PlatformType.HYBRID_MANUAL_EXPORT,
                "Perform read-only diagnostic review of target to suggest potential quality improvements.");
        b2.getAdvantages().add("Provides immediate value without modifying any code directly");
        b2.getLimitations().add("May analyze areas the user did not intend to improve");
        b2.setConfidence(0.70);

        ExecutionBranch b3 = new ExecutionBranch("branch-assisted-guided", "Interactive Guided Inspection", PlatformType.ASSISTED_CODING,
                "Guide the user interactively through possible architectural improvements.");
        b3.getAdvantages().add("Interactive alignment with user goals");
        b3.getLimitations().add("Slower than automated changes");
        b3.setConfidence(0.60);

        intent.getCandidateBranches().add(b1);
        intent.getCandidateBranches().add(b2);
        intent.getCandidateBranches().add(b3);
        intent.setSelectedBranch(b1);
        intent.setRecommendedExecutionMode(PlatformType.SIMPLE_CHAT);
        intent.setDecisionRationale("Ambiguous prompt prevents safe automatic modification. Recommending clarification.");
    }

    private void applyAiAnalysis(JSONObject json, ReconstructedIntent intent, String prompt, File targetRoot) {
        String actionTypeStr = json.optString("actionType", ActionType.PROGRAMMING.name());
        try {
            intent.setActionType(ActionType.valueOf(actionTypeStr.trim().toUpperCase()));
        } catch (Exception e) {
            intent.setActionType(ActionType.PROGRAMMING);
        }

        intent.setPrimaryObjective(json.optString("primaryObjective", prompt));
        intent.setExpectedResult(json.optString("expectedResult", "Complete user requested objective"));

        String targetRoleStr = json.optString("targetRole", null);
        if (targetRoleStr != null) {
            try {
                intent.setTargetRole(TargetRole.valueOf(targetRoleStr.trim().toUpperCase()));
            } catch (Exception ignored) {}
        }
        if (json.has("targetDescription")) {
            intent.setTargetDescription(json.optString("targetDescription"));
        }

        addStrings(json.optJSONArray("secondaryObjectives"), intent.getSecondaryObjectives());
        addStrings(json.optJSONArray("technicalConstraints"), intent.getTechnicalConstraints());
        addStrings(json.optJSONArray("confirmedFacts"), intent.getConfirmedFacts());
        addStrings(json.optJSONArray("assumptions"), intent.getAssumptions());
        addStrings(json.optJSONArray("missingInformation"), intent.getMissingInformation());
        addStrings(json.optJSONArray("ambiguities"), intent.getAmbiguities());

        intent.setClarificationRequired(json.optBoolean("clarificationRequired", false));
        intent.setClarificationQuestion(json.optString("clarificationQuestion", null));
        intent.setDecisionRationale(json.optString("decisionRationale", null));

        JSONArray branchesArr = json.optJSONArray("branches");
        if (branchesArr != null && branchesArr.length() > 0) {
            intent.getCandidateBranches().clear();
            for (int i = 0; i < branchesArr.length(); i++) {
                JSONObject bObj = branchesArr.optJSONObject(i);
                if (bObj != null) {
                    ExecutionBranch branch = ExecutionBranch.fromJsonObject(bObj);
                    if (branch != null) {
                        intent.getCandidateBranches().add(branch);
                    }
                }
            }
        }

        String selectedBranchId = json.optString("selectedBranchId", null);
        if (selectedBranchId != null) {
            for (ExecutionBranch b : intent.getCandidateBranches()) {
                if (selectedBranchId.equalsIgnoreCase(b.getId())) {
                    intent.setSelectedBranch(b);
                    break;
                }
            }
        }
    }

    private void buildFallbackIntent(ReconstructedIntent intent, String prompt, File targetRoot, boolean isCodeRelated) {
        String lower = prompt.toLowerCase(Locale.US);

        if (lower.contains("explain") || lower.contains("what does") || lower.contains("learn")
                || lower.contains("neural network") || lower.contains("how does") || lower.contains("tell me")) {
            // Explanatory / Educational / Research
            boolean hasTarget = targetRoot != null && targetRoot.exists();
            intent.setActionType(ActionType.QUESTION);
            intent.setPrimaryObjective("Provide comprehensive explanation and analysis for: " + prompt);
            intent.setExpectedResult("Detailed analytical explanation addressing core concepts");
            intent.setTargetRole(hasTarget ? TargetRole.ANALYSIS_ONLY : TargetRole.NONE);
            intent.setRecommendedExecutionMode(hasTarget ? PlatformType.HYBRID_MANUAL_EXPORT : PlatformType.SIMPLE_CHAT);
            intent.setConfidence(0.85);
            intent.setDecisionRationale("Inquiry requires educational/analytical response. Developer mode is not required.");

            ExecutionBranch b1 = new ExecutionBranch("branch-explain", "Direct Educational Explanation", PlatformType.SIMPLE_CHAT,
                    "Generate comprehensive, structured conceptual explanation directly to user.");
            b1.getAdvantages().add("Immediate, clear answer addressing the question directly");
            b1.getLimitations().add("Does not perform deep file scanning if not relevant");
            b1.setConfidence(0.9);
            b1.setSelected(true);
            b1.setSelectionRationale("Best fit for conceptual/educational inquiries.");

            ExecutionBranch b2 = new ExecutionBranch("branch-analyze-target", "Read-Only Architecture Analysis", PlatformType.HYBRID_MANUAL_EXPORT,
                    "Perform deep read-only inspection of target repository to anchor the explanation with actual code.");
            b2.getAdvantages().add("Grounds explanation in concrete codebase artifacts");
            b2.getLimitations().add("More resource-intensive");
            b2.setConfidence(hasTarget ? 0.8 : 0.4);

            ExecutionBranch b3 = new ExecutionBranch("branch-interactive-tutorial", "Interactive Guided Tutorial", PlatformType.ASSISTED_CODING,
                    "Provide step-by-step interactive tutorial with runnable examples.");
            b3.getAdvantages().add("Interactive learning experience");
            b3.getLimitations().add("Requires multiple interaction steps");
            b3.setConfidence(0.7);

            intent.getCandidateBranches().add(b1);
            intent.getCandidateBranches().add(b2);
            intent.getCandidateBranches().add(b3);
            intent.setSelectedBranch(b1);

        } else if (isCodeRelated) {
            // Programming task
            boolean hasTarget = targetRoot != null && targetRoot.exists();
            intent.setActionType(ActionType.PROGRAMMING);
            intent.setPrimaryObjective("Implement requested programming objective: " + prompt);
            intent.setExpectedResult("Production-quality Java implementation adhering to architecture standards");
            intent.setTargetRole(hasTarget ? TargetRole.MODIFICATION_TARGET : TargetRole.NONE);
            intent.setRecommendedExecutionMode(PlatformType.DEVELOPER);
            intent.setConfidence(0.9);
            intent.setDecisionRationale("Request implies technical programming. Recommending autonomous DEVELOPER execution.");

            // Branch 1: DEVELOPER with target project
            ExecutionBranch b1 = new ExecutionBranch("branch-dev-project", "DEVELOPER (Integrated Project Implementation)", PlatformType.DEVELOPER,
                    "Analyze existing project architecture and implement the required component adhering to project standards.");
            b1.getRequiredCapabilities().add("JavaDevelopmentTools");
            b1.getRequiredCapabilities().add("CompilerVerification");
            b1.getAdvantages().add("Seamlessly integrates with existing build system, dependencies, and packaging");
            b1.getLimitations().add("Requires valid project build configuration");
            b1.getRisks().add("Potential dependency conflicts if project libraries are outdated");
            b1.setConfidence(hasTarget ? 0.95 : 0.6);
            b1.setSelected(hasTarget);
            b1.setSelectionRationale("Leverages target architecture and automated verification.");

            // Branch 2: DEVELOPER standalone component
            ExecutionBranch b2 = new ExecutionBranch("branch-dev-standalone", "DEVELOPER (Clean Standalone Component)", PlatformType.DEVELOPER,
                    "Generate a clean, self-contained implementation with standard JDK utilities.");
            b2.getRequiredCapabilities().add("JavaCodeGeneration");
            b2.getAdvantages().add("Zero dependency footprint, clean and immediately reusable");
            b2.getLimitations().add("Does not integrate third-party framework optimizations");
            b2.getRisks().add("May require manual integration by user if project context was expected");
            b2.setConfidence(hasTarget ? 0.75 : 0.90);
            b2.setSelected(!hasTarget);
            b2.setSelectionRationale("Optimal when no project context exists or standalone utility is needed.");

            // Branch 3: ASSISTED_CODING
            ExecutionBranch b3 = new ExecutionBranch("branch-assisted", "ASSISTED CODING (Interactive Implementation)", PlatformType.ASSISTED_CODING,
                    "Prepare guided implementation with technical explanations and interactive review.");
            b3.getRequiredCapabilities().add("InteractiveReview");
            b3.getAdvantages().add("Full user visibility and control over each design choice");
            b3.getLimitations().add("Requires user review for each change step");
            b3.setConfidence(0.70);

            // Branch 4: CLARIFICATION
            ExecutionBranch b4 = new ExecutionBranch("branch-clarify", "CLARIFICATION (Specification Confirmation)", PlatformType.SIMPLE_CHAT,
                    "Request missing technical specifications before generating code.");
            b4.getAdvantages().add("Guarantees exact matching of user protocol/framework preferences");
            b4.getLimitations().add("Adds an extra interaction turn");
            b4.setConfidence(0.50);

            intent.getCandidateBranches().add(b1);
            intent.getCandidateBranches().add(b2);
            intent.getCandidateBranches().add(b3);
            intent.getCandidateBranches().add(b4);
            intent.setSelectedBranch(hasTarget ? b1 : b2);

        } else {
            // General task / analysis
            intent.setActionType(ActionType.INVESTIGATION);
            intent.setPrimaryObjective("Investigate and execute task: " + prompt);
            intent.setExpectedResult("Clear evaluation and execution outcome");
            intent.setConfidence(0.75);
            intent.setRecommendedExecutionMode(PlatformType.DEVELOPER);

            ExecutionBranch b1 = new ExecutionBranch("branch-autonomous", "Autonomous Execution", PlatformType.DEVELOPER,
                    "Execute task autonomously using available Darwin capabilities.");
            b1.setConfidence(0.8);
            b1.setSelected(true);

            ExecutionBranch b2 = new ExecutionBranch("branch-inspect", "Read-Only Assessment", PlatformType.HYBRID_MANUAL_EXPORT,
                    "Perform non-invasive inspection and present findings to user.");
            b2.setConfidence(0.7);

            ExecutionBranch b3 = new ExecutionBranch("branch-guided", "Assisted Step-by-Step", PlatformType.ASSISTED_CODING,
                    "Step-by-step interactive workflow with user feedback.");
            b3.setConfidence(0.65);

            intent.getCandidateBranches().add(b1);
            intent.getCandidateBranches().add(b2);
            intent.getCandidateBranches().add(b3);
            intent.setSelectedBranch(b1);
        }
    }

    private void ensureBranchCount(ReconstructedIntent intent, String prompt, File targetRoot) {
        List<ExecutionBranch> branches = intent.getCandidateBranches();
        if (branches.size() >= 3 && branches.size() <= 4) {
            return;
        }

        if (branches.size() < 3) {
            // Add sensible alternatives to ensure at least 3 branches exist
            if (branches.stream().noneMatch(b -> b.getProposedMode() == PlatformType.ASSISTED_CODING)) {
                ExecutionBranch b = new ExecutionBranch("branch-assisted-alt", "ASSISTED CODING (Interactive Alignment)",
                        PlatformType.ASSISTED_CODING, "Execute interactively with user oversight and explanations.");
                b.setConfidence(0.7);
                branches.add(b);
            }
            if (branches.size() < 3 && branches.stream().noneMatch(b -> b.getProposedMode() == PlatformType.HYBRID_MANUAL_EXPORT)) {
                ExecutionBranch b = new ExecutionBranch("branch-analysis-alt", "MEDIATED (Read-Only Analysis & Export)",
                        PlatformType.HYBRID_MANUAL_EXPORT, "Inspect target and export artifacts without direct mutation.");
                b.setConfidence(0.65);
                branches.add(b);
            }
            if (branches.size() < 3 && branches.stream().noneMatch(b -> b.getProposedMode() == PlatformType.SIMPLE_CHAT)) {
                ExecutionBranch b = new ExecutionBranch("branch-chat-alt", "CLARIFICATION / DISCUSSION",
                        PlatformType.SIMPLE_CHAT, "Clarify requirements or discuss approach before proceeding.");
                b.setConfidence(0.55);
                branches.add(b);
            }
        }

        // Cap at 4 branches max
        while (branches.size() > 4) {
            // Remove the lowest confidence unselected branch
            ExecutionBranch toRemove = branches.stream()
                    .filter(b -> !b.isSelected())
                    .min((b1, b2) -> Double.compare(b1.getConfidence(), b2.getConfidence()))
                    .orElse(branches.get(branches.size() - 1));
            branches.remove(toRemove);
        }
    }

    private void resolveWinningBranch(ReconstructedIntent intent) {
        if (intent.getSelectedBranch() != null) {
            return;
        }
        List<ExecutionBranch> branches = intent.getCandidateBranches();
        if (branches.isEmpty()) {
            return;
        }
        ExecutionBranch winner = branches.stream()
                .max((b1, b2) -> Double.compare(b1.getConfidence(), b2.getConfidence()))
                .orElse(branches.get(0));
        intent.setSelectedBranch(winner);
        intent.setRecommendedExecutionMode(winner.getProposedMode());
        if (intent.getDecisionRationale() == null || intent.getDecisionRationale().isEmpty()) {
            intent.setDecisionRationale("Selected branch '" + winner.getName() + "' with highest confidence ("
                    + String.format(Locale.US, "%.2f", winner.getConfidence()) + ").");
        }
    }

    private static void addStrings(JSONArray arr, List<String> list) {
        if (arr == null) return;
        for (int i = 0; i < arr.length(); i++) {
            String val = arr.optString(i, null);
            if (val != null && !val.trim().isEmpty()) {
                list.add(val.trim());
            }
        }
    }
}
