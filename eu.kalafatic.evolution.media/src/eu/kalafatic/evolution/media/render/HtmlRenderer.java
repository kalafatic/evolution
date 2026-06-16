package eu.kalafatic.evolution.media.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.kalafatic.evolution.media.model.*;
import eu.kalafatic.evolution.media.model.Diagram;
import eu.kalafatic.evolution.media.model.Edge;
import eu.kalafatic.evolution.media.model.ImageBlock;
import eu.kalafatic.evolution.media.model.Node;
import eu.kalafatic.evolution.media.model.Section;
import eu.kalafatic.evolution.media.model.TableBlock;
import eu.kalafatic.evolution.media.model.TextBlock;

public class HtmlRenderer {

    private final ObjectMapper mapper = new ObjectMapper();

    public String render(Diagram diagram) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<title>").append(escapeHtml(diagram.getTitle())).append("</title>\n");
        appendStyles(html);
        html.append("</head>\n<body class=\"light-theme\">\n");
        html.append("<div id=\"app\">\n");
        html.append("  <header>\n");
        html.append("    <h1>").append(escapeHtml(diagram.getTitle())).append("</h1>\n");
        html.append("    <div class=\"toolbar\">\n");
        html.append("      <button onclick=\"toggleTheme()\">Toggle Theme</button>\n");
        html.append("      <button onclick=\"resetView()\">Reset View</button>\n");
        html.append("      <button onclick=\"exportSvg()\">Export SVG</button>\n");
        html.append("      <input type=\"text\" id=\"search\" placeholder=\"Search nodes...\" onkeyup=\"searchNodes()\">\n");
        html.append("    </div>\n");
        html.append("  </header>\n");

        html.append("  <main>\n");
        html.append("    <div id=\"canvas-container\">\n");
        html.append("      <svg id=\"canvas\"></svg>\n");
        html.append("    </div>\n");

        html.append("    <aside id=\"details\">\n");
        html.append("      <div class=\"sidebar-section\">\n");
        html.append("        <h2 onclick=\"toggleSection(this)\">Selected Details <span class=\"toggle-icon\">▼</span></h2>\n");
        html.append("        <div class=\"section-content\" id=\"details-content\">\n");
        html.append("          <p>Select a node to see details.</p>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");

        for (Section section : diagram.getSections()) {
            html.append("      <div class=\"sidebar-section\">\n");
            html.append("        <h2 onclick=\"toggleSection(this)\">").append(escapeHtml(section.getTitle())).append(" <span class=\"toggle-icon\">▼</span></h2>\n");
            html.append("        <div class=\"section-content\">\n");
            for (Object block : section.getBlocks()) {
                renderBlock(html, block);
            }
            html.append("        </div>\n");
            html.append("      </div>\n");
        }

        html.append("    </aside>\n");
        html.append("  </main>\n");
        html.append("</div>\n");

