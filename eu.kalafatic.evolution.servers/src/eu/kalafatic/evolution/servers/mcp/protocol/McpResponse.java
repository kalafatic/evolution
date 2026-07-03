package eu.kalafatic.evolution.servers.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class McpResponse {
    private String jsonrpc = "2.0";
    private Object result;
    private McpError error;
    private Object id;

    public McpResponse() {}

    public static McpResponse success(Object id, Object result) {
        McpResponse resp = new McpResponse();
        resp.setId(id);
        resp.setResult(result);
        return resp;
    }

    public static McpResponse error(Object id, int code, String message) {
        McpResponse resp = new McpResponse();
        resp.setId(id);
        resp.setError(new McpError(code, message));
        return resp;
    }

    public String getJsonrpc() { return jsonrpc; }
    public void setJsonrpc(String jsonrpc) { this.jsonrpc = jsonrpc; }
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }
    public McpError getError() { return error; }
    public void setError(McpError error) { this.error = error; }
    public Object getId() { return id; }
    public void setId(Object id) { this.id = id; }
}
