package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.service.CognitiveScopes;
import eu.kalafatic.evolution.servers.service.IdentityService;
import eu.kalafatic.evolution.servers.service.ValidatedPrincipal;
import eu.kalafatic.evolution.servers.validation.ValidationCategory;
import eu.kalafatic.evolution.servers.validation.ValidationMode;
import eu.kalafatic.evolution.servers.validation.ValidationPolicyService;
import eu.kalafatic.evolution.servers.validation.ValidationResult;
import eu.kalafatic.evolution.servers.validation.ValidationRule;

/**
 * Application identity and token security: issuance, validation, revocation,
 * scope sanitization, and the invariant that registration never grants
 * automatic data access.
 */
public class CognitiveSecurityTest {

    private final DatabaseManager databaseManager = new DatabaseManager();
    private final IdentityService identityService = IdentityService.getInstance(databaseManager);

    @Test
    public void issueAndValidateTokenRoundTrip() {
        String applicationId = "sec-test-app-" + System.nanoTime();
        try {
            identityService.registerApplication(applicationId, "Security Test App", "EXTERNAL_MCP",
                    null, null);
            String token = identityService.issueToken(applicationId, 1L,
                    List.of(CognitiveScopes.CONTEXT_GET, CognitiveScopes.MEMORY_READ), null, null);
            assertNotNull(token);
            Optional<ValidatedPrincipal> principal = identityService.validateToken(token);
            assertTrue("Token must validate", principal.isPresent());
            assertEquals("admin", principal.get().getUsername());
            assertEquals(applicationId, principal.get().getApplicationId());
            assertTrue(principal.get().hasScope(CognitiveScopes.CONTEXT_GET));
            assertFalse(principal.get().hasScope(CognitiveScopes.MEMORY_WRITE));
        } finally {
            cleanupApplication(applicationId);
        }
    }

    @Test
    public void invalidAndRevokedTokensAreRejected() {
        String applicationId = "sec-revoke-app-" + System.nanoTime();
        try {
            identityService.registerApplication(applicationId, "Revoke Test App", "EXTERNAL_MCP", null, null);
            String token = identityService.issueToken(applicationId, 1L,
                    List.of(CognitiveScopes.MEMORY_READ), null, null);
            assertTrue(identityService.validateToken(token).isPresent());

            // Revoke by access id and verify rejection
            long accessId = identityService.listAccesses(applicationId).get(0).getId();
            identityService.revokeToken(accessId, "admin");
            assertTrue("Revoked token must be rejected", identityService.validateToken(token).isEmpty());
        } finally {
            cleanupApplication(applicationId);
        }
        assertTrue("Unknown token must be rejected", identityService.validateToken("definitely-not-a-token").isEmpty());
        assertTrue("Empty token must be rejected", identityService.validateToken("").isEmpty());
    }

    @Test
    public void unknownApplicationCannotIssueTokens() {
        boolean rejected = false;
        try {
            identityService.issueToken("no-such-app-" + System.nanoTime(), 1L,
                    List.of(CognitiveScopes.MEMORY_READ), null, null);
        } catch (IllegalArgumentException e) {
            rejected = true;
        }
        assertTrue("Token issuance for unknown application must fail", rejected);
    }

    @Test
    public void invalidScopesAreSanitized() {
        String applicationId = "sec-scope-app-" + System.nanoTime();
        try {
            identityService.registerApplication(applicationId, "Scope Test App", "EXTERNAL_MCP", null, null);
            String token = identityService.issueToken(applicationId, 1L,
                    List.of("context.get", "bogus.scope", "ecosystem.admin"), null, null);
            Optional<ValidatedPrincipal> principal = identityService.validateToken(token);
            assertTrue(principal.isPresent());
            assertTrue(principal.get().hasScope("context.get"));
            assertFalse("Bogus scopes must be dropped",
                    principal.get().getScopes().contains("bogus.scope"));
        } finally {
            cleanupApplication(applicationId);
        }
    }

    @Test
    public void securityRulesAreFixedAndCannotBeDisabled() {
        ValidationPolicyService policy = ValidationPolicyService.getInstance(databaseManager);
        ValidationRule authRule = policy.getRule("SECURITY.authentication");
        assertNotNull(authRule);
        assertTrue("Security rules must be fixed", authRule.isSecurityFixed());
        assertEquals("Security rules must always evaluate STRICT",
                ValidationMode.STRICT, policy.getEffectiveMode("SECURITY.authentication"));
    }

    @Test
    public void domainValidationModesSwitchWithoutCodeChanges() {
        ValidationPolicyService policy = ValidationPolicyService.getInstance(databaseManager);
        try {
            policy.setRuleMode("MEMORY.contentRequired", ValidationMode.DISABLED, "test", "test");
            assertEquals(ValidationMode.DISABLED, policy.getEffectiveMode("MEMORY.contentRequired"));

            policy.setRuleMode("MEMORY.contentRequired", ValidationMode.WARN, "test", "test");
            assertEquals(ValidationMode.WARN, policy.getEffectiveMode("MEMORY.contentRequired"));

            policy.setRuleMode("MEMORY.contentRequired", ValidationMode.STRICT, "test", "test");
            assertEquals(ValidationMode.STRICT, policy.getEffectiveMode("MEMORY.contentRequired"));
        } finally {
            policy.setRuleMode("MEMORY.contentRequired", ValidationMode.DISABLED, "test", "test cleanup");
        }
    }

    @Test
    public void strictFailureBlocksOperation() {
        ValidationPolicyService policy = ValidationPolicyService.getInstance(databaseManager);
        boolean blocked = false;
        try {
            policy.setRuleMode("CONTEXT.budgetFits", ValidationMode.STRICT, "test", "test");
            policy.enforce("CONTEXT.budgetFits", () ->
                    ValidationResult.failed("CONTEXT.budgetFits", "budget exceeded", "ERROR", false));
        } catch (eu.kalafatic.evolution.servers.validation.ValidationException e) {
            blocked = true;
        } finally {
            policy.setRuleMode("CONTEXT.budgetFits", ValidationMode.DISABLED, "test", "test cleanup");
        }
        assertTrue("STRICT failure must throw ValidationException", blocked);
    }

    @Test
    public void disabledModeSkipsDomainChecks() {
        ValidationPolicyService policy = ValidationPolicyService.getInstance(databaseManager);
        ValidationResult result = policy.evaluate("USER_PROFILE.displayName", () ->
                ValidationResult.failed("USER_PROFILE.displayName", "missing", "WARN", false));
        assertTrue("DISABLED mode must skip domain validation", result.isPassed());
    }

    private void cleanupApplication(String applicationId) {
        try {
            identityService.findApplication(applicationId).ifPresent(app -> {
                for (var access : identityService.listAccesses(applicationId)) {
                    identityService.deleteToken(access.getId());
                }
                identityService.deleteApplication(app.getId());
            });
        } catch (Exception ignored) {
        }
    }
}
