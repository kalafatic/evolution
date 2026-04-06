package eu.kalafatic.evolution.view.editors.pages.aichat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class AiSettingsGroup {
    private Composite group;
    private Combo aiModeCombo;
    private Combo aiRemoteCombo;
    private Text remoteTokenText, remoteUrlText;
    private Composite compositeRemote;
    private AiChatPage page;
    private Orchestrator orchestrator;

    public AiSettingsGroup(FormToolkit toolkit, Composite parent, AiChatPage page, Orchestrator orchestrator) {
        this.page = page;
        this.orchestrator = orchestrator;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "AI Settings", 3, false);

        SWTFactory.createLabel(group, "AI Mode:");
        aiModeCombo = SWTFactory.createCombo(group);
        for (AiMode mode : AiMode.values()) {
            aiModeCombo.add(mode.getName());
        }
        SWTFactory.createLabel(group, "");

        compositeRemote = new Composite(group, SWT.BORDER);
        compositeRemote.setLayout(new GridLayout(3, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        gd.grabExcessVerticalSpace = true;
        compositeRemote.setLayoutData(gd);

        SWTFactory.createLabel(compositeRemote, "AI Remote:");
        aiRemoteCombo = SWTFactory.createCombo(compositeRemote);
        for (String providerName : AiProviders.PROVIDERS.keySet()) {
            aiRemoteCombo.add(providerName);
        }

        Button connectionButton = SWTFactory.createButton(compositeRemote, "Test Connection", 120);
        connectionButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (orchestrator != null) {
                    page.testAiConnectionRemote(aiModeCombo.getSelectionIndex(), aiRemoteCombo.getText(), remoteTokenText.getText(), remoteUrlText.getText());
                } else {
                    MessageBox messageBox = new MessageBox(page.getShell(), SWT.ICON_WARNING | SWT.OK);
                    messageBox.setText("Warning");
                    messageBox.setMessage("Orchestrator not loaded.");
                    messageBox.open();
                }
            }
        });

        SWTFactory.createLabel(compositeRemote, "Token:");
        remoteTokenText = SWTFactory.createPasswordText(compositeRemote);
        SWTFactory.createEditButton(compositeRemote, remoteTokenText);

        SWTFactory.createLabel(compositeRemote, "API URL:");
        remoteUrlText = SWTFactory.createText(compositeRemote);
        SWTFactory.createEditButton(compositeRemote, remoteUrlText);

        aiModeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });

        aiRemoteCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String provider = aiRemoteCombo.getText();
                ProviderConfig config = AiProviders.PROVIDERS.get(provider);
                if (config != null) {
                    remoteUrlText.setText(config.getEndpointUrl() != null ? config.getEndpointUrl() : "");
                    page.syncModelWithUI();
                }
            }
        });

        remoteTokenText.addModifyListener(e -> page.syncModelWithUI());
        remoteUrlText.addModifyListener(e -> page.syncModelWithUI());
    }

    public void updateUI() {
        if (orchestrator != null) {
            aiModeCombo.select(orchestrator.getAiMode().getValue());
            String remoteModel = orchestrator.getRemoteModel();
            if (remoteModel == null || remoteModel.isEmpty()) {
                remoteModel = "deepseek";
            }
            if (remoteModel != null) {
                int index = aiRemoteCombo.indexOf(remoteModel);
                if (index >= 0) aiRemoteCombo.select(index);
            }
            remoteTokenText.setText(orchestrator.getOpenAiToken() != null ? orchestrator.getOpenAiToken() : "");
            remoteUrlText.setText((orchestrator.getAiChat() != null && orchestrator.getAiChat().getUrl() != null)
                    ? orchestrator.getAiChat().getUrl() : "");

            AiMode mode = orchestrator.getAiMode();
            boolean remoteEnabled = mode == AiMode.HYBRID || mode == AiMode.REMOTE;
            SWTFactory.setControlEnabled(remoteEnabled, true, compositeRemote.getChildren());
        }
    }

    public int getAiModeIndex() { return aiModeCombo.getSelectionIndex(); }
    public String getRemoteModel() { return aiRemoteCombo.getText(); }
    public String getRemoteToken() { return remoteTokenText.getText(); }
    public String getRemoteUrl() { return remoteUrlText.getText(); }
    public Composite getRemoteComposite() { return compositeRemote; }

    public void setRemoteToken(String token) {
	remoteTokenText.setText(token);
    }
}
