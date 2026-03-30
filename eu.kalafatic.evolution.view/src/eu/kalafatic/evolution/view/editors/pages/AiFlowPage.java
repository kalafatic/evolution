package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class AiFlowPage extends Composite {
    private Browser browser;
    private Orchestrator orchestrator;
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
        this.setLayout(new FillLayout());
        this.browser = new Browser(this, SWT.NONE);

        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                isLoaded = true;
                refreshBrowser();
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
        if (browser == null || browser.isDisposed() || !isLoaded) return;
        Display.getDefault().asyncExec(() -> {
            if (!browser.isDisposed()) {
                String json = getModelAsJson();
                browser.execute("updateGraph(" + json + ");");
            }
        });
    }

    private String getModelAsJson() {
        if (orchestrator == null) return "{}";
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
        return "<!DOCTYPE html><html><head><style>" +
               "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #fafafa; margin: 0; overflow: hidden; }" +
               "#canvas { width: 100vw; height: 100vh; }" +
               ".node { fill: #ffffff; stroke: #cfd8dc; stroke-width: 1px; transition: all 0.3s; }" +
               ".node:hover { stroke: #607d8b; stroke-width: 2px; }" +
               ".task { fill: #ffffff; stroke: #607d8b; }" +
               ".task.DONE { fill: #e8f5e9; stroke: #4caf50; }" +
               ".task.RUNNING { fill: #fffde7; stroke: #fbc02d; }" +
               ".task.FAILED { fill: #ffe9e9; stroke: #f44336; }" +
               ".agent { fill: #e3f2fd; stroke: #2196f3; stroke-width: 2px; }" +
               ".agent-link { stroke: #2196f3; stroke-width: 1px; stroke-dasharray: 4; }" +
               "text { font-size: 11px; fill: #455a64; pointer-events: none; text-anchor: middle; }" +
               "line { stroke: #b0bec5; stroke-width: 1.5px; marker-end: url(#arrowhead); }" +
               "</style></head><body>" +
               "<svg id='canvas' viewBox='0 0 1000 800'><defs><marker id='arrowhead' markerWidth='10' markerHeight='7' refX='10' refY='3.5' orient='auto'><polygon points='0 0, 10 3.5, 0 7' fill='#b0bec5'/></marker></defs><g id='viewport'></g></svg>" +
               "<script>" +
               "const viewport = document.getElementById('viewport');" +
               "function updateGraph(data) {" +
               "  viewport.innerHTML = '';" +
               "  if (!data) return;" +
               "  const nodes = {};" +
               "  const links = [];" +
               "  // Flow-like layout: sequential" +
               "  let x = 300, y = 50;" +
               "  if (data.tasks) {" +
               "    data.tasks.forEach(function(t) {" +
               "      nodes[t.id] = Object.assign({}, t, { x: x, y: y });" +
               "      x += 220;" +
               "      if (x > 800) { x = 300; y += 120; }" +
               "      if (t.next) { t.next.forEach(function(nid) { links.push({ from: t.id, to: nid }); }); }" +
               "    });" +
               "  }" +
               "  // Agent layout" +
               "  let ay = 50;" +
               "  if (data.agents) {" +
               "    data.agents.forEach(function(a) {" +
               "      const agentNode = { id: a.id, name: a.id, type: 'agent', x: 50, y: ay };" +
               "      nodes[a.id] = agentNode;" +
               "      ay += 100;" +
               "      if (a.tasks) {" +
               "        a.tasks.forEach(function(tid) { links.push({ from: a.id, to: tid, type: 'agent-link' }); });" +
               "      }" +
               "    });" +
               "  }" +
               "  links.forEach(function(l) {" +
               "    const n1 = nodes[l.from];" +
               "    const n2 = nodes[l.to];" +
               "    if (n1 && n2) {" +
               "      const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');" +
               "      const isAgentLink = l.type === 'agent-link';" +
               "      line.setAttribute('x1', isAgentLink ? n1.x + 40 : n1.x + 160);" +
               "      line.setAttribute('y1', isAgentLink ? n1.y + 40 : n1.y + 25);" +
               "      line.setAttribute('x2', n2.x);" +
               "      line.setAttribute('y2', n2.y + 25);" +
               "      line.className.baseVal = isAgentLink ? 'agent-link' : '';" +
               "      viewport.appendChild(line);" +
               "    }" +
               "  });" +
               "  Object.keys(nodes).forEach(function(key) {" +
               "    const n = nodes[key];" +
               "    const g = document.createElementNS('http://www.w3.org/2000/svg', 'g');" +
               "    const isAgent = n.type === 'agent';" +
               "    if (isAgent) {" +
               "      const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');" +
               "      circle.setAttribute('cx', n.x + 40);" +
               "      circle.setAttribute('cy', n.y + 40);" +
               "      circle.setAttribute('r', 40);" +
               "      circle.className.baseVal = 'node agent';" +
               "      g.appendChild(circle);" +
               "    } else {" +
               "      const rect = document.createElementNS('http://www.w3.org/2000/svg', 'rect');" +
               "      rect.setAttribute('x', n.x);" +
               "      rect.setAttribute('y', n.y);" +
               "      rect.setAttribute('width', 160);" +
               "      rect.setAttribute('height', 50);" +
               "      rect.setAttribute('rx', 8);" +
               "      rect.className.baseVal = 'node task ' + n.status;" +
               "      g.appendChild(rect);" +
               "    }" +
               "    const text = document.createElementNS('http://www.w3.org/2000/svg', 'text');" +
               "    text.setAttribute('x', isAgent ? n.x + 40 : n.x + 80);" +
               "    text.setAttribute('y', isAgent ? n.y + 45 : n.y + 30);" +
               "    text.textContent = n.name.length > 20 ? n.name.substring(0, 17) + '...' : n.name;" +
               "    g.appendChild(text);" +
               "    viewport.appendChild(g);" +
               "  });" +
               "}" +
               "</script></body></html>";
    }

    @Override
    public void dispose() {
        if (orchestrator != null) {
            orchestrator.eAdapters().remove(modelAdapter);
        }
        super.dispose();
    }
}
