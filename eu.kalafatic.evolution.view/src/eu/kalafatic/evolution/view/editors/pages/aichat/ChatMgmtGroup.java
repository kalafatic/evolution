package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.llm.OllamaProvider;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.HTTPUtils;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.editors.pages.properties.ModelDetailsDialog;
import eu.kalafatic.utils.factories.GUIFactory;

public class ChatMgmtGroup extends AEvoGroup {
    private Combo sessionCombo;
    private AiChatPage page;

    private Combo aiModeCombo;
    private Combo aiRemoteCombo;
    private Combo localModelCombo;
    private Text remoteTokenText, remoteUrlText;
    private Composite compositeLocal, compositeRemote;

    public ChatMgmtGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, AiChatPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
        load();
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Chat Management", 2, true);
       
        Composite sessionsComp = GUIFactory.INSTANCE.createComposite(group, 7, SWT.BORDER);
        GridLayout sessionsLayout = (GridLayout) sessionsComp.getLayout();
        sessionsLayout.marginWidth = 8;
        sessionsLayout.marginHeight = 8;
        sessionsLayout.horizontalSpacing = 6;
        sessionsLayout.verticalSpacing = 6;

        GridData sessionsGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        sessionsGd.horizontalSpan = 2;
        sessionsComp.setLayoutData(sessionsGd);

