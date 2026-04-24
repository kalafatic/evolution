package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

import eu.kalafatic.evolution.controller.manager.OllamaConfigManager;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.providers.AiProviders;
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
    private Text remoteTokenText, remoteUrlText;
    private Composite compositeRemote;
    private AiChatPage page;

    public AiSettingsGroup(FormToolkit toolkit, Composite parent, AiChatPage page, Orchestrator orchestrator) {
        super(page.getEditor(), orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "AI Settings", 3, false);

        SWTFactory.createLabel(group, "AI Mode:");
        aiModeCombo = SWTFactory.createCombo(group);
        for (AiMode mode : AiMode.values()) {
            aiModeCombo.add(mode.getName());
        }        
        if (AiMode.LOCAL.equals(AiMode.get(aiModeCombo.getSelectionIndex()))) {
        	new Label(group, SWT.NONE).setText("Select Model:");	
        	
        	selectModel(group);

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
		List<OllamaModel> models = OllamaConfigManager.loadModels(); // Load models to populate the combo
		
		Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);	
		combo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		if (models != null) {
			Set<String> uniqueModels = new LinkedHashSet<>();
		    for (OllamaModel f : models) {
			uniqueModels.add(f.getName());
		    }
		    for (String name : uniqueModels) {
			combo.add(name);
		    }
		}

		// selection listener
		combo.addListener(SWT.Selection, e -> {
		    int index = combo.getSelectionIndex();
		    if (index >= 0) {
			String selectedName = combo.getItem(index);
			//modelText.setText(selectedName);
		        System.out.println("Selected: " + selectedName);
		    }
		});
		return combo;
	}

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            aiModeCombo.select(orchestrator.getAiMode().getValue());

            // Populate AI Remote combo with model and static providers
            String current = aiRemoteCombo.getText();
            aiRemoteCombo.removeAll();
            java.util.Set<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            names.addAll(AiProviders.PROVIDERS.keySet());
            if (orchestrator.getAiProviders() != null) {
                for (eu.kalafatic.evolution.model.orchestration.AIProvider p : orchestrator.getAiProviders()) {
                    names.add(p.getName());
                }
            }
            for (String n : names) aiRemoteCombo.add(n);
            if (!current.isEmpty()) {
                int idx = aiRemoteCombo.indexOf(current);
                if (idx >= 0) aiRemoteCombo.select(idx);
            }

            String remoteModel = orchestrator.getRemoteModel();
            if (remoteModel == null || remoteModel.isEmpty()) {
                remoteModel = "deepseek";
            }
            if (remoteModel != null) {
                int index = aiRemoteCombo.indexOf(remoteModel);
                if (index >= 0) aiRemoteCombo.select(index);
            }

            eu.kalafatic.evolution.controller.security.TokenSecurityService.ResolvedProvider resolved =
                    eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().resolve(orchestrator, remoteModel);

            remoteTokenText.setText((resolved != null && resolved.token != null) ? resolved.token : "");
            remoteUrlText.setText((resolved != null && resolved.url != null) ? resolved.url : "");

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
