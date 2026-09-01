package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.LlamaService;
import eu.kalafatic.evolution.controller.manager.ModelSizePreset;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.llm.OllamaProvider;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.HTTPUtils;
import org.eclipse.jface.window.Window;
import eu.kalafatic.evolution.view.dialogs.ForgeSettingsDialog;
import eu.kalafatic.evolution.view.dialogs.MediatedTargetDialog;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.editors.pages.properties.ModelDetailsDialog;
import eu.kalafatic.utils.factories.GUIFactory;

public class ChatMgmtGroup2 extends AEvoGroup {
    private Combo sessionCombo;
    private AiChatPage page;

    private Combo aiModeCombo;
    private Combo aiRemoteCombo;
    private Combo localModelCombo;
    private Combo inferenceEngineCombo;
    private Text remoteTokenText, remoteUrlText;
    private Composite compositeLocal, compositeRemote;
    private Button forgeSettingsBtn;

    public ChatMgmtGroup2(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, AiChatPage page) {
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
       
        Composite sessionsComp = GUIFactory.INSTANCE.createComposite(group, 6, SWT.BORDER);

        Button newSessionButton = GUIFactory.INSTANCE.createButton(sessionsComp, "New Session");
        newSessionButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.createNewSession();
            }
        });
       
        
        sessionCombo = GUIFactory.INSTANCE.createCombo(sessionsComp);
        ((GridData)sessionCombo.getLayoutData()).widthHint = 100;
        
        sessionCombo.add(page.getCurrentSessionName());
        sessionCombo.select(0);
        sessionCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.switchSession(sessionCombo.getText());
            }
        });       
        
       
        Button byDateButton = GUIFactory.INSTANCE.createButton(sessionsComp, "By Date");
        byDateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.selectSessionByDate();
            }
        });
        
        Button cleanButton = GUIFactory.INSTANCE.createButton(sessionsComp, "Clean");
        cleanButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.cleanChat();
            }
        });

        Button saveButton = GUIFactory.INSTANCE.createButton(sessionsComp, "Save");
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.saveChatToFile();
            }
        });

        Button copyAllButton = GUIFactory.INSTANCE.createButton(sessionsComp, "Copy All");
        copyAllButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.copyConversationToClipboard();
            }
        });
        
        compositeRemote = GUIFactory.INSTANCE.createComposite(group, 3, SWT.BORDER);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, true);
        gd.verticalSpan = 2;
        compositeRemote.setLayoutData(gd);
        
        GUIFactory.INSTANCE.createLabel(compositeRemote, "AI Remote:");
        aiRemoteCombo = GUIFactory.INSTANCE.createCombo(compositeRemote);
        ((GridData)aiRemoteCombo.getLayoutData()).widthHint = 100;

        Button connectionButtonRemote = GUIFactory.INSTANCE.createButton(compositeRemote, "Test Connection");
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
 

        GUIFactory.INSTANCE.createLabel(compositeRemote, "Token:");
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

        GUIFactory.INSTANCE.createLabel(compositeRemote, "API URL:");
        remoteUrlText = GUIFactory.INSTANCE.createText(compositeRemote);
        GUIFactory.INSTANCE.createEditButton(compositeRemote, remoteUrlText);        

        // AI Settings part (merged)
        compositeLocal = GUIFactory.INSTANCE.createComposite(group, 4, SWT.BORDER);
        compositeLocal.setBackground(lightGreen);
        
        GUIFactory.INSTANCE.createLabel(compositeLocal, "AI Mode:", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        aiModeCombo = GUIFactory.INSTANCE.createCombo(compositeLocal, AiMode.values());
        ((GridData)aiModeCombo.getLayoutData()).widthHint = 100;
        
        forgeSettingsBtn = GUIFactory.INSTANCE.createButton(compositeLocal, "Forge Settings");
        forgeSettingsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openForgeSettingsDialog();
            }
        });
        
        Button targetButton = GUIFactory.INSTANCE.createButton(compositeLocal, "Target");
        targetButton.setBackground(lightOrange);
        
        targetButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ChatSession session = page.getCurrentSession();
                if (session != null) {
                   MediatedTargetDialog dlg = new MediatedTargetDialog(page.getShell(), session, page.getProjectRoot(), editor);
                    dlg.open();
                }
            }
        });

        GUIFactory.INSTANCE.createLabel(compositeLocal, "Model:", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        localModelCombo = selectModel(compositeLocal);
        ((GridData)localModelCombo.getLayoutData()).widthHint = 100;
        //((GridData)localModelCombo.getLayoutData()).horizontalSpan = 2; // Merges across 3 columns
        
        Button identifyButton = GUIFactory.INSTANCE.createButton(compositeLocal, "Identify LLM");
        identifyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.identifyLlmAndProcess();
            }
        });

        
        Button connectionButton = GUIFactory.INSTANCE.createButton(compositeLocal, "Test Connection");
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

        GUIFactory.INSTANCE.createLabel(compositeLocal, "Engine:", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        inferenceEngineCombo = selectEngine(compositeLocal);
        ((GridData)inferenceEngineCombo.getLayoutData()).widthHint = 100;
        ((GridData)inferenceEngineCombo.getLayoutData()).horizontalSpan = 3;

        aiModeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {            	
            	AiMode aiMode = AiMode.get(aiModeCombo.getSelectionIndex());
            	
            	ProjectModelManager.getInstance().updateAiMode(orchestrator, aiMode);            	
               
            	Map<String, Object> settings = new HashMap<>();
                settings.put("aiMode", aiModeCombo.getSelectionIndex());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
                page.updateModeDisplay();
                
                if (aiMode == AiMode.FORGE) {
                    openForgeSettingsDialog();
                }
            }
        });

        aiRemoteCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Map<String, Object> settings = new HashMap<>();
                settings.put("remoteModel", aiRemoteCombo.getText());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
                page.updateModeDisplay();
            }
        });

        remoteTokenText.addModifyListener(e -> {
            if (!isUpdating) {
                Map<String, Object> settings = new HashMap<>();
                settings.put("token_" + aiRemoteCombo.getText(), remoteTokenText.getText());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
            }
        });
        remoteUrlText.addModifyListener(e -> {
            if (!isUpdating) {
                Map<String, Object> settings = new HashMap<>();
                settings.put("url_" + aiRemoteCombo.getText(), remoteUrlText.getText());
                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
            }
        });
        group.layout(true, true);
    }

    private Combo selectModel(Composite parent) {
        Combo combo = GUIFactory.INSTANCE.createCombo(parent);
        // selection listener
        combo.addListener(SWT.Selection, e -> {
            int index = combo.getSelectionIndex();
            if (index >= 0) {
                Map<String, Object> settings = new HashMap<>();
                String selectedModel = combo.getText();
                settings.put("localModel", selectedModel);

                String engine = LlamaService.detectInferenceEngine(selectedModel);
                settings.put("inferenceEngine", engine);
                if (inferenceEngineCombo != null && !inferenceEngineCombo.isDisposed()) {
                    selectSafe(inferenceEngineCombo, engine);
                }

                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
                page.updateModeDisplay();
                page.updateStatusInfo();
            }
        });
        return combo;
    }

    private Combo selectEngine(Composite parent) {
        Combo combo = GUIFactory.INSTANCE.createCombo(parent, new String[] { "ollama", "llama-cpp", "evo native" });
        combo.addListener(SWT.Selection, e -> {
            int index = combo.getSelectionIndex();
            if (index >= 0) {
                String selectedEngine = combo.getText();
                Map<String, Object> settings = new HashMap<>();
                settings.put("inferenceEngine", selectedEngine);

                if (localModelCombo != null && !localModelCombo.isDisposed()) {
                    AiMode mode = AiMode.get(aiModeCombo.getSelectionIndex());
                    List<String> rawModels;
                    if (mode == AiMode.PROXY) {
                        rawModels = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.PROXY);
                    } else if (mode == AiMode.MEDIATED || mode == AiMode.INTENT) {
                        rawModels = ProjectModelManager.getInstance().getLlmModels(orchestrator, mode);
                    } else {
                        rawModels = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.LOCAL, AiMode.HYBRID);
                    }
                    List<String> filtered = ProjectModelManager.getInstance().filterModelsByEngine(rawModels, selectedEngine);
                    String currentSelected = localModelCombo.getText();
                    String[] newItems = filtered.toArray(new String[0]);
                    localModelCombo.setItems(newItems);
                    if (!currentSelected.isEmpty() && filtered.contains(currentSelected)) {
                        selectSafe(localModelCombo, currentSelected);
                    } else if (newItems.length > 0) {
                        localModelCombo.select(0);
                        settings.put("localModel", localModelCombo.getText());
                    } else {
                        localModelCombo.setText("");
                        settings.put("localModel", "");
                    }
                }

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
                if (!Arrays.equals(aiRemoteCombo.getItems(), newRemoteItems)) {
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

                // 3. Populate/select Inference Engine combo first
                if (inferenceEngineCombo != null) {
                    String selectedModel = localModelCombo != null ? localModelCombo.getText() : "";
                    String engine = (String) config.get("inferenceEngine");
                    if (engine == null || engine.isEmpty()) {
                        engine = LlamaService.detectInferenceEngine(selectedModel);
                    }
                    selectSafe(inferenceEngineCombo, engine);
                }

                // 2. Populate Model combo based on active inference engine
                if (localModelCombo != null) {
                    String currentLocal = localModelCombo.getText();

                    List<String> rawModels;
                    if (mode == AiMode.PROXY) {
                        rawModels = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.PROXY);
                    } else if (mode == AiMode.MEDIATED || mode == AiMode.INTENT) {
                        rawModels = ProjectModelManager.getInstance().getLlmModels(orchestrator, mode);
                    } else {
                        rawModels = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.LOCAL,
                                AiMode.HYBRID);
                    }

                    String currentEngine = inferenceEngineCombo != null ? inferenceEngineCombo.getText() : "ollama";
                    List<String> modelsToShow = ProjectModelManager.getInstance().filterModelsByEngine(rawModels, currentEngine);

                    String[] newLocalItems = modelsToShow.toArray(new String[0]);
                    if (!Arrays.equals(localModelCombo.getItems(), newLocalItems)) {
                        localModelCombo.setItems(newLocalItems);
                        if (!currentLocal.isEmpty() && modelsToShow.contains(currentLocal)) {
                            selectSafe(localModelCombo, currentLocal);
                        } else if (newLocalItems.length > 0) {
                            localModelCombo.select(0);
                        }
                    }

                    String model = (String) config.getOrDefault("localModel", session != null && session.getLocalModel() != null ? session.getLocalModel() : (orchestrator != null ? orchestrator.getLocalModel() : ""));
                    if (model != null && modelsToShow.contains(model)) {
                        selectSafe(localModelCombo, model);
                    }
                }
               
            } finally {
                isUpdating = false;
            }
        }
        group.layout(true, true);
    }

    public void openForgeSettingsDialog() {
        String currentSessionId = page.getCurrentSessionName();
        eu.kalafatic.evolution.view.projection.RuntimeProjection projection = eu.kalafatic.evolution.view.projection.ProjectionService.getInstance().getProjection(currentSessionId);
        Map<String, Object> config = projection.getConfiguration();

        String currentSize = null;
        int currentEpochs = -1;
        String currentLossThresh = null;
        double[] lossHistory = null;

        // 1. Try RuntimeProjection config
        if (config != null) {
            if (config.get("modelSize") != null) currentSize = String.valueOf(config.get("modelSize"));
            if (config.get("epochs") != null) {
                try { currentEpochs = Integer.parseInt(String.valueOf(config.get("epochs"))); } catch (Exception e) {}
            }
            if (config.get("lossThreshold") != null) currentLossThresh = String.valueOf(config.get("lossThreshold"));

            Object lhObj = config.get("lossHistory");
            if (lhObj instanceof String && !((String) lhObj).isEmpty()) {
                try {
                    org.json.JSONArray arr = new org.json.JSONArray((String) lhObj);
                    lossHistory = new double[arr.length()];
                    for (int i = 0; i < arr.length(); i++) lossHistory[i] = arr.getDouble(i);
                } catch (Exception ex) {}
            } else if (lhObj instanceof double[]) {
                lossHistory = (double[]) lhObj;
            }
        }

        // 2. Try ForgeSessionManager uiState
        try {
            Class<?> fsmClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager");
            Object instance = fsmClass.getMethod("getInstance").invoke(null);
            org.json.JSONObject uiState = (org.json.JSONObject) fsmClass.getMethod("getUiState", String.class).invoke(instance, currentSessionId);
            if (uiState != null) {
                if ((currentSize == null || currentSize.isEmpty()) && uiState.has("modelSize")) currentSize = uiState.getString("modelSize");
                if (currentEpochs <= 0 && uiState.has("epochs")) currentEpochs = uiState.getInt("epochs");
                if ((currentLossThresh == null || currentLossThresh.isEmpty()) && uiState.has("lossThreshold")) currentLossThresh = uiState.getString("lossThreshold");
                if (lossHistory == null && uiState.has("lossHistory")) {
                    String lhStr = uiState.getString("lossHistory");
                    if (lhStr != null && !lhStr.isEmpty()) {
                        org.json.JSONArray arr = new org.json.JSONArray(lhStr);
                        lossHistory = new double[arr.length()];
                        for (int i = 0; i < arr.length(); i++) lossHistory[i] = arr.getDouble(i);
                    }
                }
            }
        } catch (Exception ex) {}

        // 3. Try Orchestrator ForgeSessions
        if (orchestrator != null && orchestrator.getForgeSessions() != null) {
            for (eu.kalafatic.evolution.model.orchestration.ForgeSession fs : orchestrator.getForgeSessions()) {
                if (fs.getModelState() != null && fs.getModelState().getHyperparameters() != null) {
                    String hpStr = fs.getModelState().getHyperparameters();
                    if (hpStr != null && !hpStr.isEmpty() && !hpStr.equals("{}")) {
                        try {
                            org.json.JSONObject hpJson = new org.json.JSONObject(hpStr);
                            if ((currentSize == null || currentSize.isEmpty()) && hpJson.has("modelSize")) currentSize = hpJson.getString("modelSize");
                            if (currentEpochs <= 0 && hpJson.has("epochs")) currentEpochs = hpJson.getInt("epochs");
                            if ((currentLossThresh == null || currentLossThresh.isEmpty()) && hpJson.has("lossThreshold")) currentLossThresh = hpJson.getString("lossThreshold");
                        } catch (Exception ex) {}
                    }
                }
            }
        }

        // 4. Try IDialogSettings fallback
        if (eu.kalafatic.evolution.view.application.Activator.getDefault() != null) {
            org.eclipse.jface.dialogs.IDialogSettings settings = eu.kalafatic.evolution.view.application.Activator.getDefault().getDialogSettings();
            if (settings != null) {
                org.eclipse.jface.dialogs.IDialogSettings section = settings.getSection("AiChatSettings");
                if (section != null) {
                    if ((currentSize == null || currentSize.isEmpty()) && section.get("modelSize") != null) currentSize = section.get("modelSize");
                    if ((currentSize == null || currentSize.isEmpty()) && section.get("ModelSize") != null) currentSize = section.get("ModelSize");
                    if (currentEpochs <= 0 && section.get("epochs") != null) {
                        try { currentEpochs = Integer.parseInt(section.get("epochs")); } catch (Exception e) {}
                    }
                    if ((currentLossThresh == null || currentLossThresh.isEmpty()) && section.get("lossThreshold") != null) currentLossThresh = section.get("lossThreshold");
                }
            }
        }

        // Defaults
        if (currentSize == null || currentSize.isEmpty()) currentSize = "SMALL";
        if (currentEpochs <= 0) currentEpochs = 32;
        if (currentLossThresh == null || currentLossThresh.isEmpty()) currentLossThresh = "Epoch 16-30: Loss 2-5 → Learning phrases";

        ForgeSettingsDialog dlg = new ForgeSettingsDialog(page.getShell(), currentSize, currentEpochs, currentLossThresh, lossHistory);
        if (dlg.open() == Window.OK) {
            String selectedSize = dlg.getSelectedModelSize();
            int epochs = dlg.getEpochs();
            String lossThreshold = dlg.getLossThreshold();

            Map<String, Object> settings = new HashMap<>();
            settings.put("modelSize", selectedSize);
            settings.put("epochs", epochs);
            settings.put("lossThreshold", lossThreshold);
            page.updateConfiguration(settings);
            page.saveLastUsedSettings();

            try {
                Class<?> fsmClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager");
                Object instance = fsmClass.getMethod("getInstance").invoke(null);
                fsmClass.getMethod("updateUiState", String.class, String.class, Object.class).invoke(instance, currentSessionId, "modelSize", selectedSize);
                fsmClass.getMethod("updateUiState", String.class, String.class, Object.class).invoke(instance, currentSessionId, "epochs", epochs);
                fsmClass.getMethod("updateUiState", String.class, String.class, Object.class).invoke(instance, currentSessionId, "lossThreshold", lossThreshold);
            } catch (Exception ex) {
                // Ignore if controller not loaded
            }

            if (orchestrator != null && orchestrator.getForgeSessions() != null) {
                for (eu.kalafatic.evolution.model.orchestration.ForgeSession fs : orchestrator.getForgeSessions()) {
                    if (currentSessionId.equals(fs.getSessionId()) || orchestrator.getForgeSessions().size() == 1) {
                        if (fs.getModelState() != null) {
                            org.json.JSONObject hpJson = new org.json.JSONObject();
                            String hpStr = fs.getModelState().getHyperparameters();
                            if (hpStr != null && !hpStr.isEmpty() && !hpStr.equals("{}")) {
                                try { hpJson = new org.json.JSONObject(hpStr); } catch (Exception ex) {}
                            }
                            hpJson.put("modelSize", selectedSize);
                            hpJson.put("epochs", epochs);
                            hpJson.put("lossThreshold", lossThreshold);
                            fs.getModelState().setHyperparameters(hpJson.toString());
                            fs.setLastModified(System.currentTimeMillis());
                        }
                    }
                }
            }
        }
    }

    public String getSelectedModelSize() {
        if (page != null && page.getCurrentSessionName() != null) {
            eu.kalafatic.evolution.view.projection.RuntimeProjection projection = eu.kalafatic.evolution.view.projection.ProjectionService.getInstance().getProjection(page.getCurrentSessionName());
            if (projection != null && projection.getConfiguration() != null) {
                String size = (String) projection.getConfiguration().get("modelSize");
                if (size != null && !size.isEmpty()) {
                    return size;
                }
            }
        }
        return "SMALL";
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
        if (!Arrays.equals(sessionCombo.getItems(), threads)) {
            sessionCombo.setItems(threads);
        }
        selectSafe(sessionCombo, current);
    }

	public Combo getAiModeCombo() {
		return aiModeCombo;
	}

    public Combo getInferenceEngineCombo() {
        return inferenceEngineCombo;
    }

    public String getInferenceEngine() {
        return inferenceEngineCombo != null ? inferenceEngineCombo.getText() : "ollama";
    }
}
