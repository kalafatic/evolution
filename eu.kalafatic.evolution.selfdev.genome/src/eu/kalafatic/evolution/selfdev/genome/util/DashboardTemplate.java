package eu.kalafatic.evolution.selfdev.genome.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DashboardTemplate {

    public static String getHtml(String projectName, String timestamp, String architecture, String useCases, String milestone, String genomeJson) {
        return getHtml(projectName, timestamp, architecture, useCases, milestone, genomeJson, "");
    }

    public static String getHtml(String projectName, String timestamp, String architecture, String useCases, String milestone, String genomeJson, String baseUrl) {
        if (baseUrl == null) baseUrl = "";
        if (!baseUrl.isEmpty() && !baseUrl.endsWith("/")) baseUrl += "/";

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Genome Milestone Dashboard - " + projectName + "</title>\n" +
                "    <style>\n" +
                "        :root { --accent: #007acc; --border: #cccccc; --bg-secondary: #f3f3f3; --text: #333; --text-dim: #666; --bg-primary: #ffffff; }\n" +
                "        body, html { margin: 0; padding: 0; height: 100%; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: var(--bg-primary); color: var(--text); overflow: hidden; }\n" +
                "        #layout { display: flex; height: 100%; }\n" +
                "        #sidebar { width: 260px; min-width: 260px; background-color: var(--bg-secondary); border-right: 1px solid var(--border); display: flex; flex-direction: column; padding: 15px; box-sizing: border-box; z-index: 100; overflow-y: auto; }\n" +
                "        #sidebar h1 { font-size: 1.2em; margin: 0 0 20px 0; color: var(--accent); font-weight: bold; }\n" +
                "        .sidebar-section { margin-bottom: 20px; padding-bottom: 15px; border-bottom: 1px solid var(--border); }\n" +
                "        .sidebar-section h2 { font-size: 0.75em; text-transform: uppercase; color: var(--text-dim); margin-bottom: 10px; letter-spacing: 1px; }\n" +
                "        .control-group { display: flex; flex-direction: column; gap: 8px; }\n" +
                "        .btn { background-color: #ffffff; color: #333; border: 1px solid var(--border); padding: 6px 12px; cursor: pointer; border-radius: 2px; font-size: 0.85em; text-align: left; }\n" +
                "        .btn:hover { background-color: #f0f0f0; }\n" +
                "        .btn-primary { background-color: var(--accent); color: white; text-align: center; font-weight: bold; border-color: var(--accent); }\n" +
                "        select, input { width: 100%; background: #ffffff; border: 1px solid var(--border); padding: 6px; box-sizing: border-box; font-size: 0.85em; border-radius: 2px; }\n" +
                "        #main-container { flex-grow: 1; display: flex; flex-direction: column; position: relative; overflow-y: auto; background-color: #f4f7f6; }\n" +
                "        header { background: var(--bg-primary); border-bottom: 2px solid var(--accent); padding: 15px 20px; margin-bottom: 0; }\n" +
                "        h1 { color: var(--accent); margin: 0; font-size: 1.5em; }\n" +
                "        .meta { color: var(--text-dim); font-size: 0.85em; margin-top: 5px; }\n" +
                "        .dashboard-content { padding: 20px; max-width: 1200px; margin: 0 auto; width: 100%; box-sizing: border-box; }\n" +
                "        section { background: #fff; padding: 20px; margin-bottom: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); position: relative; }\n" +
                "        h2 { border-bottom: 1px solid #eee; padding-bottom: 10px; margin-top: 0; color: #444; font-size: 1.1em; }\n" +
                "        pre { background: #f8f8f8; padding: 15px; border-radius: 5px; overflow-x: auto; border: 1px solid #ddd; }\n" +
                "        .collapsible { cursor: pointer; user-select: none; }\n" +
                "        .collapsible:after { content: ' \\25BC'; float: right; }\n" +
                "        .active:after { content: ' \\25B2'; }\n" +
                "        .content { display: block; overflow: hidden; }\n" +
                "        #genome-container { height: 600px; background: #fff; border: 1px solid var(--border); border-radius: 4px; overflow: hidden; position: relative; cursor: grab; margin-top: 15px; }\n" +
                "        #genome-svg { width: 100%; height: 100%; }\n" +
                "        #details-panel { width: 300px; background: var(--bg-secondary); border-left: 1px solid var(--border); position: absolute; right: 0; top: 0; bottom: 0; z-index: 10; transform: translateX(100%); transition: transform 0.3s; box-shadow: -2px 0 5px rgba(0,0,0,0.1); display: flex; flex-direction: column; }\n" +
                "        #details-panel.active { transform: translateX(0); }\n" +
                "        .panel-header { padding: 10px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }\n" +
                "        .panel-body { padding: 10px; flex: 1; overflow-y: auto; font-size: 0.85em; }\n" +
                "        .zoom-btn { width: 30px; height: 30px; background: #fff; border: 1px solid var(--border); border-radius: 4px; cursor: pointer; display: flex; align-items: center; justify-content: center; font-weight: bold; margin-bottom: 5px; }\n" +
                "        #zoom-controls { position: absolute; bottom: 10px; right: 10px; z-index: 10; }\n" +
                "    </style>\n" +
                "    <script src=\"" + baseUrl + "auth-integration.js\"></script>\n" +
                "    <link rel=\"stylesheet\" href=\"" + baseUrl + "genome.css\">\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"layout\">\n" +
                "        <div id=\"sidebar\">\n" +
                "            <h1>🧬 Architecture</h1>\n" +
                "            <div class=\"sidebar-section\">\n" +
                "                <h2>View</h2>\n" +
                "                <div class=\"control-group\">\n" +
                "                    <select id=\"mode-select\" onchange=\"javaAction(this.value, 'SET_VIEW_MODE')\">\n" +
                "                        <option value=\"COMPONENTS\">Components Map</option>\n" +
                "                        <option value=\"SUBSYSTEMS\">Subsystems View</option>\n" +
                "                        <option value=\"USE_CASES\">Use Cases Trace</option>\n" +
                "                        <option value=\"KNOWLEDGE_GRAPH\">Knowledge Graph</option>\n" +
                "                        <option value=\"GENOME\" selected>Genome View</option>\n" +
                "                    </select>\n" +
                "                    <button class=\"btn\" onclick=\"javaAction('', 'REFRESH')\">🔄 Refresh View</button>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            <div class=\"sidebar-section\">\n" +
                "                <h2>Search & Filter</h2>\n" +
                "                <div class=\"control-group\">\n" +
                "                    <input type=\"text\" id=\"search-input\" placeholder=\"Search nodes...\" oninput=\"window.searchNodes(this.value)\">\n" +
                "                    <select id=\"filter-type\" onchange=\"window.filterNodes(this.value)\">\n" +
                "                        <option value=\"ALL\">All Categories</option>\n" +
                "                        <option value=\"CORE\">Core</option>\n" +
                "                        <option value=\"VIEW\">Architecture</option>\n" +
                "                        <option value=\"STORE\">Knowledge</option>\n" +
                "                        <option value=\"BRIDGE\">Integrations</option>\n" +
                "                        <option value=\"FLOW\">Evolution</option>\n" +
                "                        <option value=\"DATA\">Metadata</option>\n" +
                "                    </select>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "            <div style=\"margin-top: auto; font-size: 10px; opacity: 0.5;\">\n" +
                "                <span>Genome Dashboard v1.1.0</span>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div id=\"main-container\">\n" +
                "            <header>\n" +
                "                <h1>Genome Milestone Dashboard</h1>\n" +
                "                <div class=\"meta\">Project: " + projectName + " | Snapshot: " + timestamp + "</div>\n" +
                "            </header>\n" +
                "\n" +
                "            <div class=\"dashboard-content\">\n" +
                "                <section>\n" +
                "                    <h2 class=\"collapsible active\">Interactive Genome Map</h2>\n" +
                "                    <div class=\"content\">\n" +
                "                        <div id=\"genome-container\">\n" +
                "                            <svg id=\"genome-svg\">\n" +
                "                                <defs>\n" +
                "                                    <marker id=\"arrowhead\" viewBox=\"-0 -5 10 10\" refX=\"25\" refY=\"0\" orient=\"auto\" markerWidth=\"6\" markerHeight=\"6\" xoverflow=\"visible\">\n" +
                "                                        <path d=\"M 0,-5 L 10 ,0 L 0,5\" fill=\"#999\" style=\"stroke: none;\"></path>\n" +
                "                                    </marker>\n" +
                "                                </defs>\n" +
                "                            </svg>\n" +
                "                            <div id=\"zoom-controls\">\n" +
                "                                <button class=\"zoom-btn\" onclick=\"window.zoomIn()\">+</button>\n" +
                "                                <button class=\"zoom-btn\" onclick=\"window.zoomOut()\">-</button>\n" +
                "                                <button class=\"zoom-btn\" onclick=\"window.resetZoom()\">⊙</button>\n" +
                "                            </div>\n" +
                "                            <div id=\"details-panel\"></div>\n" +
                "                        </div>\n" +
                "                    </div>\n" +
                "                </section>\n" +
                "\n" +
                "                <section>\n" +
                "                    <h2 class=\"collapsible\">Architecture Overview</h2>\n" +
                "                    <div class=\"content\" style=\"display:none;\">" + architecture + "</div>\n" +
                "                </section>\n" +
                "\n" +
                "                <section>\n" +
                "                    <h2 class=\"collapsible\">Use Cases and Behaviors</h2>\n" +
                "                    <div class=\"content\" style=\"display:none;\">" + useCases + "</div>\n" +
                "                </section>\n" +
                "\n" +
                "                <section>\n" +
                "                    <h2 class=\"collapsible\">Milestone Freeze Point</h2>\n" +
                "                    <div class=\"content\" style=\"display:none;\">" + milestone + "</div>\n" +
                "                </section>\n" +
                "\n" +
                "                <section>\n" +
                "                    <h2 class=\"collapsible\">Genome JSON Summary</h2>\n" +
                "                    <div class=\"content\" style=\"display:none;\">\n" +
                "                        <pre><code>" + genomeJson + "</code></pre>\n" +
                "                    </div>\n" +
                "                </section>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script src=\"" + baseUrl + "genome.js\"></script>\n" +
                "    <script>\n" +
                "        function log(msg) { if (window.logFunction) window.logFunction(msg); else console.log(msg); }\n" +
                "        function javaAction(id, action) { if (window.navigatorFunction) window.navigatorFunction(id, action); else log(\"Java action (Offline): \" + id + \" \" + action); }\n" +
                "\n" +
                "        window.onload = function() {\n" +
                "            if (window.updateGenome) {\n" +
                "                window.updateGenome(" + (genomeJson != null && !genomeJson.isEmpty() ? genomeJson : "{}") + ");\n" +
                "            }\n" +
                "            \n" +
                "            var coll = document.getElementsByClassName(\"collapsible\");\n" +
                "            for (var i = 0; i < coll.length; i++) {\n" +
                "                coll[i].addEventListener(\"click\", function() {\n" +
                "                    this.classList.toggle(\"active\");\n" +
                "                    var content = this.nextElementSibling;\n" +
                "                    if (content.style.display === \"block\") {\n" +
                "                        content.style.display = \"none\";\n" +
                "                    } else {\n" +
                "                        content.style.display = \"block\";\n" +
                "                    }\n" +
                "                });\n" +
                "            }\n" +
                "        };\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
