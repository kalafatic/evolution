package eu.kalafatic.evolution.forge.agent.export;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * LlamaCppRunner - Java wrapper for llama.cpp to run EVO GGUF models
 * 
 * This class provides a Java API for loading and running GGUF models
 * using the llama.cpp inference engine.
 */
public class LlamaCppRunner {
    
    // Configuration
    private static final String LLAMA_CPP_DIR = System.getProperty("user.home") + "/llama.cpp";
    private static final String LLAMA_CLI = LLAMA_CPP_DIR + "/build/bin/llama-cli";
    private static final String LLAMA_SERVER = LLAMA_CPP_DIR + "/build/bin/llama-server";
    
    // Platform detection
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    private static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("nix") || 
                                              System.getProperty("os.name").toLowerCase().contains("nux");
    
    // Model info
    private final String modelPath;
    private final int contextLength;
    private final int batchSize;
    private final int threads;
    private final float temperature;
    private final int topK;
    private final float topP;
    private final float repeatPenalty;
    
    private Process serverProcess = null;
    private int serverPort = 8080;
    private boolean isServerRunning = false;
    
    /**
     * Builder for LlamaCppRunner
     */
    public static class Builder {
        private String modelPath;
        private int contextLength = 128;
        private int batchSize = 128;
        private int threads = Runtime.getRuntime().availableProcessors();
        private float temperature = 0.2f;
        private int topK = 40;
        private float topP = 0.95f;
        private float repeatPenalty = 1.1f;
        private int serverPort = 8080;
        
        public Builder(String modelPath) {
            this.modelPath = modelPath;
        }
        
        public Builder contextLength(int contextLength) {
            this.contextLength = contextLength;
            return this;
        }
        
        public Builder batchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }
        
        public Builder threads(int threads) {
            this.threads = threads;
            return this;
        }
        
