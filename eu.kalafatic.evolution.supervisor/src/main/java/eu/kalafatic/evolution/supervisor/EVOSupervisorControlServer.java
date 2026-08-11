package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import fi.iki.elonen.NanoHTTPD;

/**
 * Premium glassmorphic Control Server for the External Supervisor running on port 28080.
 * Provides a beautiful Web UI and REST API proxy to interact with port 8089 dynamically.
 */
public class EVOSupervisorControlServer extends NanoHTTPD {

    private final File baseDir;

    public EVOSupervisorControlServer(int port, File baseDir) {
        super("127.0.0.1", port);
        this.baseDir = baseDir;
        System.out.println("[CONTROL-HTTP] Control Server created on port " + port + " bound to 127.0.0.1");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String method = session.getMethod().toString();

        System.out.println("[CONTROL-HTTP] " + method + " " + uri);

        try {
            // Serve Web UI Dashboard at root
            if (uri.equals("/") || uri.equals("/index.html")) {
                return newFixedLengthResponse(Response.Status.OK, "text/html", getDashboardHtml());
            }

            // API Endpoints
            if (uri.startsWith("/api/")) {
                switch (uri) {
                    case "/api/state":
                        return handleGetState();
                    case "/api/logs":
                        return handleGetLogs();
                    case "/api/action":
                        return handleActionProxy(session);
                    default:
                        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                            "{\"status\":\"ERROR\",\"message\":\"API Endpoint Not Found\"}");
                }
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
        } catch (Exception e) {
            System.err.println("[CONTROL-HTTP] Error serving request " + uri + ": " + e.getMessage());
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }

    private Response handleGetState() {
        File stateFile = new File(baseDir, "self-dev-run/supervisor_state.json");
        if (stateFile.exists()) {
            try {
                byte[] bytes = Files.readAllBytes(stateFile.toPath());
                String json = new String(bytes, StandardCharsets.UTF_8);
                return newFixedLengthResponse(Response.Status.OK, "application/json", json);
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"status\":\"ERROR\",\"message\":\"Failed to read supervisor state: " + e.getMessage() + "\"}");
            }
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json",
            "{\"currentTask\":null,\"taskQueue\":[],\"taskHistory\":[],\"maxTaskAttempts\":3,\"maxRepairAttempts\":3,\"maxConsecutiveFailures\":3,\"consecutiveFailures\":0}");
    }

