package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaService;

public class ModelDownloadDialog extends Dialog {

    private String ollamaUrl;
    private Text modelNameText;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Button downloadButton;
    private String downloadedModelName;

    // Direct CLI Instruction Text Widgets
    private Text ollamaRunCmdText;
    private Text ollamaPullCmdText;
    private Text llamaCppDownloadCmdText;
    private Text llamaCppRunCmdText;

    public ModelDownloadDialog(Shell parentShell, String ollamaUrl) {
        super(parentShell);
        this.ollamaUrl = ollamaUrl;
        setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Top Section: Model Download Input
        Composite topComp = new Composite(container, SWT.NONE);
        topComp.setLayout(new GridLayout(2, false));
        topComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label label = new Label(topComp, SWT.NONE);
        label.setText("Model Name (e.g., llama3.2:3b, hf.co/Qwen/Qwen2.5-Coder-7B-Instruct-GGUF:Q4_K_M):");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);

        modelNameText = new Text(topComp, SWT.BORDER);
        modelNameText.setMessage("Enter model name...");
        modelNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        modelNameText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updateInstructionPreviews();
            }
        });

        downloadButton = new Button(topComp, SWT.PUSH);
        downloadButton.setText("Download via API");
        downloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                startDownload();
            }
        });

        statusLabel = new Label(topComp, SWT.NONE);
        statusLabel.setText("Ready");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        statusLabel.setLayoutData(gd);

        progressBar = new ProgressBar(topComp, SWT.HORIZONTAL);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        progressBar.setLayoutData(gd);

        // Group 1: Direct Download & Execution using Ollama
        Group ollamaGroup = new Group(container, SWT.NONE);
        ollamaGroup.setText("Direct Download & Run Instructions (Ollama CLI)");
        ollamaGroup.setLayout(new GridLayout(3, false));
        ollamaGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label ollamaDesc = new Label(ollamaGroup, SWT.WRAP);
        ollamaDesc.setText("Download and run standard or Hugging Face GGUF models directly via Ollama CLI:");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        ollamaDesc.setLayoutData(gd);

        // Ollama Run Command
        Label lblOllamaRun = new Label(ollamaGroup, SWT.NONE);
        lblOllamaRun.setText("Run / Pull & Chat:");
        ollamaRunCmdText = createReadOnlyCmdText(ollamaGroup);
        createCopyButton(ollamaGroup, ollamaRunCmdText);

        // Ollama Pull Command
        Label lblOllamaPull = new Label(ollamaGroup, SWT.NONE);
        lblOllamaPull.setText("Pull Only:");
        ollamaPullCmdText = createReadOnlyCmdText(ollamaGroup);
        createCopyButton(ollamaGroup, ollamaPullCmdText);

        // Group 2: Direct Download & Execution using llama-cpp
        Group llamaGroup = new Group(container, SWT.NONE);
        llamaGroup.setText("Direct Download & Run Instructions (llama-cpp / GGUF)");
        llamaGroup.setLayout(new GridLayout(3, false));
        llamaGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label llamaDesc = new Label(llamaGroup, SWT.WRAP);
        llamaDesc.setText("Download GGUF files directly to local models folder and run via llama-cli:");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        llamaDesc.setLayoutData(gd);

        // llama-cpp Direct Download Command
        Label lblLlamaDownload = new Label(llamaGroup, SWT.NONE);
        lblLlamaDownload.setText("Download GGUF:");
        llamaCppDownloadCmdText = createReadOnlyCmdText(llamaGroup);
        createCopyButton(llamaGroup, llamaCppDownloadCmdText);

        // llama-cpp Run Command
        Label lblLlamaRun = new Label(llamaGroup, SWT.NONE);
        lblLlamaRun.setText("Run GGUF:");
        llamaCppRunCmdText = createReadOnlyCmdText(llamaGroup);
        createCopyButton(llamaGroup, llamaCppRunCmdText);

        updateInstructionPreviews();

        return container;
    }

    private Text createReadOnlyCmdText(Composite parent) {
        Text txt = new Text(parent, SWT.BORDER | SWT.READ_ONLY);
        txt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        return txt;
    }

    private void createCopyButton(Composite parent, Text targetText) {
        Button btn = new Button(parent, SWT.PUSH);
        btn.setText("Copy");
        btn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String cmd = targetText.getText();
                if (cmd != null && !cmd.isEmpty()) {
                    Clipboard cb = new Clipboard(Display.getDefault());
                    cb.setContents(new Object[] { cmd }, new Transfer[] { TextTransfer.getInstance() });
                    cb.dispose();
                }
            }
        });
    }

    private void updateInstructionPreviews() {
        String input = modelNameText.getText().trim();
        String name = input.isEmpty() ? "llama3.2:3b" : input;
        String fileName = name.contains("/") ? name.substring(name.lastIndexOf('/') + 1) : name;
        if (!fileName.toLowerCase().endsWith(".gguf")) {
            fileName = fileName.replace(':', '_') + ".gguf";
        }

        if (ollamaRunCmdText != null && !ollamaRunCmdText.isDisposed()) {
            ollamaRunCmdText.setText("ollama run " + name);
        }
        if (ollamaPullCmdText != null && !ollamaPullCmdText.isDisposed()) {
            ollamaPullCmdText.setText("ollama pull " + name);
        }
        if (llamaCppDownloadCmdText != null && !llamaCppDownloadCmdText.isDisposed()) {
            if (name.startsWith("http://") || name.startsWith("https://")) {
                llamaCppDownloadCmdText.setText("curl -L -o lib/models/" + fileName + " " + name);
            } else {
                llamaCppDownloadCmdText.setText("huggingface-cli download " + name + " --local-dir lib/models/");
            }
        }
        if (llamaCppRunCmdText != null && !llamaCppRunCmdText.isDisposed()) {
            llamaCppRunCmdText.setText("llama-cli -m lib/models/" + fileName + " -p \"Hello EVO!\"");
        }
    }

    private void startDownload() {
        String modelName = modelNameText.getText().trim();
        if (modelName.isEmpty()) {
            MessageDialog.openError(getShell(), "Error", "Please enter a model name.");
            return;
        }

        downloadButton.setEnabled(false);
        modelNameText.setEnabled(false);
        statusLabel.setText("Starting download...");

        new Thread(() -> {
            try {
                OllamaService service = OllamaManager.getInstance().getService(ollamaUrl);
                service.pullModel(modelName, update -> {
                    Display.getDefault().asyncExec(() -> {
                        if (progressBar.isDisposed()) return;
                        statusLabel.setText(update.status());
                        if (update.total() > 0) {
                            int percent = (int) (update.completed() * 100 / update.total());
                            progressBar.setSelection(percent);
                        }
                    });
                });

                Display.getDefault().asyncExec(() -> {
                    this.downloadedModelName = modelName;
                    MessageDialog.openInformation(getShell(), "Success", "Model " + modelName + " downloaded successfully.");
                    okPressed();
                });
            } catch (Exception e) {
                Display.getDefault().asyncExec(() -> {
                    if (statusLabel.isDisposed()) return;
                    statusLabel.setText("Error: " + e.getMessage());
                    downloadButton.setEnabled(true);
                    modelNameText.setEnabled(true);
                    MessageDialog.openError(getShell(), "Download Failed", e.getMessage());
                });
            }
        }).start();
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Download Local Model");
    }

    public String getDownloadedModelName() {
        return downloadedModelName;
    }
}