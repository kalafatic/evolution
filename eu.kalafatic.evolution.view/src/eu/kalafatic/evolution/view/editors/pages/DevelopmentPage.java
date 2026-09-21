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
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IFileEditorInput;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.OrchestratorServiceImpl;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBootstrapController;
import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.ResourceManager;
import eu.kalafatic.evolution.controller.tools.EclipseGitEvoTool;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.application.Activator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.development.InteractiveWorkflowGroup;
import eu.kalafatic.evolution.view.editors.pages.development.RowEditDialog;
import eu.kalafatic.evolution.view.editors.pages.development.SupervisorGroup;
import eu.kalafatic.evolution.view.editors.pages.development.VizGroup;
import eu.kalafatic.evolution.view.editors.pages.iteration.SelfDevEditDialog;
import eu.kalafatic.evolution.view.projection.ProjectionService;
import eu.kalafatic.evolution.view.projection.RuntimeProjection;
import eu.kalafatic.utils.constants.FUIConstants;
import eu.kalafatic.utils.factories.GUIFactory;

public class DevelopmentPage extends AEvoPage {

	enum EStatus {
		READY, RUNNING, BLOCKED, ERROR, SUCCESS, NA
	}

	enum EApp {
		EVO, SUPERVISOR
	}

	public static class SelfDevRow {
		public static final String GIT_CHECK = "Git Check";
		public static final String MAVEN_CHECK = "Maven Check";
		public static final String GIT_CHECK_EVO = "Git Check (Evo)";
		public static final String GIT_CHECK_SUPERVISOR = "Git Check (Supervisor)";
		public static final String MAVEN_CHECK_EVO = "Maven Check (Evo)";
		public static final String MAVEN_CHECK_SUPERVISOR = "Maven Check (Supervisor)";
		public static final String SUPERVISOR_CHECK = "Supervisor Check";
		public static final String COPY_SUPERVISOR_SRC = "Copy Supervisor Source";
		public static final String BUILD_SUPERVISOR_LOCAL = "Build Supervisor";
		public static final String LLM_CHECK = "LLM Check";
		public static final String GENOME_CHECK = "Genome Check";
		public static final String PERM_CHECK = "Permissions Check";
		public static final String COPY_SOURCE = "Copy Source";
		public static final String BUILD_PROJECT = "Build Project";
		public static final String BUILD_PROJECT_EVO = "Build Project (Evo)";
		public static final String BUILD_PROJECT_SUPERVISOR = "Build Project (Supervisor)";
		public static final String EXPORT_PRODUCT = "Export Product";
		public static final String EXPORT_PRODUCT_EVO = "Export Product (Evo)";
		public static final String EXPORT_PRODUCT_SUPERVISOR = "Export Product (Supervisor)";
		public static final String START_SUPERVISOR = "Start Supervisor";
		public static final String START_EVO = "Start Evo Product";
		public static final String START_EVO_PRODUCT_SUPERVISOR = "Start Evo Product (Supervisor)";
		public static final String STOP_EVO_PRODUCT_SUPERVISOR = "Stop Evo Product (Supervisor)";
		public static final String SUPERVISOR_LOOP = "Supervisor Engine";
		public static final String SELF_DEV_LOOP = "Self-Dev Loop";

		public int order;
		public boolean selected;
		public String taskId;
		public String name;
		public String command;
		public String path;
		public String url;
		public String status;
		public String executor;

		public SelfDevRow(int order, String taskId, String name, String path, EStatus eStatus) {
			this(order, taskId, name, "", path, EStatus.NA.name(), eStatus, EApp.EVO.name());
		}

		public SelfDevRow(int order, String taskId, String name, String path, String url, EStatus eStatus) {
			this(order, taskId, name, "", path, url, eStatus, EStatus.NA.name());
		}

		public SelfDevRow(int order, String taskId, String name, String path, String url, EStatus eStatus, String executor) {
			this(order, taskId, name, "", path, url, eStatus, executor);
		}

		public SelfDevRow(int order, String taskId, String name, String command, String path, String url, EStatus eStatus, String executor) {
			this.order = order;
			this.taskId = taskId;
			this.name = name;
			this.command = command;
			this.path = path;
			this.url = url;
			this.status = eStatus.toString();
			this.executor = executor;
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
	private Color lightOrangeColor;
	private VizGroup vizGroup;
	private InteractiveWorkflowGroup workflowGroup;
	private SupervisorGroup supervisorGroup;
	private ArchitecturePage archViz;
	private boolean isLoaded = false;
	private String lastJson = "";

	@Override
	protected String getCurrentSessionName() {
		String sid = super.getCurrentSessionName();
		if (sid == null || sid.trim().isEmpty()) {
			return "Default";
		}
		return sid;
	}

	public DevelopmentPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, editor, orchestrator);
		this.setLayout(new GridLayout(1, false));
		this.lightOrangeColor = new Color(Display.getDefault(), 255, 220, 180);
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
		if (desc != null)
			imageRegistry.put(key, desc);
	}

