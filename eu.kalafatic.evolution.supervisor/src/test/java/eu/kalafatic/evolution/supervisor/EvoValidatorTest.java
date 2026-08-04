package eu.kalafatic.evolution.supervisor;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

public class EvoValidatorTest {

    @Test
    public void testValidMarker() throws IOException {
        File tempDir = createTempDir("test-variant");
        File javaFile = new File(tempDir, "TestFile.java");
        Files.write(javaFile.toPath(), Collections.singletonList("// @evo:12:A reason=test"));

        EvoPlan plan = new EvoPlan();
        plan.setIteration(12);
        plan.setVariant("A");
        plan.setFiles(Collections.singletonList("TestFile.java"));

        new EvoValidator().validate(tempDir, plan);
        // Should not exit
    }

    @Test
    public void testProductLayoutValidation() throws IOException {
        File tempDir = createTempDir("test-layout-valid");

        File pluginsDir = new File(tempDir, "plugins");
        File configDir = new File(tempDir, "configuration");
        pluginsDir.mkdirs();
        configDir.mkdirs();

        if (PlatformInfo.isWindows()) {
            File exeFile = new File(tempDir, "evo.exe");
            exeFile.createNewFile();
        } else {
            File nativeFile = new File(tempDir, "evo");
            File shFile = new File(tempDir, "evo.sh");
            nativeFile.createNewFile();
            shFile.createNewFile();
        }

        EvoProductValidator validator = new EvoProductValidator();
        assertTrue(validator.validateLayout(tempDir));
    }

    @Test
    public void testProductLayoutValidationMissingPlugins() throws IOException {
        File tempDir = createTempDir("test-layout-invalid");
        File configDir = new File(tempDir, "configuration");
        configDir.mkdirs();

        if (PlatformInfo.isWindows()) {
            File exeFile = new File(tempDir, "evo.exe");
            exeFile.createNewFile();
        } else {
            File nativeFile = new File(tempDir, "evo");
            File shFile = new File(tempDir, "evo.sh");
            nativeFile.createNewFile();
            shFile.createNewFile();
        }

        EvoProductValidator validator = new EvoProductValidator();
        assertFalse(validator.validateLayout(tempDir));
    }

    private File createTempDir(String name) throws IOException {
        File dir = Files.createTempDirectory(name).toFile();
        dir.deleteOnExit();
        return dir;
    }
}
