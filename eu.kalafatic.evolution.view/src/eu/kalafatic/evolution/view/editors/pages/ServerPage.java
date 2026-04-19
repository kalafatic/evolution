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

public class ServerPage extends SharedScrolledComposite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private boolean isUpdating = false;

    private FormToolkit toolkit;
    private Color successColor;
    private Color colorWaiting;
    private Color lightGreen;
    private Font bannerFont;
    private org.eclipse.swt.widgets.Label modeIndicatorLabel;

    private SettingsGroup settingsGroup;
    private SessionsGroup sessionsGroup;
    private ClientsGroup clientsGroup;
    private ResourcesGroup resourcesGroup;
    private MonitoringGroup monitoringGroup;

    public ServerPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        this.toolkit = new FormToolkit(parent.getDisplay());
        initResources();
        createControl();
    }

    private void initResources() {
        Display display = Display.getCurrent();
        colorWaiting = new Color(display, 255, 140, 0); // Dark Orange
        lightGreen = new Color(display, 220, 255, 220);

        Font bannerDefault = JFaceResources.getBannerFont();
        FontData[] bannerData = bannerDefault.getFontData();
        for (FontData fd : bannerData) fd.setStyle(SWT.BOLD);
        bannerFont = new Font(display, bannerData);
    }

    private void createControl() {
        Composite comp = toolkit.createComposite(this);
        comp.setLayout(new GridLayout(1, false));

        successColor = new Color(getDisplay(), 200, 240, 200);

        modeIndicatorLabel = new org.eclipse.swt.widgets.Label(comp, SWT.CENTER);
        modeIndicatorLabel.setLayoutData(new org.eclipse.swt.layout.GridData(org.eclipse.swt.layout.GridData.FILL_HORIZONTAL));
        modeIndicatorLabel.setFont(bannerFont);
        updateBanner();

        settingsGroup = new SettingsGroup(toolkit, comp, editor, orchestrator, successColor, () -> Display.getDefault().asyncExec(() -> updateBanner()));
        sessionsGroup = new SessionsGroup(toolkit, comp, editor, orchestrator, successColor);
        clientsGroup = new ClientsGroup(toolkit, comp, editor, orchestrator, successColor);
        resourcesGroup = new ResourcesGroup(toolkit, comp, editor, orchestrator, successColor);
        monitoringGroup = new MonitoringGroup(toolkit, comp, editor, orchestrator, successColor);

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

    public void updateUIFromModel() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;
        settingsGroup.updateUI();
        sessionsGroup.updateUI();
        clientsGroup.updateUI();
        resourcesGroup.updateUI();
        monitoringGroup.updateUI();
        isUpdating = false;
    }

    private void updateModelFromFields() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;
        settingsGroup.updateModel();
        isUpdating = false;
    }

    private void startTimer() {
        Display.getDefault().timerExec(5000, new Runnable() {
            @Override
            public void run() {
                if (isDisposed()) return;
                pollServerStatus();
                Display.getDefault().timerExec(5000, this);
            }
        });
    }

    private void pollServerStatus() {
        if (orchestrator == null) return;
        new Thread(() -> {
            try {
                int port = orchestrator.getServerSettings() != null ? orchestrator.getServerSettings().getPort() : 48080;
                URL url = new URL("http://localhost:" + port + "/server/status");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                if (conn.getResponseCode() == 200) {
                    InputStream is = conn.getInputStream();
                    Scanner s = new Scanner(is).useDelimiter("\\A");
                    String result = s.hasNext() ? s.next() : "";

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
                }
            } catch (Exception e) {
                // Server probably not running or port mismatch
            }
        }).start();
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        if (settingsGroup != null) settingsGroup.setOrchestrator(orchestrator);
        if (sessionsGroup != null) sessionsGroup.setOrchestrator(orchestrator);
        if (clientsGroup != null) clientsGroup.setOrchestrator(orchestrator);
        if (resourcesGroup != null) resourcesGroup.setOrchestrator(orchestrator);
        if (monitoringGroup != null) monitoringGroup.setOrchestrator(orchestrator);
        updateUIFromModel();
    }

    private void updateBanner() {
        if (modeIndicatorLabel == null || modeIndicatorLabel.isDisposed()) return;
        boolean running = ServerManager.getInstance().isRunning();
        int port = ServerManager.getInstance().getPort();
        if (running) {
            modeIndicatorLabel.setText("SERVER RUNNING ON PORT " + port);
            modeIndicatorLabel.setBackground(lightGreen);
        } else {
            modeIndicatorLabel.setText("SERVER STOPPED");
            modeIndicatorLabel.setBackground(colorWaiting);
        }
    }

    @Override
    public void dispose() {
        if (successColor != null && !successColor.isDisposed()) successColor.dispose();
        if (colorWaiting != null && !colorWaiting.isDisposed()) colorWaiting.dispose();
        if (lightGreen != null && !lightGreen.isDisposed()) lightGreen.dispose();
        if (bannerFont != null && !bannerFont.isDisposed()) bannerFont.dispose();
        if (toolkit != null) toolkit.dispose();
        super.dispose();
    }
}
