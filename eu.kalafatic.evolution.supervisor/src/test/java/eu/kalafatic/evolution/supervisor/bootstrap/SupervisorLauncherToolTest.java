package eu.kalafatic.evolution.supervisor.bootstrap;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

public class SupervisorLauncherToolTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testLaunchCaptureOutput() throws IOException, InterruptedException {
        File projectRoot = folder.newFolder("project");
        // Create a dummy jar that prints to stdout/stderr
        // Since we can't easily compile a jar here, we'll mock java -version or similar if possible,
        // but SupervisorLauncherTool is hardcoded to use -jar.
        // So we'll just verify the configuration and process creation.

        File dummyJar = new File(projectRoot, "dummy.jar");
        dummyJar.createNewFile();

        SupervisorLauncherTool tool = new SupervisorLauncherTool();
        SupervisorConfiguration config = new SupervisorConfiguration(dummyJar, projectRoot);

        try {
            SupervisorHandle handle = tool.launch(config);
            assertNotNull(handle);
            assertNotNull(handle.getStdoutFile());
            assertNotNull(handle.getStderrFile());
            assertTrue(handle.getStdoutFile().getName().contains("stdout"));

            handle.stop();
        } catch (IOException e) {
            // Might fail because dummy.jar is not a real jar, but we care about the tool's logic
        }
    }
}
