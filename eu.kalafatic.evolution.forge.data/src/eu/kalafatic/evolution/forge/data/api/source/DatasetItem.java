package eu.kalafatic.evolution.forge.data.api.source;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Clean data transfer object representing a dataset source selection item from the UI or configuration.
 */
public class DatasetItem {

    private boolean checked;
    private String path;
    private String type; // e.g. "FILE", "FOLDER", "EVO_MODEL", "OASST1", etc.

    public DatasetItem() {
        this(true, "", "FOLDER");
    }

    public DatasetItem(boolean checked, String path, String type) {
        this.checked = checked;
        this.path = path != null ? path : "";
        this.type = type != null ? type : "FOLDER";
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path != null ? path : "";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type != null ? type : "FOLDER";
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("checked", checked);
        obj.put("path", path);
        obj.put("type", type);
        return obj;
    }

    public static DatasetItem fromJsonObject(JSONObject obj) {
        if (obj == null) return new DatasetItem();
        boolean checked = obj.optBoolean("checked", true);
        String path = obj.optString("path", "");
        String type = obj.optString("type", "FOLDER");
        return new DatasetItem(checked, path, type);
    }

    public static List<DatasetItem> parseJsonList(Object jsonObjectOrString) {
        List<DatasetItem> items = new ArrayList<>();
        if (jsonObjectOrString == null) return items;

        JSONArray arr = null;
        if (jsonObjectOrString instanceof JSONArray) {
            arr = (JSONArray) jsonObjectOrString;
        } else if (jsonObjectOrString instanceof String) {
            String str = ((String) jsonObjectOrString).trim();
            if (!str.isEmpty()) {
                try {
                    arr = new JSONArray(str);
                } catch (Exception ignored) {}
            }
        }

        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null) {
                    items.add(fromJsonObject(obj));
                }
            }
        }
        return items;
    }

    @Override
    public String toString() {
        return "DatasetItem{" +
                "checked=" + checked +
                ", path='" + path + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
