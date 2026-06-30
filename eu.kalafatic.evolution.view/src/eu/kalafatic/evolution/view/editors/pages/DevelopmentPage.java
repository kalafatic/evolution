package eu.kalafatic.evolution.view.editors.pages;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBootstrapController;
import eu.kalafatic.evolution.controller.workflow.RuntimeEvent;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventBus;
import eu.kalafatic.evolution.controller.workflow.RuntimeEventListener;
import eu.kalafatic.evolution.view.projection.ProjectionService;
import eu.kalafatic.evolution.view.projection.RuntimeProjection;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.utils.constants.FUIConstants;
import eu.kalafatic.evolution.view.application.Activator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.development.InteractiveWorkflowGroup;
import eu.kalafatic.evolution.view.editors.pages.development.SupervisorGroup;
import eu.kalafatic.evolution.view.editors.pages.development.VizGroup;
import eu.kalafatic.evolution.view.editors.pages.iteration.SelfDevEditDialog;
import eu.kalafatic.utils.factories.GUIFactory;

public class DevelopmentPage extends AEvoPage {

    public static class SelfDevRow {
        public static final String SELF_DEV_LOOP = "Self-Dev Loop";
        public static final String EVO_RCP = "Evo RCP";
        public static final String GIT_CHECK = "Git Check";
        public static final String MAVEN_CHECK = "Maven Check";
        public static final String LLM_CHECK = "LLM Check";
        public static final String GENOME_CHECK = "Genome Check";
        public static final String PERM_CHECK = "Permissions Check";

        public String name;
        public String path;
        public String status;

        public SelfDevRow(String name, String path, String status) {
            this.name = name;
            this.path = path;
            this.status = status;
        }
    }

    private IterationMemoryService memoryService;
    private SelfDevBootstrapController bootstrapController;
    private File projectRoot;

    private Label sessionStatusLabel;
    private Label sessionProgressLabel;
    private TableViewer selfDevTable;
    private ImageRegistry imageRegistry;
    private java.util.Timer pollTimer;

    private VizGroup vizGroup;
    private InteractiveWorkflowGroup workflowGroup;
    private SupervisorGroup supervisorGroup;
    private ArchitecturePage archViz;
    private boolean isLoaded = false;
    private int initRetries = 0;
    private static final int MAX_INIT_RETRIES = 5;
    private String lastJson = "";
    private String lastSupervisorStatusStr = "";

    public DevelopmentPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        this.setLayout(new GridLayout(1, false));

