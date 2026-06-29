package eu.kalafatic.evolution.supervisor.bootstrap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class RcpBuildToolTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testBuildFailure() throws IOException {
        File workspace = folder.newFolder("workspace");
        Files.write(new File(workspace, "pom.xml").toPath(), "<project><modelVersion>4.0.0</modelVersion><groupId>test</groupId><artifactId>test</artifactId><version>1</version></project>".getBytes());

        RcpBuildTool tool = new RcpBuildTool();
        BuildConfiguration config = new BuildConfiguration(workspace);
        // Invalid goal to force failure
        config.addGoal("invalid-goal");

        BuildResult result = tool.build(config);

        assertFalse(result.isSuccess());
        assertNotEquals(0, result.getExitCode());
        assertNotNull(result.getStdout());
    }
}
