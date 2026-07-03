package eu.kalafatic.evolution.servers.mcp.protocol;

public class McpError {
    private int code;
    private String message;
    private Object data;

    public McpError() {}
    public McpError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
}
