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
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.orchestration.DatasetCandidateManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.forge.data.api.discovery.DatasetCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.DatasetSearchRequest;
import eu.kalafatic.evolution.forge.data.impl.discovery.HuggingFaceDatasetProvider;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dataset Editor UI Panel on Forge Models / Properties Page for managing,
 * previewing, preparing, exporting local and Hugging Face training datasets,
 * and performing remote dataset candidate discovery.
 */
public class DatasetEditorGroup extends AEvoGroup {

    private Combo sourceTypeCombo;
    private Combo domainCombo;
    private Text repoText;
    private Text splitText;
    private Text maxSamplesText;
    private Text maxSizeMbText;
    private Text outputDirText;
    private Combo samplingStrategyCombo;
    private Button cleanCheck;
    private Button deduplicateCheck;
    private Text reportArea;

    private Table candidatesTable;
    private List<DatasetCandidate> candidateList = new ArrayList<>();

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
        domainCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        domainCombo.setItems(new String[] { "General Text Corpus (Unstructured)", "Instruction Tuning & QA Pairs", "Source Code & Repositories", "Reasoning & Step-by-Step Proofs" });
        domainCombo.select(0);
        domainCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "WHERE (Predefined Target Preset):");
        Combo presetCombo = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
        presetCombo.setItems(new String[] {
            "Salesforce/wikitext (Wikitext-2)",
            "tatsu-lab/alpaca (Alpaca Instruction Tuning)",
            "HuggingFaceFW/fineweb (FineWeb-10B)",
            "HuggingFaceH4/ultrachat_200k (UltraChat)",
            "bigcode/the-stack (Code Stack)",
            "gsm8k (GSM8K Math Proofs)",
            "Custom / Manual Entry..."
        });
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
                    case 0 -> repoText.setText("wikitext");
                    case 1 -> repoText.setText("tatsu-lab/alpaca");
                    case 2 -> repoText.setText("HuggingFaceFW/fineweb");
                    case 3 -> repoText.setText("HuggingFaceH4/ultrachat_200k");
                    case 4 -> repoText.setText("bigcode/the-stack");
                    case 5 -> repoText.setText("gsm8k");
                    default -> {}
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
        btnBar.setLayout(new GridLayout(7, false));
        GridData gdBtn = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gdBtn.horizontalSpan = 2;
        btnBar.setLayoutData(gdBtn);

        Button discoverBtn = GUIFactory.INSTANCE.createButton(btnBar, "Discover Datasets");
        discoverBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleDiscoverDatasets();
            }
        });

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

        Button exportEvodataBtn = GUIFactory.INSTANCE.createButton(btnBar, "Export to .evodata");
        exportEvodataBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleExportEvodata();
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

        // SECTION: Discovered Remote Dataset Candidates SWT Table
        Composite candGroup = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Discovered Remote Dataset Candidates (EMF Persisted)", 2, true, true);
        GridData gdCandGroup = new GridData(GridData.FILL_HORIZONTAL);
        //gdCandGroup.horizontalSpan = 2;
        candGroup.setLayoutData(gdCandGroup);

        candidatesTable = new Table(candGroup, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        candidatesTable.setHeaderVisible(true);
        candidatesTable.setLinesVisible(true);
        GridData gdCandTable = new GridData(SWT.FILL, SWT.FILL, true, true);
        gdCandTable.heightHint = 130;
        candidatesTable.setLayoutData(gdCandTable);

        TableColumn colProvider = new TableColumn(candidatesTable, SWT.LEFT);
        colProvider.setText("Provider");
        colProvider.setWidth(85);

        TableColumn colRepo = new TableColumn(candidatesTable, SWT.LEFT);
        colRepo.setText("Dataset Repository ID");
        colRepo.setWidth(210);

        TableColumn colTask = new TableColumn(candidatesTable, SWT.LEFT);
        colTask.setText("Task / Domain");
        colTask.setWidth(110);

        TableColumn colLang = new TableColumn(candidatesTable, SWT.LEFT);
        colLang.setText("Lang");
        colLang.setWidth(45);

        TableColumn colFormat = new TableColumn(candidatesTable, SWT.LEFT);
        colFormat.setText("Format");
        colFormat.setWidth(95);

        TableColumn colSize = new TableColumn(candidatesTable, SWT.LEFT);
        colSize.setText("Size");
        colSize.setWidth(80);

        TableColumn colSplit = new TableColumn(candidatesTable, SWT.LEFT);
        colSplit.setText("Splits");
        colSplit.setWidth(60);

        TableColumn colCompat = new TableColumn(candidatesTable, SWT.CENTER);
        colCompat.setText("Compat.");
        colCompat.setWidth(65);

        TableColumn colStatus = new TableColumn(candidatesTable, SWT.LEFT);
        colStatus.setText("Status");
        colStatus.setWidth(80);

        Composite candBtnComp = toolkit.createComposite(candGroup);
        candBtnComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
        candBtnComp.setLayout(new GridLayout(1, true));

        Button useAsSourceBtn = GUIFactory.INSTANCE.createButton(candBtnComp, "Use as Dataset Source");
        Button viewCompatBtn = GUIFactory.INSTANCE.createButton(candBtnComp, "View Compatibility");
        Button removeCandBtn = GUIFactory.INSTANCE.createButton(candBtnComp, "Remove Candidate");
        Button clearCandBtn = GUIFactory.INSTANCE.createButton(candBtnComp, "Clear All");

        useAsSourceBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleUseCandidateAsSource();
            }
        });

        viewCompatBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleViewCandidateCompatibility();
            }
        });

        removeCandBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRemoveCandidate();
            }
        });

        clearCandBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleClearCandidates();
            }
        });

        Label infoDescLabel = toolkit.createLabel(group, "Usage Info: Discovered dataset candidates are evaluated for technical compatibility against requested dataset parameters, displayed above, and persisted in EMF. Discovered candidates serve as fallback candidates if primary dataset acquisition fails.");
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

        refreshCandidatesTableFromEmf();
    }

    private void handleDiscoverDatasets() {
        String repo = repoText.getText().trim();
        String domain = domainCombo != null ? domainCombo.getText().trim() : "General Text";
        String split = splitText.getText().trim();
        String sizeMbStr = maxSizeMbText.getText().trim();
        long targetBytes = 50 * 1024 * 1024L;
        try {
            targetBytes = Long.parseLong(sizeMbStr) * 1024L * 1024L;
        } catch (Exception ignored) {}

        String task = switch (domainCombo.getSelectionIndex()) {
            case 1 -> "INSTRUCTION";
            case 2 -> "CODE";
            case 3 -> "REASONING";
            default -> "GENERAL_TEXT";
        };

        List<String> requiredFields = "INSTRUCTION".equals(task) ? List.of("instruction", "input", "output") : List.of("text");

        DatasetSearchRequest request = new DatasetSearchRequest.Builder()
                .datasetName(repo)
                .task(task)
                .domain(domain)
                .language("en")
                .format(task)
                .requiredFields(requiredFields)
                .preferredSplit(split.isEmpty() ? "train" : split)
                .minimumSizeBytes(targetBytes / 5)
                .targetSizeBytes(targetBytes)
                .limit(20)
                .build();

        reportArea.setText("Searching remote dataset providers (Hugging Face) for compatible datasets matching:\n" +
                "- Requested Task: " + task + "\n" +
                "- Domain: " + domain + "\n" +
                "- Target Size: " + sizeMbStr + " MB\n" +
                "- Preferred Split: " + request.getPreferredSplit() + "\n\nSearching...\n");

        new Thread(() -> {
            try {
                HuggingFaceDatasetProvider provider = new HuggingFaceDatasetProvider();
                List<DatasetCandidate> discovered = provider.search(request);

                ForgeSession activeSession = ForgeSessionManager.getInstance().findSession("Active Forge Session");
                DatasetCandidateManager.getInstance().addCandidates(activeSession, discovered);

                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        refreshCandidatesTableFromEmf();

                        StringBuilder sb = new StringBuilder();
                        sb.append("REMOTE DATASET DISCOVERY COMPLETED:\n");
                        sb.append("- Discovered & Evaluated Candidates: ").append(discovered.size()).append("\n");
                        sb.append("- Saved to EMF Persistence Session: ").append(activeSession != null ? activeSession.getSessionId() : "Active").append("\n\n");
                        for (int i = 0; i < Math.min(10, discovered.size()); i++) {
                            DatasetCandidate c = discovered.get(i);
                            double mb = c.getSizeBytes() / (1024.0 * 1024.0);
                            sb.append(String.format(Locale.US, "#%d %-25s | Compat: %3d%% | Size: %6.1f MB | Status: %s\n",
                                    (i + 1), c.getRepository(), c.getCompatibilityScore(), mb, c.getStatus()));
                        }
                        reportArea.setText(sb.toString());

                        MessageDialog.openInformation(group.getShell(), "Dataset Discovery Complete",
                                "Discovered " + discovered.size() + " compatible remote dataset candidates!\nResults evaluated and persisted in EMF.");
                    }
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("Discovery Error: " + ex.getMessage());
                        MessageDialog.openError(group.getShell(), "Dataset Discovery Error", "Failed to discover remote datasets: " + ex.getMessage());
                    }
                });
            }
        }).start();
    }

    private void handleUseCandidateAsSource() {
        int idx = candidatesTable.getSelectionIndex();
        if (idx >= 0 && idx < candidateList.size()) {
            DatasetCandidate cand = candidateList.get(idx);
            repoText.setText(cand.getRepository());
            if (!cand.getSplits().isEmpty()) {
                splitText.setText(cand.getSplits().get(0));
            }
            if (cand.getSizeBytes() > 0) {
                double mb = cand.getSizeBytes() / (1024.0 * 1024.0);
                maxSizeMbText.setText(String.format(Locale.US, "%.2f", mb));
            }
            sourceTypeCombo.select(0); // Hugging Face Hub
            reportArea.setText("ACTIVE DATASET SOURCE UPDATED:\n" +
                    "- Selected Repository: " + cand.getRepository() + "\n" +
                    "- Technical Compatibility Score: " + cand.getCompatibilityScore() + "%\n" +
                    "- Provider: " + cand.getProvider() + "\n" +
                    "- Format: " + cand.getFormat() + "\n");
            MessageDialog.openInformation(group.getShell(), "Dataset Source Selected",
                    "Selected candidate '" + cand.getRepository() + "' as active Forge dataset source!");
        } else {
            MessageDialog.openWarning(group.getShell(), "No Candidate Selected", "Please select a candidate in the table first.");
        }
    }

    private void handleViewCandidateCompatibility() {
        int idx = candidatesTable.getSelectionIndex();
        if (idx >= 0 && idx < candidateList.size()) {
            DatasetCandidate cand = candidateList.get(idx);
            StringBuilder sb = new StringBuilder();
            sb.append("CANDIDATE COMPATIBILITY REPORT:\n\n");
            sb.append("Dataset: ").append(cand.getRepository()).append("\n");
            sb.append("Provider: ").append(cand.getProvider()).append("\n");
            sb.append("Technical Score: ").append(cand.getCompatibilityScore()).append("/100\n");
            sb.append("Status: ").append(cand.getStatus()).append("\n\n");

            sb.append("POSITIVE MATCH REASONS (+):\n");
            if (cand.getCompatibilityReasons().isEmpty()) {
                sb.append("  (None listed)\n");
            } else {
                for (String r : cand.getCompatibilityReasons()) {
                    sb.append("  ").append(r).append("\n");
                }
            }

            sb.append("\nWARNINGS & SHORTFALLS (-):\n");
            if (cand.getCompatibilityWarnings().isEmpty()) {
                sb.append("  (No warnings)\n");
            } else {
                for (String w : cand.getCompatibilityWarnings()) {
                    sb.append("  ").append(w).append("\n");
                }
            }

            MessageDialog.openInformation(group.getShell(), "Candidate Compatibility Details", sb.toString());
        } else {
            MessageDialog.openWarning(group.getShell(), "No Candidate Selected", "Please select a candidate in the table first.");
        }
    }

    private void handleRemoveCandidate() {
        int idx = candidatesTable.getSelectionIndex();
        if (idx >= 0 && idx < candidateList.size()) {
            DatasetCandidate cand = candidateList.remove(idx);
            ForgeSession activeSession = ForgeSessionManager.getInstance().findSession("Active Forge Session");
            DatasetCandidateManager.getInstance().removeCandidate(activeSession, cand.getId());
            refreshCandidatesTableFromEmf();
        }
    }

    private void handleClearCandidates() {
        ForgeSession activeSession = ForgeSessionManager.getInstance().findSession("Active Forge Session");
        DatasetCandidateManager.getInstance().clearCandidates(activeSession);
        refreshCandidatesTableFromEmf();
    }

    private void refreshCandidatesTableFromEmf() {
        if (candidatesTable == null || candidatesTable.isDisposed()) return;
        candidatesTable.removeAll();
        ForgeSession activeSession = ForgeSessionManager.getInstance().findSession("Active Forge Session");
        candidateList = DatasetCandidateManager.getInstance().getCandidates(activeSession);

        for (DatasetCandidate cand : candidateList) {
            TableItem item = new TableItem(candidatesTable, SWT.NONE);
            item.setText(0, cand.getProvider());
            item.setText(1, cand.getRepository());
            item.setText(2, cand.getTask());
            item.setText(3, cand.getLanguage());
            item.setText(4, cand.getFormat());
            double mb = cand.getSizeBytes() / (1024.0 * 1024.0);
            item.setText(5, cand.getSizeBytes() > 0 ? String.format(Locale.US, "%.1f MB", mb) : "Stream");
            item.setText(6, cand.getSplits().isEmpty() ? "train" : String.join(",", cand.getSplits()));
            item.setText(7, cand.getCompatibilityScore() + "%");
            item.setText(8, cand.getStatus());
            item.setData(cand);
        }
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

    private long calculateDirectoryOrFileSize(File fileOrDir) {
        if (!fileOrDir.exists()) return 0L;
        if (fileOrDir.isFile()) return fileOrDir.length();
        long total = 0L;
        File[] files = fileOrDir.listFiles();
        if (files != null) {
            for (File f : files) {
                total += calculateDirectoryOrFileSize(f);
            }
        }
        return total;
    }

    private int countDataFiles(File dir) {
        if (!dir.exists()) return 0;
        if (dir.isFile()) return 1;
        int count = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    count += countDataFiles(f);
                } else if (!f.getName().startsWith(".")) {
                    count++;
                }
            }
        }
        return count;
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
        File targetDatasetDir = eu.kalafatic.evolution.controller.tools.DatasetAcquisitionTool.resolveDatasetOutputDir(customOutputDir, repo);
        String resolvedOutputDir = targetDatasetDir.getAbsolutePath();

        String orchId = getOrchestratorId();
        OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.1, "Downloading dataset " + repo + " (" + split + ")...");

        reportArea.setText("Downloading dataset " + repo + " (" + split + ") into " + resolvedOutputDir + "...\n");

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
                req.put("outputDir", resolvedOutputDir);
                req.put("downloadOnly", true);

                OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.4, "Fetching dataset chunks for " + repo + "...");

                String res = postHttp("http://localhost:" + port + "/forge/dataset/prepare", req.toString());

                OrchestrationStatusManager.getInstance().updateStatus(orchId, 1.0, "Dataset Download Complete");

                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        File downloadedDir = targetDatasetDir;
                        long actualBytesOnDisk = calculateDirectoryOrFileSize(downloadedDir);
                        double actualMbOnDisk = actualBytesOnDisk / (1024.0 * 1024.0);
                        int dataFiles = countDataFiles(downloadedDir);

                        // Use actual properties in UI
                        repoText.setText(repo);
                        outputDirText.setText(downloadedDir.getAbsolutePath());
                        if (actualMbOnDisk > 0.0) {
                            maxSizeMbText.setText(String.format(Locale.US, "%.2f", actualMbOnDisk));
                        }

                        StringBuilder reportSb = new StringBuilder();
                        reportSb.append("ACTUAL DATASET PROPERTIES (DOWNLOADED):\n");
                        reportSb.append("- Name / Target ID: ").append(repo).append("\n");
                        reportSb.append("- Location on Disk: ").append(downloadedDir.getAbsolutePath()).append("\n");
                        reportSb.append("- Actual Size: ").append(String.format(Locale.US, "%.2f MB (%d bytes)", actualMbOnDisk, actualBytesOnDisk)).append("\n");
                        reportSb.append("- Total Data Files: ").append(dataFiles).append("\n");
                        reportSb.append("- Split: ").append(split).append("\n\n");
                        reportSb.append("DATASET DOWNLOAD RESPONSE:\n").append(res);

                        reportArea.setText(reportSb.toString());

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
                                    String.format("Dataset download completed, but source was exhausted before target size was reached.\n\nRequested Target: %.2f MB\nUsable Content Downloaded: %.2f MB\nShortfall: %.2f MB\nLocation: %s\nStatus: %s",
                                        reqMb, actMb, Math.max(0, reqMb - actMb), downloadedDir.getAbsolutePath(), status));
                            } else {
                                MessageDialog.openInformation(group.getShell(), "Dataset Downloaded",
                                    "Dataset " + repo + " downloaded successfully!\n\nLocation: " + downloadedDir.getAbsolutePath() + "\nSize: " + String.format(Locale.US, "%.2f MB", actualMbOnDisk));
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

    private void handleExportEvodata() {
        String repoOrPath = repoText.getText().trim();
        if (repoOrPath.isEmpty()) {
            MessageDialog.openWarning(group.getShell(), "No Dataset Specified", "Please specify a repository ID or target directory/file path to export.");
            return;
        }

        String customOutputDir = outputDirText.getText().trim();
        File targetDatasetDir = eu.kalafatic.evolution.controller.tools.DatasetAcquisitionTool.resolveDatasetOutputDir(customOutputDir, repoOrPath);
        if (!targetDatasetDir.exists()) {
            targetDatasetDir.mkdirs();
        }

        String itemType = "REPOSITORY";
        File pathFile = new File(repoOrPath);
        if (pathFile.exists()) {
            itemType = pathFile.isDirectory() ? "FOLDER" : "FILE";
        }

        List<eu.kalafatic.evolution.forge.data.api.source.DatasetItem> apiItems = new ArrayList<>();
        apiItems.add(new eu.kalafatic.evolution.forge.data.api.source.DatasetItem(true, pathFile.exists() ? pathFile.getAbsolutePath() : repoOrPath, itemType));

        String orchId = getOrchestratorId();
        OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.2, "Exporting .evodata artifact for " + repoOrPath + "...");
        reportArea.setText("Starting Native .evodata Export for " + repoOrPath + "...\nTarget Output Directory: " + targetDatasetDir.getAbsolutePath() + "\n");

        final File outputDir = targetDatasetDir;
        new Thread(() -> {
            try {
                eu.kalafatic.evolution.forge.data.impl.service.DatasetPreparationService service = new eu.kalafatic.evolution.forge.data.impl.service.DatasetPreparationService();
                eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext context = new eu.kalafatic.evolution.forge.data.api.source.DatasetPreparationContext();
                eu.kalafatic.evolution.forge.data.api.service.DatasetPreparationResult result = service.prepareDatasets(apiItems, context, outputDir);

                OrchestrationStatusManager.getInstance().updateStatus(orchId, 1.0, "Export Complete");

                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        if (result.getStatus() == eu.kalafatic.evolution.forge.data.api.service.DatasetPreparationResult.Status.SUCCESS && result.getOutputPath() != null) {
                            File evodataFile = new File(result.getOutputPath());
                            double sizeMb = evodataFile.length() / (1024.0 * 1024.0);

                            // Update UI fields with actual properties of exported dataset
                            repoText.setText(repoOrPath);
                            outputDirText.setText(evodataFile.getParentFile().getAbsolutePath());
                            maxSizeMbText.setText(String.format(Locale.US, "%.2f", sizeMb));

                            String reportText = String.format(Locale.US,
                                "EXPORT TO .EVODATA SUCCESSFUL:\n" +
                                "- Output File: %s\n" +
                                "- Target Name: %s\n" +
                                "- Records Accepted: %d\n" +
                                "- Usable Bytes: %d (%.2f MB)\n" +
                                "- Status: %s\n",
                                evodataFile.getAbsolutePath(), repoOrPath, result.getRecordsAccepted(), result.getAcceptedBytes(), sizeMb, result.getStatus());
                            reportArea.setText(reportText);

                            MessageDialog.openInformation(group.getShell(), "Dataset Exported",
                                "Native .evodata artifact created successfully!\n\n" +
                                "Output File: " + evodataFile.getAbsolutePath() + "\n" +
                                "Records Accepted: " + result.getRecordsAccepted() + "\n" +
                                "Usable Bytes: " + result.getAcceptedBytes() + " (" + String.format(Locale.US, "%.2f", sizeMb) + " MB)");
                        } else {
                            String errorMsg = !result.getErrors().isEmpty() ? String.join("\n", result.getErrors()) : "Status: " + result.getStatus();
                            reportArea.setText("EXPORT TO .EVODATA FAILED:\n" + errorMsg);
                            MessageDialog.openError(group.getShell(), "Dataset Export Failed", "Failed to create .evodata artifact:\n" + errorMsg);
                        }
                    }
                });
            } catch (Exception ex) {
                OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.0, "Export Error: " + ex.getMessage());
                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        reportArea.setText("EXPORT ERROR: " + ex.getMessage());
                        MessageDialog.openError(group.getShell(), "Error Exporting Dataset", "Error creating .evodata artifact: " + ex.getMessage());
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

        String customOutputDir = outputDirText.getText().trim();
        File targetDatasetDir = eu.kalafatic.evolution.controller.tools.DatasetAcquisitionTool.resolveDatasetOutputDir(customOutputDir, repo);
        targetDatasetDir.mkdirs();
        String resolvedOutputDir = targetDatasetDir.getAbsolutePath();

        reportArea.setText("Starting dataset preparation pipeline for " + repo + " (max " + sizeMbStr + " MB) into " + resolvedOutputDir + "...\n");

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
                req.put("outputDir", resolvedOutputDir);
                req.put("minQuality", 0.5);

                OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.5, "Building EVO dataset artifact (.evodata)...");

                String res = postHttp("http://localhost:" + port + "/forge/dataset/prepare", req.toString());

                OrchestrationStatusManager.getInstance().updateStatus(orchId, 1.0, "Dataset Preparation Complete");

                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        File preparedDir = targetDatasetDir;
                        long actualBytesOnDisk = calculateDirectoryOrFileSize(preparedDir);
                        double actualMbOnDisk = actualBytesOnDisk / (1024.0 * 1024.0);
                        int dataFiles = countDataFiles(preparedDir);

                        // Update actual properties in UI
                        repoText.setText(repo);
                        outputDirText.setText(preparedDir.getAbsolutePath());
                        if (actualMbOnDisk > 0.0) {
                            maxSizeMbText.setText(String.format(Locale.US, "%.2f", actualMbOnDisk));
                        }

                        StringBuilder reportSb = new StringBuilder();
                        reportSb.append("ACTUAL PREPARED DATASET PROPERTIES:\n");
                        reportSb.append("- Name / Target ID: ").append(repo).append("\n");
                        reportSb.append("- Location on Disk: ").append(preparedDir.getAbsolutePath()).append("\n");
                        reportSb.append("- Actual Size: ").append(String.format(Locale.US, "%.2f MB (%d bytes)", actualMbOnDisk, actualBytesOnDisk)).append("\n");
                        reportSb.append("- Total Data Files: ").append(dataFiles).append("\n\n");
                        reportSb.append("DATASET PREPARATION RESULT:\n").append(res);

                        reportArea.setText(reportSb.toString());

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
                                    String.format("Dataset source was exhausted before target size was reached.\n\nRequested Target: %.2f MB\nUsable Content Collected: %.2f MB\nShortfall: %.2f MB\nLocation: %s\nStatus: %s",
                                        reqMb, actMb, Math.max(0, reqMb - actMb), preparedDir.getAbsolutePath(), status));
                            } else {
                                MessageDialog.openInformation(group.getShell(), "Dataset Prepared",
                                    "EVO Training Dataset artifact built successfully!\n\nLocation: " + preparedDir.getAbsolutePath() + "\nSize: " + String.format(Locale.US, "%.2f MB", actualMbOnDisk));
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
                String serverRes = getHttp("http://localhost:" + port + "/forge/dataset/artifacts");

                // Scan local dataset output directories as well
                String customOutputDir = outputDirText.getText().trim();
                File targetDir = new File(customOutputDir);
                List<File> localArtifacts = scanLocalDatasetFiles(targetDir);

                Display.getDefault().asyncExec(() -> {
                    if (!reportArea.isDisposed()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("AVAILABLE EVO DATASET ARTIFACTS & DOWNLOADED LOCATIONS:\n");
                        sb.append("=========================================================\n");

                        if (localArtifacts != null && !localArtifacts.isEmpty()) {
                            sb.append("LOCAL DISCOVERED DATASETS / ARTIFACTS:\n");
                            for (File f : localArtifacts) {
                                double mb = f.length() / (1024.0 * 1024.0);
                                sb.append(String.format(Locale.US, "- %-30s | %-60s | %.2f MB\n", f.getName(), f.getAbsolutePath(), mb));
                            }
                            sb.append("\n");

                            // Use actual properties of first discovered local dataset if available
                            File first = localArtifacts.get(0);
                            double firstMb = first.length() / (1024.0 * 1024.0);
                            outputDirText.setText(first.getParentFile().getAbsolutePath());
                            maxSizeMbText.setText(String.format(Locale.US, "%.2f", firstMb));
                            String nameWithoutExt = first.getName().replaceAll("\\.(evodata|jsonl|parquet|txt)$", "");
                            repoText.setText(nameWithoutExt);
                        }

                        sb.append("SERVER REGISTERED ARTIFACTS:\n").append(serverRes);
                        reportArea.setText(sb.toString());
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

    private List<File> scanLocalDatasetFiles(File baseDir) {
        List<File> list = new ArrayList<>();
        if (baseDir == null || !baseDir.exists()) return list;

        File[] children = baseDir.listFiles();
        if (children == null) return list;

        for (File f : children) {
            if (f.isDirectory()) {
                list.addAll(scanLocalDatasetFiles(f));
            } else if (f.getName().endsWith(".evodata") || f.getName().endsWith(".jsonl") || f.getName().endsWith(".parquet")) {
                list.add(f);
            }
        }
        return list;
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
