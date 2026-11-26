package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.util.ArrayList;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.LlamaService;
import eu.kalafatic.evolution.controller.manager.ModelSizePreset;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.llm.LocalModelDescriptor;
import eu.kalafatic.evolution.controller.orchestration.llm.OllamaProvider;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.ChatSession;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.HTTPUtils;
import eu.kalafatic.utils.log.Log;
import org.eclipse.jface.window.Window;
import eu.kalafatic.evolution.view.dialogs.ForgeSettingsDialog;
import eu.kalafatic.evolution.view.dialogs.MediatedTargetDialog;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.AiCommanderPage;
import eu.kalafatic.evolution.view.editors.pages.properties.ModelDetailsDialog;
import eu.kalafatic.utils.factories.GUIFactory;

public class ChatMgmtGroup2 extends AEvoGroup {
    private Combo sessionCombo;
    private AiCommanderPage page;

    private Combo aiModeCombo;
    private Combo aiRemoteCombo;
    private Combo localModelCombo;
    private Combo inferenceEngineCombo;
    private Text remoteTokenText, remoteUrlText;
    private Composite compositeLocal, compositeRemote;
    private Button forgeSettingsBtn;
    private Button refreshModelsBtn;
    private Label modelStatusLabel;

    /**
     * Discovered models parallel to the {@link #localModelCombo} items: the combo
     * displays the origin-prefixed labels while this list keeps the actual
     * inference identifiers and origins.
     */
    private volatile List<LocalModelDescriptor> currentModelDescriptors = new ArrayList<>();

    /**
     * Monotonic token for provider discovery requests. Results are applied only when
     * the token still matches, so a stale asynchronous result can never overwrite a
     * newer provider selection.
     */
    private volatile long discoveryGeneration = 0;

    /**
     * Discovery key (provider@session) of the last requested discovery, used to
     * avoid re-running discovery on every periodic UI refresh.
     */
    private volatile String lastDiscoveryKey = "";

