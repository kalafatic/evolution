package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.PropertiesPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class McpOpenAiGroup {
    private Composite group;
    private Combo aiModeCombo;
    private Button offlineBtn;
    private Text mcpUrlText, openAiTokenText, openAiModelText;
    private Orchestrator orchestrator;
    private PropertiesPage page;

    public McpOpenAiGroup(FormToolkit toolkit, Composite parent, Orchestrator orchestrator, PropertiesPage page) {
        this.orchestrator = orchestrator;
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "MCP & OpenAI (Hybrid Architecture)", 3, false);
        SWTFactory.createLabel(group, "AI Mode:");
        aiModeCombo = SWTFactory.createCombo(group);
        aiModeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
        for (AiMode mode : AiMode.values()) {
            aiModeCombo.add(mode.getName());
        }
        aiModeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });

        SWTFactory.createLabel(group, "Offline Mode (Legacy):");
        offlineBtn = new Button(group, SWT.CHECK);
        offlineBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
        offlineBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
            }
        });

        SWTFactory.createLabel(group, "MCP Server URL:");
        mcpUrlText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, mcpUrlText);
        SWTFactory.createLabel(group, "OpenAI Token:");
        openAiTokenText = SWTFactory.createPasswordText(group);
        SWTFactory.createEditButton(group, openAiTokenText);
        SWTFactory.createLabel(group, "OpenAI Model:");
        openAiModelText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, openAiModelText);
    }

    public void updateUI() {
        if (orchestrator != null) {
            aiModeCombo.select(orchestrator.getAiMode().getValue());
            mcpUrlText.setText(orchestrator.getMcpServerUrl() != null ? orchestrator.getMcpServerUrl() : "");
            openAiTokenText.setText(orchestrator.getOpenAiToken() != null ? orchestrator.getOpenAiToken() : "");
            openAiModelText.setText(orchestrator.getOpenAiModel() != null ? orchestrator.getOpenAiModel() : "");
            offlineBtn.setSelection(orchestrator.isOfflineMode());
        }
    }

    public void updateModel() {
        if (orchestrator != null) {
            orchestrator.setAiMode(AiMode.get(aiModeCombo.getSelectionIndex()));
            orchestrator.setMcpServerUrl(mcpUrlText.getText());
            orchestrator.setOpenAiToken(openAiTokenText.getText());
            orchestrator.setOpenAiModel(openAiModelText.getText());
            orchestrator.setOfflineMode(offlineBtn.getSelection());
        }
    }

    public Text[] getTextFields() {
        return new Text[] { mcpUrlText, openAiTokenText, openAiModelText };
    }
}
