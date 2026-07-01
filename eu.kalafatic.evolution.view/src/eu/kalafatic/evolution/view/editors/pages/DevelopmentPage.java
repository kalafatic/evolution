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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
import eu.kalafatic.evolution.view.editors.pages.development.RowEditDialog;
import eu.kalafatic.evolution.view.editors.pages.development.SupervisorGroup;
import eu.kalafatic.evolution.view.editors.pages.development.VizGroup;
import eu.kalafatic.evolution.view.editors.pages.iteration.SelfDevEditDialog;
import eu.kalafatic.utils.factories.GUIFactory;

public class DevelopmentPage extends AEvoPage {

    public static class SelfDevRow {
        public static final String GIT_CHECK = "Git Check";
        public static final String MAVEN_CHECK = "Maven Check";
        public static final String LLM_CHECK = "LLM Check";
        public static final String GENOME_CHECK = "Genome Check";
        public static final String PERM_CHECK = "Permissions Check";
        public static final String COPY_SOURCE = "Copy Source";
        public static final String BUILD_PROJECT = "Build Project";
        public static final String EXPORT_PRODUCT = "Export Product";
        public static final String SUPERVISOR_LOOP = "Supervisor Engine";
        public static final String SELF_DEV_LOOP = "Self-Dev Loop";

        public int order;
        public boolean selected;
        public String name;
        public String path;
        public String status;

        public SelfDevRow(int order, String name, String path, String status) {
            this.order = order;
            this.name = name;
            this.path = path;
            this.status = status;
            this.selected = false;
        }
    }

