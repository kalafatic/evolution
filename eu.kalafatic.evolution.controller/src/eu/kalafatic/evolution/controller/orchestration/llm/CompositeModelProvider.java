package eu.kalafatic.evolution.controller.orchestration.llm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Composite Model Provider that orchestrates multi-model inference pipelines.
 * Deliberately coordinates multiple specialized models (e.g. Java coding specialist,
 * reasoning specialist, English explanation specialist) for a single request,
 * sharing context, accumulating intermediate responses, and composing a unified answer.
 */
public class CompositeModelProvider implements ILlmProvider {

    public enum ModelRole {
        CODE_SPECIALIST("Specialist in analyzing and generating programming code"),
        REASONING_SPECIALIST("Specialist in logical deductions, planning, and architectural implications"),
        COMMUNICATION_SPECIALIST("Specialist in clear human-readable natural language presentation");

        private final String description;

        ModelRole(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class DelegatedTask {
        private final ModelRole role;
        private final String modelName;
        private final ILlmProvider provider;
        private final String promptInstruction;

        public DelegatedTask(ModelRole role, String modelName, ILlmProvider provider, String promptInstruction) {
            this.role = role;
            this.modelName = modelName;
            this.provider = provider;
            this.promptInstruction = promptInstruction;
        }

        public ModelRole getRole() { return role; }
        public String getModelName() { return modelName; }
        public ILlmProvider getProvider() { return provider; }
        public String getPromptInstruction() { return promptInstruction; }
    }

    private final List<DelegatedTask> pipeline = new ArrayList<>();
    private final ILlmProvider nativeEvoProvider;
    private final ILlmProvider ollamaProvider;

    public CompositeModelProvider() {
        this(new NativeEvoProvider(), new OllamaProvider());
    }

    public CompositeModelProvider(ILlmProvider nativeEvo, ILlmProvider ollama) {
        this.nativeEvoProvider = nativeEvo != null ? nativeEvo : new NativeEvoProvider();
        this.ollamaProvider = ollama != null ? ollama : new OllamaProvider();
    }

    public void addDelegatedTask(DelegatedTask task) {
        if (task != null) {
            this.pipeline.add(task);
        }
    }

    public List<DelegatedTask> getPipeline() {
        return pipeline;
    }

    @Override
    public String sendRequest(Orchestrator orchestrator, String prompt, float temperature, String proxyUrl, TaskContext context) throws Exception {
        if (pipeline.isEmpty()) {
            buildDefaultCompositionPipeline(orchestrator, prompt, context);
        }

        if (context != null) {
            context.log(String.format("[COMPOSITE] Starting multi-model composition pipeline with %d stages.", pipeline.size()));
        }

        Map<ModelRole, String> intermediateResults = new LinkedHashMap<>();
        String currentContextAcc = prompt;

        for (int i = 0; i < pipeline.size(); i++) {
            DelegatedTask task = pipeline.get(i);
            if (context != null) {
                context.log(String.format("[COMPOSITE][STAGE %d/%d] Role: %s | Model: %s",
                        i + 1, pipeline.size(), task.getRole(), task.getModelName()));
            }

            String stagePrompt = task.getPromptInstruction() + "\n\nContext & Input:\n" + currentContextAcc;
            String stageOutput;

            // Route through delegated provider
            if (task.getProvider() != null) {
                stageOutput = task.getProvider().sendRequest(orchestrator, stagePrompt, temperature, proxyUrl, context);
            } else if (LlmRouter.isNativeEvoModel(task.getModelName())) {
                stageOutput = nativeEvoProvider.sendRequest(orchestrator, stagePrompt, temperature, proxyUrl, context);
            } else {
                stageOutput = ollamaProvider.sendRequest(orchestrator, stagePrompt, temperature, proxyUrl, context);
            }

            if (stageOutput == null) stageOutput = "";
            intermediateResults.put(task.getRole(), stageOutput);

            // Accumulate intermediate output into sequential context
            currentContextAcc += "\n\n[" + task.getRole().name() + " RESULT]:\n" + stageOutput;
        }

        // Return final composed response from communication specialist or last executed stage
        String finalOutput = intermediateResults.get(ModelRole.COMMUNICATION_SPECIALIST);
        if (finalOutput == null || finalOutput.trim().isEmpty()) {
            List<String> results = new ArrayList<>(intermediateResults.values());
            finalOutput = results.isEmpty() ? "" : results.get(results.size() - 1);
        }

        if (context != null) {
            context.log(String.format("[COMPOSITE] Composition pipeline complete. Stages executed: %d. Final output length: %d",
                    intermediateResults.size(), finalOutput.length()));
        }

        return finalOutput;
    }

    private void buildDefaultCompositionPipeline(Orchestrator orchestrator, String prompt, TaskContext context) {
        boolean requiresCoding = prompt != null && (prompt.toLowerCase().contains("java")
                || prompt.toLowerCase().contains("code") || prompt.toLowerCase().contains("function")
                || prompt.toLowerCase().contains("class") || prompt.toLowerCase().contains("implementation"));

        if (requiresCoding) {
            // Stage 1: Java/Code Specialist
            pipeline.add(new DelegatedTask(
                    ModelRole.CODE_SPECIALIST,
                    "evo-coding.evo",
                    nativeEvoProvider,
                    "Analyze the technical implementation and code structure thoroughly."
            ));
            // Stage 2: Reasoning Specialist
            pipeline.add(new DelegatedTask(
                    ModelRole.REASONING_SPECIALIST,
                    "evo-reasoning.evo",
                    nativeEvoProvider,
                    "Examine the architectural problems, patterns, and trade-offs."
            ));
            // Stage 3: Communication Specialist
            pipeline.add(new DelegatedTask(
                    ModelRole.COMMUNICATION_SPECIALIST,
                    "evo-english.evo",
                    nativeEvoProvider,
                    "Synthesize the findings and explain the complete solution clearly in English."
            ));
        } else {
            // Standard single-stage execution
            pipeline.add(new DelegatedTask(
                    ModelRole.COMMUNICATION_SPECIALIST,
                    "evo.evo",
                    nativeEvoProvider,
                    "Provide a comprehensive response to the user's request."
            ));
        }
    }
}
