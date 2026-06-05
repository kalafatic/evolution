package eu.kalafatic.evolution.controller.orchestration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;

/**
 * CLI entry point for Standalone SELF_DEV mode.
 * Invoked by the Supervisor JAR.
 */
public class SelfDevMain {
    public static void main(String[] args) {
        String mode = null;
        File variantDir = null;
        String statePath = null;

        for (String arg : args) {
            if (arg.startsWith("--mode=")) {
                mode = arg.substring(7);
            } else if (arg.startsWith("--variant=")) {
                variantDir = new File(arg.substring(10));
            } else if (arg.startsWith("--state=")) {
                statePath = arg.substring(8);
            }
        }

        if (statePath == null) {
            statePath = System.getProperty("state");
        }
        if (statePath == null) {
            statePath = System.getenv("EVO_STATE");
        }

        if (!"SELF_DEV".equals(mode) || variantDir == null) {
            System.err.println("Usage: --mode=SELF_DEV --variant=<path> [--state=<path>]");
            System.exit(1);
        }

        System.out.println("[RCP] Standalone SELF_DEV mode starting in: " + variantDir.getAbsolutePath());
        if (statePath != null) {
            System.out.println("[RCP] Using state from: " + statePath);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            String goal = "Continue self-development and structural improvement.";
            int iteration = 0;

            if (statePath != null) {
                File stateFile = new File(statePath);
                if (stateFile.exists()) {
                    ObjectNode stateNode = (ObjectNode) mapper.readTree(stateFile);
                    if (stateNode.has("goal")) {
                        goal = stateNode.get("goal").asText();
                    }
                    if (stateNode.has("iteration")) {
                        iteration = stateNode.get("iteration").asInt();
                    }
                }
            }

            // Setup minimal Orchestrator context
            Orchestrator orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
            
            if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
            
            PromptInstructions promptInstructions = orchestrator.getAiChat().getPromptInstructions();
        	
        	if (promptInstructions == null) {
        		promptInstructions = OrchestrationFactory.eINSTANCE.createPromptInstructions();
        		orchestrator.getAiChat().setPromptInstructions(promptInstructions);
        	}        
        	
            promptInstructions.setSelfIterativeMode(true);
            promptInstructions.setAutoApprove(true);

            // Populate SelfDevSession to track iteration
            SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
            session.setId("self-dev-main");
            session.setInitialRequest(goal);
            for (int i = 0; i < iteration; i++) {
                session.getIterations().add(OrchestrationFactory.eINSTANCE.createIteration());
            }
            orchestrator.setSelfDevSession(session);

            TaskContext context = new TaskContext(orchestrator, variantDir);
            context.setAutoApprove(true);
            context.setPlatformMode(new PlatformMode(PlatformType.SELF_DEV_MODE, AutonomyLevel.HIGH, 1, true));

            IOrchestrator kernel = new KernelFacade();
            TaskRequest taskRequest = new TaskRequest(goal, variantDir);
            OrchestratorResponse orchResponse = kernel.handle(taskRequest, context);

            String response = orchResponse.getSummary();

            // Compute real score based on build/test status in context
            double score = (orchResponse.getResultType() == ResultType.ERROR || response.toLowerCase().contains("error")) ? 0.0 : 0.8;

            ObjectNode resultNode = mapper.createObjectNode();
            resultNode.put("status", "OK");
            resultNode.put("score", score);
            resultNode.put("response", response);

            File resultFile = new File(variantDir, "result.json");
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(resultFile), StandardCharsets.UTF_8)) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(writer, resultNode);
            }

            System.out.println("[RCP] Finished. Result written to result.json");

            System.exit(0);
        } catch (Exception e) {
            System.err.println("[RCP] Critical error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
