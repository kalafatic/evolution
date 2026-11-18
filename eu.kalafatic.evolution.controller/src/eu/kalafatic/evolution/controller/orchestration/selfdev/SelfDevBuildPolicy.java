package eu.kalafatic.evolution.controller.orchestration.selfdev;

/**
 * Explicit Self-Dev build policy.
 *
 * Default policy: TESTS ENABLED. Iteration acceptance requires real test execution;
 * a build whose tests were skipped can never be used as evidence of test success.
 *
 * Explicit smoke-build escape hatch (operator-requested only):
 *   -Devo.build.skipTests=true   -> tests are skipped and the resulting build is
 *                                  recorded as SMOKE (not test-verified).
 */
public class SelfDevBuildPolicy {

    public static final String SKIP_TESTS_PROPERTY = "evo.build.skipTests";
    public static final String POLICY_PROPERTY = "evo.build.policy";

    public enum Policy {
        TESTS_ENABLED,
        SMOKE_SKIP_TESTS
    }

    private final Policy policy;
    private final boolean testsEnabled;

    public SelfDevBuildPolicy(Policy policy, boolean testsEnabled) {
        this.policy = policy != null ? policy : Policy.TESTS_ENABLED;
        this.testsEnabled = testsEnabled;
    }

    public static SelfDevBuildPolicy resolve(SelfDevContext context) {
        boolean skip = Boolean.parseBoolean(System.getProperty(SKIP_TESTS_PROPERTY, "false"));
        String explicitPolicy = System.getProperty(POLICY_PROPERTY, "");
        if ("smoke".equalsIgnoreCase(explicitPolicy.trim())) {
            skip = true;
        } else if ("tests-on".equalsIgnoreCase(explicitPolicy.trim())) {
            skip = false;
        }
        if (skip) {
            return new SelfDevBuildPolicy(Policy.SMOKE_SKIP_TESTS, false);
        }
        return new SelfDevBuildPolicy(Policy.TESTS_ENABLED, true);
    }

    public boolean isTestsEnabled() {
        return testsEnabled;
    }

    public boolean isSkipTests() {
        return !testsEnabled;
    }

    public Policy getPolicy() {
        return policy;
    }

    @Override
    public String toString() {
        return "SelfDevBuildPolicy{" + policy + "}";
    }
}
