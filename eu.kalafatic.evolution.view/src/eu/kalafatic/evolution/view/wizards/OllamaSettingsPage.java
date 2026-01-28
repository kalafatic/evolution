package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class OllamaSettingsPage extends WizardPage {
    private Text urlText, modelText, pathText;

    public OllamaSettingsPage() {
        super("OllamaSettingsPage");
        setTitle("Ollama Settings");
        setDescription("Configure Ollama API settings.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Ollama URL:");
        urlText = new Text(container, SWT.BORDER);
        urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        urlText.setText("http://localhost:11434");

        new Label(container, SWT.NONE).setText("Model Name:");
        modelText = new Text(container, SWT.BORDER);
        modelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        modelText.setText("llama3");

        new Label(container, SWT.NONE).setText("Executable Path:");
        pathText = new Text(container, SWT.BORDER);
        pathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        pathText.setText("/usr/bin/ollama");

        setControl(container);
    }

    public String getOllamaUrl() { return urlText.getText(); }
    public String getModelName() { return modelText.getText(); }
    public String getExecutablePath() { return pathText.getText(); }
}
