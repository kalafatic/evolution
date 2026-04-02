package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
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
		toolbarManager.createControl(this);

		this.browser = new Browser(this, SWT.NONE);
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
		if (browser == null || browser.isDisposed() || !isLoaded)
			return;
		Display.getDefault().asyncExec(() -> {
			if (!browser.isDisposed()) {
				String json = getModelAsJson();
				// If updateGraph is not defined, the template was lost (e.g. after reload)
				Object result = browser.evaluate("return typeof updateGraph !== 'undefined';");
				if (result instanceof Boolean && (Boolean) result) {
					browser.execute("updateGraph(" + json + ");");
				} else {
					if (browser != null && !browser.isDisposed()) {
						browser.setText(getHtmlTemplate());
					}
				}
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
				+ "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #fafafa; margin: 0; overflow: hidden; }"
				+ "#canvas { width: 100vw; height: 100vh; }"
				+ ".node { fill: #ffffff; stroke: #cfd8dc; stroke-width: 1px; transition: all 0.3s; }"
				+ ".node:hover { stroke: #607d8b; stroke-width: 2px; }" + ".task { fill: #ffffff; stroke: #607d8b; }"
				+ ".task.DONE { fill: #e8f5e9; stroke: #4caf50; }" + ".task.RUNNING { fill: #fffde7; stroke: #fbc02d; }"
				+ ".task.FAILED { fill: #ffe9e9; stroke: #f44336; }"
				+ ".agent { fill: #e3f2fd; stroke: #2196f3; stroke-width: 2px; }"
				+ ".agent-link { stroke: #2196f3; stroke-width: 1px; stroke-dasharray: 4; }"
				+ "text { font-size: 11px; fill: #455a64; pointer-events: none; text-anchor: middle; }"
				+ "line { stroke: #b0bec5; stroke-width: 1.5px; marker-end: url(#arrowhead); }"
				+ ".loop-bg { fill: #ffffff; stroke: #cbd5e1; stroke-width: 1px; rx: 12; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.05)); }"
				+ ".loop-node { fill: #f1f5f9; stroke: #64748b; stroke-width: 2px; transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1); }"
				+ ".loop-node.active { fill: #3b82f6; stroke: #1d4ed8; stroke-width: 3px; }"
				+ ".loop-text { font-size: 10px; fill: #334155; font-weight: 800; pointer-events: none; text-anchor: middle; }"
				+ ".loop-text.active { fill: #ffffff; }"
				+ ".loop-link { stroke: #94a3b8; stroke-width: 2px; fill: none; marker-end: url(#loop-arrow); }"
				+ ".loop-link.active { stroke: #3b82f6; stroke-width: 3.5px; }"
				+ "@keyframes pulse { 0% { r: 22; stroke-opacity: 1; stroke-width: 3px; } 50% { r: 28; stroke-opacity: 0.3; stroke-width: 6px; } 100% { r: 22; stroke-opacity: 1; stroke-width: 3px; } }"
				+ ".loop-node.active { animation: pulse 1.5s infinite ease-in-out; }"
				+ "</style></head><body>"
				+ "<svg id='canvas' viewBox='0 0 1000 800'><defs>"
				+ "<marker id='arrowhead' markerWidth='10' markerHeight='7' refX='10' refY='3.5' orient='auto'><polygon points='0 0, 10 3.5, 0 7' fill='#b0bec5'/></marker>"
				+ "<marker id='loop-arrow' markerWidth='6' markerHeight='4' refX='6' refY='2' orient='auto'><polygon points='0 0, 6 2, 0 4' fill='#94a3b8'/></marker>"
				+ "</defs>"
				+ "<rect class='loop-bg' x='10' y='10' width='220' height='220' />"
				+ "<g id='loop-diagram' transform='translate(120, 120)'></g>"
				+ "<g id='viewport' transform='translate(280, 0)'></g></svg>"
				+ "<script>" + "const viewport = document.getElementById('viewport');"
				+ "let currentZoom = 1.0;"
				+ "function applyZoom(factor) {"
				+ "  currentZoom *= factor;"
				+ "  viewport.setAttribute('transform', 'scale(' + currentZoom + ')');"
				+ "}"
				+ "function resetZoom() {"
				+ "  currentZoom = 1.0;"
				+ "  viewport.removeAttribute('transform');"
				+ "}"
				+ "function updateGraph(data) {"
				+ "  viewport.innerHTML = '';" + "  if (!data) return;" + "  const nodes = {};" + "  const links = [];"
				+ "  // Flow-like layout: sequential" + "  let x = 200, y = 50;" + "  if (data.tasks) {"
				+ "    data.tasks.forEach(function(t) {" + "      nodes[t.id] = Object.assign({}, t, { x: x, y: y });"
				+ "      x += 220;" + "      if (x > 700) { x = 200; y += 120; }"
				+ "      if (t.next) { t.next.forEach(function(nid) { links.push({ from: t.id, to: nid }); }); }"
				+ "    });" + "  }" + "  // Agent layout" + "  let ay = 50;" + "  if (data.agents) {"
				+ "    data.agents.forEach(function(a) {"
				+ "      const agentNode = { id: a.id, name: a.id, type: 'agent', x: 20, y: ay };"
				+ "      nodes[a.id] = agentNode;" + "      ay += 100;" + "      if (a.tasks) {"
				+ "        a.tasks.forEach(function(tid) { links.push({ from: a.id, to: tid, type: 'agent-link' }); });"
				+ "      }" + "    });" + "  }" + "  links.forEach(function(l) {" + "    const n1 = nodes[l.from];"
				+ "    const n2 = nodes[l.to];" + "    if (n1 && n2) {"
				+ "      const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');"
				+ "      const isAgentLink = l.type === 'agent-link';"
				+ "      line.setAttribute('x1', isAgentLink ? n1.x + 40 : n1.x + 160);"
				+ "      line.setAttribute('y1', isAgentLink ? n1.y + 40 : n1.y + 25);"
				+ "      line.setAttribute('x2', n2.x);" + "      line.setAttribute('y2', n2.y + 25);"
				+ "      line.className.baseVal = isAgentLink ? 'agent-link' : '';"
				+ "      viewport.appendChild(line);" + "    }" + "  });"
				+ "  Object.keys(nodes).forEach(function(key) {" + "    const n = nodes[key];"
				+ "    const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');"
				+ "    const isAgent = n.type === 'agent';" + "    if (isAgent) {"
				+ "      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');"
				+ "      circle.setAttribute('cx', n.x + 40);" + "      circle.setAttribute('cy', n.y + 40);"
				+ "      circle.setAttribute('r', 40);" + "      circle.className.baseVal = 'node agent';"
				+ "      g.appendChild(circle);" + "    } else {"
				+ "      const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');"
				+ "      rect.setAttribute('x', n.x);" + "      rect.setAttribute('y', n.y);"
				+ "      rect.setAttribute('width', 160);" + "      rect.setAttribute('height', 50);"
				+ "      rect.setAttribute('rx', 8);" + "      rect.className.baseVal = 'node task ' + n.status;"
				+ "      g.appendChild(rect);" + "    }"
				+ "    const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');"
				+ "    text.setAttribute('x', isAgent ? n.x + 40 : n.x + 80);"
				+ "    text.setAttribute('y', isAgent ? n.y + 45 : n.y + 30);"
				+ "    text.textContent = n.name.length > 20 ? n.name.substring(0, 17) + '...' : n.name;"
				+ "    g.appendChild(text);" + "    viewport.appendChild(g);" + "  });"
				+ "  if (data.session) {"
				+ "    updateLoopDiagram(data.session.phase);"
				+ "  } else {"
				+ "    updateLoopDiagram('IDLE');"
				+ "  }"
				+ "}"
				+ "function updateLoopDiagram(activePhase) {"
				+ "  const loopContainer = document.getElementById('loop-diagram');"
				+ "  loopContainer.innerHTML = '';"
				+ "  const phases = ['OBSERVE', 'ANALYZE', 'PLAN', 'VALIDATE', 'EXECUTE', 'TEST', 'EVALUATE', 'LEARN'];"
				+ "  const radius = 75;"
				+ "  const centerX = 0, centerY = 0;"
				+ "  phases.forEach((p, i) => {"
				+ "    const angle = (i / phases.length) * 2 * Math.PI - Math.PI / 2;"
				+ "    const x = centerX + radius * Math.cos(angle);"
				+ "    const y = centerY + radius * Math.sin(angle);"
				+ "    const isActive = p === activePhase;"
				+ "    const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');"
				+ "    const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');"
				+ "    circle.setAttribute('cx', x); circle.setAttribute('cy', y); circle.setAttribute('r', 22);"
				+ "    circle.className.baseVal = 'loop-node' + (isActive ? ' active' : '');"
				+ "    g.appendChild(circle);"
				+ "    const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');"
				+ "    text.setAttribute('x', x); text.setAttribute('y', y + 4);"
				+ "    text.className.baseVal = 'loop-text' + (isActive ? ' active' : '');"
				+ "    text.textContent = p.substring(0, 3);"
				+ "    g.appendChild(text);"
				+ "    loopContainer.appendChild(g);"
				+ "    const nextAngle = ((i + 1) / phases.length) * 2 * Math.PI - Math.PI / 2;"
				+ "    const x1 = centerX + (radius) * Math.cos(angle + 0.35);"
				+ "    const y1 = centerY + (radius) * Math.sin(angle + 0.35);"
				+ "    const x2 = centerX + (radius) * Math.cos(nextAngle - 0.35);"
				+ "    const y2 = centerY + (radius) * Math.sin(nextAngle - 0.35);"
				+ "    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');"
				+ "    path.setAttribute('d', `M ${x1} ${y1} A ${radius} ${radius} 0 0 1 ${x2} ${y2}`);"
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
