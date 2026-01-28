package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class AgentSettingsPage extends WizardPage {
    private Text agentsText;

    public AgentSettingsPage() {
        super("AgentSettingsPage");
        setTitle("Agent Settings");
        setDescription("Define agents for this orchestration (format: id:type, one per line).");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        new Label(container, SWT.NONE).setText("Agents (id:type):");
        agentsText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 100;
        agentsText.setLayoutData(gd);
        agentsText.setText("developer:coder\nreviewer:critic\nmanager:orchestrator");

        setControl(container);
    }

    public String getAgentsData() {
        return agentsText.getText();
    }
}
