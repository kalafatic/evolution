package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class LLMSettingsPage extends WizardPage {
    private Text modelText, tempText;

    public LLMSettingsPage() {
        super("LLMSettingsPage");
        setTitle("LLM Settings");
        setDescription("Configure LLM model and parameters.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("LLM Model:");
        modelText = new Text(container, SWT.BORDER);
        modelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        modelText.setText("gpt-4o");

        new Label(container, SWT.NONE).setText("Temperature:");
        tempText = new Text(container, SWT.BORDER);
        tempText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tempText.setText("0.7");

        setControl(container);
    }

    public String getLlmModel() { return modelText.getText(); }
    public String getTemperature() { return tempText.getText(); }
}
