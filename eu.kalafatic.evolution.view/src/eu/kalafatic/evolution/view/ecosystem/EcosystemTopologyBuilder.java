package eu.kalafatic.evolution.view.ecosystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Builds an {@link EcosystemSnapshot} from the authoritative UPM HTTP state
 * ({@code /api/state}, {@code /api/ports/inventory}, {@code /api/metrics/current},
 * {@code /api/enr/models}, {@code /api/enr/inference}).
 *
 * <p>This class is pure: no network, no SWT, no global state. Every input is
 * parsed defensively — unexpected or missing fields degrade to
 * {@code UNKNOWN}/{@code N/A} instead of throwing, so a partial UPM response
 * never breaks the graph.</p>
 *
 * <p>UPM is always the ROOT node; registered UPM projects become PRODUCT
 * nodes; the family port inventory becomes SERVICE nodes; ENR models,
 * providers and inference sessions become PROVIDER/MODEL/INSTANCE/SESSION
 * nodes; UPM system metrics become HARDWARE nodes.</p>
 */
public final class EcosystemTopologyBuilder {

    public static final String ROOT_ID = "upm";

    private EcosystemTopologyBuilder() {
    }

    /**
     * Builds a snapshot from the four UPM payloads (any may be null/empty).
     *
     * @param state   {@code /api/state} JSON object
     * @param ports   {@code /api/ports/inventory} JSON object
     * @param metrics {@code /api/metrics/current} JSON object
     * @param enrModels {@code /api/enr/models} JSON object
     * @param enrInference {@code /api/enr/inference} JSON object
     */
    public static EcosystemSnapshot build(JSONObject state, JSONObject ports,
                                          JSONObject metrics, JSONObject enrModels,
                                          JSONObject enrInference) {
        long now = System.currentTimeMillis();
        List<EcosystemNode> nodes = new ArrayList<>();
        List<EcosystemEdge> edges = new ArrayList<>();
        List<EcosystemSnapshot.EventEntry> events = new ArrayList<>();
        Map<String, Object> meta = new LinkedHashMap<>();

        JSONObject app = opt(state, "app");
        String upmVersion = optStr(app, "version", "");
        int upmPort = optInt(app, "port", 0);

        // ------------------------------------------------------------ ROOT
        EcosystemNode root = new EcosystemNode(ROOT_ID, null, "upm", "upm",
                "UPM — Unified Project Management", EcosystemNode.NodeType.ROOT,
                EcosystemNode.RuntimeStatus.RUNNING);
        root.setVersion(upmVersion);
        root.setEndpoint("http://127.0.0.1:" + upmPort);
        root.meta("description", "Authoritative ecosystem control center");
        nodes.add(root);

        // ------------------------------------------------------- PRODUCTS
        JSONArray projects = optArray(state, "projects");
        for (int i = 0; i < projects.length(); i++) {
            JSONObject p = projects.optJSONObject(i);
            if (p == null) continue;
            String id = optStr(p, "id", "");
            if (id.isBlank()) continue;
            String nodeId = productId(id);
            EcosystemNode product = new EcosystemNode(nodeId, ROOT_ID, id, id,
                    optStr(p, "name", id), EcosystemNode.NodeType.PRODUCT,
                    runtimeStatus(optStr(p, "runtimeState", "UNKNOWN")));
            product.setStatusDetail(statusDetail(p));
            product.meta("type", optStr(p, "type", ""));
            product.meta("repoDir", optStr(p, "repoDir", ""));
            product.setPort(optInt(p, "boundPort", 0));
            if (optInt(p, "boundPort", 0) > 0) {
                product.setEndpoint("http://127.0.0.1:" + optInt(p, "boundPort", 0));
            }
            product.meta("apiBase", optStr(p, "apiBase", ""));
            product.meta("enrMonitor", p.optBoolean("enrMonitor"));
            product.meta("launchConfigs", launchConfigNames(p));
            product.meta("uptimeMs", optLong(p, "uptimeMs", 0));
            nodes.add(product);
            edges.add(new EcosystemEdge("edge:upm:" + id, ROOT_ID, nodeId,
                    EcosystemEdge.EdgeType.HIERARCHICAL,
                    product.getStatus() == EcosystemNode.RuntimeStatus.RUNNING
                            ? EcosystemEdge.EdgeStatus.ACTIVE : EcosystemEdge.EdgeStatus.IDLE,
                    "manages"));

            // start-up dependencies declared by the workspace
            JSONArray startupAfter = optArray(p, "startupAfter");
            for (int j = 0; j < startupAfter.length(); j++) {
                String dep = startupAfter.optString(j, "");
                if (!dep.isBlank() && !dep.equals(id)) {
                    edges.add(new EcosystemEdge("edge:dep:" + id + ":" + dep,
                            productId(dep), nodeId, EcosystemEdge.EdgeType.DEPENDENCY,
                            EcosystemEdge.EdgeStatus.IDLE, "starts after"));
                }
            }
        }

        // ----------------------------------------------- SERVICES (ports)
        JSONArray portEntries = optArray(ports, "entries");
        Map<String, List<EcosystemNode>> servicesByProduct = new LinkedHashMap<>();
        for (int i = 0; i < portEntries.length(); i++) {
            JSONObject e = portEntries.optJSONObject(i);
            if (e == null) continue;
            String appName = optStr(e, "app", "");
            int port = optInt(e, "port", 0);
            if (port <= 0) continue;
            String productId = productId(appName);
            EcosystemNode service = new EcosystemNode(
                    "service:" + appName + ":" + port,
                    nodeOrRoot(productId, nodes),
                    appName, appName + ":" + optStr(e, "service", String.valueOf(port)),
                    optStr(e, "service", "Service") + " :" + port,
                    EcosystemNode.NodeType.SERVICE,
                    portStatus(optStr(e, "status", "READY")));
            service.setEndpoint("http://127.0.0.1:" + port);
            service.setPort(port);
            service.meta("protocol", optStr(e, "protocol", ""));
            service.meta("kind", optStr(e, "kind", ""));
            service.meta("description", optStr(e, "description", ""));
            service.meta("process", optStr(e, "process", ""));
            service.meta("pid", optInt(e, "pid", 0));
            service.setStatusDetail(optStr(e, "detail", ""));
            nodes.add(service);
            servicesByProduct.computeIfAbsent(appName, k -> new ArrayList<>()).add(service);
            edges.add(new EcosystemEdge("edge:svc:" + appName + ":" + port,
                    nodeOrRoot(productId, nodes), service.getId(),
                    EcosystemEdge.EdgeType.HIERARCHICAL,
                    service.getStatus() == EcosystemNode.RuntimeStatus.RUNNING
                            ? EcosystemEdge.EdgeStatus.ACTIVE : EcosystemEdge.EdgeStatus.IDLE,
                    null));
            if (service.getStatus() == EcosystemNode.RuntimeStatus.RUNNING) {
                edges.add(new EcosystemEdge("edge:rt:" + appName + ":" + port,
                        productId, service.getId(),
                        EcosystemEdge.EdgeType.RUNTIME_CONNECTION,
                        EcosystemEdge.EdgeStatus.ACTIVE, "listening"));
            }
        }

        // --------------------------------------------------------- ENR
        buildEnrSubtree(nodes, edges, state, enrModels, enrInference);

        // ----------------------------------------------------- HARDWARE
        buildHardware(nodes, edges, metrics);

        // ------------------------------------------- profile dependencies
        JSONArray profiles = optArray(state, "profiles");
        for (int i = 0; i < profiles.length(); i++) {
            JSONObject pr = profiles.optJSONObject(i);
            if (pr == null) continue;
            JSONArray order = optArray(pr, "startupOrder");
            String prev = null;
            for (int j = 0; j < order.length(); j++) {
                String cur = order.optString(j, "");
                if (cur.isBlank()) continue;
                if (prev != null && !prev.equals(cur)) {
                    edges.add(new EcosystemEdge("edge:profile:" + optStr(pr, "id", String.valueOf(i)) + ":" + j,
                            productId(prev), productId(cur),
                            EcosystemEdge.EdgeType.DEPENDENCY,
                            EcosystemEdge.EdgeStatus.IDLE, optStr(pr, "id", "")));
                }
                prev = cur;
            }
        }

        // --------------------------------------------------------- EVENTS
        buildEvents(events, state);

        meta.put("upmPort", upmPort);
        meta.put("workspace", optStr(state, "workspace", ""));
        JSONObject ws = opt(state, "workspace");
        if (ws != null) {
            meta.put("workspaceName", optStr(ws, "name", ""));
        }
        // Launch profiles for the Overview panel (id, name, startupOrder).
        List<Map<String, Object>> profileList = new ArrayList<>();
        for (int i = 0; i < profiles.length(); i++) {
            JSONObject pr = profiles.optJSONObject(i);
            if (pr == null) continue;
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id", optStr(pr, "id", ""));
            pm.put("name", optStr(pr, "name", ""));
            JSONArray order = optArray(pr, "startupOrder");
            List<String> orderList = new ArrayList<>();
            for (int j = 0; j < order.length(); j++) {
                String cur = order.optString(j, "");
                if (!cur.isBlank()) orderList.add(cur);
            }
            pm.put("startupOrder", orderList);
            JSONArray projectsArr = optArray(pr, "projects");
            List<String> projList = new ArrayList<>();
            for (int j = 0; j < projectsArr.length(); j++) {
                String cur = projectsArr.optString(j, "");
                if (!cur.isBlank()) projList.add(cur);
            }
            pm.put("projects", projList);
            profileList.add(pm);
        }
        meta.put("profiles", profileList);
        return new EcosystemSnapshot(now, nodes, edges, events, meta);
    }

