package eu.kalafatic.evolution.view.editors.pages.aichat;

import java.util.List;

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

import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
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
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Chat Management", 1, true);
       
        Composite sessionsComp = GUIFactory.INSTANCE.createComposite(group, 7, SWT.BORDER);

        Button newSessionButton = GUIFactory.INSTANCE.createButton(sessionsComp, "New Session");
        newSessionButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.createNewSession();
            }
        });


        GUIFactory.INSTANCE.createLabel(sessionsComp, "Select Session:");
        sessionCombo = GUIFactory.INSTANCE.createCombo(sessionsComp);
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

        // AI Settings part (merged)
        compositeLocal = GUIFactory.INSTANCE.createComposite(group, 3, SWT.BORDER);
        compositeLocal.setBackground(lightGreen);
        
        GUIFactory.INSTANCE.createLabel(compositeLocal, "AI Mode:");
        aiModeCombo = GUIFactory.INSTANCE.createCombo(compositeLocal, AiMode.values());
        Button targetButton = GUIFactory.INSTANCE.createButton(compositeLocal, "Target");
        
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

        GUIFactory.INSTANCE.createLabel(compositeLocal, "Model:");
        localModelCombo = selectModel(compositeLocal);
//        GUIFactory.INSTANCE.createLabel(compositeLocal);

        compositeRemote = GUIFactory.INSTANCE.createComposite(group, 3, SWT.BORDER);
        
        GUIFactory.INSTANCE.createLabel(compositeRemote, "AI Remote:");
        aiRemoteCombo = GUIFactory.INSTANCE.createCombo(compositeRemote);

        Button connectionButton = GUIFactory.INSTANCE.createButton(compositeRemote, "Test Connection");
        connectionButton.addSelectionListener(new SelectionAdapter() {
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
                eu.kalafatic.evolution.controller.security.TokenSecurityService.ResolvedProvider resolved = eu.kalafatic.evolution.controller.security.TokenSecurityService
                        .getInstance().resolve(orchestrator, providerName);
                if (resolved != null) {
                    remoteUrlText.setText(resolved.url != null ? resolved.url : "");
                    remoteTokenText.setText(resolved.token != null ? resolved.token : "");
                    page.syncModelWithUI();
                }
            }
        });

        load();

        remoteTokenText.addModifyListener(e -> page.syncModelWithUI());
        remoteUrlText.addModifyListener(e -> page.syncModelWithUI());
    }

    private Combo selectModel(Composite parent) {
        Combo combo = GUIFactory.INSTANCE.createCombo(parent);
        // selection listener
        combo.addListener(SWT.Selection, e -> {
            int index = combo.getSelectionIndex();
            if (index >= 0) {
                page.syncModelWithUI();
            }
        });
        return combo;
    }

    public void load(){
        if (orchestrator != null) {
            AiMode mode = orchestrator.getAiMode();
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

            String remoteModel = orchestrator.getRemoteModel();
            if (remoteModel == null || remoteModel.isEmpty()) {
                remoteModel = "deepseek";
            }
            if (remoteModel != null) {
                selectSafe(aiRemoteCombo, remoteModel);
            }

            eu.kalafatic.evolution.controller.security.TokenSecurityService.ResolvedProvider resolved = eu.kalafatic.evolution.controller.security.TokenSecurityService
                    .getInstance().resolve(orchestrator, aiRemoteCombo.getText());

            setTextSafe(remoteTokenText, (resolved != null && resolved.token != null) ? resolved.token : "");
            setTextSafe(remoteUrlText, (resolved != null && resolved.url != null) ? resolved.url : "");

            // 2. Populate Model combo
            if (localModelCombo != null) {
                String currentLocal = localModelCombo.getText();

                List<String> modelsToShow;
                if (mode == AiMode.PROXY) {
                    modelsToShow = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.PROXY);
                } else if (mode == AiMode.MEDIATED) {
                    modelsToShow = ProjectModelManager.getInstance().getLlmModels(orchestrator, AiMode.MEDIATED);
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

                String model = orchestrator.getLocalModel();

                if (model == null || model.isEmpty()) {
                    if (orchestrator.getOllama() != null)
                        model = orchestrator.getOllama().getModel();
                }
                if (model != null) {
                    selectSafe(localModelCombo, model);
                }
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
            refreshUI();
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
}
