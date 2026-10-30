package eu.kalafatic.evolution.view.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.SupervisorSettings;
import eu.kalafatic.evolution.view.editors.pages.development.SupervisorGroup;
import eu.kalafatic.utils.factories.GUIFactory;

public class SupervisorSettingsPage extends AWizardPage {
    private Text executablePathText;
    private Text sourcePathText;
    private Text commandsText;
    private Text settingsText;
    private Text gitRepoUrlText;
    private Text gitLocalPathText;
    private Text gitBranchText;
    private Text portText;
    private Text controlPortText;
    private Button deployedCheck;
    private Button debugCheck;
    private boolean isSkipped = false;

    public SupervisorSettingsPage() {
        super("SupervisorSettingsPage");
        setTitle("Supervisor Settings");
        setDescription("Set external loop controller settings, network ports, and Git repository options for self-development.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));

        SupervisorSettings supervisor = orchestrator != null ? orchestrator.getSupervisorSettings() : null;
        Git git = supervisor != null ? supervisor.getGit() : null;

        Label label = new Label(container, SWT.NONE);
        label.setText("Executable Path:");
        executablePathText = new Text(container, SWT.BORDER);
        executablePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (supervisor != null && supervisor.getExecutablePath() != null) {
            executablePathText.setText(supervisor.getExecutablePath());
        }
        GUIFactory.INSTANCE.createEditButton(container, executablePathText);

        label = new Label(container, SWT.NONE);
        label.setText("Source Path:");
        sourcePathText = new Text(container, SWT.BORDER);
        sourcePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (supervisor != null && supervisor.getSourcePath() != null) {
            sourcePathText.setText(supervisor.getSourcePath());
        }
        GUIFactory.INSTANCE.createEditButton(container, sourcePathText);

        label = new Label(container, SWT.NONE);
        label.setText("Commands:");
        commandsText = new Text(container, SWT.BORDER);
        commandsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (supervisor != null && supervisor.getCommands() != null) {
            commandsText.setText(supervisor.getCommands());
        }
        new Label(container, SWT.NONE);

        label = new Label(container, SWT.NONE);
        label.setText("Settings:");
        settingsText = new Text(container, SWT.BORDER);
        settingsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (supervisor != null && supervisor.getSettings() != null) {
            settingsText.setText(supervisor.getSettings());
        }
        new Label(container, SWT.NONE);

        label = new Label(container, SWT.NONE);
        label.setText("Git Repository URL:");
        gitRepoUrlText = new Text(container, SWT.BORDER);
        gitRepoUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (git != null && git.getRepositoryUrl() != null) {
            gitRepoUrlText.setText(git.getRepositoryUrl());
        }
        new Label(container, SWT.NONE);

        label = new Label(container, SWT.NONE);
        label.setText("Git Local Path:");
        gitLocalPathText = new Text(container, SWT.BORDER);
        gitLocalPathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (git != null && git.getLocalPath() != null) {
            gitLocalPathText.setText(git.getLocalPath());
        }
        GUIFactory.INSTANCE.createEditButton(container, gitLocalPathText);

        label = new Label(container, SWT.NONE);
        label.setText("Git Branch:");
        gitBranchText = new Text(container, SWT.BORDER);
        gitBranchText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (git != null && git.getBranch() != null) {
            gitBranchText.setText(git.getBranch());
        }
        new Label(container, SWT.NONE);

        int initPort = supervisor != null ? SupervisorGroup.extractSettingValue(supervisor.getSettings(), "port", 8089) : 8089;
        int initControlPort = supervisor != null ? SupervisorGroup.extractSettingValue(supervisor.getSettings(), "controlPort", 28080) : 28080;
        boolean initDebug = supervisor != null ? SupervisorGroup.extractSettingBoolean(supervisor.getSettings(), "debug", false) : false;

        label = new Label(container, SWT.NONE);
        label.setText("Supervisor API Port:");
        portText = new Text(container, SWT.BORDER);
        portText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        portText.setText(String.valueOf(initPort));
        new Label(container, SWT.NONE);

        label = new Label(container, SWT.NONE);
        label.setText("Web Control Port:");
        controlPortText = new Text(container, SWT.BORDER);
        controlPortText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        controlPortText.setText(String.valueOf(initControlPort));
        new Label(container, SWT.NONE);

        label = new Label(container, SWT.NONE);
        label.setText("Deployed:");
        deployedCheck = new Button(container, SWT.CHECK);
        deployedCheck.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        if (supervisor != null) {
            deployedCheck.setSelection(supervisor.isDeployed());
        }

        label = new Label(container, SWT.NONE);
        label.setText("Debug Mode:");
        debugCheck = new Button(container, SWT.CHECK);
        debugCheck.setText("Enable Debug & UI RCP Mode");
        debugCheck.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));
        debugCheck.setSelection(initDebug);

        new Label(container, SWT.NONE);
        Composite actionComp = new Composite(container, SWT.NONE);
        actionComp.setLayout(new GridLayout(2, false));
        actionComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Button deploySourcesBtn = new Button(actionComp, SWT.PUSH);
        deploySourcesBtn.setText("Deploy Sources");
        deploySourcesBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Deploy", "Deploying sources to: " + sourcePathText.getText());
            }
        });

        Button deployNewVersionBtn = new Button(actionComp, SWT.PUSH);
        deployNewVersionBtn.setText("Deploy New Version");
        deployNewVersionBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Deploy", "Deploying new version using: " + executablePathText.getText());
            }
        });

        Button skipBtn = new Button(container, SWT.CHECK);
        skipBtn.setText("Skip Supervisor Setup");
        skipBtn.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 3, 1));
        skipBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                isSkipped = skipBtn.getSelection();
                executablePathText.setEnabled(!isSkipped);
                sourcePathText.setEnabled(!isSkipped);
                commandsText.setEnabled(!isSkipped);
                settingsText.setEnabled(!isSkipped);
                gitRepoUrlText.setEnabled(!isSkipped);
                gitLocalPathText.setEnabled(!isSkipped);
                gitBranchText.setEnabled(!isSkipped);
                portText.setEnabled(!isSkipped);
                controlPortText.setEnabled(!isSkipped);
                deployedCheck.setEnabled(!isSkipped);
                debugCheck.setEnabled(!isSkipped);
                deploySourcesBtn.setEnabled(!isSkipped);
                deployNewVersionBtn.setEnabled(!isSkipped);
            }
        });

        setControl(container);
    }

    public String getExecutablePath() { return executablePathText.getText(); }
    public String getSourcePath() { return sourcePathText.getText(); }
    public String getCommands() { return commandsText.getText(); }
    public String getSettings() {
        return "port=" + portText.getText() + ";controlPort=" + controlPortText.getText() + ";debug=" + debugCheck.getSelection() + ";custom=" + settingsText.getText();
    }
    public String getGitRepoUrl() { return gitRepoUrlText.getText(); }
    public String getGitLocalPath() { return gitLocalPathText.getText(); }
    public String getGitBranch() { return gitBranchText.getText(); }
    public boolean isDeployed() { return deployedCheck.getSelection(); }
    public boolean isDebugMode() { return debugCheck.getSelection(); }
    public boolean isSkipped() { return isSkipped; }
}
