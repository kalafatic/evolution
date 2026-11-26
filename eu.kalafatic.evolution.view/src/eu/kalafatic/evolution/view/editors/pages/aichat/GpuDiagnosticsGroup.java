package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.util.function.Consumer;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.hardware.CudaDiagnosticReport;
import eu.kalafatic.evolution.controller.hardware.CudaDiagnosticService;
import eu.kalafatic.evolution.controller.hardware.ForgeGpuVerificationReport;
import eu.kalafatic.evolution.controller.hardware.ForgeGpuVerificationService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.settings.HardwareSettingsGroup;
import eu.kalafatic.evolution.view.monitor.HardwareMonitoringService;
import eu.kalafatic.evolution.view.monitor.HardwareSnapshot;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * Compact GPU diagnostic panel shared by the Forge page and the AI Commander
 * Forge panel. All values come from the centralized diagnostic services
 * ({@link CudaDiagnosticService}, {@link ForgeGpuVerificationService},
 * {@link HardwareMonitoringService}) — the same sources as the Settings
 * Hardware panel. If a test is already running elsewhere, its current status
 * is displayed and no duplicate test is launched.
 *
 * <p>Layout: a tight three-column information grid grouped into
 * device information, backend status and test results, followed by a single
 * left-aligned action row. The full textual report remains available through
 * "View Details".</p>
 */
public class GpuDiagnosticsGroup extends AEvoGroup {

    private Label gpuDevice;
    private Label cudaStatus;
    private Label policy;
    private Label selectedDevice;
    private Label actualBackend;
    private Label cudaTest;
    private Label forgeTest;

    private Button testCudaBtn;
    private Button testForgeBtn;
    private Button detailsBtn;

    private final Consumer<CudaDiagnosticReport> cudaListener = r ->
            Display.getDefault().asyncExec(this::updateUI);
    private final Consumer<ForgeGpuVerificationReport> forgeListener = r ->
            Display.getDefault().asyncExec(this::updateUI);

