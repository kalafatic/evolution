package eu.kalafatic.evolution.servers.mcp.resources;

import com.fasterxml.jackson.databind.JsonNode;
import eu.kalafatic.evolution.servers.mcp.model.Resource;
import java.util.*;

public class ResourceRegistry {
    private final Map<String, Resource> resources = new HashMap<>();
    private final Map<String, ResourceProvider> providers = new HashMap<>();

    public void register(Resource resource, ResourceProvider provider) {
        resources.put(resource.getUri(), resource);
        providers.put(resource.getUri(), provider);
    }

    public List<Resource> listResources() {
        return new ArrayList<>(resources.values());
    }

    public Object readResource(JsonNode params) throws Exception {
        String uri = params.get("uri").asText();
        String baseUri = uri;
        int queryIndex = uri.indexOf('?');
        if (queryIndex > 0) {
            baseUri = uri.substring(0, queryIndex);
        }
        ResourceProvider provider = providers.get(uri);
        if (provider == null) {
            // Query parameters are provider input, not part of the registered
            // URI: fall back to the base URI lookup (evo://memory/entry?entryId=..).
            provider = providers.get(baseUri);
        }
        if (provider == null) {
            throw new Exception("Resource not found: " + uri);
        }

        Map<String, Object> content = new HashMap<>();
        content.put("uri", uri);
        content.put("mimeType", resources.get(baseUri).getMimeType());
        content.put("text", provider.read(uri));

        Map<String, Object> result = new HashMap<>();
        result.put("contents", Collections.singletonList(content));
        return result;
    }

    public interface ResourceProvider {
        String read(String uri) throws Exception;
    }
}
