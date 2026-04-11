package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.model.orchestration.AIProvider;

public class ModelDetailsDialog extends Dialog {

    private AIProvider provider;
    private Text nameText;
    private Text urlText;
    private Text tokenText;
    private Button encryptedBtn;
    private Button useEnvBtn;
    private Text envNameText;
    private org.eclipse.swt.widgets.Scale ratingScale;
    private Label ratingLabel;
    private Combo stateCombo;
    private Text stateDescText;

    public ModelDetailsDialog(Shell parentShell, AIProvider provider) {
        super(parentShell);
        this.provider = provider;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(3, false));

        createLabel(container, "Name:");
        nameText = createText(container, provider.getName(), false);

        createLabel(container, "URL:");
        urlText = createText(container, provider.getUrl(), false);

        createLabel(container, "Token:");
        tokenText = new Text(container, SWT.BORDER | SWT.PASSWORD);
        tokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        String rawToken = provider.getApiKey();
        if (provider.isApiKeyEncrypted() && rawToken != null && !rawToken.isEmpty()) {
            try {
                rawToken = eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().decrypt(rawToken);
            } catch (Exception e) {}
        }
        tokenText.setText(rawToken != null ? rawToken : "");

        Button pasteBtn = new Button(container, SWT.PUSH);
        pasteBtn.setText("Paste");
        pasteBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Clipboard cb = new Clipboard(getShell().getDisplay());
                String text = (String) cb.getContents(TextTransfer.getInstance());
                if (text != null) tokenText.setText(text);
                cb.dispose();
            }
        });

        createLabel(container, "");
        encryptedBtn = new Button(container, SWT.CHECK);
        encryptedBtn.setText("Encrypt Token in Model");
        encryptedBtn.setSelection(provider.isApiKeyEncrypted());
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        encryptedBtn.setLayoutData(gd);

        createLabel(container, "");
        useEnvBtn = new Button(container, SWT.CHECK);
        useEnvBtn.setText("Use System Environment Variable");
        useEnvBtn.setSelection(provider.isUseEnvVar());
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        useEnvBtn.setLayoutData(gd);

        createLabel(container, "Env Var Name:");
        envNameText = createText(container, provider.getEnvVarName(), false);

        Button exportBtn = new Button(container, SWT.PUSH);
        exportBtn.setText("Export Help");
        exportBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                MessageDialog.openInformation(getShell(), "Export to Environment",
                    "To use environment variables (OS independent in terms of configuration):\n\n" +
                    "Windows: setx " + envNameText.getText() + " \"your_token\"\n" +
                    "Linux/macOS: export " + envNameText.getText() + "=\"your_token\" (add to .bashrc/.zshrc)\n\n" +
                    "The application will read this variable at runtime.");
            }
        });

        createLabel(container, "User Rating (1-100):");
        Composite ratingComp = new Composite(container, SWT.NONE);
        ratingComp.setLayout(new GridLayout(2, false));
        ratingComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        ratingScale = new org.eclipse.swt.widgets.Scale(ratingComp, SWT.HORIZONTAL);
        ratingScale.setMinimum(1);
        ratingScale.setMaximum(100);
        ratingScale.setSelection(provider.getRating() > 0 ? provider.getRating() : 50);
        ratingScale.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        ratingLabel = new Label(ratingComp, SWT.NONE);
        ratingLabel.setText(String.valueOf(ratingScale.getSelection()));
        ratingScale.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                ratingLabel.setText(String.valueOf(ratingScale.getSelection()));
            }
        });

        createLabel(container, "State:");
        stateCombo = new Combo(container, SWT.READ_ONLY);
        stateCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        stateCombo.setItems(new String[] { "NA", "OK", "ERR" });
        stateCombo.select(provider.getState() != null ? stateCombo.indexOf(provider.getState()) : 0);
        if (stateCombo.getSelectionIndex() == -1) stateCombo.select(0);

        createLabel(container, "State Description:");
        stateDescText = createText(container, provider.getStateDescription(), false);

        return container;
    }

    private void createLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
    }

    private Text createText(Composite parent, String value, boolean password) {
        Text text = new Text(parent, SWT.BORDER | (password ? SWT.PASSWORD : 0));
        text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        text.setText(value != null ? value : "");
        return text;
    }

    @Override
    protected void okPressed() {
        provider.setRating(ratingScale.getSelection());
        provider.setState(stateCombo.getText());
        provider.setStateDescription(stateDescText.getText());

        String token = tokenText.getText();
        if (encryptedBtn.getSelection()) {
            try {
                token = eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().encrypt(token);
            } catch (Exception e) {
                MessageDialog.openError(getShell(), "Encryption Error", "Failed to encrypt token: " + e.getMessage());
                return;
            }
        }

        provider.setName(nameText.getText());
        provider.setUrl(urlText.getText());
        provider.setApiKey(token);
        provider.setApiKeyEncrypted(encryptedBtn.getSelection());
        provider.setUseEnvVar(useEnvBtn.getSelection());
        provider.setEnvVarName(envNameText.getText());
        super.okPressed();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("AI Provider Details");
    }
}
