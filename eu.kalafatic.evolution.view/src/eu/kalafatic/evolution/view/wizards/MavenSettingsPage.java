package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class MavenSettingsPage extends WizardPage {
    private Text goalsText;

    public MavenSettingsPage() {
        super("MavenSettingsPage");
        setTitle("Maven Settings");
        setDescription("Configure Maven build goals.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Goals (comma separated):");
        goalsText = new Text(container, SWT.BORDER);
        goalsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        goalsText.setText("clean, install");

        setControl(container);
    }

    public String getGoals() { return goalsText.getText(); }
}
