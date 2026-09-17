package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
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
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;

import java.io.File;

/**
 * Dataset Editor UI Panel on Forge Models / Properties Page for managing,
 * previewing, and preparing local and Hugging Face training datasets.
 */
public class DatasetEditorGroup extends AEvoGroup {

    private Combo sourceTypeCombo;
    private Text repoText;
    private Text splitText;
    private Text maxSamplesText;
    private Text maxSizeMbText;
    private Text outputDirText;
    private Combo samplingStrategyCombo;
    private Button cleanCheck;
    private Button deduplicateCheck;
    private Text reportArea;

    public DatasetEditorGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Dataset Preparation & Hugging Face Acquisition", 2, true, true);

        GUIFactory.INSTANCE.createLabel(group, "FROM (Data Source Provider):");
        sourceTypeCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        sourceTypeCombo.setItems(new String[] { "Hugging Face Hub", "Local Directory / Filesystem", "EVO Codebase Git Repository", "Project Source Code", "Synthetic Generator" });
        sourceTypeCombo.select(0);
        sourceTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        sourceTypeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (sourceTypeCombo.getSelectionIndex() == 2) { // EVO Codebase Git Repository
                    String codebase = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
                    repoText.setText(codebase != null ? codebase : ".");
                    splitText.setText("main");
                }
            }
        });

        GUIFactory.INSTANCE.createLabel(group, "WHAT (Schema / Domain):");
        Combo domainCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        domainCombo.setItems(new String[] { "General Text Corpus (Unstructured)", "Instruction Tuning & QA Pairs", "Source Code & Repositories", "Reasoning & Step-by-Step Proofs" });
        domainCombo.select(0);
        domainCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "WHERE (Predefined Target Preset):");
        Combo presetCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        presetCombo.setItems(new String[] { "Salesforce/wikitext (Wikitext-2)", "HuggingFaceFW/fineweb (FineWeb-10B)", "HuggingFaceH4/ultrachat_200k (UltraChat)", "bigcode/the-stack (Code Stack)", "gsm8k (GSM8K Math Proofs)", "Custom / Manual Entry..." });
        presetCombo.select(0);
        presetCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "Repository / Dataset Target ID (Editable):");
        repoText = toolkit.createText(group, "wikitext", SWT.BORDER);
        repoText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        presetCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int idx = presetCombo.getSelectionIndex();
                switch (idx) {
                    case 0: repoText.setText("wikitext"); break;
                    case 1: repoText.setText("HuggingFaceFW/fineweb"); break;
                    case 2: repoText.setText("HuggingFaceH4/ultrachat_200k"); break;
                    case 3: repoText.setText("bigcode/the-stack"); break;
                    case 4: repoText.setText("gsm8k"); break;
                    default: break;
                }
            }
        });

        GUIFactory.INSTANCE.createLabel(group, "Split / Configuration:");
        splitText = toolkit.createText(group, "train", SWT.BORDER);
        splitText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "Max Samples Limit (0 = Unlimited):");
        maxSamplesText = toolkit.createText(group, "0", SWT.BORDER);
        maxSamplesText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "Target Usable Data Size (MB):");
        maxSizeMbText = toolkit.createText(group, "50", SWT.BORDER);
        maxSizeMbText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "Destination Output Directory:");
        Composite dirComp = toolkit.createComposite(group);
        dirComp.setLayout(new GridLayout(2, false));
        dirComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        String baseWorkspace = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getWorkspacePath();
        if (baseWorkspace == null || baseWorkspace.trim().isEmpty()) {
            baseWorkspace = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getCodebasePath();
        }
        if (baseWorkspace == null || baseWorkspace.trim().isEmpty()) {
            baseWorkspace = System.getProperty("user.dir");
        }
        String defaultDir = new File(baseWorkspace, "forge-input").getAbsolutePath();
        outputDirText = toolkit.createText(dirComp, defaultDir, SWT.BORDER);
        outputDirText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button browseBtn = GUIFactory.INSTANCE.createButton(dirComp, "Browse...");
        browseBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                org.eclipse.swt.widgets.DirectoryDialog dialog = new org.eclipse.swt.widgets.DirectoryDialog(group.getShell());
                dialog.setText("Select Destination Dataset Output Directory");
                dialog.setFilterPath(outputDirText.getText());
                String selected = dialog.open();
                if (selected != null && !selected.trim().isEmpty()) {
                    outputDirText.setText(selected.trim());
                }
            }
        });

        GUIFactory.INSTANCE.createLabel(group, "Sampling Strategy:");
        samplingStrategyCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        samplingStrategyCombo.setItems(new String[] { "Reservoir Sampling", "Random Sampling", "Sequential" });
        samplingStrategyCombo.select(0);
        samplingStrategyCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "Cleaning & Normalization:");
        cleanCheck = toolkit.createButton(group, "Enable Whitespace & Unicode Cleaning", SWT.CHECK);
        cleanCheck.setSelection(true);

        GUIFactory.INSTANCE.createLabel(group, "Deduplication:");
        deduplicateCheck = toolkit.createButton(group, "Exact Hash & Near-Duplicate Filtering", SWT.CHECK);
        deduplicateCheck.setSelection(true);

        Composite btnBar = toolkit.createComposite(group);
        btnBar.setLayout(new GridLayout(5, false));
        GridData gdBtn = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gdBtn.horizontalSpan = 2;
        btnBar.setLayoutData(gdBtn);

        Button downloadBtn = GUIFactory.INSTANCE.createButton(btnBar, "Download Dataset");
        downloadBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDownloadDataset();
            }
        });

        Button previewBtn = GUIFactory.INSTANCE.createButton(btnBar, "Preview Samples");
        previewBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handlePreview();
            }
        });

        Button prepareBtn = GUIFactory.INSTANCE.createButton(btnBar, "Build Dataset (.evodata)");
        prepareBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handlePrepareDataset();
            }
        });

        Button refreshBtn = GUIFactory.INSTANCE.createButton(btnBar, "List Datasets");
        refreshBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleListDatasets();
            }
        });

        Button addTaskBtn = GUIFactory.INSTANCE.createButton(btnBar, "Add Task to Task Stack");
        addTaskBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAddTaskToStack();
            }
        });

        Label infoDescLabel = toolkit.createLabel(group, "Usage Info: .evodata files (e.g., wikitext.evodata) package cleaned, deduplicated, and tokenized training/validation splits with metadata. The Forge Trainer ingests .evodata artifacts directly for offline or fine-tuning forging runs.");
        GridData gdInfo = new GridData(GridData.FILL_HORIZONTAL);
        gdInfo.horizontalSpan = 2;
        infoDescLabel.setLayoutData(gdInfo);

        Composite reportHeaderComp = toolkit.createComposite(group);
        reportHeaderComp.setLayout(new GridLayout(2, false));
        GridData gdRepHeader = new GridData(GridData.FILL_HORIZONTAL);
        gdRepHeader.horizontalSpan = 2;
        reportHeaderComp.setLayoutData(gdRepHeader);

        Label reportLabel = toolkit.createLabel(reportHeaderComp, "Preparation Report & Preview Log:");
        reportLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button copyReportBtn = GUIFactory.INSTANCE.createButton(reportHeaderComp, "Copy Log");
        copyReportBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String text = reportArea.getText();
                if (text != null && !text.isEmpty()) {
                    org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(Display.getDefault());
                    cb.setContents(new Object[] { text }, new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() });
                    cb.dispose();
                    MessageDialog.openInformation(group.getShell(), "Copied", "Report log copied to clipboard.");
                }
            }
        });

        reportArea = toolkit.createText(group, "", SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.READ_ONLY);
        GridData gdArea = new GridData(GridData.FILL_BOTH);
        gdArea.horizontalSpan = 2;
        gdArea.heightHint = 120;
        reportArea.setLayoutData(gdArea);
    }

    private int getServerPort() {
        if (orchestrator != null && orchestrator.getServerSettings() != null) {
            return orchestrator.getServerSettings().getPort();
        }
        return 48080;
    }

    private String getOrchestratorId() {
        if (orchestrator != null && orchestrator.getId() != null) {
            return orchestrator.getId();
        }
        return "default";
    }

    private void handleDownloadDataset() {
        String repo = repoText.getText().trim();
        String split = splitText.getText().trim();
        String samples = maxSamplesText.getText().trim();
        String sizeMbStr = maxSizeMbText.getText().trim();
        long maxBytes = 0;
        try {
            maxBytes = Long.parseLong(sizeMbStr) * 1024L * 1024L;
        } catch (Exception ignored) {}

        String sourceType = switch (sourceTypeCombo.getSelectionIndex()) {
            case 0 -> "HUGGING_FACE";
            case 2 -> "EVO_CODEBASE";
            default -> "LOCAL";
        };
        String customOutputDir = outputDirText.getText().trim();

        String orchId = getOrchestratorId();
        OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.1, "Downloading dataset " + repo + " (" + split + ")...");

        reportArea.setText("Downloading dataset " + repo + " (" + split + ") into " + customOutputDir + "...\n");

        int port = getServerPort();
        final long finalMaxBytes = maxBytes;
        new Thread(() -> {
            try {
                org.json.JSONObject req = new org.json.JSONObject();
                req.put("sourceType", sourceType);
                req.put("repository", repo);
                req.put("split", split);
                req.put("maxSamples", Long.parseLong(samples));
                req.put("maxBytes", finalMaxBytes);
                req.put("targetUsableBytes", finalMaxBytes);
                req.put("outputDir", customOutputDir);
                req.put("downloadOnly", true);

                OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.4, "Fetching dataset chunks for " + repo + "...");

                String res = postHttp("http://localhost:" + port + "/forge/dataset/prepare", req.toString());

                OrchestrationStatusManager.getInstance().updateStatus(orchId, 1.0, "Dataset Download Complete");

                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("DATASET DOWNLOAD RESULT:\n" + res);
                        try {
                            org.json.JSONObject resJson = new org.json.JSONObject(res);
                            String status = resJson.optString("status", "READY");
                            long requestedBytes = resJson.optLong("requestedUsableBytes", 0);
                            long actualBytes = resJson.optLong("actualUsableBytes", 0);

                            if ("FAILED".equalsIgnoreCase(status) || "SOURCE_EMPTY".equalsIgnoreCase(status)) {
                                MessageDialog.openError(group.getShell(), "Dataset Download Failed",
                                    "Dataset download failed or source was empty.\n\nStatus: " + status);
                            } else if ("INSUFFICIENT_SOURCE_DATA".equalsIgnoreCase(status) || (requestedBytes > 0 && actualBytes < requestedBytes)) {
                                double reqMb = requestedBytes / (1024.0 * 1024.0);
                                double actMb = actualBytes / (1024.0 * 1024.0);
                                MessageDialog.openWarning(group.getShell(), "Dataset Source Exhausted",
                                    String.format("Dataset download completed, but source was exhausted before target size was reached.\n\nRequested Target: %.2f MB\nUsable Content Downloaded: %.2f MB\nShortfall: %.2f MB\nStatus: %s",
                                        reqMb, actMb, Math.max(0, reqMb - actMb), status));
                            } else {
                                MessageDialog.openInformation(group.getShell(), "Dataset Downloaded", "Dataset downloaded successfully to destination folder!");
                            }
                        } catch (Exception ex) {
                            MessageDialog.openError(group.getShell(), "Dataset Download Error", "Error processing download response: " + ex.getMessage());
                        }
                    }
                });
            } catch (Exception ex) {
                OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.0, "Download Error: " + ex.getMessage());
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("Download Error: " + ex.getMessage());
                        MessageDialog.openError(group.getShell(), "Dataset Download Error", "Download failed: " + ex.getMessage());
                    }
                });
            }
        }).start();
    }

    private void handleAddTaskToStack() {
        String repo = repoText.getText().trim();
        String split = splitText.getText().trim();
        String sizeMbStr = maxSizeMbText.getText().trim();

        if (editor != null && orchestrator != null) {
            try {
                eu.kalafatic.evolution.view.editors.pages.TaskStackPage taskStackPage = editor.getTaskStackPage();

                if (taskStackPage != null) {
                    taskStackPage.addDownloadDatasetTask(repo, split, sizeMbStr);
                } else {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyyMMdd-HHmm");
                    String timestamp = sdf.format(new java.util.Date());
                    eu.kalafatic.evolution.model.orchestration.Task task = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createTask();
                    task.setId("DATASET-" + timestamp);
                    task.setName("Download Simple Dataset from Hugging Face");
                    task.setType("DATASET");
                    task.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.READY);
                    task.setSelected(true);
                    String prompt = "Download Hugging Face dataset " + repo + " (split: " + split + ", target size: " + sizeMbStr + " MB) into forge-input directory.";
                    task.setDescription(prompt);
                    task.setPrompt(prompt);
                    task.setBitState(eu.kalafatic.evolution.controller.orchestration.behavior.BitState.encode(
                        eu.kalafatic.evolution.controller.orchestration.behavior.BitState.MODE_LOCAL,
                        eu.kalafatic.evolution.controller.orchestration.behavior.BitState.SUPERVISION_AUTO,
                        eu.kalafatic.evolution.controller.orchestration.behavior.BitState.INTERACTION_CONTINUOUS,
                        eu.kalafatic.evolution.controller.orchestration.behavior.BitState.REASONING_ATOMIC,
                        eu.kalafatic.evolution.controller.orchestration.behavior.BitState.WORKFLOW_TASK_ORIENTED));

                    String[] subtaskNames = {"Validate Dataset Repository", "Acquire Hugging Face Chunks", "Clean & Deduplicate Content", "Package EVO Dataset Artifact (.evodata)"};
                    for (String stName : subtaskNames) {
                        eu.kalafatic.evolution.model.orchestration.Task subTask = eu.kalafatic.evolution.model.orchestration.OrchestrationFactory.eINSTANCE.createTask();
                        subTask.setName(stName);
                        subTask.setStatus(eu.kalafatic.evolution.model.orchestration.TaskStatus.READY);
                        task.getSubTasks().add(subTask);
                    }
                    orchestrator.getTasks().add(task);
                }

                editor.setDirty(true);
                reportArea.setText("TASK ADDED TO TASK STACK:\n- Task Name: Download Simple Dataset from Hugging Face\n- Repository: " + repo + "\n- Split: " + split + "\n- Target Size: " + sizeMbStr + " MB\n");
                MessageDialog.openInformation(group.getShell(), "Task Queued", "Dataset download task successfully queued on the Task Stack!");
            } catch (Exception ex) {
                MessageDialog.openError(group.getShell(), "Task Queue Error", "Failed to add task to Task Stack: " + ex.getMessage());
            }
        }
    }

    private void handlePreview() {
        String repo = repoText.getText().trim();
        String split = splitText.getText().trim();
        reportArea.setText("Fetching sample preview for " + repo + " (" + split + ")...\n");

        int port = getServerPort();
        new Thread(() -> {
            try {
                org.json.JSONObject req = new org.json.JSONObject();
                req.put("repository", repo);
                req.put("split", split);
                req.put("count", 5);

                String res = postHttp("http://localhost:" + port + "/forge/dataset/hf/preview", req.toString());
                String tableText = formatAsTable(res);
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("DATASET SAMPLE PREVIEW:\n" + tableText);
                    }
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("Preview Error: " + ex.getMessage());
                    }
                });
            }
        }).start();
    }

    private String formatAsTable(String jsonStr) {
        try {
            org.json.JSONArray array = new org.json.JSONArray(jsonStr);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%-5s | %-12s | %-50s | %-10s\n", "#", "TYPE", "PROMPT / CONTENT", "QUALITY"));
            sb.append("----------------------------------------------------------------------------------\n");
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject obj = array.getJSONObject(i);
                String type = obj.optString("sampleType", obj.optString("type", "TEXT"));
                String content = obj.optString("content", obj.optString("text", obj.optString("prompt", "")));
                content = content.replaceAll("\r?\n", " ");
                if (content.length() > 47) content = content.substring(0, 44) + "...";
                double quality = obj.optDouble("qualityScore", 1.0);
                sb.append(String.format("%-5d | %-12s | %-50s | %-10.1f%%\n", (i + 1), type, content, quality * 100.0));
            }
            return sb.toString();
        } catch (Exception e) {
            return jsonStr;
        }
    }

    private void handlePrepareDataset() {
        String repo = repoText.getText().trim();
        String split = splitText.getText().trim();
        String samples = maxSamplesText.getText().trim();
        String sizeMbStr = maxSizeMbText.getText().trim();
        long maxBytes = 0;
        try {
            maxBytes = Long.parseLong(sizeMbStr) * 1024L * 1024L;
        } catch (Exception ignored) {}

        String sourceType = switch (sourceTypeCombo.getSelectionIndex()) {
            case 0 -> "HUGGING_FACE";
            case 2 -> "EVO_CODEBASE";
            default -> "LOCAL";
        };

        String orchId = getOrchestratorId();
        OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.1, "Starting dataset preparation for " + repo + "...");

        reportArea.setText("Starting dataset preparation pipeline for " + repo + " (max " + sizeMbStr + " MB)...\n");

        String customOutputDir = outputDirText.getText().trim();
        int port = getServerPort();
        final long finalMaxBytes = maxBytes;
        new Thread(() -> {
            try {
                org.json.JSONObject req = new org.json.JSONObject();
                req.put("sourceType", sourceType);
                req.put("repository", repo);
                req.put("split", split);
                req.put("maxSamples", Long.parseLong(samples));
                req.put("maxBytes", finalMaxBytes);
                req.put("targetUsableBytes", finalMaxBytes);
                req.put("outputDir", customOutputDir);
                req.put("minQuality", 0.5);

                OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.5, "Building EVO dataset artifact (.evodata)...");

                String res = postHttp("http://localhost:" + port + "/forge/dataset/prepare", req.toString());

                OrchestrationStatusManager.getInstance().updateStatus(orchId, 1.0, "Dataset Preparation Complete");

                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("DATASET PREPARATION RESULT:\n" + res);
                        try {
                            org.json.JSONObject resJson = new org.json.JSONObject(res);
                            String status = resJson.optString("status", "READY");
                            boolean sourceExhausted = resJson.optBoolean("sourceExhausted", false);
                            long requestedBytes = resJson.optLong("requestedUsableBytes", 0);
                            long actualBytes = resJson.optLong("actualUsableBytes", 0);

                            if ("FAILED".equalsIgnoreCase(status) || "SOURCE_EMPTY".equalsIgnoreCase(status)) {
                                MessageDialog.openError(group.getShell(), "Dataset Preparation Failed",
                                    "Dataset preparation failed or source was empty.\n\nStatus: " + status);
                            } else if ("INSUFFICIENT_SOURCE_DATA".equalsIgnoreCase(status) || (sourceExhausted && actualBytes < requestedBytes) || (requestedBytes > 0 && actualBytes < requestedBytes)) {
                                double reqMb = requestedBytes / (1024.0 * 1024.0);
                                double actMb = actualBytes / (1024.0 * 1024.0);
                                MessageDialog.openWarning(group.getShell(), "Source Data Shortfall Warning",
                                    String.format("Dataset source was exhausted before target size was reached.\n\nRequested Target: %.2f MB\nUsable Content Collected: %.2f MB\nShortfall: %.2f MB\nStatus: %s",
                                        reqMb, actMb, Math.max(0, reqMb - actMb), status));
                            } else {
                                MessageDialog.openInformation(group.getShell(), "Dataset Prepared", "EVO Training Dataset artifact built successfully!");
                            }
                        } catch (Exception ex) {
                            MessageDialog.openError(group.getShell(), "Dataset Preparation Error", "Error processing preparation response: " + ex.getMessage());
                        }
                    }
                });
            } catch (Exception ex) {
                OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.0, "Preparation Error: " + ex.getMessage());
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("Preparation Error: " + ex.getMessage());
                        MessageDialog.openError(group.getShell(), "Dataset Preparation Error", "Preparation failed: " + ex.getMessage());
                    }
                });
            }
        }).start();
    }

    private void handleListDatasets() {
        int port = getServerPort();
        new Thread(() -> {
            try {
                String res = getHttp("http://localhost:" + port + "/forge/dataset/artifacts");
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("AVAILABLE EVO DATASET ARTIFACTS:\n" + res);
                    }
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("Error listing datasets: " + ex.getMessage());
                    }
                });
            }
        }).start();
    }

    private String postHttp(String urlStr, String jsonBody) throws Exception {
        java.net.URL url = java.net.URI.create(urlStr).toURL();
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-evo-runtime", "SWT");

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        java.io.InputStream stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        if (stream == null) {
            throw new Exception("HTTP error code " + code);
        }

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            String result = sb.toString();
            if (code >= 300) {
                try {
                    org.json.JSONObject errJson = new org.json.JSONObject(result);
                    if (errJson.has("error")) throw new Exception(errJson.getString("error"));
                } catch (org.json.JSONException ignored) {}
                throw new Exception("HTTP " + code + ": " + result);
            }
            return result;
        }
    }

    private String getHttp(String urlStr) throws Exception {
        java.net.URL url = java.net.URI.create(urlStr).toURL();
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("x-evo-runtime", "SWT");

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            return sb.toString();
        }
    }

    @Override
    protected void refreshUI() {}
}