	private void initMemoryService() {
		this.projectRoot = null;
		if (orchestrator != null && orchestrator.eResource() != null) {
			org.eclipse.emf.common.util.URI uri = orchestrator.eResource().getURI();
			if (uri.isPlatformResource()) {
				org.eclipse.core.resources.IResource res = org.eclipse.core.resources.ResourcesPlugin.getWorkspace()
						.getRoot().findMember(uri.toPlatformString(true));
				if (res != null)
					projectRoot = res.getProject().getLocation().toFile();
			} else if (uri.isFile())
				projectRoot = new File(uri.toFileString()).getParentFile();
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

		Composite selfDevComp = GUIFactory.INSTANCE.createExpandableGroup(toolkit, container, "Self-Development", 1,
				false, true);
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
		sdControlPanel.setLayout(new GridLayout(7, false));
		sdControlPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Button runSelectedBtn = GUIFactory.INSTANCE.createButton(sdControlPanel, "▶ Run Selected");
		runSelectedBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				runSelected();
			}
		});

		Button stopSelectedBtn = GUIFactory.INSTANCE.createButton(sdControlPanel, "■ Stop Selected");
		stopSelectedBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				stopSelected();
			}
		});

		Button selectAllBtn = GUIFactory.INSTANCE.createButton(sdControlPanel, "☑ Select All");
		selectAllBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectAll(true);
			}
		});

		Button unselectAllBtn = GUIFactory.INSTANCE.createButton(sdControlPanel, "☐ Unselect All");
		unselectAllBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectAll(false);
			}
		});

		Button debugBtn = GUIFactory.INSTANCE.createButton(sdControlPanel, "🐞 Debug");
		debugBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				runDebug();
			}
		});

		Button scenariosBtn = GUIFactory.INSTANCE.createButton(sdControlPanel, "📋 Task Scenarios");
		scenariosBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showTaskScenariosDialog();
			}
		});

		sdTable.addListener(SWT.MouseDown, event -> {
			org.eclipse.swt.graphics.Point pt = new org.eclipse.swt.graphics.Point(event.x, event.y);
			org.eclipse.swt.widgets.TableItem item = sdTable.getItem(pt);
			if (item != null) {
				for (int i = 0; i < sdTable.getColumnCount(); i++) {
					if (item.getBounds(i).contains(pt))
						handleSelfDevAction((SelfDevRow) item.getData(), i);
				}
			}
		});

		createSelfDevContextMenu();

		supervisorGroup = new SupervisorGroup(toolkit, container, editor, orchestrator);
		vizGroup = new VizGroup(toolkit, container, editor, orchestrator, this);
		String sessionId = (orchestrator != null && orchestrator.getSelfDevSession() != null)
				? orchestrator.getSelfDevSession().getId()
				: "Default";
		workflowGroup = new InteractiveWorkflowGroup(toolkit, container, editor, orchestrator, sessionId);

		Composite archGroup = GUIFactory.INSTANCE.createExpandableGroup(toolkit, container,
				"Architecture Visualization", 1, false, true);
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

	private void loadTableData() {
		List<SelfDevRow> sdData = new ArrayList<>();

		String localPath = (orchestrator != null && orchestrator.getGit() != null)
				? orchestrator.getGit().getLocalPath()
				: "";
		String repoUrl = (orchestrator != null && orchestrator.getGit() != null)
				? orchestrator.getGit().getRepositoryUrl()
				: "";
		if (localPath == null || localPath.isEmpty()) {
			localPath = EclipseGitEvoTool.getRepositoryPath(EclipseGitEvoTool.REPO_EVOLUTION);
		}
		if (repoUrl == null || repoUrl.isEmpty()) {
			repoUrl = EclipseGitEvoTool.getRepositoryRemote(EclipseGitEvoTool.REPO_EVOLUTION);
		}

		String mvnPath = (orchestrator != null && orchestrator.getMaven() != null)
				? orchestrator.getMaven().getGoals().toString()
				: "supervisor.maven";
		String llmModel = (orchestrator != null && orchestrator.getLlm() != null) ? orchestrator.getLlm().getModel()
				: "supervisor.llm";
		String targetPath = getTargetPath();
		String exportPath = getExportPath();

		String customSuperSrc = getSupervisorSourcePath();
		String customSuperBin = getTargetPath();

		int row = 1;
		sdData.add(new SelfDevRow(row++, "LLM", SelfDevRow.LLM_CHECK, "curl -s http://localhost:11434/api/tags", llmModel, EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "GIT_EVO", SelfDevRow.GIT_CHECK_EVO, "git -C " + localPath + " fetch origin && git -C " + localPath + " pull", localPath, repoUrl, EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "MAVEN_EVO", SelfDevRow.MAVEN_CHECK_EVO, "mvn clean verify -DskipTests", mvnPath, EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "COPY_SUPERVISOR", SelfDevRow.COPY_SUPERVISOR_SRC, "cp -r " + customSuperSrc + " " + targetPath, customSuperSrc, EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "BUILD_SUPERVISOR_LOCAL", SelfDevRow.BUILD_SUPERVISOR_LOCAL, "mvn clean install -pl eu.kalafatic.evolution.supervisor -am -DskipTests", customSuperBin, EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "SUPERVISOR", SelfDevRow.SUPERVISOR_CHECK, "java -jar " + customSuperBin + "/supervisor.jar --check", "supervisor.exe", EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "GIT_SUPERVISOR", SelfDevRow.GIT_CHECK_SUPERVISOR, "git -C " + localPath + " fetch origin && git -C " + localPath + " status", localPath, repoUrl, EStatus.READY, EApp.SUPERVISOR.name()));
		sdData.add(new SelfDevRow(row++, "MAVEN_SUPERVISOR", SelfDevRow.MAVEN_CHECK_SUPERVISOR, "mvn -v && mvn validate", mvnPath, EStatus.NA.name(), EStatus.READY, EApp.SUPERVISOR.name()));
		sdData.add(new SelfDevRow(row++, "GENOME", SelfDevRow.GENOME_CHECK, "mvn clean test -pl eu.kalafatic.evolution.selfdev.genome", "supervisor.genome", EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "PERMISSIONS", SelfDevRow.PERM_CHECK, "chmod -R u+rwX " + targetPath, "supervisor.fs", EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		String evoSourcePath = ResourceManager.getInstance().getPath(EvoPath.EVO_GIT_REPOSITORY).toString();
		sdData.add(new SelfDevRow(row++, "COPY", SelfDevRow.COPY_SOURCE, "cp -r " + evoSourcePath + " " + targetPath, evoSourcePath, EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "BUILD_EVO", SelfDevRow.BUILD_PROJECT_EVO, "mvn clean verify -DskipTests -Plinux", targetPath, EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "BUILD_SUPERVISOR", SelfDevRow.BUILD_PROJECT_SUPERVISOR, "mvn clean install -pl eu.kalafatic.evolution.supervisor -DskipTests", targetPath, EStatus.NA.name(), EStatus.READY, EApp.SUPERVISOR.name()));
		sdData.add(new SelfDevRow(row++, "EXPORT_EVO", SelfDevRow.EXPORT_PRODUCT_EVO, "mvn clean package -Pexport-product -DskipTests", exportPath, EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "EXPORT_SUPERVISOR", SelfDevRow.EXPORT_PRODUCT_SUPERVISOR, "mvn clean package -pl eu.kalafatic.evolution.supervisor -DskipTests", exportPath, EStatus.NA.name(), EStatus.READY, EApp.SUPERVISOR.name()));
		sdData.add(new SelfDevRow(row++, "START_SUPERVISOR", SelfDevRow.START_SUPERVISOR, "java -jar supervisor.jar --port 8099", exportPath, EStatus.NA.name(), EStatus.READY, EApp.SUPERVISOR.name()));
		sdData.add(new SelfDevRow(row++, "START_EVO", SelfDevRow.START_EVO, "./evo --port 48091", exportPath, EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "START_EVO_SUPERVISOR", SelfDevRow.START_EVO_PRODUCT_SUPERVISOR, "java -jar supervisor.jar --start-evo --path " + exportPath, exportPath, EStatus.NA.name(), EStatus.READY, EApp.SUPERVISOR.name()));
		sdData.add(new SelfDevRow(row++, "SUPERVISOR_LOOP", SelfDevRow.SUPERVISOR_LOOP, "java -jar supervisor.jar --loop", "supervisor.exe", EStatus.NA.name(), EStatus.READY, EApp.SUPERVISOR.name()));
		sdData.add(new SelfDevRow(row++, "SELF_DEV_LOOP", SelfDevRow.SELF_DEV_LOOP, "java -jar evolution.jar --self-dev-loop", "orchestrator", EStatus.NA.name(), EStatus.READY, EApp.EVO.name()));
		sdData.add(new SelfDevRow(row++, "STOP_EVO_SUPERVISOR", SelfDevRow.STOP_EVO_PRODUCT_SUPERVISOR, "java -jar supervisor.jar --stop-evo", exportPath, EStatus.NA.name(), EStatus.READY, EApp.SUPERVISOR.name()));

		sdData.sort((r1, r2) -> Integer.compare(r1.order, r2.order));
		updateRowOrders(sdData);
		selfDevTable.setInput(sdData);
	}

	private void updateRowOrders(List<SelfDevRow> rows) {
		for (int i = 0; i < rows.size(); i++) {
			rows.get(i).order = i + 1;
		}
	}

	private String getTargetPath() {
		if (bootstrapController != null && bootstrapController.getOrchestrator() != null && bootstrapController.getOrchestrator().getContext() != null) {
			return bootstrapController.getOrchestrator().getContext().getPreparedReactorDirectory().getAbsolutePath();
		}
		return ResourceManager.getInstance().getPath(EvoPath.WORKSPACE).resolve("self-dev/run/source").toString();
	}

	private String getExportPath() {
		if (bootstrapController != null && bootstrapController.getOrchestrator() != null && bootstrapController.getOrchestrator().getContext() != null) {
			return bootstrapController.getOrchestrator().getContext().getExportDirectory().getAbsolutePath();
		}
		return ResourceManager.getInstance().getPath(EvoPath.WORKSPACE).resolve("self-dev/run/export").toString();
	}

	private String getSupervisorSourcePath() {
		return ResourceManager.getInstance().getSupervisorSource().toString();
	}

	private void createSelfDevContextMenu() {
		org.eclipse.swt.widgets.Menu menu = new org.eclipse.swt.widgets.Menu(selfDevTable.getTable());
		selfDevTable.getTable().setMenu(menu);
		org.eclipse.swt.widgets.MenuItem runItem = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
		runItem.setText("▶ Run Action");
		runItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.jface.viewers.IStructuredSelection sel = selfDevTable.getStructuredSelection();
				if (!sel.isEmpty())
					handleActionInternal((SelfDevRow) sel.getFirstElement());
			}
		});
		org.eclipse.swt.widgets.MenuItem editItem = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
		editItem.setText("\u270E Edit Row");
		editItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.jface.viewers.IStructuredSelection sel = selfDevTable.getStructuredSelection();
				if (!sel.isEmpty()) {
					SelfDevRow row = (SelfDevRow) sel.getFirstElement();
					if (SelfDevRow.SELF_DEV_LOOP.equals(row.name))
						openSelfDevEditDialog();
					else
						openRowEditDialog(row);
				}
			}
		});
		org.eclipse.swt.widgets.MenuItem moveUpItem = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
		moveUpItem.setText("\u25B2 Move Up");
		moveUpItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.jface.viewers.IStructuredSelection sel = selfDevTable.getStructuredSelection();
				if (!sel.isEmpty())
					moveRowUp((SelfDevRow) sel.getFirstElement());
			}
		});
		org.eclipse.swt.widgets.MenuItem moveDownItem = new org.eclipse.swt.widgets.MenuItem(menu, SWT.PUSH);
		moveDownItem.setText("\u25BC Move Down");
		moveDownItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				org.eclipse.jface.viewers.IStructuredSelection sel = selfDevTable.getStructuredSelection();
				if (!sel.isEmpty())
					moveRowDown((SelfDevRow) sel.getFirstElement());
			}
		});
	}

	private void moveRowUp(SelfDevRow row) {
		if (row == null || !(selfDevTable.getInput() instanceof List<?> inputList)) return;
		@SuppressWarnings("unchecked")
		List<SelfDevRow> rows = (List<SelfDevRow>) inputList;
		int idx = rows.indexOf(row);
		if (idx > 0) {
			SelfDevRow temp = rows.get(idx);
			rows.set(idx, rows.get(idx - 1));
			rows.set(idx - 1, temp);
			updateRowOrders(rows);
			selfDevTable.refresh();
			selfDevTable.setSelection(new org.eclipse.jface.viewers.StructuredSelection(row));
		}
	}

	private void moveRowDown(SelfDevRow row) {
		if (row == null || !(selfDevTable.getInput() instanceof List<?> inputList)) return;
		@SuppressWarnings("unchecked")
		List<SelfDevRow> rows = (List<SelfDevRow>) inputList;
		int idx = rows.indexOf(row);
		if (idx >= 0 && idx < rows.size() - 1) {
			SelfDevRow temp = rows.get(idx);
			rows.set(idx, rows.get(idx + 1));
			rows.set(idx + 1, temp);
			updateRowOrders(rows);
			selfDevTable.refresh();
			selfDevTable.setSelection(new org.eclipse.jface.viewers.StructuredSelection(row));
		}
	}

	private void createSelfDevColumns() {
		String[] titles = { "#", "Action", "Edit", "Name", "Executed From", "Command", "Path", "URL", "Status" };
		int[] bounds = { 40, 80, 40, 110, 140, 240, 150, 180, 120 };
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

		public SelfDevLabelProvider(int col) {
			this.col = col;
		}

		@Override
		public String getText(Object element) {
			SelfDevRow row = (SelfDevRow) element;
			return switch (col) {
			case 0 -> String.valueOf(row.order);
			case 1 -> ("running".equals(row.status)) ? "\u23F8 \u23F9" : "\u25B6";
			case 2 -> "\u270E";
			case 3 -> row.name;
			case 4 -> row.executor != null ? row.executor : "NA";			
			case 5 -> row.command != null ? row.command : "";
			case 6 -> row.path != null ? row.path : "";
			case 7 -> row.url != null ? row.url : "";
			case 8 -> row.status != null ? row.status : "";
			default -> "";
			};
		}

		@Override
		public Image getImage(Object element) {
			if (col == 1)
				return imageRegistry.get("running".equals(((SelfDevRow) element).status) ? "pause" : "play");
			return null;
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return getBackground(element);
		}

		@Override
		public Color getBackground(Object element) {
			SelfDevRow row = (SelfDevRow) element;
			String status = row.status.toLowerCase();
			if (status.contains("blocked")) {
				return FUIConstants.LIGHT_BLUE;
			}
			if (status.contains("error") || status.contains("fail")) {
				return FUIConstants.LIGHT_RED;
			}
			if (status.contains("checking") || status.contains("running") || status.contains("applying")
					|| status.contains("building") || status.contains("starting")) {
				return lightOrangeColor;
			}
			if (status.contains("checked") || status.contains("success") || status.contains("ok")
					|| status.contains("updated") || status.startsWith("ready:")) {
				return FUIConstants.LIGHT_GREEN;
			}
			if (status.equals(EStatus.READY.name())) {
				return FUIConstants.GRADIENT;
			}
			return null;
		}
	}

	private void handleSelfDevAction(SelfDevRow row, int col) {
		if (col == 1)
			handleActionInternal(row);
		else if (col == 2) {
			if (SelfDevRow.SELF_DEV_LOOP.equals(row.name))
				openSelfDevEditDialog();
			else
				openRowEditDialog(row);
		}
	}

	private void handleActionInternal(SelfDevRow row) {
		if (row == null || row.taskId == null) return;
		System.out.println("[DevelopmentPage] [ACTION_TRIGGERED] Executing task row: " + row.name + ", taskId=" + row.taskId);
		executeBackgroundTask(row);
	}

	private void executeBackgroundTask(SelfDevRow row) {
		if (row == null || row.taskId == null) return;
		System.out.println("[DevelopmentPage] [BACKGROUND_TASK_START] Submitting execution for taskId: " + row.taskId + ", row: " + row.name);
		row.status = "running";
		selfDevTable.refresh(row);
		new Thread(() -> {
			eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult res = bootstrapController.runTask(row.taskId);
			String statusStr = res.isSuccess() ? "SUCCESS" : (res.getStatus() == eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus.BLOCKED ? "BLOCKED: " + res.getMessage() : "FAIL: " + res.getMessage());
			System.out.println("[DevelopmentPage] [BACKGROUND_TASK_END] Task " + row.taskId + " finished with status: " + res.getStatus() + ", message: " + res.getMessage());
			Display.getDefault().asyncExec(() -> {
				if (!selfDevTable.getTable().isDisposed()) {
					row.status = statusStr;
					selfDevTable.refresh(row);
				}
			});
		}).start();
	}

	private void openRowEditDialog(SelfDevRow row) {
		if (new RowEditDialog(getShell(), row).open() == org.eclipse.jface.window.Window.OK) {
			syncRowToModel(row);
			selfDevTable.refresh(row);
			editor.setDirty(true);
		}
	}

	private void syncRowToModel(SelfDevRow row) {
		if (orchestrator == null)
			return;
		switch (row.name) {
		case SelfDevRow.GIT_CHECK_EVO:
		case SelfDevRow.GIT_CHECK_SUPERVISOR:
			if (orchestrator.getGit() == null)
				orchestrator.setGit(OrchestrationFactory.eINSTANCE.createGit());

			orchestrator.getGit().setRepositoryUrl(row.url);
			orchestrator.getGit().setLocalPath(row.path);
			break;
		case SelfDevRow.MAVEN_CHECK_EVO:
		case SelfDevRow.MAVEN_CHECK_SUPERVISOR:
			if (orchestrator.getMaven() == null)
				orchestrator.setMaven(OrchestrationFactory.eINSTANCE.createMaven());
			orchestrator.getMaven().getGoals().clear();
			String[] goals = row.path.replace("[", "").replace("]", "").split(",");
			for (String g : goals)
				if (!g.trim().isEmpty())
					orchestrator.getMaven().getGoals().add(g.trim());
			break;
		case SelfDevRow.LLM_CHECK:
			if (orchestrator.getLlm() == null)
				orchestrator.setLlm(OrchestrationFactory.eINSTANCE.createLLM());
			orchestrator.getLlm().setModel(row.path);
			break;
		case SelfDevRow.COPY_SOURCE:
			if (orchestrator.getSupervisorSettings() == null)
				orchestrator.setSupervisorSettings(OrchestrationFactory.eINSTANCE.createSupervisorSettings());
			orchestrator.getSupervisorSettings().setSourcePath(row.path);
			break;
		case SelfDevRow.BUILD_PROJECT_EVO:
		case SelfDevRow.BUILD_PROJECT_SUPERVISOR:
			if (orchestrator.getSupervisorSettings() == null)
				orchestrator.setSupervisorSettings(OrchestrationFactory.eINSTANCE.createSupervisorSettings());
			orchestrator.getSupervisorSettings().setExecutablePath(row.path);
			break;
		case SelfDevRow.SUPERVISOR_LOOP:
			if (orchestrator.getSupervisorSettings() == null)
				orchestrator.setSupervisorSettings(OrchestrationFactory.eINSTANCE.createSupervisorSettings());
			orchestrator.getSupervisorSettings().setExecutablePath(row.path);
			break;
		}
	}

	private void showTaskScenariosDialog() {
		StringBuilder sb = new StringBuilder();
		sb.append("================================================================================\n");
		sb.append("EVO SELF-DEVELOPMENT PIPELINE TASK SCENARIOS\n");
		sb.append("================================================================================\n\n");

		sb.append("ARCHITECTURE PIPELINE FLOW:\n");
		sb.append("  Remote Git Repository -> Local Git Repository -> Build Workspace -> Supervisor -> EVO RCP\n\n");

		sb.append("CONFIGURED PATHS:\n");
		sb.append("  Local Git Repository (Source) : ")
				.append(ResourceManager.getInstance().getEvoGitRepository()).append("\n");
		sb.append("  Prepared Source (Reactor)     : ")
				.append(getTargetPath()).append("\n");
		sb.append("  Export Directory              : ")
				.append(getExportPath()).append("\n");
		sb.append("  Supervisor Source             : ")
				.append(ResourceManager.getInstance().getSupervisorSource()).append("\n\n");

		sb.append("TASK SCENARIOS (WHAT WILL BE DONE, PATHS FROM / TO):\n");
		sb.append("--------------------------------------------------------------------------------\n");

		if (selfDevTable.getInput() instanceof List<?> rows) {
			for (Object obj : rows) {
				if (obj instanceof SelfDevRow row) {
					sb.append(String.format("#%02d [%s] Executor: %s\n", row.order, row.name, row.executor));
					sb.append("    Command     : ").append(row.command != null ? row.command : "").append("\n");
					sb.append("    Path/Target : ").append(row.path).append("\n");
					sb.append("    Scenario    : ").append(getScenarioDescription(row)).append("\n");
					sb.append("--------------------------------------------------------------------------------\n");
				}
			}
		}

		String reportText = sb.toString();

		org.eclipse.jface.dialogs.Dialog dialog = new org.eclipse.jface.dialogs.Dialog(getShell()) {
			@Override
			protected void configureShell(org.eclipse.swt.widgets.Shell newShell) {
				super.configureShell(newShell);
				newShell.setText("Self-Development Task Scenarios");
				newShell.setSize(750, 550);
			}

			@Override
			protected org.eclipse.swt.widgets.Control createDialogArea(Composite parent) {
				Composite area = (Composite) super.createDialogArea(parent);
				area.setLayout(new GridLayout(1, false));

				org.eclipse.swt.widgets.Text textArea = new org.eclipse.swt.widgets.Text(area,
						SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
				textArea.setLayoutData(new GridData(GridData.FILL_BOTH));
				textArea.setFont(org.eclipse.jface.resource.JFaceResources.getTextFont());
				textArea.setText(reportText);

				return area;
			}

			@Override
			protected void createButtonsForButtonBar(Composite parent) {
				Button copyBtn = createButton(parent, 99, "Copy Content", false);
				copyBtn.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(Display.getDefault());
						cb.setContents(new Object[] { reportText },
								new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() });
						cb.dispose();
						org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Copied",
								"Task scenarios copied to clipboard.");
					}
				});
				createButton(parent, org.eclipse.jface.dialogs.IDialogConstants.OK_ID, "Close", true);
			}
		};
		dialog.open();
	}

	private String getScenarioDescription(SelfDevRow row) {
		return switch (row.name) {
		case SelfDevRow.LLM_CHECK -> "Verifies configured LLM model availability and inference responsiveness.";
		case SelfDevRow.GIT_CHECK_EVO, SelfDevRow.GIT_CHECK_SUPERVISOR ->
			"FROM Remote Git Repository TO Local Git Repository: Fetches/pulls latest commits and verifies repository remote state.";
		case SelfDevRow.MAVEN_CHECK_EVO, SelfDevRow.MAVEN_CHECK_SUPERVISOR ->
			"Verifies Maven executable (mvn/mvnw) and pom.xml build reactor configuration.";
		case SelfDevRow.COPY_SUPERVISOR_SRC ->
			"FROM Local Git Repository TO Workspace Source: Copies supervisor source module into build workspace directory.";
		case SelfDevRow.BUILD_SUPERVISOR_LOCAL ->
			"FROM Workspace Source TO Workspace Build Target: Runs Maven build to compile Supervisor JAR into build workspace.";
		case SelfDevRow.SUPERVISOR_CHECK ->
			"Verifies built Supervisor JAR executable and HTTP health control endpoints.";
		case SelfDevRow.GENOME_CHECK -> "Verifies Self-Dev Genome module source and compilation integrity.";
		case SelfDevRow.PERM_CHECK ->
			"Tests write permissions on workspace build, export, log, and runtime directories.";
		case SelfDevRow.COPY_SOURCE ->
			"FROM Local Git Repository TO Workspace Source: Prepares clean source copy in build workspace without polluting Git checkout.";
		case SelfDevRow.BUILD_PROJECT_EVO, SelfDevRow.BUILD_PROJECT_SUPERVISOR ->
			"FROM Local Git Source Checkout TO Workspace Build Target: Compiles Tycho RCP modules into build workspace (-Dproject.build.directory=...).";
		case SelfDevRow.EXPORT_PRODUCT_EVO, SelfDevRow.EXPORT_PRODUCT_SUPERVISOR ->
			"FROM Workspace Build Target TO Workspace Export Directory: Materializes unzipped RCP product layout (executables, plugins, configuration).";
		case SelfDevRow.START_SUPERVISOR ->
			"FROM Workspace Export Directory TO Active Process: Launches Supervisor process on offset port and verifies endpoint readiness.";
		case SelfDevRow.START_EVO ->
			"FROM Workspace Export Directory TO Active Process: Launches EVO RCP product process on offset port directly.";
		case SelfDevRow.START_EVO_PRODUCT_SUPERVISOR ->
			"FROM Workspace Export Directory TO Active Process: Launches EVO RCP product via Supervisor runner and verifies startup.";
		case SelfDevRow.STOP_EVO_PRODUCT_SUPERVISOR ->
			"Sends graceful stop request to running EVO RCP product via Supervisor.";
		case SelfDevRow.SUPERVISOR_LOOP -> "Starts continuous Supervisor process monitoring loop for EVO RCP runtime.";
		case SelfDevRow.SELF_DEV_LOOP -> "Executes autonomous Self-Development iteration loop in Orchestrator.";
		default -> "Executes task scenario for " + row.name + ".";
		};
	}

	private void openSelfDevEditDialog() {
		if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
			if (new SelfDevEditDialog(getShell(), orchestrator.getSelfDevSession(), this)
					.open() == org.eclipse.jface.window.Window.OK)
				updateSessionStatus();
		}
	}

	private void selectAll(boolean select) {
		if (selfDevTable.getInput() instanceof List<?> rows) {
			for (Object obj : rows) {
				if (obj instanceof SelfDevRow row) {
					row.selected = select;
				}
			}
			for (org.eclipse.swt.widgets.TableItem item : selfDevTable.getTable().getItems()) {
				item.setChecked(select);
			}
		}
	}

	private void resetAllStatuses() {
		if (selfDevTable.getInput() instanceof List<?> rows) {
			for (Object obj : rows) {
				if (obj instanceof SelfDevRow row) {
					row.status = "ready";
				}
			}
			selfDevTable.refresh();
		}
	}

	private void runDebug() {
		System.out.println(
				"[DevelopmentPage] [RUN_DEBUG_START] Resetting statuses and preparing to launch debug thread...");
		resetAllStatuses();
		new Thread(() -> {
			if (bootstrapController != null) {
				System.out.println("[DevelopmentPage] [RUN_DEBUG] Executing Self-Dev Preflight check...");
				eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflightResult preflightRes = bootstrapController
						.executePreflight();
				if (!preflightRes.isSuccess()) {
					System.err.println("[DevelopmentPage] [RUN_DEBUG] Preflight FAILED/BLOCKED:\n"
							+ preflightRes.generateSummaryReport());
					Display.getDefault().asyncExec(() -> {
						org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Self-Dev Preflight Failed",
								"Self-Dev execution cannot proceed safely.\n\n" + preflightRes.generateSummaryReport());
					});
					return;
				}
				System.out
						.println("[DevelopmentPage] [RUN_DEBUG] Preflight PASSED: status=" + preflightRes.getStatus());
			}

			if (!(selfDevTable.getInput() instanceof List<?> rows)) {
				System.err.println(
						"[DevelopmentPage] [RUN_DEBUG_FAIL] Table input is not a valid list of SelfDevRow rows.");
				return;
			}
			List<SelfDevRow> sortedRows = new ArrayList<>();
			for (Object obj : rows) {
				if (obj instanceof SelfDevRow r) {
					sortedRows.add(r);
				}
			}
			sortedRows.sort((r1, r2) -> Integer.compare(r1.order, r2.order));

			System.out.println("[DevelopmentPage] [RUN_DEBUG] Starting sequential phase verification. Number of tasks: "
					+ sortedRows.size());
			if (bootstrapController != null) {
				System.out.println("[DevelopmentPage] [RUN_DEBUG] Setting bootstrapController debugMode = true.");
				bootstrapController.setDebugMode(true);
			}
			for (SelfDevRow row : sortedRows) {
				if (row.taskId == null) continue;
				System.out.println("[DevelopmentPage] [RUN_DEBUG_STEP] Executing phase: " + row.name + " (taskId=" + row.taskId + ")");
				Display.getDefault().syncExec(() -> {
					row.status = "running";
					selfDevTable.refresh(row);
				});
				String result = "";
				boolean failed = false;
				try {
					eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult taskRes = bootstrapController.runTask(row.taskId);
					if (taskRes.isSuccess()) {
						result = "SUCCESS";
					} else if (taskRes.getStatus() == eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus.BLOCKED) {
						result = "BLOCKED: " + taskRes.getMessage();
						failed = true;
					} else {
						result = "FAIL: " + taskRes.getMessage();
						failed = true;
					}
				} catch (Exception e) {
					System.err.println("[DevelopmentPage] [RUN_DEBUG_STEP_ERROR] Exception in execution of phase "
							+ row.name + " (" + row.taskId + "): " + e.getMessage());
					e.printStackTrace();
					result = "ERROR: " + e.getMessage();
					failed = true;
				}
				final String finalResult = result;
				final boolean finalFailed = failed;
				System.out.println("[DevelopmentPage] [RUN_DEBUG_STEP_RESULT] Phase: " + row.name + ", finalFailed: "
						+ finalFailed + ", finalResult: " + finalResult);
				Display.getDefault().syncExec(() -> {
					row.status = finalResult;
					selfDevTable.refresh(row);
				});
				if (finalFailed) {
					final String phaseName = row.name;
					final String finalResultStr = finalResult;
					final boolean[] shouldContinue = { false };
					System.out.println(
							"[DevelopmentPage] [RUN_DEBUG_STEP_PROMPT] Prompting user with MessageDialog for phase: "
									+ phaseName);
					Display.getDefault().syncExec(() -> {
						org.eclipse.jface.dialogs.MessageDialog dialog = new org.eclipse.jface.dialogs.MessageDialog(
								getShell(), "Debug Phase Failed", null,
								"Debug execution phase '" + phaseName + "' failed.\n\n" + "Reason/Detailed Info:\n"
										+ finalResultStr + "\n\n" + "Would you like to continue to the next phase?",
								org.eclipse.jface.dialogs.MessageDialog.ERROR,
								new String[] { "Continue Next", "Stop Debug" }, 0);
						int code = dialog.open();
						System.out.println(
								"[DevelopmentPage] [RUN_DEBUG_STEP_PROMPT_RESPONSE] User selected button index: "
										+ code);
						if (code == 0) {
							shouldContinue[0] = true;
						}
					});
					if (!shouldContinue[0]) {
						System.out.println(
								"[DevelopmentPage] [RUN_DEBUG_STOP] Breaking sequential phase execution loop.");
						break;
					} else {
						System.out.println(
								"[DevelopmentPage] [RUN_DEBUG_CONTINUE] Continuing to next phase as requested by user.");
					}
				}
				try {
					System.out.println("[DevelopmentPage] [RUN_DEBUG_STEP_COMPLETE] Sleeping 500ms before next phase.");
					Thread.sleep(500);
				} catch (InterruptedException e) {
					System.err.println("[DevelopmentPage] [RUN_DEBUG_INTERRUPTED] Debug thread was interrupted.");
					Thread.currentThread().interrupt();
					break;
				}
			}
		}).start();
	}

	private void runSelected() {
		if (!(selfDevTable.getInput() instanceof List<?> rows)) return;
		List<SelfDevRow> selectedRows = new ArrayList<>();
		for (Object obj : rows) {
			if (obj instanceof SelfDevRow row && row.selected) {
				selectedRows.add(row);
			}
		}
		selectedRows.sort((r1, r2) -> Integer.compare(r1.order, r2.order));
		if (selectedRows.isEmpty()) return;

		new Thread(() -> {
			for (SelfDevRow row : selectedRows) {
				if (row.taskId == null) continue;
				Display.getDefault().syncExec(() -> {
					row.status = "running";
					selfDevTable.refresh(row);
				});
				eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult res = bootstrapController.runTask(row.taskId);
				String statusStr = res.isSuccess() ? "SUCCESS" : (res.getStatus() == eu.kalafatic.evolution.controller.orchestration.selfdev.TaskStatus.BLOCKED ? "BLOCKED: " + res.getMessage() : "FAIL: " + res.getMessage());
				Display.getDefault().syncExec(() -> {
					if (!selfDevTable.getTable().isDisposed()) {
						row.status = statusStr;
						selfDevTable.refresh(row);
					}
				});
			}
		}).start();
	}

	private void stopSelected() {
		if (selfDevTable.getInput() instanceof List<?> rows) {
			for (Object obj : rows) {
				if (obj instanceof SelfDevRow row && row.selected) {
					System.out.println("[DevelopmentPage] [STOP_SELECTED] Stopping selected row: " + row.name);
					if (SelfDevRow.SELF_DEV_LOOP.equals(row.name)) {
						RuntimeProjection projection = ProjectionService.getInstance()
								.getProjection(getCurrentSessionName());
						if (projection.isRunning()) {
							System.out.println("[DevelopmentPage] [STOP_SELECTED] Shutting down self-dev session: "
									+ getCurrentSessionName());
							OrchestratorServiceImpl.getInstance().shutdownSession(getCurrentSessionName());
						}
						row.status = "ready";
					} else if (SelfDevRow.SUPERVISOR_LOOP.equals(row.name)) {
						if (bootstrapController.isRunning()) {
							System.out.println("[DevelopmentPage] [STOP_SELECTED] Stopping supervisor bootstrap...");
							bootstrapController.stopBootstrap();
						}
						row.status = "STOPPED";
					} else {
						// Reset background check tasks
						row.status = "ready";
					}
					selfDevTable.refresh(row);
				}
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

	@Override
	protected void refreshUI() {
		String sid = getCurrentSessionName();
		RuntimeProjection projection = ProjectionService.getInstance().getProjection(sid);
		sessionStatusLabel.setText("Session: " + projection.getStatus());
		sessionProgressLabel.setText(String.format("Progress: %.0f%%", projection.getProgress() * 100));
		if (supervisorGroup != null)
			supervisorGroup.refreshUI();
		if (archViz != null)
			archViz.scheduleRefresh();
		if (workflowGroup != null)
			workflowGroup.scheduleRefresh();
		refreshBrowser();

		// Sync table data with model changes
		if (!selfDevTable.getTable().isDisposed() && selfDevTable.getInput() instanceof List<?> rows) {
			String targetPath = getTargetPath();
			for (Object obj : rows) {
				if (obj instanceof SelfDevRow row) {
					if ((SelfDevRow.GIT_CHECK_EVO.equals(row.name) || SelfDevRow.GIT_CHECK_SUPERVISOR.equals(row.name))
							&& orchestrator != null && orchestrator.getGit() != null) {
						String lp = orchestrator.getGit().getLocalPath();
						String url = orchestrator.getGit().getRepositoryUrl();
						if (lp == null || lp.isEmpty()) {
							lp = EclipseGitEvoTool.getRepositoryPath(EclipseGitEvoTool.REPO_EVOLUTION);
						}
						if (url == null || url.isEmpty()) {
							url = EclipseGitEvoTool.getRepositoryRemote(EclipseGitEvoTool.REPO_EVOLUTION);
						}
						row.path = lp;
						row.url = url;
						row.command = "git -C " + lp + " fetch origin && git -C " + lp + " pull";
					} else if ((SelfDevRow.MAVEN_CHECK_EVO.equals(row.name)
							|| SelfDevRow.MAVEN_CHECK_SUPERVISOR.equals(row.name)) && orchestrator != null
							&& orchestrator.getMaven() != null) {
						row.path = orchestrator.getMaven().getGoals().toString();
					} else if (SelfDevRow.LLM_CHECK.equals(row.name) && orchestrator != null
							&& orchestrator.getLlm() != null) {
						row.path = orchestrator.getLlm().getModel();
					} else if (SelfDevRow.COPY_SUPERVISOR_SRC.equals(row.name)) {
						row.path = getSupervisorSourcePath();
						row.command = "cp -r " + row.path + " " + targetPath;
					} else if (SelfDevRow.BUILD_SUPERVISOR_LOCAL.equals(row.name)) {
						row.path = getTargetPath();
					} else if (SelfDevRow.COPY_SOURCE.equals(row.name)) {
						String evoSourcePath = ResourceManager.getInstance().getPath(EvoPath.EVO_GIT_REPOSITORY).toString();
						row.path = evoSourcePath;
						row.command = "cp -r " + evoSourcePath + " " + targetPath;
					} else if (SelfDevRow.BUILD_PROJECT_EVO.equals(row.name)
							|| SelfDevRow.BUILD_PROJECT_SUPERVISOR.equals(row.name)) {
						row.path = targetPath;
					} else if (SelfDevRow.EXPORT_PRODUCT_EVO.equals(row.name)
							|| SelfDevRow.EXPORT_PRODUCT_SUPERVISOR.equals(row.name)
							|| SelfDevRow.START_SUPERVISOR.equals(row.name)
							|| SelfDevRow.START_EVO.equals(row.name)
							|| SelfDevRow.START_EVO_PRODUCT_SUPERVISOR.equals(row.name)
							|| SelfDevRow.STOP_EVO_PRODUCT_SUPERVISOR.equals(row.name)) {
						String exportPath = getExportPath();
						row.path = exportPath;
						if (SelfDevRow.START_EVO_PRODUCT_SUPERVISOR.equals(row.name)) {
							row.command = "java -jar supervisor.jar --start-evo --path " + exportPath;
						} else if (SelfDevRow.START_SUPERVISOR.equals(row.name)) {
							row.command = "java -jar supervisor.jar --port 8099";
						} else if (SelfDevRow.START_EVO.equals(row.name)) {
							row.command = "./evo --port 48091";
						}
					}
				}
			}
			Display.getDefault().asyncExec(() -> {
				if (!selfDevTable.getTable().isDisposed()) {
					selfDevTable.refresh();
				}
			});
		}
	}

	private void refreshBrowser() {
		if (vizGroup == null || vizGroup.getBrowser() == null || vizGroup.getBrowser().isDisposed())
			return;
		String json = getModelAsJson();
		if (json.equals(lastJson))
			return;
		if (!isLoaded) {
			vizGroup.getBrowser().setText(getHtmlTemplate());
			return;
		}
		try {
			if (vizGroup.getBrowser().evaluate("return typeof updateGraph !== 'undefined';") instanceof Boolean b
					&& b) {
				vizGroup.getBrowser().execute("updateGraph(" + json + ");");
				lastJson = json;
			}
		} catch (Exception e) {
		}
	}

	private String getModelAsJson() {
		if (orchestrator == null)
			return "{}";
		JSONObject root = new JSONObject();
		JSONArray agentsArr = new JSONArray();
		for (Agent agent : orchestrator.getAgents()) {
			JSONObject agentObj = new JSONObject();
			agentObj.put("id", agent.getId());
			agentObj.put("type", agent.getType());
			agentsArr.put(agentObj);
		}
		root.put("agents", agentsArr);
		JSONArray tasksArr = new JSONArray();
		for (Task task : orchestrator.getTasks()) {
			JSONObject obj = new JSONObject();
			obj.put("id", task.getId());
			obj.put("name", task.getName());
			obj.put("status", task.getStatus().toString());
			JSONArray nextIds = new JSONArray();
			for (Task n : task.getNext())
				nextIds.put(n.getId());
			obj.put("next", nextIds);
			tasksArr.put(obj);
		}
		root.put("tasks", tasksArr);
		return root.toString();
	}

	public void setupBrowserListeners(Browser browser) {
		browser.addProgressListener(new ProgressAdapter() {
			@Override
			public void completed(ProgressEvent event) {
				isLoaded = true;
				refreshBrowser();
			}
		});
		browser.setText(getHtmlTemplate());
	}

	private String getHtmlTemplate() {
		return """
			<!DOCTYPE html>
			<html>
			<head>
			    <meta charset="UTF-8">
			    <style>
			        :root {
			            --evo-bg: #f2f3f5;
			            --evo-surface: #ffffff;
			            --evo-surface-alt: #f8f9fa;
			            --evo-border: #c8ccd1;
			            --evo-border-dark: #aeb4bb;
			            --evo-text: #202428;
			            --evo-text-secondary: #555b62;
			            --evo-primary: #2864a5;
			            --evo-success: #287a45;
			            --evo-warning: #9a6a00;
			            --evo-danger: #a52a2a;
			            --evo-radius: 4px;
			            --evo-shadow: 0 2px 8px rgba(0,0,0,0.1);
			        }
			        body {
			            margin: 0;
			            padding: 0;
			            overflow: hidden;
			            background-color: var(--evo-bg);
			            color: var(--evo-text);
			            font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, sans-serif;
			            font-size: 12px;
			            height: 100vh;
			            width: 100vw;
			        }
			        #net-svg {
			            width: 100%;
			            height: 100%;
			            display: block;
			        }
			        .net-node {
			            cursor: pointer;
			        }
			        .net-node rect {
			            fill: var(--evo-surface);
			            stroke: var(--evo-border-dark);
			            stroke-width: 1.5px;
			            rx: var(--evo-radius);
			            transition: stroke 0.15s ease, fill 0.15s ease;
			        }
			        .net-node:hover rect {
			            stroke: var(--evo-primary) !important;
			            stroke-width: 2px !important;
			        }
			        .net-node.agent rect {
			            stroke: var(--evo-primary);
			            stroke-width: 2px;
			            fill: #f0f4f9;
			        }
			        .net-node.status-running rect {
			            stroke: var(--evo-primary);
			            stroke-width: 2px;
			            fill: #e8f2fc;
			        }
			        .net-node.status-success rect, .net-node.status-ok rect {
			            stroke: var(--evo-success);
			            fill: #e6f4ea;
			        }
			        .net-node.status-ready rect {
			            stroke: var(--evo-warning);
			            fill: #fef7e0;
			        }
			        .net-node.status-error rect, .net-node.status-failed rect, .net-node.status-blocked rect {
			            stroke: var(--evo-danger);
			            fill: #fce8e8;
			        }
			        .net-node text {
			            font-size: 11px;
			            fill: var(--evo-text);
			            pointer-events: none;
			        }
			        .node-title {
			            font-weight: 600;
			        }
			        .node-icon {
			            font-size: 14px;
			        }
			        .link-path {
			            fill: none;
			            stroke: var(--evo-border-dark);
			            stroke-width: 1.5px;
			            transition: stroke 0.15s ease;
			        }
			        .link-path.agent-link {
			            stroke-dasharray: 4,3;
			            stroke: var(--evo-primary);
			            opacity: 0.6;
			        }
			        .link-path.active {
			            stroke: var(--evo-primary);
			            stroke-width: 2px;
			            stroke-dasharray: 5,5;
			            animation: dash 1s linear infinite;
			        }
			        @keyframes dash {
			            to { stroke-dashoffset: -10; }
			        }
			        #legend-bar {
			            position: absolute;
			            top: 10px;
			            left: 10px;
			            background: var(--evo-surface);
			            border: 1px solid var(--evo-border);
			            padding: 6px 12px;
			            border-radius: var(--evo-radius);
			            box-shadow: var(--evo-shadow);
			            display: flex;
			            gap: 12px;
			            align-items: center;
			            font-size: 11px;
			            z-index: 10;
			        }
			        .badge {
			            display: inline-block;
			            padding: 2px 6px;
			            border-radius: var(--evo-radius);
			            font-size: 10px;
			            font-weight: 700;
			            text-transform: uppercase;
			        }
			        .badge-agent { background: #e8f2fc; color: var(--evo-primary); border: 1px solid var(--evo-primary); }
			        .badge-task { background: var(--evo-surface-alt); color: var(--evo-text-secondary); border: 1px solid var(--evo-border); }
			        .badge-ready { background: #fef7e0; color: var(--evo-warning); }
			        .badge-running { background: #e8f2fc; color: var(--evo-primary); }
			        .badge-success { background: #e6f4ea; color: var(--evo-success); }
			        .badge-error { background: #fce8e8; color: var(--evo-danger); }
			        #tooltip {
			            position: absolute;
			            opacity: 0;
			            pointer-events: none;
			            z-index: 100;
			            background: rgba(15, 23, 42, 0.92);
			            color: white;
			            padding: 8px 12px;
			            border-radius: 6px;
			            font-size: 11px;
			            box-shadow: var(--evo-shadow);
			            max-width: 220px;
			            line-height: 1.4;
			        }
			        #details-panel {
			            position: absolute;
			            top: 10px;
			            right: 10px;
			            width: 260px;
			            background: var(--evo-surface);
			            border: 1px solid var(--evo-border);
			            border-radius: var(--evo-radius);
			            box-shadow: var(--evo-shadow);
			            z-index: 20;
			            display: flex;
			            flex-direction: column;
			            max-height: calc(100% - 20px);
			            overflow-y: auto;
			            transition: transform 0.2s ease, opacity 0.2s ease;
			        }
			        #details-panel.hidden {
			            transform: translateX(300px);
			            opacity: 0;
			            pointer-events: none;
			        }
			        .panel-header {
			            display: flex;
			            justify-content: space-between;
			            align-items: center;
			            padding: 8px 12px;
			            background: var(--evo-surface-alt);
			            border-bottom: 1px solid var(--evo-border);
			            font-weight: 700;
			        }
			        .close-btn {
			            background: none;
			            border: none;
			            font-size: 14px;
			            cursor: pointer;
			            color: var(--evo-text-secondary);
			        }
			        .panel-body {
			            padding: 12px;
			        }
			        .prop-row {
			            display: flex;
			            justify-content: space-between;
			            margin-bottom: 6px;
			            padding-bottom: 4px;
			            border-bottom: 1px dashed var(--evo-border);
			        }
			        .prop-label {
			            color: var(--evo-text-secondary);
			            font-weight: 500;
			        }
			        .prop-val {
			            font-weight: 600;
			            word-break: break-all;
			        }
			    </style>
			</head>
			<body>
			    <div id="legend-bar">
			        <span style="font-weight:700;">AI Structure:</span>
			        <span class="badge badge-agent">🤖 Agent</span>
			        <span class="badge badge-task">📄 Task</span>
			        <span class="badge badge-ready">READY</span>
			        <span class="badge badge-running">RUNNING</span>
			        <span class="badge badge-success">SUCCESS</span>
			        <span class="badge badge-error">ERROR</span>
			    </div>

			    <svg id="net-svg">
			        <g id="main-g">
			            <g id="links-g"></g>
			            <g id="nodes-g"></g>
			        </g>
			    </svg>

			    <div id="tooltip"></div>

			    <div id="details-panel" class="hidden">
			        <div class="panel-header">
			            <span id="panel-title">Node Details</span>
			            <button class="close-btn" onclick="hideDetails()">×</button>
			        </div>
			        <div class="panel-body" id="panel-content"></div>
			    </div>

			    <script>
			        const SVG_NS = "http://www.w3.org/2000/svg";
			        const svg = document.getElementById("net-svg");
			        const mainG = document.getElementById("main-g");
			        const linksG = document.getElementById("links-g");
			        const nodesG = document.getElementById("nodes-g");
			        const tooltip = document.getElementById("tooltip");

			        let transform = { x: 0, y: 0, k: 1 };
			        let isPanning = false;
			        let startPos = { x: 0, y: 0 };
			        let graphData = { agents: [], tasks: [] };

			        function applyTransform() {
			            mainG.setAttribute("transform", `translate(${transform.x}, ${transform.y}) scale(${transform.k})`);
			        }

			        svg.addEventListener("mousedown", (e) => {
			            if (e.target === svg || e.target === mainG || e.target === linksG || e.target === nodesG) {
			                isPanning = true;
			                startPos = { x: e.clientX - transform.x, y: e.clientY - transform.y };
			                svg.style.cursor = "grabbing";
			            }
			        });

			        window.addEventListener("mousemove", (e) => {
			            if (isPanning) {
			                transform.x = e.clientX - startPos.x;
			                transform.y = e.clientY - startPos.y;
			                applyTransform();
			            }
			        });

			        window.addEventListener("mouseup", () => {
			            if (isPanning) {
			                isPanning = false;
			                svg.style.cursor = "default";
			            }
			        });

			        svg.addEventListener("wheel", (e) => {
			            e.preventDefault();
			            const factor = e.deltaY < 0 ? 1.15 : 0.85;
			            const newK = Math.min(Math.max(transform.k * factor, 0.1), 4);

			            const rect = svg.getBoundingClientRect();
			            const mouseX = e.clientX - rect.left;
			            const mouseY = e.clientY - rect.top;

			            transform.x = mouseX - (mouseX - transform.x) * (newK / transform.k);
			            transform.y = mouseY - (mouseY - transform.y) * (newK / transform.k);
			            transform.k = newK;
			            applyTransform();
			        }, { passive: false });

			        window.resetZoom = function() {
			            transform = { x: 0, y: 0, k: 1 };
			            applyTransform();
			        };

			        window.applyZoom = function(factor) {
			            const width = window.innerWidth;
			            const height = window.innerHeight;
			            const center = { x: width / 2, y: height / 2 };
			            const newK = Math.min(Math.max(transform.k * factor, 0.1), 4);
			            transform.x = center.x - (center.x - transform.x) * (newK / transform.k);
			            transform.y = center.y - (center.y - transform.y) * (newK / transform.k);
			            transform.k = newK;
			            applyTransform();
			        };

			        window.fitToScreen = function() {
			            const allNodes = Array.from(nodesG.children);
			            if (allNodes.length === 0) return;

			            let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
			            allNodes.forEach(g => {
			                const t = g.getAttribute("transform");
			                if (t) {
			                    const match = /translate\\(([^,]+),\\s*([^)]+)\\)/.exec(t);
			                    if (match) {
			                        const x = parseFloat(match[1]);
			                        const y = parseFloat(match[2]);
			                        minX = Math.min(minX, x);
			                        minY = Math.min(minY, y);
			                        maxX = Math.max(maxX, x + 160);
			                        maxY = Math.max(maxY, y + 50);
			                    }
			                }
			            });

			            const width = window.innerWidth;
			            const height = window.innerHeight;
			            const gWidth = maxX - minX || 500;
			            const gHeight = maxY - minY || 300;
			            const padding = 50;

			            const scale = Math.min((width - padding * 2) / gWidth, (height - padding * 2) / gHeight, 1.5);
			            transform.k = scale;
			            transform.x = (width - gWidth * scale) / 2 - minX * scale;
			            transform.y = (height - gHeight * scale) / 2 - minY * scale;
			            applyTransform();
			        };

			        window.hideDetails = function() {
			            document.getElementById("details-panel").classList.add("hidden");
			        };

			        function showNodeDetails(node) {
			            const panel = document.getElementById("details-panel");
			            panel.classList.remove("hidden");
			            document.getElementById("panel-title").textContent = (node.name || node.id || "Node").toUpperCase();

			            let html = `
			                <div class="prop-row"><span class="prop-label">ID:</span><span class="prop-val">${node.id || 'N/A'}</span></div>
			                <div class="prop-row"><span class="prop-label">Category:</span><span class="prop-val">${node.isAgent ? 'Agent' : 'Task'}</span></div>
			            `;

			            if (node.type) {
			                html += `<div class="prop-row"><span class="prop-label">Type:</span><span class="prop-val">${node.type}</span></div>`;
			            }
			            if (node.status) {
			                html += `<div class="prop-row"><span class="prop-label">Status:</span><span class="prop-val">${node.status}</span></div>`;
			            }
			            if (node.next && node.next.length > 0) {
			                html += `<div class="prop-row"><span class="prop-label">Next Tasks:</span><span class="prop-val">${node.next.join(', ')}</span></div>`;
			            }

			            document.getElementById("panel-content").innerHTML = html;
			        }

			        window.updateGraph = function(data) {
			            if (!data) return;
			            graphData = data;
			            render();
			        };

			        function render() {
			            linksG.innerHTML = "";
			            nodesG.innerHTML = "";

			            const agents = graphData.agents || [];
			            const tasks = graphData.tasks || [];

			            const nodeMap = new Map();

			            // Position Agents (left column)
			            agents.forEach((ag, i) => {
			                const node = { ...ag, isAgent: true, x: 60, y: 100 + i * 110, width: 150, height: 45 };
			                nodeMap.set("agent_" + ag.id, node);
			            });

			            // Position Tasks (horizontal flow/levels)
			            const taskLevels = new Map();
			            function assignTaskLevel(taskId, lvl) {
			                if (taskLevels.has(taskId) && taskLevels.get(taskId) >= lvl) return;
			                taskLevels.set(taskId, lvl);
			                const t = tasks.find(item => item.id === taskId);
			                if (t && t.next) {
			                    t.next.forEach(nxtId => assignTaskLevel(nxtId, lvl + 1));
			                }
			            }

			            const targetTaskIds = new Set();
			            tasks.forEach(t => (t.next || []).forEach(nid => targetTaskIds.add(nid)));
			            const rootTasks = tasks.filter(t => !targetTaskIds.has(t.id));
			            if (rootTasks.length === 0 && tasks.length > 0) rootTasks.push(tasks[0]);

			            rootTasks.forEach(r => assignTaskLevel(r.id, 0));

			            const levelGroups = new Map();
			            tasks.forEach(t => {
			                const lvl = taskLevels.get(t.id) || 0;
			                if (!levelGroups.has(lvl)) levelGroups.set(lvl, []);
			                levelGroups.get(lvl).push(t);
			            });

			            levelGroups.forEach((group, lvl) => {
			                const startY = 100;
			                group.forEach((t, i) => {
			                    const node = { ...t, isAgent: false, x: 280 + lvl * 210, y: startY + i * 90, width: 150, height: 45 };
			                    nodeMap.set("task_" + t.id, node);
			                });
			            });

			            // Draw Agent-to-Task dashed connections
			            agents.forEach(ag => {
			                const source = nodeMap.get("agent_" + ag.id);
			                if (!source) return;
			                tasks.forEach(t => {
			                    const target = nodeMap.get("task_" + t.id);
			                    if (!target) return;

			                    const path = document.createElementNS(SVG_NS, "path");
			                    path.setAttribute("class", "link-path agent-link");
			                    const x0 = source.x + source.width;
			                    const y0 = source.y + source.height / 2;
			                    const x1 = target.x;
			                    const y1 = target.y + target.height / 2;
			                    const mx = (x0 + x1) / 2;
			                    path.setAttribute("d", `M${x0},${y0} C${mx},${y0} ${mx},${y1} ${x1},${y1}`);
			                    linksG.appendChild(path);
			                });
			            });

			            // Draw Task-to-Next Task connections
			            tasks.forEach(t => {
			                const source = nodeMap.get("task_" + t.id);
			                if (!source || !t.next) return;
			                t.next.forEach(nxtId => {
			                    const target = nodeMap.get("task_" + nxtId);
			                    if (!target) return;

			                    const path = document.createElementNS(SVG_NS, "path");
			                    const isActive = (t.status === 'RUNNING' || t.status === 'EXECUTING');
			                    path.setAttribute("class", "link-path " + (isActive ? "active" : ""));
			                    const x0 = source.x + source.width;
			                    const y0 = source.y + source.height / 2;
			                    const x1 = target.x;
			                    const y1 = target.y + target.height / 2;
			                    const mx = (x0 + x1) / 2;
			                    path.setAttribute("d", `M${x0},${y0} C${mx},${y0} ${mx},${y1} ${x1},${y1}`);
			                    linksG.appendChild(path);
			                });
			            });

			            // Render Node Groups
			            nodeMap.forEach(node => {
			                const g = document.createElementNS(SVG_NS, "g");
			                let cls = "net-node " + (node.isAgent ? "agent" : "");
			                if (node.status) {
			                    cls += " status-" + node.status.toLowerCase();
			                }
			                g.setAttribute("class", cls);
			                g.setAttribute("transform", `translate(${node.x}, ${node.y})`);

			                const rect = document.createElementNS(SVG_NS, "rect");
			                rect.setAttribute("width", node.width);
			                rect.setAttribute("height", node.height);
			                g.appendChild(rect);

			                const iconText = document.createElementNS(SVG_NS, "text");
			                iconText.setAttribute("class", "node-icon");
			                iconText.setAttribute("x", "12");
			                iconText.setAttribute("y", "28");
			                iconText.textContent = node.isAgent ? '🤖' : '📄';
			                g.appendChild(iconText);

			                const titleText = document.createElementNS(SVG_NS, "text");
			                titleText.setAttribute("class", "node-title");
			                titleText.setAttribute("x", "38");
			                titleText.setAttribute("y", "20");
			                const displayName = node.name || node.id || 'Node';
			                titleText.textContent = displayName.length > 12 ? displayName.substring(0, 10) + '..' : displayName;
			                g.appendChild(titleText);

			                if (node.status) {
			                    const statusText = document.createElementNS(SVG_NS, "text");
			                    statusText.setAttribute("x", "38");
			                    statusText.setAttribute("y", "35");
			                    statusText.setAttribute("fill", "#64748b");
			                    statusText.setAttribute("font-size", "10");
			                    statusText.textContent = node.status;
			                    g.appendChild(statusText);
			                }

			                g.addEventListener("mouseover", (e) => {
			                    tooltip.style.opacity = "1";
			                    tooltip.innerHTML = `
			                        <strong>${node.isAgent ? 'Agent' : 'Task'}:</strong> ${node.name || node.id}<br/>
			                        ${node.type ? '<strong>Type:</strong> ' + node.type + '<br/>' : ''}
			                        ${node.status ? '<strong>Status:</strong> ' + node.status : ''}
			                    `;
			                    tooltip.style.left = (e.pageX + 12) + "px";
			                    tooltip.style.top = (e.pageY - 12) + "px";
			                });

			                g.addEventListener("mousemove", (e) => {
			                    tooltip.style.left = (e.pageX + 12) + "px";
			                    tooltip.style.top = (e.pageY - 12) + "px";
			                });

			                g.addEventListener("mouseout", () => {
			                    tooltip.style.opacity = "0";
			                });

			                g.addEventListener("click", (e) => {
			                    e.stopPropagation();
			                    showNodeDetails(node);
			                });

			                nodesG.appendChild(g);
			            });
			        }
			    </script>
			</body>
			</html>
			""";
	}

	@Override
	public void setOrchestrator(Orchestrator o) {
		super.setOrchestrator(o);
		initMemoryService();
		if (archViz != null)
			archViz.setOrchestrator(o);
		scheduleRefresh();
	}

	@Override
	public void dispose() {
		if (imageRegistry != null)
			imageRegistry.dispose();
		if (vizGroup != null)
			vizGroup.dispose();
		if (workflowGroup != null)
			workflowGroup.dispose();
		if (lightOrangeColor != null)
			lightOrangeColor.dispose();
		super.dispose();
	}
}
