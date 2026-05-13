package eu.kalafatic.evolution.view.editors.pages.development;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SupervisorSettings;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class SupervisorGroup extends AEvoGroup {

    private Text executablePathText;
    private Text sourcePathText;
    private Text commandsText;
    private Text settingsText;
    private Button deployedCheck;

    public SupervisorGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Supervisor Settings", 3, true);

        SWTFactory.createLabel(group, "Executable Path:");
        executablePathText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, executablePathText);

        SWTFactory.createLabel(group, "Source Path:");
        sourcePathText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, sourcePathText);

        SWTFactory.createLabel(group, "Commands:");
        commandsText = SWTFactory.createText(group);
        new org.eclipse.swt.widgets.Label(group, SWT.NONE);

        SWTFactory.createLabel(group, "Settings:");
        settingsText = SWTFactory.createText(group);
        new org.eclipse.swt.widgets.Label(group, SWT.NONE);

        SWTFactory.createLabel(group, "Deployed:");
        deployedCheck = toolkit.createButton(group, "", SWT.CHECK);
        deployedCheck.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 2, 1));

        load();

        ModifyListener ml = e -> {
            updateModel();
            editor.setDirty(true);
        };
        executablePathText.addModifyListener(ml);
        sourcePathText.addModifyListener(ml);
        commandsText.addModifyListener(ml);
        settingsText.addModifyListener(ml);

        deployedCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateModel();
                editor.setDirty(true);
            }
        });
    }

    public void load() {
        if (orchestrator != null && orchestrator.getSupervisorSettings() != null) {
            SupervisorSettings settings = orchestrator.getSupervisorSettings();
            executablePathText.setText(settings.getExecutablePath() != null ? settings.getExecutablePath() : "");
            sourcePathText.setText(settings.getSourcePath() != null ? settings.getSourcePath() : "");
            commandsText.setText(settings.getCommands() != null ? settings.getCommands() : "");
            settingsText.setText(settings.getSettings() != null ? settings.getSettings() : "");
            deployedCheck.setSelection(settings.isDeployed());
        }
    }

    @Override
    public void refreshUI() {
        load();
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
        settings.setSettings(settingsText.getText());
        settings.setDeployed(deployedCheck.getSelection());
    }
}
