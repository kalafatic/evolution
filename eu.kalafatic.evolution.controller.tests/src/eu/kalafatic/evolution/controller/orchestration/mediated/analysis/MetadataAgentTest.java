package eu.kalafatic.evolution.controller.orchestration.mediated.analysis;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    @Test
    public void testGenerateWithOptions() throws IOException {
        File root = tempFolder.newFolder("test-project-options");
        File javaFile = new File(root, "Test.java");
        Files.write(javaFile.toPath(), "public class Test {}".getBytes());

        MetadataAgent generator = new MetadataAgent();

        // 1. Test skipExisting
        File sidecar = new File(root, "Test.java.ai.json");
        Files.write(sidecar.toPath(), "existing content".getBytes());
        long lastMod = sidecar.lastModified();

        MetadataAgent.Options options = new MetadataAgent.Options();
        options.skipExisting = true;
        generator.generate(root, options, null);

        assertTrue("Sidecar should not have been overwritten", sidecar.exists());
        String content = new String(Files.readAllBytes(sidecar.toPath()));
        assertTrue("Sidecar should contain original content", content.contains("existing content"));

        // 2. Test cleanExisting
        options = new MetadataAgent.Options();
        options.cleanExisting = true;
        generator.generate(root, options, null);

        assertTrue("New sidecar should have been generated after clean", sidecar.exists());
        content = new String(Files.readAllBytes(sidecar.toPath()));
        assertFalse("Sidecar should NOT contain original content", content.contains("existing content"));
        assertTrue("Sidecar should be a valid JSON", content.contains("\"role\":"));

        // 3. Test useTimestamp
        Files.write(sidecar.toPath(), "content before timestamp".getBytes());
        options = new MetadataAgent.Options();
        options.useTimestamp = true;
        generator.generate(root, options, null);

        String suffix = new SimpleDateFormat("_ddMMyy").format(new Date());
        File renamed = new File(root, "Test.java" + suffix + ".ai.json");
        assertTrue("Renamed sidecar with timestamp should exist: " + renamed.getAbsolutePath(), renamed.exists());
        assertTrue("New sidecar should also exist", sidecar.exists());

        String renamedContent = new String(Files.readAllBytes(renamed.toPath()));
        assertTrue("Renamed sidecar should contain original content", renamedContent.contains("content before timestamp"));
    }
}
