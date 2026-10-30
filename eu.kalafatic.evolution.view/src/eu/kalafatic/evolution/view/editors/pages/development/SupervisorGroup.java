package eu.kalafatic.evolution.view.editors.pages.development;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBootstrapController;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SupervisorClient;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SupervisorSettings;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.constants.FUIConstants;
import eu.kalafatic.utils.factories.GUIFactory;

public class SupervisorGroup extends AEvoGroup {

    private Text executablePathText;
    private Text sourcePathText;
    private Text commandsText;
    private Text settingsText;
    private Button deployedCheck;

    // Additional Properties
    private Text gitRepoUrlText;
    private Text gitLocalPathText;
    private Text gitBranchText;
    private Text portText;
    private Text controlPortText;
    private Button debugCheck;

    // Telemetry & Status Widgets
    private Label statusBadgeLabel;
    private Label pidLabel;
    private Label baseUrlLabel;
    private Label workDirLabel;
    private Label logPathLabel;

    // Controls
    private Button startBtn;
    private Button stopBtn;
    private Button pingBtn;
    private Button buildDeployBtn;
    private Button openWebHubBtn;
    private Button viewLogBtn;

    private Color lightGreenColor;
    private Color lightRedColor;

    public SupervisorGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        this.lightGreenColor = new Color(Display.getDefault(), 220, 245, 220);
        this.lightRedColor = new Color(Display.getDefault(), 252, 232, 232);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Supervisor Configuration & Control Center", 1, false);