        Button newSessionButton = GUIFactory.INSTANCE.createButton(sessionsComp, "New Session");
        GridData newSessionGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        newSessionGd.widthHint = 110;
        newSessionButton.setLayoutData(newSessionGd);
        newSessionButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.createNewSession();
            }
        });

        Button identifyButton = GUIFactory.INSTANCE.createButton(sessionsComp, "Identify LLM/Process");
        GridData identifyGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        identifyGd.widthHint = 150;
        identifyButton.setLayoutData(identifyGd);
        identifyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.identifyLlmAndProcess();
            }
        });
        
        sessionCombo = GUIFactory.INSTANCE.createCombo(sessionsComp);
        GridData sessionComboGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        sessionComboGd.widthHint = 100;
        sessionCombo.setLayoutData(sessionComboGd);
        sessionCombo.setVisibleItemCount(100);
        sessionCombo.add(page.getCurrentSessionName());
        sessionCombo.select(0);
        sessionCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.switchSession(sessionCombo.getText());
            }
        });

       
        Button byDateButton = GUIFactory.INSTANCE.createButton(sessionsComp, "By Date");
        GridData byDateGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        byDateGd.widthHint = 90;
        byDateButton.setLayoutData(byDateGd);
        byDateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.selectSessionByDate();
            }
        });
        
        Button cleanButton = GUIFactory.INSTANCE.createButton(sessionsComp, "Clean");
        GridData cleanGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        cleanGd.widthHint = 90;
        cleanButton.setLayoutData(cleanGd);
        cleanButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.cleanChat();
            }
        });

        Button saveButton = GUIFactory.INSTANCE.createButton(sessionsComp, "Save");
        GridData saveGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        saveGd.widthHint = 90;
        saveButton.setLayoutData(saveGd);
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.saveChatToFile();
            }
        });

        Button copyAllButton = GUIFactory.INSTANCE.createButton(sessionsComp, "Copy All");
        GridData copyAllGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        copyAllGd.widthHint = 90;
        copyAllButton.setLayoutData(copyAllGd);
        copyAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.copyConversationToClipboard();
            }
        });
        
        // AI Settings part (merged)
        compositeLocal = GUIFactory.INSTANCE.createComposite(group, 3, SWT.BORDER);
        compositeLocal.setBackground(lightGreen);
        GridLayout localLayout = (GridLayout) compositeLocal.getLayout();
        localLayout.marginWidth = 8;
        localLayout.marginHeight = 8;
        localLayout.horizontalSpacing = 6;
        localLayout.verticalSpacing = 6;

        GridData localGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        compositeLocal.setLayoutData(localGd);
        
        GUIFactory.INSTANCE.createLabel(compositeLocal, "AI Mode:", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        aiModeCombo = GUIFactory.INSTANCE.createCombo(compositeLocal, AiMode.values());
        GridData aiModeGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        aiModeGd.widthHint = 100;
        aiModeCombo.setLayoutData(aiModeGd);
        aiModeCombo.setVisibleItemCount(100);

        Button targetButton = GUIFactory.INSTANCE.createButton(compositeLocal, "Target");
        targetButton.setBackground(lightOrange);
        targetButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                eu.kalafatic.evolution.model.orchestration.ChatSession session = page.getCurrentSession();
                if (session != null) {
                    eu.kalafatic.evolution.view.dialogs.MediatedTargetDialog dlg = new eu.kalafatic.evolution.view.dialogs.MediatedTargetDialog(page.getShell(), session, page.getProjectRoot(), editor);
                    dlg.open();
                }
            }
        });

        GUIFactory.INSTANCE.createLabel(compositeLocal, "Model:", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        localModelCombo = selectModel(compositeLocal);
        
        Button connectionButton = GUIFactory.INSTANCE.createButton(compositeLocal, "Test Connection");
        GridData connectionButtonGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        connectionButtonGd.widthHint = 110;
        connectionButton.setLayoutData(connectionButtonGd);
        connectionButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (orchestrator != null) {        
                    int testLLM = OllamaProvider.testLLM(orchestrator.getOllama().getUrl(), localModelCombo.getText());
                    String message = HTTPUtils.getMessage(testLLM);
                    
                    MessageBox messageBox = new MessageBox(page.getShell(), SWT.ICON_INFORMATION | SWT.OK);
                    messageBox.setText("Response");
                    messageBox.setMessage("Ollama + " + localModelCombo.getText() + " : " + message);
                    messageBox.open();
                } else {
                    MessageBox messageBox = new MessageBox(page.getShell(), SWT.ICON_WARNING | SWT.OK);
                    messageBox.setText("Warning");
                    messageBox.setMessage("Orchestrator not loaded.");
                    messageBox.open();
                }
            }
        });

        compositeRemote = GUIFactory.INSTANCE.createComposite(group, 3, SWT.BORDER);
        GridLayout remoteLayout = (GridLayout) compositeRemote.getLayout();
        remoteLayout.marginWidth = 8;
        remoteLayout.marginHeight = 8;
        remoteLayout.horizontalSpacing = 6;
        remoteLayout.verticalSpacing = 6;

        GridData remoteGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        compositeRemote.setLayoutData(remoteGd);

        GUIFactory.INSTANCE.createLabel(compositeRemote, "AI Remote:", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        aiRemoteCombo = GUIFactory.INSTANCE.createCombo(compositeRemote);
        GridData aiRemoteGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        aiRemoteGd.widthHint = 100;
        aiRemoteCombo.setLayoutData(aiRemoteGd);
        aiRemoteCombo.setVisibleItemCount(100);

        Button connectionButtonRemote = GUIFactory.INSTANCE.createButton(compositeRemote, "Test Connection");
        GridData connectionButtonRemoteGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        connectionButtonRemoteGd.widthHint = 110;
        connectionButtonRemote.setLayoutData(connectionButtonRemoteGd);
        connectionButtonRemote.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                if (orchestrator != null) {
                    page.testAiConnectionRemote(aiModeCombo.getSelectionIndex(), aiRemoteCombo.getText(),
                            remoteTokenText.getText(), remoteUrlText.getText());
                } else {
                    MessageBox messageBox = new MessageBox(page.getShell(), SWT.ICON_WARNING | SWT.OK);
                    messageBox.setText("Warning");
                    messageBox.setMessage("Orchestrator not loaded.");
                    messageBox.open();
                }
            }
        });

        GUIFactory.INSTANCE.createLabel(compositeRemote, "Token:", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        remoteTokenText = GUIFactory.INSTANCE.createPasswordText(compositeRemote);
        Button editTokenBtn = GUIFactory.INSTANCE.createEditButton(compositeRemote, remoteTokenText);
        editTokenBtn.setText("\u2699"); // Gear icon
        editTokenBtn.setToolTipText("Detailed Configuration");
        editTokenBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDetailedConfig();
            }
        });

        GUIFactory.INSTANCE.createLabel(compositeRemote, "API URL:", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        remoteUrlText = GUIFactory.INSTANCE.createText(compositeRemote);
        GUIFactory.INSTANCE.createEditButton(compositeRemote, remoteUrlText);

        aiModeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("aiMode", aiModeCombo.getSelectionIndex());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
                page.updateModeDisplay();
            }
        });

        aiRemoteCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("remoteModel", aiRemoteCombo.getText());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
                page.updateModeDisplay();
            }
        });

        remoteTokenText.addModifyListener(e -> {
            if (!isUpdating) {
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("token_" + aiRemoteCombo.getText(), remoteTokenText.getText());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
            }
        });
        remoteUrlText.addModifyListener(e -> {
            if (!isUpdating) {
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("url_" + aiRemoteCombo.getText(), remoteUrlText.getText());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
            }
        });
    }

    private Combo selectModel(Composite parent) {
        Combo combo = GUIFactory.INSTANCE.createCombo(parent);
        GridData localModelGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        localModelGd.widthHint = 100;
        combo.setLayoutData(localModelGd);
        combo.setVisibleItemCount(100);
        // selection listener
        combo.addListener(SWT.Selection, e -> {
            int index = combo.getSelectionIndex();
            if (index >= 0) {
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("localModel", combo.getText());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
                page.updateModeDisplay();
                page.updateStatusInfo();
            }
        });
        return combo;
    }

    private boolean isUpdating = false;

    public void load(){
        if (!isUpdating) {
            isUpdating = true;
            try {
                eu.kalafatic.evolution.model.orchestration.ChatSession session = page.getCurrentSession();
                eu.kalafatic.evolution.view.projection.RuntimeProjection projection = eu.kalafatic.evolution.view.projection.ProjectionService.getInstance().getProjection(page.getCurrentSessionName());
                java.util.Map<String, Object> config = projection.getConfiguration();

                int modeVal = (int) config.getOrDefault("aiMode", session != null && session.getAiMode() != null ? session.getAiMode().getValue() : (orchestrator != null ? orchestrator.getAiMode().getValue() : 0));
                AiMode mode = AiMode.get(modeVal);
                if (aiModeCombo.getSelectionIndex() != mode.getValue()) {
                    aiModeCombo.select(mode.getValue());
                }

                // 1. Populate AI Remote combo
                String currentRemote = aiRemoteCombo.getText();
                List<String> remoteModels = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.REMOTE);
                String[] newRemoteItems = remoteModels.toArray(new String[0]);
                if (!java.util.Arrays.equals(aiRemoteCombo.getItems(), newRemoteItems)) {
                    aiRemoteCombo.setItems(newRemoteItems);
                    if (!currentRemote.isEmpty()) {
                        int idx = aiRemoteCombo.indexOf(currentRemote);
                        if (idx >= 0) aiRemoteCombo.select(idx);
                    }
                }

                String remoteModel = (String) config.getOrDefault("remoteModel", session != null && session.getRemoteModel() != null ? session.getRemoteModel() : (orchestrator != null ? orchestrator.getRemoteModel() : "deepseek"));
                if (remoteModel != null) {
                    selectSafe(aiRemoteCombo, remoteModel);
                }

                String token = (String) config.getOrDefault("token_" + aiRemoteCombo.getText(), "");
                String url = (String) config.getOrDefault("url_" + aiRemoteCombo.getText(), "");

                setTextSafe(remoteTokenText, token);
                setTextSafe(remoteUrlText, url);

                // 2. Populate Model combo
                if (localModelCombo != null) {
                    String currentLocal = localModelCombo.getText();

                    List<String> modelsToShow;
                    if (mode == AiMode.PROXY) {
                        modelsToShow = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.PROXY);
                    } else if (mode == AiMode.MEDIATED || mode == AiMode.INTENT) {
                        modelsToShow = ProjectModelManager.getInstance().getLlmModels(orchestrator, mode);
                    } else {
                        modelsToShow = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.LOCAL,
                                AiMode.HYBRID);
                    }

                    String[] newLocalItems = modelsToShow.toArray(new String[0]);
                    if (!java.util.Arrays.equals(localModelCombo.getItems(), newLocalItems)) {
                        localModelCombo.setItems(newLocalItems);
                        if (!currentLocal.isEmpty()) {
                            int idx = localModelCombo.indexOf(currentLocal);
                            if (idx >= 0) localModelCombo.select(idx);
                        }
                    }

                    String model = (String) config.getOrDefault("localModel", session != null && session.getLocalModel() != null ? session.getLocalModel() : (orchestrator != null ? orchestrator.getLocalModel() : ""));
                    if (model != null) {
                        selectSafe(localModelCombo, model);
                    }
                }
            } finally {
                isUpdating = false;
            }
        }
    }


    public int getAiModeIndex() {
        return aiModeCombo.getSelectionIndex();
    }

    public String getLocalModel() {
        return localModelCombo != null ? localModelCombo.getText() : "";
    }

    public String getRemoteModel() {
        return aiRemoteCombo.getText();
    }

    public String getRemoteToken() {
        return remoteTokenText.getText();
    }

    public String getRemoteUrl() {
        return remoteUrlText.getText();
    }

    public void setRemoteToken(String token) {
        setTextSafe(remoteTokenText, token);
    }

    private void handleDetailedConfig() {
        if (orchestrator == null)
            return;
        String providerName = aiRemoteCombo.getText();

        eu.kalafatic.evolution.model.orchestration.AIProvider provider = orchestrator.getAiProviders().stream()
                .filter(p -> p.getName().equalsIgnoreCase(providerName)).findFirst().orElse(null);

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
            scheduleRefresh();
        }
    }

    public void setSessionSelection(String sessionId) {
        if (sessionCombo.isDisposed()) return;
        selectSafe(sessionCombo, sessionId);
    }

    public void updateSessionCombo(String[] threads, String current) {
        if (sessionCombo.isDisposed()) return;
        if (!java.util.Arrays.equals(sessionCombo.getItems(), threads)) {
            sessionCombo.setItems(threads);
        }
        selectSafe(sessionCombo, current);
    }

	public Combo getAiModeCombo() {
		return aiModeCombo;
	}
}