    public ChatMgmtGroup2(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, AiCommanderPage page) {
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
        compositeLocal = GUIFactory.INSTANCE.createComposite(group, 5, SWT.BORDER);
        compositeLocal.setBackground(lightGreen);
        
        GUIFactory.INSTANCE.createLabel(compositeLocal, "AI Mode:", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        aiModeCombo = GUIFactory.INSTANCE.createCombo(compositeLocal, AiMode.values());
        ((GridData)aiModeCombo.getLayoutData()).widthHint = 100;
        GUIFactory.INSTANCE.createLabel(compositeLocal);
        
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
        
        GUIFactory.INSTANCE.createLabel(compositeLocal, "Provider:", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        inferenceEngineCombo = selectEngine(compositeLocal);
        ((GridData)inferenceEngineCombo.getLayoutData()).widthHint = 100;
        //((GridData)inferenceEngineCombo.getLayoutData()).horizontalSpan = 2;

        GUIFactory.INSTANCE.createLabel(compositeLocal);GUIFactory.INSTANCE.createLabel(compositeLocal);GUIFactory.INSTANCE.createLabel(compositeLocal);
        
        GUIFactory.INSTANCE.createLabel(compositeLocal, "Model:", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        localModelCombo = selectModel(compositeLocal);
        ((GridData)localModelCombo.getLayoutData()).widthHint = 100;

        refreshModelsBtn = GUIFactory.INSTANCE.createButton(compositeLocal, "Refresh");
        refreshModelsBtn.setToolTipText("Re-discover the models available through the selected provider");
        refreshModelsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String providerLabel = inferenceEngineCombo != null && !inferenceEngineCombo.isDisposed()
                        ? inferenceEngineCombo.getText() : "";
                refreshModelDiscovery(providerLabel, getLocalModelId());
            }
        });
        
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

                    String selectedEngine = inferenceEngineCombo != null && !inferenceEngineCombo.isDisposed()
                            ? inferenceEngineCombo.getText() : "";
                    String selectedModel = getLocalModel();
                    if (LlmRouter.isEnrEngine(selectedEngine)) {
                        testEnrConnection();
                    } else {
                        // Diagnostic probe through the Ollama integration; for the
                        // llama.cpp provider the Ollama probe communicates that the
                        // model is not an Ollama-registered model.
                        int testLLM = OllamaProvider.testLLM(orchestrator.getOllama().getUrl(), selectedModel);
                        String message = HTTPUtils.getMessage(testLLM);

					    MessageBox messageBox = new MessageBox(page.getShell(), SWT.ICON_INFORMATION | SWT.OK);
					    messageBox.setText("Response");
					    messageBox.setMessage(selectedEngine + " + " + selectedModel + " : " + message);
					    messageBox.open();
                    }
                    
                } else {
                    MessageBox messageBox = new MessageBox(page.getShell(), SWT.ICON_WARNING | SWT.OK);
                    messageBox.setText("Warning");
                    messageBox.setMessage("Orchestrator not loaded.");
                    messageBox.open();
                }
            }
        });

        modelStatusLabel = GUIFactory.INSTANCE.createLabel(compositeLocal, "", SWT.NONE, GUIFactory.BUTTON_WIDTH);
        GridData statusGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        statusGd.horizontalSpan = 5;
        modelStatusLabel.setLayoutData(statusGd);
        modelStatusLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY));
        setModelStatus("");

        aiModeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {            	
            	AiMode aiMode = AiMode.get(aiModeCombo.getSelectionIndex());
            	
            	ProjectModelManager.getInstance().updateAiMode(orchestrator, aiMode);
            	Log.log("[AI-CMD][MODE] Mode selected: " + aiMode.getName());
                
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
                LocalModelDescriptor descriptor = index < currentModelDescriptors.size()
                        ? currentModelDescriptors.get(index) : null;
                if (descriptor != null) {
                    // Store the actual inference identifier (provider-prefixed for
                    // ENR); the display label only carries the origin.
                    settings.put("localModel", descriptor.id());
                    Log.log("[AI-CMD][MODEL] Model selected: " + descriptor.id()
                            + " (origin=" + descriptor.origin() + ", engine=" + descriptor.engine()
                            + ", available=" + descriptor.available() + ")");
                } else {
                    settings.put("localModel", combo.getText());
                    Log.log("[AI-CMD][MODEL] Model selected (no descriptor): " + combo.getText());
                }

                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
                page.updateModeDisplay();
                page.updateStatusInfo();
            }
        });
        return combo;
    }

    /**
     * Maps a persisted engine/provider value onto one of the current provider combo
     * items (delegates to {@link ProviderModelSelectionSupport#normalizeProviderLabel}).
     */
    private static String normalizeEngineLabel(String value) {
        return ProviderModelSelectionSupport.normalizeProviderLabel(value);
    }

    private Combo selectEngine(Composite parent) {
        Combo combo = GUIFactory.INSTANCE.createCombo(parent, ProviderModelSelectionSupport.PROVIDER_ITEMS);
        combo.addListener(SWT.Selection, e -> {
            int index = combo.getSelectionIndex();
            if (index >= 0) {
                String selectedEngine = combo.getText();
                Map<String, Object> settings = new HashMap<>();
                settings.put("inferenceEngine", selectedEngine);
                Log.log("[AI-CMD][PROVIDER] Provider selected: " + selectedEngine
                        + " (key=" + ProviderModelSelectionSupport.providerKey(selectedEngine) + ")");

                page.updateConfiguration(settings);
                page.saveLastUsedSettings();
                page.updateModeDisplay();
                page.updateStatusInfo();

                // Provider selection is authoritative for model discovery.
                refreshModelDiscovery(selectedEngine, getLocalModelId());
            }
        });
        return combo;
    }

    /**
     * Re-discovers the models of the selected provider on a background thread and
     * applies the result on the UI thread. A monotonic generation token makes sure a
     * stale asynchronous result can never overwrite a newer provider selection.
     */
    private void refreshModelDiscovery(String providerLabel, String previousSelection) {
        if (providerLabel == null || providerLabel.isBlank()) {
            return;
        }
        final long generation = ++discoveryGeneration;
        final String key = ProviderModelSelectionSupport.providerKey(providerLabel);
        lastDiscoveryKey = providerLabel + "@" + (page != null ? page.getCurrentSessionName() : "");
        setModelStatus("Loading models from " + providerLabel + "...");
        Log.log("[AI-CMD][DISCOVERY] Provider discovery started: " + providerLabel
                + " (key=" + key + ", previous=" + previousSelection + ")");

        new Thread(() -> {
            try {
                List<LocalModelDescriptor> models = LlmRouter.getInstance().discoverModels(providerLabel, orchestrator);
                Display.getDefault().asyncExec(() -> {
                    if (generation != discoveryGeneration) {
                        Log.log("[AI-CMD][DISCOVERY] Discarding stale discovery result for "
                                + providerLabel + " (a newer provider selection exists)");
                        return;
                    }
                    applyDiscoveryResult(providerLabel, models, previousSelection, null);
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (generation != discoveryGeneration) {
                        return;
                    }
                    applyDiscoveryResult(providerLabel, null, previousSelection, ex);
                });
            }
        }, "AiCmd-ModelDiscovery").start();
    }

    /**
     * Applies a discovery result to the model combo on the UI thread.
     */
    private void applyDiscoveryResult(String providerLabel, List<LocalModelDescriptor> discovered,
            String previousSelection, Exception error) {
        if (localModelCombo == null || localModelCombo.isDisposed()) {
            return;
        }
        // Guard: the result must still belong to the currently selected provider.
        if (inferenceEngineCombo == null || inferenceEngineCombo.isDisposed()
                || !providerLabel.equals(inferenceEngineCombo.getText())) {
            Log.log("[AI-CMD][DISCOVERY] Discarding discovery result for " + providerLabel
                    + " (current provider is " + inferenceEngineCombo.getText() + ")");
            return;
        }

        if (error != null) {
            Log.log("[AI-CMD][DISCOVERY] Provider discovery FAILED for " + providerLabel
                    + ": " + error.getMessage());
            setModelStatus("Discovery failed: " + error.getMessage());
            // Keep the previously discovered entries so the page stays usable.
            return;
        }

        List<LocalModelDescriptor> models = ProviderModelSelectionSupport.dedupeById(discovered);
        currentModelDescriptors = models;

        String[] items = new String[models.size()];
        for (int i = 0; i < models.size(); i++) {
            items[i] = ProviderModelSelectionSupport.displayLabel(models.get(i));
        }
        localModelCombo.setItems(items);

        String selectedId;
        int selectedIndex = ProviderModelSelectionSupport.resolveSelectionIndex(models, previousSelection);
        if (selectedIndex >= 0) {
            localModelCombo.select(selectedIndex);
            selectedId = models.get(selectedIndex).id();
        } else if (!models.isEmpty()) {
            localModelCombo.select(0);
            selectedId = models.get(0).id();
        } else {
            localModelCombo.deselectAll();
            selectedId = "";
        }

        if (models.isEmpty()) {
            setModelStatus("No models available from " + providerLabel + ".");
            Log.log("[AI-CMD][DISCOVERY] Provider discovery empty for " + providerLabel);
        } else {
            setModelStatus(models.size() + " model(s) available from " + providerLabel + ".");
        }
        Log.log("[AI-CMD][DISCOVERY] Provider discovery applied: provider=" + providerLabel
                + ", models=" + models.size() + ", selected=" + selectedId);

        Map<String, Object> settings = new HashMap<>();
        settings.put("localModel", selectedId);
        page.updateConfiguration(settings);
        page.saveLastUsedSettings();
        page.updateModeDisplay();
        page.updateStatusInfo();
    }

    private void setModelStatus(String text) {
        if (modelStatusLabel == null || modelStatusLabel.isDisposed()) {
            return;
        }
        if (!modelStatusLabel.getText().equals(text == null ? "" : text)) {
            modelStatusLabel.setText(text == null ? "" : text);
        }
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
                Log.log("[AI-CMD][MODE] Mode restored: " + mode.getName());

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

                // 2. Restore the provider selection (authoritative for model discovery)
                String providerLabel = "";
                if (inferenceEngineCombo != null) {
                    String engine = (String) config.get("inferenceEngine");
                    if (engine == null || engine.isEmpty()) {
                        engine = LlamaService.detectInferenceEngine("");
                    }
                    providerLabel = normalizeEngineLabel(engine);
                    if (!Arrays.asList(ProviderModelSelectionSupport.PROVIDER_ITEMS).contains(providerLabel)) {
                        providerLabel = ProviderModelSelectionSupport.PROVIDER_OLLAMA;
                    }
                    selectSafe(inferenceEngineCombo, providerLabel);
                }

                // 3. Repopulate the Model combo from the selected provider's own
                // discovery mechanism. Runs asynchronously; the generation token
                // guards against stale results when the provider changes again.
                // Rediscovery only runs when the provider/session changed or the
                // combo is still empty — periodic UI refreshes reuse the current list
                // (the Refresh button forces a fresh discovery).
                String previousModel = (String) config.getOrDefault("localModel", session != null && session.getLocalModel() != null ? session.getLocalModel() : (orchestrator != null ? orchestrator.getLocalModel() : ""));
                String discoveryKey = providerLabel + "@" + page.getCurrentSessionName();
                boolean needsDiscovery = !discoveryKey.equals(lastDiscoveryKey)
                        || (localModelCombo != null && localModelCombo.getItemCount() == 0);
                if (!providerLabel.isEmpty() && needsDiscovery) {
                    refreshModelDiscovery(providerLabel, previousModel);
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
        double currentDesiredLoss = -1.0;
        double[] lossHistory = null;
        String datasetsJson = null;

        // 1. Try RuntimeProjection config
        if (config != null) {
            if (config.get("modelSize") != null) currentSize = String.valueOf(config.get("modelSize"));
            if (config.get("epochs") != null) {
                try { currentEpochs = Integer.parseInt(String.valueOf(config.get("epochs"))); } catch (Exception e) {}
            }
            if (config.get("lossThreshold") != null) currentLossThresh = String.valueOf(config.get("lossThreshold"));
            if (config.get("desiredLossThreshold") != null) {
                try { currentDesiredLoss = Double.parseDouble(String.valueOf(config.get("desiredLossThreshold"))); } catch (Exception e) {}
            }
            if (config.get("datasets") != null) {
                datasetsJson = String.valueOf(config.get("datasets"));
            }

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
                if (currentDesiredLoss <= 0.0 && uiState.has("desiredLossThreshold")) currentDesiredLoss = uiState.getDouble("desiredLossThreshold");
                if ((datasetsJson == null || datasetsJson.isEmpty()) && uiState.has("datasets")) datasetsJson = uiState.get("datasets").toString();
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
                            if (currentDesiredLoss <= 0.0 && hpJson.has("desiredLossThreshold")) currentDesiredLoss = hpJson.getDouble("desiredLossThreshold");
                            if ((datasetsJson == null || datasetsJson.isEmpty()) && hpJson.has("datasets")) datasetsJson = hpJson.get("datasets").toString();
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
                    if (currentDesiredLoss <= 0.0 && section.get("desiredLossThreshold") != null) {
                        try { currentDesiredLoss = Double.parseDouble(section.get("desiredLossThreshold")); } catch (Exception e) {}
                    }
                    if ((datasetsJson == null || datasetsJson.isEmpty()) && section.get("datasets") != null) datasetsJson = section.get("datasets");
                }
            }
        }

        // Defaults
        if (currentSize == null || currentSize.isEmpty()) currentSize = "SMALL";
        if (currentEpochs <= 0) currentEpochs = 4;
        if (currentLossThresh == null || currentLossThresh.isEmpty()) currentLossThresh = "Epoch 16-30: Loss 2-5 → Learning phrases";
        if (currentDesiredLoss <= 0.0) currentDesiredLoss = 1.0;

        ForgeSettingsDialog dlg = new ForgeSettingsDialog(page.getShell(), currentSize, currentEpochs, currentLossThresh, currentDesiredLoss, lossHistory);
        if (datasetsJson != null && !datasetsJson.isEmpty()) {
            dlg.setDatasetsFromJson(datasetsJson);
        }

        if (dlg.open() == Window.OK) {
            org.json.JSONObject uiStateJson = dlg.getUiStateJson();

            Map<String, Object> settings = new HashMap<>();
            java.util.Iterator<String> keys = uiStateJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                settings.put(key, uiStateJson.get(key));
            }
            page.updateConfiguration(settings);
            page.saveLastUsedSettings();

            try {
                Class<?> fsmClass = Class.forName("eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager");
                Object instance = fsmClass.getMethod("getInstance").invoke(null);
                java.util.Iterator<String> keys2 = uiStateJson.keys();
                while (keys2.hasNext()) {
                    String key = keys2.next();
                    fsmClass.getMethod("updateUiState", String.class, String.class, Object.class).invoke(instance, currentSessionId, key, uiStateJson.get(key));
                }
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
                            java.util.Iterator<String> keys3 = uiStateJson.keys();
                            while (keys3.hasNext()) {
                                String key = keys3.next();
                                hpJson.put(key, uiStateJson.get(key));
                            }
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

    /**
     * The plain (display/diagnostic) name of the selected local model — the origin
     * prefix is stripped. Returns an empty string when nothing is selected.
     */
    public String getLocalModel() {
        return LocalModelDescriptor.plainName(getLocalModelId());
    }

    /**
     * The actual inference identifier of the selected local model (provider-prefixed
     * for ENR entries, e.g. {@code ollama:llama3.2:3b}).
     */
    public String getLocalModelId() {
        if (localModelCombo == null || localModelCombo.isDisposed()) {
            return "";
        }
        int index = localModelCombo.getSelectionIndex();
        if (index >= 0 && index < currentModelDescriptors.size()) {
            LocalModelDescriptor descriptor = currentModelDescriptors.get(index);
            if (descriptor != null) {
                return descriptor.id();
            }
        }
        return localModelCombo.getText();
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

    /**
     * Tests the connection through the selected ENR provider using the currently
     * selected model, so the diagnostic exercises the real inference path instead
     * of a hardcoded Ollama probe.
     */
    private void testEnrConnection() {
        MessageBox messageBox;
        String selectedModel = getLocalModelId();
        LlmRouter router = LlmRouter.getInstance();
        if (!router.isEnrAvailable()) {
            messageBox = new MessageBox(page.getShell(), SWT.ICON_WARNING | SWT.OK);
            messageBox.setText("Response");
            messageBox.setMessage("EVO Native ENR + " + selectedModel + " : ENR runtime is not registered.");
            messageBox.open();
            return;
        }
        try {
            String reply = router.sendEnrRequest(orchestrator, "Reply with exactly OK.", 0.2f, null, null);
            messageBox = new MessageBox(page.getShell(), SWT.ICON_INFORMATION | SWT.OK);
            messageBox.setText("Response");
            messageBox.setMessage("EVO Native ENR + " + selectedModel + " : "
                    + (reply != null && !reply.isBlank() ? HTTPUtils.getMessage(200) : "Empty response."));
        } catch (Exception ex) {
            Log.log("[AI-CMD][INFERENCE] ENR connection test failed for model " + selectedModel
                    + ": " + ex.getMessage());
            messageBox = new MessageBox(page.getShell(), SWT.ICON_ERROR | SWT.OK);
            messageBox.setText("Response");
            messageBox.setMessage("EVO Native ENR + " + selectedModel + " : " + ex.getMessage());
        }
        messageBox.open();
    }
}
