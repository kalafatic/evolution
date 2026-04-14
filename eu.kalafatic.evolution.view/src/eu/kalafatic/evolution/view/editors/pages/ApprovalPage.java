package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import java.io.File;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.approval.*;

public class ApprovalPage extends SharedScrolledComposite {
	private MultiPageEditor editor;
	private Orchestrator orchestrator;
	private boolean isLoaded = false;
	private int initRetries = 0;
	private static final int MAX_INIT_RETRIES = 5;
	private FormToolkit toolkit;
	private String lastJson = "";

	private SummaryGroup summaryGroup;
	private ReviewGroup reviewGroup;
	private FeedbackGroup feedbackGroup;
	private ProposedTasksGroup proposedTasksGroup;
	private VizGroup vizGroup;
	private ActionsGroup actionsGroup;

	private Adapter modelAdapter = new EContentAdapter() {
		@Override public void notifyChanged(Notification notification) {
			super.notifyChanged(notification);
			refreshUI();
		}
	};

	public ApprovalPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		this.editor = editor;
		this.orchestrator = orchestrator;
		this.setExpandHorizontal(true);
		this.setExpandVertical(true);
		this.toolkit = new FormToolkit(parent.getDisplay());
		createControl();
		setOrchestrator(orchestrator);
	}

	private void createControl() {
		Composite comp = toolkit.createComposite(this);
		comp.setLayout(new GridLayout(1, false));

		summaryGroup = new SummaryGroup(toolkit, comp, editor, orchestrator);
		reviewGroup = new ReviewGroup(toolkit, comp, editor, orchestrator);
		feedbackGroup = new FeedbackGroup(toolkit, comp, editor, orchestrator);
		proposedTasksGroup = new ProposedTasksGroup(toolkit, comp, editor, orchestrator, this);
		vizGroup = new VizGroup(toolkit, comp, editor, orchestrator, this);
		actionsGroup = new ActionsGroup(toolkit, comp, editor, orchestrator, this);

		this.setContent(comp);
		this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	public void setOrchestrator(Orchestrator orchestrator) {
		if (this.orchestrator != null) this.orchestrator.eAdapters().remove(modelAdapter);
		this.orchestrator = orchestrator;
		if (this.orchestrator != null) this.orchestrator.eAdapters().add(modelAdapter);
		refreshUI();
	}

	public void createColumns(TableViewer tableViewer) {
		TableViewerColumn colName = new TableViewerColumn(tableViewer, SWT.NONE);
		colName.getColumn().setText("Task Name (Click to edit)"); colName.getColumn().setWidth(300);
		colName.setLabelProvider(new ColumnLabelProvider() { @Override public String getText(Object element) { return ((Task)element).getName(); } });
		colName.setEditingSupport(new org.eclipse.jface.viewers.EditingSupport(tableViewer) {
			@Override protected void setValue(Object element, Object value) { ((Task)element).setName(String.valueOf(value)); editor.setDirty(true); tableViewer.update(element, null); }
			@Override protected Object getValue(Object element) { return ((Task)element).getName(); }
			@Override protected org.eclipse.jface.viewers.CellEditor getCellEditor(Object element) { return new org.eclipse.jface.viewers.TextCellEditor(tableViewer.getTable()); }
			@Override protected boolean canEdit(Object element) { return true; }
		});
		TableViewerColumn colType = new TableViewerColumn(tableViewer, SWT.NONE);
		colType.getColumn().setText("Type"); colType.getColumn().setWidth(100);
		colType.setLabelProvider(new ColumnLabelProvider() { @Override public String getText(Object element) { return ((Task)element).getType(); } });
		TableViewerColumn colStatus = new TableViewerColumn(tableViewer, SWT.NONE);
		colStatus.getColumn().setText("Status"); colStatus.getColumn().setWidth(100);
		colStatus.setLabelProvider(new ColumnLabelProvider() { @Override public String getText(Object element) { return ((Task)element).getStatus().toString(); } });
	}

	public void handleMoveTask(int direction) {
		IStructuredSelection selection = (IStructuredSelection) proposedTasksGroup.getTableViewer().getSelection();
		if (selection.isEmpty()) return;
		Task task = (Task) selection.getFirstElement();
		int index = orchestrator.getTasks().indexOf(task);
		int newIndex = index + direction;
		if (newIndex >= 0 && newIndex < orchestrator.getTasks().size()) {
			orchestrator.getTasks().move(newIndex, index); editor.setDirty(true); proposedTasksGroup.getTableViewer().refresh();
		}
	}

	public void handleDeleteTask() {
		IStructuredSelection selection = (IStructuredSelection) proposedTasksGroup.getTableViewer().getSelection();
		if (selection.isEmpty()) return;
		Task task = (Task) selection.getFirstElement();
		orchestrator.getTasks().remove(task); editor.setDirty(true); proposedTasksGroup.getTableViewer().refresh();
	}

	private void refreshUI() {
		if (isDisposed()) return;
		Display.getDefault().asyncExec(() -> {
			if (isDisposed()) return;
			summaryGroup.updateUI();
			reviewGroup.updateUI();
			feedbackGroup.updateUI();
			proposedTasksGroup.updateUI();
			refreshBrowser();
			this.reflow(true);
		});
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

	private void refreshBrowser() {
		if (vizGroup.getBrowser() == null || vizGroup.getBrowser().isDisposed()) return;

		String json = getModelAsJson();
		if (json.equals(lastJson) && isLoaded) return;

		if (!isLoaded) {
			vizGroup.getBrowser().setText(getHtmlTemplate());
			return;
		}

		try {
			Object result = vizGroup.getBrowser().evaluate("return typeof updateGraph !== 'undefined';");
			if (result instanceof Boolean && (Boolean) result) {
				vizGroup.getBrowser().execute("updateGraph(" + json + ");");
				lastJson = json;
				initRetries = 0;
			} else {
				if (initRetries < MAX_INIT_RETRIES) {
					initRetries++;
					vizGroup.getBrowser().setText(getHtmlTemplate());
				}
			}
		} catch (Exception e) {
			// Browser might not be ready for evaluate
		}
	}

	public void handleApprove() {
		if (editor.getCurrentContext() != null) {
			editor.getCurrentContext().provideApproval(true);
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK); mb.setText("Approval Confirmed"); mb.setMessage("Approval confirmed and orchestration will continue."); mb.open();
			editor.showAiChatPage();
		} else {
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK); mb.setText("Approval Confirmed"); mb.setMessage("Approval confirmed and changes applied to the system."); mb.open();
			editor.showAiChatPage();
		}
	}

	public void handleApproveAll() {
		if (editor.getCurrentContext() != null) {
			editor.getCurrentContext().setAutoApprove(true);
			editor.getCurrentContext().provideApproval(true);
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK); mb.setText("Auto-Approval Enabled"); mb.setMessage("All remaining tasks in the current orchestration loop will be automatically approved."); mb.open();
			editor.showAiChatPage();
		} else {
			handleApprove();
		}
	}

	public void handleReject() {
		if (editor.getCurrentContext() != null) {
			editor.getCurrentContext().provideApproval(false);
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK); mb.setText("Changes Requested"); mb.setMessage("Changes rejected. Orchestration aborted."); mb.open();
		} else {
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK); mb.setText("Changes Requested"); mb.setMessage("Changes rejected. Feedback sent to the autonomous agent for refinement."); mb.open();
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
			String projectRoot = orchestrator.getFileConfig() != null ? orchestrator.getFileConfig().getLocalPath() : null;
			if (projectRoot != null && new File(projectRoot).exists()) {
				IterationMemoryService memoryService = new IterationMemoryService(new File(projectRoot));
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
				+ "body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 0; overflow: hidden; }"
				+ "#canvas { width: 100%; height: 100%; }"
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
				+ "<svg id='canvas' viewBox='0 0 1000 800'><defs>"
				+ "<marker id='arrowhead' markerWidth='10' markerHeight='7' refX='10' refY='3.5' orient='auto'><polygon points='0 0, 10 3.5, 0 7' fill='#94a3b8'/></marker>"
				+ "<marker id='loop-arrow' markerWidth='6' markerHeight='4' refX='6' refY='2' orient='auto'><polygon points='0 0, 6 2, 0 4' fill='#94a3b8'/></marker>"
				+ "</defs>"
				+ "<rect class='loop-bg' x='10' y='10' width='220' height='220' />"
				+ "<g id='loop-diagram' transform='translate(120, 120)'></g>"
				+ "<g id='viewport' transform='translate(280, 20)'></g></svg>"
				+ "<script>"
				+ "var viewport = document.getElementById('viewport');"
				+ "var currentZoom = 1.0;"
				+ "function applyZoom(factor) {"
				+ "  currentZoom *= factor;"
				+ "  viewport.setAttribute('transform', 'translate(280, 20) scale(' + currentZoom + ')');"
				+ "}"
				+ "function resetZoom() {"
				+ "  currentZoom = 1.0;"
				+ "  viewport.setAttribute('transform', 'translate(280, 20)');"
				+ "}"
				+ "function updateGraph(data) {"
				+ "  viewport.innerHTML = '';"
				+ "  if (!data) return;"
				+ "  var nodes = {};"
				+ "  var links = [];"
				+ "  var x = 140, y = 40;"
				+ "  if (data.tasks) {"
				+ "    data.tasks.forEach(function(t) {"
				+ "      nodes[t.id] = { id: t.id, name: t.name, status: t.status, next: t.next, x: x, y: y };"
				+ "      x += 180; if (x > 500) { x = 140; y += 100; }"
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
	public void dispose() {
		if (orchestrator != null) orchestrator.eAdapters().remove(modelAdapter);
		if (toolkit != null) toolkit.dispose();
		super.dispose();
	}
}
