package eu.kalafatic.evolution.servers.mcp.connectors;

import com.fasterxml.jackson.databind.JsonNode;
import eu.kalafatic.evolution.servers.mcp.tools.ToolRegistry;
import eu.kalafatic.evolution.servers.mcp.model.Tool;
import java.util.*;

public class DummyConnector {

    public void registerTools(ToolRegistry registry) {
        // Jira tools
        registry.register(new Tool("searchIssues", "Search for Jira issues", createSearchSchema()),
            this::searchIssues);
        registry.register(new Tool("getIssue", "Get details of a Jira issue", Collections.singletonMap("type", "object")),
            this::getIssue);

        // Confluence tools
        registry.register(new Tool("searchPages", "Search for Confluence pages", createSearchSchema()),
            this::searchPages);
        registry.register(new Tool("getPage", "Get details of a Confluence page", Collections.singletonMap("type", "object")),
            this::getPage);

        // GitHub tools
        registry.register(new Tool("searchRepositories", "Search for GitHub repositories", createSearchSchema()),
            this::searchRepositories);
    }

    private Map<String, Object> createSearchSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections.singletonMap("query", Collections.singletonMap("type", "string")));
        schema.put("required", Collections.singletonList("query"));
        return schema;
    }

    private Object searchIssues(JsonNode args) {
        List<Map<String, Object>> issues = new ArrayList<>();
        issues.add(createIssue("PROJECT-123", "Implement metadata explorer", "In Progress", "John Doe"));
        issues.add(createIssue("PROJECT-124", "Fix bug in evolution engine", "Open", "Jane Smith"));
        return Collections.singletonMap("issues", issues);
    }

    private Object getIssue(JsonNode args) {
        return createIssue("PROJECT-123", "Implement metadata explorer", "In Progress", "John Doe");
    }

    private Map<String, Object> createIssue(String id, String summary, String status, String assignee) {
        Map<String, Object> issue = new HashMap<>();
        issue.put("id", id);
        issue.put("summary", summary);
        issue.put("status", status);
        issue.put("assignee", assignee);
        return issue;
    }

    private Object searchPages(JsonNode args) {
        List<Map<String, Object>> pages = new ArrayList<>();
        pages.add(Map.of("id", "1001", "title", "Architecture Overview"));
        pages.add(Map.of("id", "1002", "title", "MCP Protocol Definition"));
        return Collections.singletonMap("pages", pages);
    }

    private Object getPage(JsonNode args) {
        return Map.of("id", "1001", "title", "Architecture Overview", "content", "This page describes the overall architecture...");
    }

    private Object searchRepositories(JsonNode args) {
        List<Map<String, Object>> repos = new ArrayList<>();
        repos.add(Map.of("name", "evolution-platform", "url", "https://github.com/kalafatic/evolution-platform"));
        repos.add(Map.of("name", "mcp-java-sdk", "url", "https://github.com/kalafatic/mcp-java-sdk"));
        return Collections.singletonMap("repositories", repos);
    }
}
