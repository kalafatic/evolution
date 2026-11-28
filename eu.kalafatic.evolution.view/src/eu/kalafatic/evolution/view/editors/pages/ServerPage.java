package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import eu.kalafatic.evolution.model.orchestration.MonitoringData;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.ServerSession;
import eu.kalafatic.evolution.model.orchestration.SessionType;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.server.*;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONObject;

public class ServerPage extends AEvoPage {

    private boolean isUpdating = false;
    /** Prevents overlapping /server/status polls and tracks the last diagnostic. */
    private final java.util.concurrent.atomic.AtomicBoolean statusPollInFlight =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    private volatile String lastStatusDiagnostic = "";
    private volatile long lastStatusDiagnosticAt = 0;

    private Color successColor;
    private Color colorWaiting;
    private Color lightGreen;
    private Font bannerFont;
    private org.eclipse.swt.widgets.Label modeIndicatorLabel;

    private SettingsGroup settingsGroup;
    private PortAllocationGroup portAllocationGroup;
    private SessionsGroup sessionsGroup;
    private ClientsGroup clientsGroup;
    private ResourcesGroup resourcesGroup;
    private MonitoringGroup monitoringGroup;
    private ServerMemoryContextGroup serverMemoryContextGroup;

    public ServerPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
       // initResources();
        createControl();
    }

