package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class AiChatSettingsPage extends WizardPage {
    private Text urlText, tokenText, promptText, proxyUrlText;
    private Button skipCheck;

    public AiChatSettingsPage() {
        super("AiChatSettingsPage");
        setTitle("AI Chat Settings");
        setDescription("Configure AI Chat service settings.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Chat URL:");
        urlText = new Text(container, SWT.BORDER);
        urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        urlText.setText("http://localhost:58080/ai");

        new Label(container, SWT.NONE).setText("Token:");
        tokenText = new Text(container, SWT.BORDER | SWT.PASSWORD);
        tokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tokenText.setText("ENTER_TOKEN_HERE");

        new Label(container, SWT.NONE).setText("Initial Prompt:");
        promptText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 60;
        promptText.setLayoutData(gd);
        promptText.setText("You are a helpful assistant.");

        new Label(container, SWT.NONE).setText("Proxy URL:");
        proxyUrlText = new Text(container, SWT.BORDER);
        proxyUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        skipCheck = new Button(container, SWT.CHECK);
        skipCheck.setText("Skip this step and setup later");
        skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

        setControl(container);
    }

    public String getChatUrl() { return urlText.getText(); }
    public String getToken() { return tokenText.getText(); }
    public String getPrompt() { return promptText.getText(); }
    public String getProxyUrl() { return proxyUrlText.getText(); }
    public boolean isSkipped() { return skipCheck.getSelection(); }
}
