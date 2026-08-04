package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class EvoProductBuildService {
    private final File baseDir;
    private final ProcessRunner runner;

    public EvoProductBuildService(File baseDir) {
        this.baseDir = baseDir;
        this.runner = new ProcessRunner();
    }

    public EvoProductBuildService(File baseDir, ProcessRunner runner) {
        this.baseDir = baseDir;
        this.runner = runner != null ? runner : new ProcessRunner();
    }

    public ProductBuildResult buildProduct() {
        if (runner != null && runner.getMockBuildResult() != null) {
            System.out.println("[EvoProductBuildService] [MOCK] Bypassing actual product build, returning mock build result: " + runner.getMockBuildResult());
            boolean success = runner.getMockBuildResult();
            return new ProductBuildResult(success, success ? 0 : 1, Instant.now(), Instant.now(), success ? baseDir.toPath().resolve("release/mock-artifact") : null, success ? "Mock build successful" : "Mock build failed");
        }

        Instant startedAt = Instant.now();
        System.out.println("[EvoProductBuildService] Triggering headless product build...");

        boolean success = runner.runBuild(baseDir);
        Instant finishedAt = Instant.now();

        Path releaseDir = baseDir.toPath().resolve("release");
        Path artifactPath = null;

        if (success) {
            String expectedName = PlatformInfo.isWindows() ? "EVO-win-x64.zip" : "EVO-linux-x64.tar.gz";
            Path expectedArtifact = releaseDir.resolve(expectedName);
            if (Files.exists(expectedArtifact)) {
                artifactPath = expectedArtifact;
                System.out.println("[EvoProductBuildService] Located build artifact: " + artifactPath.toAbsolutePath());
            } else {
                // Wildcard fallback
                System.out.println("[EvoProductBuildService] Standard artifact " + expectedName + " not found. Checking alternate locations...");
                try {
                    Path productsDir = baseDir.toPath().resolve("eu.kalafatic.evolution.repository/target/products");
                    if (Files.exists(productsDir)) {
                        artifactPath = Files.walk(productsDir)
                            .filter(p -> p.toString().endsWith(".zip") || p.toString().endsWith(".tar.gz"))
                            .findFirst()
                            .orElse(null);
                    }
                } catch (IOException ignored) {}
            }
        }

        String summary = success ? "Headless product built successfully" : "Maven build failed";
        if (success && artifactPath == null) {
            success = false;
            summary = "Build succeeded but product package archive is missing";
        }

        return new ProductBuildResult(success, success ? 0 : 1, startedAt, finishedAt, artifactPath, summary);
    }
}
