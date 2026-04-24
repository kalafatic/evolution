package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class AiChatSettingsPage extends AWizardPage {
    private Text urlText, tokenText, promptText, proxyUrlText;
    private Button skipCheck;
    private ControlDecoration urlDecorator, tokenDecorator;

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

        urlDecorator = new ControlDecoration(urlText, SWT.TOP | SWT.LEFT);
        urlDecorator.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
        urlDecorator.setDescriptionText("AI Chat URL is required for remote features.");
        urlDecorator.setShowOnlyOnFocus(false);

        new Label(container, SWT.NONE).setText("Token:");
        tokenText = new Text(container, SWT.BORDER | SWT.PASSWORD);
        tokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        tokenDecorator = new ControlDecoration(tokenText, SWT.TOP | SWT.LEFT);
        tokenDecorator.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
        tokenDecorator.setDescriptionText("API Token is required for remote service authentication.");
        tokenDecorator.setShowOnlyOnFocus(false);

        urlText.addModifyListener(e -> validateFields());
        tokenText.addModifyListener(e -> validateFields());

        new Label(container, SWT.NONE).setText("Initial Prompt:");
        promptText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 60;
        promptText.setLayoutData(gd);

        new Label(container, SWT.NONE).setText("Proxy URL:");
        proxyUrlText = new Text(container, SWT.BORDER);
        proxyUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        skipCheck = new Button(container, SWT.CHECK);
        skipCheck.setText("Skip this step and setup later");
        skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

        setControl(container);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (!visible && orchestrator != null) {
            updateModel();
        }
    }

    private void validateFields() {
        if (urlText.getText().isEmpty()) urlDecorator.show(); else urlDecorator.hide();
        if (tokenText.getText().isEmpty()) tokenDecorator.show(); else tokenDecorator.hide();
    }

    public void updateModel() {
        if (orchestrator == null || isSkipped()) return;
        eu.kalafatic.evolution.model.orchestration.AiChat aiChat = orchestrator.getAiChat();
        if (aiChat == null) {
            aiChat = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createAiChat();
            orchestrator.setAiChat(aiChat);
        }
        aiChat.setUrl(getChatUrl());
        aiChat.setToken(getToken());
        aiChat.setPrompt(getPrompt());
        aiChat.setProxyUrl(getProxyUrl());
    }

    public String getChatUrl() { return urlText.getText(); }
    public String getToken() { return tokenText.getText(); }
    public String getPrompt() { return promptText.getText(); }
    public String getProxyUrl() { return proxyUrlText.getText(); }
    public boolean isSkipped() { return skipCheck.getSelection(); }
}