    // ------------------------------------------------------------------
    // ENR subtree: provider / models / instances / sessions
    // ------------------------------------------------------------------
    private static void buildEnrSubtree(List<EcosystemNode> nodes, List<EcosystemEdge> edges,
                                        JSONObject state, JSONObject enrModels,
                                        JSONObject enrInference) {
        JSONObject enrState = opt(state, "enr");
        String enrProductId = productId("ENR");
        EcosystemNode enrProduct = find(nodes, enrProductId);
        if (enrProduct == null) return; // ENR not registered in this workspace

        String enrRunState = optStr(enrState, "state", "UNKNOWN");
        boolean enrUp = "RUNNING".equals(enrRunState) || "READY".equals(enrRunState);
        String health = optStr(enrState, "health", "");

        // Provider node
        EcosystemNode provider = new EcosystemNode("provider:ENR:enr",
                enrProductId, "ENR", "enr-provider",
                "ENR — EVO Native Runtime", EcosystemNode.NodeType.PROVIDER,
                enrUp ? EcosystemNode.RuntimeStatus.RUNNING : EcosystemNode.RuntimeStatus.STOPPED);
        provider.setStatusDetail(health);
        provider.meta("pid", optInt(enrState, "pid", 0));
        nodes.add(provider);
        edges.add(new EcosystemEdge("edge:enr:provider", enrProductId, provider.getId(),
                EcosystemEdge.EdgeType.HIERARCHICAL,
                enrUp ? EcosystemEdge.EdgeStatus.ACTIVE : EcosystemEdge.EdgeStatus.IDLE, null));

        // Models (loaded models from ENR /models; discovered .gguf files otherwise)
        JSONObject loaded = opt(enrModels, "loaded");
        JSONArray models = optArray(loaded, "models");
        if (models.length() == 0 && enrModels != null) {
            JSONArray files = optArray(enrModels, "files");
            for (int i = 0; i < files.length(); i++) {
                JSONObject f = files.optJSONObject(i);
                if (f != null && f.optString("name", "").length() > 0) {
                    JSONObject fake = new JSONObject();
                    fake.put("id", f.optString("name"));
                    fake.put("name", f.optString("name"));
                    fake.put("path", f.optString("path"));
                    fake.put("sizeBytes", f.optLong("sizeBytes", -1));
                    fake.put("available", true);
                    models.put(fake);
                }
            }
        }
        for (int i = 0; i < models.length(); i++) {
            JSONObject m = models.optJSONObject(i);
            if (m == null) continue;
            String name = optStr(m, "name", optStr(m, "id", "model" + i));
            String mid = "model:ENR:" + name;
            boolean loadedNow = !m.has("available") || m.optBoolean("available", true);
            EcosystemNode model = new EcosystemNode(mid, provider.getId(), "ENR", name,
                    name, EcosystemNode.NodeType.MODEL,
                    loadedNow ? EcosystemNode.RuntimeStatus.READY
                            : EcosystemNode.RuntimeStatus.STOPPED);
            model.meta("path", optStr(m, "path", ""));
            model.meta("sizeBytes", optLong(m, "sizeBytes", -1));
            model.meta("quantization", optStr(m, "quantization", ""));
            model.meta("parameters", optStr(m, "parameters", ""));
            model.meta("contextSize", optStr(m, "contextSize", ""));
            nodes.add(model);
            edges.add(new EcosystemEdge("edge:model:" + name, provider.getId(), mid,
                    EcosystemEdge.EdgeType.HIERARCHICAL,
                    loadedNow ? EcosystemEdge.EdgeStatus.ACTIVE : EcosystemEdge.EdgeStatus.IDLE,
                    loadedNow ? "loaded" : "available"));
        }

        // Inference sessions → INSTANCE/SESSION nodes under the provider
        JSONObject inference = enrInference != null ? opt(enrInference, "sessions") : null;
        JSONArray sessions = inference instanceof JSONObject
                ? optArray((JSONObject) inference, "sessions")
                : new JSONArray();
        // Also accept a bare array payload.
        if (enrInference != null && enrInference.optJSONArray("sessions") != null) {
            sessions = enrInference.optJSONArray("sessions");
        }
        for (int i = 0; i < sessions.length(); i++) {
            JSONObject s = sessions.optJSONObject(i);
            if (s == null) continue;
            String sid = optStr(s, "id", "session" + i);
            String modelName = optStr(s, "model", "");
            String sessionNode = "session:ENR:" + sid;
            EcosystemNode session = new EcosystemNode(sessionNode, provider.getId(),
                    "ENR", sid, "Session " + sid, EcosystemNode.NodeType.SESSION,
                    EcosystemNode.RuntimeStatus.RUNNING);
            session.meta("model", modelName);
            session.meta("state", optStr(s, "state", "active"));
            nodes.add(session);
            edges.add(new EcosystemEdge("edge:session:" + sid, provider.getId(), sessionNode,
                    EcosystemEdge.EdgeType.HIERARCHICAL, EcosystemEdge.EdgeStatus.ACTIVE, null));
            if (!modelName.isBlank()) {
                edges.add(new EcosystemEdge("edge:session-model:" + sid,
                        sessionNode, "model:ENR:" + modelName,
                        EcosystemEdge.EdgeType.DATA_FLOW, EcosystemEdge.EdgeStatus.ACTIVE, "inference"));
            }
        }

        // Devices → INSTANCE nodes under hardware where reported by ENR
        JSONObject devices = opt(enrState, "devices");
        JSONArray deviceList = devices instanceof JSONObject
                ? optArray((JSONObject) devices, "devices") : new JSONArray();
        for (int i = 0; i < deviceList.length(); i++) {
            JSONObject d = deviceList.optJSONObject(i);
            if (d == null) continue;
            String name = optStr(d, "name", "device" + i);
            String did = "instance:ENR:device:" + i;
            EcosystemNode dev = new EcosystemNode(did, provider.getId(), "ENR",
                    "device-" + i, name, EcosystemNode.NodeType.INSTANCE,
                    EcosystemNode.RuntimeStatus.RUNNING);
            dev.meta("kind", optStr(d, "kind", ""));
            nodes.add(dev);
            edges.add(new EcosystemEdge("edge:dev:" + i, provider.getId(), did,
                    EcosystemEdge.EdgeType.HIERARCHICAL, EcosystemEdge.EdgeStatus.ACTIVE, null));
        }
    }

