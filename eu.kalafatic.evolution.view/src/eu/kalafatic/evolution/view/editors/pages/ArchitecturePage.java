package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.browser.BrowserFunction;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.DesignExporter;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.agents.MetadataAgent;
import eu.kalafatic.evolution.controller.orchestration.design.DesignRenderer;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * @evo:19:A reason=dynamic-architecture-page
 */
public class ArchitecturePage extends AEvoPage {
    private Browser browser;
    private DesignRenderer renderer = new DesignRenderer();
    private Runnable refreshRunnable = this::refreshBrowser;

    public ArchitecturePage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        this.setLayout(new GridLayout(1, false));

        createControlPanel();

        this.browser = new Browser(this, SWT.NONE);
        this.browser.setLayoutData(new GridData(GridData.FILL_BOTH));

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
                    eu.kalafatic.evolution.controller.log.Log.log("[ARCH_JS] " + arguments[0]);
                }
                return null;
            }
        };

        hookContextMenu();
        refreshBrowser();
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
            String json = serializeModelToJson(model);
            ctx.getOrchestrator().setSharedMemory(
                eu.kalafatic.evolution.controller.orchestration.ConversationState.save(
                    ctx.getOrchestrator().getSharedMemory(), ctx.getSessionId(), "architecture_cache", json));
        } catch (Exception e) {}
    }

    private DesignModel loadModelFromCache() {
        if (orchestrator == null || orchestrator.getSharedMemory() == null) return null;
        try {
            String sid = (orchestrator.getSelfDevSession() != null) ? orchestrator.getSelfDevSession().getId() : "discovery-session";
            String json = eu.kalafatic.evolution.controller.orchestration.ConversationState.load(orchestrator.getSharedMemory(), sid).getMetadata("architecture_cache");
            if (json == null || json.isEmpty()) return null;

            JSONObject obj = new JSONObject(json);
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
        return obj.toString();
    }

    @Override
    public void scheduleRefresh() {
        super.scheduleRefresh();
    }

    private void createControlPanel() {
        Composite toolbarComp = new Composite(this, SWT.NONE);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        toolbarComp.setLayout(layout);
        toolbarComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        org.eclipse.swt.widgets.Combo modeCombo = new org.eclipse.swt.widgets.Combo(toolbarComp, SWT.READ_ONLY);
        modeCombo.setItems(new String[] { "Use Cases", "Subsystems", "Components", "Knowledge Graph" });
        modeCombo.select(2); // Components
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
        if (editor == null) return;
        org.eclipse.ui.IEditorInput input = editor.getEditorInput();
        if (input instanceof org.eclipse.ui.IFileEditorInput) {
            org.eclipse.core.resources.IProject project = ((org.eclipse.ui.IFileEditorInput) input).getFile().getProject();
            java.io.File root = project.getLocation().toFile();
            eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Starting Discovery for Project: " + project.getName() + " at " + root.getAbsolutePath());

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

                            eu.kalafatic.evolution.controller.mediation.model.TargetRealityModel reality = agent.discover("Analyze repository architecture and key hotspots", ctx, snapshot);
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
    }

    private void handleGenerateMetadata() {
        if (editor == null) return;
        org.eclipse.ui.IEditorInput input = editor.getEditorInput();
        if (input instanceof org.eclipse.ui.IFileEditorInput) {
            org.eclipse.core.resources.IProject project = ((org.eclipse.ui.IFileEditorInput) input).getFile().getProject();
            java.io.File root = project.getLocation().toFile();

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
                                box.setMessage("AI Metadata generation completed for: " + project.getName());
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
                DesignExporter.exportToHtml(renderer.render(model), new java.io.File(path), context);
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
        if (browser == null || browser.isDisposed()) return;
        Display.getDefault().asyncExec(() -> {
            if (browser == null || browser.isDisposed()) return;
            DesignModel model = extractModel();
            browser.setText(renderer.render(model, currentMode.name()));
        });
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
        if (editor == null) return createDefaultModel();
        org.eclipse.ui.IEditorInput input = editor.getEditorInput();
        if (!(input instanceof org.eclipse.ui.IFileEditorInput)) return createDefaultModel();

        org.eclipse.core.resources.IProject project = ((org.eclipse.ui.IFileEditorInput) input).getFile().getProject();
        java.io.File root = project.getLocation().toFile();

        // 1. Try to load from Cache first
        DesignModel cached = loadModelFromCache();
        if (cached != null) {
            eu.kalafatic.evolution.controller.log.Log.log("[ARCH_PAGE] Returning cached model: " + cached.getComponents().size() + " components.");
            return filterModel(cached, currentMode);
        }

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

        model.setName(project.getName() + " Architecture");

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
                Map<String, ComponentRecord> ucNodes = new HashMap<>();
                for (ComponentRecord comp : model.getComponents()) {
                    for (String uc : comp.getUseCases()) {
                        ComponentRecord ucNode = ucNodes.computeIfAbsent(uc, k -> {
                            ComponentRecord r = new ComponentRecord();
                            r.setId("uc:" + k);
                            r.setName(k);
                            r.setType("USE_CASE");
                            filtered.getComponents().add(r);
                            return r;
                        });
                        eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord rel = new eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord();
                        rel.setFrom(comp.getId());
                        rel.setTo(ucNode.getId());
                        rel.setType("IMPLEMENTS");
                        filtered.getRelationships().add(rel);
                        if (!filtered.getComponents().contains(comp)) filtered.getComponents().add(comp);
                    }
                }
                break;

            case SUBSYSTEMS:
                filtered.setComponents(model.getComponents().stream()
                    .filter(c -> "SUBSYSTEM".equals(c.getType()) || "DOMAIN".equals(c.getType()) || c.getImportanceScore() > 0.8)
                    .collect(Collectors.toList()));
                List<String> ids = filtered.getComponents().stream().map(ComponentRecord::getId).collect(Collectors.toList());
                filtered.setRelationships(model.getRelationships().stream()
                    .filter(r -> ids.contains(r.getFrom()) && ids.contains(r.getTo()))
                    .collect(Collectors.toList()));
                break;

            case COMPONENTS:
                filtered.setComponents(model.getComponents().stream()
                    .filter(c -> !"USE_CASE".equals(c.getType()))
                    .collect(Collectors.toList()));
                filtered.setRelationships(model.getRelationships().stream()
                    .filter(r -> !"IMPLEMENTS".equals(r.getType()))
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
