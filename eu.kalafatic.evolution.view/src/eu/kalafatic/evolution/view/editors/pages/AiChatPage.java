package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.IFileEditorInput;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.orchestration.EvolutionOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class AiChatPage extends Composite {
    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private StyledText requestText;
    private StyledText responseText;
    private Label ollamaStatusLabel;
    private Label modelStatusLabel;
    private Label statusLabel;
    private ProgressBar progressBar;
    private OllamaService ollamaService;
    private Map<String, String> threads = new HashMap<>();
    private String currentThread = "Default";
    private Combo threadCombo;

    public AiChatPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.NONE);
        this.editor = editor;
        this.orchestrator = orchestrator;
        createControl();
    }

    private void createControl() {
        GridLayout layout = new GridLayout();
        this.setLayout(layout);
        layout.numColumns = 1;

        Composite toolbar = new Composite(this, SWT.NONE);
        toolbar.setLayout(new GridLayout(4, false));
        toolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button cleanButton = new Button(toolbar, SWT.PUSH);
        cleanButton.setText("Clean");
        cleanButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                responseText.setText("");
                threads.put(currentThread, "");
            }
        });

        Button newThreadButton = new Button(toolbar, SWT.PUSH);
        newThreadButton.setText("New Thread");
        newThreadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createNewThread();
            }
        });

        createLabel(toolbar, "Select Thread:");
        threadCombo = new Combo(toolbar, SWT.READ_ONLY);
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

        createLabel(this, "Request:");
        requestText = new StyledText(this, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData requestGridData = new GridData(GridData.FILL_BOTH);
        requestGridData.heightHint = 100;
        requestText.setLayoutData(requestGridData);
        Button sendButton = new Button(this, SWT.PUSH);
        sendButton.setText("Send");
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
        createLabel(this, "Response:");
        responseText = new StyledText(this, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY | SWT.WRAP);
        GridData responseGridData = new GridData(GridData.FILL_BOTH);
        responseGridData.heightHint = 200;
        responseText.setLayoutData(responseGridData);
        responseText.setEditable(false);
        Composite statusBar = new Composite(this, SWT.NONE);
        statusBar.setLayout(new GridLayout(4, false));
        statusBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createLabel(statusBar, "Ollama Status:");
        ollamaStatusLabel = new Label(statusBar, SWT.NONE);
        ollamaStatusLabel.setText("Unknown");
        ollamaStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        createLabel(statusBar, "Model:");
        modelStatusLabel = new Label(statusBar, SWT.NONE);
        modelStatusLabel.setText("Not Configured");
        modelStatusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Composite progressBox = new Composite(this, SWT.NONE);
        progressBox.setLayout(new GridLayout(2, false));
        progressBox.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        statusLabel = new Label(progressBox, SWT.NONE);
        statusLabel.setText("Idle");
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        progressBar = new ProgressBar(progressBox, SWT.HORIZONTAL);
        progressBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);

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
    }

    private void sendAction() {
        String request = requestText.getText().trim();
        if (request.isEmpty()) return;

        if (orchestrator != null && (orchestrator.getId() == null || orchestrator.getId().isEmpty())) {
            orchestrator.setId("chat-" + System.currentTimeMillis());
        }

        String currentResponse = responseText.getText();
        String newText = currentResponse + (currentResponse.isEmpty() ? "" : "\n\n") + "You: " + request + "\n\nEvolution: Initializing orchestration...";
        responseText.setText(newText);
        threads.put(currentThread, newText);
        requestText.setText("");
        new Thread(() -> {
            try {
                EvolutionOrchestrator evolutionOrchestrator = new EvolutionOrchestrator();
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
                TaskContext context = new TaskContext(orchestrator, projectRoot);
                context.addLogListener(log -> {
                    Display.getDefault().asyncExec(() -> {
                        if (!responseText.isDisposed()) {
                            responseText.append("\n" + log);
                            responseText.setSelection(responseText.getCharCount());
                            threads.put(currentThread, responseText.getText());
                        }
                    });
                });
                String result = evolutionOrchestrator.execute(request, context);
                Display.getDefault().asyncExec(() -> {
                    if (!responseText.isDisposed()) {
                        responseText.append("\n\nEvolution: " + result);
                        responseText.setSelection(responseText.getCharCount());
                        threads.put(currentThread, responseText.getText());
                    }
                });
            } catch (Exception e) {
                Display.getDefault().asyncExec(() -> {
                    if (!responseText.isDisposed()) {
                        responseText.append("\n\nError: " + e.getMessage());
                        responseText.setSelection(responseText.getCharCount());
                        threads.put(currentThread, responseText.getText());
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
                currentThread = name;
                threads.put(currentThread, "");
                threadCombo.add(currentThread);
                threadCombo.select(threadCombo.getItemCount() - 1);
                responseText.setText("");
            }
        }
    }

    private void switchThread() {
        threads.put(currentThread, responseText.getText());
        currentThread = threadCombo.getText();
        responseText.setText(threads.getOrDefault(currentThread, ""));
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
        updateStatusInfo();
    }
}