    // ------------------------------------------------------------------
    // Hardware subtree (CPU / RAM / GPU) under the root
    // ------------------------------------------------------------------
    private static void buildHardware(List<EcosystemNode> nodes, List<EcosystemEdge> edges,
                                      JSONObject metrics) {
        if (metrics == null) return;

        JSONObject cpu = opt(metrics, "cpu");
        if (cpu != null) {
            EcosystemNode n = new EcosystemNode("hw:cpu", ROOT_ID, "upm", "cpu",
                    "CPU — " + optStr(cpu, "model", "System CPU"),
                    EcosystemNode.NodeType.HARDWARE, EcosystemNode.RuntimeStatus.RUNNING);
            n.metric("utilizationPercent", numOr(cpu, "percent", -1));
            n.metric("physicalCores", optInt(cpu, "physicalCores", 0));
            n.metric("logicalProcessors", optArray(cpu, "cores").length());
            n.metric("frequencyMhz", optStr(cpu, "frequencyMhz", "N/A"));
            n.metric("temperatureCelsius", optStr(cpu, "temperature", "N/A"));
            n.metric("load1", optStr(cpu, "load1", "N/A"));
            nodes.add(n);
            edges.add(new EcosystemEdge("edge:hw:cpu", ROOT_ID, "hw:cpu",
                    EcosystemEdge.EdgeType.HIERARCHICAL, EcosystemEdge.EdgeStatus.ACTIVE, null));
        }

        JSONObject mem = opt(metrics, "memory");
        if (mem != null) {
            EcosystemNode n = new EcosystemNode("hw:ram", ROOT_ID, "upm", "ram",
                    "System RAM", EcosystemNode.NodeType.HARDWARE,
                    EcosystemNode.RuntimeStatus.RUNNING);
            n.metric("utilizationPercent", numOr(mem, "percent", -1));
            n.metric("usedGb", kbToGb(optLong(mem, "usedKb", -1)));
            n.metric("totalGb", kbToGb(optLong(mem, "totalKb", -1)));
            n.metric("availableGb", kbToGb(optLong(mem, "availableKb", -1)));
            n.metric("swapPercent", numOr(mem, "swapPercent", -1));
            nodes.add(n);
            edges.add(new EcosystemEdge("edge:hw:ram", ROOT_ID, "hw:ram",
                    EcosystemEdge.EdgeType.HIERARCHICAL, EcosystemEdge.EdgeStatus.ACTIVE, null));
        }

        JSONArray gpus = optArray(metrics, "gpu");
        for (int i = 0; i < gpus.length(); i++) {
            JSONObject g = gpus.optJSONObject(i);
            if (g == null) continue;
            String gid = "hw:gpu:" + i;
            EcosystemNode n = new EcosystemNode(gid, ROOT_ID, "upm", "gpu:" + i,
                    optStr(g, "name", "GPU " + i), EcosystemNode.NodeType.HARDWARE,
                    EcosystemNode.RuntimeStatus.RUNNING);
            n.metric("utilizationPercent", numOr(g, "utilPercent", -1));
            n.metric("vramUsedMb", numOr(g, "memUsedMb", -1));
            n.metric("vramTotalMb", numOr(g, "memTotalMb", -1));
            n.metric("vramPercent", numOr(g, "vramPercent", -1));
            n.metric("temperatureCelsius", numOr(g, "temperature", -1));
            n.metric("powerW", numOr(g, "powerW", -1));
            n.metric("driver", optStr(g, "driver", "N/A"));
            JSONObject cuda = opt(g, "cuda");
            n.metric("cudaStatus", cuda != null ? optStr(cuda, "status", "N/A") : "N/A");
            nodes.add(n);
            edges.add(new EcosystemEdge("edge:hw:gpu:" + i, ROOT_ID, gid,
                    EcosystemEdge.EdgeType.HIERARCHICAL, EcosystemEdge.EdgeStatus.ACTIVE, null));
        }
    }

