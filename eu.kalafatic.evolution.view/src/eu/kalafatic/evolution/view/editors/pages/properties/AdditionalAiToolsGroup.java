package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class AdditionalAiToolsGroup extends AEvoGroup {
    private Text aiChatUrlText, neuronAiUrlText, compilerSourceText;

    public AdditionalAiToolsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Additional AI & Tools", 3, false);
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

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            if (orchestrator.getAiChat() != null) aiChatUrlText.setText(orchestrator.getAiChat().getUrl() != null ? orchestrator.getAiChat().getUrl() : "");
            if (orchestrator.getNeuronAI() != null) neuronAiUrlText.setText(orchestrator.getNeuronAI().getUrl() != null ? orchestrator.getNeuronAI().getUrl() : "");
            if (orchestrator.getCompiler() != null) compilerSourceText.setText(orchestrator.getCompiler().getSourceVersion() != null ? orchestrator.getCompiler().getSourceVersion() : "");
        }
    }

    @Override
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

    @Override
    public Text[] getTextFields() {
        return new Text[] { aiChatUrlText, neuronAiUrlText, compilerSourceText };
    }
}
