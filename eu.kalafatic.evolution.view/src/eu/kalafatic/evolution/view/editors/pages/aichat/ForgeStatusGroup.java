package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager.TrainingTelemetry;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * Forge training control and status group shown on the AI Commander page.
 * All displayed values come from the authoritative {@link ForgeSessionManager}
 * state; actions invoke the real backend (REST forging endpoints and the
 * session manager control methods). Nothing is simulated.
 */
public class ForgeStatusGroup extends AEvoGroup {
    private Label sessionLabel;
    private Label statusLabel;
    private Label metricsLabel;
    private ProgressBar progressBar;
    private Button startBtn;
    private Button pauseBtn;
    private Button resumeBtn;
    private Button stopBtn;
    private Button forgePageBtn;
    private Button forgeSettingsBtn;

    private final Runnable openForgeSettings;

    public ForgeStatusGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator,
                            Runnable openForgeSettings) {
        super(editor, orchestrator);
        this.openForgeSettings = openForgeSettings;
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
        updateForgeStatus();
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Forge (Local LLM Training)", 4, false);

        GUIFactory.INSTANCE.createLabel(group, "Session:");
        sessionLabel = new Label(group, SWT.NONE);
        sessionLabel.setText("No forge session");
        sessionLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 3, 1));

        GUIFactory.INSTANCE.createLabel(group, "Status:");
        statusLabel = new Label(group, SWT.NONE);
        statusLabel.setText("IDLE");
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        progressBar = new ProgressBar(group, SWT.HORIZONTAL);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));

        metricsLabel = new Label(group, SWT.NONE);
        metricsLabel.setText("");
        metricsLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 4, 1));

        startBtn = GUIFactory.INSTANCE.createButton(group, "Start Training");
        startBtn.addListener(SWT.Selection, e -> startOrResume());
        pauseBtn = GUIFactory.INSTANCE.createButton(group, "Pause");
        pauseBtn.addListener(SWT.Selection, e -> {
            String sid = resolveForgeSessionId();
            if (sid != null) ForgeSessionManager.getInstance().requestPause(sid);
        });
        resumeBtn = GUIFactory.INSTANCE.createButton(group, "Resume");
        resumeBtn.addListener(SWT.Selection, e -> startOrResume());
        stopBtn = GUIFactory.INSTANCE.createButton(group, "Stop");
        stopBtn.addListener(SWT.Selection, e -> {
            String sid = resolveForgeSessionId();
            if (sid != null) ForgeSessionManager.getInstance().requestFinish(sid);
        });
        forgePageBtn = GUIFactory.INSTANCE.createButton(group, "Open Forge Page");
        forgePageBtn.addListener(SWT.Selection, e -> {
            if (editor != null) {
                editor.showForgePage();
            }
        });
        forgeSettingsBtn = GUIFactory.INSTANCE.createButton(group, "Forge Settings");
        forgeSettingsBtn.addListener(SWT.Selection, e -> {
            if (openForgeSettings != null) openForgeSettings.run();
        });

        updateForgeStatus();
    }

    private String resolveForgeSessionId() {
        // Single source of truth: the authoritative ForgeSessionManager resolution
        // (active training → non-IDLE telemetry → default session).
        return ForgeSessionManager.getInstance().getCurrentSessionId();
    }

    private void startOrResume() {
        String sid = resolveForgeSessionId();
        if (sid == null) return;
        int port = 28080;
        if (orchestrator != null && orchestrator.getServerSettings() != null && orchestrator.getServerSettings().getPort() > 0) {
            port = orchestrator.getServerSettings().getPort();
        }
        final int serverPort = port;
        final String endpoint = "http://localhost:" + serverPort + "/forge/session/" + sid + "/forging/start";
        new Thread(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .timeout(Duration.ofSeconds(15))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                eu.kalafatic.evolution.view.HTTPUtils.requireEvolutionJson(endpoint,
                        response.statusCode(),
                        response.headers().firstValue("content-type").orElse(null));
            } catch (Exception ex) {
                System.err.println("[FORGE] Could not start forging via REST: " + ex.getMessage());
                Display.getDefault().asyncExec(() -> {
                    if (group != null && !group.isDisposed()) {
                        org.eclipse.jface.dialogs.MessageDialog.openError(group.getShell(),
                                "Forge backend unavailable",
                                "Could not start forging via the EVO backend:\n\n" + ex.getMessage());
                    }
                });
            }
        }, "forge-status-group-start").start();
    }

    /**
     * Reads the authoritative ForgeSessionManager state and updates all widgets.
     * Must be called on the SWT UI thread (the AiCommanderPage timer marshals
     * through Display.asyncExec).
     */
    public void updateForgeStatus() {
        if (group == null || group.isDisposed()) return;

        String sid = resolveForgeSessionId();
        ForgeSessionManager mgr = ForgeSessionManager.getInstance();
        TrainingTelemetry telemetry = sid != null ? mgr.getTelemetry(sid) : null;
        ForgeSession session = sid != null ? mgr.findSessionById(sid) : null;

        if (session != null) {
            String name = session.getName() != null ? session.getName() : "";
            setTextSafe(sessionLabel, name + " (" + shortId(session.getSessionId()) + ")");
        } else {
            setTextSafe(sessionLabel, "No forge session");
        }

        String status = telemetry != null && telemetry.status != null ? telemetry.status : "IDLE";
        String workflow = sid != null ? mgr.getUiState(sid).optString("forge.workflow.status", "") : "";
        String statusText = status;
        if (workflow != null && !workflow.isEmpty()) {
            statusText = status + " · " + workflow;
        }
        setTextSafe(statusLabel, statusText);

        int progress = 0;
        if (telemetry != null) {
            if (telemetry.totalSteps > 0) {
                progress = (int) Math.min(100, Math.max(0, (100.0 * telemetry.currentStep / telemetry.totalSteps)));
            } else if (telemetry.totalTokens > 0) {
                progress = (int) Math.min(100, Math.max(0, (100.0 * telemetry.processedTokens / telemetry.totalTokens)));
            }
        }
        if (!progressBar.isDisposed() && progressBar.getSelection() != progress) {
            progressBar.setSelection(progress);
        }

        StringBuilder metrics = new StringBuilder();
        if (telemetry != null) {
            metrics.append("Epoch ").append(telemetry.currentEpoch).append('/').append(telemetry.totalEpochs);
            metrics.append(" · Step ").append(telemetry.currentStep).append('/').append(telemetry.totalSteps);
            if (telemetry.currentLoss > 0) {
                metrics.append(" · Loss ").append(String.format("%.4f", telemetry.currentLoss));
            }
            if (telemetry.valLoss > 0) {
                metrics.append(" · Val ").append(String.format("%.4f", telemetry.valLoss));
            }
            if (telemetry.tokensPerSec > 0) {
                metrics.append(" · ").append(String.format("%.1f", telemetry.tokensPerSec)).append(" tok/s");
            }
        }
        setTextSafe(metricsLabel, metrics.toString());

        String upper = status.toUpperCase();
        boolean running = "RUNNING".equals(upper) || "TRAINING".equals(upper);
        boolean paused = "PAUSED".equals(upper);
        boolean interrupted = "INTERRUPTED".equals(upper);

        setEnabledSafe(startBtn, !running && !paused);
        setEnabledSafe(pauseBtn, running);
        setEnabledSafe(resumeBtn, paused || interrupted);
        setEnabledSafe(stopBtn, running || paused);
        setEnabledSafe(forgePageBtn, true);
        setEnabledSafe(forgeSettingsBtn, true);
    }

    private String shortId(String id) {
        if (id == null) return "";
        return id.length() > 8 ? id.substring(0, 8) : id;
    }
}
