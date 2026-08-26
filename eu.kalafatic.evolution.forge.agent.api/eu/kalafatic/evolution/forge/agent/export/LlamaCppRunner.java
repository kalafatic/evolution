package eu.kalafatic.evolution.forge.agent.export;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Minimal llama.cpp runner - works with pre-downloaded binaries
 * No auto-download, no external dependencies
 */
public class LlamaCppRunner {

    private static final String LLAMA_CPP_DIR = System.getProperty("user.dir") + "/lib/llama-cpp";

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final boolean IS_LINUX = System.getProperty("os.name").toLowerCase().contains("nix") ||
                                             System.getProperty("os.name").toLowerCase().contains("nux");
    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");

    private final String modelPath;
    private final int contextLength;
    private final int threads;
    private final float temperature;
    private final int topK;
    private final float topP;
    private final float repeatPenalty;

    private String cliPath = null;
    private boolean initialized = false;

    /**
     * Builder for LlamaCppRunner
     */
    public static class Builder {
        private final String modelPath;
        private int contextLength = 2048;
        private int threads = Runtime.getRuntime().availableProcessors();
        private float temperature = 0.2f;
        private int topK = 40;
        private float topP = 0.95f;
        private float repeatPenalty = 1.1f;

        public Builder(String modelPath) {
            this.modelPath = modelPath;
        }

        public Builder contextLength(int contextLength) {
            this.contextLength = contextLength;
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

        public LlamaCppRunner build() {
            return new LlamaCppRunner(this);
        }
    }

    private LlamaCppRunner(Builder builder) {
        this.modelPath = builder.modelPath;
        this.contextLength = builder.contextLength;
        this.threads = builder.threads;
        this.temperature = builder.temperature;
        this.topK = builder.topK;
        this.topP = builder.topP;
        this.repeatPenalty = builder.repeatPenalty;

        init();
    }

    public static Builder builder(String modelPath) {
        return new Builder(modelPath);
    }