//    protected void initResources() {
//        Display display = Display.getCurrent();
//        colorWaiting = new Color(display, 255, 140, 0); // Dark Orange
//        lightGreen = new Color(display, 220, 255, 220);
//
//        Font bannerDefault = JFaceResources.getBannerFont();
//        FontData[] bannerData = bannerDefault.getFontData();
//        for (FontData fd : bannerData) fd.setStyle(SWT.BOLD);
//        bannerFont = new Font(display, bannerData);
//    }

    private void createControl() {
        Composite comp = toolkit.createComposite(this);
        comp.setLayout(new GridLayout(1, false));

        successColor = new Color(getDisplay(), 200, 240, 200);

        modeIndicatorLabel = new org.eclipse.swt.widgets.Label(comp, SWT.CENTER);
        modeIndicatorLabel.setLayoutData(new org.eclipse.swt.layout.GridData(org.eclipse.swt.layout.GridData.FILL_HORIZONTAL));
        modeIndicatorLabel.setFont(bannerFont);
        updateBanner();

        settingsGroup = new SettingsGroup(toolkit, comp, editor, orchestrator, successColor, () -> Display.getDefault().asyncExec(() -> updateBanner()));
        portAllocationGroup = new PortAllocationGroup(toolkit, comp, editor, orchestrator);
        sessionsGroup = new SessionsGroup(toolkit, comp, editor, orchestrator, successColor);
        clientsGroup = new ClientsGroup(toolkit, comp, editor, orchestrator, successColor);
        resourcesGroup = new ResourcesGroup(toolkit, comp, editor, orchestrator, successColor);
        monitoringGroup = new MonitoringGroup(toolkit, comp, editor, orchestrator, successColor);
        serverMemoryContextGroup = new ServerMemoryContextGroup(toolkit, comp, editor, orchestrator, successColor);

        ModifyListener ml = e -> {
            if (orchestrator != null && !isUpdating) {
                updateModelFromFields();
                editor.setDirty(true);
            }
        };

        settingsGroup.addModifyListener(ml);

        startTimer();

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        updateUIFromModel();
    }

    @Override
    protected void refreshUI() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;
        settingsGroup.updateUI();
        if (portAllocationGroup != null) portAllocationGroup.refreshUI();
        sessionsGroup.updateUI();
        clientsGroup.updateUI();
        resourcesGroup.updateUI();
        monitoringGroup.updateUI();
        if (serverMemoryContextGroup != null) serverMemoryContextGroup.updateUI();
        isUpdating = false;
    }

    public void updateUIFromModel() {
        scheduleRefresh();
    }

    private void updateModelFromFields() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;
        settingsGroup.updateModel();
        isUpdating = false;
    }

    private void startTimer() {
        Display.getDefault().asyncExec(() -> {
            if (isDisposed()) return;
            Display.getDefault().timerExec(5000, new Runnable() {
                @Override
                public void run() {
                    Display.getDefault().asyncExec(() -> {
                        if (isDisposed()) return;
                        pollServerStatus();
                        Display.getDefault().timerExec(5000, this);
                    });
                }
            });
        });
    }

    private void pollServerStatus() {
        if (orchestrator == null || isDisposed()) return;
        if (!statusPollInFlight.compareAndSet(false, true)) return;

        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                eu.kalafatic.evolution.controller.resource.EvoService serverSvc =
                        eu.kalafatic.evolution.controller.resource.ResourceManager.getInstance().getService("SERVER");
                int port = serverSvc != null ? serverSvc.getPort() : 28080;
                eu.kalafatic.evolution.controller.orchestration.ServerEntry entry =
                        ServerManager.getInstance().getServerEntry(port);
                if (entry != null && entry.getState()
                        != eu.kalafatic.evolution.controller.orchestration.ServerState.RUNNING) {
                    Display.getDefault().asyncExec(() -> {
                        if (!isDisposed()) updateBanner();
                    });
                    return;
                }

                String serverStatusUrl = (serverSvc != null ? serverSvc.getUrl()
                        : "http://127.0.0.1:28080") + "/server/status";
                URL url = new URL(serverStatusUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                // Never abort the response mid-write: a short read timeout closes the
                // socket while NanoHTTPD is still streaming the gzipped body, which shows
                // up as SEVERE "Connection reset by peer"/"Broken pipe". The status
                // endpoint is cached server-side and answers immediately.
                conn.setReadTimeout(10000);
                conn.setRequestProperty("Accept", "application/json");

                int code = conn.getResponseCode();
                String contentType = conn.getContentType();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
                String result = "";
                if (is != null) {
                    try (Scanner s = new Scanner(is).useDelimiter("\\A")) {
                        result = s.hasNext() ? s.next() : "";
                    }
                }
                if (code < 200 || code >= 300) return;
                if (contentType == null || !contentType.toLowerCase().contains("json")) {
                    logStatusDiagnostic("Non-JSON /server/status response from " + serverStatusUrl
                            + " (Content-Type: " + contentType + "). Another application may be occupying the EVO port.");
                    return;
                }

                JSONObject json = new JSONObject(result);
                JSONObject monitoring = json.getJSONObject("monitoring");
                JSONArray sessions = json.getJSONArray("sessions");

                Display.getDefault().asyncExec(() -> {
                    if (isDisposed() || orchestrator == null) return;
                    updateBanner();

                    // Update Monitoring History
                    MonitoringData data = OrchestrationFactory.eINSTANCE.createMonitoringData();
                    data.setTimestamp(monitoring.getLong("timestamp"));
                    data.setCpuUsage(monitoring.getDouble("cpuUsage"));
                    data.setMemoryUsage(monitoring.getLong("memoryUsage"));
                    data.setTotalMemory(monitoring.getLong("totalMemory"));

                    orchestrator.getMonitoringHistory().add(data);
                    if (orchestrator.getMonitoringHistory().size() > 50) {
                        orchestrator.getMonitoringHistory().remove(0);
                    }

                    // Update Sessions
                    orchestrator.getServerSessions().clear();
                    for (int i = 0; i < sessions.length(); i++) {
                        JSONObject sj = sessions.getJSONObject(i);
                        ServerSession ss = OrchestrationFactory.eINSTANCE.createServerSession();
                        ss.setId(sj.getString("id"));
                        ss.setType(SessionType.getByName(sj.getString("type")));
                        ss.setStartTime(sj.getLong("startTime"));
                        ss.setLastActivity(sj.getLong("lastActivity"));
                        ss.setClientIp(sj.getString("clientIp"));
                        orchestrator.getServerSessions().add(ss);
                    }
                    updateUIFromModel();
                });
            } catch (Exception e) {
                // Server not running / not an EVO server: a poll failure is not an error.
            } finally {
                if (conn != null) conn.disconnect();
                statusPollInFlight.set(false);
            }
        }, "evo-server-status-poll").start();
    }

    /** Logs a connectivity diagnostic at most once per 30s to avoid poller spam. */
    private void logStatusDiagnostic(String message) {
        long now = System.currentTimeMillis();
        if (message.equals(lastStatusDiagnostic) && now - lastStatusDiagnosticAt < 30000) {
            return;
        }
        lastStatusDiagnostic = message;
        lastStatusDiagnosticAt = now;
        eu.kalafatic.evolution.controller.log.Log.log("[ServerPage] " + message);
    }

    @Override
    public void setOrchestrator(Orchestrator orchestrator) {
        super.setOrchestrator(orchestrator);
        if (settingsGroup != null) settingsGroup.setOrchestrator(orchestrator);
        if (portAllocationGroup != null) portAllocationGroup.setOrchestrator(orchestrator);
        if (sessionsGroup != null) sessionsGroup.setOrchestrator(orchestrator);
        if (clientsGroup != null) clientsGroup.setOrchestrator(orchestrator);
        if (resourcesGroup != null) resourcesGroup.setOrchestrator(orchestrator);
        if (monitoringGroup != null) monitoringGroup.setOrchestrator(orchestrator);
        if (serverMemoryContextGroup != null) serverMemoryContextGroup.setOrchestrator(orchestrator);
    }

    private void updateBanner() {
        if (modeIndicatorLabel == null || modeIndicatorLabel.isDisposed()) return;
        java.util.Map<Integer, Boolean> statuses = ServerManager.getInstance().getServerStatuses();
        
        if (statuses.isEmpty()) {
            setTextSafe(modeIndicatorLabel, "NO SERVERS RUNNING");
            setBackgroundSafe(modeIndicatorLabel, colorWaiting);
        } else {
            String text = statuses.entrySet().stream()
                .map(e -> "PORT " + e.getKey() + ": " + (e.getValue() ? "RUNNING" : "STOPPED"))
                .collect(java.util.stream.Collectors.joining(" | "));
            setTextSafe(modeIndicatorLabel, text);
            setBackgroundSafe(modeIndicatorLabel, lightGreen);
        }
    }

    @Override
    public void dispose() {
        if (successColor != null && !successColor.isDisposed()) successColor.dispose();
        if (colorWaiting != null && !colorWaiting.isDisposed()) colorWaiting.dispose();
        if (lightGreen != null && !lightGreen.isDisposed()) lightGreen.dispose();
        if (bannerFont != null && !bannerFont.isDisposed()) bannerFont.dispose();
        super.dispose();
    }
}
