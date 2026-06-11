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
        return render(model, "COMPONENTS");
    }

    public String render(DesignModel model, String viewMode) {
        if (model != null) {
            eu.kalafatic.evolution.controller.log.Log.log("[DESIGN_RENDERER] Rendering model: " + model.getName() + " with " + model.getComponents().size() + " components. Mode: " + viewMode);
        }
        String template = eu.kalafatic.evolution.controller.tools.FileTool.readResource("/template.html");
        if (template == null) return "Error: Template not found";

        String navigatorJs = eu.kalafatic.evolution.controller.tools.FileTool.readResource("/js/navigator.js");
        if (navigatorJs == null) navigatorJs = "// Navigator script not found";

        String d3Js = eu.kalafatic.evolution.controller.tools.FileTool.readResource("/js/d3.v7.min.js");
        if (d3Js == null) d3Js = "// D3.js not found";

        return template
            .replace("{{MODEL_JSON}}", serializeModel(model))
            .replace("{{VIEW_MODE}}", viewMode != null ? viewMode : "COMPONENTS")
            .replace("{{NAVIGATOR_JS}}", navigatorJs)
            .replace("{{D3_JS}}", d3Js);
    }

    private String serializeModel(DesignModel model) {
        if (model == null) return "{}";
        org.json.JSONObject json = new org.json.JSONObject();
        json.put("name", model.getName() != null ? model.getName() : "Evolution Architecture");
        org.json.JSONArray comps = new org.json.JSONArray();
        for (ComponentRecord cr : model.getComponents()) {
            org.json.JSONObject c = new org.json.JSONObject();
            c.put("id", cr.getId());
            c.put("name", cr.getName());
            c.put("type", cr.getType());
            c.put("description", cr.getDescription());
            c.put("path", cr.getPath());
            c.put("importanceScore", cr.getImportanceScore());
            c.put("useCases", new org.json.JSONArray(cr.getUseCases()));
            c.put("keyClasses", new org.json.JSONArray(cr.getKeyClasses()));
            comps.put(c);
        }
        json.put("components", comps);

        org.json.JSONArray rels = new org.json.JSONArray();
        for (RelationshipRecord rr : model.getRelationships()) {
            org.json.JSONObject r = new org.json.JSONObject();
            r.put("from", rr.getFrom());
            r.put("to", rr.getTo());
            r.put("type", rr.getType());
            rels.put(r);
        }
        json.put("relationships", rels);
        return json.toString();
    }

    private String renderLink(int x1, int y1, int x2, int y2) {
        double length = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        double angle = Math.atan2(y2 - y1, x2 - x1) * 180 / Math.PI;
        return String.format("<div class='relationship' style='width: %.2fpx; left: %dpx; top: %dpx; transform: rotate(%.2fdeg);'></div>",
                length, x1, y1, angle);
    }
}
