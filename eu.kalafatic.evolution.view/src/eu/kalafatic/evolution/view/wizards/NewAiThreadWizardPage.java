package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NewAiThreadWizardPage extends WizardPage {
    private Text threadNameText;

    protected NewAiThreadWizardPage() {
        super("NewAiThreadWizardPage");
        setTitle("New AI Chat Thread");
        setDescription("Enter a description for the new AI chat thread.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Thread Description:");
        threadNameText = new Text(container, SWT.BORDER);
        threadNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        threadNameText.setText("task");
        threadNameText.addModifyListener(e -> {
            setPageComplete(!threadNameText.getText().trim().isEmpty());
        });

        setControl(container);
    }

    public String getThreadName() {
        return threadNameText.getText();
    }
}
