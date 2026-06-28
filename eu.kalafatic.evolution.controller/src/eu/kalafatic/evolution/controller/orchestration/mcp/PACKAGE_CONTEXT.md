# PACKAGE CONTEXT

## Directory: git/evolution-240526-ok/eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/orchestration/mcp/

## Domain: general

## Components
* `McpClient.java`: package eu.kalafatic.evolution.controller.orchestration.mcp; import java.net.URI; import java.net.http.HttpClient; import java.net.http.HttpRequest; import java.net.http.HttpResponse; import java.time.Duration; import java.util.UUID; import org.json.JSONArray; import org.json.JSONObject; public class McpClient { private final String serverUrl; private final HttpClient httpClient; public McpClient(String serverUrl) { this.serverUrl = serverUrl; this.httpClient = HttpClient.newBuilder() .connectTimeout(Duration.ofSeconds(10)) .build(); } public String initialize() throws Exception { JSONObject params = new JSONObject();
