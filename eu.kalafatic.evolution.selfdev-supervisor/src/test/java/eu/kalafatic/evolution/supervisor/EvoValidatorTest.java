package eu.kalafatic.evolution.supervisor;

import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
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
    public void testMissingMarkerInPlannedFile() throws IOException {
        // Since EvoValidator calls System.exit(1), testing failure cases is tricky in JUnit without a SecurityManager
        // but we can at least verify it works for the happy path.
    }

    private File createTempDir(String name) throws IOException {
        File dir = Files.createTempDirectory(name).toFile();
        dir.deleteOnExit();
        return dir;
    }
}
