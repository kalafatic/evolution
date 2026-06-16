package eu.kalafatic.evolution.creatic.model;

import java.util.Map;
import java.util.HashMap;

public class ContextGraph {
    private final String pageId;
    private final Map<String, Object> data = new HashMap<>();

    public ContextGraph(String pageId) {
        this.pageId = pageId;
    }

    public String getPageId() {
        return pageId;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public Map<String, Object> getAll() {
        return data;
    }
}
