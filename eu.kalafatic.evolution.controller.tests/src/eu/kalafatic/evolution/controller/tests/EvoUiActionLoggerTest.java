package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.kalafatic.evolution.controller.ui.EvoTooltipManager;
import eu.kalafatic.evolution.controller.ui.EvoUiActionDescriptor;
import eu.kalafatic.evolution.controller.ui.EvoUiActionLogger;

public class EvoUiActionLoggerTest {

    @Test
    public void testParameterSanitizationAndRedaction() {
        EvoUiActionDescriptor descriptor = new EvoUiActionDescriptor("btn_save", "Save Config", "SettingsPage", "save_config");
        descriptor.addParameter("username", "admin");
        descriptor.addParameter("password", "secret123");
        descriptor.addParameter("authToken", "bearer_xyz");
        descriptor.addParameter("multiline", "line1\nline2");

        assertEquals("admin", descriptor.getParameters().get("username"));
        assertEquals("******", descriptor.getParameters().get("password"));
        assertEquals("******", descriptor.getParameters().get("authToken"));
        assertEquals("line1\\nline2", descriptor.getParameters().get("multiline"));
    }

    @Test
    public void testLongValueTruncation() {
        String longText = "A".repeat(300);
        String sanitized = EvoUiActionDescriptor.sanitizeValue("data", longText);
        assertEquals(200, sanitized.length());
        assertTrue(sanitized.endsWith("..."));
    }

    @Test
    public void testLifecycleAndDurationCalculation() throws Exception {
        EvoUiActionDescriptor descriptor = new EvoUiActionDescriptor("btn_gen", "Generate", "MetadataPage", "generate_metadata");

        // Initial state before start: duration should default to 0 on completion
        EvoUiActionLogger.getInstance().logSuccess(descriptor, "Fast finish");
        assertEquals(0, descriptor.getDurationMs());
        assertEquals(EvoUiActionDescriptor.ActionStatus.ACTION_SUCCESS, descriptor.getStatus());

        // Proper start lifecycle
        EvoUiActionLogger.getInstance().logStart(descriptor);
        assertTrue(descriptor.getStartTimeMs() > 0);
        Thread.sleep(10);
        EvoUiActionLogger.getInstance().logSuccess(descriptor, "Done");
        assertTrue(descriptor.getDurationMs() >= 10);
    }

    @Test
    public void testFailureLoggingWithException() {
        EvoUiActionDescriptor descriptor = new EvoUiActionDescriptor("btn_run", "Run Task", "TaskPage", "run_task");
        EvoUiActionLogger.getInstance().logStart(descriptor);

        Exception ex = new IllegalArgumentException("Invalid input value\nwith multiline");
        EvoUiActionLogger.getInstance().logFailure(descriptor, ex, "Execution failed");

        assertEquals(EvoUiActionDescriptor.ActionStatus.ACTION_FAILED, descriptor.getStatus());
        assertEquals(ex, descriptor.getException());
    }

    @Test
    public void testTooltipFormatting() {
        EvoUiActionDescriptor descriptor = new EvoUiActionDescriptor("btn_export", "Export Model", "ForgePage", "export_model")
            .setTargetPath("/path/to/target")
            .addParameter("format", "GGUF");

        descriptor.setStatus(EvoUiActionDescriptor.ActionStatus.NOT_STARTED);
        String tooltipNotStarted = EvoTooltipManager.getInstance().formatTooltip(descriptor);
        assertTrue(tooltipNotStarted.contains("Export Model"));
        assertTrue(tooltipNotStarted.contains("Target: /path/to/target"));
        assertTrue(tooltipNotStarted.contains("Status: Not started"));

        descriptor.setStatus(EvoUiActionDescriptor.ActionStatus.ACTION_SUCCESS);
        descriptor.setResultSummary("Exported 100MB");
        String tooltipSuccess = EvoTooltipManager.getInstance().formatTooltip(descriptor);
        assertTrue(tooltipSuccess.contains("Status: Completed successfully"));
        assertTrue(tooltipSuccess.contains("Result: Exported 100MB"));
    }
}