    private void init() {
        if (initialized) return;

        String osDir = getOsDir();
        String cliName = IS_WINDOWS ? "llama-cli.exe" : "llama-cli";

        // 1. Try OSGi Bundle / Resource Extraction via Reflection
        cliPath = resolveFromOsgiBundle(osDir, cliName);

        // 2. Fallback: Try filesystem search paths
        if (cliPath == null) {
            String codebasePath = getCodebasePathViaReflection();
            List<String> searchPaths = new ArrayList<>();
            if (codebasePath != null) {
                searchPaths.add(codebasePath + "/eu.kalafatic.evolution.controller/lib/llama-cpp/" + osDir + "/" + cliName);
                searchPaths.add(codebasePath + "/eu.kalafatic.evolution.controller/lib/llama-cpp/linux/" + cliName);
                searchPaths.add(codebasePath + "/eu.kalafatic.evolution.controller/lib/llama-cpp/win/" + cliName);
                searchPaths.add(codebasePath + "/lib/llama-cpp/" + osDir + "/" + cliName);
                searchPaths.add(codebasePath + "/lib/llama-cpp/linux/" + cliName);
                searchPaths.add(codebasePath + "/lib/llama-cpp/win/" + cliName);
                scanPluginsDirectory(codebasePath, searchPaths, osDir, cliName);
            }
            String userDir = System.getProperty("user.dir");
            if (userDir != null) {
                searchPaths.add(userDir + "/eu.kalafatic.evolution.controller/lib/llama-cpp/" + osDir + "/" + cliName);
                searchPaths.add(userDir + "/eu.kalafatic.evolution.controller/lib/llama-cpp/linux/" + cliName);
                searchPaths.add(userDir + "/eu.kalafatic.evolution.controller/lib/llama-cpp/win/" + cliName);
                searchPaths.add(userDir + "/lib/llama-cpp/" + osDir + "/" + cliName);
                searchPaths.add(userDir + "/lib/llama-cpp/linux/" + cliName);
                searchPaths.add(userDir + "/lib/llama-cpp/win/" + cliName);
                searchPaths.add(userDir + "/../eu.kalafatic.evolution.controller/lib/llama-cpp/" + osDir + "/" + cliName);
                searchPaths.add(userDir + "/../eu.kalafatic.evolution.controller/lib/llama-cpp/linux/" + cliName);
                searchPaths.add(userDir + "/../eu.kalafatic.evolution.controller/lib/llama-cpp/win/" + cliName);
                searchPaths.add(userDir + "/eu.kalafatic.evolution.forge.agent.api/lib/llama-cpp/" + osDir + "/" + cliName);
                scanPluginsDirectory(userDir, searchPaths, osDir, cliName);
                scanPluginsDirectory(userDir + "/..", searchPaths, osDir, cliName);
            }
            searchPaths.add(LLAMA_CPP_DIR + "/" + osDir + "/" + cliName);
            searchPaths.add(LLAMA_CPP_DIR + "/linux/" + cliName);
            searchPaths.add(LLAMA_CPP_DIR + "/win/" + cliName);
            searchPaths.add(LLAMA_CPP_DIR + "/" + cliName);
            searchPaths.add(System.getProperty("user.home") + "/llama.cpp/" + cliName);
            searchPaths.add(System.getProperty("user.home") + "/llama.cpp/build/bin/" + cliName);
            searchPaths.add("/usr/local/bin/llama-cli");
            searchPaths.add("/usr/bin/llama-cli");

            for (String path : searchPaths) {
                try {
                    Path p = Paths.get(path);
                    if (Files.exists(p)) {
                        cliPath = p.toAbsolutePath().normalize().toString();
                        System.out.println("[LlamaCpp] Found llama-cli at: " + cliPath);
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (cliPath != null) {
            try {
                File file = new File(cliPath);
                if (!IS_WINDOWS && file.exists()) {
                    file.setExecutable(true, false);
                }
            } catch (Exception e) {
                // Non-critical permission log
            }
        } else {
            System.err.println("[LlamaCpp] llama-cli not found. Please place llama-cli in: " + LLAMA_CPP_DIR + "/" + getOsDir() + "/");
        }

        initialized = true;
    }

    private String getCodebasePathViaReflection() {
        try {
            Class<?> clazz = Class.forName("eu.kalafatic.evolution.controller.manager.ProjectModelManager");
            return (String) clazz.getMethod("getCodebasePath").invoke(null);
        } catch (Throwable t1) {
            try {
                Class<?> clazz = Class.forName("eu.kalafatic.evolution.view.provider.ProjectManager");
                return (String) clazz.getMethod("getCodebasePath").invoke(null);
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    private void scanPluginsDirectory(String parentDirPath, List<String> searchPaths, String osDir, String cliName) {
        if (parentDirPath == null) return;
        File pluginsDir = new File(parentDirPath, "plugins");
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            pluginsDir = new File(parentDirPath);
        }
        if (pluginsDir.exists() && pluginsDir.isDirectory()) {
            File[] pluginFolders = pluginsDir.listFiles((dir, name) -> name.contains("eu.kalafatic.evolution.controller") || name.contains("eu.kalafatic.evolution.forge"));
            if (pluginFolders != null) {
                for (File pFolder : pluginFolders) {
                    if (pFolder.isDirectory()) {
                        searchPaths.add(new File(pFolder, "lib/llama-cpp/" + osDir + "/" + cliName).getAbsolutePath());
                        searchPaths.add(new File(pFolder, "lib/llama-cpp/linux/" + cliName).getAbsolutePath());
                        searchPaths.add(new File(pFolder, "lib/llama-cpp/win/" + cliName).getAbsolutePath());
                        searchPaths.add(new File(pFolder, "lib/llama-cpp/" + cliName).getAbsolutePath());
                    }
                }
            }
        }
    }

    private String resolveFromOsgiBundle(String osDir, String cliName) {
        try {
            Class<?> frameworkUtilClass = Class.forName("org.osgi.framework.FrameworkUtil");

            // Try bundles for LlamaCppRunner and LlamaService (controller bundle)
            List<Object> bundles = new ArrayList<>();
            Object b1 = frameworkUtilClass.getMethod("getBundle", Class.class).invoke(null, LlamaCppRunner.class);
            if (b1 != null) bundles.add(b1);

            try {
                Class<?> llamaServiceClass = Class.forName("eu.kalafatic.evolution.controller.manager.LlamaService");
                Object b2 = frameworkUtilClass.getMethod("getBundle", Class.class).invoke(null, llamaServiceClass);
                if (b2 != null && !bundles.contains(b2)) bundles.add(b2);
            } catch (Throwable ignored) {}

            for (Object bundle : bundles) {
                try {
                    Class<?> fileLocatorClass = Class.forName("org.eclipse.core.runtime.FileLocator");
                    Class<?> bundleClass = Class.forName("org.osgi.framework.Bundle");
                    Object bundleFileObj = fileLocatorClass.getMethod("getBundleFile", bundleClass).invoke(null, bundle);
                    if (bundleFileObj instanceof File) {
                        File bundleDir = (File) bundleFileObj;
                        if (bundleDir.isDirectory()) {
                            File[] candidateFiles = {
                                new File(bundleDir, "lib/llama-cpp/" + osDir + "/" + cliName),
                                new File(bundleDir, "lib/llama-cpp/linux/" + cliName),
                                new File(bundleDir, "lib/llama-cpp/win/" + cliName),
                                new File(bundleDir, "lib/llama-cpp/" + cliName)
                            };
                            for (File f : candidateFiles) {
                                if (f.exists()) {
                                    System.out.println("[LlamaCpp] Resolved bundle file path: " + f.getAbsolutePath());
                                    return f.getAbsolutePath();
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}

                String[] candidateSubPaths = {
                    "lib/llama-cpp/" + osDir + "/" + cliName,
                    "/lib/llama-cpp/" + osDir + "/" + cliName,
                    "lib/llama-cpp/linux/" + cliName,
                    "/lib/llama-cpp/linux/" + cliName,
                    "lib/llama-cpp/win/" + cliName,
                    "/lib/llama-cpp/win/" + cliName,
                    "lib/llama-cpp/" + cliName,
                    "/lib/llama-cpp/" + cliName
                };

                for (String subPath : candidateSubPaths) {
                    Object entryUrl = bundle.getClass().getMethod("getEntry", String.class).invoke(bundle, subPath);
                    if (entryUrl != null) {
                        Class<?> fileLocatorClass = Class.forName("org.eclipse.core.runtime.FileLocator");
                        java.net.URL fileUrl = (java.net.URL) fileLocatorClass.getMethod("toFileURL", java.net.URL.class).invoke(null, entryUrl);
                        if (fileUrl != null) {
                            String urlStr = fileUrl.toExternalForm();
                            File extractedFile = null;
                            if (urlStr.startsWith("file:")) {
                                extractedFile = new File(fileUrl.getPath());
                                if (!extractedFile.exists()) {
                                    try {
                                        extractedFile = new File(fileUrl.toURI());
                                    } catch (Exception ignored) {}
                                }
                            }
                            if (extractedFile != null && extractedFile.exists()) {
                                System.out.println("[LlamaCpp] Resolved OSGi bundle entry path: " + extractedFile.getAbsolutePath());
                                return extractedFile.getAbsolutePath();
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // OSGi/Eclipse classes not present on context classpath - fallback to filesystem
        }
        return null;
    }

    private String getOsDir() {
        if (IS_WINDOWS) return "win";
        if (IS_MAC) return "mac";
        if (IS_LINUX) return "linux";
        return "linux";
    }

    public boolean isAvailable() {
        return cliPath != null && Files.exists(Paths.get(cliPath));
    }

    /**
     * Terminate process and entire process tree forcibly.
     */
    private void terminateProcessTree(Process p) {
        if (p == null) return;
        try {
            p.toHandle().descendants().forEach(ProcessHandle::destroyForcibly);
        } catch (Throwable ignored) {}
        try {
            p.destroyForcibly();
        } catch (Throwable ignored) {}
    }

    /**
     * Validates the GGUF model file with bounded timeout and process tree termination.
     */
    public boolean validateModel() {
        if (!isAvailable()) {
            System.err.println("[LlamaCpp] llama-cli not available");
            return false;
        }

        try {
            System.out.println("[LlamaCpp] Validating model: " + modelPath);

            List<String> command = new ArrayList<>();
            command.add(cliPath);
            command.add("-m");
            command.add(modelPath);
            command.add("-v");

            ProcessBuilder pb = createProcessBuilder(command);
            Process p = pb.start();

            StringBuilder stdOut = new StringBuilder();
            StringBuilder stdErr = new StringBuilder();

            Thread stdOutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdOut.append(line).append("\n");
                        if (line.contains("llama_model_loader")) {
                            System.out.println("[LlamaCpp] " + line);
                        }
                    }
                } catch (IOException ignored) {}
            });
            stdOutThread.setDaemon(true);
            stdOutThread.start();

            Thread stdErrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdErr.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            });
            stdErrThread.setDaemon(true);
            stdErrThread.start();

            boolean finished = p.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                terminateProcessTree(p);
                System.err.println("[LlamaCpp] LLAMA_CPP_TIMEOUT: Validation timed out after 30s");
                return false;
            }

            int exitCode = p.exitValue();
            String combinedOutput = stdOut.toString() + "\n" + stdErr.toString();
            boolean isValid = exitCode == 0 && combinedOutput.contains("llama_model_loader");

            if (isValid) {
                System.out.println("[LlamaCpp] ✅ Model validation: PASSED");
            } else {
                System.out.println("[LlamaCpp] ❌ Model validation: FAILED");
            }

            return isValid;
        } catch (Exception e) {
            System.err.println("[LlamaCpp] Validation error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Runs inference on a single prompt
     */
    public String generate(String prompt) throws IOException, InterruptedException {
        return generate(prompt, 20);
    }

    /**
     * Runs inference with custom token count with bounded 30s timeout and full tree termination.
     */
    public String generate(String prompt, int nPredict) throws IOException, InterruptedException {
        if (!isAvailable()) {
            throw new IOException("llama-cli not available at: " + cliPath);
        }

        System.out.println("[LlamaCpp] Running inference...");

        List<String> command = new ArrayList<>();
        command.add(cliPath);
        command.add("-m");
        command.add(modelPath);
        command.add("-p");
        command.add(prompt);
        command.add("-n");
        command.add(String.valueOf(nPredict));
        command.add("-c");
        command.add(String.valueOf(contextLength));
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

        ProcessBuilder pb = createProcessBuilder(command);
        Process p = pb.start();

        StringBuilder stdOut = new StringBuilder();
        StringBuilder stdErr = new StringBuilder();

        Thread stdOutThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdOut.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });
        stdOutThread.setDaemon(true);
        stdOutThread.start();

        Thread stdErrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    stdErr.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });
        stdErrThread.setDaemon(true);
        stdErrThread.start();

        boolean finished = p.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            terminateProcessTree(p);
            throw new IOException("LLAMA_CPP_TIMEOUT: llama-cli execution timed out after 30s");
        }

        int exitCode = p.exitValue();
        if (exitCode != 0) {
            throw new IOException("llama-cli failed with exit code: " + exitCode + "\nStdOut: " + stdOut + "\nStdErr: " + stdErr);
        }

        return stdOut.toString();
    }

    /**
     * Gets model info
     */
    public String getModelInfo() throws IOException, InterruptedException {
        if (!isAvailable()) {
            throw new IOException("llama-cli not available");
        }

        List<String> command = new ArrayList<>();
        command.add(cliPath);
        command.add("-m");
        command.add(modelPath);
        command.add("-v");

        ProcessBuilder pb = createProcessBuilder(command);
        Process p = pb.start();

        StringBuilder output = new StringBuilder();
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            } catch (IOException ignored) {}
        });
        readerThread.setDaemon(true);
        readerThread.start();

        boolean finished = p.waitFor(15, TimeUnit.SECONDS);
        if (!finished) {
            terminateProcessTree(p);
            throw new IOException("LLAMA_CPP_TIMEOUT: llama-cli getModelInfo timed out after 15s");
        }

        return output.toString();
    }

    private ProcessBuilder createProcessBuilder(List<String> command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (cliPath != null) {
            Path cliParent = Paths.get(cliPath).getParent();
            if (cliParent != null && Files.exists(cliParent)) {
                pb.directory(cliParent.toFile());
                if (!IS_WINDOWS) {
                    String ldPath = pb.environment().get("LD_LIBRARY_PATH");
                    String parentPath = cliParent.toAbsolutePath().toString();
                    pb.environment().put("LD_LIBRARY_PATH", (ldPath != null && !ldPath.isEmpty()) ? parentPath + ":" + ldPath : parentPath);
                }
            }
        }
        return pb;
    }
}
