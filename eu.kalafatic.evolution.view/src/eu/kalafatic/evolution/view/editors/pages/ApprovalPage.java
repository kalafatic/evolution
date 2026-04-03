package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
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
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Table;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class ApprovalPage extends Composite {
	private MultiPageEditor editor;
	private Orchestrator orchestrator;
	private Browser browser;
	private boolean isLoaded = false;
	private boolean pendingRefresh = false;
	private Label sessionIdLabel;
	private Label statusLabel;
	private Label iterationsLabel;
	private Label branchLabel;
	private TableViewer tableViewer;

	private Adapter modelAdapter = new EContentAdapter() {
		@Override
		public void notifyChanged(Notification notification) {
			super.notifyChanged(notification);
			refreshUI();
		}
	};

	public ApprovalPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, SWT.NONE);
		this.editor = editor;
		this.orchestrator = orchestrator;
		createControl();
		setOrchestrator(orchestrator);
	}

	private void createControl() {
		this.setLayout(new GridLayout(1, false));

		// Summary Group
		Group summaryGroup = SWTFactory.createGroup(this, "Approval Summary", 2);
		SWTFactory.createLabel(summaryGroup, "Session ID:");
		sessionIdLabel = new Label(summaryGroup, SWT.NONE);
		sessionIdLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		SWTFactory.createLabel(summaryGroup, "Status:");
		statusLabel = new Label(summaryGroup, SWT.NONE);
		statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		SWTFactory.createLabel(summaryGroup, "Iterations:");
		iterationsLabel = new Label(summaryGroup, SWT.NONE);
		iterationsLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		SWTFactory.createLabel(summaryGroup, "Git Branch:");
		branchLabel = new Label(summaryGroup, SWT.NONE);
		branchLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Task Management Group
		Group taskGroup = SWTFactory.createGroup(this, "Proposed Tasks", 1);
		taskGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		((GridData)taskGroup.getLayoutData()).heightHint = 180;

		Composite taskTableComposite = new Composite(taskGroup, SWT.NONE);
		taskTableComposite.setLayout(new GridLayout(2, false));
		taskTableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

		tableViewer = new TableViewer(taskTableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		createColumns();
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());

		Composite taskActions = new Composite(taskTableComposite, SWT.NONE);
		taskActions.setLayout(new GridLayout(1, false));
		taskActions.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, false, false));

		Button upBtn = SWTFactory.createButton(taskActions, "Move Up", 80);
		upBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				handleMoveTask(-1);
			}
		});

		Button downBtn = SWTFactory.createButton(taskActions, "Move Down", 80);
		downBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				handleMoveTask(1);
			}
		});

		Button deleteBtn = SWTFactory.createButton(taskActions, "Delete", 80);
		deleteBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				handleDeleteTask();
			}
		});

		// Visualization Area
		Group vizGroup = SWTFactory.createGroup(this, "AI Network & Process Flow", 1);
		vizGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		vizGroup.setLayout(new GridLayout(1, false));

		ToolBarManager toolbarManager = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		toolbarManager.add(new Action("Zoom In") {
			@Override
			public void run() {
				browser.execute("applyZoom(1.2);");
			}
		});
		toolbarManager.add(new Action("Zoom Out") {
			@Override
			public void run() {
				browser.execute("applyZoom(0.8);");
			}
		});
		toolbarManager.add(new Action("Reset Zoom") {
			@Override
			public void run() {
				browser.execute("resetZoom();");
			}
		});
		toolbarManager.createControl(vizGroup);

		browser = new Browser(vizGroup, SWT.NONE);
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		browser.addProgressListener(new ProgressAdapter() {
			@Override
			public void completed(ProgressEvent event) {
				isLoaded = true;
				refreshBrowser();
			}
		});
		browser.addLocationListener(new LocationAdapter() {
			@Override
			public void changing(LocationEvent event) {
				if (event.location.startsWith("file://")) {
					event.doit = false;
				}
			}
		});
		browser.setText(getHtmlTemplate());

		// Actions Area
		Group actionsGroup = SWTFactory.createGroup(this, "Review Actions", 2);
		Button approveBtn = SWTFactory.createButton(actionsGroup, "Approve & Apply", 150);
		approveBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				handleApprove();
			}
		});

		Button rejectBtn = SWTFactory.createButton(actionsGroup, "Reject & Request Changes", 200);
		rejectBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
			@Override
			public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
				handleReject();
			}
		});
	}

	public void setOrchestrator(Orchestrator orchestrator) {
		if (this.orchestrator != null) {
			this.orchestrator.eAdapters().remove(modelAdapter);
		}
		this.orchestrator = orchestrator;
		if (this.orchestrator != null) {
			this.orchestrator.eAdapters().add(modelAdapter);
		}
		refreshUI();
	}

	private void createColumns() {
		TableViewerColumn colName = new TableViewerColumn(tableViewer, SWT.NONE);
		colName.getColumn().setText("Task Name (Click to edit)");
		colName.getColumn().setWidth(300);
		colName.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Task)element).getName();
			}
		});
		colName.setEditingSupport(new org.eclipse.jface.viewers.EditingSupport(tableViewer) {
			@Override
			protected void setValue(Object element, Object value) {
				((Task)element).setName(String.valueOf(value));
				editor.setDirty(true);
				tableViewer.update(element, null);
			}
			@Override
			protected Object getValue(Object element) {
				return ((Task)element).getName();
			}
			@Override
			protected org.eclipse.jface.viewers.CellEditor getCellEditor(Object element) {
				return new org.eclipse.jface.viewers.TextCellEditor(tableViewer.getTable());
			}
			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		});

		TableViewerColumn colType = new TableViewerColumn(tableViewer, SWT.NONE);
		colType.getColumn().setText("Type");
		colType.getColumn().setWidth(100);
		colType.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Task)element).getType();
			}
		});

		TableViewerColumn colStatus = new TableViewerColumn(tableViewer, SWT.NONE);
		colStatus.getColumn().setText("Status");
		colStatus.getColumn().setWidth(100);
		colStatus.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Task)element).getStatus().toString();
			}
		});
	}

	private void handleMoveTask(int direction) {
		IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
		if (selection.isEmpty()) return;
		Task task = (Task) selection.getFirstElement();
		int index = orchestrator.getTasks().indexOf(task);
		int newIndex = index + direction;
		if (newIndex >= 0 && newIndex < orchestrator.getTasks().size()) {
			orchestrator.getTasks().move(newIndex, index);
			editor.setDirty(true);
			tableViewer.refresh();
		}
	}

	private void handleDeleteTask() {
		IStructuredSelection selection = (IStructuredSelection) tableViewer.getSelection();
		if (selection.isEmpty()) return;
		Task task = (Task) selection.getFirstElement();
		orchestrator.getTasks().remove(task);
		editor.setDirty(true);
		tableViewer.refresh();
	}

	private void refreshUI() {
		if (isDisposed()) return;
		Display.getDefault().asyncExec(() -> {
			if (isDisposed()) return;

			if (orchestrator != null) {
				tableViewer.setInput(orchestrator.getTasks());
			}

			if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
				SelfDevSession session = orchestrator.getSelfDevSession();
				sessionIdLabel.setText(session.getId() != null ? session.getId() : "N/A");
				statusLabel.setText(session.getStatus() != null ? session.getStatus().toString() : "N/A");
				iterationsLabel.setText(String.valueOf(session.getIterations().size()) + " / " + session.getMaxIterations());

				if (!session.getIterations().isEmpty()) {
					Iteration last = session.getIterations().get(session.getIterations().size() - 1);
					branchLabel.setText(last.getBranchName() != null ? last.getBranchName() : "N/A");
				} else {
					branchLabel.setText("N/A");
				}
			} else {
				sessionIdLabel.setText("No active session");
				statusLabel.setText("N/A");
				iterationsLabel.setText("0 / 0");
				branchLabel.setText("N/A");
			}
			refreshBrowser();
		});
	}

	private void refreshBrowser() {
		if (browser == null || browser.isDisposed() || pendingRefresh) return;

		pendingRefresh = true;
		Display.getDefault().timerExec(100, () -> {
			if (browser == null || browser.isDisposed()) {
				pendingRefresh = false;
				return;
			}

			if (!isLoaded) {
				browser.setText(getHtmlTemplate());
				pendingRefresh = false;
				return;
			}

			String json = getModelAsJson();
			try {
				Object result = browser.evaluate("return typeof updateGraph !== 'undefined';");
				if (result instanceof Boolean && (Boolean) result) {
					browser.execute("updateGraph(" + json + ");");
				} else {
					isLoaded = false;
					browser.setText(getHtmlTemplate());
				}
			} catch (Exception e) {
				isLoaded = false;
				browser.setText(getHtmlTemplate());
			} finally {
				pendingRefresh = false;
			}
		});
	}

	private void handleApprove() {
		if (editor.getCurrentContext() != null) {
			editor.getCurrentContext().provideApproval(true);
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
			mb.setText("Approval Confirmed");
			mb.setMessage("Approval confirmed and orchestration will continue.");
			mb.open();
			editor.showAiChatPage();
		} else {
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
			mb.setText("Approval Confirmed");
			mb.setMessage("Approval confirmed and changes applied to the system.");
			mb.open();
			editor.showAiChatPage();
		}
	}

	private void handleReject() {
		if (editor.getCurrentContext() != null) {
			editor.getCurrentContext().provideApproval(false);
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK);
			mb.setText("Changes Requested");
			mb.setMessage("Changes rejected. Orchestration aborted.");
			mb.open();
		} else {
			MessageBox mb = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.OK);
			mb.setText("Changes Requested");
			mb.setMessage("Changes rejected. Feedback sent to the autonomous agent for refinement.");
			mb.open();
		}
	}

	private String getModelAsJson() {
		if (orchestrator == null) return "{}";
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
			tasksArr.put(serializeTask(task));
		}
		root.put("tasks", tasksArr);

		if (orchestrator.getSelfDevSession() != null) {
			SelfDevSession session = orchestrator.getSelfDevSession();
			JSONObject sessionObj = new JSONObject();
			sessionObj.put("id", session.getId());
			sessionObj.put("status", session.getStatus().toString());

			if (!session.getIterations().isEmpty()) {
				Iteration last = session.getIterations().get(session.getIterations().size() - 1);
				sessionObj.put("phase", last.getPhase() != null ? last.getPhase() : "IDLE");
			} else {
				sessionObj.put("phase", "IDLE");
			}

			root.put("session", sessionObj);
		}

		return root.toString();
	}

	private JSONObject serializeTask(Task task) {
		JSONObject obj = new JSONObject();
		obj.put("id", task.getId());
		obj.put("name", task.getName());
		obj.put("status", task.getStatus().toString());
		JSONArray nextIds = new JSONArray();
		for (Task n : task.getNext()) nextIds.put(n.getId());
		obj.put("next", nextIds);
		return obj;
	}

	private String getHtmlTemplate() {
		return "<!DOCTYPE html><html><head><style>"
				+ "body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; overflow: hidden; }"
				+ "#canvas { width: 100vw; height: 100vh; }"
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
				+ "<g id='viewport' transform='translate(280, 0)'></g></svg>"
				+ "<script>"
				+ "var viewport = document.getElementById('viewport');"
				+ "var currentZoom = 1.0;"
				+ "function applyZoom(factor) {"
				+ "  currentZoom *= factor;"
				+ "  viewport.setAttribute('transform', 'translate(280, 0) scale(' + currentZoom + ')');"
				+ "}"
				+ "function resetZoom() {"
				+ "  currentZoom = 1.0;"
				+ "  viewport.setAttribute('transform', 'translate(280, 0)');"
				+ "}"
				+ "function updateGraph(data) {"
				+ "  viewport.innerHTML = '';"
				+ "  if (!data) return;"
				+ "  var nodes = {};"
				+ "  var links = [];"
				+ "  var x = 200, y = 80;"
				+ "  if (data.tasks) {"
				+ "    data.tasks.forEach(function(t) {"
				+ "      nodes[t.id] = { id: t.id, name: t.name, status: t.status, next: t.next, x: x, y: y };"
				+ "      x += 220; if (x > 700) { x = 200; y += 120; }"
				+ "      if (t.next) t.next.forEach(function(nid) { links.push({ from: t.id, to: nid }); });"
				+ "    });"
				+ "  }"
				+ "  var ay = 80;"
				+ "  if (data.agents) {"
				+ "    data.agents.forEach(function(a) {"
				+ "      nodes[a.id] = { id: a.id, name: a.id, type: 'agent', x: 20, y: ay };"
				+ "      ay += 120;"
				+ "    });"
				+ "  }"
				+ "  links.forEach(function(l) {"
				+ "    var n1 = nodes[l.from], n2 = nodes[l.to];"
				+ "    if (n1 && n2) {"
				+ "      var line = document.createElementNS('http://www.w3.org/2000/svg', 'line');"
				+ "      line.setAttribute('x1', n1.x + 160); line.setAttribute('y1', n1.y + 30);"
				+ "      line.setAttribute('x2', n2.x); line.setAttribute('y2', n2.y + 30);"
				+ "      viewport.appendChild(line);"
				+ "    }"
				+ "  });"
				+ "  Object.keys(nodes).forEach(function(id) {"
				+ "    var n = nodes[id];"
				+ "    var g = document.createElementNS('http://www.w3.org/2000/svg', 'g');"
				+ "    if (n.type === 'agent') {"
				+ "      var circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');"
				+ "      circle.setAttribute('cx', n.x + 40); circle.setAttribute('cy', n.y + 40); circle.setAttribute('r', 40);"
				+ "      circle.className.baseVal = 'node agent';"
				+ "      g.appendChild(circle);"
				+ "    } else {"
				+ "      var rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');"
				+ "      rect.setAttribute('x', n.x); rect.setAttribute('y', n.y); rect.setAttribute('width', 160); rect.setAttribute('height', 60); rect.setAttribute('rx', 8);"
				+ "      rect.className.baseVal = 'node task ' + n.status;"
				+ "      g.appendChild(rect);"
				+ "    }"
				+ "    var txt = document.createElementNS('http://www.w3.org/2000/svg', 'text');"
				+ "    txt.setAttribute('x', n.type === 'agent' ? n.x + 40 : n.x + 80); txt.setAttribute('y', n.type === 'agent' ? n.y + 45 : n.y + 35);"
				+ "    txt.textContent = n.name.length > 18 ? n.name.substring(0, 15) + '...' : n.name;"
				+ "    g.appendChild(txt);"
				+ "    viewport.appendChild(g);"
				+ "  });"
				+ "  if (data.session) {"
				+ "    document.getElementById('info').textContent = 'Session: ' + data.session.id + ' (' + data.session.status + ')';"
				+ "    updateLoopDiagram(data.session.phase);"
				+ "  } else {"
				+ "    updateLoopDiagram('IDLE');"
				+ "  }"
				+ "}"
				+ "function updateLoopDiagram(activePhase) {"
				+ "  var loopContainer = document.getElementById('loop-diagram');"
				+ "  loopContainer.innerHTML = '';"
				+ "  var phases = ['OBSERVE', 'ANALYZE', 'PLAN', 'VALIDATE', 'EXECUTE', 'TEST', 'EVALUATE', 'LEARN'];"
				+ "  var radius = 75;"
				+ "  var centerX = 0, centerY = 0;"
				+ "  phases.forEach(function(p, i) {"
				+ "    var angle = (i / phases.length) * 2 * Math.PI - Math.PI / 2;"
				+ "    var x = centerX + radius * Math.cos(angle);"
				+ "    var y = centerY + radius * Math.sin(angle);"
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
				+ "}"
				+ "</script></body></html>";
	}

	@Override
	public void dispose() {
		if (orchestrator != null) orchestrator.eAdapters().remove(modelAdapter);
		super.dispose();
	}
}
