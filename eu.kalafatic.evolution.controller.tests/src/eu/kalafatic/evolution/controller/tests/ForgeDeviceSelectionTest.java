package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.forge.trainer.device.ForgeDeviceSelector;
import eu.kalafatic.evolution.forge.trainer.device.TrainingDevice;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression test for device-selection CPU fallback across runtimes.
 *
 * <p>The GPU bundle is compiled against stable Java 22 FFM (classfile 66.0). On a Java
 * 21 runtime those classes cannot load, so {@link ForgeDeviceSelector#isCudaAvailable()}
 * must catch the {@link java.lang.UnsupportedClassVersionError} and report the GPU as
 * unavailable, selecting CPU — it must never crash the application. On a Java 22+
 * runtime with a usable CUDA device it selects CUDA.</p>
 *
 * <p>These assertions hold on BOTH runtimes, so the test is a stable regression guard
 * regardless of the build JDK.</p>
 */
public class ForgeDeviceSelectionTest {

    @Test
    public void testIsCudaAvailableNeverThrows() {
        // Must return a boolean and never propagate a linkage error (e.g. a Java 22
        // GPU class on a Java 21 JVM throwing UnsupportedClassVersionError).
        boolean available = ForgeDeviceSelector.isCudaAvailable();
        assertTrue(available || !available); // trivially a boolean; proves no exception
    }

    @Test
    public void testExplicitCpuAlwaysSelectsCpu() {
        ForgeDeviceSelector.Selection sel = ForgeDeviceSelector.select(ForgeDeviceSelector.Policy.CPU);
        assertNotNull(sel);
        assertEquals("Explicit CPU must never select CUDA", TrainingDevice.CPU, sel.selected);
    }

    @Test
    public void testAutoSelectionIsValidAndNeverThrows() {
        ForgeDeviceSelector.Selection sel = ForgeDeviceSelector.select(ForgeDeviceSelector.Policy.AUTO);
        assertNotNull(sel);
        assertTrue("AUTO must select a valid device",
                sel.selected == TrainingDevice.CPU || sel.selected == TrainingDevice.CUDA);
        // Selection is truthful: if CUDA is not actually available, AUTO must be CPU.
        if (!sel.cudaAvailable) {
            assertEquals("AUTO must fall back to CPU when CUDA is unavailable",
                    TrainingDevice.CPU, sel.selected);
        }
    }

    @Test
    public void testExplicitCudaFallsBackToCpuWhenUnavailable() {
        ForgeDeviceSelector.Selection sel = ForgeDeviceSelector.select(ForgeDeviceSelector.Policy.CUDA);
        assertNotNull(sel);
        if (!sel.cudaAvailable) {
            assertEquals("Explicit CUDA without a usable device must fall back to CPU "
                    + "(with a note), never silently claim GPU",
                    TrainingDevice.CPU, sel.selected);
            assertNotNull(sel.note);
        }
    }
}
