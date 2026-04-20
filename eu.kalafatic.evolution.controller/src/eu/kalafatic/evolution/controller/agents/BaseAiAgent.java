package eu.kalafatic.evolution.controller.agents;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.mcp.McpClient;
import eu.kalafatic.evolution.controller.orchestration.util.DataScrubber;
import eu.kalafatic.evolution.controller.services.BestPracticesService;
import eu.kalafatic.evolution.controller.services.NeuronContextService;
import eu.kalafatic.evolution.controller.tools.ITool;

/**
 * Base AI Agent that wraps existing AI model/chat code.
 */
public abstract class BaseAiAgent implements IAgent {
    protected final String id;
    protected final String type;
    protected final List<ITool> tools = new ArrayList<>();
    protected final LlmRouter llmRouter = new LlmRouter();
    
    protected final AiService aiService = new AiService();
    protected BestPracticesService bestPracticesService;
    protected NeuronContextService neuronContextService;

    public BaseAiAgent(String id, String type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public List<ITool> getTools() {
        return tools;
    }

    public void addTool(ITool tool) {
        tools.add(tool);
    }

    /**
     * Specific instructions for the agent type.
     * @return Agent-specific instructions.
     */
    protected abstract String getAgentInstructions();

    /**
     * Optional footer instructions for the agent (e.g. JSON format).
     * @return Footer instructions.
     */
    protected String getFooterInstructions() {
        return "SPECIAL DIRECTIVE: If you receive the request 'Execute the simplest working solution.', you MUST immediately provide a minimal, functional implementation of the goal previously discussed in shared memory without asking for any further clarification.\n\n" +
               "Based on the context and the task, provide your response.\n" +
               "If you need user clarification to proceed with a plan or an action, explicitly ask and suggest options.\n" +
               "Use keywords like 'CREATE' (to suggest proceeding) or 'CLARIFY' (to request more info).\n" +
               "Additionally, you can offer general one-click solutions using the format: [PROPOSAL: Action Label | Explicit Request Text]\n" +
               "Example: 'I can help you with that. [PROPOSAL: Create a test class | Create a JUnit 5 test class for the current Main.java file]'.\n\n" +
               "You can also link to specific files in your response using the format: [FILE:path/to/file]\n" +
               "Example: 'I have updated the logic in [FILE:src/Main.java].'";
    }

    protected String buildPrompt(String taskDescription, TaskContext context, String lastFeedback) {
        Orchestrator orchestrator = context.getOrchestrator();

        // Initialize services if needed
        if (bestPracticesService == null) {
            bestPracticesService = new BestPracticesService(orchestrator, context.getProjectRoot());
        }
        if (neuronContextService == null) {
            neuronContextService = new NeuronContextService(orchestrator, context.getProjectRoot());
        }

        // 1. Fetch MCP context if enabled
        String mcpContext = "";
        String mcpUrl = orchestrator.getMcpServerUrl();
        if (mcpUrl != null && !mcpUrl.isEmpty()) {
            try {
                McpClient mcpClient = new McpClient(mcpUrl);
                mcpClient.initialize();
                mcpContext = "\nMCP Local Context: " + mcpClient.listResources();
            } catch (Exception e) {
                // Log but continue
            }
        }

        String projectRootPath = context.getProjectRoot() != null ? context.getProjectRoot().getAbsolutePath() : "Unknown";

        StringBuilder sb = new StringBuilder();
        sb.append("You are acting as a ").append(type).append(" Agent.\n");
        sb.append("PROJECT ROOT: ").append(projectRootPath).append("\n");

        String memory = context.getSharedMemory();
        if (memory != null && !memory.isEmpty()) {
            sb.append("\n--- SHARED MEMORY (HISTORY) ---\n");
            sb.append(memory).append("\n");
            sb.append("--- END SHARED MEMORY ---\n");
        }

        if (!mcpContext.isEmpty()) {
            sb.append(mcpContext).append("\n");
        }

        sb.append("\nINSTRUCTIONS:\n").append(getAgentInstructions()).append("\n");

        // Best Practices injection
        String bp = bestPracticesService.getCombinedPractices();
        if (bp != null && !bp.isEmpty()) {
            sb.append("\n").append(bp).append("\n");
        }

        // Special Context injection
        if (orchestrator.getAiChat().getPromptInstructions().isIterativeMode()) {
            String ic = bestPracticesService.getSpecialContext("iterative_loop.md");
            if (ic != null && !ic.isEmpty()) {
                sb.append("\n--- ITERATIVE LOOP CONTEXT ---\n").append(ic).append("\n--- END ITERATIVE LOOP CONTEXT ---\n");
            }
        }
        if (orchestrator.getAiChat().getPromptInstructions().isSelfIterativeMode()) {
            String sc = bestPracticesService.getSpecialContext("self_development.md");
            if (sc != null && !sc.isEmpty()) {
                sb.append("\n--- SELF DEVELOPMENT CONTEXT ---\n").append(sc).append("\n--- END SELF DEVELOPMENT CONTEXT ---\n");
            }
        }

        // Neuron Context injection
        String nc = neuronContextService.getLearnedContext();
        if (nc != null && !nc.isEmpty()) {
            sb.append("\n").append(nc).append("\n");
        }

        // External Instruction Files
        List<String> extFiles = context.getInstructionFiles();
        if (extFiles != null && !extFiles.isEmpty()) {
            sb.append("\n--- EXTERNAL INSTRUCTIONS ---\n");
            for (String filePath : extFiles) {
                try {
                    String content = Files.readString(Paths.get(filePath));
                    sb.append("File: ").append(new File(filePath).getName()).append("\n");
                    sb.append(content).append("\n\n");
                } catch (Exception e) {
                    sb.append("Error reading instruction file: ").append(filePath).append(" (").append(e.getMessage()).append(")\n");
                }
            }
            sb.append("--- END EXTERNAL INSTRUCTIONS ---\n");
        }

        if (lastFeedback != null && !lastFeedback.isEmpty()) {
            sb.append("\n--- PREVIOUS ATTEMPT FAILED ---\n");
            sb.append("Feedback: ").append(lastFeedback).append("\n");
            sb.append("Please correct your approach based on this feedback.\n");
        }

        sb.append("\nCURRENT TASK:\n").append(taskDescription).append("\n");

        sb.append("\nFINAL DIRECTIVE:\n").append(getFooterInstructions());

        return sb.toString();
    }

    @Override
    public String process(String taskDescription, TaskContext context, String lastFeedback) throws Exception {
        Orchestrator orchestrator = context.getOrchestrator();
        String prompt = buildPrompt(taskDescription, context, lastFeedback);

        // Data Scrubbing if online
        if (!orchestrator.isOfflineMode()) {
            prompt = DataScrubber.scrub(prompt);
        }

        context.log("Evo-" + type + "-" + context.getCurrentTaskName() + ": Processing task");
        context.log("Evo-" + type + "-Thinking: " + prompt);

        // Routing via LlmRouter
        float temperature = 0.7f;
        if (orchestrator.getLlm() != null) {
            temperature = (float) orchestrator.getLlm().getTemperature();
        }

        String proxyUrl = (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null;
        String response = llmRouter.sendRequest(orchestrator, prompt, temperature, proxyUrl, context);
        context.log("Evo-" + type + "-Response: " + response);

        return cleanResponse(response);
    }

    protected String cleanResponse(String response) {
        String trimmed = response.trim();
        int firstBackticks = trimmed.indexOf("```");
        if (firstBackticks != -1) {
            int firstNewline = trimmed.indexOf("\n", firstBackticks);
            int lastBackticks = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastBackticks > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastBackticks).trim();
            }
        }
        return trimmed;
    }
}
