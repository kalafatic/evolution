package eu.kalafatic.evolution.controller.orchestration.design;

import eu.kalafatic.evolution.model.orchestration.GenomeSnapshot;

/**
 * @evo:20:A reason=rich-design-renderer
 */
public class DesignRenderer {

    public String render(DesignModel model) {
        return render(model, "COMPONENTS");
    }

    public String render(DesignModel model, String viewMode) {
        return render(model, viewMode, null, null, new java.util.ArrayList<String>(), new java.util.ArrayList<GenomeSnapshot>(), null);
    }

    public String render(DesignModel model, String viewMode, String targetPath, java.util.List<String> history) {
        return render(model, viewMode, targetPath, null, history, new java.util.ArrayList<GenomeSnapshot>(), null);
    }

    public String render(DesignModel model, String viewMode, String targetPath, String defaultPath, java.util.List<String> history) {
        return render(model, viewMode, targetPath, defaultPath, history, new java.util.ArrayList<GenomeSnapshot>(), null);
    }

    public String render(DesignModel model, String viewMode, String targetPath, String defaultPath, java.util.List<String> history, java.util.List<GenomeSnapshot> snapshots, String currentSnapshot) {
        return render(model, viewMode, targetPath, defaultPath, history, snapshots, currentSnapshot, "");
    }

    public String render(DesignModel model, String viewMode, String targetPath, String defaultPath, java.util.List<String> history, java.util.List<GenomeSnapshot> snapshots, String currentSnapshot, String baseUrl) {
        return render(model, viewMode, targetPath, defaultPath, history, snapshots, currentSnapshot, baseUrl, "{}");
    }

    public String render(DesignModel model, String viewMode, String targetPath, String defaultPath, java.util.List<String> history, java.util.List<GenomeSnapshot> snapshots, String currentSnapshot, String baseUrl, String genomeJson) {
        if (model != null) {
            eu.kalafatic.evolution.controller.log.Log.log("[DESIGN_RENDERER] Rendering model: " + model.getName() + " with " + model.getComponents().size() + " components. Mode: " + viewMode);
        }

        String templatePath = "/template.html";
        if ("GENOME".equals(viewMode)) {
            templatePath = "/genome.html";
        }

        String template = eu.kalafatic.evolution.controller.tools.FileTool.readResource(templatePath);
        if (template == null) {
            eu.kalafatic.evolution.controller.log.Log.log("[DESIGN_RENDERER] ERROR: " + templatePath + " not found in bundle resources!");
            return "Error: Template not found";
        }

        String navigatorJs = eu.kalafatic.evolution.controller.tools.FileTool.readResource("/js/navigator.js");
        if (navigatorJs == null) {
            eu.kalafatic.evolution.controller.log.Log.log("[DESIGN_RENDERER] ERROR: /js/navigator.js not found in bundle resources!");
            navigatorJs = "log('ERROR: /js/navigator.js not found in bundle resources!');";
        }

        // Use JSON quoting to safely escape strings (especially paths with backslashes) for JS insertion
        String viewModeJson = org.json.JSONObject.quote(viewMode != null ? viewMode : "COMPONENTS");
        String targetPathJson = org.json.JSONObject.quote(targetPath != null ? targetPath : "");
        String defaultPathJson = org.json.JSONObject.quote(defaultPath != null ? defaultPath : "");
        String currentSnapshotJson = org.json.JSONObject.quote(currentSnapshot != null ? currentSnapshot : "");

        org.json.JSONArray snapArray = new org.json.JSONArray();
        if (snapshots != null) {
            for (GenomeSnapshot s : snapshots) {
                snapArray.put(new org.json.JSONObject()
                    .put("timestamp", s.getTimestamp())
                    .put("dashboard", s.getDashboardArtifact()));
            }
        }

        return template
            .replace("{{BASE_URL}}", baseUrl)
            .replace("{{MODEL_JSON}}", serializeModel(model))
            .replace("{{VIEW_MODE_JSON}}", viewModeJson)
            .replace("{{TARGET_PATH_JSON}}", targetPathJson)
            .replace("{{DEFAULT_PATH_JSON}}", defaultPathJson)
            .replace("{{TARGET_HISTORY_JSON}}", new org.json.JSONArray(history).toString())
            .replace("{{SNAPSHOTS_JSON}}", snapArray.toString())
            .replace("{{CURRENT_SNAPSHOT_JSON}}", currentSnapshotJson)
            .replace("{{NAVIGATOR_JS}}", navigatorJs)
            .replace("{{GENOME_JSON}}", genomeJson != null ? genomeJson : "{}");
    }

    public String serializeModel(DesignModel model) {
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
