package eu.kalafatic.evolution.controller.orchestration.mediated.analysis;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import eu.kalafatic.evolution.controller.agents.MetadataAgent;
import eu.kalafatic.evolution.controller.agents.MetadataResult;

public class MetadataAgentTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testGenerate() throws IOException {
        File root = tempFolder.newFolder("test-project");
        File src = new File(root, "src/eu/kalafatic/evolution/orchestration");
        src.mkdirs();
        File javaFile = new File(src, "MyClass.java");
        Files.write(javaFile.toPath(), "package eu.kalafatic.evolution.orchestration;\npublic class MyClass {}\n".getBytes());

        MetadataAgent generator = new MetadataAgent();
        MetadataResult result = generator.generate(root);

        assertNotNull("Result should not be null", result);
        assertTrue("Generated files should not be empty", !result.getGeneratedFiles().isEmpty());

        // Check sidecar
        File sidecar = new File(src, "MyClass.java.ai.json");
        assertTrue("Sidecar should exist", sidecar.exists());
        String content = new String(Files.readAllBytes(sidecar.toPath()));
        assertTrue("Role should be orchestration", content.contains("\"role\": \"orchestration\""));
        assertTrue("Importance should be high", content.contains("\"importanceScore\": 1.0"));

        // Check navigation maps
        assertTrue("ARCHITECTURE_CONTEXT.md should exist", new File(root, "ARCHITECTURE_CONTEXT.md").exists());
        assertTrue("SEMANTIC_OVERVIEW.md should exist", new File(root, "SEMANTIC_OVERVIEW.md").exists());
        assertTrue("TRAJECTORY_MAP.json should exist", new File(root, "TRAJECTORY_MAP.json").exists());
        assertTrue("PACKAGE_CONTEXT.md should exist in src", new File(src, "PACKAGE_CONTEXT.md").exists());

        // Verify result tracking
        assertTrue("Result should contain ARCHITECTURE_CONTEXT.md", result.getGeneratedFiles().stream().anyMatch(f -> f.getName().equals(MetadataAgent.ARCHITECTURE_CONTEXT)));
    }
}
