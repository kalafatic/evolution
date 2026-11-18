package eu.kalafatic.evolution.servers.mcp.model;

import java.util.concurrent.atomic.AtomicBoolean;

import eu.kalafatic.evolution.servers.service.ValidatedPrincipal;

/**
 * Execution context associated with an MCP request.
 * Contains session identifier, security principal, cancellation flag, and timeout bounds.
 */
public class McpExecutionContext {

    private final String sessionId;
    private final String principal;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private long timeoutMs = 60000L; // default 60 seconds
    private final long startTimeMs = System.currentTimeMillis();

    /** Raw bearer token as presented by the client (never logged). */
    private String bearerToken;

    /** Principal established by successful bearer authentication. */
    private ValidatedPrincipal authenticatedPrincipal;

    public McpExecutionContext(String sessionId, String principal) {
        this.sessionId = (sessionId != null && !sessionId.trim().isEmpty()) ? sessionId : "default-session";
        this.principal = (principal != null && !principal.trim().isEmpty()) ? principal : "SYSTEM";
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public ValidatedPrincipal getAuthenticatedPrincipal() {
        return authenticatedPrincipal;
    }

    public void setAuthenticatedPrincipal(ValidatedPrincipal authenticatedPrincipal) {
        this.authenticatedPrincipal = authenticatedPrincipal;
    }

    public McpExecutionContext(String sessionId, String principal, long timeoutMs) {
        this(sessionId, principal);
        if (timeoutMs > 0) {
            this.timeoutMs = timeoutMs;
        }
    }

    public static McpExecutionContext system() {
        return new McpExecutionContext("system-session", "SYSTEM");
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getPrincipal() {
        return principal;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancel() {
        cancelled.set(true);
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public boolean isTimedOut() {
        return (System.currentTimeMillis() - startTimeMs) > timeoutMs;
    }

    public void checkCancelledOrTimedOut() throws InterruptedException {
        if (isCancelled()) {
            throw new InterruptedException("MCP operation cancelled by user or coordinator.");
        }
        if (isTimedOut()) {
            throw new InterruptedException("MCP operation timed out after " + timeoutMs + "ms.");
        }
    }
}
