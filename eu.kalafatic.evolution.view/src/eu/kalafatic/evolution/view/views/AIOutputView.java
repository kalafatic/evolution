package eu.kalafatic.evolution.view.views;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import eu.kalafatic.evolution.controller.handlers.OrchestrationCommandHandler;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class AIOutputView extends ViewPart {

    public static final String ID = "eu.kalafatic.evolution.view.aiOutputView";
    private TextViewer viewer;
    private Combo providerCombo;
    private Button useProxyCheck;
    private Text proxyUrlText;
    private Text inputPrompt;
    private Button sendButton;
    private Map<String, String> threads = new HashMap<>();
    private String currentThread = "Default";
    private Combo threadCombo;

    public AIOutputView() {
    }

    @Override
    public void createPartControl(Composite parent) {
        Composite mainContainer = new Composite(parent, SWT.NONE);
        mainContainer.setLayout(new GridLayout(1, false));

        // Top Header for Settings
        Composite header = new Composite(mainContainer, SWT.NONE);
        header.setLayout(new GridLayout(7, false));
        header.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Button newThreadBtn = new Button(header, SWT.PUSH);
        newThreadBtn.setText("New");
        newThreadBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                createNewThread();
            }
        });

        new Label(header, SWT.NONE).setText("Thread:");
        threadCombo = new Combo(header, SWT.READ_ONLY);
        threadCombo.add(currentThread);
        threadCombo.select(0);
        threads.put(currentThread, "");
        threadCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                switchThread();
            }
        });

        new Label(header, SWT.NONE).setText("Provider:");
        providerCombo = new Combo(header, SWT.READ_ONLY);
        providerCombo.setItems("Local (Ollama)", "Internet (Proxy)");
        providerCombo.select(0);

        useProxyCheck = new Button(header, SWT.CHECK);
        useProxyCheck.setText("Use Proxy");

        proxyUrlText = new Text(header, SWT.BORDER);
        proxyUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        proxyUrlText.setMessage("Proxy URL (e.g. http://proxy:8080)");
        proxyUrlText.setEnabled(false);

        useProxyCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                proxyUrlText.setEnabled(useProxyCheck.getSelection());
            }
        });

        // Try to initialize from selection
        Orchestrator initialOrch = findOrchestrator();
        if (initialOrch != null && initialOrch.getAiChat() != null) {
            String initialProxy = initialOrch.getAiChat().getProxyUrl();
            if (initialProxy != null && !initialProxy.isEmpty()) {
                proxyUrlText.setText(initialProxy);
                useProxyCheck.setSelection(true);
                proxyUrlText.setEnabled(true);
            }
        }

        // Middle for Output
        viewer = new TextViewer(mainContainer, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        viewer.setDocument(new org.eclipse.jface.text.Document());
        viewer.setEditable(false);

        // Bottom for Input
        Composite footer = new Composite(mainContainer, SWT.NONE);
        footer.setLayout(new GridLayout(2, false));
        footer.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, true, false));

        inputPrompt = new Text(footer, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gd.heightHint = 50;
        inputPrompt.setLayoutData(gd);
        inputPrompt.setMessage("Enter your prompt here...");
        inputPrompt.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR) {
                    if ((e.stateMask & SWT.MODIFIER_MASK) == 0) {
                        e.doit = false;
                        sendMessage();
                    }
                }
            }
        });

        Composite buttonArea = new Composite(footer, SWT.NONE);
        buttonArea.setLayout(new GridLayout(1, false));
        buttonArea.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));

        sendButton = new Button(buttonArea, SWT.PUSH);
        sendButton.setText("Send");
        sendButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        sendButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                sendMessage();
            }
        });

        Button clearButton = new Button(buttonArea, SWT.PUSH);
        clearButton.setText("Clear");
        clearButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        clearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                viewer.getDocument().set("");
                threads.put(currentThread, "");
            }
        });
    }

    private void createNewThread() {
        InputDialog dlg = new InputDialog(getSite().getShell(), "New Chat Thread", "Enter thread name:", "Thread " + (threads.size() + 1), null);
        if (dlg.open() == Window.OK) {
            String name = dlg.getValue();
            if (name != null && !name.trim().isEmpty() && !threads.containsKey(name)) {
                threads.put(currentThread, viewer.getDocument().get());
                currentThread = name;
                threads.put(currentThread, "");
                threadCombo.add(currentThread);
                threadCombo.select(threadCombo.getItemCount() - 1);
                viewer.getDocument().set("");
            }
        }
    }

    private void switchThread() {
        threads.put(currentThread, viewer.getDocument().get());
        currentThread = threadCombo.getText();
        viewer.getDocument().set(threads.getOrDefault(currentThread, ""));
    }

    private void sendMessage() {
        String prompt = inputPrompt.getText();
        if (prompt.isEmpty()) return;

        String provider = providerCombo.getText();
        boolean useProxy = useProxyCheck.getSelection();
        String proxyUrl = useProxy ? proxyUrlText.getText() : null;

        // Call this on UI thread before background job
        Orchestrator orch = findOrchestrator();
        if (orch == null) {
            viewer.getDocument().set(viewer.getDocument().get() + "\nError: No Orchestrator selected in Workbench.\n");
            return;
        }

        String initialText = viewer.getDocument().get() + "\n\nUser: " + prompt + "\n";
        viewer.getDocument().set(initialText);
        threads.put(currentThread, initialText);
        inputPrompt.setText("");

        Job job = new Job("AI Chat Request") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    OrchestrationCommandHandler handler = new OrchestrationCommandHandler();
                    String response = handler.sendRequest(orch, prompt, proxyUrl);

                    Display.getDefault().asyncExec(() -> {
                        viewer.getDocument().set(viewer.getDocument().get() + "\nAI: " + response + "\n");
                        threads.put(currentThread, viewer.getDocument().get());
                    });
                } catch (Exception e) {
                    Display.getDefault().asyncExec(() -> {
                        viewer.getDocument().set(viewer.getDocument().get() + "\nError: " + e.getMessage() + "\n");
                        threads.put(currentThread, viewer.getDocument().get());
                    });
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private Orchestrator findOrchestrator() {
        ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
        if (selection instanceof IStructuredSelection) {
            Object first = ((IStructuredSelection) selection).getFirstElement();
            if (first instanceof Orchestrator) {
                return (Orchestrator) first;
            }
        }
        return null;
    }

    @Override
    public void setFocus() {
        inputPrompt.setFocus();
    }
}
