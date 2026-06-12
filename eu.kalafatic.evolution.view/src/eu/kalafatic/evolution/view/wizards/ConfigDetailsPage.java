package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ConfigDetailsPage extends AWizardPage {
    private Text fileNameText;
    private Text defaultTargetText;

    public ConfigDetailsPage() {
        super("ConfigDetailsPage");
        setTitle("Configuration Details");
        setDescription("Enter the configuration file name.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(3, false));

        new Label(container, SWT.NONE).setText("Config File Name (.xml):");
        fileNameText = new Text(container, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        fileNameText.setLayoutData(gd);
        fileNameText.setText("evo_config.xml");

        new Label(container, SWT.NONE).setText("Default Target Path (EVO):");
        defaultTargetText = new Text(container, SWT.BORDER);
        defaultTargetText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (orchestrator != null && orchestrator.getDefaultTarget() != null) {
            defaultTargetText.setText(orchestrator.getDefaultTarget());
        }

        org.eclipse.swt.widgets.Button browseBtn = new org.eclipse.swt.widgets.Button(container, SWT.PUSH);
        browseBtn.setText("Browse...");
        browseBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                org.eclipse.swt.widgets.DirectoryDialog dialog = new org.eclipse.swt.widgets.DirectoryDialog(getShell());
                dialog.setText("Select Default Target Codebase");
                dialog.setMessage("Choose the folder containing the EVO repository.");
                String current = defaultTargetText.getText();
                if (current != null && !current.isEmpty()) dialog.setFilterPath(current);
                String selected = dialog.open();
                if (selected != null) {
                    defaultTargetText.setText(selected);
                }
            }
        });

        setControl(container);
    }

    public String getFileName() { return fileNameText.getText(); }
    public String getDefaultTargetPath() { return defaultTargetText.getText(); }
}
