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
                    .contextLength(2048)
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

        if (runner == null || !runner.isAvailable()) {
            throw new IllegalStateException("LlamaService: runner is not initialized or llama-cli is unavailable. Model path: " + modelPath);
        }

        // Run inference via llama.cpp
        String answer = runner.generate(userInput);
        answer = cleanLlamaOutput(answer);

        if (answer != null && answer.contains("token_")) {
            throw new IOException("LlamaService: llama.cpp output contains placeholder tokens ('token_XXXX')");
        }

        history.add(new Message("assistant", answer));
        return answer;
    }

    private String cleanLlamaOutput(String rawOutput) {
        if (rawOutput == null || rawOutput.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : rawOutput.split("\r?\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("llama_perf_") || trimmed.startsWith("llama_print_timings") || trimmed.startsWith("load_tensors")) {
                continue;
            }
            sb.append(line).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Standard generate implementation to match OllamaService.
     */
    public String generate(String prompt) throws Exception {
        if (runner == null || !runner.isAvailable()) {
            throw new IllegalStateException("LlamaService: runner is not initialized or llama-cli is unavailable. Model path: " + modelPath);
        }
        String answer = runner.generate(prompt);
        answer = cleanLlamaOutput(answer);
        if (answer != null && answer.contains("token_")) {
            throw new IOException("LlamaService: llama.cpp output contains placeholder tokens ('token_XXXX')");
        }
        return answer;
    }

    public List<Message> getMessages(String sessionId) {
        return sessionMessages.getOrDefault(sessionId, Collections.emptyList());
    }

    /**
     * Resolves the primary llama-cpp lib folder across codebase/workspace/user.dir locations.
     * @return File representing the llama-cpp directory.
     */
    public static File resolveLlamaCppLibDir() {
        String codebasePath = ProjectModelManager.getCodebasePath();
        String userDir = System.getProperty("user.dir");
        List<String> candidatePaths = new ArrayList<>();
        if (codebasePath != null && !codebasePath.isEmpty()) {
            candidatePaths.add(codebasePath + "/eu.kalafatic.evolution.controller/lib/llama-cpp");
            candidatePaths.add(codebasePath + "/lib/llama-cpp");
        }
        if (userDir != null && !userDir.isEmpty()) {
            candidatePaths.add(userDir + "/eu.kalafatic.evolution.controller/lib/llama-cpp");
            candidatePaths.add(userDir + "/../eu.kalafatic.evolution.controller/lib/llama-cpp");
            candidatePaths.add(userDir + "/lib/llama-cpp");
            candidatePaths.add(userDir + "/eu.kalafatic.evolution.forge.agent.api/lib/llama-cpp");
        }

        for (String pathStr : candidatePaths) {
            File dir = new File(pathStr);
            if (dir.exists() && dir.isDirectory()) {
                return dir;
            }
        }

        String primaryPath = !candidatePaths.isEmpty() ? candidatePaths.get(0) : userDir + "/lib/llama-cpp";
        File dir = new File(primaryPath);
        dir.mkdirs();
        return dir;
    }

    /**
     * Copies and overwrites the specified source GGUF file to the internal llama-cpp lib folder as evo.gguf.
     * @param sourceGguf Path to the source GGUF file.
     * @return true if copy succeeded.
     */
    public static boolean copyToLlamaCppLibDir(java.nio.file.Path sourceGguf) {
        return copyToLlamaCppLibDir(sourceGguf, null);
    }

    /**
     * Copies and overwrites the specified source GGUF file to the internal llama-cpp lib folder as evo.gguf and modelName.gguf.
     * @param sourceGguf Path to the source GGUF file.
     * @param modelName Optional target model name (e.g. "evo-12345").
     * @return true if copy succeeded.
     */
    public static boolean copyToLlamaCppLibDir(java.nio.file.Path sourceGguf, String modelName) {
        if (sourceGguf == null || !java.nio.file.Files.exists(sourceGguf)) {
            return false;
        }
        try {
            File llamaCppDir = resolveLlamaCppLibDir();
            if (!llamaCppDir.exists()) {
                llamaCppDir.mkdirs();
            }
            java.nio.file.Path targetEvoGguf = llamaCppDir.toPath().resolve("evo.gguf");
            java.nio.file.Files.copy(sourceGguf, targetEvoGguf, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[LlamaService] Copied/overwrote latest forged model to llama-cpp lib folder: " + targetEvoGguf.toAbsolutePath());

            if (modelName != null && !modelName.isEmpty() && !"evo".equalsIgnoreCase(modelName)) {
                java.nio.file.Path targetNamedGguf = llamaCppDir.toPath().resolve(modelName + ".gguf");
                java.nio.file.Files.copy(sourceGguf, targetNamedGguf, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[LlamaService] Copied/overwrote model as " + modelName + ".gguf in llama-cpp lib folder.");
            }
            return true;
        } catch (Exception e) {
            System.err.println("[LlamaService] Failed to copy GGUF to llama-cpp lib folder: " + e.getMessage());
            return false;
        }
    }

    /**
     * Resolves the physical GGUF file path for the given model name using various local paths.
     * Default priority is given to the internal llama-cpp lib directory.
     * @param modelName The target model name (e.g., "evo" or "evo-llm-001")
     * @return File object pointing to the GGUF file, or null if not found.
     */
    public static File resolveEvoModelPath(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return null;
        }

        // 1. DEFAULT: Check inside internal llama-cpp lib folder first
        File llamaCppDir = resolveLlamaCppLibDir();
        if (llamaCppDir != null && llamaCppDir.exists() && llamaCppDir.isDirectory()) {
            File f = new File(llamaCppDir, modelName + ".gguf");
            if (f.exists()) return f;
            f = new File(llamaCppDir, "evo.gguf");
            if (f.exists()) return f;
        }

        // 2. Check user home ~/.ollama/models/<modelName>.gguf
        File file = new File(System.getProperty("user.home"), ".ollama/models/" + modelName + ".gguf");
        if (file.exists()) return file;

        // 3. Check user home ~/.ollama/models/evo.gguf
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

        // 6. Fallback to current working directory dist / source/models / forge-output
        File userDir = new File(System.getProperty("user.dir"));
        File[] candidateDirs = {
            new File(userDir, "source/models"),
            new File(userDir, "dist"),
            new File(userDir, "forge-output")
        };
        for (File dir : candidateDirs) {
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles((d, name) -> name.endsWith(".gguf"));
                if (files != null && files.length > 0) {
                    return files[0];
                }
                File[] subdirs = dir.listFiles(File::isDirectory);
                if (subdirs != null) {
                    for (File subdir : subdirs) {
                        File f = new File(subdir, "evo.gguf");
                        if (f.exists()) return f;
                        File[] ggufs = subdir.listFiles((d, name) -> name.endsWith(".gguf"));
                        if (ggufs != null && ggufs.length > 0) return ggufs[0];
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
