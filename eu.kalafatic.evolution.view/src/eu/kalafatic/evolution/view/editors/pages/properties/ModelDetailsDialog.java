package eu.kalafatic.evolution.view.editors.pages.properties;

import java.util.LinkedHashMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.utils.dialogs.DynamicField;
import eu.kalafatic.utils.dialogs.DynamicMapDialog;
import eu.kalafatic.utils.factories.GUIFactory;

public class ModelDetailsDialog extends DynamicMapDialog {

    private AIProvider provider;

    private static final String NAME = "name";
    private static final String URL = "url";
    private static final String TOKEN = "token";
    private static final String ENCRYPTED = "encrypted";
    private static final String USE_ENV = "useEnv";
    private static final String ENV_NAME = "envName";
    private static final String RATING = "rating";
    private static final String STATE = "state";
    private static final String STATE_DESC = "stateDesc";

    public ModelDetailsDialog(Shell parentShell, AIProvider provider) {
        super(parentShell, createFields(provider));
        this.provider = provider;
        setTitle("AI Provider Details");
        setContainerWidth(700);
    }

    private static LinkedHashMap<String, DynamicField> createFields(AIProvider provider) {
        LinkedHashMap<String, DynamicField> fields = new LinkedHashMap<>();
        fields.put(NAME, new DynamicField("Name:", DynamicField.TYPE_TEXT, provider.getName()));
        fields.put(URL, new DynamicField("URL:", DynamicField.TYPE_TEXT, provider.getUrl()));

        String rawToken = provider.getApiKey();
        if (provider.isApiKeyEncrypted() && rawToken != null && !rawToken.isEmpty()) {
            try {
                rawToken = eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().decrypt(rawToken);
            } catch (Exception e) {}
        }
        fields.put(TOKEN, new DynamicField("Token:", DynamicField.TYPE_TEXT | DynamicField.PASSWORD, rawToken != null ? rawToken : ""));

        fields.put(ENCRYPTED, new DynamicField("Encrypt Token in Model", DynamicField.TYPE_CHECKBOX, provider.isApiKeyEncrypted()));
        fields.put(USE_ENV, new DynamicField("Use System Environment Variable", DynamicField.TYPE_CHECKBOX, provider.isUseEnvVar()));
        fields.put(ENV_NAME, new DynamicField("Env Var Name:", DynamicField.TYPE_TEXT, provider.getEnvVarName()));
        fields.put(RATING, new DynamicField("User Rating (1-100):", DynamicField.TYPE_SPINNER, provider.getRating() > 0 ? provider.getRating() : 50));
        fields.put(STATE, new DynamicField("State:", DynamicField.TYPE_COMBO, provider.getState() != null ? provider.getState() : "NA", "NA", "OK", "ERR"));
        fields.put(STATE_DESC, new DynamicField("State Description:", DynamicField.TYPE_TEXT, provider.getStateDescription()));

        return fields;
    }

    @Override
    protected void createFieldEditor(Composite parent, String key, DynamicField field) {
        super.createFieldEditor(parent, key, field);

        if (TOKEN.equals(key)) {
            Button pasteBtn = GUIFactory.INSTANCE.createButton(parent, "Paste");
            pasteBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Clipboard cb = new Clipboard(getShell().getDisplay());
                    String text = (String) cb.getContents(TextTransfer.getInstance());
                    if (text != null) {
                        Control c = controls.get(TOKEN);
                        if (c instanceof Text) {
                            ((Text) c).setText(text);
                        }
                    }
                    cb.dispose();
                }
            });
        } else if (ENV_NAME.equals(key)) {
            Button exportBtn = GUIFactory.INSTANCE.createButton(parent, "Export Help");
            exportBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Control c = controls.get(ENV_NAME);
                    String envName = (c instanceof Text) ? ((Text) c).getText() : "";
                    MessageDialog.openInformation(getShell(), "Export to Environment",
                        "To use environment variables (OS independent in terms of configuration):\n\n" +
                        "Windows: setx " + envName + " \"your_token\"\n" +
                        "Linux/macOS: export " + envName + "=\"your_token\" (add to .bashrc/.zshrc)\n\n" +
                        "The application will read this variable at runtime.");
                }
            });
        }
    }

    @Override
    protected void okPressed() {
        if (!validate()) return;
        saveValues();

        provider.setRating(getInteger(RATING));
        provider.setState(getString(STATE));
        provider.setStateDescription(getString(STATE_DESC));

        String token = getString(TOKEN);
        boolean encrypt = getBoolean(ENCRYPTED);
        if (encrypt) {
            try {
                token = eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().encrypt(token);
            } catch (Exception e) {
                MessageDialog.openError(getShell(), "Encryption Error", "Failed to encrypt token: " + e.getMessage());
                return;
            }
        }

        provider.setName(getString(NAME));
        provider.setUrl(getString(URL));
        provider.setApiKey(token);
        provider.setApiKeyEncrypted(encrypt);
        provider.setUseEnvVar(getBoolean(USE_ENV));
        provider.setEnvVarName(getString(ENV_NAME));
        super.okPressed();
    }
}
