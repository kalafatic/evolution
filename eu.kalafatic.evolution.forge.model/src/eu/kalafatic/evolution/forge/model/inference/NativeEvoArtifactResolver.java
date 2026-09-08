package eu.kalafatic.evolution.forge.model.inference;

import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Production-safe model resolution contract for native EVO artifacts.
 * Explicitly resolves, validates, and distinguishes between .evo files,
 * model directories, GGUF files, and missing/unsupported paths.
 */
public class NativeEvoArtifactResolver {

    public static class ArtifactResolutionResult {
        private final Path resolvedPath;
        private final EvoModelArtifact artifact;

        public ArtifactResolutionResult(Path resolvedPath, EvoModelArtifact artifact) {
            this.resolvedPath = resolvedPath;
            this.artifact = artifact;
        }

        public Path getResolvedPath() {
            return resolvedPath;
        }

        public EvoModelArtifact getArtifact() {
            return artifact;
        }
    }

    /**
     * Resolves a native EVO model artifact from a given Path.
     *
     * Rules:
     * - .evo file: accepted directly if it exists.
     * - GGUF file: rejected with IllegalArgumentException.
     * - directory: rejected unless it explicitly resolves to exactly one .evo artifact file inside.
     *   (If 0 or >1 .evo files exist in directory, fails immediately).
     * - missing path: throws FileNotFoundException.
     */
    public static ArtifactResolutionResult resolveNativeArtifact(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("[EVO-NATIVE] Model resolution path cannot be null");
        }

        String pathStr = path.toString().toLowerCase();

        // Check if path ends with .gguf
        if (pathStr.endsWith(".gguf")) {
            throw new IllegalArgumentException("[EVO-NATIVE] GGUF files (" + path.getFileName() + ") cannot be used by the native EVO engine. Use llama-cpp/Ollama runner for GGUF.");
        }

        // Check if path exists
        if (!Files.exists(path)) {
            // Attempt to check if path + ".evo" exists
            Path appendedEvo = path.getParent() != null ?
                    path.getParent().resolve(path.getFileName().toString() + ".evo") :
                    path.getFileSystem().getPath(path.toString() + ".evo");

            if (Files.exists(appendedEvo) && Files.isRegularFile(appendedEvo)) {
                path = appendedEvo;
            } else {
                throw new FileNotFoundException("[EVO-NATIVE] Model artifact path does not exist: " + path);
            }
        }

        // Handle Directory
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                List<Path> evoFiles = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".evo"))
                        .collect(Collectors.toList());

                if (evoFiles.isEmpty()) {
                    throw new IllegalArgumentException("[EVO-NATIVE] Directory '" + path.toAbsolutePath() +
                            "' does not contain a canonical .evo file artifact. Directories must contain exactly one .evo file.");
                } else if (evoFiles.size() > 1) {
                    throw new IllegalArgumentException("[EVO-NATIVE] Directory '" + path.toAbsolutePath() +
                            "' contains multiple .evo files (" + evoFiles.stream().map(p -> p.getFileName().toString()).collect(Collectors.joining(", ")) +
                            "). Resolution aborted to prevent ambiguous model selection.");
                } else {
                    path = evoFiles.get(0);
                }
            }
        }

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("[EVO-NATIVE] Path is not a regular file: " + path);
        }

        // Load the artifact using standard EvoModelArtifact.load
        EvoModelArtifact artifact = EvoModelArtifact.load(path);
        return new ArtifactResolutionResult(path, artifact);
    }
}
