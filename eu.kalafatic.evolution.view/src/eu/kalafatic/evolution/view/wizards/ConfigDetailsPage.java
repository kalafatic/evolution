package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ConfigDetailsPage extends WizardPage {
    private Text fileNameText;

    public ConfigDetailsPage() {
        super("ConfigDetailsPage");
        setTitle("Configuration Details");
        setDescription("Enter the configuration file name.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Config File Name (.xml):");
        fileNameText = new Text(container, SWT.BORDER);
        fileNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fileNameText.setText("evo_config.xml");

        setControl(container);
    }

    public String getFileName() { return fileNameText.getText(); }
}
