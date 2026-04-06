package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;
import java.io.File;

public class MavenGroup {
    private Composite group;
    private Text mavenGoalsText, mavenProfilesText;
    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private Color successColor;

    public MavenGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.successColor = successColor;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Maven Tool Settings", 3, false);
        SWTFactory.createLabel(group, "Goals:");
        mavenGoalsText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, mavenGoalsText);

        SWTFactory.createLabel(group, "Profiles:");
        mavenProfilesText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, mavenProfilesText);

        SWTFactory.createLabel(group, "");
        Button testBtn = SWTFactory.createButton(group, "Test Maven");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testMaven();
            }
        });
    }

    private void testMaven() {
        try {
            File workingDir = getWorkingDir();
            TaskContext context = new TaskContext(orchestrator, workingDir);
            eu.kalafatic.evolution.controller.orchestration.ShellTool shell = new eu.kalafatic.evolution.controller.orchestration.ShellTool();
            String result = shell.execute(System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd -version" : "mvn -version", workingDir, context);
            MessageDialog.openInformation(group.getShell(), "Maven Test", "Maven is available:\n" + result);
            if (orchestrator.getMaven() != null) {
                orchestrator.getMaven().setTestStatus("SUCCESS");
                updateGroupStatus();
            }
        } catch (Exception e) {
            MessageDialog.openError(group.getShell(), "Maven Test Failed", e.getMessage());
            if (orchestrator.getMaven() != null) {
                orchestrator.getMaven().setTestStatus("FAILED");
                updateGroupStatus();
            }
        }
    }

    private File getWorkingDir() {
        // Fallback working directory
        return new File(System.getProperty("java.io.tmpdir"));
    }

    public void updateUI() {
        if (orchestrator.getMaven() != null) {
            Maven maven = orchestrator.getMaven();
            mavenGoalsText.setText(maven.getGoals().toString());
            mavenProfilesText.setText(maven.getProfiles().toString());
            updateGroupStatus();
        }
    }

    public void updateModel() {
        if (orchestrator.getMaven() == null) {
            orchestrator.setMaven(OrchestrationFactory.eINSTANCE.createMaven());
        }
        Maven maven = orchestrator.getMaven();
        maven.getGoals().clear();
        for (String goal : mavenGoalsText.getText().replace("[", "").replace("]", "").split("[,\\s]+")) {
            if (!goal.trim().isEmpty()) maven.getGoals().add(goal.trim());
        }
        maven.getProfiles().clear();
        for (String prof : mavenProfilesText.getText().replace("[", "").replace("]", "").split("[,\\s]+")) {
            if (!prof.trim().isEmpty()) maven.getProfiles().add(prof.trim());
        }
    }

    public void updateGroupStatus() {
        if (orchestrator.getMaven() != null) {
            String status = orchestrator.getMaven().getTestStatus();
            if ("SUCCESS".equals(status)) {
                group.setBackground(successColor);
            } else if ("FAILED".equals(status)) {
                group.setBackground(group.getDisplay().getSystemColor(SWT.COLOR_RED));
            } else {
                group.setBackground(group.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
            }
        }
    }

    public Text[] getTextFields() {
        return new Text[] { mavenGoalsText, mavenProfilesText };
    }

    public Composite getGroup() {
        return group;
    }
}
