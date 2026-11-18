package eu.kalafatic.evolution.servers.mcp.server;

import java.util.Optional;

import eu.kalafatic.evolution.servers.service.ValidatedPrincipal;

/**
 * Validates bearer tokens presented by MCP clients. Implementations decide
 * which token classes are accepted (user session tokens, application tokens,
 * or both). Never trusts client-supplied identity parameters.
 */
public interface McpAuthenticator {

    Optional<ValidatedPrincipal> authenticate(String bearerToken);
}