    public GpuDiagnosticsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor,
                               Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);

        CudaDiagnosticService.getInstance().addListener(cudaListener);
        ForgeGpuVerificationService.getInstance().addListener(forgeListener);
        group.addListener(SWT.Dispose, e -> {
            CudaDiagnosticService.getInstance().removeListener(cudaListener);
            ForgeGpuVerificationService.getInstance().removeListener(forgeListener);
        });
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent,
                "GPU Diagnostics (CUDA · Forge Training)", 1, true);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite grid = new Composite(group, SWT.NONE);
        grid.setLayout(new GridLayout(3, true));
        grid.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Row 1 — device information
        Composite deviceCell = cell(toolkit, grid, "GPU DEVICE");
        gpuDevice = value(deviceCell);
        Composite selectedCell = cell(toolkit, grid, "SELECTED DEVICE");
        selectedDevice = value(selectedCell);
        Composite policyCell = cell(toolkit, grid, "FORGE DEVICE POLICY");
        policy = value(policyCell);

        // Row 2 — backend status
        Composite cudaCell = cell(toolkit, grid, "CUDA BACKEND");
        cudaStatus = value(cudaCell);
        Composite backendCell = cell(toolkit, grid, "ACTUAL LAST TRAINING BACKEND");
        actualBackend = value(backendCell);
        Composite forgeCell = cell(toolkit, grid, "LAST FORGE GPU TRAINING TEST");
        forgeTest = value(forgeCell);

        // Row 3 — test results (full width)
        Composite cudaTestCell = cell(toolkit, grid, "LAST CUDA BACKEND TEST");
        GridData cudaTestData = (GridData) cudaTestCell.getLayoutData();
        cudaTestData.horizontalSpan = 3;
        cudaTest = value(cudaTestCell);

        // Action row
        Composite actions = new Composite(group, SWT.NONE);
        GridLayout actionsLayout = new GridLayout(4, false);
        actionsLayout.marginWidth = 0;
        actionsLayout.marginHeight = 0;
        actionsLayout.horizontalSpacing = 6;
        actions.setLayout(actionsLayout);
        actions.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        testCudaBtn = GUIFactory.INSTANCE.createButton(actions, "Test CUDA Backend");
        testCudaBtn.addListener(SWT.Selection, e -> testCuda());
        testForgeBtn = GUIFactory.INSTANCE.createButton(actions, "Test Forge GPU Training");
        testForgeBtn.addListener(SWT.Selection, e -> testForge());
        detailsBtn = GUIFactory.INSTANCE.createButton(actions, "View Details");
        detailsBtn.addListener(SWT.Selection, e -> showDetails());
        Label spacer = new Label(actions, SWT.NONE);
        spacer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false));

        updateUI();
    }

    /** A caption/value cell: small muted caption over a wrapping value line. */
    private static Composite cell(FormToolkit toolkit, Composite parent, String captionText) {
        Composite cell = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 1;
        cell.setLayout(layout);
        cell.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.BEGINNING, true, false));
        Label caption = toolkit.createLabel(cell, captionText);
        caption.setFont(JFaceResources.getFontRegistry()
                .getBold(JFaceResources.DEFAULT_FONT));
        return cell;
    }

    private static Label value(Composite parent) {
        Label l = new Label(parent, SWT.WRAP);
        l.setText("—");
        l.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return l;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------
    private void testCuda() {
        CudaDiagnosticService service = CudaDiagnosticService.getInstance();
        if (!service.isRunning()) {
            service.startAsync();
        }
        updateUI();
    }

    private void testForge() {
        ForgeGpuVerificationService service = ForgeGpuVerificationService.getInstance();
        if (!service.isRunning()) {
            service.startAsync();
        }
        updateUI();
    }

    private void showDetails() {
        CudaDiagnosticReport cuda = CudaDiagnosticService.getInstance().getCurrent();
        ForgeGpuVerificationReport forge = ForgeGpuVerificationService.getInstance().getCurrent();
        StringBuilder sb = new StringBuilder();
        if (cuda != null) {
            sb.append(cuda.toText());
        } else {
            sb.append("No CUDA backend test executed yet.\n");
        }
        sb.append("\n\n");
        if (forge != null) {
            sb.append(forge.toText());
        } else {
            sb.append("No Forge GPU training test executed yet.\n");
        }
        HardwareSettingsGroup.openReportDialog(group.getShell(),
                "GPU Diagnostics — Details", sb.toString());
    }

    // ------------------------------------------------------------------
    // Refresh
    // ------------------------------------------------------------------
    @Override
    protected void refreshUI() {
        CudaDiagnosticService cuda = CudaDiagnosticService.getInstance();
        ForgeGpuVerificationService forge = ForgeGpuVerificationService.getInstance();

        // GPU device: from the CUDA report, else the live hardware snapshot.
        String gpuName = null;
        CudaDiagnosticReport cudaReport = cuda.getCurrent();
        if (cudaReport != null && cudaReport.gpuName != null && !cudaReport.gpuName.isEmpty()) {
            gpuName = cudaReport.gpuName;
        } else {
            HardwareSnapshot s = HardwareMonitoringService.getInstance().getLatest();
            if (s != null && s.gpuAvailable) {
                gpuName = s.gpuName;
            }
        }
        setTextSafe(gpuDevice, gpuName != null ? gpuName : "No NVIDIA GPU detected");

        if (cudaReport != null) {
            setTextSafe(cudaStatus, cudaReport.overallStatus
                    + (cudaReport.kernelExecuted() ? " — kernel executed, numerically verified" : ""));
        } else {
            setTextSafe(cudaStatus, "not tested");
        }

        setTextSafe(policy, forge.getCurrentPolicyLabel());

        ForgeGpuVerificationReport forgeReport = forge.getCurrent();
        if (forgeReport != null) {
            setTextSafe(selectedDevice, forgeReport.selectedDevice
                    + " (CUDA available: " + (forgeReport.cudaAvailable ? "YES" : "NO") + ")");
            setTextSafe(actualBackend,
                    forgeReport.actualBackend.isEmpty() ? "—" : forgeReport.actualBackend
                    + " [" + forgeReport.actualDevice + "]");
            setTextSafe(forgeTest, forgeReport.overallStatus
                    + " (" + forgeReport.totalDurationMs() + " ms"
                    + (forgeReport.blockedByActiveSession ? ", blocked by active session" : "") + ")");
        } else {
            setTextSafe(selectedDevice, "—");
            setTextSafe(actualBackend, "—");
            setTextSafe(forgeTest, "not executed");
        }

        if (cudaReport != null) {
            setTextSafe(cudaTest, cudaReport.overallStatus
                    + " (" + cudaReport.totalDurationMs() + " ms, " + cudaReport.finishedAt + ")");
        } else {
            setTextSafe(cudaTest, "not executed");
        }

        boolean anyRunning = cuda.isRunning() || forge.isRunning();
        setEnabledSafe(testCudaBtn, !anyRunning);
        setEnabledSafe(testForgeBtn, !anyRunning);
        setEnabledSafe(detailsBtn, cudaReport != null || forgeReport != null);
    }
}
