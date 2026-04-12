package eu.kalafatic.evolution.view.editors.pages.tools;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Eclipse;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;
import java.io.File;

public class EclipseGroup extends AToolGroup {
    private Text eclipseWorkspaceText, eclipseInstallationText, eclipseTargetPlatformText;

    public EclipseGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Eclipse Development Settings", 3, false);

        SWTFactory.createLabel(group, "Workspace Path:");
        eclipseWorkspaceText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, eclipseWorkspaceText);

        SWTFactory.createLabel(group, "Eclipse Installation:");
        eclipseInstallationText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, eclipseInstallationText);

        SWTFactory.createLabel(group, "Target Platform:");
        eclipseTargetPlatformText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, eclipseTargetPlatformText);

        SWTFactory.createLabel(group, "");
        Button testBtn = SWTFactory.createButton(group, "Test Eclipse");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testEclipse();
            }
        });
    }

    private void testEclipse() {
        try {
            eu.kalafatic.evolution.controller.tools.EclipseTool tool = new eu.kalafatic.evolution.controller.tools.EclipseTool();
            File workingDir = getWorkingDir();
            TaskContext context = new TaskContext(orchestrator, workingDir);
            String result = tool.execute("TEST_CONNECTION", workingDir, context);
            MessageDialog.openInformation(group.getShell(), "Eclipse Tool Test", result);
            if (orchestrator.getEclipse() != null) {
                orchestrator.getEclipse().setTestStatus("SUCCESS");
                updateGroupStatus();
            }
        } catch (Exception e) {
            MessageDialog.openError(group.getShell(), "Eclipse Tool Test Failed", e.getMessage());
            if (orchestrator.getEclipse() != null) {
                orchestrator.getEclipse().setTestStatus("FAILED");
                updateGroupStatus();
            }
        }
    }

    private File getWorkingDir() {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    @Override
    protected void refreshUI() {
        if (orchestrator.getEclipse() != null) {
            Eclipse eclipse = orchestrator.getEclipse();
            eclipseWorkspaceText.setText(eclipse.getWorkspace() != null ? eclipse.getWorkspace() : "");
            eclipseInstallationText.setText(eclipse.getInstallation() != null ? eclipse.getInstallation() : "");
            eclipseTargetPlatformText.setText(eclipse.getTargetPlatform() != null ? eclipse.getTargetPlatform() : "");
            updateGroupStatus();
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator.getEclipse() == null) {
            orchestrator.setEclipse(OrchestrationFactory.eINSTANCE.createEclipse());
        }
        Eclipse eclipse = orchestrator.getEclipse();
        eclipse.setWorkspace(eclipseWorkspaceText.getText());
        eclipse.setInstallation(eclipseInstallationText.getText());
        eclipse.setTargetPlatform(eclipseTargetPlatformText.getText());
    }

    @Override
    protected String getTestStatus() {
        return orchestrator.getEclipse() != null ? orchestrator.getEclipse().getTestStatus() : null;
    }

    @Override
    protected void clearTestStatus() {
        if (orchestrator.getEclipse() != null) {
            orchestrator.getEclipse().setTestStatus(null);
        }
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { eclipseWorkspaceText, eclipseInstallationText, eclipseTargetPlatformText };
    }
}
