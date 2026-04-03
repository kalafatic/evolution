package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class AiFlowPage extends Composite {
	private Browser browser;
	private Orchestrator orchestrator;
	private MultiPageEditor editor;
	private boolean isLoaded = false;
	private ScrolledComposite vizScrolled;
	private Composite browserContainer;
	private int browserWidth = 1000;
	private int browserHeight = 800;

	private Adapter modelAdapter = new EContentAdapter() {
		@Override
		public void notifyChanged(Notification notification) {
			super.notifyChanged(notification);
			refreshBrowser();
		}
	};

	public AiFlowPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, SWT.NONE);
		this.editor = editor;
		this.setLayout(new GridLayout(1, false));

		ToolBarManager toolbarManager = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
		toolbarManager.add(new Action("Zoom In") {
			@Override
			public void run() {
				browserWidth = (int)(browserWidth * 1.2);
				browserHeight = (int)(browserHeight * 1.2);
				updateScrolledContent();
			}
		});
		toolbarManager.add(new Action("Zoom Out") {
			@Override
			public void run() {
				browserWidth = (int)(browserWidth * 0.8);
				browserHeight = (int)(browserHeight * 0.8);
				updateScrolledContent();
			}
		});
		toolbarManager.add(new Action("Reset Zoom") {
			@Override
			public void run() {
				browserWidth = 1000;
				browserHeight = 800;
				updateScrolledContent();
			}
		});
		toolbarManager.createControl(this);

		vizScrolled = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		vizScrolled.setLayoutData(new GridData(GridData.FILL_BOTH));
		vizScrolled.setExpandHorizontal(true);
		vizScrolled.setExpandVertical(true);

		browserContainer = new Composite(vizScrolled, SWT.NONE);
		browserContainer.setLayout(new GridLayout(1, false));
		vizScrolled.setContent(browserContainer);

		this.browser = new Browser(browserContainer, SWT.NONE);
		GridData browserGD = new GridData(SWT.LEFT, SWT.TOP, false, false);
		browserGD.widthHint = browserWidth;
		browserGD.heightHint = browserHeight;
		browser.setLayoutData(browserGD);

		new BrowserFunction(browser, "javaZoom") {
			@Override
			public Object function(Object[] arguments) {
				if (arguments.length > 0 && arguments[0] instanceof Number) {
					double factor = ((Number) arguments[0]).doubleValue();
					browserWidth = (int) (browserWidth * factor);
					browserHeight = (int) (browserHeight * factor);
					updateScrolledContent();
				}
				return null;
			}
		};

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
				// Prevent navigation to local file system which happens on some reloads
				if (event.location.startsWith("file://") || event.location.equals("about:blank")) {
					if (!event.location.equals("about:blank")) {
						event.doit = false;
						browser.setText(getHtmlTemplate());
					}
				}
			}
		});

		setOrchestrator(orchestrator);
		browser.setText(getHtmlTemplate());
		updateScrolledContent();
	}

	private void updateScrolledContent() {
		if (vizScrolled == null || vizScrolled.isDisposed()) return;
		if (browser != null && !browser.isDisposed()) {
			GridData gd = (GridData) browser.getLayoutData();
			gd.widthHint = browserWidth;
			gd.heightHint = browserHeight;
		}
		browserContainer.layout(true, true);
		vizScrolled.setMinSize(browserContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	public void setOrchestrator(Orchestrator orchestrator) {
		if (this.orchestrator != null) {
			this.orchestrator.eAdapters().remove(modelAdapter);
		}
		this.orchestrator = orchestrator;
		if (this.orchestrator != null) {
			this.orchestrator.eAdapters().add(modelAdapter);
		}
		refreshBrowser();
	}

	private void refreshBrowser() {
		if (browser == null || browser.isDisposed())
			return;
		Display.getDefault().asyncExec(() -> {
			if (browser == null || browser.isDisposed()) return;

			if (!isLoaded) {
				browser.setText(getHtmlTemplate());
				return;
			}

			String json = getModelAsJson();
			// If updateGraph is not defined, the template was lost (e.g. after reload)
			Object result = browser.evaluate("return typeof updateGraph !== 'undefined';");
			if (result instanceof Boolean && (Boolean) result) {
				browser.execute("updateGraph(" + json + ");");
			} else {
				isLoaded = false;
				browser.setText(getHtmlTemplate());
			}
		});
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
			JSONArray taskIds = new JSONArray();
			for (Task t : agent.getTasks()) {
				taskIds.put(t.getId());
			}
			agentObj.put("tasks", taskIds);
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
		for (Task n : task.getNext()) {
			nextIds.put(n.getId());
		}
		obj.put("next", nextIds);
		JSONArray subTasks = new JSONArray();
		for (Task st : task.getSubTasks()) {
			subTasks.put(serializeTask(st));
		}
		obj.put("subTasks", subTasks);
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
				+ "<svg id='canvas' viewBox='0 0 1000 800'><defs>"
				+ "<marker id='arrowhead' markerWidth='10' markerHeight='7' refX='10' refY='3.5' orient='auto'><polygon points='0 0, 10 3.5, 0 7' fill='#94a3b8'/></marker>"
				+ "<marker id='loop-arrow' markerWidth='6' markerHeight='4' refX='6' refY='2' orient='auto'><polygon points='0 0, 6 2, 0 4' fill='#94a3b8'/></marker>"
				+ "</defs>"
				+ "<rect class='loop-bg' x='10' y='10' width='220' height='220' />"
				+ "<g id='loop-diagram' transform='translate(120, 120)'></g>"
				+ "<g id='viewport' transform='translate(280, 0)'></g></svg>"
				+ "<script>" + "var viewport = document.getElementById('viewport');"
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
				+ "  viewport.innerHTML = '';" + "  if (!data) return;" + "  var nodes = {};" + "  var links = [];"
				+ "  // Flow-like layout: sequential" + "  var x = 200, y = 80;" + "  if (data.tasks) {"
				+ "    data.tasks.forEach(function(t) {"
				+ "      nodes[t.id] = { id: t.id, name: t.name, status: t.status, next: t.next, x: x, y: y };"
				+ "      x += 220;" + "      if (x > 700) { x = 200; y += 120; }"
				+ "      if (t.next) { t.next.forEach(function(nid) { links.push({ from: t.id, to: nid }); }); }"
				+ "    });" + "  }" + "  // Agent layout" + "  var ay = 80;" + "  if (data.agents) {"
				+ "    data.agents.forEach(function(a) {"
				+ "      var agentNode = { id: a.id, name: a.id, type: 'agent', x: 20, y: ay };"
				+ "      nodes[a.id] = agentNode;" + "      ay += 120;" + "      if (a.tasks) {"
				+ "        a.tasks.forEach(function(tid) { links.push({ from: a.id, to: tid, type: 'agent-link' }); });"
				+ "      }" + "    });" + "  }" + "  links.forEach(function(l) {" + "    var n1 = nodes[l.from];"
				+ "    var n2 = nodes[l.to];" + "    if (n1 && n2) {"
				+ "      var line = document.createElementNS('http://www.w3.org/2000/svg', 'line');"
				+ "      var isAgentLink = l.type === 'agent-link';"
				+ "      line.setAttribute('x1', isAgentLink ? n1.x + 40 : n1.x + 160);"
				+ "      line.setAttribute('y1', isAgentLink ? n1.y + 40 : n1.y + 30);"
				+ "      line.setAttribute('x2', n2.x);" + "      line.setAttribute('y2', n2.y + 30);"
				+ "      line.className.baseVal = isAgentLink ? 'agent-link' : '';"
				+ "      viewport.appendChild(line);" + "    }" + "  });"
				+ "  Object.keys(nodes).forEach(function(key) {" + "    var n = nodes[key];"
				+ "    var g = document.createElementNS('http://www.w3.org/2000/svg', 'g');"
				+ "    var isAgent = n.type === 'agent';" + "    if (isAgent) {"
				+ "      var circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');"
				+ "      circle.setAttribute('cx', n.x + 40);" + "      circle.setAttribute('cy', n.y + 40);"
				+ "      circle.setAttribute('r', 40);" + "      circle.className.baseVal = 'node agent';"
				+ "      g.appendChild(circle);" + "    } else {"
				+ "      var rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');"
				+ "      rect.setAttribute('x', n.x);" + "      rect.setAttribute('y', n.y);"
				+ "      rect.setAttribute('width', 160);" + "      rect.setAttribute('height', 60);"
				+ "      rect.setAttribute('rx', 8);" + "      rect.className.baseVal = 'node task ' + n.status;"
				+ "      g.appendChild(rect);" + "    }"
				+ "    var text = document.createElementNS('http://www.w3.org/2000/svg', 'text');"
				+ "    text.setAttribute('x', isAgent ? n.x + 40 : n.x + 80);"
				+ "    text.setAttribute('y', isAgent ? n.y + 45 : n.y + 35);"
				+ "    text.textContent = n.name.length > 20 ? n.name.substring(0, 17) + '...' : n.name;"
				+ "    g.appendChild(text);" + "    viewport.appendChild(g);" + "  });"
				+ "  if (data.session) {"
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
		if (orchestrator != null) {
			orchestrator.eAdapters().remove(modelAdapter);
		}
		super.dispose();
	}
}