    // ------------------------------------------------------------------
    // Event feed (ops + product state)
    // ------------------------------------------------------------------
    private static void buildEvents(List<EcosystemSnapshot.EventEntry> events, JSONObject state) {
        JSONArray projects = optArray(state, "projects");
        for (int i = 0; i < projects.length(); i++) {
            JSONObject p = projects.optJSONObject(i);
            if (p == null) continue;
            String stateStr = optStr(p, "runtimeState", "UNKNOWN");
            if ("RUNNING".equals(stateStr)) {
                events.add(new EcosystemSnapshot.EventEntry(System.currentTimeMillis(),
                        "product", productId(optStr(p, "id", "")),
                        optStr(p, "name", "?") + " is running (pid "
                                + optInt(p, "pid", 0) + ")"));
            } else if ("FAILED".equals(stateStr)) {
                events.add(new EcosystemSnapshot.EventEntry(System.currentTimeMillis(),
                        "error", productId(optStr(p, "id", "")),
                        optStr(p, "name", "?") + " failed: "
                                + optStr(p, "lastError", "unknown error")));
            }
        }
        JSONObject ops = opt(state, "ops");
        JSONArray active = optArray(ops, "active");
        for (int i = 0; i < active.length(); i++) {
            JSONObject op = active.optJSONObject(i);
            if (op == null) continue;
            events.add(new EcosystemSnapshot.EventEntry(System.currentTimeMillis(),
                    "op", productId(optStr(op, "projectId", "")),
                    "Operation " + optStr(op, "type", "?") + " "
                            + optStr(op, "phase", "") + " — " + optStr(op, "message", "")));
        }
        JSONArray hist = optArray(ops, "history");
        int cap = Math.min(hist.length(), 8);
        for (int i = cap - 1; i >= 0; i--) {
            JSONObject op = hist.optJSONObject(i);
            if (op == null) continue;
            long end = optLong(op, "endMs", 0);
            events.add(new EcosystemSnapshot.EventEntry(end > 0 ? end : System.currentTimeMillis(),
                    "op", productId(optStr(op, "projectId", "")),
                    optStr(op, "type", "?") + " " + optStr(op, "status", "?")));
        }
    }

