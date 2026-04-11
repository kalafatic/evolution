package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class OrchestrationGeneralPage extends AWizardPage {
    private Text nameText;
    private Text idText;

    public OrchestrationGeneralPage() {
        super("OrchestrationGeneralPage");
        setTitle("General Orchestration Settings");
        setDescription("Enter name and ID for the new orchestration.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Name:");
        nameText = new Text(container, SWT.BORDER);
        nameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        nameText.setText("New Orchestration");

        new Label(container, SWT.NONE).setText("ID:");
        idText = new Text(container, SWT.BORDER);
        idText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        idText.setText("orch" + (System.currentTimeMillis() % 10000));

        setControl(container);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible && orchestrator != null) {
            orchestrator.setName(getOrchestrationName());
            orchestrator.setId(getOrchestrationId());
        }
    }

    public String getOrchestrationName() { return nameText.getText(); }
    public String getOrchestrationId() { return idText.getText(); }
}
