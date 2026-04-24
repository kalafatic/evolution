package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Display;

import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaService;

public class ModelDownloadDialog extends Dialog {

    private String ollamaUrl;
    private Text modelNameText;
    private ProgressBar progressBar;
    private Label statusLabel;
    private Button downloadButton;
    private String downloadedModelName;

    public ModelDownloadDialog(Shell parentShell, String ollamaUrl) {
        super(parentShell);
        this.ollamaUrl = ollamaUrl;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(2, false));

        Label label = new Label(container, SWT.NONE);
        label.setText("Model Name (e.g. llama3, hf.co/Qwen/Qwen3-8B-GGUF:Q4_K_M):");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);

        modelNameText = new Text(container, SWT.BORDER);
        modelNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        downloadButton = new Button(container, SWT.PUSH);
        downloadButton.setText("Download");
        downloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                startDownload();
            }
        });

        statusLabel = new Label(container, SWT.NONE);
        statusLabel.setText("Ready");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        statusLabel.setLayoutData(gd);

        progressBar = new ProgressBar(container, SWT.HORIZONTAL);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        progressBar.setLayoutData(gd);

        return container;
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
