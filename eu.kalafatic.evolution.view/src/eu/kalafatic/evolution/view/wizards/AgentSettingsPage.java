package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class AgentSettingsPage extends WizardPage {
    private Text agentsText;
    private Button skipCheck;
    private ControlDecoration agentsDecorator;

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

        agentsDecorator = new ControlDecoration(agentsText, SWT.TOP | SWT.LEFT);
        agentsDecorator.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        agentsDecorator.hide();

        agentsText.addModifyListener(e -> validateAgents());

        skipCheck = new Button(container, SWT.CHECK);
        skipCheck.setText("Skip this step and setup later");
        skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false));
        skipCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                validateAgents();
            }
        });

        setControl(container);
        validateAgents();
    }

    private void validateAgents() {
        if (isSkipped()) {
            agentsDecorator.hide();
            setErrorMessage(null);
            setPageComplete(true);
            return;
        }

        String text = agentsText.getText();
        if (text.isEmpty()) {
            agentsDecorator.setDescriptionText("Agents list cannot be empty.");
            agentsDecorator.show();
            setErrorMessage("At least one agent is required.");
            setPageComplete(false);
            return;
        }

        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            if (!line.trim().isEmpty() && !line.contains(":")) {
                agentsDecorator.setDescriptionText("Invalid format: " + line + ". Expected id:type");
                agentsDecorator.show();
                setErrorMessage("Invalid agent format. Expected id:type");
                setPageComplete(false);
                return;
            }
        }

        agentsDecorator.hide();
        setErrorMessage(null);
        setPageComplete(true);
    }

    public boolean isSkipped() {
        return skipCheck != null && skipCheck.getSelection();
    }

    public String getAgentsData() {
        return agentsText.getText();
    }
}
