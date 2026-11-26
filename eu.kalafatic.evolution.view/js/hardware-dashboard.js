/* EVO Hardware Monitor — dashboard client.
 *
 * Receives incremental samples from the Java side through
 * `window.evoHw.push(json)` (never reloads the page), keeps bounded ring
 * buffers and renders professional canvas charts with axes, labels, legends,
 * units and timestamps. Missing metrics (-1 / undefined) render as gaps.
 */
(function () {
  "use strict";

  var HISTORY = 240;

  /* ---------------------------------------------------------------- */
  /* Formatting helpers                                                */
  /* ---------------------------------------------------------------- */
  function fmtBytes(bytes) {
    if (bytes === undefined || bytes === null || bytes < 0) return "N/A";
    var gib = bytes / (1024 * 1024 * 1024);
    if (gib >= 1024) return (gib / 1024).toFixed(2) + " TiB";
    return gib.toFixed(1) + " GiB";
  }
  function fmtNum(v, digits) {
    if (v === undefined || v === null || v < 0) return "N/A";
    return Number(v).toFixed(digits === undefined ? 1 : digits);
  }
  function fmtTime(ts) {
    if (!ts) return "--:--:--";
    var d = new Date(ts);
    function p(n) { return (n < 10 ? "0" : "") + n; }
    return p(d.getHours()) + ":" + p(d.getMinutes()) + ":" + p(d.getSeconds());
  }
  function niceMax(v) {
    if (!isFinite(v) || v <= 0) return 10;
    if (v <= 10) return 10;
    var mag = Math.pow(10, Math.floor(Math.log10(v)));
    var n = v / mag;
    var f = n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10;
    return f * mag;
  }

  /* ---------------------------------------------------------------- */
  /* Canvas chart                                                      */
  /* ---------------------------------------------------------------- */
  function Chart(canvas, title, unit, series) {
    this.canvas = canvas;
    this.title = title;
    this.unit = unit;
    this.series = series; // [{key,label,color}]
    this.data = {};       // key -> [{t,v}]
    var self = this;
    series.forEach(function (s) { self.data[s.key] = []; });
  }

  Chart.prototype.push = function (t, values) {
    var self = this;
    this.series.forEach(function (s) {
      var v = values[s.key];
      var ok = (v !== undefined && v !== null && v >= 0);
      self.data[s.key].push({ t: t, v: ok ? v : null });
      if (self.data[s.key].length > HISTORY) self.data[s.key].shift();
    });
    this.render();
  };

  Chart.prototype.render = function () {
    var canvas = this.canvas;
    if (!canvas || canvas.clientWidth === 0) return;
    var dpr = window.devicePixelRatio || 1;
    var w = canvas.clientWidth, h = canvas.clientHeight;
    if (canvas.width !== w * dpr || canvas.height !== h * dpr) {
      canvas.width = w * dpr;
      canvas.height = h * dpr;
    }
    var ctx = canvas.getContext("2d");
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);

    var padL = 44, padR = 10, padT = 8, padB = 20;
    var plotW = w - padL - padR, plotH = h - padT - padB;
    if (plotW <= 10 || plotH <= 10) return;

    // Compute global y-scale across all series.
    var maxV = 0;
    var tMin = Infinity, tMax = -Infinity;
    var self = this;
    this.series.forEach(function (s) {
      self.data[s.key].forEach(function (p) {
        if (p.v !== null && p.v > maxV) maxV = p.v;
        if (p.t < tMin) tMin = p.t;
        if (p.t > tMax) tMax = p.t;
      });
    });
    var yMax = niceMax(maxV || 0);

    // Grid + y labels (5 ticks).
    ctx.font = "10px " + getComputedStyle(document.body).fontFamily;
    ctx.strokeStyle = "rgba(139,158,190,0.16)";
    ctx.fillStyle = "#8a9bb5";
    ctx.textAlign = "right";
    ctx.textBaseline = "middle";
    var ticks = 5;
    for (var i = 0; i <= ticks; i++) {
      var yv = yMax * i / ticks;
      var y = padT + plotH - (plotH * i / ticks);
      ctx.beginPath();
      ctx.moveTo(padL, y);
      ctx.lineTo(padL + plotW, y);
      ctx.stroke();
      ctx.fillText(this.formatTick(yv), padL - 6, y);
    }

    // X time labels (min / mid / max).
    ctx.textAlign = "center";
    ctx.textBaseline = "top";
    if (isFinite(tMin) && isFinite(tMax)) {
      var xs = [padL, padL + plotW / 2, padL + plotW];
      var ts = [tMin, (tMin + tMax) / 2, tMax];
      for (var k = 0; k < xs.length; k++) {
        ctx.fillText(fmtTime(ts[k]), xs[k], padT + plotH + 4);
      }
    }

    // Lines with gaps for missing values.
    ctx.lineWidth = 1.4;
    this.series.forEach(function (s) {
      var pts = self.data[s.key];
      if (pts.length < 2) return;
      var span = Math.max(1, tMax - tMin);
      ctx.strokeStyle = s.color;
      ctx.beginPath();
      var drawing = false;
      for (var i = 0; i < pts.length; i++) {
        var p = pts[i];
        var x = padL + plotW * ((p.t - tMin) / span);
        var y = padT + plotH - (plotH * Math.min(1, (p.v === null ? 0 : p.v) / yMax));
        if (p.v === null) {
          drawing = false;
          continue;
        }
        if (!drawing) { ctx.moveTo(x, y); drawing = true; }
        else ctx.lineTo(x, y);
      }
      ctx.stroke();
    });

    // Legend.
    ctx.textAlign = "left";
    ctx.textBaseline = "middle";
    var lx = padL;
    this.series.forEach(function (s) {
      ctx.fillStyle = s.color;
      ctx.fillRect(lx, 2, 10, 3);
      ctx.fillStyle = "#8a9bb5";
      ctx.fillText(s.label + " (" + self.unit + ")", lx + 13, 4);
      lx += 13 + ctx.measureText(s.label + " (" + self.unit + ")").width + 16;
    });
  };

  Chart.prototype.formatTick = function (v) {
    if (v >= 1000) return (v / 1000).toFixed(1) + "k";
    if (v >= 100) return String(Math.round(v));
    return Number(v).toFixed(1);
  };

  Chart.prototype.clear = function () {
    var self = this;
    this.series.forEach(function (s) { self.data[s.key] = []; });
    this.render();
  };

  /* ---------------------------------------------------------------- */
  /* Fixed charts                                                      */
  /* ---------------------------------------------------------------- */
  var C = {
    cpuUtil:  new Chart(document.getElementById("chart-cpu-util"),  "CPU Utilization", "%",  [{ key: "util", label: "CPU", color: "#4c8dff" }]),
    cpuFreq:  new Chart(document.getElementById("chart-cpu-freq"),  "CPU Frequency",   "MHz",[{ key: "freq", label: "Freq", color: "#e6a23c" }]),
    cpuTemp:  new Chart(document.getElementById("chart-cpu-temp"),  "CPU Temperature", "°C", [{ key: "temp", label: "Temp", color: "#e05c5c" }]),
    cpuLoad:  new Chart(document.getElementById("chart-cpu-load"),  "Load Average",    "",   [{ key: "load", label: "Load", color: "#9b59b6" }]),
    ramUtil:  new Chart(document.getElementById("chart-ram-util"),  "Physical RAM",    "%",  [{ key: "util", label: "RAM", color: "#2ecc71" }]),
    ramUsed:  new Chart(document.getElementById("chart-ram-used"),  "RAM Used/Avail",  "GiB",[
      { key: "used", label: "Used", color: "#4c8dff" },
      { key: "available", label: "Avail", color: "#8a9bb5" }]),
    jvmUtil:  new Chart(document.getElementById("chart-jvm-util"),  "JVM Heap",        "%",  [{ key: "util", label: "Heap", color: "#3aa0d8" }]),
    jvmUsed:  new Chart(document.getElementById("chart-jvm-used"),  "JVM Heap Used",   "GiB",[{ key: "heap", label: "Used", color: "#9b59b6" }])
  };

  var gpuCharts = {}; // index -> {charts...}

  /* ---------------------------------------------------------------- */
  /* Metric cards                                                      */
  /* ---------------------------------------------------------------- */
  function el(id) { return document.getElementById(id); }

  function card(label, value, sub, tone) {
    return '<div class="hw-card ' + (tone || "") + '">' +
      '<div class="hw-card-label">' + label + '</div>' +
      '<div class="hw-card-value">' + value + '</div>' +
      (sub ? '<div class="hw-card-sub">' + sub + '</div>' : '') +
      '</div>';
  }

  function renderCpuCards(s) {
    var c = s.cpu;
    var html = "";
    html += card("Utilization", fmtNum(c.util) + " %", "logical: " + c.logicalCores, "");
    html += card("Frequency", c.freq > 0 ? fmtNum(c.freq, 0) + " MHz" : "N/A", "max: " + (c.maxFreq > 0 ? fmtNum(c.maxFreq, 0) + " MHz" : "N/A"), "");
    html += card("Temperature", c.temp > 0 ? fmtNum(c.temp) + " °C" : "N/A", "", c.temp > 85 ? "err" : c.temp > 70 ? "warn" : "");
    html += card("Load (1 min)", c.load > 0 ? fmtNum(c.load, 2) : "N/A", "load average", "");
    html += card("Cores", (c.physicalCores > 0 ? c.physicalCores : "?") + " phys / " + c.logicalCores + " logical", "physical / logical", "");
    html += card("Processor", String(c.model || "Unknown"), (c.vendor || "") + " · " + (c.arch || ""), "");
    el("hw-cpu-cards").innerHTML = html;
  }

  function renderRamCards(s) {
    var r = s.ram, j = s.jvm;
    var html = "";
    html += card("Physical Used", fmtBytes(r.used), fmtBytes(r.total) + " total", r.util > 90 ? "err" : r.util > 75 ? "warn" : "");
    html += card("Physical Available", fmtBytes(r.available), "available to apps", "");
    html += card("Physical Free", fmtBytes(r.free), "unused", "");
    html += card("RAM Utilization", fmtNum(r.util) + " %", "of physical memory", "");
    html += card("JVM Heap", fmtBytes(j.heap), "max " + fmtBytes(j.max), j.util > 90 ? "err" : "");
    html += card("JVM Non-Heap", fmtBytes(j.nonHeap), "metaspace + code", "");
    el("hw-ram-cards").innerHTML = html;
  }

  /* ---------------------------------------------------------------- */
  /* GPU blocks (built dynamically, one per device)                    */
  /* ---------------------------------------------------------------- */
  function ensureGpuBlock(gpu) {
    if (gpuCharts[gpu.index]) return gpuCharts[gpu.index];
    var container = el("hw-gpu-container");
    var block = document.createElement("div");
    block.className = "hw-gpu-block";
    block.innerHTML =
      '<div class="hw-gpu-title"><span class="hw-gpu-idx">GPU ' + gpu.index + '</span> — ' +
      escapeHtml(gpu.name || "Unknown") + '</div>' +
      '<div class="hw-cards" id="hw-gpu-' + gpu.index + '-cards"></div>' +
      '<div class="hw-charts">' +
      chartBox("GPU Utilization", "%", "chart-gpu-" + gpu.index + "-util") +
      chartBox("VRAM Utilization", "%", "chart-gpu-" + gpu.index + "-vram") +
      chartBox("GPU Temperature", "°C", "chart-gpu-" + gpu.index + "-temp") +
      chartBox("Power Draw", "W", "chart-gpu-" + gpu.index + "-power") +
      chartBox("Memory Controller", "%", "chart-gpu-" + gpu.index + "-memctrl") +
      "</div>";
    container.appendChild(block);
    var charts = {
      util:    new Chart(el("chart-gpu-" + gpu.index + "-util"),    "GPU Utilization",  "%",  [{ key: "util", label: "GPU", color: "#4c8dff" }]),
      vram:    new Chart(el("chart-gpu-" + gpu.index + "-vram"),    "VRAM Utilization", "%",  [{ key: "vramUtil", label: "VRAM", color: "#2ecc71" }]),
      temp:    new Chart(el("chart-gpu-" + gpu.index + "-temp"),    "GPU Temperature",  "°C", [{ key: "temp", label: "Temp", color: "#e05c5c" }]),
      power:   new Chart(el("chart-gpu-" + gpu.index + "-power"),   "Power Draw",       "W",  [{ key: "power", label: "Power", color: "#e6a23c" }]),
      memctrl: new Chart(el("chart-gpu-" + gpu.index + "-memctrl"), "Memory Controller","%",  [{ key: "memCtrl", label: "MemCtrl", color: "#9b59b6" }])
    };
    var entry = { charts: charts, cards: el("hw-gpu-" + gpu.index + "-cards") };
    gpuCharts[gpu.index] = entry;
    return entry;
  }

  function chartBox(title, unit, id) {
    return '<div class="hw-chart-box"><div class="hw-chart-head">' + title +
      '<span class="hw-unit">' + unit + '</span></div><canvas id="' + id + '"></canvas></div>';
  }

  function renderGpuCards(gpu, entry) {
    var html = "";
    html += card("Utilization", fmtNum(gpu.util) + " %", "GPU compute", "");
    html += card("Memory Controller", fmtNum(gpu.memCtrl) + " %", "mem bandwidth", "");
    html += card("VRAM", fmtBytes(gpu.vramUsed) + " / " + fmtBytes(gpu.vramTotal),
      "used of total", gpu.vramUtil > 90 ? "err" : gpu.vramUtil > 75 ? "warn" : "");
    html += card("Temperature", gpu.temp > 0 ? fmtNum(gpu.temp) + " °C" : "N/A", "",
      gpu.temp > 85 ? "err" : gpu.temp > 70 ? "warn" : "");
    html += card("Power", gpu.power > 0 ? fmtNum(gpu.power) + " W" : "N/A",
      "limit " + (gpu.powerLimit > 0 ? fmtNum(gpu.powerLimit, 0) + " W" : "N/A"), "");
    html += card("SM Clock", gpu.smClock > 0 ? gpu.smClock + " MHz" : "N/A", "current clock", "");
    html += card("Driver", String(gpu.driver || "N/A"), "state " + (gpu.state || "N/A"), "");
    entry.cards.innerHTML = html;
  }

  function escapeHtml(s) {
    return String(s).replace(/[&<>"']/g, function (c) {
      return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
    });
  }

  /* ---------------------------------------------------------------- */
  /* Status chips                                                      */
  /* ---------------------------------------------------------------- */
  function setChip(id, dotClass, text) {
    var chip = el(id);
    if (!chip) return;
    chip.innerHTML = '<span class="hw-dot ' + dotClass + '"></span><span>' + text + '</span>';
  }

  /* ---------------------------------------------------------------- */
  /* Java → JS entry points                                            */
  /* ---------------------------------------------------------------- */
  var sampleCount = 0;

  window.evoHw = {
    /** One live sample (JSON string or object). */
    push: function (payload) {
      try {
        var s = typeof payload === "string" ? JSON.parse(payload) : payload;
        if (!s) return;
        var t = s.ts || Date.now();

        C.cpuUtil.push(t, { util: s.cpu.util });
        C.cpuFreq.push(t, { freq: s.cpu.freq });
        C.cpuTemp.push(t, { temp: s.cpu.temp });
        C.cpuLoad.push(t, { load: s.cpu.load });
        C.ramUtil.push(t, { util: s.ram.util });
        C.ramUsed.push(t, {
          used: s.ram.used / (1024 * 1024 * 1024),
          available: s.ram.available / (1024 * 1024 * 1024)
        });
        C.jvmUtil.push(t, { util: s.jvm.util });
        C.jvmUsed.push(t, { heap: s.jvm.heap / (1024 * 1024 * 1024) });

        renderCpuCards(s);
        renderRamCards(s);

        (s.gpus || []).forEach(function (gpu) {
          var entry = ensureGpuBlock(gpu);
          entry.charts.util.push(t, { util: gpu.util });
          entry.charts.vram.push(t, { vramUtil: gpu.vramUtil });
          entry.charts.temp.push(t, { temp: gpu.temp });
          entry.charts.power.push(t, { power: gpu.power });
          entry.charts.memctrl.push(t, { memCtrl: gpu.memCtrl });
          renderGpuCards(gpu, entry);
        });

        sampleCount++;
        el("hw-chip-time").innerHTML = "<span>Last update: " + fmtTime(ts) + "</span>";
        el("hw-chip-time").innerHTML = '<span>Last update: ' + fmtTime(ts) + ' · samples: ' + sampleCount + "</span>";
      } catch (e) {
        console.log("[evoHw] push error: " + e.message);
      }
    },

    /** Hardware scan result (JSON string or object). */
    setScan: function (payload) {
      try {
        var r = typeof payload === "string" ? JSON.parse(payload) : payload;
        if (!r) return;
        setChip("hw-chip-scan", r.status === "SUCCESS" ? "on" : "err",
          "Scan: " + r.status + " (" + r.durationMs + " ms)");
        var cuda = r.cudaStatus || "UNKNOWN";
        var cudaDot = cuda === "DETECTED" ? "on" : cuda === "UNSUPPORTED_JVM" || cuda === "BACKEND_UNAVAILABLE" ? "err" : "";
        setChip("hw-chip-cuda", cudaDot, "CUDA: " + cuda);

        if (r.cpu) {
          renderCpuCards({ cpu: r.cpu, ram: r.ram, jvm: r.jvm });
        }
        if (r.ram && r.jvm) {
          renderRamCards({ ram: r.ram, jvm: r.jvm });
        }
        if (r.gpus && r.gpus.length > 0) {
          r.gpus.forEach(function (g) {
            var entry = ensureGpuBlock(g);
            renderGpuCards(g, entry);
          });
        }
        var footer = el("hw-footer");
        var notes = (r.notes || []).map(escapeHtml).join(" · ");
        footer.innerHTML = "Scan " + r.status + " · started " + (r.startedAt || "") +
          " · finished " + (r.finishedAt || "") +
          (notes ? "<br>Notes: " + notes : "");
      } catch (e) {
        console.log("[evoHw] setScan error: " + e.message);
      }
    },

    /** Monitoring / diagnostic status update. */
    setStatus: function (payload) {
      try {
        var st = typeof payload === "string" ? JSON.parse(payload) : payload;
        if (!st) return;
        if (st.monitoring !== undefined) {
          setChip("hw-chip-monitoring", st.monitoring ? "on" : "",
            "Monitoring: " + (st.monitoring ? "RUNNING (" + st.intervalMs + " ms)" : "STOPPED"));
        }
        if (st.cuda !== undefined) {
          var dot = st.cuda === "PASS" ? "on" : st.cuda === "RUNNING" ? "busy" : st.cuda === "FAILED" ? "err" : "";
          setChip("hw-chip-cuda", dot, "CUDA: " + st.cuda);
        }
      } catch (e) {
        console.log("[evoHw] setStatus error: " + e.message);
      }
    },

    clear: function () {
      Object.keys(C).forEach(function (k) { C[k].clear(); });
      Object.keys(gpuCharts).forEach(function (k) {
        var entry = gpuCharts[k];
        Object.keys(entry.charts).forEach(function (ck) { entry.charts[ck].clear(); });
      });
      sampleCount = 0;
    }
  };

  /* ---------------------------------------------------------------- */
  /* Actions (JS → Java bridge) + ready handshake                       */
  /* ---------------------------------------------------------------- */
  function callJava(action) {
    try { evoHwAction(action); } catch (e) { console.log("[evoHw] bridge unavailable: " + e.message); }
  }
  el("hw-btn-scan").addEventListener("click", function () { callJava("scan"); });
  el("hw-btn-cuda").addEventListener("click", function () { callJava("test-cuda"); });
  el("hw-btn-refresh").addEventListener("click", function () { callJava("refresh"); });

  window.addEventListener("resize", function () {
    Object.keys(C).forEach(function (k) { C[k].render(); });
    Object.keys(gpuCharts).forEach(function (k) {
      var entry = gpuCharts[k];
      Object.keys(entry.charts).forEach(function (ck) { entry.charts[ck].render(); });
    });
  });

  try { evoHwReady("ready"); } catch (e) { /* SWT bridge not installed yet */ }
})();
