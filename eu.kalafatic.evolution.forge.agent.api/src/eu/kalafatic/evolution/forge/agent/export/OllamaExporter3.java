package eu.kalafatic.evolution.forge.agent.export;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.llm.EvoModelExporter;


/**
 * EVO -> GGUF -> llama.cpp -> Ollama exporter/validator.
 *
 * This class intentionally does NOT perform the actual EVO-to-GGUF tensor
 * conversion. It validates an already generated GGUF and gates Ollama
 * registration/runtime on successful llama.cpp execution.
 */
public class OllamaExporter3 implements EvoModelExporter {


    private static final Duration LLAMA_TIMEOUT =
            Duration.ofMinutes(2);

    private static final Duration OLLAMA_TIMEOUT =
            Duration.ofMinutes(2);

    private static final Duration PROCESS_KILL_GRACE =
            Duration.ofSeconds(2);

    private final Path llamaCli;
    private final Path ollamaExecutable;

    public OllamaExporter3() {
        this(null, null);
    }

    public OllamaExporter3(
            Path llamaCli,
            Path ollamaExecutable) {

        this.llamaCli =
                llamaCli != null
                        ? llamaCli
                        : findLlamaCli();

        this.ollamaExecutable =
                ollamaExecutable != null
                        ? ollamaExecutable
                        : findOllama();
    }

    // =====================================================================
    // PUBLIC EXPORT API
    // =====================================================================
    
    @Override
    public void export(EvoModelArtifact artifact, Path outputPath) throws Exception {
        EvoLlmModel model = artifact.createModel();
        java.util.Map<Integer, String> customVocab = new java.util.HashMap<>();
        artifact.getTokenizerVocab().forEach((k, v) -> customVocab.put(v, k));
        export(artifact.getModelName(), outputPath, model);
    }

    /**
     * Existing EVO exporter API.
     *
     * The GGUF path is expected to point to the already generated GGUF.
     */
    public Path export(
            String modelName,
            Path gguf,
            EvoLlmModel model) throws IOException {

        if (modelName == null || modelName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "modelName must not be empty");
        }

        if (gguf == null) {
            throw new IllegalArgumentException(
                    "gguf must not be null");
        }

        gguf = gguf.toAbsolutePath().normalize();

        if (!Files.exists(gguf)) {
            throw new IOException(
                    "[OLLAMA] GGUF does not exist: " + gguf);
        }

        if (!Files.isRegularFile(gguf)) {
            throw new IOException(
                    "[OLLAMA] GGUF is not a regular file: " + gguf);
        }

        log("==================================================");
        log("[OLLAMA] EVO EXPORT");
        log("[OLLAMA] Model : " + modelName);
        log("[OLLAMA] GGUF  : " + gguf);
        log("==================================================");

        /*
         * IMPORTANT:
         *
         * Do not send the GGUF to Ollama before llama.cpp has proven that
         * the file can actually be loaded and executed.
         */

        validateGguf(gguf);

        validateWithLlamaCpp(gguf);

        validateWithOllama(
                modelName,
                gguf);

        log("[OLLAMA] EXPORT VALIDATION PASSED");

