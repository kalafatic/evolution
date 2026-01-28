package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class GitSettingsPage extends WizardPage {
    private Text repoUrlText, branchText, usernameText, localPathText;

    public GitSettingsPage() {
        super("GitSettingsPage");
        setTitle("Git Settings");
        setDescription("Configure Git repository settings.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Repository URL:");
        repoUrlText = new Text(container, SWT.BORDER);
        repoUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        repoUrlText.setText("https://github.com/kalafatic/evo.git");

        new Label(container, SWT.NONE).setText("Branch:");
        branchText = new Text(container, SWT.BORDER);
        branchText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        branchText.setText("master");

        new Label(container, SWT.NONE).setText("Username:");
        usernameText = new Text(container, SWT.BORDER);
        usernameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        usernameText.setText("admin");

        new Label(container, SWT.NONE).setText("Local Path:");
        localPathText = new Text(container, SWT.BORDER);
        localPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        localPathText.setText("./repo");

        setControl(container);
    }

    public String getRepoUrl() { return repoUrlText.getText(); }
    public String getBranch() { return branchText.getText(); }
    public String getUsername() { return usernameText.getText(); }
    public String getLocalPath() { return localPathText.getText(); }
}