        appendScripts(html, diagram);
        html.append("</body>\n</html>");
        return html.toString();
    }

    private void renderBlock(StringBuilder html, Object block) {
        if (block instanceof TextBlock tb) {
            html.append("<p>").append(escapeHtml(tb.getContent())).append("</p>\n");
        } else if (block instanceof TableBlock table) {
            html.append("<table>\n<thead><tr>");
            for (String h : table.getHeaders()) {
                html.append("<th>").append(escapeHtml(h)).append("</th>");
            }
            html.append("</tr></thead>\n<tbody>");
            for (List<String> row : table.getRows()) {
                html.append("<tr>");
                for (String cell : row) {
                    html.append("<td>").append(escapeHtml(cell)).append("</td>");
                }
                html.append("</tr>");
            }
            html.append("</tbody>\n</table>\n");
        } else if (block instanceof ImageBlock img) {
            html.append("<figure>\n");
            html.append("  <img src=\"").append(escapeHtml(img.getUrl())).append("\" alt=\"").append(escapeHtml(img.getCaption())).append("\">\n");
            html.append("  <figcaption>").append(escapeHtml(img.getCaption())).append("</figcaption>\n");
            html.append("</figure>\n");
        }
    }

    private void appendStyles(StringBuilder html) {
        html.append("<style>\n");
        html.append("  :root {\n");
        html.append("    --bg: #ffffff; --text: #212529; --accent: #007bff; --border: #dee2e6; --sidebar: #f8f9fa;\n");
        html.append("    --node-fill: #fff; --node-stroke: #007bff; --edge-stroke: #999;\n");
        html.append("  }\n");
        html.append("  body.dark-theme {\n");
        html.append("    --bg: #1a1a1a; --text: #e0e0e0; --accent: #3391ff; --border: #444; --sidebar: #252525;\n");
        html.append("    --node-fill: #333; --node-stroke: #3391ff; --edge-stroke: #666;\n");
        html.append("  }\n");
        html.append("  body { font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, Helvetica, Arial, sans-serif; margin: 0; background: var(--bg); color: var(--text); transition: background 0.3s, color 0.3s; }\n");
        html.append("  #app { display: flex; flex-direction: column; height: 100vh; }\n");
        html.append("  header { padding: 10px 20px; background: var(--sidebar); border-bottom: 1px solid var(--border); display: flex; justify-content: space-between; align-items: center; }\n");
        html.append("  h1 { margin: 0; font-size: 1.2rem; }\n");
        html.append("  main { flex: 1; display: flex; overflow: hidden; }\n");
        html.append("  #canvas-container { flex: 1; position: relative; background: var(--bg); cursor: grab; }\n");
        html.append("  #canvas { width: 100%; height: 100%; }\n");
        html.append("  #details { width: 350px; border-left: 1px solid var(--border); background: var(--sidebar); overflow-y: auto; }\n");
        html.append("  .sidebar-section { border-bottom: 1px solid var(--border); }\n");
        html.append("  .sidebar-section h2 { margin: 0; padding: 10px 15px; font-size: 1rem; cursor: pointer; display: flex; justify-content: space-between; align-items: center; background: rgba(0,0,0,0.03); }\n");
        html.append("  .sidebar-section h2:hover { background: rgba(0,0,0,0.06); }\n");
        html.append("  .section-content { padding: 15px; font-size: 0.9rem; }\n");
        html.append("  .section-content.collapsed { display: none; }\n");
        html.append("  .toggle-icon { font-size: 0.8rem; transition: transform 0.2s; }\n");
        html.append("  .collapsed .toggle-icon { transform: rotate(-90deg); }\n");
        html.append("  .node { cursor: pointer; }\n");
        html.append("  .node circle { fill: var(--node-fill); stroke: var(--node-stroke); stroke-width: 2px; }\n");
        html.append("  .node text { font-size: 12px; pointer-events: none; fill: var(--text); }\n");
        html.append("  .node.highlighted circle { stroke-width: 4px; filter: drop-shadow(0 0 5px var(--accent)); }\n");
        html.append("  .edge { stroke: var(--edge-stroke); stroke-opacity: 0.6; stroke-width: 1.5px; fill: none; }\n");
        html.append("  .edge.highlighted { stroke: var(--accent); stroke-opacity: 1; stroke-width: 3px; }\n");
        html.append("  .toolbar button, .toolbar input { padding: 5px 10px; margin-left: 5px; border: 1px solid var(--border); background: var(--bg); color: var(--text); border-radius: 4px; }\n");
        html.append("  table { width: 100%; border-collapse: collapse; margin-top: 10px; }\n");
        html.append("  th, td { border: 1px solid var(--border); padding: 8px; text-align: left; }\n");
        html.append("  th { background: rgba(0,0,0,0.05); }\n");
        html.append("  img { max-width: 100%; height: auto; }\n");
        html.append("</style>\n");
    }

    private void appendScripts(StringBuilder html, Diagram diagram) {
        html.append("<script>\n");

        Map<String, Object> data = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Node n : diagram.getNodes()) {
            Map<String, Object> nodeMap = new HashMap<>();
            nodeMap.put("id", n.getId());
            nodeMap.put("label", n.getLabel());
            nodeMap.put("type", n.getType());
            nodeMap.put("properties", n.getProperties());
            nodes.add(nodeMap);
        }

        List<Map<String, Object>> links = new ArrayList<>();
        for (Edge e : diagram.getEdges()) {
            Map<String, Object> linkMap = new HashMap<>();
            linkMap.put("source", e.getSourceId());
            linkMap.put("target", e.getTargetId());
            linkMap.put("label", e.getLabel());
            linkMap.put("type", e.getType());
            links.add(linkMap);
        }

        data.put("nodes", nodes);
        data.put("links", links);

        try {
            html.append("const data = ").append(mapper.writeValueAsString(data)).append(";\n");
        } catch (Exception e) {
            html.append("const data = {nodes:[], links:[]};\n");
        }

        html.append("""
            // Minimal Force-Directed Layout Implementation
            const nodes = data.nodes;
            const links = data.links;
            const width = window.innerWidth - 350;
            const height = window.innerHeight - 60;

            // Initialize positions
            nodes.forEach(n => {
                n.x = Math.random() * width;
                n.y = Math.random() * height;
                n.vx = 0; n.vy = 0;
            });

            const svg = document.getElementById('canvas');
            const g = document.createElementNS("http://www.w3.org/2000/svg", "g");
            svg.appendChild(g);

            // Zoom/Pan variables
            let scale = 1, tx = 0, ty = 0;
            svg.addEventListener('wheel', e => {
                e.preventDefault();
                const delta = e.deltaY > 0 ? 0.9 : 1.1;
                scale *= delta;
                updateTransform();
            });

            let isDraggingCanvas = false;
            let lastMouseX, lastMouseY;
            svg.addEventListener('mousedown', e => {
                if (e.target === svg) {
                    isDraggingCanvas = true;
                    lastMouseX = e.clientX;
                    lastMouseY = e.clientY;
                }
            });
            window.addEventListener('mousemove', e => {
                if (isDraggingCanvas) {
                    tx += e.clientX - lastMouseX;
                    ty += e.clientY - lastMouseY;
                    lastMouseX = e.clientX;
                    lastMouseY = e.clientY;
                    updateTransform();
                }
            });
            window.addEventListener('mouseup', () => isDraggingCanvas = false);

            function updateTransform() {
                g.setAttribute("transform", `translate(${tx},${ty}) scale(${scale})`);
            }

            // Create SVG elements
            const edgeElements = links.map(l => {
                const line = document.createElementNS("http://www.w3.org/2000/svg", "line");
                line.setAttribute("class", "edge");
                g.appendChild(line);
                return { line, link: l };
            });

            const nodeElements = nodes.map(n => {
                const group = document.createElementNS("http://www.w3.org/2000/svg", "g");
                group.setAttribute("class", "node");

                const circle = document.createElementNS("http://www.w3.org/2000/svg", "circle");
                circle.setAttribute("r", 10);
                group.appendChild(circle);

                const text = document.createElementNS("http://www.w3.org/2000/svg", "text");
                text.setAttribute("dx", 12);
                text.setAttribute("dy", "0.35em");
                text.textContent = n.label;
                group.appendChild(text);

                group.addEventListener('click', () => selectNode(n));

                g.appendChild(group);
                return { group, node: n };
            });

            function selectNode(n) {
                // Highlight node and edges
                nodeElements.forEach(ne => ne.group.classList.toggle('highlighted', ne.node.id === n.id));
                edgeElements.forEach(ee => {
                    const isConnected = ee.link.source === n.id || ee.link.target === n.id;
                    ee.line.classList.toggle('highlighted', isConnected);
                });

                // Show details
                let html = `<h3>${escapeHtml(n.label)}</h3><p>Type: ${escapeHtml(n.type)}</p>`;
                if (n.properties) {
                    html += "<ul>";
                    for (const [k, v] of Object.entries(n.properties)) {
                        html += `<li><strong>${escapeHtml(k)}:</strong> ${escapeHtml(v)}</li>`;
                    }
                    html += "</ul>";
                }
                document.getElementById("details-content").innerHTML = html;
            }

            function escapeHtml(text) {
                const p = document.createElement('p');
                p.textContent = text;
                return p.innerHTML;
            }

            // Simple Simulation Loop
            function tick() {
                const k = 0.05;
                const charge = 500;

                // Repulsion
                for (let i = 0; i < nodes.length; i++) {
                    for (let j = i + 1; j < nodes.length; j++) {
                        const dx = nodes[i].x - nodes[j].x;
                        const dy = nodes[i].y - nodes[j].y;
                        const d2 = dx * dx + dy * dy + 0.1;
                        const force = charge / d2;
                        const fx = force * dx / Math.sqrt(d2);
                        const fy = force * dy / Math.sqrt(d2);
                        nodes[i].vx += fx; nodes[i].vy += fy;
                        nodes[j].vx -= fx; nodes[j].vy -= fy;
                    }
                }

                // Attraction
                links.forEach(l => {
                    const s = nodes.find(n => n.id === l.source);
                    const t = nodes.find(n => n.id === l.target);
                    if (!s || !t) return;
                    const dx = t.x - s.x;
                    const dy = t.y - s.y;
                    const dist = Math.sqrt(dx * dx + dy * dy);
                    const force = (dist - 100) * k;
                    const fx = force * dx / dist;
                    const fy = force * dy / dist;
                    s.vx += fx; s.vy += fy;
                    t.vx -= fx; t.vy -= fy;
                });

                // Apply velocity and damping
                nodes.forEach(n => {
                    n.x += n.vx; n.y += n.vy;
                    n.vx *= 0.6; n.vy *= 0.6;

                    // Keep in bounds (mostly)
                    if (n.x < 0) n.x = 0; if (n.x > width) n.x = width;
                    if (n.y < 0) n.y = 0; if (n.y > height) n.y = height;
                });

                // Update positions
                edgeElements.forEach(ee => {
                    const s = nodes.find(n => n.id === ee.link.source);
                    const t = nodes.find(n => n.id === ee.link.target);
                    if (s && t) {
                        ee.line.setAttribute("x1", s.x);
                        ee.line.setAttribute("y1", s.y);
                        ee.line.setAttribute("x2", t.x);
                        ee.line.setAttribute("y2", t.y);
                    }
                });
                nodeElements.forEach(ne => {
                    ne.group.setAttribute("transform", `translate(${ne.node.x},${ne.node.y})`);
                });

                requestAnimationFrame(tick);
            }
            tick();

            // UI Actions
            function toggleTheme() {
                document.body.classList.toggle('dark-theme');
                document.body.classList.toggle('light-theme');
            }
            function resetView() {
                tx = 0; ty = 0; scale = 1; updateTransform();
            }
            function toggleSection(header) {
                header.parentElement.querySelector('.section-content').classList.toggle('collapsed');
                header.parentElement.classList.toggle('collapsed');
            }
            function searchNodes() {
                const term = document.getElementById("search").value.toLowerCase();
                nodeElements.forEach(ne => {
                    ne.group.style.opacity = ne.node.label.toLowerCase().includes(term) ? 1 : 0.1;
                });
            }
            function exportSvg() {
                const svgData = document.getElementById("canvas").outerHTML;
                const svgBlob = new Blob([svgData], {type:"image/svg+xml;charset=utf-8"});
                const svgUrl = URL.createObjectURL(svgBlob);
                const downloadLink = document.createElement("a");
                downloadLink.href = svgUrl;
                downloadLink.download = "diagram.svg";
                document.body.appendChild(downloadLink);
                downloadLink.click();
                document.body.removeChild(downloadLink);
            }
            """);
        html.append("</script>\n");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