        // Sub-panel 1: Status & Telemetry Summary
        Composite statusComp = toolkit.createComposite(group);
        statusComp.setLayout(new GridLayout(4, false));
        statusComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        toolkit.createLabel(statusComp, "Status: ");
        statusBadgeLabel = toolkit.createLabel(statusComp, " IDLE ");
        statusBadgeLabel.setFont(org.eclipse.jface.resource.JFaceResources.getFontRegistry().getBold(org.eclipse.jface.resource.JFaceResources.DEFAULT_FONT));
        statusBadgeLabel.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_TRANSPARENT));

        toolkit.createLabel(statusComp, "PID: ");
        pidLabel = toolkit.createLabel(statusComp, "N/A");

        toolkit.createLabel(statusComp, "Endpoint: ");
        baseUrlLabel = toolkit.createLabel(statusComp, "http://127.0.0.1:8089");
        baseUrlLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        toolkit.createLabel(statusComp, "Working Dir: ");
        workDirLabel = toolkit.createLabel(statusComp, "N/A");
        workDirLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        toolkit.createLabel(statusComp, "Log File: ");
        logPathLabel = toolkit.createLabel(statusComp, "N/A");
        logPathLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        // Sub-panel 2: Action Controls Toolbar
        Composite toolbarComp = toolkit.createComposite(group);
        toolbarComp.setLayout(new GridLayout(6, false));
        toolbarComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        startBtn = GUIFactory.INSTANCE.createButton(toolbarComp, "▶ Start Supervisor");
        stopBtn = GUIFactory.INSTANCE.createButton(toolbarComp, "■ Stop Supervisor");
        pingBtn = GUIFactory.INSTANCE.createButton(toolbarComp, "📡 Ping Health");
        buildDeployBtn = GUIFactory.INSTANCE.createButton(toolbarComp, "🛠️ Build & Deploy");
        openWebHubBtn = GUIFactory.INSTANCE.createButton(toolbarComp, "🌐 Open Web Hub");
        viewLogBtn = GUIFactory.INSTANCE.createButton(toolbarComp, "📜 View Logs");

        startBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleStartSupervisor();
            }
        });

        stopBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleStopSupervisor();
            }
        });

        pingBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handlePingHealth();
            }
        });

        buildDeployBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleBuildAndDeploy();
            }
        });

        openWebHubBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleOpenWebHub();
            }
        });

        viewLogBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleViewLogs();
            }
        });

        // Sub-panel 3: All Supervisor Properties Grid
        Composite propsComp = toolkit.createComposite(group);
        propsComp.setLayout(new GridLayout(3, false));
        propsComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(propsComp, "Executable Path:");
        executablePathText = GUIFactory.INSTANCE.createText(propsComp);
        GUIFactory.INSTANCE.createEditButton(propsComp, executablePathText);

        GUIFactory.INSTANCE.createLabel(propsComp, "Source Path:");
        sourcePathText = GUIFactory.INSTANCE.createText(propsComp);
        GUIFactory.INSTANCE.createEditButton(propsComp, sourcePathText);

        GUIFactory.INSTANCE.createLabel(propsComp, "Commands:");
        commandsText = GUIFactory.INSTANCE.createText(propsComp);
        new Label(propsComp, SWT.NONE);

        GUIFactory.INSTANCE.createLabel(propsComp, "Settings:");
        settingsText = GUIFactory.INSTANCE.createText(propsComp);
        new Label(propsComp, SWT.NONE);

        GUIFactory.INSTANCE.createLabel(propsComp, "Git Repository URL:");
        gitRepoUrlText = GUIFactory.INSTANCE.createText(propsComp);
        new Label(propsComp, SWT.NONE);

        GUIFactory.INSTANCE.createLabel(propsComp, "Git Local Path:");
        gitLocalPathText = GUIFactory.INSTANCE.createText(propsComp);
        GUIFactory.INSTANCE.createEditButton(propsComp, gitLocalPathText);

        GUIFactory.INSTANCE.createLabel(propsComp, "Git Branch:");
        gitBranchText = GUIFactory.INSTANCE.createText(propsComp);
        new Label(propsComp, SWT.NONE);

        GUIFactory.INSTANCE.createLabel(propsComp, "Supervisor API Port:");
        portText = GUIFactory.INSTANCE.createText(propsComp);
        new Label(propsComp, SWT.NONE);

        GUIFactory.INSTANCE.createLabel(propsComp, "Web Control Port:");
        controlPortText = GUIFactory.INSTANCE.createText(propsComp);
        new Label(propsComp, SWT.NONE);

        GUIFactory.INSTANCE.createLabel(propsComp, "Deployed State:");
        deployedCheck = toolkit.createButton(propsComp, "Deployed", SWT.CHECK);
        deployedCheck.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        GUIFactory.INSTANCE.createLabel(propsComp, "Debug / RCP Mode:");
        debugCheck = toolkit.createButton(propsComp, "Enable Debug Loop (--debug / --ui)", SWT.CHECK);
        debugCheck.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        registerControlAction(executablePathText, "executable_path", "Executable Path", "update_supervisor_executable", "setExecutablePath", null);
        registerControlAction(sourcePathText, "source_path", "Source Path", "update_supervisor_source", "setSourcePath", null);
        registerControlAction(commandsText, "commands", "Commands", "update_supervisor_commands", "setCommands", null);
        registerControlAction(settingsText, "settings", "Settings", "update_supervisor_settings", "setSettings", null);
        registerControlAction(gitRepoUrlText, "git_repo_url", "Git Repository URL", "update_supervisor_git_url", "setRepositoryUrl", null);
        registerControlAction(gitLocalPathText, "git_local_path", "Git Local Path", "update_supervisor_git_local", "setLocalPath", null);
        registerControlAction(gitBranchText, "git_branch", "Git Branch", "update_supervisor_git_branch", "setBranch", null);
        registerControlAction(portText, "port", "API Port", "update_supervisor_port", "setSettings", null);
        registerControlAction(controlPortText, "control_port", "Control Port", "update_supervisor_control_port", "setSettings", null);
        registerControlAction(deployedCheck, "deployed_check", "Deployed Checkbox", "toggle_supervisor_deployed", "setDeployed", null);
        registerControlAction(debugCheck, "debug_check", "Debug Checkbox", "toggle_supervisor_debug", "setDebugMode", null);

        load();

        ModifyListener ml = e -> {
            updateModel();
            editor.setDirty(true);
        };
        executablePathText.addModifyListener(ml);
        sourcePathText.addModifyListener(ml);
        commandsText.addModifyListener(ml);
        settingsText.addModifyListener(ml);
        gitRepoUrlText.addModifyListener(ml);
        gitLocalPathText.addModifyListener(ml);
        gitBranchText.addModifyListener(ml);
        portText.addModifyListener(ml);
        controlPortText.addModifyListener(ml);

        SelectionAdapter sa = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateModel();
                editor.setDirty(true);
            }
        };
        deployedCheck.addSelectionListener(sa);
        debugCheck.addSelectionListener(sa);
    }

    public void load() {
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            SupervisorSettings settings = orchestrator.getSupervisorSettings();
            setTextSafe(executablePathText, settings.getExecutablePath());
            setTextSafe(sourcePathText, settings.getSourcePath());
            setTextSafe(commandsText, settings.getCommands());
            setTextSafe(settingsText, settings.getSettings());
            setSelectionSafe(deployedCheck, settings.isDeployed());

            if (settings.getGit() != null) {
                Git git = settings.getGit();
                setTextSafe(gitRepoUrlText, git.getRepositoryUrl());
                setTextSafe(gitLocalPathText, git.getLocalPath());
                setTextSafe(gitBranchText, git.getBranch());
            }

            int port = extractSettingValue(settings.getSettings(), "port", 8089);
            int controlPort = extractSettingValue(settings.getSettings(), "controlPort", 28080);
            boolean debug = extractSettingBoolean(settings.getSettings(), "debug", false);

            setTextSafe(portText, String.valueOf(port));
            setTextSafe(controlPortText, String.valueOf(controlPort));
            setSelectionSafe(debugCheck, debug);

            baseUrlLabel.setText("http://127.0.0.1:" + port);
        }
    }

    public static int extractSettingValue(String rawSettings, String key, int defaultVal) {
        if (rawSettings == null || rawSettings.isEmpty()) return defaultVal;
        try {
            Pattern pattern = Pattern.compile("(?:^|[;,])\\s*" + Pattern.quote(key) + "\\s*=\\s*([^;,]+)");
            Matcher matcher = pattern.matcher(rawSettings);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1).trim());
            }
        } catch (Exception ignored) {}
        return defaultVal;
    }

    public static boolean extractSettingBoolean(String rawSettings, String key, boolean defaultVal) {
        if (rawSettings == null || rawSettings.isEmpty()) return defaultVal;
        try {
            Pattern pattern = Pattern.compile("(?:^|[;,])\\s*" + Pattern.quote(key) + "\\s*=\\s*([^;,]+)");
            Matcher matcher = pattern.matcher(rawSettings);
            if (matcher.find()) {
                return Boolean.parseBoolean(matcher.group(1).trim());
            }
        } catch (Exception ignored) {}
        return defaultVal;
    }

    @Override
    public void refreshUI() {
        load();
        checkProcessTelemetry();
    }

    private void checkProcessTelemetry() {
        int port = parsePort(portText.getText(), 8089);
        Job job = new Job("Supervisor Telemetry Ping") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                SupervisorClient client = new SupervisorClient("http://127.0.0.1:" + port);
                boolean pingOk = client.ping();

                Display.getDefault().asyncExec(() -> {
                    if (statusBadgeLabel == null || statusBadgeLabel.isDisposed()) return;
                    if (pingOk) {
                        statusBadgeLabel.setText(" RUNNING ");
                        statusBadgeLabel.setBackground(lightGreenColor);
                    } else {
                        statusBadgeLabel.setText(" IDLE / STOPPED ");
                        statusBadgeLabel.setBackground(FUIConstants.GRADIENT);
                    }
                });
                return Status.OK_STATUS;
            }
        };
        job.setSystem(true);
        job.schedule();
    }

    private int parsePort(String text, int defaultPort) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception e) {
            return defaultPort;
        }
    }

    public void updateModel() {
        if (orchestrator == null) return;
        if (orchestrator.getSupervisorSettings() == null) {
            orchestrator.setSupervisorSettings(OrchestrationFactory.eINSTANCE.createSupervisorSettings());
        }
        SupervisorSettings settings = orchestrator.getSupervisorSettings();
        settings.setExecutablePath(executablePathText.getText());
        settings.setSourcePath(sourcePathText.getText());
        settings.setCommands(commandsText.getText());

        int port = parsePort(portText.getText(), 8089);
        int controlPort = parsePort(controlPortText.getText(), 28080);
        boolean debug = debugCheck.getSelection();

        String combinedSettings = "port=" + port + ";controlPort=" + controlPort + ";debug=" + debug + ";custom=" + settingsText.getText();
        settings.setSettings(combinedSettings);
        settings.setDeployed(deployedCheck.getSelection());

        if (settings.getGit() == null) {
            settings.setGit(OrchestrationFactory.eINSTANCE.createGit());
        }
        Git git = settings.getGit();
        git.setRepositoryUrl(gitRepoUrlText.getText());
        git.setLocalPath(gitLocalPathText.getText());
        git.setBranch(gitBranchText.getText());
    }

    private void handleStartSupervisor() {
        updateModel();
        Job job = new Job("Start Supervisor Process") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Starting External Supervisor...", 100);
                File projectRoot = resolveProjectRoot();
                SelfDevBootstrapController controller = new SelfDevBootstrapController(projectRoot, orchestrator);
                controller.setDebugMode(debugCheck.getSelection());
                controller.startBootstrap();
                monitor.worked(100);
                Display.getDefault().asyncExec(() -> refreshUI());
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void handleStopSupervisor() {
        Job job = new Job("Stop Supervisor Process") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Stopping External Supervisor...", 100);
                File projectRoot = resolveProjectRoot();
                SelfDevBootstrapController controller = new SelfDevBootstrapController(projectRoot, orchestrator);
                controller.stopBootstrap();
                monitor.worked(100);
                Display.getDefault().asyncExec(() -> refreshUI());
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void handlePingHealth() {
        int port = parsePort(portText.getText(), 8089);
        Job job = new Job("Supervisor Health Check") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                SupervisorClient client = new SupervisorClient("http://127.0.0.1:" + port);
                boolean pingOk = client.ping();
                Display.getDefault().asyncExec(() -> {
                    if (pingOk) {
                        MessageDialog.openInformation(group.getShell(), "Supervisor Health Ping", "Supervisor service is active and responding on http://127.0.0.1:" + port);
                    } else {
                        MessageDialog.openWarning(group.getShell(), "Supervisor Health Ping", "Supervisor service is not responding on http://127.0.0.1:" + port);
                    }
                    refreshUI();
                });
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private void handleBuildAndDeploy() {
        updateModel();
        Job job = new Job("Build and Deploy Supervisor") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                monitor.beginTask("Building and Deploying Supervisor...", 100);
                File projectRoot = resolveProjectRoot();
                SelfDevBootstrapController controller = new SelfDevBootstrapController(projectRoot, orchestrator);
                TaskResult res = controller.runTask("BUILD_SUPERVISOR_LOCAL");
                monitor.worked(100);
                Display.getDefault().asyncExec(() -> {
                    if (res.isSuccess()) {
                        MessageDialog.openInformation(group.getShell(), "Build Supervisor Success", "Supervisor built and deployed successfully:\n" + res.getMessage());
                    } else {
                        MessageDialog.openError(group.getShell(), "Build Supervisor Failed", "Supervisor build failed:\n" + res.getMessage());
                    }
                    refreshUI();
                });
                return res.isSuccess() ? Status.OK_STATUS : Status.CANCEL_STATUS;
            }
        };
        job.schedule();
    }

    private void handleOpenWebHub() {
        int controlPort = parsePort(controlPortText.getText(), 28080);
        String url = "http://127.0.0.1:" + controlPort;
        try {
            Program.launch(url);
        } catch (Exception e) {
            MessageDialog.openError(group.getShell(), "Launch Control Hub Failed", "Could not open browser for " + url + ": " + e.getMessage());
        }
    }

    private void handleViewLogs() {
        File projectRoot = resolveProjectRoot();
        File logFile = new File(projectRoot, "self-dev/run/logs/supervisor_runtime.log");
        if (!logFile.exists()) {
            logFile = new File(System.getProperty("user.home"), "git/evolution/self-dev/run/logs/supervisor_runtime.log");
        }

        String content = "No supervisor log file found at " + logFile.getAbsolutePath();
        if (logFile.exists()) {
            try {
                List<String> lines = java.nio.file.Files.readAllLines(logFile.toPath());
                int total = lines.size();
                int start = Math.max(0, total - 100);
                StringBuilder sb = new StringBuilder();
                for (int i = start; i < total; i++) {
                    sb.append(lines.get(i)).append("\n");
                }
                content = sb.toString();
            } catch (Exception e) {
                content = "Error reading log file: " + e.getMessage();
            }
        }

        final String finalContent = content;
        org.eclipse.jface.dialogs.Dialog dialog = new org.eclipse.jface.dialogs.Dialog(group.getShell()) {
            @Override
            protected void configureShell(org.eclipse.swt.widgets.Shell newShell) {
                super.configureShell(newShell);
                newShell.setText("Supervisor Process Runtime Logs");
                newShell.setSize(800, 500);
            }

            @Override
            protected org.eclipse.swt.widgets.Control createDialogArea(Composite parent) {
                Composite area = (Composite) super.createDialogArea(parent);
                area.setLayout(new GridLayout(1, false));
                Text text = new Text(area, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
                text.setLayoutData(new GridData(GridData.FILL_BOTH));
                text.setFont(org.eclipse.jface.resource.JFaceResources.getTextFont());
                text.setText(finalContent);
                return area;
            }
        };
        dialog.open();
    }

    private File resolveProjectRoot() {
        if (orchestrator != null && orchestrator.eResource() != null) {
            org.eclipse.emf.common.util.URI uri = orchestrator.eResource().getURI();
            if (uri.isFile()) {
                return new File(uri.toFileString()).getParentFile();
            }
        }
        return new File(System.getProperty("user.dir"));
    }

    @Override
    public void dispose() {
        if (lightGreenColor != null) lightGreenColor.dispose();
        if (lightRedColor != null) lightRedColor.dispose();
        super.dispose();
    }
}
