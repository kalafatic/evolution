package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.IControlContentAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import eu.kalafatic.evolution.controller.manager.NeuronService;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.controller.orchestration.mcp.McpClient;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevSupervisor;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class AiChatPage extends ScrolledComposite {
    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private StyledText requestText;
    private StyledText responseText;
    private Label ollamaStatusLabel;
    private Label modelStatusLabel;
    private Label modeIndicatorLabel;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Composite approvalComposite;
    private Label approvalLabel;
    private Button approveButton;
    private Button rejectButton;
    private TaskContext currentContext;
    private OllamaService ollamaService;
    private Map<String, String> threads = new HashMap<>();
    private Map<String, StyleRange[]> threadStyles = new HashMap<>();
    private String currentThread = "Default";
    private Combo threadCombo;
    private Combo aiModeCombo;    
    private Combo aiRemoteCombo;
    private Label aiRemoteLabel;
    private Text remoteTokenText;
    private Text remoteUrlText;
    private Label remoteTokenLabel;
    private Label remoteUrlLabel;
    private Composite content;

    // Colors and Fonts
    private Color colorUser;
    private Color colorEvolution;
    private Color colorPlanner;
    private Color colorArchitect;
    private Color colorJavaDev;
    private Color colorTester;
    private Color colorReviewer;
    private Color colorError;
    private Color colorWhite;
    private Color colorLocal;
    private Color colorHybrid;
    private Color colorRemote;
    private Font chatFont;
    private Font bannerFont;
    
    


    public AiChatPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        this.editor = editor;
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        this.orchestrator = orchestrator;
        initResources();
        createControl();
        addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if (chatFont != null && !chatFont.isDisposed()) chatFont.dispose();
                if (bannerFont != null && !bannerFont.isDisposed()) bannerFont.dispose();
            }
        });
    }

    private void initResources() {
        Display display = getDisplay();
        colorUser = display.getSystemColor(SWT.COLOR_DARK_BLUE);
        colorEvolution = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);
        colorPlanner = display.getSystemColor(SWT.COLOR_DARK_CYAN);
        colorArchitect = display.getSystemColor(SWT.COLOR_DARK_GREEN);
        colorJavaDev = display.getSystemColor(SWT.COLOR_BLUE);
        colorTester = display.getSystemColor(SWT.COLOR_DARK_YELLOW);
        colorReviewer = display.getSystemColor(SWT.COLOR_MAGENTA);
        colorError = display.getSystemColor(SWT.COLOR_RED);
        colorWhite = display.getSystemColor(SWT.COLOR_WHITE);
        colorLocal = display.getSystemColor(SWT.COLOR_DARK_GREEN);
        colorHybrid = display.getSystemColor(SWT.COLOR_DARK_BLUE);
        colorRemote = display.getSystemColor(SWT.COLOR_DARK_MAGENTA);

        Font defaultFont = JFaceResources.getDefaultFont();
        FontData[] fontData = defaultFont.getFontData();
        for (FontData fd : fontData) {
            fd.setHeight(11);
        }
        chatFont = new Font(display, fontData);

        Font bannerDefault = JFaceResources.getBannerFont();
        FontData[] bannerData = bannerDefault.getFontData();
        for (FontData fd : bannerData) {
            fd.setStyle(SWT.BOLD);
        }
        bannerFont = new Font(display, bannerData);
    }

    private void createControl() {
        content = new Composite(this, SWT.NONE);
        content.setLayout(new GridLayout(1, false));
        this.setContent(content);

        modeIndicatorLabel = new Label(content, SWT.CENTER);
        modeIndicatorLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        modeIndicatorLabel.setFont(bannerFont);
        modeIndicatorLabel.setText("INITIALIZING...");

        Group chatMgmtGroup = SWTFactory.createGroup(content, "Chat Management", 5);

        Button cleanButton = SWTFactory.createButton(chatMgmtGroup, "Clean");
        cleanButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                responseText.setText("");
                threads.put(currentThread, "");
                threadStyles.put(currentThread, new StyleRange[0]);
            }
        });

        Button saveButton =  SWTFactory.createButton(chatMgmtGroup, "Save");
        saveButton.setText("Save");
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                saveChatToFile();
            }
        });

        createLabel(chatMgmtGroup, "Select Thread:");
        threadCombo = new Combo(chatMgmtGroup, SWT.READ_ONLY);
        threadCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        threadCombo.add(currentThread);
        threadCombo.select(0);
        threads.put(currentThread, "");
        threadCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                switchThread();
            }
        });

        Button newThreadButton =  SWTFactory.createButton(chatMgmtGroup, "New Thread");
        newThreadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createNewThread();
            }
        });

        Button selfDevButton = SWTFactory.createButton(chatMgmtGroup, "🚀 Self-Dev");
        selfDevButton.setToolTipText("Start an autonomous self-development session to improve the codebase.");
        selfDevButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                startSelfDevAction();
            }
        });
       

        
        final Group groupMode = SWTFactory.createGroup(content, "AI Settings", 2);

        createLabel(groupMode, "AI Mode:");
        aiModeCombo = new Combo(groupMode, SWT.DROP_DOWN | SWT.READ_ONLY);
        aiModeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        for (AiMode mode : AiMode.values()) {
            aiModeCombo.add(mode.getName());
        }

        aiRemoteLabel = new Label(groupMode, SWT.NONE);
        aiRemoteLabel.setText("AI Remote:");
        aiRemoteLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        aiRemoteCombo = new Combo(groupMode, SWT.DROP_DOWN | SWT.READ_ONLY);
        aiRemoteCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (String providerName : AiProviders.PROVIDERS.keySet()) {
            aiRemoteCombo.add(providerName);
        }

        remoteTokenLabel = new Label(groupMode, SWT.NONE);
        remoteTokenLabel.setText("Token:");
        remoteTokenLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        remoteTokenText = new Text(groupMode, SWT.BORDER | SWT.PASSWORD);
        remoteTokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        remoteUrlLabel = new Label(groupMode, SWT.NONE);
        remoteUrlLabel.setText("API URL:");
        remoteUrlLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        remoteUrlText = new Text(groupMode, SWT.BORDER);
        remoteUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        if (orchestrator != null) {
            aiModeCombo.select(orchestrator.getAiMode().getValue());
            String remoteModel = orchestrator.getRemoteModel();

            // Set default to deepseek if not configured
            if (remoteModel == null || remoteModel.isEmpty()) {
                remoteModel = "deepseek";
            }

            if (remoteModel != null) {
                int index = aiRemoteCombo.indexOf(remoteModel);
                if (index >= 0) aiRemoteCombo.select(index);
            }

            remoteTokenText.setText(orchestrator.getOpenAiToken() != null ? orchestrator.getOpenAiToken() : "");
            remoteUrlText.setText((orchestrator.getAiChat() != null && orchestrator.getAiChat().getUrl() != null) ? orchestrator.getAiChat().getUrl() : "");

            AiMode mode = orchestrator.getAiMode();
            boolean remoteVisible = mode == AiMode.HYBRID || mode == AiMode.REMOTE;
            aiRemoteLabel.setVisible(remoteVisible);
            aiRemoteCombo.setVisible(remoteVisible);
            remoteTokenLabel.setVisible(remoteVisible);
            remoteTokenText.setVisible(remoteVisible);
            remoteUrlLabel.setVisible(remoteVisible);
            remoteUrlText.setVisible(remoteVisible);
        } else {
            aiModeCombo.select(0);
            aiRemoteLabel.setVisible(false);
            aiRemoteCombo.setVisible(false);
            remoteTokenLabel.setVisible(false);
            remoteTokenText.setVisible(false);
            remoteUrlLabel.setVisible(false);
            remoteUrlText.setVisible(false);
        }

        aiModeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                syncModelWithUI();
            }
        });

        aiRemoteCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String provider = aiRemoteCombo.getText();
                ProviderConfig config = AiProviders.PROVIDERS.get(provider);
                if (config != null) {
                    remoteUrlText.setText(config.getEndpointUrl() != null ? config.getEndpointUrl() : "");
                    syncModelWithUI();
                }
            }
        });

        remoteTokenText.addModifyListener(e -> syncModelWithUI());
        remoteUrlText.addModifyListener(e -> syncModelWithUI());
        Button connectionButton = SWTFactory.createButton(groupMode, "Test Connection", 120);
        connectionButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) { 
                if (orchestrator != null) {
                    testAiConnectionRemote();
                } else {
                    MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK);
                    messageBox.setText("Warning");
                    messageBox.setMessage("Orchestrator not loaded.");
                    messageBox.open();
                }
            }
        });

        Group inputGroup = SWTFactory.createGroup(content, "Message Input", 1);
        requestText = new StyledText(inputGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        setupContextAssist();
        GridData requestGridData = new GridData(GridData.FILL_BOTH);
        requestGridData.heightHint = 100;
        requestText.setLayoutData(requestGridData);
        
        Button sendButton = SWTFactory.createButton(inputGroup, "Send");
        
        sendButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) { sendAction(); }
        });
        requestText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    if ((e.stateMask & SWT.MODIFIER_MASK) == 0) {
                        e.doit = false;
                        sendAction();
                    }
                }
            }
        });

        Group historyGroup = SWTFactory.createGroup(content, "Conversation History", 1);
        responseText = new StyledText(historyGroup, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        GridData responseGridData = new GridData(GridData.FILL_BOTH);
        responseGridData.heightHint = 250;
        responseText.setLayoutData(responseGridData);
        responseText.setEditable(false);
        responseText.setFont(chatFont);
        responseText.setMargins(10, 10, 10, 10);

        Group systemStatusGroup = SWTFactory.createGroup(content, "System Status", 4);
        createLabel(systemStatusGroup, "Ollama Status:");
        ollamaStatusLabel = new Label(systemStatusGroup, SWT.NONE);
        ollamaStatusLabel.setText("Unknown");
        ollamaStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createLabel(systemStatusGroup, "Model:");
        modelStatusLabel = new Label(systemStatusGroup, SWT.NONE);
        modelStatusLabel.setText("Not Configured");
        modelStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        statusLabel = new Label(systemStatusGroup, SWT.NONE);
        statusLabel.setText("Idle");
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
        progressBar = new ProgressBar(systemStatusGroup, SWT.HORIZONTAL);
        progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);

        approvalComposite = new Composite(content, SWT.NONE);
        approvalComposite.setLayout(new GridLayout(3, false));
        approvalComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        approvalComposite.setVisible(false);
        ((GridData)approvalComposite.getLayoutData()).exclude = true;

        approvalLabel = new Label(approvalComposite, SWT.NONE);
        approvalLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        approveButton = SWTFactory.createButton(approvalComposite, "Approve");
        approveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (currentContext != null) {
                    currentContext.provideApproval(true);
                    hideApprovalUI();
                }
            }
        });

        rejectButton = SWTFactory.createButton(approvalComposite, "Reject");
        rejectButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (currentContext != null) {
                    currentContext.provideApproval(false);
                    hideApprovalUI();
                }
            }
        });

        Runnable timer = new Runnable() {
            public void run() {
                if (!statusLabel.isDisposed()) {
                    String id = orchestrator != null ? orchestrator.getId() : null;
                    if (id != null) {
                        double progress = OrchestrationStatusManager.getInstance().getProgress(id);
                        String status = OrchestrationStatusManager.getInstance().getStatus(id);
                        statusLabel.setText(status);
                        progressBar.setSelection((int)(progress * 100));
                    }
                    Display.getDefault().timerExec(500, this);
                }
            }
        };
        Display.getDefault().timerExec(500, timer);

        updateStatusInfo();
        updateModeDisplay();
        updateScrolledContent();
    }

    private void updateScrolledContent() {
        if (content == null || content.isDisposed()) return;
        content.layout(true, true);
        this.setMinSize(content.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    private void updateModeDisplay() {
        if (orchestrator == null || modeIndicatorLabel == null || modeIndicatorLabel.isDisposed()) return;

        AiMode mode = orchestrator.getAiMode();
        modeIndicatorLabel.setText(mode.getName().toUpperCase() + " MODE ACTIVE");
        modeIndicatorLabel.setForeground(colorWhite);

        switch (mode) {
            case LOCAL:
                modeIndicatorLabel.setBackground(colorLocal);
                break;
            case HYBRID:
                modeIndicatorLabel.setBackground(colorHybrid);
                break;
            case REMOTE:
                modeIndicatorLabel.setBackground(colorRemote);
                break;
        }

        boolean remoteVisible = mode == AiMode.HYBRID || mode == AiMode.REMOTE;
        if (aiRemoteLabel != null && !aiRemoteLabel.isDisposed()) {
            aiRemoteLabel.setVisible(remoteVisible);
            aiRemoteCombo.setVisible(remoteVisible);
            remoteTokenLabel.setVisible(remoteVisible);
            remoteTokenText.setVisible(remoteVisible);
            remoteUrlLabel.setVisible(remoteVisible);
            remoteUrlText.setVisible(remoteVisible);

            updateScrolledContent();
        }
    }

    private void syncModelWithUI() {
        if (orchestrator == null) return;

        AiMode aiMode = AiMode.get(aiModeCombo.getSelectionIndex());
        orchestrator.setAiMode(aiMode);

        String remoteModel = aiRemoteCombo.getText();
        orchestrator.setRemoteModel(remoteModel);

        ProviderConfig config = AiProviders.PROVIDERS.get(remoteModel);
        if (config != null) {
            orchestrator.setOpenAiModel(config.getDefaultModel());
        }

        orchestrator.setOpenAiToken(remoteTokenText.getText());

        if (orchestrator.getAiChat() == null) {
            orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
        }
        orchestrator.getAiChat().setUrl(remoteUrlText.getText());

        editor.setDirty(true);
        updateModeDisplay();
    }

    private void sendAction() {
        String request = requestText.getText().trim();
        if (request.isEmpty()) return;

        if (orchestrator != null) {
            if (orchestrator.getAiChat() == null) {
                orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
            }
            if (orchestrator.getLlm() == null) {
                orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
            }
            NeuronService.getInstance().train(orchestrator, request);
            editor.setDirty(true);
            if (orchestrator.getId() == null || orchestrator.getId().isEmpty()) {
                orchestrator.setId("chat-" + System.currentTimeMillis());
            }
        }

        if (!responseText.getText().isEmpty()) {
            responseText.append("\n\n");
        }
        appendStyledText("You: " + request, colorUser, SWT.BOLD);
        appendStyledText("\n\nEvolution: Initializing orchestration...", colorEvolution, SWT.ITALIC);

        threads.put(currentThread, responseText.getText());
        threadStyles.put(currentThread, responseText.getStyleRanges());
        requestText.setText("");
        new Thread(() -> {
            try {
                EvolutionOrchestrator evolutionOrchestrator = new EvolutionOrchestrator();
                File projectRoot = getProjectRoot();
                TaskContext context = new TaskContext(orchestrator, projectRoot);
                this.currentContext = context;
                Display.getDefault().asyncExec(() -> editor.setCurrentContext(context));
                context.addApprovalListener(message -> {
                    Display.getDefault().asyncExec(() -> {
                        showApprovalUI(message);
                        if (TaskContext.PLAN_APPROVAL_MESSAGE.equals(message)) {
                            editor.showApprovalPage();
                        }
                    });
                });
                context.addTokenRequestListener((provider, future) -> {
                    Display.getDefault().asyncExec(() -> {
                        InputDialog dlg = new InputDialog(getShell(), "API Token Required", "Please enter the API token for " + provider + ":", "", null);
                        if (dlg.open() == Window.OK) {
                            String token = dlg.getValue();
                            remoteTokenText.setText(token);
                            syncModelWithUI();
                            future.complete(token);
                        } else {
                            future.completeExceptionally(new Exception("Token request cancelled by user."));
                        }
                    });
                });
                context.addLogListener(log -> {
                    Display.getDefault().asyncExec(() -> {
                        if (!responseText.isDisposed()) {
                            processLogEntry(log);
                            threads.put(currentThread, responseText.getText());
                            threadStyles.put(currentThread, responseText.getStyleRanges());
                        }
                    });
                });
                context.addTokenRequestListener((provider, future) -> {
                    Display.getDefault().asyncExec(() -> {
                        InputDialog dlg = new InputDialog(getShell(), "API Token Required", "Please enter the API token for " + provider + ":", "", null);
                        if (dlg.open() == Window.OK) {
                            String token = dlg.getValue();
                            remoteTokenText.setText(token);
                            syncModelWithUI();
                            future.complete(token);
                        } else {
                            future.completeExceptionally(new Exception("Token request cancelled by user."));
                        }
                    });
                });
                String result = evolutionOrchestrator.execute(request, context);
                Display.getDefault().asyncExec(() -> {
                    if (!responseText.isDisposed()) {
                        responseText.append("\n\n");
                        appendStyledText("Evolution: " + result, colorEvolution, SWT.BOLD);
                        threads.put(currentThread, responseText.getText());
                        threadStyles.put(currentThread, responseText.getStyleRanges());
                    }
                });
            } catch (Exception e) {
                Display.getDefault().asyncExec(() -> {
                    if (!responseText.isDisposed()) {
                        responseText.append("\n\n");
                        appendStyledText("Error: " + e.getMessage(), colorError, SWT.BOLD);
                        threads.put(currentThread, responseText.getText());
                        threadStyles.put(currentThread, responseText.getStyleRanges());
                    }
                });
            }
        }).start();
    }

    private void createNewThread() {
        InputDialog dlg = new InputDialog(getShell(), "New Chat Thread", "Enter thread name:", "Thread " + (threads.size() + 1), null);
        if (dlg.open() == Window.OK) {
            String name = dlg.getValue();
            if (name != null && !name.trim().isEmpty() && !threads.containsKey(name)) {
                threads.put(currentThread, responseText.getText());
                threadStyles.put(currentThread, responseText.getStyleRanges());
                currentThread = name;
                threads.put(currentThread, "");
                threadStyles.put(currentThread, new StyleRange[0]);
                threadCombo.add(currentThread);
                threadCombo.select(threadCombo.getItemCount() - 1);
                responseText.setText("");
            }
        }
    }

    private void switchThread() {
        threads.put(currentThread, responseText.getText());
        threadStyles.put(currentThread, responseText.getStyleRanges());
        currentThread = threadCombo.getText();
        responseText.setText(threads.getOrDefault(currentThread, ""));
        responseText.setStyleRanges(threadStyles.getOrDefault(currentThread, new StyleRange[0]));
        responseText.setSelection(responseText.getCharCount());
    }

    private void startSelfDevAction() {
        String request = requestText.getText().trim();
        if (request.isEmpty()) {
            request = "Analyze the project and suggest improvements.";
        }

        final String finalRequest = request;

        if (orchestrator != null) {
            if (orchestrator.getAiChat() == null) {
                orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
            }
            if (orchestrator.getLlm() == null) {
                orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
            }
            if (orchestrator.getId() == null || orchestrator.getId().isEmpty()) {
                orchestrator.setId("selfdev-" + System.currentTimeMillis());
            }
        }

        if (!responseText.getText().isEmpty()) {
            responseText.append("\n\n");
        }
        appendStyledText("User [SELF-DEV]: " + finalRequest, colorUser, SWT.BOLD);
        appendStyledText("\n\nEvolution: Initializing Self-Development Supervisor loop...", colorEvolution, SWT.ITALIC | SWT.BOLD);

        requestText.setText("");

        new Thread(() -> {
            try {
                File projectRoot = getProjectRoot();
                TaskContext context = new TaskContext(orchestrator, projectRoot);
                this.currentContext = context;

                context.addLogListener(log -> {
                    Display.getDefault().asyncExec(() -> {
                        if (!responseText.isDisposed()) {
                            processLogEntry(log);
                        }
                    });
                });
                context.addTokenRequestListener((provider, future) -> {
                    Display.getDefault().asyncExec(() -> {
                        InputDialog dlg = new InputDialog(getShell(), "API Token Required", "Please enter the API token for " + provider + ":", "", null);
                        if (dlg.open() == Window.OK) {
                            String token = dlg.getValue();
                            remoteTokenText.setText(token);
                            syncModelWithUI();
                            future.complete(token);
                        } else {
                            future.completeExceptionally(new Exception("Token request cancelled by user."));
                        }
                    });
                });

                SelfDevSession session = OrchestrationFactory.eINSTANCE.createSelfDevSession();
                session.setId("session-" + System.currentTimeMillis());
                session.setMaxIterations(5);
                orchestrator.setSelfDevSession(session);

                SelfDevSupervisor supervisor = new SelfDevSupervisor(session, context);
                supervisor.startSession();

                Display.getDefault().asyncExec(() -> {
                    if (!responseText.isDisposed()) {
                        responseText.append("\n\n");
                        appendStyledText("Evolution: Self-Development session finished. Status: " + session.getStatus(), colorEvolution, SWT.BOLD);
                        editor.setDirty(true);
                    }
                });
            } catch (Exception e) {
                Display.getDefault().asyncExec(() -> {
                    if (!responseText.isDisposed()) {
                        responseText.append("\n\n");
                        appendStyledText("Supervisor Error: " + e.getMessage(), colorError, SWT.BOLD);
                    }
                });
            }
        }).start();
    }

    private File getProjectRoot() {
        File projectRoot = null;
        if (editor.getEditorInput() instanceof IFileEditorInput) {
            projectRoot = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject().getLocation().toFile();
        } else if (orchestrator != null && orchestrator.eResource() != null) {
            org.eclipse.emf.common.util.URI uri = orchestrator.eResource().getURI();
            if (uri.isPlatformResource()) {
                String path = uri.toPlatformString(true);
                projectRoot = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(path)).getProject().getLocation().toFile();
            }
        }
        if (projectRoot == null) projectRoot = new File(System.getProperty("java.io.tmpdir"));
        return projectRoot;
    }

    private void saveChatToFile() {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { "*.txt", "*.*" });
        dialog.setFileName(currentThread + ".txt");
        String path = dialog.open();
        if (path != null) {
            try (FileWriter writer = new FileWriter(path)) {
                writer.write(responseText.getText());
            } catch (Exception e) {
                appendStyledText("\nError saving file: " + e.getMessage(), colorError, SWT.BOLD);
            }
        }
    }

    private void createLabel(Composite parent, String text) {
        GridData gd = new GridData();
        gd.widthHint = 100;
        Label label = new Label(parent, SWT.NONE);
        label.setLayoutData(gd);
        label.setText(text);
    }

    public void updateStatusInfo() {
        if (orchestrator != null && orchestrator.getOllama() != null) {
            String url = orchestrator.getOllama().getUrl();
            String model = orchestrator.getOllama().getModel();
            if (ollamaService == null) {
                float temp = 0.7f;
                if (orchestrator.getLlm() != null) temp = orchestrator.getLlm().getTemperature();
                ollamaService = new OllamaService(url, model).setTemperature(temp);
            }
            modelStatusLabel.setText(model != null ? model : "Not Configured");
            new Thread(() -> {
                boolean isOnline = ollamaService.ping();
                Display.getDefault().asyncExec(() -> {
                    if (ollamaStatusLabel.isDisposed()) return;
                    ollamaStatusLabel.setText((isOnline ? "Online (" : "Offline (") + url + ")");
                    ollamaStatusLabel.setForeground(Display.getDefault().getSystemColor(isOnline ? SWT.COLOR_DARK_GREEN : SWT.COLOR_RED));
                });
            }).start();
        } else {
            ollamaStatusLabel.setText("Not Configured");
            modelStatusLabel.setText("Not Configured");
        }
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        this.ollamaService = null;
        if (orchestrator != null && aiModeCombo != null && !aiModeCombo.isDisposed()) {
            aiModeCombo.select(orchestrator.getAiMode().getValue());

            String remoteModel = orchestrator.getRemoteModel();
            if (remoteModel != null) {
                int index = aiRemoteCombo.indexOf(remoteModel);
                if (index >= 0) aiRemoteCombo.select(index);
            }

            remoteTokenText.setText(orchestrator.getOpenAiToken() != null ? orchestrator.getOpenAiToken() : "");
            remoteUrlText.setText((orchestrator.getAiChat() != null && orchestrator.getAiChat().getUrl() != null) ? orchestrator.getAiChat().getUrl() : "");

            AiMode mode = orchestrator.getAiMode();
            boolean remoteVisible = mode == AiMode.HYBRID || mode == AiMode.REMOTE;
            aiRemoteLabel.setVisible(remoteVisible);
            aiRemoteCombo.setVisible(remoteVisible);
            remoteTokenLabel.setVisible(remoteVisible);
            remoteTokenText.setVisible(remoteVisible);
            remoteUrlLabel.setVisible(remoteVisible);
            remoteUrlText.setVisible(remoteVisible);
        }
        updateStatusInfo();
        updateModeDisplay();
    }

    private void showApprovalUI(String message) {
        if (approvalComposite.isDisposed()) return;
        approvalLabel.setText(message);
        approvalComposite.setVisible(true);
        ((GridData)approvalComposite.getLayoutData()).exclude = false;
        updateScrolledContent();
    }

    private void hideApprovalUI() {
        if (approvalComposite.isDisposed()) return;
        approvalComposite.setVisible(false);
        ((GridData)approvalComposite.getLayoutData()).exclude = true;
        updateScrolledContent();
    }

    private void appendStyledText(String text, Color color, int style) {
        if (responseText.isDisposed()) return;
        int start = responseText.getCharCount();
        responseText.append(text);
        int length = text.length();
        StyleRange range = new StyleRange(start, length, color, null, style);
        responseText.setStyleRange(range);
        responseText.setSelection(responseText.getCharCount());
    }

    private void setupContextAssist() {
        IContentProposalProvider proposalProvider = new IContentProposalProvider() {
            @Override
            public IContentProposal[] getProposals(String contents, int position) {
                String prefix = contents.substring(0, position);
                int lastSpace = prefix.lastIndexOf(' ');
                if (lastSpace != -1) {
                    prefix = prefix.substring(lastSpace + 1);
                }

                String finalPrefix = prefix;
                String[] proposals = NeuronService.getInstance().getProposals(orchestrator, finalPrefix);

                IContentProposal[] result = new IContentProposal[proposals.length];
                for (int i = 0; i < proposals.length; i++) {
                    final String proposal = proposals[i];
                    result[i] = new IContentProposal() {
                        @Override
                        public String getContent() { return proposal; }
                        @Override
                        public int getCursorPosition() { return proposal.length(); }
                        @Override
                        public String getLabel() { return proposal; }
                        @Override
                        public String getDescription() { return null; }
                    };
                }
                return result;
            }
        };

        IControlContentAdapter contentAdapter = new IControlContentAdapter() {
            @Override
            public void setControlContents(org.eclipse.swt.widgets.Control control, String contents, int cursorPosition) {
                ((StyledText) control).setText(contents);
                ((StyledText) control).setSelection(cursorPosition);
            }
            @Override
            public void insertControlContents(org.eclipse.swt.widgets.Control control, String contents, int cursorPosition) {
                StyledText text = (StyledText) control;
                String currentText = text.getText();
                int selectionStart = text.getSelection().x;

                // Find where the word starts
                int wordStart = selectionStart;
                while (wordStart > 0 && !Character.isWhitespace(currentText.charAt(wordStart - 1))) {
                    wordStart--;
                }

                String newText = currentText.substring(0, wordStart) + contents + currentText.substring(selectionStart);
                text.setText(newText);
                text.setSelection(wordStart + cursorPosition);
            }
            @Override
            public String getControlContents(org.eclipse.swt.widgets.Control control) {
                return ((StyledText) control).getText();
            }
            @Override
            public int getCursorPosition(org.eclipse.swt.widgets.Control control) {
                return ((StyledText) control).getCaretOffset();
            }
            @Override
            public org.eclipse.swt.graphics.Rectangle getInsertionBounds(org.eclipse.swt.widgets.Control control) {
                StyledText text = (StyledText) control;
                return text.getBounds();
            }
            @Override
            public void setCursorPosition(org.eclipse.swt.widgets.Control control, int index) {
                ((StyledText) control).setSelection(index);
            }
        };

        KeyStroke keyStroke = null;
        try {
            keyStroke = KeyStroke.getInstance("Ctrl+Space");
        } catch (Exception e) {
            e.printStackTrace();
        }

        ContentProposalAdapter adapter = new ContentProposalAdapter(requestText, contentAdapter, proposalProvider, keyStroke, null);
        adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
        adapter.setAutoActivationDelay(200);
        adapter.setAutoActivationCharacters(new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' });
    }

    private void testAiConnectionRemote() {
        if (orchestrator == null) return;

        // Get values from UI first
        final int modeIndex = aiModeCombo.getSelectionIndex();
        final String remoteModel = aiRemoteCombo.getText();
        final String token = remoteTokenText.getText();
        final String apiUrl = remoteUrlText.getText();

        new Thread(() -> {
            try {
                // Create a temporary orchestrator for testing to avoid side effects
                Orchestrator tempOrch = OrchestrationFactory.eINSTANCE.createOrchestrator();
                tempOrch.setAiMode(AiMode.get(modeIndex));
                tempOrch.setRemoteModel(remoteModel);
                tempOrch.setOpenAiToken(token);
                tempOrch.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
                tempOrch.getAiChat().setUrl(apiUrl);
                tempOrch.setLlm(OrchestrationFactory.eINSTANCE.createLLM());

                // Inherit hybrid/local models if needed by testConnection
                tempOrch.setHybridModel(orchestrator.getHybridModel());
                tempOrch.setLocalModel(orchestrator.getLocalModel());
                if (orchestrator.getOllama() != null) {
                    tempOrch.setOllama(OrchestrationFactory.eINSTANCE.createOllama());
                    tempOrch.getOllama().setUrl(orchestrator.getOllama().getUrl());
                    tempOrch.getOllama().setModel(orchestrator.getOllama().getModel());
                }

                LlmRouter router = new LlmRouter();
                float temp = 0.7f;
                if (orchestrator.getLlm() != null) temp = orchestrator.getLlm().getTemperature();
                                
                String proxyUrl = (orchestrator.getAiChat() != null) ? orchestrator.getAiChat().getProxyUrl() : null;

                TaskContext context = new TaskContext(tempOrch, null);
                context.addTokenRequestListener((provider, future) -> {
                    Display.getDefault().asyncExec(() -> {
                        InputDialog dlg = new InputDialog(getShell(), "API Token Required", "Please enter the API token for " + provider + ":", "", null);
                        if (dlg.open() == Window.OK) {
                            String newToken = dlg.getValue();
                            remoteTokenText.setText(newToken);
                            syncModelWithUI();
                            future.complete(newToken);
                        } else {
                            future.completeExceptionally(new Exception("Token request cancelled by user."));
                        }
                    });
                });
                String response = router.testConnection(tempOrch, temp, proxyUrl, context);

                Display.getDefault().asyncExec(() -> {
                    if (isDisposed()) return;

                    // On success, sync to real model
                    orchestrator.setAiMode(AiMode.get(modeIndex));
                    orchestrator.setRemoteModel(remoteModel);
                    orchestrator.setOpenAiToken(token);
                    if (orchestrator.getAiChat() == null) {
                        orchestrator.setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
                    }
                    orchestrator.getAiChat().setUrl(apiUrl);
                    if (orchestrator.getLlm() == null) {
                        orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
                    }

                    editor.setDirty(true);
                    updateModeDisplay();
                    updateStatusInfo();

                    MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                    mb.setText("AI Connection Success");
                    mb.setMessage("Connected to AI provider successfully and settings saved.\nResponse: " + response);
                    mb.open();
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (isDisposed()) return;
                    MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                    mb.setText("AI Connection Failed");
                    mb.setMessage("Error connecting to AI provider (settings NOT saved): " + ex.getMessage());
                    mb.open();
                });
            }
        }).start();
    }

    private void processLogEntry(String log) {
        if (log == null || log.isEmpty()) return;

        Color color = null;
        int style = SWT.NORMAL;

        if (log.startsWith("Orchestrator:")) {
            color = colorEvolution;
            style = SWT.ITALIC;
        } else if (log.contains("Agent [") && log.contains("Planner")) {
            color = colorPlanner;
            style = SWT.BOLD;
        } else if (log.contains("Agent [") && log.contains("Architect")) {
            color = colorArchitect;
            style = SWT.BOLD;
        } else if (log.contains("Agent [") && log.contains("JavaDev")) {
            color = colorJavaDev;
            style = SWT.BOLD;
        } else if (log.contains("Agent [") && log.contains("Tester")) {
            color = colorTester;
            style = SWT.BOLD;
        } else if (log.contains("Agent [") && log.contains("Reviewer")) {
            color = colorReviewer;
            style = SWT.BOLD;
        } else if (log.startsWith("Orchestrator Error:") || log.contains("Exception:")) {
            color = colorError;
            style = SWT.BOLD;
        }

        appendStyledText("\n" + log, color, style);
    }
    

}
