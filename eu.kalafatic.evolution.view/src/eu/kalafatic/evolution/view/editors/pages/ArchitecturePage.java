package eu.kalafatic.evolution.view.editors.pages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.agents.MetadataAgent;
import eu.kalafatic.evolution.controller.discovery.SourceDiscoveryResult;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.DesignExporter;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.orchestration.design.DesignRenderer;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.GenomeSnapshot;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.selfdev.genome.milestone.MilestoneGenerator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * @evo:19:A reason=dynamic-architecture-page
 */
public class ArchitecturePage extends AEvoPage {
    private Browser browser;
    private DesignRenderer renderer = new DesignRenderer();
    private Runnable refreshRunnable = this::refreshBrowser;
    private boolean isLoaded = false;
    private boolean isJsReady = false;
    private String lastJson = "";
    private String lastTargetPath = "";
    private boolean showingSnapshot = false;

    private String currentTargetPath;
    private String defaultTargetPath;
    private List<String> targetHistory = new ArrayList<>();
    private org.eclipse.swt.widgets.Combo targetCombo;
    private org.eclipse.swt.widgets.Combo snapshotCombo;
    private MilestoneGenerator milestoneGenerator = new MilestoneGenerator();

    public ArchitecturePage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        this.setLayout(new GridLayout(1, false));

        createControlPanel();

        this.browser = new Browser(this, SWT.NONE);
        this.browser.setLayoutData(new GridData(GridData.FILL_BOTH));

        browser.addProgressListener(new org.eclipse.swt.browser.ProgressAdapter() {
            @Override
            public void completed(org.eclipse.swt.browser.ProgressEvent event) {
                isLoaded = true;
                setupJavaScriptBridges();

                Display.getDefault().asyncExec(() -> {
                    Display.getDefault().timerExec(200, () -> {
                        refreshBrowser();
                    });
                });
            }
        });

