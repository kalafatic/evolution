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
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.PropertiesPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class McpOpenAiGroup extends AEvoGroup {
    private Combo aiModeCombo;
    private Button offlineBtn;
    private Text mcpUrlText, openAiTokenText, openAiModelText;
    private PropertiesPage page;

    public McpOpenAiGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, PropertiesPage page) {
        super(editor, orchestrator);
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
        Button editTokenBtn = SWTFactory.createEditButton(group, openAiTokenText);
        editTokenBtn.setText("\u2699");
        editTokenBtn.setToolTipText("Detailed Configuration");
        editTokenBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDetailedConfig();
            }
        });
        SWTFactory.createLabel(group, "OpenAI Model:");
        openAiModelText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, openAiModelText);
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            aiModeCombo.select(orchestrator.getAiMode().getValue());
            mcpUrlText.setText(orchestrator.getMcpServerUrl() != null ? orchestrator.getMcpServerUrl() : "");

            eu.kalafatic.evolution.controller.security.TokenSecurityService.ResolvedProvider resolved =
                    eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().resolve(orchestrator, "openai");

            openAiTokenText.setText((resolved != null && resolved.token != null) ? resolved.token : "");
            openAiModelText.setText((resolved != null && resolved.model != null) ? resolved.model : "");
            offlineBtn.setSelection(orchestrator.isOfflineMode());
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator != null) {
            ProjectModelManager.getInstance().updateAiMode(orchestrator, AiMode.get(aiModeCombo.getSelectionIndex()));
            ProjectModelManager.getInstance().updateMcpServerUrl(orchestrator, mcpUrlText.getText());

            eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance()
                .updateToken(orchestrator, "openai", openAiTokenText.getText());

            ProjectModelManager.getInstance().updateOpenAiModel(orchestrator, openAiModelText.getText());
            ProjectModelManager.getInstance().updateOfflineMode(orchestrator, offlineBtn.getSelection());
        }
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { mcpUrlText, openAiTokenText, openAiModelText };
    }

    private void handleDetailedConfig() {
        if (orchestrator == null) return;
        String providerName = "openai";

        // Find existing or create temporary provider
        eu.kalafatic.evolution.model.orchestration.AIProvider provider = orchestrator.getAiProviders().stream()
                .filter(p -> p.getName().equalsIgnoreCase(providerName))
                .findFirst().orElse(null);

        boolean isNew = false;
        if (provider == null) {
            provider = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createAIProvider();
            provider.setName(providerName);
            eu.kalafatic.evolution.controller.providers.ProviderConfig config = eu.kalafatic.evolution.controller.providers.AiProviders.PROVIDERS.get(providerName);
            if (config != null) {
                provider.setUrl(config.getUrl());
                provider.setDefaultModel(config.getDefaultModel());
            }
            isNew = true;
        }

        ModelDetailsDialog dialog = new ModelDetailsDialog(group.getShell(), provider);
        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            if (isNew) {
                orchestrator.getAiProviders().add(provider);
            }
            editor.setDirty(true);
            refreshUI();
        }
    }
}