    private IterationMemoryService memoryService;
    private SelfDevBootstrapController bootstrapController;
    private File projectRoot;
    private Label sessionStatusLabel;
    private Label sessionProgressLabel;
    private TableViewer selfDevTable;
    private ImageRegistry imageRegistry;
    private VizGroup vizGroup;
    private InteractiveWorkflowGroup workflowGroup;
    private SupervisorGroup supervisorGroup;
    private ArchitecturePage archViz;
    private boolean isLoaded = false;
    private String lastJson = "";

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
    }

    private void registerImage(String key, String pluginId, String path) {
        ImageDescriptor desc = Activator.getImageDescriptor(pluginId, path);
        if (desc != null) imageRegistry.put(key, desc);
    }

    private void initMemoryService() {
        this.projectRoot = null;
        if (orchestrator != null && orchestrator.eResource() != null) {
            org.eclipse.emf.common.util.URI uri = orchestrator.eResource().getURI();
            if (uri.isPlatformResource()) {
                org.eclipse.core.resources.IResource res = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().findMember(uri.toPlatformString(true));
                if (res != null) projectRoot = res.getProject().getLocation().toFile();
            } else if (uri.isFile()) projectRoot = new File(uri.toFileString()).getParentFile();
        }
        if (projectRoot == null && editor.getEditorInput() instanceof IFileEditorInput fei) {
            projectRoot = fei.getFile().getProject().getLocation().toFile();
        }
        if (projectRoot != null) {
            this.memoryService = new IterationMemoryService(projectRoot);
            this.bootstrapController = new SelfDevBootstrapController(projectRoot, orchestrator);
        }
    }

    private void createControl() {
        Composite container = toolkit.createComposite(this);
        container.setLayout(new GridLayout(1, false));

        Composite selfDevComp = GUIFactory.INSTANCE.createExpandableGroup(toolkit, container, "Self-Development", 1, true, true);
        selfDevComp.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite sdStatusComp = toolkit.createComposite(selfDevComp);
        sdStatusComp.setLayout(new GridLayout(2, false));
        sdStatusComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        sessionStatusLabel = toolkit.createLabel(sdStatusComp, "Session: READY");
        sessionStatusLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
        sessionProgressLabel = toolkit.createLabel(sdStatusComp, "Progress: 0%");

        selfDevTable = new TableViewer(selfDevComp, SWT.BORDER | SWT.FULL_SELECTION | SWT.CHECK);
        Table sdTable = selfDevTable.getTable();
        sdTable.setHeaderVisible(true);
        sdTable.setLinesVisible(true);
        GridData gdSdTable = new GridData(GridData.FILL_BOTH);
        gdSdTable.heightHint = 180;
        gdSdTable.grabExcessVerticalSpace = true;
        sdTable.setLayoutData(gdSdTable);

        createSelfDevColumns();
        selfDevTable.setContentProvider(ArrayContentProvider.getInstance());
        selfDevTable.getTable().addListener(SWT.Selection, event -> {
            if (event.detail == SWT.CHECK) {
                SelfDevRow row = (SelfDevRow) event.item.getData();
                row.selected = ((org.eclipse.swt.widgets.TableItem) event.item).getChecked();
            }
        });

        loadTableData();

        Composite sdControlPanel = toolkit.createComposite(selfDevComp);
        sdControlPanel.setLayout(new GridLayout(3, false));
        sdControlPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        Button runSelectedBtn = GUIFactory.INSTANCE.createButton(sdControlPanel, "▶ Run Selected");
        runSelectedBtn.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) { runSelected(); }
        });

        sdTable.addListener(SWT.MouseDown, event -> {
            org.eclipse.swt.graphics.Point pt = new org.eclipse.swt.graphics.Point(event.x, event.y);
            org.eclipse.swt.widgets.TableItem item = sdTable.getItem(pt);
            if (item != null) {
                for (int i = 0; i < sdTable.getColumnCount(); i++) {
                    if (item.getBounds(i).contains(pt)) handleSelfDevAction((SelfDevRow) item.getData(), i);
                }
            }
        });

        createSelfDevContextMenu();

        supervisorGroup = new SupervisorGroup(toolkit, container, editor, orchestrator);
        vizGroup = new VizGroup(toolkit, container, editor, orchestrator, this);
        String sessionId = (orchestrator != null && orchestrator.getSelfDevSession() != null) ? orchestrator.getSelfDevSession().getId() : "Default";
        workflowGroup = new InteractiveWorkflowGroup(toolkit, container, editor, orchestrator, sessionId);

        Composite archGroup = GUIFactory.INSTANCE.createExpandableGroup(toolkit, container, "Architecture Visualization", 1, false, true);
        archGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite archComp = toolkit.createComposite(archGroup);
        archComp.setLayout(new org.eclipse.swt.layout.FillLayout());
        GridData archGd = new GridData(GridData.FILL_BOTH);
        archGd.minimumHeight = 800;
        archComp.setLayoutData(archGd);
        archViz = new ArchitecturePage(archComp, editor, orchestrator);

        this.setContent(container);
        Display.getCurrent().asyncExec(() -> { if (!isDisposed()) { refreshBrowser(); container.layout(true, true); } });
    }

    private void loadTableData() {
        List<SelfDevRow> sdData = new ArrayList<>();
        String gitUrl = (orchestrator != null && orchestrator.getGit() != null) ? orchestrator.getGit().getRepositoryUrl() : "supervisor.git";
        String mvnPath = (orchestrator != null && orchestrator.getMaven() != null) ? orchestrator.getMaven().toString() : "supervisor.maven";
        
        sdData.add(new SelfDevRow(1, SelfDevRow.GIT_CHECK, gitUrl, "ready"));
        sdData.add(new SelfDevRow(2, SelfDevRow.MAVEN_CHECK, mvnPath, "ready"));
        sdData.add(new SelfDevRow(3, SelfDevRow.LLM_CHECK, "supervisor.llm", "ready"));
        sdData.add(new SelfDevRow(4, SelfDevRow.GENOME_CHECK, "supervisor.genome", "ready"));
        sdData.add(new SelfDevRow(5, SelfDevRow.PERM_CHECK, "supervisor.fs", "ready"));
        sdData.add(new SelfDevRow(6, SelfDevRow.COPY_SOURCE, "sandbox.copy", "ready"));
        sdData.add(new SelfDevRow(7, SelfDevRow.BUILD_PROJECT, "sandbox.build", "ready"));
        sdData.add(new SelfDevRow(8, SelfDevRow.EXPORT_PRODUCT, "sandbox.export", "ready"));
        sdData.add(new SelfDevRow(9, SelfDevRow.SUPERVISOR_LOOP, "supervisor.exe", "ready"));
        sdData.add(new SelfDevRow(10, SelfDevRow.SELF_DEV_LOOP, "orchestrator", "ready"));
        selfDevTable.setInput(sdData);
    }

    private void createSelfDevContextMenu() {
        org.eclipse.swt.widgets.Menu menu = new org.eclipse.swt.widgets.Menu(selfDevTable.getTable());
        selfDevTable.getTable().setMenu(menu);
        org.eclipse.swt.widgets.MenuItem runItem = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
        runItem.setText("▶ Run Action");
        runItem.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                org.eclipse.jface.viewers.IStructuredSelection sel = selfDevTable.getStructuredSelection();
                if (!sel.isEmpty()) handleActionInternal((SelfDevRow) sel.getFirstElement());
            }
        });
        org.eclipse.swt.widgets.MenuItem editItem = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
        editItem.setText("\u270E Edit Row");
        editItem.addSelectionListener(new SelectionAdapter() {
            @Override public void widgetSelected(SelectionEvent e) {
                org.eclipse.jface.viewers.IStructuredSelection sel = selfDevTable.getStructuredSelection();
                if (!sel.isEmpty()) {
                    SelfDevRow row = (SelfDevRow) sel.getFirstElement();
                    if (SelfDevRow.SELF_DEV_LOOP.equals(row.name)) openSelfDevEditDialog();
                    else openRowEditDialog(row);
                }
            }
        });
    }

    private void createSelfDevColumns() {
        String[] titles = { "#", "Action", "Edit", "Name", "Path/URL", "Status" };
        int[] bounds = { 40, 100, 50, 150, 250, 150 };
        for (int i = 0; i < titles.length; i++) {
            TableViewerColumn col = new TableViewerColumn(selfDevTable, SWT.NONE);
            col.getColumn().setText(titles[i]);
            col.getColumn().setWidth(bounds[i]);
            col.getColumn().setResizable(true);
            col.setLabelProvider(new SelfDevLabelProvider(i));
        }
    }

    private class SelfDevLabelProvider extends ColumnLabelProvider implements ITableColorProvider {
        private final int col;
        public SelfDevLabelProvider(int col) { this.col = col; }
        @Override public String getText(Object element) {
            SelfDevRow row = (SelfDevRow) element;
            return switch (col) {
                case 0 -> String.valueOf(row.order);
                case 1 -> ("running".equals(row.status)) ? "\u23F8 \u23F9" : "\u25B6";
                case 2 -> "\u270E";
                case 3 -> row.name;
                case 4 -> row.path;
                case 5 -> row.status;
                default -> "";
            };
        }
        @Override public Image getImage(Object element) {
            if (col == 1) return imageRegistry.get("running".equals(((SelfDevRow)element).status) ? "pause" : "play");
            return null;
        }
        @Override public Color getForeground(Object element, int columnIndex) { return null; }
        @Override public Color getBackground(Object element, int columnIndex) {
            SelfDevRow row = (SelfDevRow) element;
            String status = row.status.toLowerCase();
            if (status.contains("error") || status.contains("fail")) return FUIConstants.LIGHT_RED;
            if (status.contains("checked") || status.equals("success") || status.equals("running")) return FUIConstants.LIGHT_GREEN;
            if (status.equals("ready")) return FUIConstants.GRADIENT;
            return null;
        }
    }

    private void handleSelfDevAction(SelfDevRow row, int col) {
        if (col == 1) handleActionInternal(row);
        else if (col == 2) {
            if (SelfDevRow.SELF_DEV_LOOP.equals(row.name)) openSelfDevEditDialog();
            else openRowEditDialog(row);
        }
    }

    private void handleActionInternal(SelfDevRow row) {
        if (SelfDevRow.SELF_DEV_LOOP.equals(row.name)) {
            RuntimeProjection projection = ProjectionService.getInstance().getProjection(getCurrentSessionName());
            if (projection.isRunning()) OrchestratorServiceImpl.getInstance().shutdownSession(getCurrentSessionName());
            else {
                TaskRequest req = new TaskRequest("Start Self-Dev Bootstrap", projectRoot);
                req.getContext().put("orchestrator", orchestrator);
                req.getContext().put("sessionId", getCurrentSessionName());
                OrchestratorServiceImpl.getInstance().submit(getCurrentSessionName(), req);
            }
        } else if (SelfDevRow.SUPERVISOR_LOOP.equals(row.name)) {
            if (bootstrapController.isRunning()) { bootstrapController.stopBootstrap(); row.status = "STOPPED"; }
            else { try { bootstrapController.startBootstrap(); row.status = "RUNNING"; } catch (Exception e) { row.status = "ERROR"; } }
            selfDevTable.refresh(row);
        } else if (SelfDevRow.COPY_SOURCE.equals(row.name)) executeBackgroundTask(row, "COPY");
        else if (SelfDevRow.BUILD_PROJECT.equals(row.name)) executeBackgroundTask(row, "BUILD");
        else {
            String type = switch(row.name) {
                case SelfDevRow.GIT_CHECK -> "GIT";
                case SelfDevRow.MAVEN_CHECK -> "MAVEN";
                case SelfDevRow.LLM_CHECK -> "LLM";
                case SelfDevRow.GENOME_CHECK -> "GENOME";
                case SelfDevRow.PERM_CHECK -> "PERMISSIONS";
                case SelfDevRow.EXPORT_PRODUCT -> "EXPORT";
                default -> null;
            };
            if (type != null) { row.status = bootstrapController.check(type); selfDevTable.refresh(row); }
        }
    }

    private void executeBackgroundTask(SelfDevRow row, String type) {
        row.status = "running"; selfDevTable.refresh(row);
        new Thread(() -> {
            String res = bootstrapController.check(type);
            Display.getDefault().asyncExec(() -> { if (!selfDevTable.getTable().isDisposed()) { row.status = res; selfDevTable.refresh(row); } });
        }).start();
    }

    private void openRowEditDialog(SelfDevRow row) {
        if (new RowEditDialog(getShell(), row).open() == org.eclipse.jface.window.Window.OK) { selfDevTable.refresh(row); editor.setDirty(true); }
    }

    private void openSelfDevEditDialog() {
        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            if (new SelfDevEditDialog(getShell(), orchestrator.getSelfDevSession(), this).open() == org.eclipse.jface.window.Window.OK) updateSessionStatus();
        }
    }

    private void runSelected() {
        if (selfDevTable.getInput() instanceof List<?> rows) {
            for (Object obj : rows) {
                if (obj instanceof SelfDevRow row && row.selected) handleActionInternal(row);
            }
        }
    }

    private void updateSessionStatus() {
        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            SelfDevSession session = orchestrator.getSelfDevSession();
            sessionStatusLabel.setText("Session: " + session.getStatus().getName());
            int max = session.getMaxIterations();
            int current = session.getIterations().size();
            sessionProgressLabel.setText(String.format("Progress: %.0f%%", max > 0 ? (double) current / max * 100 : 0));
        }
    }

    @Override protected void refreshUI() {
        String sid = getCurrentSessionName();
        RuntimeProjection projection = ProjectionService.getInstance().getProjection(sid);
        sessionStatusLabel.setText("Session: " + projection.getStatus());
        sessionProgressLabel.setText(String.format("Progress: %.0f%%", projection.getProgress() * 100));
        if (supervisorGroup != null) supervisorGroup.refreshUI();
        if (archViz != null) archViz.scheduleRefresh();
        if (workflowGroup != null) workflowGroup.scheduleRefresh();
        refreshBrowser();
        // Sync table data with model changes if needed
        Display.getDefault().asyncExec(() -> { if (!selfDevTable.getTable().isDisposed()) { selfDevTable.refresh(); } });
    }

    private void refreshBrowser() {
        if (vizGroup == null || vizGroup.getBrowser() == null || vizGroup.getBrowser().isDisposed()) return;
        String json = getModelAsJson();
        if (json.equals(lastJson)) return;
        if (!isLoaded) { vizGroup.getBrowser().setText(getHtmlTemplate()); return; }
        try {
            if (vizGroup.getBrowser().evaluate("return typeof updateGraph !== 'undefined';") instanceof Boolean b && b) {
                vizGroup.getBrowser().execute("updateGraph(" + json + ");"); lastJson = json;
            }
        } catch (Exception e) {}
    }

    private String getModelAsJson() {
        if (orchestrator == null) return "{}";
        JSONObject root = new JSONObject();
        JSONArray agentsArr = new JSONArray();
        for (Agent agent : orchestrator.getAgents()) { JSONObject agentObj = new JSONObject(); agentObj.put("id", agent.getId()); agentObj.put("type", agent.getType()); agentsArr.put(agentObj); }
        root.put("agents", agentsArr);
        JSONArray tasksArr = new JSONArray();
        for (Task task : orchestrator.getTasks()) {
            JSONObject obj = new JSONObject(); obj.put("id", task.getId()); obj.put("name", task.getName()); obj.put("status", task.getStatus().toString());
            JSONArray nextIds = new JSONArray(); for (Task n : task.getNext()) nextIds.put(n.getId());
            obj.put("next", nextIds); tasksArr.put(obj);
        }
        root.put("tasks", tasksArr);
        return root.toString();
    }

    public void setupBrowserListeners(Browser browser) {
        browser.addProgressListener(new ProgressAdapter() { @Override public void completed(ProgressEvent event) { isLoaded = true; refreshBrowser(); } });
        browser.setText(getHtmlTemplate());
    }

    private String getHtmlTemplate() { return "<html><body><div id='info'>AI Network Structure</div><script>function updateGraph(data) {}</script></body></html>"; }
    @Override public void setOrchestrator(Orchestrator o) { super.setOrchestrator(o); initMemoryService(); if (archViz != null) archViz.setOrchestrator(o); scheduleRefresh(); }
    @Override public void dispose() { if (imageRegistry != null) imageRegistry.dispose(); if (vizGroup != null) vizGroup.dispose(); if (workflowGroup != null) workflowGroup.dispose(); super.dispose(); }
}
