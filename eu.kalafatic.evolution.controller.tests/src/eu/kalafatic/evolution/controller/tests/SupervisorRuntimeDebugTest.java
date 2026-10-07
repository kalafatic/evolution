package eu.kalafatic.evolution.controller.tests;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SupervisorRuntime;

public class SupervisorRuntimeDebugTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testSelfDevContextDebugModeFlag() throws Exception {
        File projectRoot = tempFolder.newFolder("debug-test-root");
        SelfDevContext context = new SelfDevContext(projectRoot, null);

        context.setDebugMode(false);
        Assert.assertFalse(context.isDebugMode());
        Assert.assertEquals(0, context.getPortOffset());

        context.setDebugMode(true);
        Assert.assertTrue(context.isDebugMode());
        Assert.assertEquals(10, context.getPortOffset());
    }

    @Test
    public void testSupervisorRuntimeProcessGetters() {
        SupervisorRuntime runtime = new SupervisorRuntime();
        Assert.assertNotNull(runtime);
        Assert.assertFalse(runtime.isAlive());
        Assert.assertEquals(-1, runtime.getPid());
    }
}