    // ------------------------------------------------------------------
    // JSON helpers (defensive)
    // ------------------------------------------------------------------
    public static String productId(String raw) {
        if (raw == null) return "";
        String id = raw.trim();
        switch (id.toLowerCase(Locale.ROOT)) {
            case "upm": return ROOT_ID;
            case "dcode": return "product:DCode";
            case "dcoder": return "product:DCode";
            case "dstudio": return "product:DStudio";
            case "evo": return "product:EVO";
            case "evolution": return "product:EVO";
            case "enr": return "product:ENR";
            case "tryst": return "product:TRYST";
            case "ollama": return "product:Ollama";
            default: return "product:" + id;
        }
    }

    static String nodeOrRoot(String productId, List<EcosystemNode> nodes) {
        if (productId.equals(ROOT_ID)) return ROOT_ID;
        return find(nodes, productId) != null ? productId : ROOT_ID;
    }

    private static EcosystemNode find(List<EcosystemNode> nodes, String id) {
        for (EcosystemNode n : nodes) {
            if (n.getId().equals(id)) return n;
        }
        return null;
    }

    private static EcosystemNode.RuntimeStatus runtimeStatus(String raw) {
        if (raw == null) return EcosystemNode.RuntimeStatus.UNKNOWN;
        switch (raw.toUpperCase(Locale.ROOT)) {
            case "RUNNING": case "READY": return EcosystemNode.RuntimeStatus.RUNNING;
            case "STARTING": case "STOPPING": return EcosystemNode.RuntimeStatus.STARTING;
            case "STOPPED": return EcosystemNode.RuntimeStatus.STOPPED;
            case "FAILED": return EcosystemNode.RuntimeStatus.FAILED;
            case "EXTERNAL": return EcosystemNode.RuntimeStatus.RUNNING;
            default: return EcosystemNode.RuntimeStatus.UNKNOWN;
        }
    }

