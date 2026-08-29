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
            if (trimmed.startsWith("llama_perf_") || trimmed.startsWith("llama_print_timings") || trimmed.startsWith("load_tensors")
                    || trimmed.startsWith("system_info:") || trimmed.startsWith("main:")) {
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
     * Resolves the primary controller models folder (eu.kalafatic.evolution.controller/lib/models) across codebase/workspace/user.dir locations.
     * @return File representing the models directory.
     */
    public static File resolveControllerModelsDir() {
        String codebasePath = ProjectModelManager.getCodebasePath();
        String userDir = System.getProperty("user.dir");
        List<String> candidatePaths = new ArrayList<>();
        if (codebasePath != null && !codebasePath.isEmpty()) {
            candidatePaths.add(codebasePath + "/eu.kalafatic.evolution.controller/lib/models");
            candidatePaths.add(codebasePath + "/lib/models");
        }
        if (userDir != null && !userDir.isEmpty()) {
            candidatePaths.add(userDir + "/eu.kalafatic.evolution.controller/lib/models");
            candidatePaths.add(userDir + "/../eu.kalafatic.evolution.controller/lib/models");
            candidatePaths.add(userDir + "/lib/models");
            candidatePaths.add(userDir + "/eu.kalafatic.evolution.forge.agent.api/lib/models");
        }

        for (String pathStr : candidatePaths) {
            File dir = new File(pathStr);
            if (dir.exists() && dir.isDirectory()) {
                return dir;
            }
        }

        String primaryPath = !candidatePaths.isEmpty() ? candidatePaths.get(0) : userDir + "/lib/models";
        File dir = new File(primaryPath);
        dir.mkdirs();
        return dir;
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
    /**
     * Copies and overwrites the specified source GGUF file to the controller models folder as evo.gguf.
     * @param sourceGguf Path to the source GGUF file.
     * @return true if copy succeeded.
     */
    public static boolean copyToModelsDir(java.nio.file.Path sourceGguf) {
        return copyToModelsDir(sourceGguf, null);
    }

    /**
     * Copies and overwrites the specified source GGUF file to the controller models folder as evo.gguf and modelName.gguf.
     * @param sourceGguf Path to the source GGUF file.
     * @param modelName Optional target model name (e.g. "evo-12345").
     * @return true if copy succeeded.
     */
    public static boolean copyToModelsDir(java.nio.file.Path sourceGguf, String modelName) {
        if (sourceGguf == null || !java.nio.file.Files.exists(sourceGguf)) {
            return false;
        }
        try {
            File modelsDir = resolveControllerModelsDir();
            if (!modelsDir.exists()) {
                modelsDir.mkdirs();
            }
            java.nio.file.Path targetEvoGguf = modelsDir.toPath().resolve("evo.gguf");
            java.nio.file.Files.copy(sourceGguf, targetEvoGguf, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[LlamaService] Copied/overwrote latest forged model to models folder: " + targetEvoGguf.toAbsolutePath());

            if (modelName != null && !modelName.isEmpty() && !"evo".equalsIgnoreCase(modelName)) {
                java.nio.file.Path targetNamedGguf = modelsDir.toPath().resolve(modelName + ".gguf");
                java.nio.file.Files.copy(sourceGguf, targetNamedGguf, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[LlamaService] Copied/overwrote model as " + modelName + ".gguf in models folder.");
            }

            // Copy to user home ~/.ollama/models directory
            try {
                java.nio.file.Path ollamaHomeModels = java.nio.file.Paths.get(System.getProperty("user.home")).resolve(".ollama/models");
                java.nio.file.Files.createDirectories(ollamaHomeModels);
                java.nio.file.Files.copy(sourceGguf, ollamaHomeModels.resolve("evo.gguf"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                if (modelName != null && !modelName.isEmpty() && !"evo".equalsIgnoreCase(modelName)) {
                    java.nio.file.Files.copy(sourceGguf, ollamaHomeModels.resolve(modelName + ".gguf"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                System.out.println("[LlamaService] Copied GGUF to default Ollama models folder: " + ollamaHomeModels.toAbsolutePath());
            } catch (Exception ex) {
                System.err.println("[LlamaService] Warning: Failed to copy GGUF to Ollama models folder: " + ex.getMessage());
            }

            List<java.nio.file.Path> candidateDirs = new ArrayList<>();
            java.nio.file.Path p = sourceGguf.getParent();
            while (p != null && candidateDirs.size() < 4) {
                candidateDirs.add(p);
                p = p.getParent();
            }

            String[] companionFiles = { "Modelfile", "weights.bin", "config.json", "tokenizer.json", "model.json" };
            for (java.nio.file.Path candidateDir : candidateDirs) {
                if (java.nio.file.Files.exists(candidateDir) && java.nio.file.Files.isDirectory(candidateDir)) {
                    for (String compName : companionFiles) {
                        java.nio.file.Path compFile = candidateDir.resolve(compName);
                        if (java.nio.file.Files.exists(compFile) && !java.nio.file.Files.isDirectory(compFile)) {
                            java.nio.file.Files.copy(compFile, modelsDir.toPath().resolve(compName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(candidateDir)) {
                        stream.filter(f -> f.getFileName().toString().endsWith(".evo"))
                              .forEach(evoFile -> {
                                  try {
                                      java.nio.file.Files.copy(evoFile, modelsDir.toPath().resolve("evo.evo"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                      if (modelName != null && !modelName.isEmpty() && !"evo".equalsIgnoreCase(modelName)) {
                                          java.nio.file.Files.copy(evoFile, modelsDir.toPath().resolve(modelName + ".evo"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                      } else {
                                          java.nio.file.Files.copy(evoFile, modelsDir.toPath().resolve(evoFile.getFileName()), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                      }
                                  } catch (Exception ignored) {}
                              });
                    } catch (Exception ignored) {}
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("[LlamaService] Failed to copy model to models folder: " + e.getMessage());
            return false;
        }
    }

    public static boolean copyToLlamaCppLibDir(java.nio.file.Path sourceGguf, String modelName) {
        if (sourceGguf == null || !java.nio.file.Files.exists(sourceGguf)) {
            return false;
        }
        boolean modelsCopy = copyToModelsDir(sourceGguf, modelName);
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
            return modelsCopy;
        }
    }

    /**
     * Determines default inference engine for a model name according to rules:
     * 1. .gguf model (except evo/evo.gguf) -> "ollama"
     * 2. evo or evo.gguf -> "llama-cpp"
     * 3. other evo (with timestamps/candidates) based llm -> "evo native"
     * 4. standard llm -> "ollama"
     *
     * @param modelName Model name to check.
     * @return "ollama", "llama-cpp", or "evo native"
     */
    public static String detectInferenceEngine(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return "ollama";
        }
        String lower = modelName.toLowerCase().trim();

        // Rule 2: evo or evo.gguf
        if (lower.equals("evo") || lower.equals("evo.gguf")) {
            return "llama-cpp";
        }

        // Rule 1: any gguf model except evo / evo.gguf
        if (lower.endsWith(".gguf")) {
            return "ollama";
        }

        // Rule 3: other evo based models (e.g. evo-<target>-<arch>-<timestamp>)
        if (lower.contains("evo") || lower.contains("forging")) {
            return "evo native";
        }

        // Rule 4: standard models
        return "ollama";
    }

    public static File resolveEvoModelPath(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return null;
        }

        boolean isEvoVariant = modelName.equalsIgnoreCase("evo") || modelName.toLowerCase().contains("evo");

        // 1. DEFAULT: Check inside internal controller models and llama-cpp lib folders first
        File modelsDir = resolveControllerModelsDir();
        if (modelsDir != null && modelsDir.exists() && modelsDir.isDirectory()) {
            File f = new File(modelsDir, modelName + ".gguf");
            if (f.exists()) return f;
            if (isEvoVariant) {
                f = new File(modelsDir, "evo.gguf");
                if (f.exists()) return f;
            }
        }

        File llamaCppDir = resolveLlamaCppLibDir();
        if (llamaCppDir != null && llamaCppDir.exists() && llamaCppDir.isDirectory()) {
            File f = new File(llamaCppDir, modelName + ".gguf");
            if (f.exists()) return f;
            if (isEvoVariant) {
                f = new File(llamaCppDir, "evo.gguf");
                if (f.exists()) return f;
            }
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
