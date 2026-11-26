/* EVO Ecosystem — vanilla SVG topology graph. No frameworks.
   Java pushes snapshots via window.eco.applySnapshot(json); JS diffs
   incrementally so viewport, selection, collapse state and manual node
   positions survive every update. Business logic stays in Java. */
"use strict";

(function () {
  var SVGNS = "http://www.w3.org/2000/svg";

  // ------------------------------------------------------------- state
  var model = { nodes: {}, edges: [], events: [], meta: {} };
  var ui = {
    viewport: { x: 60, y: 60, k: 1 },
    selectedId: null,
    manualPositions: {},      // nodeId -> {x,y} (user-dragged)
    collapsed: {},            // nodeId -> true
    autoLayout: true,
    filters: { product: "", type: "", status: "" },
    search: "",
    mode: "graph",            // graph | overview
    followEvents: false,
    pending: null,            // pending async action {action,nodeId}
  };

  var els = {};
  function $(id) { return document.getElementById(id); }

  var NODE_W = 190, NODE_H = 46;
  var TYPE_TAGS = {
    ROOT: "ROOT", PRODUCT: "PRODUCT", SERVICE: "SERVICE", PROVIDER: "PROVIDER",
    MODEL: "MODEL", INSTANCE: "INSTANCE", PROJECT: "PROJECT", WORKSPACE: "WORKSPACE",
    SESSION: "SESSION", TASK: "TASK", RESOURCE: "RESOURCE", HARDWARE: "HARDWARE"
  };

  // ------------------------------------------------------------- bridge
  function ready() {
    try { if (window.ecoReady) window.ecoReady(); } catch (e) { /* noop */ }
  }

  function callJava(action, nodeId, payload) {
    try {
      var raw = window.ecoAction ? window.ecoAction(action, nodeId || "", payload || "") : null;
      if (raw === null || raw === undefined) return { ok: false, error: "Bridge unavailable" };
      if (typeof raw === "string" && raw.length) {
        return JSON.parse(raw);
      }
      return { ok: true, raw: String(raw) };
    } catch (e) {
      return { ok: false, error: String(e) };
    }
  }

  // ======================================================= applySnapshot
  window.eco = {
    applySnapshot: function (json) { applySnapshot(json); },
    setConnection: function (state, message) {
      var c = $("conn");
      c.textContent = state === "ok" ? "UPM connected" : (message || "UPM unreachable");
      c.className = "conn " + (state === "ok" ? "ok" : "err");
    }
  };

  function applySnapshot(json) {
    var incoming = {};
    var edges = [];
    var keepCollapsed = {};
    var keepSelected = ui.selectedId;
    var parentOf = {};
    json.edges.forEach(function (e) {
      edges.push(e);
      if (e.type === "HIERARCHICAL") parentOf[e.target] = e.source;
    });
    var nextFreeY = {};
    json.nodes.forEach(function (n) {
      incoming[n.id] = n;
      if (model.nodes[n.id] && model.nodes[n.id].collapsed) keepCollapsed[n.id] = true;
      if (ui.collapsed[n.id]) keepCollapsed[n.id] = true;
      if (ui.manualPositions[n.id]) { n.x = ui.manualPositions[n.id].x; n.y = ui.manualPositions[n.id].y; }
    });

    // remove nodes that disappeared
    Object.keys(model.nodes).forEach(function (id) {
      if (!incoming[id]) removeNodeDom(id);
    });

    var added = [];
    Object.keys(incoming).forEach(function (id) {
      var n = incoming[id];
      var prev = model.nodes[id];
      if (!prev) { model.nodes[id] = n; added.push(id); }
      else { prev.status = n.status; prev.statusDetail = n.statusDetail; prev.metrics = n.metrics; prev.metadata = n.metadata; prev.endpoint = n.endpoint; prev.port = n.port; prev.name = n.name; prev.type = n.type; }
      n.collapsed = !!keepCollapsed[id];
      if (n.x === undefined || n.y === undefined) {
        // default placement: beside/below the parent when available
        var p = parentOf[id];
        var px = (p && model.nodes[p]) ? (model.nodes[p].x || 80) : 80;
        var py = (p && model.nodes[p]) ? (model.nodes[p].y || 60) : 60;
        var slot = nextFreeY[p || ""] = (nextFreeY[p || ""] || 0) + 1;
        n.x = px + NODE_W + 90;
        n.y = py + (slot - 1) * (NODE_H + 26);
      }
    });
    model.edges = edges;
    model.meta = json.meta || {};
    model.nodes = incoming;

    // events feed (append new, cap)
    var known = {};
    json.events.forEach(function (ev) { known[ev.t + "|" + ev.message] = ev; });
    model.events = json.events;

    renderAll(added);
    if (keepSelected && model.nodes[keepSelected]) selectNode(keepSelected, true);
    updateConn(json);
    updateFilters();
  }

  function updateConn(json) {
    var c = $("conn");
    if (json.meta && json.meta.upmPort) {
      c.textContent = "UPM :" + json.meta.upmPort + " · " + Object.keys(model.nodes).length + " nodes";
      c.className = "conn ok";
    }
  }

  // ======================================================= rendering
  var nodeDoms = {};   // id -> group element
  var edgeDoms = {};   // id -> path element

  function removeNodeDom(id) {
    var g = nodeDoms[id];
    if (g && g.parentNode) g.parentNode.removeChild(g);
    delete nodeDoms[id];
    Object.keys(edgeDoms).forEach(function (eid) {
      var ed = model.edges.find(function (e) { return e.id === eid; });
      if (!ed) { var p = edgeDoms[eid]; if (p && p.parentNode) p.parentNode.removeChild(p); delete edgeDoms[eid]; }
    });
  }

  function renderAll(addedIds) {
    renderEdges();
    renderNodes();
    if (ui.autoLayout) autoLayout(addedIds && addedIds.length > 0);
    renderEvents();
    if (ui.mode === "overview") { renderOverview(); renderProfiles(); }
    applyFilters();
  }

  function renderEdges() {
    var g = $("edges");
    var seen = {};
    model.edges.forEach(function (e) {
      seen[e.id] = true;
      var p = edgeDoms[e.id];
      if (!p) {
        p = document.createElementNS(SVGNS, "path");
        p.setAttribute("class", "edge");
        p.addEventListener("click", function (ev) { ev.stopPropagation(); selectEdge(e); });
        g.appendChild(p);
        edgeDoms[e.id] = p;
      }
      p.dataset.type = e.type;
      p.dataset.status = e.status;
      p.classList.add(e.type, e.status);
    });
    Object.keys(edgeDoms).forEach(function (eid) {
      if (!seen[eid]) {
        var p = edgeDoms[eid];
        if (p.parentNode) p.parentNode.removeChild(p);
        delete edgeDoms[eid];
      }
    });
    layoutEdges();
  }

  function renderNodes() {
    var g = $("nodes");
    Object.keys(model.nodes).forEach(function (id) {
      var n = model.nodes[id];
      var grp = nodeDoms[id];
      if (!grp) {
        grp = buildNodeGroup(n);
        g.appendChild(grp);
        nodeDoms[id] = grp;
      }
      updateNodeGroup(grp, n);
      grp.dataset.id = id;
    });
  }

  function buildNodeGroup(n) {
    var g = document.createElementNS(SVGNS, "g");
    g.setAttribute("class", "node");
    var box = document.createElementNS(SVGNS, "rect");
    box.setAttribute("class", "box");
    box.setAttribute("width", NODE_W);
    box.setAttribute("height", NODE_H);
    var fill = document.createElementNS(SVGNS, "rect");
    fill.setAttribute("class", "fill");
    fill.setAttribute("width", NODE_W);
    fill.setAttribute("height", NODE_H);
    var dot = document.createElementNS(SVGNS, "circle");
    dot.setAttribute("class", "dot");
    dot.setAttribute("r", 4);
    var name = document.createElementNS(SVGNS, "text");
    name.setAttribute("class", "name");
    var sub = document.createElementNS(SVGNS, "text");
    sub.setAttribute("class", "sub");
    var tag = document.createElementNS(SVGNS, "text");
    tag.setAttribute("class", "type-tag");
    var toggle = document.createElementNS(SVGNS, "text");
    toggle.setAttribute("class", "toggle");

    g.appendChild(fill);
    g.appendChild(box);
    g.appendChild(dot);
    g.appendChild(name);
    g.appendChild(sub);
    g.appendChild(tag);
    g.appendChild(toggle);

    g.addEventListener("click", function (ev) {
      ev.stopPropagation();
      selectNode(n.id, false);
    });
    g.addEventListener("dblclick", function (ev) {
      ev.stopPropagation();
      toggleCollapse(n.id);
    });
    g.addEventListener("contextmenu", function (ev) {
      ev.preventDefault();
      ev.stopPropagation();
      showContextMenu(n, ev.clientX, ev.clientY);
    });
    g.addEventListener("pointerdown", function (ev) {
      if (ev.button !== 0) return;
      ev.stopPropagation();
      selectNode(n.id, false);
      startDrag(n.id, ev);
    });
    return g;
  }

  function updateNodeGroup(g, n) {
    var cls = (n.type || "").toLowerCase();
    g.classList.remove("root", "product", "service", "provider", "model", "hardware", "selected", "dim");
    g.classList.add(cls, "has-children");
    var box = g.querySelector(".box");
    box.classList.remove("running", "starting", "failed", "occupied", "ready", "stopped", "unknown");
    box.classList.add((n.status || "unknown").toLowerCase());
    var dot = g.querySelector(".dot");
    dot.classList.remove("running", "starting", "failed", "occupied", "ready", "stopped", "unknown");
    dot.classList.add((n.status || "unknown").toLowerCase());
    dot.setAttribute("cx", 14);
    dot.setAttribute("cy", NODE_H / 2);
    g.querySelector(".name").setAttribute("x", 24);
    g.querySelector(".name").setAttribute("y", 20);
    g.querySelector(".name").textContent = truncate(n.name || n.id, 30);
    g.querySelector(".sub").setAttribute("x", 24);
    g.querySelector(".sub").setAttribute("y", 34);
    g.querySelector(".sub").textContent = statusLine(n);
    g.querySelector(".type-tag").setAttribute("x", NODE_W - 6);
    g.querySelector(".type-tag").setAttribute("y", 12);
    g.querySelector(".type-tag").setAttribute("text-anchor", "end");
    g.querySelector(".type-tag").textContent = TYPE_TAGS[n.type] || n.type || "";
    var hasChildren = hasChildNodes(n.id);
    g.querySelector(".toggle").setAttribute("x", 8);
    g.querySelector(".toggle").setAttribute("y", NODE_H / 2 + 4);
    g.querySelector(".toggle").setAttribute("text-anchor", "middle");
    g.querySelector(".toggle").textContent = hasChildren ? (n.collapsed ? "\u25B8" : "\u25BE") : "";
    if (n.x !== undefined && n.y !== undefined) {
      g.setAttribute("transform", "translate(" + n.x + "," + n.y + ")");
    }
    if (ui.selectedId === n.id) g.classList.add("selected");
  }

  function statusLine(n) {
    var s = (n.statusDetail || n.status || "unknown").toLowerCase();
    if (n.port > 0) s += " · :" + n.port;
    if (n.metrics && n.metrics.utilizationPercent !== undefined && n.metrics.utilizationPercent >= 0) {
      s += " · " + Math.round(n.metrics.utilizationPercent) + "%";
    }
    return truncate(s, 42);
  }

  function layoutEdges() {
    model.edges.forEach(function (e) {
      var p = edgeDoms[e.id];
      if (!p) return;
      var s = nodeDoms[e.source], t = nodeDoms[e.target];
      if (!s || !t) { p.setAttribute("d", ""); return; }
      var sx = posOf(s), tx = posOf(t);
      var d = edgePath(sx, tx);
      p.setAttribute("d", d);
    });
  }

  function posOf(g) {
    var tr = g.getAttribute("transform");
    var m = tr ? /translate\(([-\d.]+),([-\d.]+)\)/.exec(tr) : null;
    return m ? { x: +m[1], y: +m[2] } : { x: 0, y: 0 };
  }

  function edgePath(s, t) {
    var sxc = s.x + NODE_W / 2, syc = s.y + NODE_H / 2;
    var txc = t.x + NODE_W / 2, tyc = t.y + NODE_H / 2;
    var mx = (sxc + txc) / 2;
    return "M" + sxc + "," + syc + " C" + mx + "," + syc + " " + mx + "," + tyc + " " + txc + "," + tyc;
  }

  function hasChildNodes(id) {
    for (var i = 0; i < model.edges.length; i++) {
      var e = model.edges[i];
      if (e.type === "HIERARCHICAL" && e.source === id && e.target !== id) return true;
    }
    return false;
  }

  // ======================================================= layout
  function autoLayout(fresh) {
    var byParent = {};
    Object.keys(model.nodes).forEach(function (id) {
      var n = model.nodes[id];
      var p = n.parentId || "";
      (byParent[p] = byParent[p] || []).push(id);
    });
    // depth assignment via BFS from root(s)
    var depth = {};
    var roots = Object.keys(model.nodes).filter(function (id) {
      var n = model.nodes[id];
      return !n.parentId || !model.nodes[n.parentId];
    });
    if (roots.length === 0) roots = [Object.keys(model.nodes)[0]];
    var queue = roots.slice();
    queue.forEach(function (r) { depth[r] = 0; });
    while (queue.length) {
      var cur = queue.shift();
      var children = visibleChildren(cur);
      children.forEach(function (c) {
        if (depth[c] === undefined) { depth[c] = depth[cur] + 1; queue.push(c); }
      });
    }
    var columns = {};
    Object.keys(depth).forEach(function (id) {
      (columns[depth[id]] = columns[depth[id]] || []).push(id);
    });
    var rootIsUpm = model.nodes["upm"];
    var colX = 80, gapY = NODE_H + 26, colGap = NODE_W + 90;
    Object.keys(columns).sort(function (a, b) { return a - b; }).forEach(function (dk) {
      var ids = columns[dk];
      var totalH = ids.length * gapY;
      var y = 60 + (700 - totalH) / 2;
      ids.forEach(function (id) {
        var n = model.nodes[id];
        if (fresh || !ui.manualPositions[id]) {
          n.x = colX;
          n.y = y;
          y += gapY;
        }
      });
      colX += colGap;
    });
    // reorder columns so root sits left; already does (depth 0 first)
    layoutEdges();
    renderNodes();
  }

  function visibleChildren(id) {
    var out = [];
    model.edges.forEach(function (e) {
      if (e.type === "HIERARCHICAL" && e.source === id) out.push(e.target);
    });
    return out;
  }

  // ======================================================= selection
  function selectNode(id, keepScroll) {
    ui.selectedId = id;
    Object.keys(nodeDoms).forEach(function (nid) {
      var g = nodeDoms[nid];
      if (g) g.classList.toggle("selected", nid === id);
    });
    highlightConnections(id);
    renderInspector(id);
    if (ui.followEvents) scrollToNode(id);
  }

  function selectEdge(e) {
    // highlight the edge and its endpoints
    highlightConnections(null, e.id);
    renderEdgeInspector(e);
  }

  function highlightConnections(nodeId, edgeId) {
    var connected = {};
    model.edges.forEach(function (e) {
      var match = (nodeId && (e.source === nodeId || e.target === nodeId)) || (edgeId && e.id === edgeId);
      var p = edgeDoms[e.id];
      if (p) {
        p.classList.toggle("hl", !!match);
        p.classList.toggle("dim", !!(nodeId && !match));
        if (edgeId === e.id) p.classList.add("hl");
      }
      if (match) { connected[e.source] = true; connected[e.target] = true; }
    });
    if (nodeId) {
      Object.keys(nodeDoms).forEach(function (nid) {
        var g = nodeDoms[nid];
        if (g) g.classList.toggle("dim", nid !== nodeId && !connected[nid]);
      });
    }
    if (edgeId) {
      var e = model.edges.find(function (x) { return x.id === edgeId; });
      if (e) {
        Object.keys(nodeDoms).forEach(function (nid) {
          var g = nodeDoms[nid];
          if (g) g.classList.toggle("dim", nid !== e.source && nid !== e.target);
        });
      }
    }
  }

  function clearHighlight() {
    ui.selectedId = null;
    model.edges.forEach(function (e) {
      var p = edgeDoms[e.id];
      if (p) p.classList.remove("hl", "dim");
    });
    Object.keys(nodeDoms).forEach(function (id) {
      var g = nodeDoms[id];
      if (g) g.classList.remove("dim", "selected");
    });
    $("inspector-empty").classList.remove("hidden");
    $("inspector-body").classList.add("hidden");
  }

  // ======================================================= inspector
  function renderInspector(id) {
    var n = model.nodes[id];
    if (!n) return;
    $("inspector-empty").classList.add("hidden");
    var body = $("inspector-body");
    body.classList.remove("hidden");
    var rows = [];
    rows.push(row("ID", n.id));
    rows.push(row("Product", n.productId || "—"));
    rows.push(row("Component", n.componentId || n.id));
    if (n.version) rows.push(row("Version", n.version));
    if (n.endpoint) rows.push(row("Endpoint", n.endpoint));
    if (n.port > 0) rows.push(row("Port", n.port));
    Object.keys(n.metadata || {}).forEach(function (k) {
      var v = n.metadata[k];
      if (v === "" || v === null || v === undefined) return;
      if (Array.isArray(v)) v = v.join(", ");
      rows.push(row(prettyKey(k), String(v)));
    });
    Object.keys(n.metrics || {}).forEach(function (k) {
      var v = n.metrics[k];
      if (v === undefined || v === null || v === -1) return;
      rows.push(row("metric:" + k, fmtMetric(k, v)));
    });
    // connections count
    var inN = 0, outN = 0;
    model.edges.forEach(function (e) {
      if (e.source === id) outN++;
      if (e.target === id) inN++;
    });
    rows.push(row("Connections", "in " + inN + " · out " + outN));

    var html = "<h2>" + escapeHtml(n.name) + "</h2>" +
      "<div class='node-type'>" + (TYPE_TAGS[n.type] || n.type) + "</div>" +
      "<div class='status-line'><span class='status-badge " + (n.status || "unknown").toLowerCase() + "'>" + n.status + "</span>" +
      (n.statusDetail ? " <span class='muted'>" + escapeHtml(n.statusDetail) + "</span>" : "") + "</div>";
    html += "<div class='ins-sec'><h3>General</h3>" + rows.slice(0, 10).join("") + "</div>";
    html += "<div class='ins-sec'><h3>Runtime</h3>" + rows.slice(10).join("") + "</div>";
    html += "<div class='ins-sec'><h3>Actions</h3><div class='ins-actions' id='ins-actions'>" + actionButtons(n) + "</div></div>";
    body.innerHTML = html;
    bindActionButtons(n);
  }

  function renderEdgeInspector(e) {
    $("inspector-empty").classList.add("hidden");
    var body = $("inspector-body");
    body.classList.remove("hidden");
    var s = model.nodes[e.source], t = model.nodes[e.target];
    body.innerHTML = "<h2>Connection</h2><div class='node-type'>" + e.type + "</div>" +
      "<div class='ins-sec'><h3>General</h3>" +
      row("Type", e.type) + row("Status", e.status) + row("Label", e.label || "—") +
      row("Source", s ? s.name : e.source) + row("Target", t ? t.name : e.target) +
      "</div>";
  }

  function prettyKey(k) {
    return k.replace(/([A-Z])/g, " $1").replace(/^./, function (c) { return c.toUpperCase(); });
  }

  function fmtMetric(k, v) {
    if (k === "utilizationPercent") return Math.round(v) + "%";
    if (k === "vramPercent") return v + "%";
    if (k === "sizeBytes") return v > 0 ? (v / 1048576).toFixed(0) + " MB" : "—";
    if (typeof v === "number" && k.toLowerCase().indexOf("gb") >= 0) return v + " GB";
    return String(v);
  }

  function row(k, v) {
    return "<div class='ins-row'><span class='ins-key'>" + escapeHtml(k) + "</span>" +
      "<span class='ins-val'>" + escapeHtml(String(v)) + "</span></div>";
  }

  function escapeHtml(s) {
    return String(s).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;").replace(/'/g, "&#39;");
  }

  function truncate(s, max) {
    s = String(s);
    return s.length > max ? s.substring(0, max - 1) + "\u2026" : s;
  }

  // ======================================================= actions
  function actionsFor(n) {
    var list = [];
    var t = n.type, st = n.status;
    if (t === "PRODUCT" || t === "ROOT" || t === "PROVIDER") {
      if (st === "STOPPED" || st === "FAILED") {
        list.push({ label: "Start", action: "start" });
        if (n.productId && n.productId !== "upm") list.push({ label: "Restart", action: "restart" });
      } else if (st === "RUNNING") {
        if (n.productId && n.productId !== "upm") {
          list.push({ label: "Stop", action: "stop", danger: true });
          list.push({ label: "Restart", action: "restart" });
        }
      } else if (st === "STARTING") {
        list.push({ label: "Stop", action: "stop", danger: true });
      }
    }
    if (n.endpoint && n.endpoint.indexOf("http") === 0) {
      list.push({ label: "Open in Web", action: "openWeb" });
    }
    if (n.productId && n.productId !== "upm") {
      list.push({ label: "Open Configuration", action: "openConfig" });
      list.push({ label: "Show Logs", action: "showLogs" });
    }
    list.push({ label: "Refresh Status", action: "refresh" });
    list.push({ label: "Show Dependencies", action: "showDeps" });
    list.push({ label: "Copy Identifier", action: "copyId" });
    return list;
  }

  function actionButtons(n) {
    var b = actionsFor(n).map(function (a) {
      return "<button data-act='" + a.action + "' class='" + (a.danger ? "danger" : "") + "'>" + a.label + "</button>";
    }).join("");
    return b + "<span class='progress' id='act-progress'></span>";
  }

  function bindActionButtons(n) {
    $("inspector-body").querySelectorAll("[data-act]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var act = btn.dataset.act;
        if (act === "copyId") {
          try { navigator.clipboard.writeText(n.id); } catch (e) { /* noop */ }
          flashProgress("copied " + n.id);
          return;
        }
        if (act === "showDeps") { highlightConnections(n.id); return; }
        executeAction(act, n.id, btn);
      });
    });
  }

  function executeAction(action, nodeId, btn) {
    var progress = $("act-progress");
    if (progress) progress.textContent = "executing…";
    if (btn) btn.disabled = true;
    var res = callJava(action, nodeId, "");
    if (progress) progress.textContent = res && res.ok ? "done" : ("error: " + (res && res.error || "?"));
    if (btn) btn.disabled = false;
    setTimeout(function () { if (progress) progress.textContent = ""; }, 4000);
  }

  function flashProgress(msg) {
    var progress = $("act-progress");
    if (progress) { progress.textContent = msg; setTimeout(function () { progress.textContent = ""; }, 2500); }
  }

  // ======================================================= context menu
  function showContextMenu(n, x, y) {
    var ctx = $("ctx");
    ctx.innerHTML = "";
    var title = document.createElement("div");
    title.textContent = n.name + " — " + n.status;
    title.style.color = "var(--muted)";
    title.style.cursor = "default";
    ctx.appendChild(title);
    actionsFor(n).forEach(function (a) {
      var item = document.createElement("div");
      item.textContent = a.label;
      item.addEventListener("click", function () {
        hideContextMenu();
        if (a.action === "copyId") {
          try { navigator.clipboard.writeText(n.id); } catch (e) { /* noop */ }
          return;
        }
        if (a.action === "showDeps") { selectNode(n.id, false); highlightConnections(n.id); return; }
        executeAction(a.action, n.id, null);
      });
      ctx.appendChild(item);
    });
    ctx.style.left = x + "px";
    ctx.style.top = y + "px";
    ctx.classList.remove("hidden");
  }

  function hideContextMenu() {
    $("ctx").classList.add("hidden");
  }

  // ======================================================= drag/pan/zoom
  var drag = null;

  function startDrag(nodeId, ev) {
    var g = nodeDoms[nodeId];
    var startX = ev.clientX, startY = ev.clientY;
    var orig = posOf(g);
    var moved = false;
    function move(e2) {
      var dx = (e2.clientX - startX) / ui.viewport.k;
      var dy = (e2.clientY - startY) / ui.viewport.k;
      if (Math.abs(dx) + Math.abs(dy) > 3) moved = true;
      var nx = orig.x + dx, ny = orig.y + dy;
      ui.manualPositions[nodeId] = { x: nx, y: ny };
      model.nodes[nodeId].x = nx;
      model.nodes[nodeId].y = ny;
      g.setAttribute("transform", "translate(" + nx + "," + ny + ")");
      layoutEdges();
    }
    function up() {
      document.removeEventListener("pointermove", move);
      document.removeEventListener("pointerup", up);
      if (!moved) return; // it was a click; nothing else to do
    }
    document.addEventListener("pointermove", move);
    document.addEventListener("pointerup", up);
  }

  function setupPanZoom() {
    var svg = $("graph");
    var panning = null;
    svg.addEventListener("pointerdown", function (ev) {
      if (ev.target !== svg && ev.target.id !== "nodes" && ev.target.id !== "edges") return;
      ev.preventDefault();
      panning = { x: ev.clientX, y: ev.clientY };
      svg.classList.add("panning");
      clearHighlight();
    });
    document.addEventListener("pointermove", function (ev) {
      if (!panning) return;
      var dx = ev.clientX - panning.x, dy = ev.clientY - panning.y;
      panning = { x: ev.clientX, y: ev.clientY };
      ui.viewport.x += dx;
      ui.viewport.y += dy;
      applyViewport();
    });
    document.addEventListener("pointerup", function () {
      panning = null;
      svg.classList.remove("panning");
    });
    svg.addEventListener("wheel", function (ev) {
      ev.preventDefault();
      var factor = ev.deltaY < 0 ? 1.12 : 1 / 1.12;
      var rect = svg.getBoundingClientRect();
      var px = ev.clientX - rect.left, py = ev.clientY - rect.top;
      var k2 = Math.min(3, Math.max(0.2, ui.viewport.k * factor));
      ui.viewport.x = px - (px - ui.viewport.x) * (k2 / ui.viewport.k);
      ui.viewport.y = py - (py - ui.viewport.y) * (k2 / ui.viewport.k);
      ui.viewport.k = k2;
      applyViewport();
    });
  }

  function applyViewport() {
    var g = $("viewport-group");
    if (!g) return;
    g.setAttribute("transform", "translate(" + ui.viewport.x + "," + ui.viewport.y + ") scale(" + ui.viewport.k + ")");
  }

  // ======================================================= collapse
  function toggleCollapse(id) {
    var n = model.nodes[id];
    n.collapsed = !n.collapsed;
    applyCollapsed();
    if (ui.autoLayout) autoLayout(false);
  }

  function applyCollapsed() {
    var hidden = {};
    function hideBranch(nid) {
      visibleChildren(nid).forEach(function (c) {
        hidden[c] = true;
        hideBranch(c);
      });
    }
    Object.keys(model.nodes).forEach(function (id) {
      if (model.nodes[id].collapsed) hideBranch(id);
    });
    Object.keys(nodeDoms).forEach(function (id) {
      var g = nodeDoms[id];
      if (g) g.style.display = hidden[id] ? "none" : "";
    });
    model.edges.forEach(function (e) {
      var p = edgeDoms[e.id];
      if (p) p.style.display = (hidden[e.source] || hidden[e.target]) ? "none" : "";
    });
    layoutEdges();
  }

  // ======================================================= filters
  function applyFilters() {
    var f = ui.filters, q = ui.search.toLowerCase();
    Object.keys(nodeDoms).forEach(function (id) {
      var n = model.nodes[id];
      var g = nodeDoms[id];
      if (!g) return;
      var ok = true;
      if (f.product && n.productId !== f.product && n.id !== "upm" && n.parentId !== "upm") ok = false;
      if (f.product === "upm" && n.productId !== "upm" && n.type !== "HARDWARE") ok = false;
      if (f.type && n.type !== f.type) ok = false;
      if (f.status && (n.status || "").toLowerCase() !== f.status.toLowerCase()) ok = false;
      if (q && (n.name + " " + n.id + " " + (n.productId || "")).toLowerCase().indexOf(q) < 0) ok = false;
      g.classList.toggle("dim", !ok);
    });
    // hide edges whose endpoints are dimmed
    model.edges.forEach(function (e) {
      var p = edgeDoms[e.id];
      if (!p) return;
      var s = nodeDoms[e.source], t = nodeDoms[e.target];
      var dim = (s && s.classList.contains("dim")) || (t && t.classList.contains("dim"));
      if (!dim) dim = (s && s.style.display === "none") || (t && t.style.display === "none");
      p.classList.toggle("dim", !!dim);
    });
  }

  function updateFilters() {
    var products = {};
    Object.keys(model.nodes).forEach(function (id) {
      var n = model.nodes[id];
      if (n.type === "PRODUCT") products[n.productId] = true;
    });
    var ps = $("filter-product");
    var cur = ps.value;
    ps.innerHTML = "<option value=''>All products</option><option value='upm'>UPM (root)</option>";
    Object.keys(products).sort().forEach(function (p) {
      var o = document.createElement("option");
      o.value = p;
      o.textContent = p;
      ps.appendChild(o);
    });
    ps.value = cur;
    var types = {};
    Object.keys(model.nodes).forEach(function (id) {
      types[model.nodes[id].type] = true;
    });
    var ts = $("filter-type");
    var curT = ts.value;
    ts.innerHTML = "<option value=''>All types</option>";
    Object.keys(types).sort().forEach(function (t) {
      var o = document.createElement("option");
      o.value = t;
      o.textContent = t;
      ts.appendChild(o);
    });
    ts.value = curT;
  }

  // ======================================================= events feed
  function renderEvents() {
    var list = $("event-list");
    list.innerHTML = "";
    model.events.slice(0, 60).forEach(function (ev) {
      var r = document.createElement("div");
      r.className = "event-row";
      var d = new Date(ev.t);
      r.innerHTML = "<span class='event-t'>" + d.toLocaleTimeString() + "</span>" +
        "<span class='event-k k-" + (ev.kind || "info") + "'>" + escapeHtml(ev.kind || "") + "</span>" +
        "<span class='event-m'>" + escapeHtml(ev.message || "") + "</span>";
      if (ev.nodeId) {
        r.addEventListener("click", function () {
          selectNode(ev.nodeId, true);
          if (ui.mode !== "graph") switchMode("graph");
        });
      }
      list.appendChild(r);
    });
  }

  function scrollToNode(id) {
    var g = nodeDoms[id];
    if (!g) return;
    var p = posOf(g);
    var svg = $("graph");
    var w = svg.clientWidth / 2, h = svg.clientHeight / 2;
    ui.viewport.x = w - p.x * ui.viewport.k;
    ui.viewport.y = h - p.y * ui.viewport.k;
    applyViewport();
  }

  // ======================================================= overview
  function renderOverview() {
    var box = $("overview-table");
    var rows = [];
    var products = [];
    Object.keys(model.nodes).forEach(function (id) {
      var n = model.nodes[id];
      if (n.type === "PRODUCT" || n.type === "PROVIDER" || n.type === "SERVICE") products.push(n);
    });
    var html = "<table class='tbl'><thead><tr><th>Component</th><th>Product</th><th>Type</th>" +
      "<th>Runtime Status</th><th>Activity</th><th>Endpoint</th><th>Actions</th></tr></thead><tbody>";
    products.forEach(function (n) {
      var acts = "";
      var t = n.type;
      if (t === "PRODUCT") {
        if (n.status === "STOPPED" || n.status === "FAILED") acts += "<button data-ov='start'>Start</button>";
        if (n.status === "RUNNING") acts += "<button data-ov='stop'>Stop</button> " + "<button data-ov='restart'>Restart</button>";
        if (n.status === "STARTING") acts += "<button data-ov='stop'>Stop</button>";
      }
      if (n.endpoint && n.endpoint.indexOf("http") === 0) acts += "<button data-ov='openWeb'>Open</button>";
      acts += "<button data-ov='inspect'>Inspect</button>";
      html += "<tr><td>" + escapeHtml(n.name) + "</td><td>" + escapeHtml(n.productId || "—") + "</td>" +
        "<td>" + (TYPE_TAGS[n.type] || n.type) + "</td>" +
        "<td><span class='status-badge " + (n.status || "").toLowerCase() + "'>" + n.status + "</span></td>" +
        "<td class='muted'>" + (n.statusDetail ? escapeHtml(truncate(n.statusDetail, 40)) : "—") + "</td>" +
        "<td class='mono'>" + escapeHtml(n.endpoint || "—") + "</td>" +
        "<td data-id='" + n.id + "'>" + acts + "</td></tr>";
    });
    html += "</tbody></table>";
    box.innerHTML = html;
    box.querySelectorAll("[data-ov]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var id = btn.closest("td[data-id]").dataset.id;
        var act = btn.dataset.ov;
        if (act === "inspect") { switchMode("graph"); selectNode(id, true); return; }
        executeAction(act, id, btn);
      });
    });
  }

  function renderProfiles() {
    var box = $("profile-table");
    var profiles = (model.meta && model.meta.profiles) || [];
    if (!profiles.length) {
      box.innerHTML = "<div class='muted'>No launch profiles defined in UPM.</div>";
      return;
    }
    var html = "<table class='tbl'><thead><tr><th>Profile</th><th>Products (startup order)</th><th>Actions</th></tr></thead><tbody>";
    profiles.forEach(function (p) {
      var order = (p.startupOrder || p.projects || []).join(" → ");
      html += "<tr><td><b>" + escapeHtml(p.name) + "</b><div class='mono muted'>" + escapeHtml(p.id) + "</div></td>" +
        "<td class='mono'>" + escapeHtml(order) + "</td>" +
        "<td><button data-prof='" + escapeHtml(p.id) + "' data-act='launch'>Launch</button> " +
        "<button data-prof='" + escapeHtml(p.id) + "' data-act='stop'>Stop</button></td></tr>";
    });
    html += "</tbody></table>";
    box.innerHTML = html;
    box.querySelectorAll("[data-prof]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var prof = btn.dataset.prof;
        var res = callJava(btn.dataset.act === "launch" ? "profileLaunch" : "profileStop", "", prof);
        flashProgress((res && res.ok) ? "profile op submitted" : ("error: " + (res && res.error || "?")));
      });
    });
  }

  // ======================================================= toolbar
  function switchMode(mode) {
    ui.mode = mode;
    $("btn-mode").textContent = mode === "graph" ? "Overview" : "Graph";
    $("graph-wrap").classList.toggle("hidden", mode !== "graph");
    $("overview-wrap").classList.toggle("hidden", mode !== "overview");
    $("inspector").classList.toggle("hidden", mode !== "graph");
    if (mode === "overview") { renderOverview(); renderProfiles(); }
  }

  function setupToolbar() {
    $("btn-mode").addEventListener("click", function () { switchMode(ui.mode === "graph" ? "overview" : "graph"); });
    $("btn-fit").addEventListener("click", fitToScreen);
    $("btn-layout").addEventListener("click", function () {
      ui.autoLayout = !ui.autoLayout;
      $("btn-layout").classList.toggle("active", ui.autoLayout);
      if (ui.autoLayout) autoLayout(false);
    });
    $("btn-refresh").addEventListener("click", function () {
      callJava("refresh", "", "");
    });
    $("search").addEventListener("input", function () {
      ui.search = this.value;
      applyFilters();
      if (ui.search) focusFirstMatch();
    });
    $("filter-product").addEventListener("change", function () { ui.filters.product = this.value; applyFilters(); });
    $("filter-type").addEventListener("change", function () { ui.filters.type = this.value; applyFilters(); });
    $("filter-status").addEventListener("change", function () { ui.filters.status = this.value; applyFilters(); });
    $("ev-follow").addEventListener("change", function () { ui.followEvents = this.checked; });
    document.addEventListener("click", hideContextMenu);
    document.addEventListener("contextmenu", function (ev) {
      if (!ev.target.closest || !ev.target.closest(".node")) hideContextMenu();
    });
    $("btn-layout").classList.add("active");
  }

  function focusFirstMatch() {
    var q = ui.search.toLowerCase();
    for (var id in model.nodes) {
      var n = model.nodes[id];
      if ((n.name + " " + n.id).toLowerCase().indexOf(q) >= 0) {
        selectNode(id, true);
        scrollToNode(id);
        return;
      }
    }
  }

  function fitToScreen() {
    var minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9, count = 0;
    Object.keys(nodeDoms).forEach(function (id) {
      var p = posOf(nodeDoms[id]);
      minX = Math.min(minX, p.x); minY = Math.min(minY, p.y);
      maxX = Math.max(maxX, p.x + NODE_W); maxY = Math.max(maxY, p.y + NODE_H);
      count++;
    });
    if (!count) return;
    var svg = $("graph");
    var w = svg.clientWidth - 80, h = svg.clientHeight - 80;
    var bw = Math.max(1, maxX - minX), bh = Math.max(1, maxY - minY);
    var k = Math.min(2, Math.min(w / bw, h / bh));
    ui.viewport.k = k;
    ui.viewport.x = 40 - minX * k;
    ui.viewport.y = 40 - minY * k;
    applyViewport();
  }

  // ======================================================= init
  function init() {
    // wrap edge/node groups in a viewport group
    var svg = $("graph");
    var vg = document.createElementNS(SVGNS, "g");
    vg.id = "viewport-group";
    var edges = $("edges"), nodes = $("nodes");
    svg.insertBefore(vg, edges);
    vg.appendChild(edges);
    vg.appendChild(nodes);
    applyViewport();
    setupPanZoom();
    setupToolbar();
    ready();
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
