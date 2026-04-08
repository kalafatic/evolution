package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class AdditionalAiToolsGroup {
    private Composite group;
    private Text aiChatUrlText, neuronAiUrlText, compilerSourceText;
    private Orchestrator orchestrator;

    public AdditionalAiToolsGroup(FormToolkit toolkit, Composite parent, Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(parent, "Additional AI & Tools", 3, false);
        SWTFactory.createLabel(group, "AI Chat URL:");
        aiChatUrlText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, aiChatUrlText);
        SWTFactory.createLabel(group, "Neuron AI URL:");
        neuronAiUrlText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, neuronAiUrlText);
        SWTFactory.createLabel(group, "Compiler Source:");
        compilerSourceText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, compilerSourceText);
    }

    public void updateUI() {
        if (orchestrator != null) {
            if (orchestrator.getAiChat() != null) aiChatUrlText.setText(orchestrator.getAiChat().getUrl() != null ? orchestrator.getAiChat().getUrl() : "");
            if (orchestrator.getNeuronAI() != null) neuronAiUrlText.setText(orchestrator.getNeuronAI().getUrl() != null ? orchestrator.getNeuronAI().getUrl() : "");
            if (orchestrator.getCompiler() != null) compilerSourceText.setText(orchestrator.getCompiler().getSourceVersion() != null ? orchestrator.getCompiler().getSourceVersion() : "");
        }
    }

    public void updateModel() {
        if (orchestrator != null) {
            if (orchestrator.getAiChat() == null) orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
            orchestrator.getAiChat().setUrl(aiChatUrlText.getText());
            if (orchestrator.getNeuronAI() == null) orchestrator.setNeuronAI(OrchestrationFactory.eINSTANCE.createNeuronAI());
            orchestrator.getNeuronAI().setUrl(neuronAiUrlText.getText());
            if (orchestrator.getCompiler() == null) orchestrator.setCompiler(OrchestrationFactory.eINSTANCE.createCompiler());
            orchestrator.getCompiler().setSourceVersion(compilerSourceText.getText());
        }
    }

    public Text[] getTextFields() {
        return new Text[] { aiChatUrlText, neuronAiUrlText, compilerSourceText };
    }
}
