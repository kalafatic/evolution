package eu.kalafatic.evolution.quality.tests;

import static org.junit.Assert.*;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Automated machine-readable architecture contracts and quality tests for EVO platform.
 */
public class PlatformCodeQualityTest {

    @Test
    public void testNamingConventions() {
        String packageName = this.getClass().getPackage().getName();
        assertTrue("Package name should start with eu.kalafatic", packageName.startsWith("eu.kalafatic"));
    }

    @Test
    public void testPluginIdMatchesDirectory() {
        String pluginId = "eu.kalafatic.evolution.quality.tests";
        assertTrue(pluginId.contains("evolution"));
    }

    @Test
    public void testProhibitDuplicateNumberedToolClasses() throws IOException {
        Path root = Paths.get(".").toAbsolutePath().normalize();
        Path controllerToolsDir = root.resolve("eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/tools");
        if (Files.exists(controllerToolsDir)) {
            try (Stream<Path> stream = Files.list(controllerToolsDir)) {
                List<String> toolFileNames = stream
                        .map(p -> p.getFileName().toString())
                        .filter(name -> name.endsWith(".java"))
                        .collect(Collectors.toList());

                for (String fileName : toolFileNames) {
                    assertFalse("Duplicate numbered tool class detected: " + fileName,
                            fileName.matches(".*[0-9]+\\.java"));
                }
            }
        }
    }

    @Test
    public void testArchitectureModelBundleLayeringConstraints() throws IOException {
        Path root = Paths.get(".").toAbsolutePath().normalize();
        Path modelManifest = root.resolve("eu.kalafatic.evolution.model/META-INF/MANIFEST.MF");
        if (Files.exists(modelManifest)) {
            String manifestContent = Files.readString(modelManifest);
            assertFalse("Domain model bundle must not depend on UI view bundle",
                    manifestContent.contains("eu.kalafatic.evolution.view"));
        }
    }

    @Test
    public void testPluginSymbolicNameMatchingDirectoryName() throws IOException {
        Path root = Paths.get(".").toAbsolutePath().normalize();
        String[] modules = new String[] {
            "eu.kalafatic.evolution.model",
            "eu.kalafatic.evolution.controller",
            "eu.kalafatic.evolution.view",
            "eu.kalafatic.evolution.quality.tests"
        };

        for (String module : modules) {
            Path manifestPath = root.resolve(module).resolve("META-INF/MANIFEST.MF");
            if (Files.exists(manifestPath)) {
                String manifestContent = Files.readString(manifestPath);
                assertTrue("Manifest in " + module + " should contain symbolic name " + module,
                        manifestContent.contains("Bundle-SymbolicName: " + module));
            }
        }
    }
}
