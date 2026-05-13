package eu.kalafatic.evolution.view.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class SupervisorSettingsPage extends AWizardPage {
    private Text executablePathText;
    private Text sourcePathText;
    private Text commandsText;
    private Text settingsText;
    private Button deployedCheck;
    private boolean isSkipped = false;

    public SupervisorSettingsPage() {
        super("SupervisorSettingsPage");
        setTitle("Supervisor Settings");
        setDescription("Set external loop controller settings for self-development.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));

        Label label = new Label(container, SWT.NONE);
        label.setText("Executable Path:");
        executablePathText = new Text(container, SWT.BORDER);
        executablePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            executablePathText.setText(orchestrator.getSupervisorSettings().getExecutablePath() != null ? orchestrator.getSupervisorSettings().getExecutablePath() : "");
        }
        SWTFactory.createEditButton(container, executablePathText);

        label = new Label(container, SWT.NONE);
        label.setText("Source Path:");
        sourcePathText = new Text(container, SWT.BORDER);
        sourcePathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            sourcePathText.setText(orchestrator.getSupervisorSettings().getSourcePath() != null ? orchestrator.getSupervisorSettings().getSourcePath() : "");
        }
        SWTFactory.createEditButton(container, sourcePathText);

        label = new Label(container, SWT.NONE);
        label.setText("Commands:");
        commandsText = new Text(container, SWT.BORDER);
        commandsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        new Label(container, SWT.NONE); // Placeholder

        label = new Label(container, SWT.NONE);
        label.setText("Settings:");
        settingsText = new Text(container, SWT.BORDER);
        settingsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        new Label(container, SWT.NONE); // Placeholder

        label = new Label(container, SWT.NONE);
        label.setText("Deployed:");
        deployedCheck = new Button(container, SWT.CHECK);
        deployedCheck.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

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
                deployedCheck.setEnabled(!isSkipped);
                deploySourcesBtn.setEnabled(!isSkipped);
                deployNewVersionBtn.setEnabled(!isSkipped);
            }
        });

        setControl(container);
    }

    public String getExecutablePath() { return executablePathText.getText(); }
    public String getSourcePath() { return sourcePathText.getText(); }
    public String getCommands() { return commandsText.getText(); }
    public String getSettings() { return settingsText.getText(); }
    public boolean isDeployed() { return deployedCheck.getSelection(); }
    public boolean isSkipped() { return isSkipped; }
}
