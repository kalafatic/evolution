package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;

public class OrchestratorGroup extends AEvoGroup {
    private Text orchIdText, orchNameText, projectNameText;

    public OrchestratorGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Orchestrator", 3, true);
        GUIFactory.INSTANCE.createLabel(group, "ID:");
        orchIdText = GUIFactory.INSTANCE.createText(group);
        GUIFactory.INSTANCE.createEditButton(group, orchIdText);
        GUIFactory.INSTANCE.createLabel(group, "Name:");
        orchNameText = GUIFactory.INSTANCE.createText(group);
        GUIFactory.INSTANCE.createEditButton(group, orchNameText);
        GUIFactory.INSTANCE.createLabel(group, "Project Name:");
        projectNameText = GUIFactory.INSTANCE.createText(group);
        GUIFactory.INSTANCE.createEditButton(group, projectNameText);
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            setTextSafe(orchIdText, orchestrator.getId());
            setTextSafe(orchNameText, orchestrator.getName());

            String projectName = "";
            if (orchestrator.eContainer() instanceof EvoProject) {
                projectName = ((EvoProject) orchestrator.eContainer()).getName();
            }
            if (projectName == null || projectName.isEmpty()) {
                if (editor != null && editor.getEditorInput() instanceof org.eclipse.ui.IFileEditorInput) {
                    projectName = ((org.eclipse.ui.IFileEditorInput) editor.getEditorInput()).getFile().getProject().getName();
                }
            }
            setTextSafe(projectNameText, projectName);
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator != null) {
            ProjectModelManager.getInstance().updateOrchestratorGeneral(orchestrator, orchIdText.getText(), orchNameText.getText());
            if (orchestrator.eContainer() instanceof EvoProject) {
                ((EvoProject) orchestrator.eContainer()).setName(projectNameText.getText());
            }
        }
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { orchIdText, orchNameText, projectNameText };
    }
}
