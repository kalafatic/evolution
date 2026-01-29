package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NeuronAISettingsPage extends WizardPage {
    private Text urlText, modelText;
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
        urlText.setText("http://localhost:8080/neuron");

        new Label(container, SWT.NONE).setText("Model Name:");
        modelText = new Text(container, SWT.BORDER);
        modelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        modelText.setText("default-neuron-model");

        skipCheck = new Button(container, SWT.CHECK);
        skipCheck.setText("Skip this step and setup later");
        skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

        setControl(container);
    }

    public String getUrl() { return urlText.getText(); }
    public String getModelName() { return modelText.getText(); }
    public boolean isSkipped() { return skipCheck.getSelection(); }
}
