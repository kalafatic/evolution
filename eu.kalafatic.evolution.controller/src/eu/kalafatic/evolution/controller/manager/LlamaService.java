package eu.kalafatic.evolution.controller.manager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import eu.kalafatic.evolution.forge.agent.export.LlamaCppRunner;

/**
 * LlamaService - Java wrapper / proxy running inference via llama.cpp for small
 * evo models to work around Ollama issues.
 */
public class LlamaService {

    private final String model;
    private final String modelPath;
    private final Map<String, List<Message>> sessionMessages = new ConcurrentHashMap<>();

    private float temperature = 0.2f;
    private LlamaCppRunner runner;

    public LlamaService(String model, String modelPath) {
        this.model = model;
        this.modelPath = modelPath;
        initRunner();
    }

    private void initRunner() {
        if (modelPath != null && !modelPath.isEmpty() && new File(modelPath).exists()) {
            this.runner = LlamaCppRunner.builder(modelPath)
                    .temperature(temperature)
                    .build();
        }
    }

    public String getModel() {
        return model;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
        initRunner();
    }

    /**
     * Runs chat-based inference on userInput.
     */
    public String chat(String userInput) throws Exception {
        return chat(userInput, "Default");
    }

    /**
     * Runs chat-based inference on userInput for a specific session.
     */
    public String chat(String userInput, String sessionId) throws Exception {
        List<Message> history = sessionMessages.computeIfAbsent(sessionId, k -> {
            List<Message> list = new ArrayList<>();
            list.add(new Message("system", "You are a concise, helpful Java programming assistant."));
            return list;
        });

        history.add(new Message("user", userInput));

        if (runner == null) {
            throw new IllegalStateException("LlamaService: runner is not initialized. Model path: " + modelPath);
        }

        // Run inference via llama.cpp
        String answer = runner.generate(userInput);

        history.add(new Message("assistant", answer));
        return answer;
    }

    /**
     * Standard generate implementation to match OllamaService.
     */
    public String generate(String prompt) throws Exception {
        if (runner == null) {
            throw new IllegalStateException("LlamaService: runner is not initialized. Model path: " + modelPath);
        }
        return runner.generate(prompt);
    }

    public List<Message> getMessages(String sessionId) {
        return sessionMessages.getOrDefault(sessionId, Collections.emptyList());
    }

    /**
     * Resolves the physical GGUF file path for the given model name using various local paths.
     * @param modelName The target model name (e.g., "evo" or "evo-llm-001")
     * @return File object pointing to the GGUF file, or null if not found.
     */
    public static File resolveEvoModelPath(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return null;
        }

        // 1. Check user home ~/.ollama/models/<modelName>.gguf
        File file = new File(System.getProperty("user.home"), ".ollama/models/" + modelName + ".gguf");
        if (file.exists()) return file;

        // 2. Check user home ~/.ollama/models/evo.gguf
        file = new File(System.getProperty("user.home"), ".ollama/models/evo.gguf");
        if (file.exists()) return file;

        // Get workspace or codebase paths
        String codebasePath = ProjectModelManager.getCodebasePath();
        String workspacePath = ProjectModelManager.getWorkspacePath();

        // 3. Fallback to codebase source/models folder
        if (codebasePath != null) {
            File sourceModelsDir = new File(codebasePath, "source/models");
            if (sourceModelsDir.exists() && sourceModelsDir.isDirectory()) {
                File f = new File(sourceModelsDir, modelName + ".gguf");
                if (f.exists()) return f;
                f = new File(sourceModelsDir, "evo.gguf");
                if (f.exists()) return f;
            }
        }

        // 4. Fallback to codebase dist folder
        if (codebasePath != null) {
            File distDir = new File(codebasePath, "dist");
            if (distDir.exists() && distDir.isDirectory()) {
                File[] subdirs = distDir.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        if (subdir.getName().equalsIgnoreCase(modelName) || subdir.getName().startsWith("evo-")) {
                            File f = new File(subdir, "evo.gguf");
                            if (f.exists()) return f;
                        }
                    }
                }
            }
        }

        // 5. Fallback to forge-output workspace folder
        if (workspacePath != null && !workspacePath.isEmpty()) {
            File forgeOutputDir = new File(workspacePath, "forge-output");
            if (forgeOutputDir.exists() && forgeOutputDir.isDirectory()) {
                File[] subdirs = forgeOutputDir.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        if (subdir.getName().equalsIgnoreCase(modelName) || subdir.getName().startsWith("evo-")) {
                            File f = new File(subdir, "evo.gguf");
                            if (f.exists()) return f;
                        }
                    }
                }
            }
        }

        return null;
    }

    // Inner class for messages to match OllamaService
    public static class Message {
        private final String role;
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public String getContent() { return content; }
    }
}