    private Response handleGetLogs() {
        File logFile = new File(baseDir, "self-dev-run/events.log");
        if (logFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
                // Get the last 100 lines
                int total = lines.size();
                int start = Math.max(0, total - 100);
                StringBuilder sb = new StringBuilder();
                for (int i = start; i < total; i++) {
                    sb.append(lines.get(i)).append("\n");
                }
                Map<String, String> data = new HashMap<>();
                data.put("status", "OK");
                data.put("logs", sb.toString());

                // Manual basic JSON escaping
                String cleanLogs = sb.toString()
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");

                return newFixedLengthResponse(Response.Status.OK, "application/json",
                    "{\"status\":\"OK\",\"logs\":\"" + cleanLogs + "\"}");
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                    "{\"status\":\"ERROR\",\"message\":\"Failed to read events.log: " + e.getMessage() + "\"}");
            }
        }
        return newFixedLengthResponse(Response.Status.OK, "application/json",
            "{\"status\":\"OK\",\"logs\":\"[SYSTEM] No event logs found. Waiting for supervisor to start writing logs...\"}");
    }

    private Response handleActionProxy(IHTTPSession session) {
        Map<String, List<String>> parameters = session.getParameters();
        List<String> actionList = parameters.get("action");
        if (actionList == null || actionList.isEmpty()) {
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json",
                "{\"status\":\"ERROR\",\"message\":\"Missing 'action' parameter\"}");
        }

        String action = actionList.get(0);
        String subPath = "";
        switch (action) {
            case "ping":
                subPath = "/ping";
                break;
            case "git-check":
                subPath = "/git-check";
                break;
            case "maven-check":
                subPath = "/maven-check";
                break;
            case "build":
                subPath = "/build";
                break;
            case "export":
                subPath = "/export";
                break;
            case "start-evo":
                subPath = "/start-evo";
                break;
            case "stop-evo":
                subPath = "/stop-evo";
                break;
            default:
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "application/json",
                    "{\"status\":\"ERROR\",\"message\":\"Invalid action: " + action + "\"}");
        }

        // Forward query params if they exist
        StringBuilder queryParams = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            if (entry.getKey().equals("action")) continue;
            for (String val : entry.getValue()) {
                if (first) {
                    queryParams.append("?");
                    first = false;
                } else {
                    queryParams.append("&");
                }
                queryParams.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                           .append("=")
                           .append(URLEncoder.encode(val, StandardCharsets.UTF_8));
            }
        }

        String targetUrl = "http://127.0.0.1:8089" + subPath + queryParams.toString();
        System.out.println("[CONTROL-PROXY] Forwarding request to target: " + targetUrl);

        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(subPath.equals("/build") || subPath.equals("/start-evo") || subPath.equals("/stop-evo") ? "POST" : "GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);

            int code = conn.getResponseCode();
            BufferedReader r = new BufferedReader(new InputStreamReader(
                (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8
            ));

            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                responseBody.append(line);
            }

            // Return exact response from 8089 server with proper content type
            String responseStr = responseBody.toString();
            String contentType = conn.getHeaderField("Content-Type");
            if (contentType == null) contentType = "application/json";

            return newFixedLengthResponse(Response.Status.lookup(code), contentType, responseStr);
        } catch (Exception e) {
            System.err.println("[CONTROL-PROXY] Failed to forward request to supervisor: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                "{\"status\":\"ERROR\",\"message\":\"Proxy call failed: " + e.getMessage().replace("\"", "\\\"") + "\"}");
        }
    }

    private String getDashboardHtml() {
        return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>EVO AI External Supervisor Hub</title>\n" +
            "    <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&family=JetBrains+Mono&display=swap\" rel=\"stylesheet\">\n" +
            "    <style>\n" +
            "        :root {\n" +
            "            --bg-gradient: linear-gradient(135deg, #090d16 0%, #111827 50%, #1e1b4b 100%);\n" +
            "            --panel-bg: rgba(255, 255, 255, 0.03);\n" +
            "            --panel-border: rgba(255, 255, 255, 0.08);\n" +
            "            --accent-primary: #6366f1;\n" +
            "            --accent-glow: rgba(99, 102, 241, 0.2);\n" +
            "            --text-main: #f1f5f9;\n" +
            "            --text-muted: #94a3b8;\n" +
            "            --color-success: #10b981;\n" +
            "            --color-danger: #ef4444;\n" +
            "            --color-warning: #f59e0b;\n" +
            "            --color-info: #3b82f6;\n" +
            "        }\n" +
            "\n" +
            "        * {\n" +
            "            box-sizing: border-box;\n" +
            "            margin: 0;\n" +
            "            padding: 0;\n" +
            "        }\n" +
            "\n" +
            "        body {\n" +
            "            font-family: 'Inter', sans-serif;\n" +
            "            background: var(--bg-gradient);\n" +
            "            background-attachment: fixed;\n" +
            "            color: var(--text-main);\n" +
            "            min-height: 100vh;\n" +
            "            overflow-x: hidden;\n" +
            "            padding-bottom: 40px;\n" +
            "        }\n" +
            "\n" +
            "        header {\n" +
            "            background: rgba(15, 23, 42, 0.6);\n" +
            "            backdrop-filter: blur(12px);\n" +
            "            border-bottom: 1px solid var(--panel-border);\n" +
            "            padding: 16px 40px;\n" +
            "            display: flex;\n" +
            "            justify-content: space-between;\n" +
            "            align-items: center;\n" +
            "            position: sticky;\n" +
            "            top: 0;\n" +
            "            z-index: 100;\n" +
            "        }\n" +
            "\n" +
            "        .header-title {\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            gap: 12px;\n" +
            "        }\n" +
            "\n" +
            "        .header-title h1 {\n" +
            "            font-size: 20px;\n" +
            "            font-weight: 700;\n" +
            "            letter-spacing: -0.5px;\n" +
            "            background: linear-gradient(90deg, #a5b4fc, #6366f1);\n" +
            "            -webkit-background-clip: text;\n" +
            "            -webkit-text-fill-color: transparent;\n" +
            "        }\n" +
            "\n" +
            "        .header-title .logo {\n" +
            "            font-size: 24px;\n" +
            "        }\n" +
            "\n" +
            "        .connection-status {\n" +
            "            display: flex;\n" +
            "            gap: 16px;\n" +
            "        }\n" +
            "\n" +
            "        .status-badge {\n" +
            "            background: rgba(255, 255, 255, 0.05);\n" +
            "            border: 1px solid var(--panel-border);\n" +
            "            padding: 6px 14px;\n" +
            "            border-radius: 20px;\n" +
            "            font-size: 12px;\n" +
            "            font-weight: 500;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            gap: 8px;\n" +
            "        }\n" +
            "\n" +
            "        .status-dot {\n" +
            "            width: 8px;\n" +
            "            height: 8px;\n" +
            "            border-radius: 50%;\n" +
            "            background: var(--color-danger);\n" +
            "            box-shadow: 0 0 8px var(--color-danger);\n" +
            "        }\n" +
            "\n" +
            "        .status-dot.online {\n" +
            "            background: var(--color-success);\n" +
            "            box-shadow: 0 0 8px var(--color-success);\n" +
            "        }\n" +
            "\n" +
            "        .container {\n" +
            "            max-width: 1400px;\n" +
            "            margin: 32px auto 0;\n" +
            "            padding: 0 24px;\n" +
            "            display: grid;\n" +
            "            grid-template-columns: 1fr 350px;\n" +
            "            gap: 24px;\n" +
            "        }\n" +
            "\n" +
            "        @media (max-width: 1100px) {\n" +
            "            .container {\n" +
            "                grid-template-columns: 1fr;\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        .main-panel {\n" +
            "            display: flex;\n" +
            "            flex-direction: column;\n" +
            "            gap: 24px;\n" +
            "        }\n" +
            "\n" +
            "        .sidebar {\n" +
            "            display: flex;\n" +
            "            flex-direction: column;\n" +
            "            gap: 24px;\n" +
            "        }\n" +
            "\n" +
            "        /* Glassmorphic Card Panel Style */\n" +
            "        .glass-card {\n" +
            "            background: var(--panel-bg);\n" +
            "            backdrop-filter: blur(16px);\n" +
            "            border: 1px solid var(--panel-border);\n" +
            "            border-radius: 16px;\n" +
            "            padding: 24px;\n" +
            "            box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.3);\n" +
            "            position: relative;\n" +
            "            overflow: hidden;\n" +
            "        }\n" +
            "\n" +
            "        .glass-card::before {\n" +
            "            content: '';\n" +
            "            position: absolute;\n" +
            "            top: 0;\n" +
            "            left: 0;\n" +
            "            right: 0;\n" +
            "            height: 2px;\n" +
            "            background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);\n" +
            "        }\n" +
            "\n" +
            "        .card-header {\n" +
            "            display: flex;\n" +
            "            justify-content: space-between;\n" +
            "            align-items: center;\n" +
            "            margin-bottom: 20px;\n" +
            "            border-bottom: 1px solid rgba(255, 255, 255, 0.05);\n" +
            "            padding-bottom: 12px;\n" +
            "        }\n" +
            "\n" +
            "        .card-header h2 {\n" +
            "            font-size: 16px;\n" +
            "            font-weight: 600;\n" +
            "            letter-spacing: -0.2px;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            gap: 10px;\n" +
            "        }\n" +
            "\n" +
            "        /* Status Stats Grid */\n" +
            "        .stats-grid {\n" +
            "            display: grid;\n" +
            "            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n" +
            "            gap: 16px;\n" +
            "        }\n" +
            "\n" +
            "        .stat-item {\n" +
            "            background: rgba(255, 255, 255, 0.02);\n" +
            "            border: 1px solid rgba(255, 255, 255, 0.04);\n" +
            "            border-radius: 12px;\n" +
            "            padding: 16px;\n" +
            "            text-align: center;\n" +
            "            transition: transform 0.2s, background 0.2s;\n" +
            "        }\n" +
            "\n" +
            "        .stat-item:hover {\n" +
            "            transform: translateY(-2px);\n" +
            "            background: rgba(255, 255, 255, 0.04);\n" +
            "        }\n" +
            "\n" +
            "        .stat-label {\n" +
            "            font-size: 11px;\n" +
            "            color: var(--text-muted);\n" +
            "            text-transform: uppercase;\n" +
            "            letter-spacing: 0.5px;\n" +
            "            margin-bottom: 6px;\n" +
            "        }\n" +
            "\n" +
            "        .stat-value {\n" +
            "            font-size: 24px;\n" +
            "            font-weight: 700;\n" +
            "            color: var(--text-main);\n" +
            "        }\n" +
            "\n" +
            "        /* Actions Controls Center */\n" +
            "        .controls-grid {\n" +
            "            display: grid;\n" +
            "            grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));\n" +
            "            gap: 14px;\n" +
            "        }\n" +
            "\n" +
            "        .btn {\n" +
            "            padding: 12px 16px;\n" +
            "            border-radius: 12px;\n" +
            "            border: 1px solid rgba(255, 255, 255, 0.08);\n" +
            "            font-size: 13px;\n" +
            "            font-weight: 600;\n" +
            "            cursor: pointer;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            justify-content: center;\n" +
            "            gap: 10px;\n" +
            "            transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);\n" +
            "            background: rgba(255, 255, 255, 0.03);\n" +
            "            color: var(--text-main);\n" +
            "        }\n" +
            "\n" +
            "        .btn:hover {\n" +
            "            background: rgba(255, 255, 255, 0.08);\n" +
            "            border-color: rgba(255, 255, 255, 0.15);\n" +
            "            transform: translateY(-2px);\n" +
            "        }\n" +
            "\n" +
            "        .btn:active {\n" +
            "            transform: scale(0.98);\n" +
            "        }\n" +
            "\n" +
            "        .btn-ping { border-left: 3px solid var(--accent-primary); }\n" +
            "        .btn-ping:hover { background: rgba(99, 102, 241, 0.15); box-shadow: 0 0 16px rgba(99, 102, 241, 0.2); }\n" +
            "        \n" +
            "        .btn-git { border-left: 3px solid #db2777; }\n" +
            "        .btn-git:hover { background: rgba(219, 39, 119, 0.15); box-shadow: 0 0 16px rgba(219, 39, 119, 0.2); }\n" +
            "\n" +
            "        .btn-maven { border-left: 3px solid #d97706; }\n" +
            "        .btn-maven:hover { background: rgba(217, 119, 6, 0.15); box-shadow: 0 0 16px rgba(217, 119, 6, 0.2); }\n" +
            "\n" +
            "        .btn-build { border-left: 3px solid #2563eb; }\n" +
            "        .btn-build:hover { background: rgba(37, 99, 235, 0.15); box-shadow: 0 0 16px rgba(37, 99, 235, 0.2); }\n" +
            "\n" +
            "        .btn-export { border-left: 3px solid #9333ea; }\n" +
            "        .btn-export:hover { background: rgba(147, 51, 234, 0.15); box-shadow: 0 0 16px rgba(147, 51, 234, 0.2); }\n" +
            "\n" +
            "        .btn-start { border-left: 3px solid #16a34a; }\n" +
            "        .btn-start:hover { background: rgba(22, 163, 74, 0.15); box-shadow: 0 0 16px rgba(22, 163, 74, 0.2); }\n" +
            "\n" +
            "        .btn-stop { border-left: 3px solid #dc2626; }\n" +
            "        .btn-stop:hover { background: rgba(220, 38, 38, 0.15); box-shadow: 0 0 16px rgba(220, 38, 38, 0.2); }\n" +
            "\n" +
            "        /* Active Task Panel */\n" +
            "        .active-task-wrapper {\n" +
            "            display: flex;\n" +
            "            flex-direction: column;\n" +
            "            gap: 12px;\n" +
            "        }\n" +
            "\n" +
            "        .task-title-row {\n" +
            "            display: flex;\n" +
            "            justify-content: space-between;\n" +
            "            align-items: center;\n" +
            "            margin-bottom: 8px;\n" +
            "        }\n" +
            "\n" +
            "        .task-id {\n" +
            "            font-family: 'JetBrains Mono', monospace;\n" +
            "            font-size: 13px;\n" +
            "            background: rgba(99, 102, 241, 0.15);\n" +
            "            padding: 4px 10px;\n" +
            "            border-radius: 6px;\n" +
            "            border: 1px solid rgba(99, 102, 241, 0.2);\n" +
            "            color: #a5b4fc;\n" +
            "        }\n" +
            "\n" +
            "        .task-status-lbl {\n" +
            "            font-size: 11px;\n" +
            "            font-weight: 700;\n" +
            "            text-transform: uppercase;\n" +
            "            letter-spacing: 0.5px;\n" +
            "            padding: 4px 10px;\n" +
            "            border-radius: 6px;\n" +
            "        }\n" +
            "\n" +
            "        .status-running { background: rgba(59, 130, 246, 0.15); color: #60a5fa; border: 1px solid rgba(59, 130, 246, 0.2); }\n" +
            "        .status-success { background: rgba(16, 185, 129, 0.15); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.2); }\n" +
            "        .status-failed { background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.2); }\n" +
            "        .status-idle { background: rgba(255, 255, 255, 0.05); color: var(--text-muted); border: 1px solid rgba(255, 255, 255, 0.08); }\n" +
            "\n" +
            "        .task-desc {\n" +
            "            font-size: 14px;\n" +
            "            line-height: 1.5;\n" +
            "            margin-bottom: 12px;\n" +
            "        }\n" +
            "\n" +
            "        .task-meta-grid {\n" +
            "            display: grid;\n" +
            "            grid-template-columns: repeat(2, 1fr);\n" +
            "            gap: 12px;\n" +
            "            background: rgba(255, 255, 255, 0.01);\n" +
            "            border: 1px solid rgba(255, 255, 255, 0.03);\n" +
            "            padding: 12px;\n" +
            "            border-radius: 8px;\n" +
            "        }\n" +
            "\n" +
            "        .meta-entry {\n" +
            "            font-size: 12px;\n" +
            "        }\n" +
            "        .meta-entry .lbl {\n" +
            "            color: var(--text-muted);\n" +
            "            font-size: 10px;\n" +
            "            text-transform: uppercase;\n" +
            "            margin-bottom: 2px;\n" +
            "        }\n" +
            "\n" +
            "        /* Live Event Log Console Terminal */\n" +
            "        .terminal {\n" +
            "            background: #05070c;\n" +
            "            border: 1px solid var(--panel-border);\n" +
            "            border-radius: 12px;\n" +
            "            padding: 20px;\n" +
            "            font-family: 'JetBrains Mono', monospace;\n" +
            "            font-size: 12px;\n" +
            "            line-height: 1.6;\n" +
            "            color: #38bdf8;\n" +
            "            height: 320px;\n" +
            "            overflow-y: auto;\n" +
            "            white-space: pre-wrap;\n" +
            "            box-shadow: inset 0 4px 16px rgba(0, 0, 0, 0.8);\n" +
            "        }\n" +
            "\n" +
            "        .terminal::-webkit-scrollbar {\n" +
            "            width: 8px;\n" +
            "        }\n" +
            "        .terminal::-webkit-scrollbar-track {\n" +
            "            background: rgba(0, 0, 0, 0.3);\n" +
            "        }\n" +
            "        .terminal::-webkit-scrollbar-thumb {\n" +
            "            background: rgba(255, 255, 255, 0.1);\n" +
            "            border-radius: 4px;\n" +
            "        }\n" +
            "        .terminal::-webkit-scrollbar-thumb:hover {\n" +
            "            background: rgba(255, 255, 255, 0.2);\n" +
            "        }\n" +
            "\n" +
            "        /* Action Response Callout */\n" +
            "        .response-callout {\n" +
            "            background: rgba(15, 23, 42, 0.6);\n" +
            "            border: 1px solid rgba(255, 255, 255, 0.05);\n" +
            "            border-radius: 12px;\n" +
            "            padding: 16px;\n" +
            "            margin-top: 10px;\n" +
            "            display: none;\n" +
            "            animation: fadeIn 0.3s ease-out;\n" +
            "        }\n" +
            "\n" +
            "        .response-header {\n" +
            "            font-size: 11px;\n" +
            "            font-weight: 700;\n" +
            "            text-transform: uppercase;\n" +
            "            margin-bottom: 8px;\n" +
            "            color: var(--accent-primary);\n" +
            "            display: flex;\n" +
            "            justify-content: space-between;\n" +
            "            align-items: center;\n" +
            "        }\n" +
            "\n" +
            "        .response-body {\n" +
            "            font-family: 'JetBrains Mono', monospace;\n" +
            "            font-size: 12px;\n" +
            "            color: #e2e8f0;\n" +
            "            max-height: 150px;\n" +
            "            overflow-y: auto;\n" +
            "            white-space: pre-wrap;\n" +
            "        }\n" +
            "\n" +
            "        /* Task Lists in Sidebar */\n" +
            "        .task-list {\n" +
            "            display: flex;\n" +
            "            flex-direction: column;\n" +
            "            gap: 10px;\n" +
            "            max-height: 350px;\n" +
            "            overflow-y: auto;\n" +
            "            padding-right: 4px;\n" +
            "        }\n" +
            "\n" +
            "        .task-list::-webkit-scrollbar {\n" +
            "            width: 4px;\n" +
            "        }\n" +
            "        .task-list::-webkit-scrollbar-thumb {\n" +
            "            background: rgba(255, 255, 255, 0.1);\n" +
            "            border-radius: 2px;\n" +
            "        }\n" +
            "\n" +
            "        .task-list-item {\n" +
            "            background: rgba(255, 255, 255, 0.02);\n" +
            "            border: 1px solid rgba(255, 255, 255, 0.05);\n" +
            "            border-radius: 8px;\n" +
            "            padding: 10px 12px;\n" +
            "            font-size: 12px;\n" +
            "            display: flex;\n" +
            "            justify-content: space-between;\n" +
            "            align-items: center;\n" +
            "            gap: 12px;\n" +
            "            transition: background 0.2s;\n" +
            "        }\n" +
            "\n" +
            "        .task-list-item:hover {\n" +
            "            background: rgba(255, 255, 255, 0.04);\n" +
            "        }\n" +
            "\n" +
            "        .task-item-left {\n" +
            "            display: flex;\n" +
            "            flex-direction: column;\n" +
            "            gap: 2px;\n" +
            "            overflow: hidden;\n" +
            "        }\n" +
            "\n" +
            "        .task-item-obj {\n" +
            "            font-weight: 500;\n" +
            "            white-space: nowrap;\n" +
            "            overflow: hidden;\n" +
            "            text-overflow: ellipsis;\n" +
            "        }\n" +
            "\n" +
            "        .task-item-id {\n" +
            "            font-family: 'JetBrains Mono', monospace;\n" +
            "            font-size: 10px;\n" +
            "            color: var(--text-muted);\n" +
            "        }\n" +
            "\n" +
            "        .spinner {\n" +
            "            animation: rotate 1s linear infinite;\n" +
            "            width: 14px;\n" +
            "            height: 14px;\n" +
            "            border: 2px solid currentColor;\n" +
            "            border-top-color: transparent;\n" +
            "            border-radius: 50%;\n" +
            "            display: inline-block;\n" +
            "        }\n" +
            "\n" +
            "        @keyframes rotate {\n" +
            "            100% { transform: rotate(360deg); }\n" +
            "        }\n" +
            "\n" +
            "        @keyframes fadeIn {\n" +
            "            from { opacity: 0; transform: translateY(4px); }\n" +
            "            to { opacity: 1; transform: translateY(0); }\n" +
            "        }\n" +
            "\n" +
            "        .empty-state {\n" +
            "            font-size: 11px;\n" +
            "            color: var(--text-muted);\n" +
            "            text-align: center;\n" +
            "            padding: 12px;\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "\n" +
            "    <header>\n" +
            "        <div class=\"header-title\">\n" +
            "            <span class=\"logo\">🧬</span>\n" +
            "            <h1>EVO AI External Supervisor Hub</h1>\n" +
            "        </div>\n" +
            "        <div class=\"connection-status\">\n" +
            "            <div class=\"status-badge\">\n" +
            "                <span class=\"status-dot online\"></span>\n" +
            "                Control Server (28080)\n" +
            "            </div>\n" +
            "            <div class=\"status-badge\">\n" +
            "                <span id=\"supervisor-status-dot\" class=\"status-dot\"></span>\n" +
            "                Supervisor API (8089)\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </header>\n" +
            "\n" +
            "    <div class=\"container\">\n" +
            "        \n" +
            "        <div class=\"main-panel\">\n" +
            "            \n" +
            "            <!-- Overview Statistics Panel -->\n" +
            "            <div class=\"glass-card\">\n" +
            "                <div class=\"card-header\">\n" +
            "                    <h2>📈 Overview & Telemetry</h2>\n" +
            "                </div>\n" +
            "                <div class=\"stats-grid\">\n" +
            "                    <div class=\"stat-item\">\n" +
            "                        <div class=\"stat-label\">Queued Tasks</div>\n" +
            "                        <div id=\"stat-queue\" class=\"stat-value\">0</div>\n" +
            "                    </div>\n" +
            "                    <div class=\"stat-item\">\n" +
            "                        <div class=\"stat-label\">Completed Tasks</div>\n" +
            "                        <div id=\"stat-completed\" class=\"stat-value\">0</div>\n" +
            "                    </div>\n" +
            "                    <div class=\"stat-item\">\n" +
            "                        <div class=\"stat-label\">Consecutive Failures</div>\n" +
            "                        <div id=\"stat-failures\" class=\"stat-value\" style=\"color: var(--color-danger);\">0</div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "\n" +
            "            <!-- Control Actions Center Panel -->\n" +
            "            <div class=\"glass-card\">\n" +
            "                <div class=\"card-header\">\n" +
            "                    <h2>⚡ Operations Control Center</h2>\n" +
            "                </div>\n" +
            "                <div class=\"controls-grid\">\n" +
            "                    <button class=\"btn btn-ping\" onclick=\"triggerAction('ping', this)\">\n" +
            "                        📡 Ping Health\n" +
            "                    </button>\n" +
            "                    <button class=\"btn btn-git\" onclick=\"triggerAction('git-check', this)\">\n" +
            "                        🐙 Git Check\n" +
            "                    </button>\n" +
            "                    <button class=\"btn btn-maven\" onclick=\"triggerAction('maven-check', this)\">\n" +
            "                        ☕ Maven Check\n" +
            "                    </button>\n" +
            "                    <button class=\"btn btn-build\" onclick=\"triggerAction('build', this)\">\n" +
            "                        🛠️ Run Build\n" +
            "                    </button>\n" +
            "                    <button class=\"btn btn-export\" onclick=\"triggerAction('export', this)\">\n" +
            "                        📦 Export Product\n" +
            "                    </button>\n" +
            "                    <button class=\"btn btn-start\" onclick=\"triggerAction('start-evo', this)\">\n" +
            "                        🚀 Start EVO\n" +
            "                    </button>\n" +
            "                    <button class=\"btn btn-stop\" onclick=\"triggerAction('stop-evo', this)\">\n" +
            "                        🛑 Stop EVO\n" +
            "                    </button>\n" +
            "                </div>\n" +
            "\n" +
            "                <!-- Command Response Box -->\n" +
            "                <div id=\"response-callout\" class=\"response-callout\">\n" +
            "                    <div class=\"response-header\">\n" +
            "                        <span>Command Output</span>\n" +
            "                        <span id=\"response-action-lbl\"></span>\n" +
            "                    </div>\n" +
            "                    <div id=\"response-body\" class=\"response-body\"></div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "\n" +
            "            <!-- Live Event Log Stream -->\n" +
            "            <div class=\"glass-card\">\n" +
            "                <div class=\"card-header\">\n" +
            "                    <h2>📟 Real-Time Event Log</h2>\n" +
            "                </div>\n" +
            "                <div id=\"terminal\" class=\"terminal\">[SYSTEM] Initializing console terminal...</div>\n" +
            "            </div>\n" +
            "\n" +
            "        </div>\n" +
            "\n" +
            "        <div class=\"sidebar\">\n" +
            "            \n" +
            "            <!-- Active Task Details Panel -->\n" +
            "            <div class=\"glass-card\">\n" +
            "                <div class=\"card-header\">\n" +
            "                    <h2>🎯 Active Task Details</h2>\n" +
            "                </div>\n" +
            "                <div id=\"active-task-container\" class=\"active-task-wrapper\">\n" +
            "                    <div class=\"empty-state\">No active task currently processing</div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "\n" +
            "            <!-- Task Queue List -->\n" +
            "            <div class=\"glass-card\">\n" +
            "                <div class=\"card-header\">\n" +
            "                    <h2>📋 Pending Queue</h2>\n" +
            "                </div>\n" +
            "                <div id=\"queue-list\" class=\"task-list\">\n" +
            "                    <div class=\"empty-state\">Queue is currently empty</div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "\n" +
            "            <!-- Task History List -->\n" +
            "            <div class=\"glass-card\">\n" +
            "                <div class=\"card-header\">\n" +
            "                    <h2>🕒 Run History</h2>\n" +
            "                </div>\n" +
            "                <div id=\"history-list\" class=\"task-list\">\n" +
            "                    <div class=\"empty-state\">No tasks in history</div>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "\n" +
            "        </div>\n" +
            "\n" +
            "    </div>\n" +
            "\n" +
            "    <script>\n" +
            "        // State tracking\n" +
            "        let isSupervisorAlive = false;\n" +
            "\n" +
            "        async function updateDashboardData() {\n" +
            "            try {\n" +
            "                // 1. Fetch State\n" +
            "                const stateRes = await fetch('/api/state');\n" +
            "                if (stateRes.ok) {\n" +
            "                    const state = await stateRes.json();\n" +
            "                    renderState(state);\n" +
            "                }\n" +
            "            } catch (e) {\n" +
            "                console.error(\"Error updating state data:\", e);\n" +
            "            }\n" +
            "\n" +
            "            try {\n" +
            "                // 2. Fetch Logs\n" +
            "                const logsRes = await fetch('/api/logs');\n" +
            "                if (logsRes.ok) {\n" +
            "                    const data = await logsRes.json();\n" +
            "                    const term = document.getElementById('terminal');\n" +
            "                    // Keep scrolled to bottom if user is already at the bottom\n" +
            "                    const shouldScroll = term.scrollHeight - term.clientHeight <= term.scrollTop + 30;\n" +
            "                    term.textContent = data.logs || \"[SYSTEM] Empty log output.\";\n" +
            "                    if (shouldScroll) {\n" +
            "                        term.scrollTop = term.scrollHeight;\n" +
            "                    }\n" +
            "                }\n" +
            "            } catch (e) {\n" +
            "                console.error(\"Error updating logs:\", e);\n" +
            "            }\n" +
            "\n" +
            "            // 3. Ping Supervisor API (8089) for status dot\n" +
            "            try {\n" +
            "                const pingRes = await fetch('/api/action?action=ping');\n" +
            "                const dot = document.getElementById('supervisor-status-dot');\n" +
            "                if (pingRes.ok) {\n" +
            "                    const pingData = await pingRes.json();\n" +
            "                    if (pingData && pingData.status === \"OK\") {\n" +
            "                        dot.className = \"status-dot online\";\n" +
            "                        isSupervisorAlive = true;\n" +
            "                    } else {\n" +
            "                        dot.className = \"status-dot\";\n" +
            "                        isSupervisorAlive = false;\n" +
            "                    }\n" +
            "                } else {\n" +
            "                    dot.className = \"status-dot\";\n" +
            "                    isSupervisorAlive = false;\n" +
            "                }\n" +
            "            } catch (e) {\n" +
            "                document.getElementById('supervisor-status-dot').className = \"status-dot\";\n" +
            "                isSupervisorAlive = false;\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        function renderState(state) {\n" +
            "            // Update Stats counters\n" +
            "            document.getElementById('stat-queue').textContent = state.taskQueue ? state.taskQueue.length : 0;\n" +
            "            document.getElementById('stat-completed').textContent = state.taskHistory ? state.taskHistory.filter(t => t.status === 'COMPLETED').length : 0;\n" +
            "            document.getElementById('stat-failures').textContent = state.consecutiveFailures || 0;\n" +
            "\n" +
            "            // Update Active Task\n" +
            "            const activeContainer = document.getElementById('active-task-container');\n" +
            "            if (state.currentTask) {\n" +
            "                const t = state.currentTask;\n" +
            "                const statusClass = t.status === 'EVO_RUNNING' || t.status === 'RUNNING' || t.status === 'BUILDING' || t.status === 'TESTING' || t.status === 'VERIFYING' ? 'status-running' : 'status-idle';\n" +
            "                \n" +
            "                let scopeHtml = \"\";\n" +
            "                if (t.scope && t.scope.length > 0) {\n" +
            "                    scopeHtml = `<div class=\"meta-entry\" style=\"grid-column: span 2;\">\n" +
            "                        <div class=\"lbl\">Scope / Affected Modules</div>\n" +
            "                        <div style=\"font-size:11px; font-family: monospace; color: #a5b4fc;\">${t.scope.join(', ')}</div>\n" +
            "                    </div>`;\n" +
            "                }\n" +
            "\n" +
            "                activeContainer.innerHTML = `\n" +
            "                    <div class=\"task-title-row\">\n" +
            "                        <span class=\"task-id\">${t.id}</span>\n" +
            "                        <span class=\"task-status-lbl ${statusClass}\">${t.status}</span>\n" +
            "                    </div>\n" +
            "                    <div class=\"task-desc\">${t.objective}</div>\n" +
            "                    <div class=\"task-meta-grid\">\n" +
            "                        <div class=\"meta-entry\">\n" +
            "                            <div class=\"lbl\">Attempts</div>\n" +
            "                            <div style=\"font-weight:600;\">${t.attempts || 0} / ${state.maxTaskAttempts}</div>\n" +
            "                        </div>\n" +
            "                        <div class=\"meta-entry\">\n" +
            "                            <div class=\"lbl\">Parent Task</div>\n" +
            "                            <div>${t.parentTaskId || 'None'}</div>\n" +
            "                        </div>\n" +
            "                        ${scopeHtml}\n" +
            "                    </div>\n" +
            "                `;\n" +
            "            } else {\n" +
            "                activeContainer.innerHTML = `<div class=\"empty-state\">No active task currently processing</div>`;\n" +
            "            }\n" +
            "\n" +
            "            // Update Queue List\n" +
            "            const queueList = document.getElementById('queue-list');\n" +
            "            if (state.taskQueue && state.taskQueue.length > 0) {\n" +
            "                queueList.innerHTML = state.taskQueue.map(t => `\n" +
            "                    <div class=\"task-list-item\">\n" +
            "                        <div class=\"task-item-left\">\n" +
            "                            <span class=\"task-item-obj\">${t.objective}</span>\n" +
            "                            <span class=\"task-item-id\">${t.id}</span>\n" +
            "                        </div>\n" +
            "                        <span class=\"task-status-lbl status-idle\" style=\"font-size: 9px; padding: 2px 6px;\">${t.status}</span>\n" +
            "                    </div>\n" +
            "                `).join('');\n" +
            "            } else {\n" +
            "                queueList.innerHTML = `<div class=\"empty-state\">Queue is currently empty</div>`;\n" +
            "            }\n" +
            "\n" +
            "            // Update History List\n" +
            "            const historyList = document.getElementById('history-list');\n" +
            "            if (state.taskHistory && state.taskHistory.length > 0) {\n" +
            "                historyList.innerHTML = state.taskHistory.slice().reverse().map(t => {\n" +
            "                    const statusClass = t.status === 'COMPLETED' ? 'status-success' : 'status-failed';\n" +
            "                    return `\n" +
            "                        <div class=\"task-list-item\">\n" +
            "                            <div class=\"task-item-left\">\n" +
            "                                <span class=\"task-item-obj\">${t.objective}</span>\n" +
            "                                <span class=\"task-item-id\">${t.id}</span>\n" +
            "                            </div>\n" +
            "                            <span class=\"task-status-lbl ${statusClass}\" style=\"font-size: 9px; padding: 2px 6px;\">${t.status}</span>\n" +
            "                        </div>\n" +
            "                    `;\n" +
            "                }).join('');\n" +
            "            } else {\n" +
            "                historyList.innerHTML = `<div class=\"empty-state\">No tasks in history</div>`;\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        async function triggerAction(action, btnElement) {\n" +
            "            const originalText = btnElement.innerHTML;\n" +
            "            btnElement.disabled = true;\n" +
            "            btnElement.innerHTML = `<span class=\"spinner\"></span> Processing...`;\n" +
            "\n" +
            "            const callout = document.getElementById('response-callout');\n" +
            "            const headerLbl = document.getElementById('response-action-lbl');\n" +
            "            const body = document.getElementById('response-body');\n" +
            "\n" +
            "            // Visual response cleanup\n" +
            "            callout.style.display = 'block';\n" +
            "            headerLbl.textContent = action.toUpperCase();\n" +
            "            body.textContent = \"Executing API operation... Please wait.\";\n" +
            "\n" +
            "            try {\n" +
            "                const res = await fetch(`/api/action?action=${action}`);\n" +
            "                const text = await res.text();\n" +
            "                let prettyText = text;\n" +
            "                try {\n" +
            "                    // Attempt to format JSON pretty if possible\n" +
            "                    const obj = JSON.parse(text);\n" +
            "                    prettyText = JSON.stringify(obj, null, 4);\n" +
            "                } catch(e) {}\n" +
            "                \n" +
            "                body.textContent = prettyText || \"Action executed successfully (No output returned).\";\n" +
            "            } catch (err) {\n" +
            "                body.textContent = \"API Error: \" + err.message;\n" +
            "            } finally {\n" +
            "                btnElement.disabled = false;\n" +
            "                btnElement.innerHTML = originalText;\n" +
            "                updateDashboardData();\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        // Loop every 2 seconds\n" +
            "        updateDashboardData();\n" +
            "        setInterval(updateDashboardData, 2000);\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }
}
