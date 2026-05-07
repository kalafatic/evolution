package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.editors.pages.properties.ModelDetailsDialog;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class AiSettingsGroup extends AEvoGroup {
	
	
    private Combo aiModeCombo;
    private Combo aiRemoteCombo;
    private Combo localModelCombo;
    private Text remoteTokenText, remoteUrlText;
    private Composite compositeLocal, compositeRemote;
    private AiChatPage page;

    public AiSettingsGroup(FormToolkit toolkit, Composite parent, AiChatPage page, Orchestrator orchestrator) {
        super(page.getEditor(), orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "AI Settings", 1, false);
        
        compositeLocal = SWTFactory.createComposite(group, SWT.BORDER, 3);
        
        SWTFactory.createLabel(compositeLocal, "AI Mode:");
        aiModeCombo = SWTFactory.createCombo(compositeLocal);
        for (AiMode mode : AiMode.values()) {
            aiModeCombo.add(mode.getName());
        }       
        SWTFactory.createLabel(compositeLocal, "");
      
        
        SWTFactory.createLabel(compositeLocal,"Model:");	    	
	localModelCombo = selectModel(compositeLocal);
    	SWTFactory.createLabel(compositeLocal, "");
    	
        
        compositeRemote = SWTFactory.createComposite(group, SWT.BORDER, 3);
        SWTFactory.createLabel(compositeRemote, "AI Remote:");
        aiRemoteCombo = SWTFactory.createCombo(compositeRemote);
        // Providers will be populated in refreshUI

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
        Button editTokenBtn = SWTFactory.createEditButton(compositeRemote, remoteTokenText);
        editTokenBtn.setText("\u2699"); // Gear icon
        editTokenBtn.setToolTipText("Detailed Configuration");
        editTokenBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDetailedConfig();
            }
        });

        SWTFactory.createLabel(compositeRemote, "API URL:");
        remoteUrlText = SWTFactory.createText(compositeRemote);
        SWTFactory.createEditButton(compositeRemote, remoteUrlText);

        aiModeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.syncModelWithUI();
                refreshUI();
            }
        });

        aiRemoteCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String providerName = aiRemoteCombo.getText();
                eu.kalafatic.evolution.controller.security.TokenSecurityService.ResolvedProvider resolved =
                        eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().resolve(orchestrator, providerName);
                if (resolved != null) {
                    remoteUrlText.setText(resolved.url != null ? resolved.url : "");
                    remoteTokenText.setText(resolved.token != null ? resolved.token : "");
                    page.syncModelWithUI();
                }
            }
        });

        remoteTokenText.addModifyListener(e -> page.syncModelWithUI());
        remoteUrlText.addModifyListener(e -> page.syncModelWithUI());
    }
    


	private Combo selectModel(Composite parent) {
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);	
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// selection listener
		combo.addListener(SWT.Selection, e -> {
		    int index = combo.getSelectionIndex();
		    if (index >= 0) {
			String selectedName = combo.getItem(index);
		        System.out.println("Selected: " + selectedName);
		        page.syncModelWithUI();
		    }
		});
		return combo;
	}

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            AiMode mode = orchestrator.getAiMode();
            aiModeCombo.select(mode.getValue());

            // 1. Populate AI Remote combo (contains all non-local models)
            String currentRemote = aiRemoteCombo.getText();
            aiRemoteCombo.removeAll();
            List<String> remoteModels = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.REMOTE);
            for (String n : remoteModels) aiRemoteCombo.add(n);

            if (!currentRemote.isEmpty()) {
                int idx = aiRemoteCombo.indexOf(currentRemote);
                if (idx >= 0) aiRemoteCombo.select(idx);
            }

            String remoteModel = orchestrator.getRemoteModel();
            if (remoteModel == null || remoteModel.isEmpty()) {
                remoteModel = "deepseek";
            }
            if (remoteModel != null) {
                int index = aiRemoteCombo.indexOf(remoteModel);
                if (index >= 0) {
                    aiRemoteCombo.select(index);
                } else {
                    aiRemoteCombo.add(remoteModel);
                    aiRemoteCombo.select(aiRemoteCombo.indexOf(remoteModel));
                }
            }

            eu.kalafatic.evolution.controller.security.TokenSecurityService.ResolvedProvider resolved =
                    eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().resolve(orchestrator, aiRemoteCombo.getText());

            remoteTokenText.setText((resolved != null && resolved.token != null) ? resolved.token : "");
            remoteUrlText.setText((resolved != null && resolved.url != null) ? resolved.url : "");

            // 2. Populate Model combo (filtered by mode)
            if (localModelCombo != null) {
                String currentLocal = localModelCombo.getText();
                localModelCombo.removeAll();

                List<String> modelsToShow;
                if (mode == AiMode.PROXY) {
                    modelsToShow = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.PROXY);
                } else {
                    modelsToShow = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.LOCAL, AiMode.HYBRID);
                }

                for (String n : modelsToShow) localModelCombo.add(n);

                if (!currentLocal.isEmpty()) {
                    int idx = localModelCombo.indexOf(currentLocal);
                    if (idx >= 0) localModelCombo.select(idx);
                }

                String model = orchestrator.getLocalModel();

                if (model == null || model.isEmpty()) {
                    if (orchestrator.getOllama() != null) model = orchestrator.getOllama().getModel();
                }
                if (model != null) {
                    int idx = localModelCombo.indexOf(model);
                    if (idx >= 0) {
                        localModelCombo.select(idx);
                    } else {
                        localModelCombo.add(model);
                        localModelCombo.select(localModelCombo.indexOf(model));
                    }
                }
            }
        }
    }

    public int getAiModeIndex() { return aiModeCombo.getSelectionIndex(); }
    public String getLocalModel() { return localModelCombo != null ? localModelCombo.getText() : ""; }
    public String getRemoteModel() { return aiRemoteCombo.getText(); }
    public String getRemoteToken() { return remoteTokenText.getText(); }
    public String getRemoteUrl() { return remoteUrlText.getText(); }
    public Composite getRemoteComposite() { return compositeRemote; }

    public void setRemoteToken(String token) {
	remoteTokenText.setText(token);
    }

    private void handleDetailedConfig() {
        if (orchestrator == null) return;
        String providerName = aiRemoteCombo.getText();

        // Find existing or create temporary provider
        eu.kalafatic.evolution.model.orchestration.AIProvider provider = orchestrator.getAiProviders().stream()
                .filter(p -> p.getName().equalsIgnoreCase(providerName))
                .findFirst().orElse(null);

        boolean isNew = false;
        if (provider == null) {
            provider = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createAIProvider();
            provider.setName(providerName);
            ProviderConfig config = AiProviders.PROVIDERS.get(providerName.toLowerCase());
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