    private static EcosystemNode.RuntimeStatus portStatus(String raw) {
        if (raw == null) return EcosystemNode.RuntimeStatus.UNKNOWN;
        switch (raw.toUpperCase(Locale.ROOT)) {
            case "RUNNING": return EcosystemNode.RuntimeStatus.RUNNING;
            case "READY": return EcosystemNode.RuntimeStatus.READY;
            case "OCCUPIED": return EcosystemNode.RuntimeStatus.OCCUPIED;
            default: return EcosystemNode.RuntimeStatus.UNKNOWN;
        }
    }

    private static String statusDetail(JSONObject p) {
        StringBuilder sb = new StringBuilder();
        String st = optStr(p, "runtimeState", "UNKNOWN");
        sb.append(st.toLowerCase(Locale.ROOT));
        int pid = optInt(p, "pid", 0);
        if (pid > 0) sb.append(" · pid ").append(pid);
        int port = optInt(p, "boundPort", 0);
        if (port > 0) sb.append(" · port ").append(port);
        String err = optStr(p, "lastError", "");
        if (!err.isBlank()) sb.append(" · ").append(err);
        return sb.toString();
    }

    private static List<String> launchConfigNames(JSONObject p) {
        List<String> out = new ArrayList<>();
        JSONArray lcs = optArray(p, "launchConfigs");
        for (int i = 0; i < lcs.length(); i++) {
            JSONObject lc = lcs.optJSONObject(i);
            if (lc != null && lc.optString("id", "").length() > 0) {
                out.add(lc.optString("id"));
            }
        }
        return out;
    }

    private static JSONObject opt(JSONObject o, String key) {
        return o == null ? null : o.optJSONObject(key);
    }

    private static JSONArray optArray(JSONObject o, String key) {
        if (o == null) return new JSONArray();
        JSONArray a = o.optJSONArray(key);
        return a == null ? new JSONArray() : a;
    }

    private static String optStr(JSONObject o, String key, String def) {
        if (o == null || o.isNull(key)) return def;
        Object v = o.opt(key);
        return v == null ? def : String.valueOf(v);
    }

    private static int optInt(JSONObject o, String key, int def) {
        if (o == null) return def;
        try {
            return o.optInt(key, def);
        } catch (Exception e) {
            return def;
        }
    }

    private static long optLong(JSONObject o, String key, long def) {
        if (o == null) return def;
        try {
            return o.optLong(key, def);
        } catch (Exception e) {
            return def;
        }
    }

    /** Numeric or -1; string values like "N/A" map to -1. */
    private static double numOr(JSONObject o, String key, double def) {
        if (o == null) return def;
        Object v = o.opt(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
        return def;
    }

    private static double kbToGb(long kb) {
        return kb > 0 ? Math.round(kb / 1048576.0 * 10.0) / 10.0 : -1;
    }
}
