package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class OrchestratorGroup {
    private Composite group;
    private Text orchIdText, orchNameText;
    private Orchestrator orchestrator;

    public OrchestratorGroup(FormToolkit toolkit, Composite parent, Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(parent, "Orchestrator", 3, true);
        SWTFactory.createLabel(group, "ID:");
        orchIdText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, orchIdText);
        SWTFactory.createLabel(group, "Name:");
        orchNameText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, orchNameText);
    }

    public void updateUI() {
        if (orchestrator != null) {
            orchIdText.setText(orchestrator.getId() != null ? orchestrator.getId() : "");
            orchNameText.setText(orchestrator.getName() != null ? orchestrator.getName() : "");
        }
    }

    public void updateModel() {
        if (orchestrator != null) {
            orchestrator.setId(orchIdText.getText());
            orchestrator.setName(orchNameText.getText());
        }
    }

    public Text[] getTextFields() {
        return new Text[] { orchIdText, orchNameText };
    }
}