        return gguf;
    }

    // =====================================================================
    // INDEPENDENT GGUF VALIDATION
    // =====================================================================

    /**
     * Minimal independent GGUF structural validation.
     *
     * This does not depend on Ollama or llama.cpp.
     */
    private void validateGguf(
            Path gguf) throws IOException {

        log("[GGUF] Independent validation started");

        long size =
                Files.size(gguf);

        if (size < 24) {
            throw new IOException(
                    "[GGUF] File is too small: "
                            + size
                            + " bytes");
        }

        try (InputStream in =
                     Files.newInputStream(gguf)) {

            byte[] magic =
                    readExactly(in, 4);

            String magicString =
                    new String(
                            magic,
                            StandardCharsets.US_ASCII);

            if (!"GGUF".equals(magicString)) {
                throw new IOException(
                        "[GGUF] Invalid magic: "
                                + magicString);
            }

            long version =
                    readUInt32LE(in);

            if (version < 1 || version > 3) {
                throw new IOException(
                        "[GGUF] Unsupported version: "
                                + version);
            }

            long tensorCount =
                    readUInt64LE(in);

            long metadataCount =
                    readUInt64LE(in);

            log("[GGUF] Magic          : GGUF");
            log("[GGUF] Version        : " + version);
            log("[GGUF] Tensor count   : " + tensorCount);
            log("[GGUF] Metadata count : " + metadataCount);
            log("[GGUF] File size      : " + size);

            if (tensorCount < 0) {
                throw new IOException(
                        "[GGUF] Invalid tensor count");
            }

            if (metadataCount < 0) {
                throw new IOException(
                        "[GGUF] Invalid metadata count");
            }
        }

        log("[GGUF] Independent validation PASSED");
    }

    // =====================================================================
    // LLAMA.CPP GATE
    // =====================================================================

    /**
     * llama.cpp is the actual execution gate.
     *
     * If llama.cpp cannot load the GGUF, the exporter stops here.
     *
     * The command is deliberately minimal. Application-level arguments such
     * as "explanation" must NEVER be passed as llama-cli command options.
     */
    private void validateWithLlamaCpp(
            Path gguf) throws IOException {

        if (llamaCli == null) {
            throw new IOException(
                    "[LlamaCpp] llama-cli was not found.");
        }

        if (!Files.exists(llamaCli)) {
            throw new IOException(
                    "[LlamaCpp] llama-cli does not exist: "
                            + llamaCli);
        }

        log("[LlamaCpp] ==================================");
        log("[LlamaCpp] EXECUTION GATE");
        log("[LlamaCpp] Executable: " + llamaCli);
        log("[LlamaCpp] Model     : " + gguf);

        /*
         * Keep this invocation extremely conservative.
         *
         * This specifically avoids the failure seen in your log:
         *
         *     error: invalid argument: explanation
         *
         * "explanation" is application/prompt data, not a llama-cli option.
         */
        List<String> command =
                new ArrayList<>();

        command.add(llamaCli.toString());
        command.add("-m");
        command.add(gguf.toString());
        command.add("-p");
        command.add("Hello.");
        command.add("-n");
        command.add("8");

        ProcessResult result =
                runProcess(
                        command,
                        LLAMA_TIMEOUT);

        if (result.timedOut) {
            throw new IOException(
                    "[LlamaCpp] TIMEOUT after "
                            + LLAMA_TIMEOUT
                            + "\nOutput:\n"
                            + result.output);
        }

        if (result.exitCode != 0) {
            throw new IOException(
                    "[LlamaCpp] EXECUTION FAILED"
                            + "\nExit code: "
                            + result.exitCode
                            + "\nOutput:\n"
                            + result.output);
        }

        log("[LlamaCpp] EXECUTION GATE PASSED");
    }

    // =====================================================================
    // OLLAMA VALIDATION
    // =====================================================================

    /**
     * Validate Ollama independently after llama.cpp succeeds.
     *
     * A temporary Ollama model is created so that an existing "evo" model
     * is never overwritten.
     */
    private void validateWithOllama(
            String modelName,
            Path gguf) throws IOException {

        if (ollamaExecutable == null) {
            throw new IOException(
                    "[Ollama] Ollama executable was not found.");
        }

        log("[Ollama] ===================================");
        log("[Ollama] VALIDATION");
        log("[Ollama] Executable: " + ollamaExecutable);

        String safeName =
                sanitizeModelName(modelName);

        String validationName =
                safeName
                        + "-validation-"
                        + System.currentTimeMillis();

        Path modelfile =
                Files.createTempFile(
                        "evo-validation-",
                        ".Modelfile");

        try {

            String modelfileText =
                    "FROM "
                            + quotePathForModelfile(gguf)
                            + System.lineSeparator();

            Files.writeString(
                    modelfile,
                    modelfileText,
                    StandardCharsets.UTF_8);

            log("[Ollama] Creating temporary model: "
                    + validationName);

            List<String> createCommand =
                    Arrays.asList(
                            ollamaExecutable.toString(),
                            "create",
                            validationName,
                            "-f",
                            modelfile.toString());

            ProcessResult createResult =
                    runProcess(
                            createCommand,
                            OLLAMA_TIMEOUT);

            if (createResult.timedOut) {
                throw new IOException(
                        "[Ollama] CREATE timed out.\n"
                                + createResult.output);
            }

            if (createResult.exitCode != 0) {
                throw new IOException(
                        "[Ollama] CREATE failed.\n"
                                + "Exit code: "
                                + createResult.exitCode
                                + "\n"
                                + createResult.output);
            }

            log("[Ollama] IMPORT PASSED");

            /*
             * Actual runtime test.
             */
            List<String> runCommand =
                    Arrays.asList(
                            ollamaExecutable.toString(),
                            "run",
                            validationName,
                            "Hello.");

            ProcessResult runResult =
                    runProcess(
                            runCommand,
                            OLLAMA_TIMEOUT);

            if (runResult.timedOut) {
                throw new IOException(
                        "[Ollama] RUN timed out.\n"
                                + runResult.output);
            }

            if (runResult.exitCode != 0) {
                throw new IOException(
                        "[Ollama] RUN failed.\n"
                                + "Exit code: "
                                + runResult.exitCode
                                + "\n"
                                + runResult.output);
            }

            log("[Ollama] RUNTIME VALIDATION PASSED");

        } finally {

            deleteQuietly(modelfile);

            removeTemporaryOllamaModel(
                    validationName);
        }
    }

    private void removeTemporaryOllamaModel(
            String modelName) {

        if (ollamaExecutable == null) {
            return;
        }

        try {

            List<String> command =
                    Arrays.asList(
                            ollamaExecutable.toString(),
                            "rm",
                            modelName);

            ProcessResult result =
                    runProcess(
                            command,
                            Duration.ofSeconds(30));

            if (result.exitCode == 0) {
                log("[Ollama] Temporary model removed");
            } else {
                log("[Ollama] WARNING: temporary model could not "
                        + "be removed: "
                        + result.output);
            }

        } catch (Exception e) {

            log("[Ollama] WARNING: cleanup failed: "
                    + e.getMessage());
        }
    }

    // =====================================================================
    // PROCESS RUNNER
    // =====================================================================

    /**
     * Central process execution with timeout.
     *
     * The Process reference is FINAL.
     *
     * This specifically fixes:
     *
     * "Local variable process is required to be final or effectively final"
     */
    private ProcessResult runProcess(
            List<String> command,
            Duration timeout) throws IOException {

        if (command == null
                || command.isEmpty()) {

            throw new IllegalArgumentException(
                    "command must not be empty");
        }

        log("[PROCESS] "
                + String.join(" ", command));

        final Process process =
                new ProcessBuilder(command)
                        .redirectErrorStream(true)
                        .start();

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        try {

            Future<String> outputFuture =
                    executor.submit(() -> {

                        try (InputStream input =
                                     process.getInputStream()) {

                            return new String(
                                    input.readAllBytes(),
                                    StandardCharsets.UTF_8);
                        }
                    });

            boolean finished;

            try {

                finished =
                        process.waitFor(
                                timeout.toMillis(),
                                TimeUnit.MILLISECONDS);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                process.destroyForcibly();

                return new ProcessResult(
                        -1,
                        true,
                        "Process interrupted.");
            }

            if (!finished) {

                log("[PROCESS] TIMEOUT: "
                        + timeout);

                process.destroy();

                try {

                    if (!process.waitFor(
                            PROCESS_KILL_GRACE.toMillis(),
                            TimeUnit.MILLISECONDS)) {

                        log("[PROCESS] Destroying forcibly");

                        process.destroyForcibly();
                    }

                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();

                    process.destroyForcibly();
                }

                String output =
                        getOutput(
                                outputFuture);

                return new ProcessResult(
                        -1,
                        true,
                        output);
            }

            String output =
                    getOutput(
                            outputFuture);

            int exitCode =
                    process.exitValue();

            return new ProcessResult(
                    exitCode,
                    false,
                    output);

        } finally {

            executor.shutdownNow();
        }
    }

    private String getOutput(
            Future<String> outputFuture) {

        if (outputFuture == null) {
            return "";
        }

        try {

            return outputFuture.get(
                    2,
                    TimeUnit.SECONDS);

        } catch (Exception e) {

            return "[Unable to collect process output: "
                    + e.getMessage()
                    + "]";
        }
    }

    // =====================================================================
    // EXECUTABLE DISCOVERY
    // =====================================================================

    private Path findLlamaCli() {

        String home =
                System.getProperty("user.home");

        if (home == null) {
            return null;
        }

        List<Path> candidates =
                Arrays.asList(

                        Paths.get(
                                home,
                                "llama.cpp",
                                "llama-cli.exe"),

                        Paths.get(
                                home,
                                "llama.cpp",
                                "build",
                                "bin",
                                "llama-cli.exe"),

                        Paths.get(
                                home,
                                "llama.cpp",
                                "build",
                                "bin",
                                "Release",
                                "llama-cli.exe"),

                        Paths.get(
                                home,
                                "llama.cpp",
                                "llama-cli"),

                        Paths.get(
                                "/usr/local/bin/llama-cli"),

                        Paths.get(
                                "/usr/bin/llama-cli")
                );

        for (Path candidate : candidates) {

            if (Files.isRegularFile(candidate)) {

                log("[LlamaCpp] Found llama-cli at: "
                        + candidate);

                return candidate
                        .toAbsolutePath()
                        .normalize();
            }
        }

        return findFromPath(
                "llama-cli");
    }

    private Path findOllama() {

        String explicit =
                System.getenv(
                        "OLLAMA_EXECUTABLE");

        if (explicit != null
                && !explicit.trim().isEmpty()) {

            Path path =
                    Paths.get(explicit);

            if (Files.isRegularFile(path)) {
                return path
                        .toAbsolutePath()
                        .normalize();
            }
        }

        String os =
                System.getProperty(
                        "os.name",
                        "")
                        .toLowerCase();

        List<Path> candidates =
                new ArrayList<>();

        if (os.contains("win")) {

            String user =
                    System.getProperty(
                            "user.name");

            candidates.add(
                    Paths.get(
                            "C:",
                            "Users",
                            user,
                            "AppData",
                            "Local",
                            "Programs",
                            "Ollama",
                            "ollama.exe"));

            candidates.add(
                    Paths.get(
                            "C:",
                            "Program Files",
                            "Ollama",
                            "ollama.exe"));
        }

        candidates.add(
                Paths.get(
                        "/usr/local/bin/ollama"));

        candidates.add(
                Paths.get(
                        "/usr/bin/ollama"));

        for (Path candidate : candidates) {

            if (Files.isRegularFile(candidate)) {

                log("[Ollama] Found ollama at: "
                        + candidate);

                return candidate
                        .toAbsolutePath()
                        .normalize();
            }
        }

        return findFromPath("ollama");
    }

    private Path findFromPath(
            String executable) {

        String os =
                System.getProperty(
                        "os.name",
                        "")
                        .toLowerCase();

        String locator =
                os.contains("win")
                        ? "where"
                        : "which";

        try {

            ProcessResult result =
                    runProcess(
                            Arrays.asList(
                                    locator,
                                    executable),
                            Duration.ofSeconds(10));

            if (result.exitCode != 0) {
                return null;
            }

            String output =
                    result.output.trim();

            if (output.isEmpty()) {
                return null;
            }

            String first =
                    output.split("\\R")[0].trim();

            Path path =
                    Paths.get(first);

            if (Files.exists(path)) {

                log("[PATH] Found "
                        + executable
                        + " at: "
                        + path);

                return path
                        .toAbsolutePath()
                        .normalize();
            }

        } catch (Exception e) {

            log("[PATH] Unable to find "
                    + executable
                    + ": "
                    + e.getMessage());
        }

        return null;
    }

    // =====================================================================
    // GGUF READER
    // =====================================================================

    private byte[] readExactly(
            InputStream input,
            int length) throws IOException {

        byte[] data =
                new byte[length];

        int offset = 0;

        while (offset < length) {

            int count =
                    input.read(
                            data,
                            offset,
                            length - offset);

            if (count < 0) {

                throw new IOException(
                        "Unexpected end of GGUF file");
            }

            offset += count;
        }

        return data;
    }

    private long readUInt32LE(
            InputStream input) throws IOException {

        byte[] b =
                readExactly(
                        input,
                        4);

        return
                ((long) b[0] & 0xffL)
                | (((long) b[1] & 0xffL) << 8)
                | (((long) b[2] & 0xffL) << 16)
                | (((long) b[3] & 0xffL) << 24);
    }

    private long readUInt64LE(
            InputStream input) throws IOException {

        byte[] b =
                readExactly(
                        input,
                        8);

        return
                ((long) b[0] & 0xffL)
                | (((long) b[1] & 0xffL) << 8)
                | (((long) b[2] & 0xffL) << 16)
                | (((long) b[3] & 0xffL) << 24)
                | (((long) b[4] & 0xffL) << 32)
                | (((long) b[5] & 0xffL) << 40)
                | (((long) b[6] & 0xffL) << 48)
                | (((long) b[7] & 0xffL) << 56);
    }

    // =====================================================================
    // UTILITIES
    // =====================================================================

    private String sanitizeModelName(
            String name) {

        String value =
                name.trim()
                        .toLowerCase()
                        .replaceAll(
                                "[^a-z0-9._-]",
                                "-");

        if (value.isEmpty()) {
            value = "evo";
        }

        return value;
    }

    private String quotePathForModelfile(
            Path path) {

        String value =
                path.toAbsolutePath()
                        .normalize()
                        .toString()
                        .replace(
                                "\\",
                                "/")
                        .replace(
                                "\"",
                                "\\\"");

        return "\"" + value + "\"";
    }

    private void deleteQuietly(
            Path path) {

        if (path == null) {
            return;
        }

        try {

            Files.deleteIfExists(path);

        } catch (IOException e) {

            log("[CLEANUP] Unable to delete "
                    + path
                    + ": "
                    + e.getMessage());
        }
    }

    private void log(
            String message) {

        System.out.println(message);
    }

    // =====================================================================
    // RESULT
    // =====================================================================

    private static final class ProcessResult {

        private final int exitCode;
        private final boolean timedOut;
        private final String output;

        private ProcessResult(
                int exitCode,
                boolean timedOut,
                String output) {

            this.exitCode = exitCode;
            this.timedOut = timedOut;
            this.output =
                    output != null
                            ? output
                            : "";
        }
    }

    
}