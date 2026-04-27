package eu.kalafatic.evolution.view.editors.pages.architecture;

import java.util.HashMap;
import java.util.Map;
import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;

/**
 * @evo:19:A reason=dynamic-design-renderer
 */
public class DesignRenderer {

    public String render(DesignModel model) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>");
        html.append("body { font-family: 'Segoe UI', sans-serif; background-color: #f8fafc; padding: 40px; margin: 0; }");
        html.append(".canvas { position: relative; width: 1000px; height: 800px; background: white; border-radius: 12px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1); margin: auto; overflow: hidden; }");
        html.append(".component { position: absolute; width: 160px; padding: 15px; background: #eff6ff; border: 2px solid #3b82f6; border-radius: 8px; text-align: center; z-index: 10; }");
        html.append(".component-name { font-weight: bold; color: #1e40af; margin-bottom: 4px; }");
        html.append(".component-type { font-size: 0.8em; color: #60a5fa; text-transform: uppercase; }");
        html.append(".relationship { position: absolute; height: 2px; background: #94a3b8; transform-origin: 0 0; z-index: 5; }");
        html.append(".relationship::after { content: ''; position: absolute; right: -5px; top: -4px; border: 5px solid transparent; border-left-color: #94a3b8; }");
        html.append(".header { text-align: center; margin-bottom: 30px; }");
        html.append("h1 { color: #1e293b; margin: 0; }");
        html.append("p { color: #64748b; }");
        html.append("</style></head><body>");

        html.append("<div class='header'>");
        html.append("<h1>").append(model.getName() != null ? model.getName() : "Evolution Architecture").append("</h1>");
        html.append("<p>Model-driven dynamic visualization</p>");
        html.append("</div>");

        html.append("<div class='canvas'>");

        Map<String, ComponentRecord> compMap = new HashMap<>();
        for (ComponentRecord comp : model.getComponents()) {
            compMap.put(comp.getName(), comp);
            html.append("<div class='component' style='left: ").append(comp.getX()).append("px; top: ").append(comp.getY()).append("px;'>");
            html.append("<div class='component-name'>").append(comp.getName()).append("</div>");
            html.append("<div class='component-type'>").append(comp.getType()).append("</div>");
            html.append("</div>");
        }

        for (RelationshipRecord rel : model.getRelationships()) {
            ComponentRecord from = compMap.get(rel.getFrom());
            ComponentRecord to = compMap.get(rel.getTo());
            if (from != null && to != null) {
                html.append(renderLink(from.getX() + 80, from.getY() + 30, to.getX() + 80, to.getY() + 30));
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