        setupJavaScriptBridges();
        hookContextMenu();
        initTargetPath();
        initBrowser();
    }

    private void initTargetPath() {
        if (orchestrator != null && orchestrator.getDefaultTarget() != null && !orchestrator.getDefaultTarget().isEmpty()) {
            defaultTargetPath = orchestrator.getDefaultTarget();
            currentTargetPath = defaultTargetPath;
            eu.kalafatic.evolution.controller.log.Log.log("[ARCH] Default target from model: " + currentTargetPath);
        }

        // Use discovery to populate target history and fallback paths
        SourceDiscoveryResult discovery = ProjectModelManager.getInstance().getOrDiscoverWorkspace();
        if (discovery.getPrimaryRepository() != null) {
            String primary = discovery.getPrimaryRepository().getAbsolutePath();
            if (currentTargetPath == null) {
                currentTargetPath = primary;
                defaultTargetPath = primary;
            }
            if (!targetHistory.contains(primary)) targetHistory.add(primary);
        }

        for (java.io.File repo : discovery.getGitRepositories()) {
            String path = repo.getAbsolutePath();
            if (!targetHistory.contains(path)) targetHistory.add(path);
        }

        if (currentTargetPath == null && editor != null) {
            org.eclipse.ui.IEditorInput input = editor.getEditorInput();
            if (input instanceof org.eclipse.ui.IFileEditorInput) {
                currentTargetPath = ((org.eclipse.ui.IFileEditorInput) input).getFile().getProject().getLocation().toOSString();
            }
        }

        if (currentTargetPath != null && !targetHistory.contains(currentTargetPath)) {
            targetHistory.add(0, currentTargetPath);
        }

        synchronizeSnapshotsWithDisk();
        updateTargetCombo();
        populateSnapshotCombo();
    }

    private String findEvoRepository() {
        if (editor != null) {
            org.eclipse.ui.IEditorInput input = editor.getEditorInput();
            if (input instanceof org.eclipse.ui.IFileEditorInput) {
                java.io.File root = ((org.eclipse.ui.IFileEditorInput) input).getFile().getProject().getLocation().toFile();
                if (isEvoRepo(root)) return root.getAbsolutePath();

                java.io.File parent = root.getParentFile();
                if (parent != null && isEvoRepo(parent)) return parent.getAbsolutePath();
            }
        }
        return null;
    }

    private boolean isEvoRepo(java.io.File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        return new java.io.File(dir, ".git").exists() &&
               (new java.io.File(dir, "eu.kalafatic.evolution.controller").exists() ||
                new java.io.File(dir, "pom.xml").exists());
    }

    private void setupJavaScriptBridges() {
        if (browser.isDisposed()) return;

        new BrowserFunction(browser, "navigatorFunction") {
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length >= 2) {
                    String id = (String) arguments[0];
                    String action = (String) arguments[1];
                    handleNavigatorAction(id, action);
                }
                return null;
            }
        };

        new BrowserFunction(browser, "logFunction") {
            @Override
            public Object function(Object[] arguments) {
                if (arguments.length >= 1) {
                    String msg = (String) arguments[0];
                    if ("ready".equals(msg)) {
                        isJsReady = true;
                        refreshBrowser();
                    } else {
                        eu.kalafatic.evolution.controller.log.Log.log("[ARCH_JS] " + msg);
                    }
                }
                return null;
            }
        };
    }

    private void handleNavigatorAction(String id, String action) {
        eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Navigator Action: " + action + " for ID: " + id);
        Display.getDefault().asyncExec(() -> {
            switch (action) {
                case "REFRESH":
                    scheduleRefresh();
                    break;
                case "DISCOVER":
                    handleDiscover();
                    break;
                case "GENERATE_METADATA":
                    handleGenerateMetadata();
                    break;
                case "EXPORT_HTML":
                    handleExport();
                    break;
                case "SAVE_JSON":
                    handleSaveModel();
                    break;
                case "BROWSE_TARGET":
                    handleBrowseTarget();
                    break;
                case "SET_TARGET":
                    setTargetPath(id);
                    break;
                case "SET_VIEW_MODE":
                    try {
                        setViewMode(ViewMode.valueOf(id));
                    } catch (Exception e) {}
                    break;
                case "OPEN":
                    if (id != null && !id.startsWith("domain:") && !id.startsWith("uc:")) {
                        eu.kalafatic.evolution.view.handlers.OpenSourceHandler.open(id);
                    }
                    break;
                case "CONTEXT":
                    // Generate context package for this node
                    break;
                case "EXPAND":
                case "SHOW_CHILDREN":
                case "SHOW_USE_CASES":
                case "SHOW_CLASSES":
                    expandAndRefresh(id, action);
                    break;
            }
        });
    }

    private void handleBrowseTarget() {
        org.eclipse.swt.widgets.DirectoryDialog dialog = new org.eclipse.swt.widgets.DirectoryDialog(getShell());
        dialog.setText("Select Architecture Target");
        dialog.setMessage("Choose a folder to analyze repository architecture.");
        if (currentTargetPath != null) dialog.setFilterPath(currentTargetPath);
        String selected = dialog.open();
        if (selected != null) {
            setTargetPath(selected);
        }
    }

    private void setTargetPath(String path) {
        if (path == null || path.isEmpty()) return;
        if (path.equals(currentTargetPath)) return;

        java.io.File f = new java.io.File(path);
        if (!f.exists() || !f.canRead()) {
            MessageBox box = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            box.setText("Invalid Path");
            box.setMessage("Target path does not exist or is not readable: " + path);
            box.open();
            return;
        }

        this.currentTargetPath = path;
        if (!targetHistory.contains(path)) {
            targetHistory.add(0, path);
        } else {
            targetHistory.remove(path);
            targetHistory.add(0, path);
        }

        synchronizeSnapshotsWithDisk();
        updateTargetCombo();
        populateSnapshotCombo();
        invalidateCache();
        scheduleRefresh();
    }

    private void synchronizeSnapshotsWithDisk() {
        if (currentTargetPath == null || orchestrator == null) return;
        java.io.File milestonesDir = new java.io.File(currentTargetPath, "milestones");
        if (!milestonesDir.exists()) return;

        java.io.File[] dirs = milestonesDir.listFiles(java.io.File::isDirectory);
        if (dirs == null) return;

        java.util.List<String> diskSnapshots = java.util.Arrays.stream(dirs)
                .filter(d -> d.getName().startsWith("genome_"))
                .map(d -> d.getName().replace("genome_", ""))
                .collect(java.util.stream.Collectors.toList());

        // Remove from model if not on disk
        orchestrator.getGenomeSnapshots().removeIf(s -> !diskSnapshots.contains(s.getTimestamp()));

        // Add to model if on disk but not in model
        for (String ts : diskSnapshots) {
            boolean exists = orchestrator.getGenomeSnapshots().stream().anyMatch(s -> ts.equals(s.getTimestamp()));
            if (!exists) {
                GenomeSnapshot snapshot = OrchestrationFactory.eINSTANCE.createGenomeSnapshot();
                snapshot.setTimestamp(ts);
                snapshot.setArchitectureArtifact("milestones/genome_" + ts + "/architecture.md");
                snapshot.setUseCaseArtifact("milestones/genome_" + ts + "/use_cases.md");
                snapshot.setMilestoneArtifact("milestones/genome_" + ts + "/milestone_v1.md");
                snapshot.setGenomeArtifact("milestones/genome_" + ts + "/genome.json");
                snapshot.setDashboardArtifact("milestones/genome_" + ts + "/milestone_dashboard.html");
                orchestrator.getGenomeSnapshots().add(snapshot);
            }
        }

        // Sort by timestamp and keep latest 8
        java.util.Collections.sort(orchestrator.getGenomeSnapshots(), (a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
        while (orchestrator.getGenomeSnapshots().size() > 8) {
            orchestrator.getGenomeSnapshots().remove(0);
        }
    }

    private void updateTargetCombo() {
        if (targetCombo == null || targetCombo.isDisposed()) return;
        targetCombo.setItems(targetHistory.toArray(new String[0]));
        if (currentTargetPath != null) {
            targetCombo.setText(currentTargetPath);
        }
    }

    private void invalidateCache() {
        if (orchestrator == null || orchestrator.getSharedMemory() == null) return;
        try {
            String sid = (orchestrator.getSelfDevSession() != null) ? orchestrator.getSelfDevSession().getId() : "discovery-session";
            orchestrator.setSharedMemory(
                eu.kalafatic.evolution.controller.orchestration.ConversationState.save(
                    orchestrator.getSharedMemory(), sid, "architecture_cache", ""));
        } catch (Exception e) {}
    }

    private void expandAndRefresh(String id, String action) {
        if (id == null) return;
        DesignModel model = extractModel();
        ComponentRecord comp = model.getComponents().stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst().orElse(null);

        if (comp != null) {
            if ("SHOW_USE_CASES".equals(action)) {
                String title = "Use Cases: " + comp.getName();
                String jsonItems = new JSONArray(comp.getUseCases()).toString();
                browser.execute("window.showPopup('" + title + "', " + jsonItems + ")");
                return;
            } else if ("SHOW_CLASSES".equals(action)) {
                String title = "Key Classes: " + comp.getName();
                String jsonItems = new JSONArray(comp.getKeyClasses()).toString();
                browser.execute("window.showPopup('" + title + "', " + jsonItems + ")");
                return;
            }
        }

        if ("SHOW_CHILDREN".equals(action)) {
            setViewMode(ViewMode.KNOWLEDGE_GRAPH);
        } else {
            scheduleRefresh();
        }
    }

    private void saveModelToCache(eu.kalafatic.evolution.controller.orchestration.TaskContext ctx, DesignModel model) {
        try {
            JSONObject obj = serializeModelToJSONObject(model);
            obj.put("targetPath", currentTargetPath);
            obj.put("viewMode", currentMode.name());
            obj.put("timestamp", System.currentTimeMillis());

            ctx.getOrchestrator().setSharedMemory(
                eu.kalafatic.evolution.controller.orchestration.ConversationState.save(
                    ctx.getOrchestrator().getSharedMemory(), ctx.getSessionId(), "architecture_cache", obj.toString()));
        } catch (Exception e) {}
    }

    private DesignModel loadModelFromCache() {
        if (orchestrator == null || orchestrator.getSharedMemory() == null) return null;
        try {
            String sid = (orchestrator.getSelfDevSession() != null) ? orchestrator.getSelfDevSession().getId() : "discovery-session";
            String json = eu.kalafatic.evolution.controller.orchestration.ConversationState.load(orchestrator.getSharedMemory(), sid).getMetadata("architecture_cache");
            if (json == null || json.isEmpty()) return null;

            JSONObject obj = new JSONObject(json);

            // Path-aware validation
            String cachedPath = obj.optString("targetPath");
            String cachedMode = obj.optString("viewMode");
            if (cachedPath != null && !cachedPath.equals(currentTargetPath)) return null;
            if (cachedMode != null && !cachedMode.equals(currentMode.name())) return null;

            DesignModel model = new DesignModel();
            model.setName(obj.optString("name", "Cached Architecture"));

            JSONArray comps = obj.optJSONArray("components");
            if (comps != null) {
                for (int i = 0; i < comps.length(); i++) {
                    JSONObject co = comps.getJSONObject(i);
                    ComponentRecord c = new ComponentRecord();
                    c.setId(co.optString("id"));
                    c.setName(co.optString("name"));
                    c.setType(co.optString("type"));
                    c.setDescription(co.optString("description"));
                    c.setPath(co.optString("path"));
                    c.setImportanceScore(co.optDouble("importance", 0.5));

                    JSONArray uc = co.optJSONArray("useCases");
                    if (uc != null) for (int j = 0; j < uc.length(); j++) c.getUseCases().add(uc.getString(j));

                    JSONArray kc = co.optJSONArray("keyClasses");
                    if (kc != null) for (int j = 0; j < kc.length(); j++) c.getKeyClasses().add(kc.getString(j));

                    model.getComponents().add(c);
                }
            }

            JSONArray rels = obj.optJSONArray("relationships");
            if (rels != null) {
                for (int i = 0; i < rels.length(); i++) {
                    JSONObject ro = rels.getJSONObject(i);
                    RelationshipRecord r = new RelationshipRecord();
                    r.setFrom(ro.optString("from"));
                    r.setTo(ro.optString("to"));
                    r.setType(ro.optString("type"));
                    model.getRelationships().add(r);
                }
            }
            return model;
        } catch (Exception e) {
            return null;
        }
    }

    private String serializeModelToJson(DesignModel model) {
        return serializeModelToJSONObject(model).toString();
    }

    private JSONObject serializeModelToJSONObject(DesignModel model) {
        JSONObject obj = new JSONObject();
        obj.put("name", model.getName());
        JSONArray comps = new JSONArray();
        for (ComponentRecord c : model.getComponents()) {
            JSONObject co = new JSONObject();
            co.put("id", c.getId());
            co.put("name", c.getName());
            co.put("type", c.getType());
            co.put("description", c.getDescription());
            co.put("path", c.getPath());
            co.put("importance", c.getImportanceScore());
            co.put("useCases", new JSONArray(c.getUseCases()));
            co.put("keyClasses", new JSONArray(c.getKeyClasses()));
            comps.put(co);
        }
        obj.put("components", comps);

        JSONArray rels = new JSONArray();
        for (RelationshipRecord r : model.getRelationships()) {
            JSONObject ro = new JSONObject();
            ro.put("from", r.getFrom());
            ro.put("to", r.getTo());
            ro.put("type", r.getType());
            rels.put(ro);
        }
        obj.put("relationships", rels);
        return obj;
    }

    @Override
    public void scheduleRefresh() {
        super.scheduleRefresh();
    }

    private void createControlPanel() {
        Composite toolbarComp = new Composite(this, SWT.NONE);
        GridLayout layout = new GridLayout(4, false);
        layout.marginHeight = 0;
        toolbarComp.setLayout(layout);
        toolbarComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Combo modeCombo = GUIFactory.INSTANCE.createCombo(toolbarComp, "", "Use Cases", "Subsystems", "Components", "Knowledge Graph");       
      
        modeCombo.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                switch (modeCombo.getSelectionIndex()) {
                    case 0: setViewMode(ViewMode.USE_CASES); break;
                    case 1: setViewMode(ViewMode.SUBSYSTEMS); break;
                    case 2: setViewMode(ViewMode.COMPONENTS); break;
                    case 3: setViewMode(ViewMode.KNOWLEDGE_GRAPH); break;
                }
            }
        });
        targetCombo = GUIFactory.INSTANCE.createCombo(toolbarComp);    
        ((GridData)targetCombo.getLayoutData()).widthHint = 400;
        targetCombo.setToolTipText("Select Target Project to analyze");
        updateTargetCombo();
        targetCombo.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                setTargetPath(targetCombo.getText());
            }
            @Override
            public void widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent e) {
                setTargetPath(targetCombo.getText());
            }
        });

        Button browseBtn = new Button(toolbarComp, SWT.PUSH);
        browseBtn.setText("Browse...");
        browseBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleBrowseTarget();
            }
        });

        org.eclipse.jface.action.ToolBarManager mgr = new org.eclipse.jface.action.ToolBarManager(SWT.FLAT | SWT.RIGHT);
        mgr.createControl(toolbarComp);

        mgr.add(new org.eclipse.jface.action.Action("Refresh") { @Override public void run() { scheduleRefresh(); } });
        mgr.add(new org.eclipse.jface.action.Separator());
        mgr.add(new org.eclipse.jface.action.Action("Discover") { @Override public void run() { handleDiscover(); } });
        mgr.add(new org.eclipse.jface.action.Separator());
        mgr.add(new org.eclipse.jface.action.Action("Export HTML") { @Override public void run() { handleExport(); } });
        mgr.add(new org.eclipse.jface.action.Action("Save JSON") { @Override public void run() { handleSaveModel(); } });
        mgr.add(new org.eclipse.jface.action.Separator());
        mgr.add(new org.eclipse.jface.action.Action("Generate Metadata") { @Override public void run() { handleGenerateMetadata(); } });

        mgr.update(true);

        Composite milestoneComp = new Composite(this, SWT.NONE);
        milestoneComp.setLayout(new GridLayout(3, false));
        milestoneComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        new org.eclipse.swt.widgets.Label(milestoneComp, SWT.NONE).setText("Genome Milestones:");

        snapshotCombo = new org.eclipse.swt.widgets.Combo(milestoneComp, SWT.READ_ONLY);
        snapshotCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        populateSnapshotCombo();
        snapshotCombo.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                int index = snapshotCombo.getSelectionIndex();
                if (index > 0) { // Index 0 is "Current"
                    showingSnapshot = true;
                    loadMilestoneDashboard(orchestrator.getGenomeSnapshots().get(index - 1));
                } else {
                    showingSnapshot = false;
                    scheduleRefresh();
                }
            }
        });

        Button genMilestoneBtn = new Button(milestoneComp, SWT.PUSH);
        genMilestoneBtn.setText("Generate Milestone");
        genMilestoneBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleGenerateMilestone();
            }
        });
    }

    private void populateSnapshotCombo() {
        if (snapshotCombo == null || snapshotCombo.isDisposed()) return;
        List<String> items = new ArrayList<>();
        items.add("Current (Live Architecture)");
        if (orchestrator != null) {
            for (GenomeSnapshot snapshot : orchestrator.getGenomeSnapshots()) {
                items.add("genome_" + snapshot.getTimestamp());
            }
        }
        snapshotCombo.setItems(items.toArray(new String[0]));
        snapshotCombo.select(0);
    }

    private void handleGenerateMilestone() {
        if (currentTargetPath == null) {
            MessageBox box = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK);
            box.setText("No Target Selected");
            box.setMessage("Please select a target repository before generating a milestone.");
            box.open();
            return;
        }

        java.io.File root = new java.io.File(currentTargetPath);
        String projectName = (orchestrator.getName() != null) ? orchestrator.getName() : root.getName();

        org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job("Generating Genome Milestone") {
            @Override
            protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                try {
                    String timestamp = milestoneGenerator.generateMilestone(root, projectName, "v1");

                    Display.getDefault().asyncExec(() -> {
                        GenomeSnapshot snapshot = OrchestrationFactory.eINSTANCE.createGenomeSnapshot();
                        snapshot.setTimestamp(timestamp);
                        snapshot.setArchitectureArtifact("milestones/genome_" + timestamp + "/architecture.md");
                        snapshot.setUseCaseArtifact("milestones/genome_" + timestamp + "/use_cases.md");
                        snapshot.setMilestoneArtifact("milestones/genome_" + timestamp + "/milestone_v1.md");
                        snapshot.setGenomeArtifact("milestones/genome_" + timestamp + "/genome.json");
                        snapshot.setDashboardArtifact("milestones/genome_" + timestamp + "/milestone_dashboard.html");

                        orchestrator.getGenomeSnapshots().add(snapshot);

                        // Explicit save
                        try {
                            eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance().saveResource(orchestrator.eResource());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        // Synchronize model with filesystem retention policy (latest 8)
                        java.io.File milestonesDir = new java.io.File(root, "milestones");
                        java.io.File[] dirs = milestonesDir.listFiles(java.io.File::isDirectory);
                        if (dirs != null) {
                            java.util.Set<String> existingTimestamps = java.util.Arrays.stream(dirs)
                                    .map(d -> d.getName().replace("genome_", ""))
                                    .collect(java.util.stream.Collectors.toSet());

                            orchestrator.getGenomeSnapshots().removeIf(s -> !existingTimestamps.contains(s.getTimestamp()));
                        }

                        while (orchestrator.getGenomeSnapshots().size() > 8) {
                            orchestrator.getGenomeSnapshots().remove(0);
                        }

                        populateSnapshotCombo();
                        snapshotCombo.select(snapshotCombo.getItemCount() - 1);
                        loadMilestoneDashboard(snapshot);
                    });

                    return org.eclipse.core.runtime.Status.OK_STATUS;
                } catch (Exception e) {
                    return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.IStatus.ERROR, "eu.kalafatic.evolution.view", "Milestone generation failed", e);
                }
            }
        };
        job.schedule();
    }

    private void loadMilestoneDashboard(GenomeSnapshot snapshot) {
        if (currentTargetPath == null || browser == null || browser.isDisposed()) return;

        java.io.File dashboardFile = new java.io.File(currentTargetPath, snapshot.getDashboardArtifact());
        if (dashboardFile.exists()) {
            try {
                String html = java.nio.file.Files.readString(dashboardFile.toPath());
                browser.setText(html);
                lastJson = ""; // Invalidate lastJson to force reload when switching back to Current
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            MessageBox box = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            box.setText("Dashboard Missing");
            box.setMessage("Milestone dashboard HTML not found: " + snapshot.getDashboardArtifact());
            box.open();
        }
    }

    private void hookContextMenu() {
        org.eclipse.jface.action.MenuManager menuMgr = new org.eclipse.jface.action.MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> fillContextMenu(manager));
        org.eclipse.swt.widgets.Menu menu = menuMgr.createContextMenu(browser);
        browser.setMenu(menu);
    }

    private void fillContextMenu(org.eclipse.jface.action.IMenuManager manager) {
        manager.add(new org.eclipse.jface.action.Action("Refresh") { @Override public void run() { scheduleRefresh(); } });
        manager.add(new org.eclipse.jface.action.Separator());
        manager.add(new org.eclipse.jface.action.Action("Zoom In") { @Override public void run() { if (browser != null) browser.execute("document.body.style.zoom = (parseFloat(document.body.style.zoom || 1) + 0.1);"); } });
        manager.add(new org.eclipse.jface.action.Action("Zoom Out") { @Override public void run() { if (browser != null) browser.execute("document.body.style.zoom = (parseFloat(document.body.style.zoom || 1) - 0.1);"); } });
        manager.add(new org.eclipse.jface.action.Action("Reset Zoom") { @Override public void run() { if (browser != null) browser.execute("document.body.style.zoom = 1.0;"); } });
        manager.add(new org.eclipse.jface.action.Separator());
        manager.add(new org.eclipse.jface.action.Action("Export Architecture (HTML)") { @Override public void run() { handleExport(); } });
        manager.add(new org.eclipse.jface.action.Action("Save Design Model (JSON)") { @Override public void run() { handleSaveModel(); } });
    }

    private void handleDiscover() {
        if (currentTargetPath == null) {
            handleBrowseTarget();
            if (currentTargetPath == null) return;
        }

        java.io.File root = new java.io.File(currentTargetPath);
        eu.kalafatic.evolution.controller.log.Log.log("[ARCH] Target path: " + currentTargetPath);
        eu.kalafatic.evolution.controller.log.Log.log("[ARCH] Mode: " + currentMode.name());
        eu.kalafatic.evolution.controller.log.Log.log("[ARCH] Starting scan...");

        org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job("Discovering Architecture") {
            @Override
            protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                try {
                    // 1. Physical Scan
                    eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner scanner = new eu.kalafatic.evolution.controller.mediation.scanner.TargetScanner();
                    eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot snapshot = scanner.scanToSnapshot(root, eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot.TargetType.PROJECT);
                    eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Physical Scan complete. Found " + snapshot.getNodes().size() + " nodes.");

                    // 2. AI Understanding (Mediated Mode Style)
                    if (orchestrator != null) {
                        String sid = (orchestrator.getSelfDevSession() != null && orchestrator.getSelfDevSession().getId() != null) ?
                                     orchestrator.getSelfDevSession().getId() : "discovery-session";

                        eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getOrCreateSession(sid);
                        eu.kalafatic.evolution.controller.orchestration.TaskContext ctx = (session instanceof eu.kalafatic.evolution.controller.orchestration.SessionContext) ?
                                ((eu.kalafatic.evolution.controller.orchestration.SessionContext)session).getTaskContext() : null;

                        if (ctx == null) {
                            ctx = new eu.kalafatic.evolution.controller.orchestration.TaskContext(orchestrator, root);
                            ctx.setSessionId(sid);
                            if (session instanceof eu.kalafatic.evolution.controller.orchestration.SessionContext) {
                                ((eu.kalafatic.evolution.controller.orchestration.SessionContext)session).setTaskContext(ctx);
                            }
                        }

                        monitor.subTask("Mediating High-Signal Context");
                        eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator curator = new eu.kalafatic.evolution.controller.mediation.analysis.ContextCurator();
                        java.util.List<String> candidates = curator.selectContext(snapshot, "architectural overview and key entry points", 32);

                        eu.kalafatic.evolution.controller.mediation.analysis.SemanticExtractor extractor = new eu.kalafatic.evolution.controller.mediation.analysis.SemanticExtractor();
                        extractor.extractToSnapshot(snapshot, candidates);

                        monitor.subTask("Synthesizing Reality Model");
                        eu.kalafatic.evolution.controller.agents.RealityDiscoveryAgent agent = new eu.kalafatic.evolution.controller.agents.RealityDiscoveryAgent(session);

                        // FIX: Avoid using Orchestrator.getAiService() which is undefined. Use TaskContext.getAiService().
                        agent.setAiService(ctx.getAiService());

                            eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel reality = agent.discover("Analyze repository architecture and key hotspots", ctx, currentTargetPath);
                        eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Reality Model synthesized: " + reality.getDomain());

                        // Save to metadata for extractModel to find it
                        ctx.getOrchestrationState().getMetadata().put("targetRealityModel", reality);

                        // Also trigger MetadataAgent for persistent sidecars
                        monitor.subTask("Generating AI Metadata Sidecars");
                        MetadataAgent generator = new MetadataAgent();
                        generator.generate(root, monitor);

                        // Persistent Cache in Shared Memory
                        saveModelToCache(ctx, extractModel());
                    }

                    scheduleRefresh();
                    return org.eclipse.core.runtime.Status.OK_STATUS;
                } catch (Exception e) {
                    return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.IStatus.ERROR, "eu.kalafatic.evolution.view", "Discovery failed", e);
                }
            }
        };
        job.schedule();
    }

    private void handleGenerateMetadata() {
        if (currentTargetPath == null) return;
        java.io.File root = new java.io.File(currentTargetPath);

        org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job("Generating AI Metadata") {
            @Override
            protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                try {
                    MetadataAgent generator = new MetadataAgent();
                    generator.generate(root, monitor);

                    Display.getDefault().asyncExec(() -> {
                        if (!getShell().isDisposed()) {
                            MessageBox box = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                            box.setText("Metadata Generation");
                            box.setMessage("AI Metadata generation completed for: " + root.getName());
                            box.open();
                        }
                    });
                    return org.eclipse.core.runtime.Status.OK_STATUS;
                } catch (Exception e) {
                    return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.IStatus.ERROR, "eu.kalafatic.evolution.view", "Failed to generate metadata", e);
                }
            }
        };
        job.schedule();
    }

    private void handleExport() {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { "*.html" });
        dialog.setFileName("architecture.html");
        String path = dialog.open();
        if (path != null) {
            try {
                DesignModel model = extractModel();
                eu.kalafatic.evolution.controller.orchestration.TaskContext context = new eu.kalafatic.evolution.controller.orchestration.TaskContext(orchestrator, null);
                DesignExporter.exportToHtml(renderer.render(model, currentMode.name(), currentTargetPath, targetHistory), new java.io.File(path), context);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleSaveModel() {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { "*.json" });
        dialog.setFileName("design_model.json");
        String path = dialog.open();
        if (path != null) {
            try {
                eu.kalafatic.evolution.controller.orchestration.TaskContext context = new eu.kalafatic.evolution.controller.orchestration.TaskContext(orchestrator, null);
                DesignExporter.saveModelAsJson(orchestrator.getSharedMemory(), new java.io.File(path), context);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void refreshBrowser() {
        if (browser == null || browser.isDisposed() || !isLoaded || showingSnapshot) return;

        Display.getDefault().asyncExec(() -> {
            if (browser == null || browser.isDisposed() || showingSnapshot) return;

            DesignModel model = extractModel();
            boolean targetChanged = currentTargetPath != null && !currentTargetPath.equals(lastTargetPath);

            // Re-render full template only if target path changed or it's the first load
            if (lastJson.isEmpty() || targetChanged) {
                eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Full browser reload triggered for: " + currentTargetPath);
                isJsReady = false; // Reset readiness as we are loading a new page
                browser.setText(renderer.render(model, currentMode.name(), currentTargetPath, defaultTargetPath, targetHistory));
                lastJson = renderer.serializeModel(model);
                lastTargetPath = currentTargetPath;
                return;
            }

            if (!isJsReady) {
                eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Skipping update - JS not ready yet.");
                return;
            }

            String json = renderer.serializeModel(model);
            if (json.equals(lastJson)) return;

            eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Updating graph with new JSON (" + (model != null ? model.getComponents().size() : 0) + " components)");
            lastJson = json;
            browser.execute("if(window.updateGraph) { window.updateGraph(" + json + "); } else { console.log('window.updateGraph not found'); }");
        });
    }

    private void initBrowser() {
        if (browser == null || browser.isDisposed()) return;
        browser.setText(renderer.render(null, currentMode.name(), currentTargetPath, defaultTargetPath, targetHistory));
    }

    public enum ViewMode {
        USE_CASES, SUBSYSTEMS, COMPONENTS, KNOWLEDGE_GRAPH
    }

    private ViewMode currentMode = ViewMode.COMPONENTS;

    public void setViewMode(ViewMode mode) {
        this.currentMode = mode;
        scheduleRefresh();
    }

    private DesignModel extractModel() {
        if (currentTargetPath == null) return createDefaultModel();
        java.io.File root = new java.io.File(currentTargetPath);

        // 1. Try to load from Cache first
        DesignModel cached = loadModelFromCache();
        if (cached != null) {
            eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Returning cached model for: " + currentTargetPath);
            return filterModel(cached, currentMode);
        }

        // 2. Initial view: physical scan for metadata
        DesignModel model = discoverArchitectureNodes(root);
        eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Discovered " + model.getComponents().size() + " components via node scan.");

        // Integrate Reality Discovery Model if present in orchestrator
        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            String sid = orchestrator.getSelfDevSession().getId();
            eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(sid);
            if (session instanceof eu.kalafatic.evolution.controller.orchestration.SessionContext) {
                eu.kalafatic.evolution.controller.orchestration.TaskContext ctx = ((eu.kalafatic.evolution.controller.orchestration.SessionContext)session).getTaskContext();
                if (ctx != null) {
                    Object trm = ctx.getOrchestrationState().getMetadata().get("targetRealityModel");
                    if (trm instanceof eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) {
                        convertRealityToModel((eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel) trm, model);
                    }
                }
            }
        }

        model.setName(root.getName() + " Architecture");

        if (model.getComponents().isEmpty()) {
            return createDefaultModel();
        }

        return filterModel(model, currentMode);
    }

    private void convertRealityToModel(eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel reality, DesignModel model) {
        if (reality.getDomain() != null) {
            ComponentRecord d = new ComponentRecord();
            d.setId("reality:domain");
            d.setName(reality.getDomain());
            d.setType("DOMAIN");
            d.setDescription(reality.getPurpose());
            model.getComponents().add(d);
        }

        // Subsystems
        for (eu.kalafatic.evolution.controller.mediation.model.Subsystem sub : reality.getSubsystems()) {
            ComponentRecord sr = new ComponentRecord();
            sr.setId("reality:subsystem:" + sub.getId());
            sr.setName(sub.getName());
            sr.setType("SUBSYSTEM");
            sr.setDescription(sub.getPurpose());
            sr.setImportanceScore(sub.getConfidence());
            model.getComponents().add(sr);

            for (String file : sub.getCriticalFiles()) {
                eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord rel = new eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord();
                rel.setFrom(sr.getId());
                rel.setTo(file);
                rel.setType("CONTAINS");
                model.getRelationships().add(rel);
            }
        }

        // Use Cases
        for (eu.kalafatic.evolution.controller.mediation.model.ArchitecturalUseCase uc : reality.getUseCases()) {
            ComponentRecord ur = new ComponentRecord();
            ur.setId("reality:uc:" + uc.getId());
            ur.setName(uc.getName());
            ur.setType("USE_CASE");
            ur.setDescription(uc.getDescription());
            ur.setImportanceScore(uc.getConfidence());
            ur.getProperties().add("Rationale: " + uc.getRationale());
            model.getComponents().add(ur);

            for (String comp : uc.getSupportingComponents()) {
                eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord rel = new eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord();
                rel.setFrom(ur.getId());
                rel.setTo(comp);
                rel.setType("SUPPORTED_BY");
                model.getRelationships().add(rel);
            }
            for (String file : uc.getSupportingFiles()) {
                eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord rel = new eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord();
                rel.setFrom(ur.getId());
                rel.setTo(file);
                rel.setType("EVIDENCE");
                model.getRelationships().add(rel);
            }
        }

        for (eu.kalafatic.evolution.controller.mediation.model.Hotspot h : reality.getHotspots()) {
            ComponentRecord hr = new ComponentRecord();
            hr.setId("reality:hotspot:" + h.getId());
            hr.setName(h.getName());
            hr.setType("HOTSPOT");
            hr.setDescription(h.getDescription());
            hr.setImportanceScore(h.getSignificance());
            model.getComponents().add(hr);

            for (String art : h.getRelatedArtifacts()) {
                eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord rel = new eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord();
                rel.setFrom(hr.getId());
                rel.setTo(art);
                rel.setType("HIGHLIGHTS");
                model.getRelationships().add(rel);
            }
        }

        for (String obj : reality.getObjectives()) {
            ComponentRecord o = new ComponentRecord();
            o.setId("reality:objective:" + obj.hashCode());
            o.setName(obj);
            o.setType("OBJECTIVE");
            model.getComponents().add(o);
        }
    }

    private DesignModel discoverArchitectureNodes(java.io.File root) {
        DesignModel model = new DesignModel();
        eu.kalafatic.utils.semantic.AIContextTool tool = new eu.kalafatic.utils.semantic.AIContextTool();
        Map<String, ComponentRecord> nodes = new HashMap<>();

        eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Scanning for metadata at: " + root.getAbsolutePath());
        // 1. Scan for .ai.json files
        scanForMetadata(root, root, tool, nodes, model);

        // 2. Parse ARCHITECTURE_CONTEXT.md if it exists
        java.io.File archCtx = new java.io.File(root, "ARCHITECTURE_CONTEXT.md");
        if (archCtx.exists()) {
            parseArchitectureContext(archCtx, nodes, model);
        }

        // 3. Fallback: Local Structure Discovery
        if (model.getComponents().isEmpty()) {
            discoverLocalStructure(root, root, model);
        }

        return model;
    }

    private void discoverLocalStructure(java.io.File current, java.io.File root, DesignModel model) {
        java.io.File[] files = current.listFiles();
        if (files == null) {
            eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Local structure: No files found in " + current.getAbsolutePath());
            return;
        }

        for (java.io.File f : files) {
            if (f.isDirectory()) {
                String name = f.getName();
                if (!name.startsWith(".") && !name.equals("target") && !name.equals("bin") && !name.equals("node_modules")) {
                    ComponentRecord rec = new ComponentRecord();
                    rec.setId(root.toURI().relativize(f.toURI()).getPath());
                    rec.setName(name);
                    rec.setType("MODULE");
                    rec.setDescription("Discovered module directory");
                    model.getComponents().add(rec);

                    // Only scan one level deep for fallback to keep it clean
                    if (current.equals(root)) {
                        discoverLocalStructure(f, root, model);
                    }
                }
            }
        }
    }

    private void scanForMetadata(java.io.File current, java.io.File root, eu.kalafatic.utils.semantic.AIContextTool tool, Map<String, ComponentRecord> nodes, DesignModel model) {
        java.io.File[] files = current.listFiles();
        if (files == null) return;

        if (current.equals(root)) eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] scanForMetadata: Processing " + files.length + " entries at root.");

        for (java.io.File f : files) {
            if (f.isDirectory()) {
                if (!f.getName().startsWith(".") && !f.getName().equals("target") && !f.getName().equals("bin")) {
                    scanForMetadata(f, root, tool, nodes, model);
                }
            } else if (f.getName().endsWith(".java") || f.getName().endsWith(".md") || f.getName().endsWith(".json")) {
                eu.kalafatic.utils.semantic.EvoMetadata meta = tool.loadMetadata(f);
                if (meta != null) {
                    eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Found Metadata for: " + f.getName());
                    ComponentRecord rec = new ComponentRecord();
                    rec.setId(meta.getPath() != null ? meta.getPath() : f.getName());
                    rec.setName(f.getName());
                    rec.setType(meta.getRole() != null ? meta.getRole().toUpperCase() : "COMPONENT");
                    rec.setDescription(meta.getSummary());
                    rec.setPath(meta.getPath());
                    rec.setImportanceScore(meta.getImportanceScore());
                    //rec.getUseCases().addAll(meta.getUseCases());
                   // rec.getKeyClasses().addAll(meta.getKeyClasses());

                    model.getComponents().add(rec);
                    nodes.put(rec.getId(), rec);

                    // Relationships from dependencyLinks
                    for (String dep : meta.getDependencyLinks()) {
                        eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord rel = new eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord();
                        rel.setFrom(rec.getId());
                        rel.setTo(dep);
                        rel.setType("DEPENDS_ON");
                        model.getRelationships().add(rel);
                    }
                }
            }
        }
    }

    private void parseArchitectureContext(java.io.File archCtx, Map<String, ComponentRecord> nodes, DesignModel model) {
        try {
            java.util.List<String> lines = java.nio.file.Files.readAllLines(archCtx.toPath());
            String currentDomain = null;
            for (String line : lines) {
                if (line.startsWith("* **")) {
                    // Domain definition: * **Orchestration**: ...
                    int end = line.indexOf("**", 4);
                    if (end > 4) {
                        String domain = line.substring(4, end);
                        ComponentRecord rec = new ComponentRecord();
                        rec.setId("domain:" + domain.toLowerCase());
                        rec.setName(domain);
                        rec.setType("SUBSYSTEM");
                        rec.setDescription(line.substring(end + 3));
                        model.getComponents().add(rec);
                        nodes.put(rec.getId(), rec);
                    }
                } else if (line.startsWith("* `")) {
                    // Component link: * `path/to/file`: description
                    int end = line.indexOf("`", 3);
                    if (end > 3) {
                        String path = line.substring(3, end);
                        ComponentRecord rec = nodes.get(path);
                        if (rec != null && currentDomain != null) {
                            eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord rel = new eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord();
                            rel.setFrom(rec.getId());
                            rel.setTo("domain:" + currentDomain.toLowerCase());
                            rel.setType("PART_OF");
                            model.getRelationships().add(rel);
                        }
                    }
                } else if (line.startsWith("## ")) {
                    String header = line.substring(3).trim();
                    if (!header.equalsIgnoreCase("Core Domains") && !header.equalsIgnoreCase("High-Importance Components")) {
                        currentDomain = header;
                    }
                }
            }
        } catch (java.io.IOException e) {}
    }

    private DesignModel filterModel(DesignModel model, ViewMode mode) {
        DesignModel filtered = new DesignModel();
        filtered.setName(model.getName() + " - " + mode.name());

        switch (mode) {
            case USE_CASES:
                filtered.setComponents(model.getComponents().stream()
                    .filter(c -> "USE_CASE".equals(c.getType()))
                    .collect(Collectors.toList()));
                List<String> ucIds = filtered.getComponents().stream().map(ComponentRecord::getId).collect(Collectors.toList());

                // Add supporting components
                List<ComponentRecord> supporting = new ArrayList<>();
                for (RelationshipRecord rel : model.getRelationships()) {
                    if (ucIds.contains(rel.getFrom()) && ("SUPPORTED_BY".equals(rel.getType()) || "EVIDENCE".equals(rel.getType()))) {
                        model.getComponents().stream().filter(c -> c.getId().equals(rel.getTo())).findFirst().ifPresent(supporting::add);
                        filtered.getRelationships().add(rel);
                    }
                }
                supporting.forEach(c -> { if (!filtered.getComponents().contains(c)) filtered.getComponents().add(c); });
                break;

            case SUBSYSTEMS:
                filtered.setComponents(model.getComponents().stream()
                    .filter(c -> "SUBSYSTEM".equals(c.getType()) || "DOMAIN".equals(c.getType()))
                    .collect(Collectors.toList()));
                List<String> subIds = filtered.getComponents().stream().map(ComponentRecord::getId).collect(Collectors.toList());

                // Add contained artifacts
                List<ComponentRecord> contained = new ArrayList<>();
                for (RelationshipRecord rel : model.getRelationships()) {
                    if (subIds.contains(rel.getFrom()) && "CONTAINS".equals(rel.getType())) {
                        model.getComponents().stream().filter(c -> c.getId().equals(rel.getTo())).findFirst().ifPresent(contained::add);
                        filtered.getRelationships().add(rel);
                    }
                }
                contained.forEach(c -> { if (!filtered.getComponents().contains(c)) filtered.getComponents().add(c); });
                break;

            case COMPONENTS:
                filtered.setComponents(model.getComponents().stream()
                    .filter(c -> !"USE_CASE".equals(c.getType()) && !"SUBSYSTEM".equals(c.getType()) && !"DOMAIN".equals(c.getType()))
                    .collect(Collectors.toList()));
                List<String> compIds = filtered.getComponents().stream().map(ComponentRecord::getId).collect(Collectors.toList());
                filtered.setRelationships(model.getRelationships().stream()
                    .filter(r -> compIds.contains(r.getFrom()) && compIds.contains(r.getTo()))
                    .collect(Collectors.toList()));
                break;

            case KNOWLEDGE_GRAPH:
                return model;
        }

        return filtered;
    }

    private DesignModel extractModelLegacy() {
        DesignModel model = new DesignModel();
        if (orchestrator == null) return model;

        model.setName(orchestrator.getName() != null ? orchestrator.getName() : "Evolution Architecture");

        int i = 0;

        // 1. Static Configuration Components
        if (orchestrator.getGit() != null) {
            ComponentRecord git = new ComponentRecord();
            git.setName("Git: " + (orchestrator.getGit().getBranch() != null ? orchestrator.getGit().getBranch() : "master"));
            git.setType("VCS");
            git.setX(50); git.setY(50);
            if (orchestrator.getGit().getRepositoryUrl() != null) git.getProperties().add("URL: " + orchestrator.getGit().getRepositoryUrl());
            model.getComponents().add(git);
        }

        if (orchestrator.getOllama() != null) {
            ComponentRecord ollama = new ComponentRecord();
            ollama.setName("Ollama: " + (orchestrator.getOllama().getModel() != null ? orchestrator.getOllama().getModel() : "local"));
            ollama.setType("LLM Provider");
            ollama.setX(300); ollama.setY(50);
            if (orchestrator.getOllama().getUrl() != null) ollama.getProperties().add("URL: " + orchestrator.getOllama().getUrl());
            model.getComponents().add(ollama);
        }

        if (orchestrator.getLlm() != null) {
            ComponentRecord llm = new ComponentRecord();
            llm.setName("LLM: " + (orchestrator.getLlm().getModel() != null ? orchestrator.getLlm().getModel() : "gpt-4o"));
            llm.setType("Model");
            llm.setX(550); llm.setY(50);
            llm.getProperties().add("Temp: " + orchestrator.getLlm().getTemperature());
            model.getComponents().add(llm);
        }

        i = 2; // Offset for agents/tasks

        // 2. Agents as components
        for (eu.kalafatic.evolution.model.orchestration.Agent agent : orchestrator.getAgents()) {
            ComponentRecord rec = new ComponentRecord();
            rec.setName(agent.getId());
            rec.setType(agent.getType() != null ? agent.getType() : "Agent");
            rec.setX(50 + (i * 220) % 880);
            rec.setY(250 + (i / 4) * 200);
            model.getComponents().add(rec);
            i++;
        }

        // 3. Tasks as components
        for (eu.kalafatic.evolution.model.orchestration.Task task : orchestrator.getTasks()) {
            ComponentRecord rec = new ComponentRecord();
            String taskName = task.getName() != null ? task.getName() : (task.getId() != null ? task.getId() : "Task " + task.hashCode());
            rec.setName(taskName);
            rec.setType("Task");
            rec.setX(50 + (i * 220) % 880);
            rec.setY(250 + (i / 4) * 200);

            if (task.getStatus() != null) {
                rec.getProperties().add("Status: " + task.getStatus().toString());
            }
            if (task.getRating() > 0) rec.getProperties().add("Rating: " + task.getRating());

            model.getComponents().add(rec);

            // Relationships from task hierarchy/flow
            for (eu.kalafatic.evolution.model.orchestration.Task next : task.getNext()) {
                RelationshipRecord rel = new RelationshipRecord();
                rel.setFrom(taskName);
                String nextName = next.getName() != null ? next.getName() : (next.getId() != null ? next.getId() : "Task " + next.hashCode());
                rel.setTo(nextName);
                rel.setType("next");
                model.getRelationships().add(rel);
            }
            i++;
        }

        // 4. Iterations if present
        if (orchestrator.getSelfDevSession() != null) {
            i = 0;
            for (eu.kalafatic.evolution.model.orchestration.Iteration iter : orchestrator.getSelfDevSession().getIterations()) {
                ComponentRecord rec = new ComponentRecord();
                String iterName = iter.getId() != null ? iter.getId() : "Iteration " + iter.hashCode();
                rec.setName(iterName);
                rec.setType("Iteration");
                rec.setX(50 + (i * 220) % 880);
                rec.setY(250 + (i / 4) * 200);
                if (iter.getPhase() != null) rec.getProperties().add("Phase: " + iter.getPhase());
                if (iter.getStatus() != null) rec.getProperties().add("Status: " + iter.getStatus());
                model.getComponents().add(rec);
                i++;
            }
        }

        // 4. Shared Memory elements if they look like components
        String sharedMemory = orchestrator.getSharedMemory();
        if (sharedMemory != null && sharedMemory.startsWith("{")) {
            try {
                JSONObject json = new JSONObject(sharedMemory);
                if (json.has("components")) {
                    JSONArray comps = json.getJSONArray("components");
                    for (int j = 0; j < comps.length(); j++) {
                        JSONObject c = comps.getJSONObject(j);
                        ComponentRecord rec = new ComponentRecord();
                        rec.setName(c.optString("name", "Unknown"));
                        rec.setType(c.optString("type", "Component"));
                        rec.setX(c.optInt("x", 500));
                        rec.setY(c.optInt("y", 500));
                        model.getComponents().add(rec);
                    }
                }
            } catch (Exception e) {
                // Ignore parsing errors for shared memory
            }
        }

        if (model.getComponents().isEmpty()) {
            return createDefaultModel();
        }

        return model;
    }

    private DesignModel createDefaultModel() {
        DesignModel model = new DesignModel();

        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            eu.kalafatic.evolution.model.orchestration.SelfDevSession session = orchestrator.getSelfDevSession();
            model.setName("Self-Development Session: " + (session.getId() != null ? session.getId() : "Active"));

            int i = 0;
            for (eu.kalafatic.evolution.model.orchestration.Iteration iter : session.getIterations()) {
                ComponentRecord comp = new ComponentRecord();
                comp.setName(iter.getId());
                comp.setType("Step");
                comp.setX(100 + (i * 250) % 750);
                comp.setY(100 + (i / 3) * 200);
                model.getComponents().add(comp);

                if (i > 0) {
                    RelationshipRecord rel = new RelationshipRecord();
                    rel.setFrom(session.getIterations().get(i - 1).getId());
                    rel.setTo(iter.getId());
                    rel.setType("evolves");
                    model.getRelationships().add(rel);
                }
                i++;
            }
            if (model.getComponents().isEmpty()) {
                ComponentRecord comp = new ComponentRecord();
                comp.setName("Session Active");
                comp.setType("Status");
                comp.setX(100); comp.setY(100);
                model.getComponents().add(comp);
            }
            return model;
        }

        if (orchestrator != null && !orchestrator.getTasks().isEmpty()) {
            model.setName("Active Session Tasks");
            int i = 0;
            boolean hasClassTasks = orchestrator.getTasks().stream()
                .anyMatch(t -> t.getName().toLowerCase().contains("class") || t.getName().toLowerCase().contains("interface"));

            for (eu.kalafatic.evolution.model.orchestration.Task task : orchestrator.getTasks()) {
                if (!hasClassTasks || task.getName().toLowerCase().contains("class") || task.getName().toLowerCase().contains("interface")) {
                    ComponentRecord comp = new ComponentRecord();
                    String name = task.getName().replace("Create", "").replace("create", "").replace("class", "").replace("java", "").replace(".", "").trim();
                    if (name.isEmpty()) name = task.getId() != null ? task.getId() : "Task" + i;
                    comp.setName(name);
                    comp.setType(hasClassTasks ? "Class" : "Task");
                    comp.setX(100 + (i * 220) % 660);
                    comp.setY(100 + (i / 3) * 180);

                    if (task.getDescription() != null) {
                        String desc = task.getDescription();
                        if (desc.contains("method") || desc.contains("(")) {
                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\w+)\\s*\\(").matcher(desc);
                            while (m.find()) {
                                if (!m.group(1).equalsIgnoreCase("create")) comp.getMethods().add(m.group(1) + "()");
                            }
                        }
                    }

                    model.getComponents().add(comp);
                    i++;
                }
            }
            if (!model.getComponents().isEmpty()) return model;
        }

        model.setName("Darwinian Evolution (Default)");

        ComponentRecord engine = new ComponentRecord();
        engine.setName("DarwinEngine"); engine.setType("Engine"); engine.setX(100); engine.setY(100);

        ComponentRecord mdm = new ComponentRecord();
        mdm.setName("DesignModel"); mdm.setType("Data"); mdm.setX(400); mdm.setY(100);

        RelationshipRecord rel = new RelationshipRecord();
        rel.setFrom("DarwinEngine"); rel.setTo("DesignModel"); rel.setType("manages");

        model.getComponents().add(engine);
        model.getComponents().add(mdm);
        model.getRelationships().add(rel);

        return model;
    }

    @Override
    protected void refreshUI() {
        refreshBrowser();
    }
}
