package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class LlmSettingsGroup {
    private Composite group;
    private Text llmModelText, llmTempText;
    private Orchestrator orchestrator;
    private ControlDecoration llmTempDecorator;

    public LlmSettingsGroup(FormToolkit toolkit, Composite parent, Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "LLM Settings", 3, false);
        SWTFactory.createLabel(group, "Model:");
        llmModelText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, llmModelText);
        SWTFactory.createLabel(group, "Temperature:");
        llmTempText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, llmTempText);

        llmTempDecorator = new ControlDecoration(llmTempText, SWT.TOP | SWT.LEFT);
        llmTempDecorator.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        llmTempDecorator.hide();
    }

    public void updateUI() {
        if (orchestrator != null && orchestrator.getLlm() != null) {
            llmModelText.setText(orchestrator.getLlm().getModel() != null ? orchestrator.getLlm().getModel() : "");
            llmTempText.setText(String.valueOf(orchestrator.getLlm().getTemperature()));
        }
    }

    public void updateModel() {
        if (orchestrator != null) {
            if (orchestrator.getLlm() == null) {
                orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
            }
            orchestrator.getLlm().setModel(llmModelText.getText());
            try {
                orchestrator.getLlm().setTemperature(Float.parseFloat(llmTempText.getText()));
                llmTempDecorator.hide();
            } catch (NumberFormatException e) {
                llmTempDecorator.setDescriptionText("Temperature must be a number");
                llmTempDecorator.show();
            }
        }
    }

    public Text[] getTextFields() {
        return new Text[] { llmModelText, llmTempText };
    }
}