        public Builder temperature(float temperature) {
            this.temperature = temperature;
            return this;
        }
        
        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }
        
        public Builder topP(float topP) {
            this.topP = topP;
            return this;
        }
        
        public Builder repeatPenalty(float repeatPenalty) {
            this.repeatPenalty = repeatPenalty;
            return this;
        }
        
        public Builder serverPort(int serverPort) {
            this.serverPort = serverPort;
            return this;
        }
        
        public LlamaCppRunner build() {
            return new LlamaCppRunner(this);
        }
    }
    
    private LlamaCppRunner(Builder builder) {
        this.modelPath = builder.modelPath;
        this.contextLength = builder.contextLength;
        this.batchSize = builder.batchSize;
        this.threads = builder.threads;
        this.temperature = builder.temperature;
        this.topK = builder.topK;
        this.topP = builder.topP;
        this.repeatPenalty = builder.repeatPenalty;
        this.serverPort = builder.serverPort;
        
        // Ensure llama.cpp is available
        ensureLlamaCppAvailable();
    }
    
    /**
     * Creates a builder for LlamaCppRunner
     */
    public static Builder builder(String modelPath) {
        return new Builder(modelPath);
    }
    
    /**
     * Ensures llama.cpp is downloaded and built
     */
    private void ensureLlamaCppAvailable() {
        Path llamaPath = Paths.get(LLAMA_CPP_DIR);
        if (!Files.exists(llamaPath)) {
            System.out.println("[LlamaCpp] llama.cpp not found. Attempting to download...");
            try {
                downloadLlamaCpp();
                buildLlamaCpp();
            } catch (Exception e) {
                System.err.println("[LlamaCpp] Failed to setup llama.cpp: " + e.getMessage());
                System.err.println("[LlamaCpp] Please manually install llama.cpp from: https://github.com/ggerganov/llama.cpp");
            }
        }
        
        // Check if executable exists
        String executable = getExecutablePath();
        if (!Files.exists(Paths.get(executable))) {
            System.err.println("[LlamaCpp] llama-cli not found at: " + executable);
            System.err.println("[LlamaCpp] Please build llama.cpp first.");
        }
    }
    
    /**
     * Downloads llama.cpp
     */
    private void downloadLlamaCpp() throws IOException, InterruptedException {
        System.out.println("[LlamaCpp] Downloading llama.cpp...");
        ProcessBuilder pb = new ProcessBuilder("git", "clone", "https://github.com/ggerganov/llama.cpp.git", LLAMA_CPP_DIR);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("Failed to clone llama.cpp");
        }
        System.out.println("[LlamaCpp] Download complete.");
    }
    
    /**
     * Builds llama.cpp
     */
    private void buildLlamaCpp() throws IOException, InterruptedException {
        System.out.println("[LlamaCpp] Building llama.cpp...");
        
        if (IS_WINDOWS) {
            // Windows build
            ProcessBuilder pb = new ProcessBuilder("cmake", "-B", "build", "-DCMAKE_BUILD_TYPE=Release");
            pb.directory(new File(LLAMA_CPP_DIR));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
            
            pb = new ProcessBuilder("cmake", "--build", "build", "--config", "Release", "--parallel");
            pb.directory(new File(LLAMA_CPP_DIR));
            pb.redirectErrorStream(true);
            p = pb.start();
            p.waitFor();
        } else {
            // Linux/Mac build
            ProcessBuilder pb = new ProcessBuilder("make", "-j", String.valueOf(Runtime.getRuntime().availableProcessors()));
            pb.directory(new File(LLAMA_CPP_DIR));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
        }
        
        System.out.println("[LlamaCpp] Build complete.");
    }
    
    /**
     * Gets the path to the llama-cli executable
     */
    private String getExecutablePath() {
        if (IS_WINDOWS) {
            return LLAMA_CPP_DIR + "/build/bin/Release/llama-cli.exe";
        } else {
            return LLAMA_CPP_DIR + "/build/bin/llama-cli";
        }
    }
    
    /**
     * Gets the path to the llama-server executable
     */
    private String getServerPath() {
        if (IS_WINDOWS) {
            return LLAMA_CPP_DIR + "/build/bin/Release/llama-server.exe";
        } else {
            return LLAMA_CPP_DIR + "/build/bin/llama-server";
        }
    }
    
    /**
     * Runs inference on a single prompt
     */
    public String generate(String prompt) throws IOException, InterruptedException {
        return generate(prompt, 100); // Default 100 tokens
    }
    
    /**
     * Runs inference on a single prompt with custom token count
     */
    public String generate(String prompt, int nPredict) throws IOException, InterruptedException {
        System.out.println("[LlamaCpp] Running inference with prompt: " + prompt.substring(0, Math.min(50, prompt.length())) + "...");
        
        String executable = getExecutablePath();
        if (!Files.exists(Paths.get(executable))) {
            throw new IOException("llama-cli not found at: " + executable);
        }
        
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-m");
        command.add(modelPath);
        command.add("-p");
        command.add(prompt);
        command.add("-n");
        command.add(String.valueOf(nPredict));
        command.add("-c");
        command.add(String.valueOf(contextLength));
        command.add("-b");
        command.add(String.valueOf(batchSize));
        command.add("-t");
        command.add(String.valueOf(threads));
        command.add("--temp");
        command.add(String.valueOf(temperature));
        command.add("--top-k");
        command.add(String.valueOf(topK));
        command.add("--top-p");
        command.add(String.valueOf(topP));
        command.add("--repeat-penalty");
        command.add(String.valueOf(repeatPenalty));
        command.add("--no-display-prompt");
        command.add("--simple-io");
        
        // Additional flags for better output
        command.add("--keep");
        command.add("0");
        command.add("-e");
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println("[LlamaCpp] " + line);
            }
        }
        
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("llama-cli failed with exit code: " + exitCode);
        }
        
        return output.toString();
    }
    
    /**
     * Starts the llama.cpp server for API access
     */
    public void startServer() throws IOException, InterruptedException {
        if (isServerRunning) {
            System.out.println("[LlamaCpp] Server already running on port: " + serverPort);
            return;
        }
        
        String serverPath = getServerPath();
        if (!Files.exists(Paths.get(serverPath))) {
            throw new IOException("llama-server not found at: " + serverPath);
        }
        
        System.out.println("[LlamaCpp] Starting llama-server on port: " + serverPort);
        
        List<String> command = new ArrayList<>();
        command.add(serverPath);
        command.add("-m");
        command.add(modelPath);
        command.add("-c");
        command.add(String.valueOf(contextLength));
        command.add("-b");
        command.add(String.valueOf(batchSize));
        command.add("-t");
        command.add(String.valueOf(threads));
        command.add("--port");
        command.add(String.valueOf(serverPort));
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        serverProcess = pb.start();
        
        // Wait for server to start
        Thread.sleep(2000);
        isServerRunning = true;
        System.out.println("[LlamaCpp] Server started successfully.");
    }
    
    /**
     * Stops the llama.cpp server
     */
    public void stopServer() {
        if (serverProcess != null && isServerRunning) {
            System.out.println("[LlamaCpp] Stopping llama-server...");
            serverProcess.destroy();
            try {
                serverProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                serverProcess.destroyForcibly();
            }
            isServerRunning = false;
            serverProcess = null;
            System.out.println("[LlamaCpp] Server stopped.");
        }
    }
    
    /**
     * Server info for API access
     */
    public String getServerUrl() {
        return "http://localhost:" + serverPort;
    }
    
    /**
     * Checks if server is running
     */
    public boolean isServerRunning() {
        return isServerRunning;
    }
    
    /**
     * Runs inference using the server API (requires server to be running)
     */
    public String generateViaServer(String prompt) throws IOException, InterruptedException {
        return generateViaServer(prompt, 100);
    }
    
    /**
     * Runs inference using the server API
     */
    public String generateViaServer(String prompt, int nPredict) throws IOException, InterruptedException {
        if (!isServerRunning) {
            throw new IllegalStateException("Server is not running. Call startServer() first.");
        }
        
        // Use curl to call the server
        String jsonPayload = String.format(
            "{\"prompt\":\"%s\",\"n_predict\":%d,\"temperature\":%f,\"top_k\":%d,\"top_p\":%f,\"repeat_penalty\":%f}",
            prompt.replace("\"", "\\\"").replace("\n", "\\n"),
            nPredict,
            temperature,
            topK,
            topP,
            repeatPenalty
        );
        
        String url = getServerUrl() + "/completion";
        List<String> command;
        
        if (IS_WINDOWS) {
            command = Arrays.asList(
                "curl", "-X", "POST", url,
                "-H", "Content-Type: application/json",
                "-d", jsonPayload
            );
        } else {
            command = Arrays.asList(
                "curl", "-s", "-X", "POST", url,
                "-H", "Content-Type: application/json",
                "-d", jsonPayload
            );
        }
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        
        p.waitFor();
        return output.toString();
    }
    
    /**
     * Validates the GGUF model file
     */
    public boolean validateModel() {
        try {
            System.out.println("[LlamaCpp] Validating model: " + modelPath);
            String executable = getExecutablePath();
            
            List<String> command = new ArrayList<>();
            command.add(executable);
            command.add("-m");
            command.add(modelPath);
            command.add("--info");
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = p.waitFor();
            boolean isValid = exitCode == 0 && output.toString().contains("llama_model");
            
            if (isValid) {
                System.out.println("[LlamaCpp] Model validation: PASSED");
            } else {
                System.out.println("[LlamaCpp] Model validation: FAILED");
                System.out.println("[LlamaCpp] Output: " + output);
            }
            
            return isValid;
        } catch (Exception e) {
            System.err.println("[LlamaCpp] Model validation error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets model info from GGUF
     */
    public String getModelInfo() throws IOException, InterruptedException {
        String executable = getExecutablePath();
        
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.add("-m");
        command.add(modelPath);
        command.add("--info");
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        p.waitFor();
        return output.toString();
    }
    
    /**
     * Runs a benchmark test
     */
    public String benchmark(String prompt, int nPredict, int iterations) throws IOException, InterruptedException {
        System.out.println("[LlamaCpp] Running benchmark: " + iterations + " iterations");
        
        StringBuilder results = new StringBuilder();
        long totalTime = 0;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            String response = generate(prompt, nPredict);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            totalTime += duration;
            
            results.append(String.format("Iteration %d: %d ms\n", i + 1, duration));
            results.append("Response length: " + response.length() + " chars\n\n");
        }
        
        double avgTime = (double) totalTime / iterations;
        results.append(String.format("Average time: %.2f ms\n", avgTime));
        results.append(String.format("Tokens per second: %.2f\n", (nPredict * 1000.0) / avgTime));
        
        return results.toString();
    }
    
    /**
     * Reads the process output
     */
    private String readProcessOutput(Process p) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString();
    }
    
    /**
     * Cleanup resources
     */
    public void close() {
        stopServer();
    }
}
