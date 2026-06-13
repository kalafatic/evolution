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
                "        body { font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #333; max-width: 1000px; margin: 0 auto; padding: 20px; background-color: #f4f7f6; }\n" +
                "        header { border-bottom: 2px solid #007acc; padding-bottom: 10px; margin-bottom: 30px; }\n" +
                "        h1 { color: #007acc; margin: 0; }\n" +
                "        .meta { color: #666; font-size: 0.9em; margin-top: 5px; }\n" +
                "        section { background: #fff; padding: 20px; margin-bottom: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
                "        h2 { border-bottom: 1px solid #eee; padding-bottom: 10px; margin-top: 0; color: #444; }\n" +
                "        pre { background: #f8f8f8; padding: 15px; border-radius: 5px; overflow-x: auto; border: 1px solid #ddd; }\n" +
                "        code { font-family: \"SFMono-Regular\", Consolas, \"Liberation Mono\", Menlo, Courier, monospace; font-size: 0.9em; }\n" +
                "        .collapsible { cursor: pointer; user-select: none; }\n" +
                "        .collapsible:after { content: ' \\25BC'; float: right; }\n" +
                "        .active:after { content: ' \\25B2'; }\n" +
                "        .content { display: block; overflow: hidden; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <header>\n" +
                "        <h1>Genome Milestone Dashboard</h1>\n" +
                "        <div class=\"meta\">Project: " + projectName + " | Snapshot: genome_" + timestamp + "</div>\n" +
                "    </header>\n" +
                "\n" +
                "    <section>\n" +
                "        <h2 class=\"collapsible active\">Architecture Overview</h2>\n" +
                "        <div class=\"content\">" + architecture + "</div>\n" +
                "    </section>\n" +
                "\n" +
                "    <section>\n" +
                "        <h2 class=\"collapsible active\">Use Cases and Behaviors</h2>\n" +
                "        <div class=\"content\">" + useCases + "</div>\n" +
                "    </section>\n" +
                "\n" +
                "    <section>\n" +
                "        <h2 class=\"collapsible active\">Milestone Freeze Point</h2>\n" +
                "        <div class=\"content\">" + milestone + "</div>\n" +
                "    </section>\n" +
                "\n" +
                "    <section>\n" +
                "        <h2 class=\"collapsible\">Genome JSON Summary</h2>\n" +
                "        <div class=\"content\" style=\"display:none;\">\n" +
                "            <pre><code>" + genomeJson + "</code></pre>\n" +
                "        </div>\n" +
                "    </section>\n" +
                "\n" +
                "    <script>\n" +
                "        var coll = document.getElementsByClassName(\"collapsible\");\n" +
                "        for (var i = 0; i < coll.length; i++) {\n" +
                "            coll[i].addEventListener(\"click\", function() {\n" +
                "                this.classList.toggle(\"active\");\n" +
                "                var content = this.nextElementSibling;\n" +
                "                if (content.style.display === \"block\" || content.style.display === \"\") {\n" +
                "                    content.style.display = \"none\";\n" +
                "                } else {\n" +
                "                    content.style.display = \"block\";\n" +
                "                }\n" +
                "            });\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