        initImageRegistry();
        initMemoryService();
        createControl();
    }

    private void initImageRegistry() {
        this.imageRegistry = new ImageRegistry(Display.getDefault());
        registerImage("play", "eu.kalafatic.utils", "icons/actions/play.png");
        registerImage("pause", "eu.kalafatic.utils", "icons/actions/pause.png");
        registerImage("stop", "eu.kalafatic.utils", "icons/actions/stop.png");
        registerImage("resume", "eu.kalafatic.utils", "icons/actions/restart.png");
    }

    private void registerImage(String key, String pluginId, String path) {
        ImageDescriptor desc = Activator.getImageDescriptor(pluginId, path);
        if (desc != null) {
            imageRegistry.put(key, desc);
        }
    }

    private void initMemoryService() {
        this.projectRoot = null;
        if (orchestrator != null && orchestrator.eResource() != null) {
            org.eclipse.emf.common.util.URI uri = orchestrator.eResource().getURI();
            if (uri.isPlatformResource()) {
                org.eclipse.core.resources.IResource res = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().findMember(uri.toPlatformString(true));
                if (res != null) projectRoot = res.getProject().getLocation().toFile();
            } else if (uri.isFile()) {
                projectRoot = new File(uri.toFileString()).getParentFile();
            }
        }
        if (projectRoot == null) {
            IEditorInput input = editor.getEditorInput();
            if (input instanceof IFileEditorInput) {
                projectRoot = ((IFileEditorInput) input).getFile().getProject().getLocation().toFile();
            }
        }
        if (projectRoot != null) {
            this.memoryService = new IterationMemoryService(projectRoot);
            this.bootstrapController = new SelfDevBootstrapController(projectRoot, orchestrator);
        }
    }

    private void createControl() {
        Composite container = toolkit.createComposite(this);
        container.setLayout(new GridLayout(1, false));

        // 1. Self-Development Section
        Composite selfDevComp = GUIFactory.INSTANCE.createExpandableGroup(toolkit, container, "Self-Development", 1, true);
        selfDevComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite sdStatusComp = toolkit.createComposite(selfDevComp);
        sdStatusComp.setLayout(new GridLayout(2, false));
        sdStatusComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        sessionStatusLabel = toolkit.createLabel(sdStatusComp, "Session: READY");
        sessionStatusLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
        sessionProgressLabel = toolkit.createLabel(sdStatusComp, "Progress: 0%");

        selfDevTable = new TableViewer(selfDevComp, SWT.BORDER | SWT.FULL_SELECTION);
        Table sdTable = selfDevTable.getTable();
        sdTable.setHeaderVisible(true);
        sdTable.setLinesVisible(true);
        GridData gdSdTable = new GridData(GridData.FILL_HORIZONTAL);
        gdSdTable.heightHint = 100;
        sdTable.setLayoutData(gdSdTable);

        createSelfDevColumns();
        selfDevTable.setContentProvider(ArrayContentProvider.getInstance());
        List<SelfDevRow> sdData = new ArrayList<>();
        sdData.add(new SelfDevRow(SelfDevRow.SELF_DEV_LOOP, "orchestrator", "ready"));
        sdData.add(new SelfDevRow(SelfDevRow.EVO_RCP, "/xx/", "ready"));
        sdData.add(new SelfDevRow(SelfDevRow.GIT_CHECK, "supervisor.git", "ready"));
        sdData.add(new SelfDevRow(SelfDevRow.MAVEN_CHECK, "supervisor.maven", "ready"));
        sdData.add(new SelfDevRow(SelfDevRow.LLM_CHECK, "supervisor.llm", "ready"));
        sdData.add(new SelfDevRow(SelfDevRow.GENOME_CHECK, "supervisor.genome", "ready"));
        sdData.add(new SelfDevRow(SelfDevRow.PERM_CHECK, "supervisor.fs", "ready"));
        selfDevTable.setInput(sdData);

        sdTable.addListener(SWT.MouseDown, event -> {
            org.eclipse.swt.graphics.Point pt = new org.eclipse.swt.graphics.Point(event.x, event.y);
            org.eclipse.swt.widgets.TableItem item = sdTable.getItem(pt);
            if (item != null) {
                for (int i = 0; i < sdTable.getColumnCount(); i++) {
                    org.eclipse.swt.graphics.Rectangle rect = item.getBounds(i);
                    if (rect.contains(pt)) {
                        handleSelfDevAction((SelfDevRow) item.getData(), i);
                    }
                }
            }
        });

        // 1.5 Supervisor Group
        supervisorGroup = new SupervisorGroup(toolkit, container, editor, orchestrator);

        // 2. Network Visualization
        vizGroup = new VizGroup(toolkit, container, editor, orchestrator, this);

        // 2.5 Interactive AI Workflow
        String sessionId = (orchestrator != null && orchestrator.getSelfDevSession() != null) ?
            orchestrator.getSelfDevSession().getId() : "Default";
        workflowGroup = new eu.kalafatic.evolution.view.editors.pages.development.InteractiveWorkflowGroup(
            toolkit, container, editor, orchestrator, sessionId);

        // 3. Architecture Visualization
        Composite archGroup = GUIFactory.INSTANCE.createExpandableGroup(toolkit, container, "Architecture Visualization", 1, false, true);
        archGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite archComp = toolkit.createComposite(archGroup);
        archComp.setLayout(new org.eclipse.swt.layout.FillLayout());
        GridData archGd = new GridData(GridData.FILL_BOTH);
        archGd.minimumHeight = 800;
        archComp.setLayoutData(archGd);
        archViz = new ArchitecturePage(archComp, editor, orchestrator);

        this.setContent(container);
        
        Display.getCurrent().asyncExec(() -> {
			if (!isDisposed()) {
				refreshBrowser();
				container.layout(true, true);
			}
		});
    }

    private class SelfDevLabelProvider extends ColumnLabelProvider implements ITableColorProvider {
        private final int col;

        public SelfDevLabelProvider(int col) {
            this.col = col;
        }

        @Override
        public String getText(Object element) {
            SelfDevRow row = (SelfDevRow) element;
            switch (col) {
                case 0: // Action
                    if ("running".equals(row.status) || "starting".equals(row.status) || "building".equals(row.status) || "evaluating".equals(row.status)) {
                        return "\u23F8 \u23F9"; // pause stop
                    } else if ("paused".equals(row.status)) {
                        return "\u25B6 \u23F9"; // play stop
                    } else {
                        return "\u25B6"; // play
                    }
                case 1: // Edit
                    if (SelfDevRow.SELF_DEV_LOOP.equals(row.name) || row.name.endsWith("Check")) return "\u270E";
                    return "";
                case 2: return row.name;
                case 3: return row.path;
                case 4: return row.status;
                default: return "";
            }
        }

        @Override
        public Image getImage(Object element) {
            if (col == 0) {
                SelfDevRow row = (SelfDevRow) element;
                if ("running".equals(row.status) || "starting".equals(row.status) || "building".equals(row.status) || "evaluating".equals(row.status)) {
                    return imageRegistry.get("pause");
                } else {
                    return imageRegistry.get("play");
                }
            }
            return null;
        }

        @Override
        public Color getForeground(Object element, int columnIndex) {
            return null;
        }

        @Override
        public Color getBackground(Object element, int columnIndex) {
            SelfDevRow row = (SelfDevRow) element;
            String status = row.status.toLowerCase();
            if (status.contains("error") || status.contains("fail") || status.contains("missing")) {
                return FUIConstants.LIGHT_RED;
            } else if (status.equals("checked") || status.equals("success")) {
                return FUIConstants.LIGHT_GREEN;
            } else if (status.equals("ready")) {
                return FUIConstants.GRADIENT; // Light orange-ish
            }
            return null;
        }
    }

    private void createSelfDevColumns() {
        String[] titles = { "Action", "Edit", "Name", "Path/URL", "Status" };
        int[] bounds = { 100, 50, 150, 250, 150 };

        for (int i = 0; i < titles.length; i++) {
            TableViewerColumn col = createTableViewerColumn(selfDevTable, titles[i], bounds[i], i);
            col.setLabelProvider(new SelfDevLabelProvider(i));
        }
    }

    private TableViewerColumn createTableViewerColumn(TableViewer viewer, String title, int bound, final int colNumber) {
        final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
        final org.eclipse.swt.widgets.TableColumn column = viewerColumn.getColumn();
        column.setText(title);
        column.setWidth(bound);
        column.setResizable(true);
        column.setMoveable(true);
        return viewerColumn;
    }

    @Override
    protected String getCurrentSessionName() {
        if (editor != null && editor.getAiChatPage() != null) {
            String chatSid = editor.getAiChatPage().getCurrentSessionName();
            if (chatSid != null && !chatSid.equals("Default")) {
                return chatSid;
            }
        }
        return (orchestrator != null && orchestrator.getSelfDevSession() != null) ?
                orchestrator.getSelfDevSession().getId() : super.getCurrentSessionName();
    }

    private void handleSelfDevAction(SelfDevRow row, int columnIndex) {
        if (columnIndex == 0) { // Action
            if (SelfDevRow.SELF_DEV_LOOP.equals(row.name)) {
                RuntimeProjection projection = ProjectionService.getInstance().getProjection(getCurrentSessionName());
                if (projection.isRunning()) {
                    OrchestratorServiceImpl.getInstance().shutdownSession(getCurrentSessionName());
                } else {
                    TaskRequest request = new TaskRequest("Start Self-Dev Bootstrap", projectRoot);
                    request.getContext().put("orchestrator", orchestrator);
                    request.getContext().put("sessionId", getCurrentSessionName());
                    OrchestratorServiceImpl.getInstance().submit(getCurrentSessionName(), request);
                }
            } else if (SelfDevRow.GIT_CHECK.equals(row.name)) {
                row.status = bootstrapController.check("GIT");
                selfDevTable.refresh(row);
            } else if (SelfDevRow.MAVEN_CHECK.equals(row.name)) {
                row.status = bootstrapController.check("MAVEN");
                selfDevTable.refresh(row);
            } else if (SelfDevRow.LLM_CHECK.equals(row.name)) {
                row.status = bootstrapController.check("LLM");
                selfDevTable.refresh(row);
            } else if (SelfDevRow.GENOME_CHECK.equals(row.name)) {
                row.status = bootstrapController.check("GENOME");
                selfDevTable.refresh(row);
            } else if (SelfDevRow.PERM_CHECK.equals(row.name)) {
                row.status = bootstrapController.check("PERMISSIONS");
                selfDevTable.refresh(row);
            } else {
                if ("running".equals(row.status)) {
                    row.status = "paused";
                } else if ("paused".equals(row.status)) {
                    row.status = "running";
                } else {
                    row.status = "running";
                }
                selfDevTable.refresh(row);
            }
        } else if (columnIndex == 1) { // Edit
            if (SelfDevRow.SELF_DEV_LOOP.equals(row.name) || row.name.endsWith("Check")) {
                openSelfDevEditDialog();
            }
        }
    }

    private void openSelfDevEditDialog() {
        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            SelfDevEditDialog dialog = new SelfDevEditDialog(getShell(), orchestrator.getSelfDevSession(), this);
            if (dialog.open() == org.eclipse.jface.window.Window.OK) {
                updateSessionStatus();
            }
        }
    }

    public void setDirty(boolean dirty) {
        if (editor != null) {
            editor.setDirty(dirty);
        }
    }


    private void updateSelfDevStatus(JSONObject status) {
        String phase = (bootstrapController != null && bootstrapController.isRunning()) ? "bootstrapping" : "ready";
        if (status != null) {
            phase = status.optString("phase", phase);
        }

        updateRowStatus(SelfDevRow.SELF_DEV_LOOP, phase.toLowerCase());
    }

    private void updateRowStatus(String name, String status) {
        Object input = selfDevTable.getInput();
        if (input instanceof List) {
            List<SelfDevRow> rows = (List<SelfDevRow>) input;
            for (SelfDevRow row : rows) {
                if (name.equals(row.name)) {
                    if (!status.equals(row.status)) {
                        row.status = status;
                        selfDevTable.refresh(row);
                    }
                    break;
                }
            }
        }
    }

    private void updateSessionStatus() {
        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            eu.kalafatic.evolution.model.orchestration.SelfDevSession session = orchestrator.getSelfDevSession();
            sessionStatusLabel.setText("Session: " + session.getStatus().getName());

            int max = session.getMaxIterations();
            int current = session.getIterations().size();
            double progress = max > 0 ? (double) current / max * 100 : 0;
            sessionProgressLabel.setText(String.format("Progress: %.0f%%", progress));
        }
    }

    @Override
    protected void refreshUI() {
        String currentSid = getCurrentSessionName();
        RuntimeProjection projection = ProjectionService.getInstance().getProjection(currentSid);
        updateRowStatus(SelfDevRow.SELF_DEV_LOOP, projection.getStatus().toLowerCase());

        sessionStatusLabel.setText("Session: " + projection.getStatus());
        sessionProgressLabel.setText(String.format("Progress: %.0f%%", projection.getProgress() * 100));

        syncWorkflowSession();
        if (supervisorGroup != null) supervisorGroup.refreshUI();
        if (archViz != null) archViz.scheduleRefresh();
        if (workflowGroup != null) workflowGroup.scheduleRefresh();
        refreshBrowser();
    }

    private void syncWorkflowSession() {
        if (editor != null && editor.getAiChatPage() != null && workflowGroup != null) {
            String currentChatSessionId = editor.getAiChatPage().getCurrentSessionName();
            if (currentChatSessionId != null && !currentChatSessionId.isEmpty()) {
                workflowGroup.setSessionId(currentChatSessionId);
            }
        }
    }

    public void setupBrowserListeners(Browser browser) {
        browser.addProgressListener(new ProgressAdapter() {
            @Override public void completed(ProgressEvent event) {
                isLoaded = true;
                initRetries = 0;
                refreshBrowser();
            }
        });
        browser.addLocationListener(new LocationAdapter() { @Override public void changing(LocationEvent event) { if (event.location.startsWith("file://") || event.location.equals("about:blank")) { if (!event.location.equals("about:blank")) { event.doit = false; browser.setText(getHtmlTemplate()); } } } });
        browser.setText(getHtmlTemplate());
    }

    private boolean isVizInitializing = false;

    private void refreshBrowser() {
        if (vizGroup == null || vizGroup.getBrowser() == null || vizGroup.getBrowser().isDisposed()) return;

        String json = getModelAsJson();
        if (json.equals(lastJson)) return;

        if (!isLoaded) {
            if (!isVizInitializing) {
                isVizInitializing = true;
                vizGroup.getBrowser().setText(getHtmlTemplate());
            }
            return;
        }

        try {
            Object result = vizGroup.getBrowser().evaluate("return typeof updateGraph !== 'undefined';");
            if (result instanceof Boolean && (Boolean) result) {
                vizGroup.getBrowser().execute("updateGraph(" + json + ");");
                lastJson = json;
                initRetries = 0;
                isVizInitializing = false;
            } else {
                if (initRetries < MAX_INIT_RETRIES) {
                    initRetries++;
                    if (!isVizInitializing) {
                        isVizInitializing = true;
                        vizGroup.getBrowser().setText(getHtmlTemplate());
                    }
                }
            }
        } catch (Exception e) {
            // Browser might not be ready, will retry on next scheduleRefresh
        }
    }

    private String getModelAsJson() {
        if (orchestrator == null) return "{}";
        JSONObject root = new JSONObject();
        JSONArray agentsArr = new JSONArray();
        for (Agent agent : orchestrator.getAgents()) { JSONObject agentObj = new JSONObject(); agentObj.put("id", agent.getId()); agentObj.put("type", agent.getType()); agentsArr.put(agentObj); }
        root.put("agents", agentsArr);
        JSONArray tasksArr = new JSONArray();
        for (Task task : orchestrator.getTasks()) tasksArr.put(serializeTask(task));
        root.put("tasks", tasksArr);
        if (orchestrator.getSelfDevSession() != null) {
            SelfDevSession session = orchestrator.getSelfDevSession(); JSONObject sessionObj = new JSONObject(); sessionObj.put("id", session.getId()); sessionObj.put("status", session.getStatus().toString());
            if (!session.getIterations().isEmpty()) { Iteration last = session.getIterations().get(session.getIterations().size() - 1); sessionObj.put("phase", last.getPhase() != null ? last.getPhase() : "IDLE"); }
            else sessionObj.put("phase", "IDLE");
            root.put("session", sessionObj);
        }

        try {
            if (memoryService != null) {
                List<IterationRecord> records = memoryService.getRecords();
                JSONArray variantsArr = new JSONArray();
                for (IterationRecord rec : records) {
                    JSONObject varObj = new JSONObject();
                    varObj.put("strategy", rec.getStrategy());
                    varObj.put("branch", rec.getBranch());
                    varObj.put("score", rec.getScore());
                    varObj.put("result", rec.getResult());
                    variantsArr.put(varObj);
                }
                root.put("variants", variantsArr);
            }
        } catch (Exception e) {}

        return root.toString();
    }

    private JSONObject serializeTask(Task task) {
        JSONObject obj = new JSONObject(); obj.put("id", task.getId()); obj.put("name", task.getName()); obj.put("status", task.getStatus().toString()); obj.put("rating", task.getRating()); obj.put("likes", task.isLikes());
        JSONArray nextIds = new JSONArray(); for (Task n : task.getNext()) nextIds.put(n.getId());
        obj.put("next", nextIds); return obj;
    }

    private String getHtmlTemplate() {
        return "<!DOCTYPE html><html><head><style>"
                + "html, body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 0; overflow: hidden; width: 100%; height: 100%; }"
                + "#canvas { width: 100%; height: 100%; display: block; }"
                + ".node { fill: #fff; stroke: #cbd5e1; stroke-width: 1px; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }"
                + ".node:hover { stroke: #94a3b8; stroke-width: 2px; transform: translateY(-2px); }"
                + ".task.DONE { fill: #f0fdf4; stroke: #22c55e; }"
                + ".task.RUNNING { fill: #fffbeb; stroke: #f59e0b; }"
                + ".task.FAILED { fill: #fef2f2; stroke: #ef4444; }"
                + ".agent { fill: #eff6ff; stroke: #3b82f6; stroke-width: 2px; }"
                + ".agent-link { stroke: #3b82f6; stroke-width: 1px; stroke-dasharray: 4; }"
                + "text { font-size: 11px; fill: #334155; font-weight: 500; text-anchor: middle; pointer-events: none; }"
                + "line { stroke: #94a3b8; stroke-width: 1.5px; marker-end: url(#arrowhead); }"
                + ".session-info { position: absolute; top: 10px; right: 10px; background: rgba(255,255,255,0.8); padding: 10px; border-radius: 8px; border: 1px solid #e2e8f0; font-size: 12px; }"
                + ".loop-bg { fill: #ffffff; stroke: #cbd5e1; stroke-width: 1px; rx: 12; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.05)); }"
                + ".loop-node { fill: #f1f5f9; stroke: #64748b; stroke-width: 2px; transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1); }"
                + ".loop-node.active { fill: #3b82f6; stroke: #1d4ed8; stroke-width: 3px; }"
                + ".loop-text { font-size: 10px; fill: #334155; font-weight: 800; pointer-events: none; text-anchor: middle; }"
                + ".loop-text.active { fill: #ffffff; }"
                + ".loop-link { stroke: #94a3b8; stroke-width: 2px; fill: none; marker-end: url(#loop-arrow); }"
                + ".loop-link.active { stroke: #3b82f6; stroke-width: 3.5px; }"
                + ".variant-node { fill: #f8fafc; stroke: #94a3b8; stroke-width: 1.5px; }"
                + ".variant-node.SUCCESS { fill: #dcfce7; stroke: #22c55e; }"
                + ".variant-node.FAIL { fill: #fee2e2; stroke: #ef4444; }"
                + ".variant-link { stroke: #94a3b8; stroke-width: 1px; stroke-dasharray: 2; }"
                + "@keyframes pulse { 0% { stroke-opacity: 1; stroke-width: 3px; } 50% { stroke-opacity: 0.4; stroke-width: 8px; } 100% { stroke-opacity: 1; stroke-width: 3px; } }"
                + ".loop-node.active { animation: pulse 2s infinite ease-in-out; }"
                + "</style></head><body>"
                + "<div class='session-info' id='info'>AI Network Structure</div>"
        + "<svg id='canvas' width='100%' height='100%' viewBox='0 0 1000 650' preserveAspectRatio='xMidYMid meet'><defs>"
                + "<marker id='arrowhead' markerWidth='10' markerHeight='7' refX='10' refY='3.5' orient='auto'><polygon points='0 0, 10 3.5, 0 7' fill='#94a3b8'/></marker>"
                + "<marker id='loop-arrow' markerWidth='6' markerHeight='4' refX='6' refY='2' orient='auto'><polygon points='0 0, 6 2, 0 4' fill='#94a3b8'/></marker>"
                + "</defs>"
                + "<rect class='loop-bg' x='10' y='10' width='230' height='230' />"
                + "<g id='loop-diagram' transform='translate(125, 125)'></g>"
                + "<g id='viewport' transform='translate(260, 20)'></g></svg>"
                + "<script>"
                + "var viewport = document.getElementById('viewport');"
                + "var currentZoom = 1.0;"
        + "var translateX = 260, translateY = 20;"
        + "var isDragging = false, startX, startY;"
                + "function applyZoom(factor) {"
                + "  currentZoom *= factor;"
        + "  updateTransform();"
                + "}"
                + "function resetZoom() {"
        + "  currentZoom = 1.0; translateX = 260; translateY = 20;"
        + "  updateTransform();"
        + "}"
        + "function updateTransform() {"
        + "  viewport.setAttribute('transform', 'translate(' + translateX + ', ' + translateY + ') scale(' + currentZoom + ')');"
                + "}"
        + "function fitToScreen() {"
        + "  resetZoom();"
        + "}"
        + "document.getElementById('canvas').addEventListener('mousedown', function(e) {"
        + "  isDragging = true;"
        + "  startX = e.clientX - translateX;"
        + "  startY = e.clientY - translateY;"
        + "});"
        + "window.addEventListener('mousemove', function(e) {"
        + "  if (isDragging) {"
        + "    translateX = e.clientX - startX;"
        + "    translateY = e.clientY - startY;"
        + "    updateTransform();"
        + "  }"
        + "});"
        + "window.addEventListener('mouseup', function() { isDragging = false; });"
                + "window.addEventListener('resize', function() {"
                + "  /* SVG with viewBox and 100% size resizes automatically */"
                + "});"
                + "function updateGraph(data) {"
                + "  viewport.innerHTML = '';"
                + "  if (!data) return;"
                + "  var nodes = {};"
                + "  var links = [];"
                + "  var x = 140, y = 40;"
                + "  if (data.tasks) {"
                + "    data.tasks.forEach(function(t, i) {"
                + "      nodes[t.id] = { id: t.id, name: t.name, status: t.status, next: t.next, x: 140 + (i % 4) * 200, y: 40 + Math.floor(i / 4) * 120 };"
                + "      if (t.next) t.next.forEach(function(nid) { links.push({ from: t.id, to: nid }); });"
                + "    });"
                + "  }"
                + "  var ay = 40;"
                + "  if (data.agents) {"
                + "    data.agents.forEach(function(a) {"
                + "      nodes[a.id] = { id: a.id, name: a.id, type: 'agent', x: 0, y: ay };"
                + "      ay += 100;"
                + "    });"
                + "  }"
                + "  links.forEach(function(l) {"
                + "    var n1 = nodes[l.from], n2 = nodes[l.to];"
                + "    if (n1 && n2) {"
                + "      var line = document.createElementNS('http://www.w3.org/2000/svg', 'line');"
                + "      line.setAttribute('x1', n1.x + 140); line.setAttribute('y1', n1.y + 25);"
                + "      line.setAttribute('x2', n2.x); line.setAttribute('y2', n2.y + 25);"
                + "      viewport.appendChild(line);"
                + "    }"
                + "  });"
                + "  Object.keys(nodes).forEach(function(id) {"
                + "    var n = nodes[id];"
                + "    var g = document.createElementNS('http://www.w3.org/2000/svg', 'g');"
                + "    if (n.type === 'agent') {"
                + "      var circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');"
                + "      circle.setAttribute('cx', n.x + 35); circle.setAttribute('cy', n.y + 35); circle.setAttribute('r', 35);"
                + "      circle.className.baseVal = 'node agent';"
                + "      g.appendChild(circle);"
                + "    } else {"
                + "      var rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');"
                + "      rect.setAttribute('x', n.x); rect.setAttribute('y', n.y); rect.setAttribute('width', 140); rect.setAttribute('height', 50); rect.setAttribute('rx', 8);"
                + "      rect.className.baseVal = 'node task ' + n.status;"
                + "      g.appendChild(rect);"
                + "    }"
                + "    var txt = document.createElementNS('http://www.w3.org/2000/svg', 'text');"
                + "    txt.setAttribute('x', n.type === 'agent' ? n.x + 35 : n.x + 70); txt.setAttribute('y', n.type === 'agent' ? n.y + 40 : n.y + 30);"
                + "    txt.textContent = n.name.length > 18 ? n.name.substring(0, 15) + '...' : n.name;"
                + "    g.appendChild(txt);"
                + "    viewport.appendChild(g);"
                + "  });"
                + "  if (data.session) {"
                + "    document.getElementById('info').textContent = 'Session: ' + data.session.id + ' (' + data.session.status + ')';"
                + "    updateLoopDiagram(data.session.phase, data.variants);"
                + "  } else {"
                + "    updateLoopDiagram('IDLE', data.variants);"
                + "  }"
                + "}"
                + "function updateLoopDiagram(activePhase, variants) {"
                + "  var loopContainer = document.getElementById('loop-diagram');"
                + "  loopContainer.innerHTML = '';"
                + "  var phases = ['OBSERVE', 'ANALYZE', 'PLAN', 'VALIDATE', 'EXECUTE', 'TEST', 'EVALUATE', 'COMMIT', 'PR', 'FEEDBACK', 'REFINE', 'LEARN'];"
                + "  var radius = 75;"
                + "  var centerX = 0, centerY = 0;"
                + "  var planX, planY;"
                + "  phases.forEach(function(p, i) {"
                + "    var angle = (i / phases.length) * 2 * Math.PI - Math.PI / 2;"
                + "    var x = centerX + radius * Math.cos(angle);"
                + "    var y = centerY + radius * Math.sin(angle);"
                + "    if (p === 'PLAN') { planX = x; planY = y; }"
                + "    var isActive = p === activePhase;"
                + "    var g = document.createElementNS('http://www.w3.org/2000/svg', 'g');"
                + "    var circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');"
                + "    circle.setAttribute('cx', x); circle.setAttribute('cy', y); circle.setAttribute('r', 22);"
                + "    circle.className.baseVal = 'loop-node' + (isActive ? ' active' : '');"
                + "    g.appendChild(circle);"
                + "    var text = document.createElementNS('http://www.w3.org/2000/svg', 'text');"
                + "    text.setAttribute('x', x); text.setAttribute('y', y + 4);"
                + "    text.className.baseVal = 'loop-text' + (isActive ? ' active' : '');"
                + "    text.textContent = p.substring(0, 3);"
                + "    g.appendChild(text);"
                + "    loopContainer.appendChild(g);"
                + "    var nextAngle = ((i + 1) / phases.length) * 2 * Math.PI - Math.PI / 2;"
                + "    var x1 = centerX + (radius) * Math.cos(angle + 0.35);"
                + "    var y1 = centerY + (radius) * Math.sin(angle + 0.35);"
                + "    var x2 = centerX + (radius) * Math.cos(nextAngle - 0.35);"
                + "    var y2 = centerY + (radius) * Math.sin(nextAngle - 0.35);"
                + "    var path = document.createElementNS('http://www.w3.org/2000/svg', 'path');"
                + "    path.setAttribute('d', 'M ' + x1 + ' ' + y1 + ' A ' + radius + ' ' + radius + ' 0 0 1 ' + x2 + ' ' + y2);"
                + "    path.className.baseVal = 'loop-link' + (isActive ? ' active' : '');"
                + "    loopContainer.appendChild(path);"
                + "  });"
                + "  if (variants && variants.length > 0) {"
                + "    variants.forEach(function(v, idx) {"
                + "      var vAngle = (idx - (variants.length-1)/2) * 0.4;"
                + "      var vx = planX + 50 * Math.cos(vAngle);"
                + "      var vy = planY + 50 * Math.sin(vAngle);"
                + "      var vLine = document.createElementNS('http://www.w3.org/2000/svg', 'line');"
                + "      vLine.setAttribute('x1', planX); vLine.setAttribute('y1', planY);"
                + "      vLine.setAttribute('x2', vx); vLine.setAttribute('y2', vy);"
                + "      vLine.className.baseVal = 'variant-link';"
                + "      loopContainer.appendChild(vLine);"
                + "      var vCircle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');"
                + "      vCircle.setAttribute('cx', vx); vCircle.setAttribute('cy', vy); vCircle.setAttribute('r', 10);"
                + "      vCircle.className.baseVal = 'variant-node ' + v.result;"
                + "      var title = document.createElementNS('http://www.w3.org/2000/svg', 'title');"
                + "      title.textContent = v.strategy + ' (Score: ' + v.score + ')';"
                + "      vCircle.appendChild(title);"
                + "      loopContainer.appendChild(vCircle);"
                + "    });"
                + "  }"
                + "}"
                + "</script></body></html>";
    }

    @Override
    public void setOrchestrator(Orchestrator orchestrator) {
        super.setOrchestrator(orchestrator);
        initMemoryService();
        if (archViz != null) archViz.setOrchestrator(orchestrator);
        scheduleRefresh();
    }

    @Override
    public void onEvent(RuntimeEvent event) {
        scheduleRefresh();
    }

    @Override
    public void dispose() {
        String sid = (orchestrator != null && orchestrator.getSelfDevSession() != null) ?
            orchestrator.getSelfDevSession().getId() : "Default";
        eu.kalafatic.evolution.controller.orchestration.SessionContainer session = eu.kalafatic.evolution.controller.orchestration.SessionManager.getInstance().getSession(sid);
        if (session != null) {
            session.getEventBus().unsubscribe(this);
        }
        if (pollTimer != null) pollTimer.cancel();
        if (imageRegistry != null) imageRegistry.dispose();
        if (vizGroup != null) vizGroup.dispose();
        if (workflowGroup != null) workflowGroup.dispose();
        super.dispose();
    }
}
