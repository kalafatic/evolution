package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.controller.discovery.SourceDiscoveryResult;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;

public class ConfigDetailsPage extends AWizardPage {
    private Text fileNameText;
    private Combo defaultTargetCombo;

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
        defaultTargetCombo = new Combo(container, SWT.DROP_DOWN);
        defaultTargetCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Populate with discovery results
        SourceDiscoveryResult discovery = ProjectModelManager.getInstance().getOrDiscoverWorkspace();
        if (discovery.getPrimaryRepository() != null) {
            defaultTargetCombo.add(discovery.getPrimaryRepository().getAbsolutePath());
        }
        for (java.io.File repo : discovery.getGitRepositories()) {
            String path = repo.getAbsolutePath();
            boolean exists = false;
            for (String item : defaultTargetCombo.getItems()) {
                if (item.equals(path)) { exists = true; break; }
            }
            if (!exists) defaultTargetCombo.add(path);
        }

        if (orchestrator != null && orchestrator.getDefaultTarget() != null) {
            defaultTargetCombo.setText(orchestrator.getDefaultTarget());
        } else if (defaultTargetCombo.getItemCount() > 0) {
            defaultTargetCombo.select(0);
        }

        org.eclipse.swt.widgets.Button browseBtn = new org.eclipse.swt.widgets.Button(container, SWT.PUSH);
        browseBtn.setText("Browse...");
        browseBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                org.eclipse.swt.widgets.DirectoryDialog dialog = new org.eclipse.swt.widgets.DirectoryDialog(getShell());
                dialog.setText("Select Default Target Codebase");
                dialog.setMessage("Choose the folder containing the EVO repository.");
                String current = defaultTargetCombo.getText();
                if (current != null && !current.isEmpty()) dialog.setFilterPath(current);
                String selected = dialog.open();
                if (selected != null) {
                    defaultTargetCombo.setText(selected);
                }
            }
        });

        setControl(container);
    }

    public String getFileName() { return fileNameText.getText(); }
    public String getDefaultTargetPath() { return defaultTargetCombo.getText(); }
}
