package eu.kalafatic.evolution.media.render;

import eu.kalafatic.evolution.media.model.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class SvgRenderer {

    public String render(Diagram diagram) {
        StringBuilder svg = new StringBuilder();
        int width = 1200;
        int height = 800;

        svg.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\">\n", width, height));
        svg.append("<rect width=\"100%\" height=\"100%\" fill=\"#f8f9fa\"/>\n");

        Map<String, Point> positions = new HashMap<>();
        List<Node> nodes = diagram.getNodes();
        int n = nodes.size();

        // Basic layout: grid or circle
        for (int i = 0; i < n; i++) {
            Node node = nodes.get(i);
            double angle = 2 * Math.PI * i / n;
            int cx = (int) (width / 2 + 300 * Math.cos(angle));
            int cy = (int) (height / 2 + 300 * Math.sin(angle));
            positions.put(node.getId(), new Point(cx, cy));
        }

        // Render edges first so they are behind nodes
        for (Edge edge : diagram.getEdges()) {
            Point p1 = positions.get(edge.getSourceId());
            Point p2 = positions.get(edge.getTargetId());
            if (p1 != null && p2 != null) {
                svg.append(String.format("<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#999\" stroke-width=\"1.5\" stroke-opacity=\"0.6\" />\n", p1.x, p1.y, p2.x, p2.y));
            }
        }

        // Render nodes
        for (Node node : nodes) {
            Point p = positions.get(node.getId());
            svg.append(String.format("<circle cx=\"%d\" cy=\"%d\" r=\"20\" fill=\"white\" stroke=\"#007bff\" stroke-width=\"2\" />\n", p.x, p.y));
            svg.append(String.format("<text x=\"%d\" y=\"%d\" font-family=\"sans-serif\" font-size=\"12\" text-anchor=\"middle\" fill=\"#333\">%s</text>\n", p.x, p.y + 35, escapeHtml(node.getLabel())));
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }
}
