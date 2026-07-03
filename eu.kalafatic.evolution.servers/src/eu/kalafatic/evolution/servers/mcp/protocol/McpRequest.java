package eu.kalafatic.evolution.servers.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpRequest {
    private String jsonrpc = "2.0";
    private String method;
    private JsonNode params;
    private Object id;

    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public JsonNode getParams() { return params; }
    public void setParams(JsonNode params) { this.params = params; }
    public Object getId() { return id; }
    public void setId(Object id) { this.id = id; }
}
