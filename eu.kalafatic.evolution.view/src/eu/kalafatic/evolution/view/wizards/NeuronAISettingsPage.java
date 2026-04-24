package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.model.orchestration.NeuronType;

public class NeuronAISettingsPage extends AWizardPage {
    private Text urlText, modelText;
    private ControlDecoration urlDecorator;
    private Combo typeCombo;
    private Button skipCheck;

    public NeuronAISettingsPage() {
        super("NeuronAISettingsPage");
        setTitle("Neuron AI Settings");
        setDescription("Configure Neuron Network AI settings.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Neuron AI URL:");
        urlText = new Text(container, SWT.BORDER);
        urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        urlDecorator = new ControlDecoration(urlText, SWT.TOP | SWT.LEFT);
        urlDecorator.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
        urlDecorator.setDescriptionText("Neuron AI URL is required for neural network features.");
        urlDecorator.setShowOnlyOnFocus(false);

        urlText.addModifyListener(e -> validateFields());

        new Label(container, SWT.NONE).setText("Model Name:");
        modelText = new Text(container, SWT.BORDER);
        modelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        new Label(container, SWT.NONE).setText("Model Type:");
        typeCombo = new Combo(container, SWT.READ_ONLY);
        typeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (NeuronType type : NeuronType.VALUES) {
            typeCombo.add(type.getName());
        }
        typeCombo.select(0); // MLP

        skipCheck = new Button(container, SWT.CHECK);
        skipCheck.setText("Skip this step and setup later");
        skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

        setControl(container);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible && orchestrator != null) {
            updateModel();
        }
    }

    private void validateFields() {
        if (urlText.getText().isEmpty()) urlDecorator.show(); else urlDecorator.hide();
    }

    public void updateModel() {
        if (orchestrator == null || isSkipped()) return;
        eu.kalafatic.evolution.model.orchestration.NeuronAI neuronAI = orchestrator.getNeuronAI();
        if (neuronAI == null) {
            neuronAI = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createNeuronAI();
            orchestrator.setNeuronAI(neuronAI);
        }
        neuronAI.setUrl(getUrl());
        neuronAI.setModel(getModelName());
        neuronAI.setType(getModelType());
    }

    public String getUrl() { return urlText.getText(); }
    public String getModelName() { return modelText.getText(); }
    public NeuronType getModelType() {
        return NeuronType.getByName(typeCombo.getText());
    }
    public boolean isSkipped() { return skipCheck.getSelection(); }
}
