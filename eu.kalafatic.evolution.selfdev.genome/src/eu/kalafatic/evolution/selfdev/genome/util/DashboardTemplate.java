package eu.kalafatic.evolution.selfdev.genome.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class DashboardTemplate {

    public static String getHtml(String projectName, String timestamp, String architecture, String useCases, String milestone, String genomeJson) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Genome Milestone Dashboard - " + projectName + "</title>\n" +
                "    <style>\n" +
                "        :root { --accent: #007acc; --border: #cccccc; --bg-secondary: #f3f3f3; --text: #333; --text-dim: #666; }\n" +
                "        body { font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #333; max-width: 1200px; margin: 0 auto; padding: 20px; background-color: #f4f7f6; }\n" +
                "        header { border-bottom: 2px solid #007acc; padding-bottom: 10px; margin-bottom: 30px; }\n" +
                "        h1 { color: #007acc; margin: 0; }\n" +
                "        .meta { color: #666; font-size: 0.9em; margin-top: 5px; }\n" +
                "        section { background: #fff; padding: 20px; margin-bottom: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); position: relative; }\n" +
                "        h2 { border-bottom: 1px solid #eee; padding-bottom: 10px; margin-top: 0; color: #444; }\n" +
                "        pre { background: #f8f8f8; padding: 15px; border-radius: 5px; overflow-x: auto; border: 1px solid #ddd; }\n" +
                "        code { font-family: \"SFMono-Regular\", Consolas, \"Liberation Mono\", Menlo, Courier, monospace; font-size: 0.9em; }\n" +
                "        .collapsible { cursor: pointer; user-select: none; }\n" +
                "        .collapsible:after { content: ' \\25BC'; float: right; }\n" +
                "        .active:after { content: ' \\25B2'; }\n" +
                "        .content { display: block; overflow: hidden; }\n" +
                "        \n" +
                "        #genome-container { height: 500px; background: #fff; border: 1px solid var(--border); border-radius: 4px; overflow: hidden; position: relative; cursor: grab; margin-top: 15px; }\n" +
                "        #genome-svg { width: 100%; height: 100%; }\n" +
                "        #details-panel { width: 300px; background: var(--bg-secondary); border-left: 1px solid var(--border); position: absolute; right: 0; top: 0; bottom: 0; z-index: 10; transform: translateX(100%); transition: transform 0.3s; box-shadow: -2px 0 5px rgba(0,0,0,0.1); display: flex; flex-direction: column; }\n" +
                "        #details-panel.active { transform: translateX(0); }\n" +
                "        .panel-header { padding: 10px; border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }\n" +
                "        .panel-body { padding: 10px; flex: 1; overflow-y: auto; font-size: 0.85em; }\n" +
                "        .type-badge { font-size: 0.7em; background: #444; color: #fff; padding: 2px 5px; border-radius: 3px; font-weight: bold; }\n" +
                "        .zoom-btn { width: 30px; height: 30px; background: #fff; border: 1px solid var(--border); border-radius: 4px; cursor: pointer; display: flex; align-items: center; justify-content: center; font-weight: bold; margin-bottom: 5px; }\n" +
                "        #zoom-controls { position: absolute; bottom: 10px; right: 10px; z-index: 10; }\n" +
                "    </style>\n" +
                "    <script src=\"/auth-integration.js\"></script>\n" +
                "    <link rel=\"stylesheet\" href=\"/genome.css\">\n" +
                "</head>\n" +
                "<body>\n" +
                "    <header>\n" +
                "        <h1>Genome Milestone Dashboard</h1>\n" +
                "        <div class=\"meta\">Project: " + projectName + " | Snapshot: genome_" + timestamp + "</div>\n" +
                "    </header>\n" +
                "\n" +
                "    <section>\n" +
                "        <h2 class=\"collapsible active\">Interactive Genome Map</h2>\n" +
                "        <div class=\"content\">\n" +
                "            <div id=\"genome-container\">\n" +
                "                <svg id=\"genome-svg\">\n" +
                "                    <defs>\n" +
                "                        <marker id=\"arrowhead\" viewBox=\"-0 -5 10 10\" refX=\"25\" refY=\"0\" orient=\"auto\" markerWidth=\"6\" markerHeight=\"6\" xoverflow=\"visible\">\n" +
                "                            <path d=\"M 0,-5 L 10 ,0 L 0,5\" fill=\"#999\" style=\"stroke: none;\"></path>\n" +
                "                        </marker>\n" +
                "                    </defs>\n" +
                "                </svg>\n" +
                "                <div id=\"zoom-controls\">\n" +
                "                    <button class=\"zoom-btn\" onclick=\"window.zoomIn()\">+</button>\n" +
                "                    <button class=\"zoom-btn\" onclick=\"window.zoomOut()\">-</button>\n" +
                "                    <button class=\"zoom-btn\" onclick=\"window.resetZoom()\">⊙</button>\n" +
                "                </div>\n" +
                "                <div id=\"details-panel\"></div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </section>\n" +
                "\n" +
                "    <section>\n" +
                "        <h2 class=\"collapsible\">Architecture Overview</h2>\n" +
                "        <div class=\"content\" style=\"display:none;\">" + architecture + "</div>\n" +
                "    </section>\n" +
                "\n" +
                "    <section>\n" +
                "        <h2 class=\"collapsible\">Use Cases and Behaviors</h2>\n" +
                "        <div class=\"content\" style=\"display:none;\">" + useCases + "</div>\n" +
                "    </section>\n" +
                "\n" +
                "    <section>\n" +
                "        <h2 class=\"collapsible\">Milestone Freeze Point</h2>\n" +
                "        <div class=\"content\" style=\"display:none;\">" + milestone + "</div>\n" +
                "    </section>\n" +
                "\n" +
                "    <section>\n" +
                "        <h2 class=\"collapsible\">Genome JSON Summary</h2>\n" +
                "        <div class=\"content\" style=\"display:none;\">\n" +
                "            <pre><code>" + genomeJson + "</code></pre>\n" +
                "        </div>\n" +
                "    </section>\n" +
                "\n" +
                "    <script src=\"/genome.js\"></script>\n" +
                "    <script>\n" +
                "        window.onload = function() {\n" +
                "            if (window.updateGenome) {\n" +
                "                window.updateGenome(" + genomeJson + ");\n" +
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
