/* ENR Web Control Center — dependency-free status UI. Talks to the ENR API over api_base_url. */
"use strict";

let API_BASE = "";
const WEB_BASE = (location.origin || "http://127.0.0.1") + "";

const $ = (sel) => document.querySelector(sel);

function setText(sel, value) {
  const el = $(sel);
  if (el) el.textContent = value;
}

function setClass(sel, cls) {
  const el = $(sel);
  if (el) el.className = "value " + cls;
}

function statusClass(ok) {
  return ok ? "ok" : "bad";
}

async function fetchJson(url, timeoutMs) {
  const controller = new AbortController();
  const t = setTimeout(() => controller.abort(), timeoutMs || 4000);
  try {
    const res = await fetch(url, { signal: controller.signal });
    if (!res.ok) throw new Error("HTTP " + res.status);
    return await res.json();
  } finally {
    clearTimeout(t);
  }
}

async function refresh() {
  // Web-side bootstrap: read api_base_url and web endpoint from the web listener itself.
  try {
    const info = await fetchJson("/api/wcc/info", 4000);
    if (info.api_base_url) API_BASE = info.api_base_url;
    setText("web-endpoint", info.web_base_url || WEB_BASE);
    setText("web-api-base", info.api_base_url || "—");
    setText("footer-api", info.api_base_url || "—");
  } catch (e) {
    setText("web-endpoint", WEB_BASE);
  }
  setText("web-status", "UP");
  setClass("web-status", "ok");

  // ENR API: health, runtime, providers, models, capabilities.
  let apiOk = false;
  try {
    const health = await fetchJson(API_BASE + "/enr/v1/health", 5000);
    setText("rt-status", health.status || "—");
    setClass("rt-status", health.status === "UP" ? "ok" : "bad");
    setText("rt-version", health.version || "—");
    setText("rt-ready", String(health.ready));
    setClass("rt-ready", health.ready ? "ok" : "bad");
    setText("api-endpoint", API_BASE);
    setText("api-health", health.status || "—");
    setClass("api-health", health.status === "UP" ? "ok" : "bad");
    apiOk = true;
    setConn(true);
  } catch (e) {
    setText("rt-status", "UNREACHABLE");
    setClass("rt-status", "bad");
    setText("rt-ready", "—");
    setText("api-endpoint", API_BASE || "—");
    setText("api-health", "UNREACHABLE");
    setClass("api-health", "bad");
    setConn(false);
  }

  try {
    const runtime = await fetchJson(API_BASE + "/enr/v1/runtime", 5000);
    const engines = Array.isArray(runtime.activeEngines) ? runtime.activeEngines.join(", ") : "—";
    setText("api-engines", engines || "—");
  } catch (e) {
    setText("api-engines", "—");
  }

  try {
    const providers = await fetchJson(API_BASE + "/enr/v1/providers", 5000);
    const byId = {};
    for (const p of providers || []) byId[p.id] = p;
    renderProvider("prov-ollama", byId["ollama"]);
    renderProvider("prov-native", byId["evo-native"]);
    renderProvider("prov-llama", byId["llama-cpp"]);
  } catch (e) {
    renderProvider("prov-ollama", null);
    renderProvider("prov-native", null);
    renderProvider("prov-llama", null);
  }

  try {
    const models = await fetchJson(API_BASE + "/enr/v1/models", 8000);
    renderModels(models || []);
  } catch (e) {
    renderModels([]);
  }

  try {
    const caps = await fetchJson(API_BASE + "/enr/v1/capabilities", 5000);
    const list = $("#capabilities");
    if (list) {
      list.textContent = "";
      for (const c of caps || []) {
        const li = document.createElement("li");
        li.textContent = c.name + (c.description ? " — " + c.description : "");
        list.appendChild(li);
      }
    }
  } catch (e) { /* capabilities unavailable */ }
}

function renderProvider(sel, provider) {
  if (!provider) {
    setText(sel, "unknown");
    setClass(sel, "unknown");
    return;
  }
  setText(sel, provider.available ? "available" : "unavailable");
  setClass(sel, provider.available ? "ok" : "bad");
}

function renderModels(models) {
  const ollama = models.filter((m) => m.provider === "ollama");
  const native = models.filter((m) => m.provider === "evo-native");
  const llama = models.filter((m) => m.provider === "llama-cpp");
  setText("model-count", String(models.length));
  setText("count-ollama", String(ollama.length));
  setText("count-native", String(native.length));
  setText("count-llama", String(llama.length));
  fillList("models-ollama", ollama);
  fillList("models-native", native);
  fillList("models-llama", llama);
}

function fillList(sel, items) {
  const list = $(sel);
  if (!list) return;
  list.textContent = "";
  if (items.length === 0) {
    const li = document.createElement("li");
    li.className = "empty";
    li.textContent = "none";
    list.appendChild(li);
    return;
  }
  const shown = items.slice(0, 200);
  for (const m of shown) {
    const li = document.createElement("li");
    li.textContent = m.name || m.id;
    li.title = m.id;
    list.appendChild(li);
  }
  if (items.length > shown.length) {
    const li = document.createElement("li");
    li.className = "empty";
    li.textContent = "… " + (items.length - shown.length) + " more";
    list.appendChild(li);
  }
}

function setConn(ok) {
  const dot = $("#conn-dot");
  const txt = $("#conn-text");
  if (dot) dot.className = "dot " + (ok ? "ok" : "bad");
  if (txt) txt.textContent = ok ? "API connected" : "API unreachable";
}

$("#refresh-btn")?.addEventListener("click", refresh);
refresh();
setInterval(refresh, 5000);
