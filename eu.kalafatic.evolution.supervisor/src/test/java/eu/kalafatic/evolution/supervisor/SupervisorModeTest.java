package eu.kalafatic.evolution.supervisor;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import eu.kalafatic.evolution.supervisor.ui.SupervisorRcpApplication;
import eu.kalafatic.evolution.supervisor.ui.SupervisorUiWindow;

public class SupervisorModeTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testSupervisorUiWindowInitialization() throws Exception {
        File baseDir = tempFolder.newFolder("supervisor-ui-test");
        SupervisorUiWindow ui = new SupervisorUiWindow(baseDir, 8189, 28180, true);
        Assert.assertNotNull(ui);
        Assert.assertFalse(ui.isRunning());
    }

    @Test
    public void testSupervisorRcpApplicationInstantiation() {
        SupervisorRcpApplication app = new SupervisorRcpApplication();
        Assert.assertNotNull(app);
    }

    @Test
    public void testSystemPropertyModeParsing() {
        System.setProperty("evo.supervisor.mode", "ui");
        Assert.assertEquals("ui", System.getProperty("evo.supervisor.mode"));
        System.setProperty("evo.supervisor.mode", "headless");
        Assert.assertEquals("headless", System.getProperty("evo.supervisor.mode"));
    }
}
