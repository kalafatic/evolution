package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.MavenSupervisorBuilder;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevBuildPolicy;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TaskResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.TychoEvoRcpBuilder;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

/**
 * WP3 - Build pipeline policy tests: tests enabled by default, explicit
 * smoke-build escape hatch, EXPORT never implicitly builds, supervisor
 * reactor isolation.
 */
public class SelfDevBuildPolicyTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceManager resourceManager;

    @Before
    public void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
        System.clearProperty(SelfDevBuildPolicy.SKIP_TESTS_PROPERTY);
        System.clearProperty(SelfDevBuildPolicy.POLICY_PROPERTY);
    }

    @After
    public void tearDown() {
        System.clearProperty(SelfDevBuildPolicy.SKIP_TESTS_PROPERTY);
        System.clearProperty(SelfDevBuildPolicy.POLICY_PROPERTY);
    }

    @Test
    public void testDefaultPolicyEnablesTests() {
        SelfDevBuildPolicy policy = SelfDevBuildPolicy.resolve(null);
        assertEquals(SelfDevBuildPolicy.Policy.TESTS_ENABLED, policy.getPolicy());
        assertTrue("Tests must be enabled by default", policy.isTestsEnabled());
        assertFalse("skipTests must be false by default", policy.isSkipTests());
    }

    @Test
    public void testExplicitSmokeEscapeHatch() {
        System.setProperty(SelfDevBuildPolicy.SKIP_TESTS_PROPERTY, "true");
        SelfDevBuildPolicy policy = SelfDevBuildPolicy.resolve(null);
        assertEquals(SelfDevBuildPolicy.Policy.SMOKE_SKIP_TESTS, policy.getPolicy());
        assertTrue("Explicit smoke hatch must skip tests", policy.isSkipTests());
    }

    @Test
    public void testPolicyPropertySmokeAndTestsOn() {
        System.setProperty(SelfDevBuildPolicy.POLICY_PROPERTY, "smoke");
        assertTrue(SelfDevBuildPolicy.resolve(null).isSkipTests());
        System.setProperty(SelfDevBuildPolicy.POLICY_PROPERTY, "tests-on");
        System.setProperty(SelfDevBuildPolicy.SKIP_TESTS_PROPERTY, "true");
        // explicit tests-on wins over the escape hatch
        assertFalse(SelfDevBuildPolicy.resolve(null).isSkipTests());
    }

    @Test
    public void testTychoBuilderDefaultsToTestsEnabled() {
        TychoEvoRcpBuilder builder = new TychoEvoRcpBuilder();
        assertFalse("TychoEvoRcpBuilder must default to tests enabled", builder.isSkipTests());
    }

    @Test
    public void testExportNeverImplicitlyBuilds() throws Exception {
        File runDir = tempFolder.newFolder("exportRun");
        File sourceDir = new File(runDir, "source");
        sourceDir.mkdirs();
        try (FileWriter fw = new FileWriter(new File(sourceDir, "pom.xml"))) {
            fw.write("<project><modelVersion>4.0.0</modelVersion></project>");
        }
        File fakeRepo = tempFolder.newFolder("fakeRepo");
        new File(fakeRepo, ".git").mkdirs();

        SelfDevContext context = new SelfDevContext(fakeRepo, runDir, resourceManager.getOrchestrator());
        TychoEvoRcpBuilder builder = new TychoEvoRcpBuilder();

        long start = System.currentTimeMillis();
        TaskResult res = builder.exportProduct(context);
        long elapsed = System.currentTimeMillis() - start;

        assertFalse("Export must fail when the build artifact is missing", res.isSuccess());
        assertTrue("Export failure must direct the operator to BUILD_EVO (D-11): " + res.getMessage(),
                res.getMessage().contains("BUILD_EVO") && res.getMessage().contains("never"));
        assertTrue("Export must fail fast without executing a Maven build", elapsed < 15000);
    }

    @Test
    public void testSupervisorBuildRefusesCanonicalRepoRoot() throws Exception {
        File repo = tempFolder.newFolder("repo");
        new File(repo, ".git").mkdirs();
        try (FileWriter fw = new FileWriter(new File(repo, "pom.xml"))) {
            fw.write("<project></project>");
        }
        File runDir = tempFolder.newFolder("supRun");

        SelfDevContext context = new SelfDevContext(repo, runDir, resourceManager.getOrchestrator()) {
            @Override
            public File getPreparedReactorDirectory() {
                // No isolated reactor available -> the fallback path must refuse
                // to build inside the canonical repository.
                return new File(runDir, "source-nonexistent");
            }
            @Override
            public File getSupervisorDirectory() {
                return getRepositoryRoot();
            }
        };

        MavenSupervisorBuilder builder = new MavenSupervisorBuilder();
        TaskResult res = builder.build(context);
        assertFalse("Supervisor build at canonical repository root must be refused", res.isSuccess());
        assertTrue("Refusal message must identify the isolation rule: " + res.getMessage(),
                res.getMessage().contains("canonical repository root"));
    }

    @Test
    public void testSupervisorBuildPrefersIsolatedReactor() throws Exception {
        File repo = tempFolder.newFolder("repo2");
        new File(repo, ".git").mkdirs();
        File runDir = tempFolder.newFolder("supRun2");
        File sourceDir = new File(runDir, "source");
        sourceDir.mkdirs();
        try (FileWriter fw = new FileWriter(new File(sourceDir, "pom.xml"))) {
            fw.write("<project></project>");
        }
        new File(sourceDir, "eu.kalafatic.evolution.selfdev.genome").mkdirs();

        SelfDevContext context = new SelfDevContext(repo, runDir, resourceManager.getOrchestrator()) {
            @Override
            public File getSupervisorDirectory() {
                return repo;
            }
        };

        MavenSupervisorBuilder builder = new MavenSupervisorBuilder();
        TaskResult res = builder.build(context);
        // The isolated-reactor branch executes a real Maven build against a dummy pom
        // and must therefore fail the build (dummy pom), NOT refuse with the
        // canonical-repo guard and NOT succeed.
        assertNotNull(res);
        assertFalse("Dummy-pom build cannot succeed", res.isSuccess());
        assertFalse("Must not trigger the canonical-repo refusal when an isolated reactor exists",
                res.getMessage().contains("canonical repository root"));
    }
}
