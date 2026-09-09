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
        sourceTypeCombo.setItems(new String[] { "Hugging Face Hub", "Local Directory / Filesystem", "Project Source Code", "Synthetic Generator" });
        sourceTypeCombo.select(0);
        sourceTypeCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "WHAT (Schema / Domain):");
        Combo domainCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        domainCombo.setItems(new String[] { "General Text Corpus (Unstructured)", "Instruction Tuning & QA Pairs", "Source Code & Repositories", "Reasoning & Step-by-Step Proofs" });
        domainCombo.select(0);
        domainCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "WHERE (Predefined Target Preset):");
        Combo presetCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        presetCombo.setItems(new String[] { "Salesforce/wikitext (Wikitext-2)", "huggingFaceFW/fineWeb (FineWeb-10B)", "HuggingFaceH4/ultrachat_200k (UltraChat)", "bigcode/the-stack (Code Stack)", "gsm8k (GSM8K Math Proofs)", "Custom / Manual Entry..." });
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
                    case 1: repoText.setText("huggingFaceFW/fineWeb"); break;
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
        maxSizeMbText = toolkit.createText(group, "500", SWT.BORDER);
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
        btnBar.setLayout(new GridLayout(4, false));
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

    private void handleDownloadDataset() {
        String repo = repoText.getText().trim();
        String split = splitText.getText().trim();
        String samples = maxSamplesText.getText().trim();
        String sizeMbStr = maxSizeMbText.getText().trim();
        long maxBytes = 0;
        try {
            maxBytes = Long.parseLong(sizeMbStr) * 1024L * 1024L;
        } catch (Exception ignored) {}

        String sourceType = sourceTypeCombo.getSelectionIndex() == 0 ? "HUGGING_FACE" : "LOCAL";
        String customOutputDir = outputDirText.getText().trim();

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
                req.put("outputDir", customOutputDir);
                req.put("downloadOnly", true);

                String res = postHttp("http://localhost:" + port + "/forge/dataset/prepare", req.toString());
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("DATASET DOWNLOAD COMPLETE:\n" + res);
                        MessageDialog.openInformation(group.getShell(), "Dataset Downloaded", "Dataset downloaded successfully to destination folder!");
                    }
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("Download Error: " + ex.getMessage());
                    }
                });
            }
        }).start();
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

        String sourceType = sourceTypeCombo.getSelectionIndex() == 0 ? "HUGGING_FACE" : "LOCAL";

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
                req.put("outputDir", customOutputDir);
                req.put("minQuality", 0.5);

                String res = postHttp("http://localhost:" + port + "/forge/dataset/prepare", req.toString());
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("DATASET PREPARATION COMPLETE:\n" + res);
                        MessageDialog.openInformation(group.getShell(), "Dataset Prepared", "EVO Training Dataset artifact built successfully!");
                    }
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("Preparation Error: " + ex.getMessage());
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
