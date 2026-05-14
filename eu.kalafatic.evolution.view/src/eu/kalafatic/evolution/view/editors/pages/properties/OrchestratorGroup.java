package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class OrchestratorGroup extends AEvoGroup {
    private Text orchIdText, orchNameText;

    public OrchestratorGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Orchestrator", 3, true);
        SWTFactory.createLabel(group, "ID:");
        orchIdText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, orchIdText);
        SWTFactory.createLabel(group, "Name:");
        orchNameText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, orchNameText);
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            setTextSafe(orchIdText, orchestrator.getId());
            setTextSafe(orchNameText, orchestrator.getName());
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator != null) {
            ProjectModelManager.getInstance().updateOrchestratorGeneral(orchestrator, orchIdText.getText(), orchNameText.getText());
        }
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { orchIdText, orchNameText };
    }
}
