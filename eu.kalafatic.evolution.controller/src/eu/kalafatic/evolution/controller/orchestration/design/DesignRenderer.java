package eu.kalafatic.evolution.controller.orchestration.design;

import java.util.HashMap;
import java.util.Map;
import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;

/**
 * @evo:20:A reason=rich-design-renderer
 */
public class DesignRenderer {

    public String render(DesignModel model) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>");
        html.append("html, body { font-family: 'Segoe UI', sans-serif; background-color: #f1f5f9; padding: 0; margin: 0; width: 100%; height: 100%; overflow: hidden; }");
        html.append(".canvas { position: relative; width: 100%; height: 100%; background: #f8fafc; overflow: auto; padding: 100px; box-sizing: border-box; border: 1px solid #e2e8f0; }");
        html.append(".component { position: absolute; min-width: 200px; background: white; border: 1px solid #cbd5e1; border-radius: 8px; box-shadow: 0 10px 15px -3px rgba(0,0,0,0.1); z-index: 10; overflow: hidden; transition: transform 0.2s; }");
        html.append(".component:hover { transform: scale(1.02); border-color: #3b82f6; box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1); }");
        html.append(".comp-header { background: #f1f5f9; border-bottom: 1px solid #e2e8f0; padding: 12px; text-align: center; }");
        html.append(".comp-name { font-weight: 800; color: #1e293b; font-size: 1.1em; }");
        html.append(".comp-type { font-size: 0.75em; color: #64748b; text-transform: uppercase; font-style: italic; }");
        html.append(".comp-content { padding: 8px; font-size: 0.85em; color: #334155; }");
        html.append(".comp-section { border-top: 1px solid #f1f5f9; margin-top: 4px; padding-top: 4px; }");
        html.append(".item { padding: 2px 0; }");
        html.append(".item::before { content: '• '; color: #94a3b8; }");
        html.append(".relationship { position: absolute; height: 2px; background: #94a3b8; transform-origin: 0 0; z-index: 5; opacity: 0.6; }");
        html.append(".relationship::after { content: ''; position: absolute; right: -4px; top: -3.5px; border: 5px solid transparent; border-left-color: #94a3b8; }");
        html.append(".header { text-align: center; margin-bottom: 30px; }");
        html.append("h1 { color: #0f172a; margin: 0; font-size: 1.8em; }");
        html.append("p { color: #475569; margin-top: 8px; }");
        html.append("</style></head><body>");

        html.append("<div class='header'>");
        html.append("<h1>").append(model.getName() != null ? model.getName() : "Evolution Design").append("</h1>");
        html.append("<p>Dynamic architecture visualization from active context</p>");
        html.append("</div>");

        html.append("<div class='canvas'>");

        Map<String, ComponentRecord> compMap = new HashMap<>();
        for (ComponentRecord comp : model.getComponents()) {
            compMap.put(comp.getName(), comp);
            html.append("<div class='component' style='left: ").append(comp.getX()).append("px; top: ").append(comp.getY()).append("px;'>");
            html.append("<div class='comp-header'>");
            html.append("<div class='comp-name'>").append(comp.getName()).append("</div>");
            html.append("<div class='comp-type'>").append(comp.getType()).append("</div>");
            html.append("</div>");

            if (!comp.getProperties().isEmpty() || !comp.getMethods().isEmpty()) {
                html.append("<div class='comp-content'>");
                if (!comp.getProperties().isEmpty()) {
                    for (String prop : comp.getProperties()) {
                        html.append("<div class='item'>").append(prop).append("</div>");
                    }
                }
                if (!comp.getMethods().isEmpty()) {
                    html.append("<div class='comp-section'>");
                    for (String method : comp.getMethods()) {
                        html.append("<div class='item'>").append(method).append("</div>");
                    }
                    html.append("</div>");
                }
                html.append("</div>");
            }
            html.append("</div>");
        }

        for (RelationshipRecord rel : model.getRelationships()) {
            ComponentRecord from = compMap.get(rel.getFrom());
            ComponentRecord to = compMap.get(rel.getTo());
            if (from != null && to != null) {
                html.append(renderLink(from.getX() + 100, from.getY() + 40, to.getX() + 100, to.getY() + 40));
            }
        }

        html.append("</div></body></html>");
        return html.toString();
    }

    private String renderLink(int x1, int y1, int x2, int y2) {
        double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        double angle = Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI;
        return String.format("<div class='relationship' style='width: %.2fpx; left: %dpx; top: %dpx; transform: rotate(%.2fdeg);'></div>",
                length, x1, y1, angle);
    }
}
