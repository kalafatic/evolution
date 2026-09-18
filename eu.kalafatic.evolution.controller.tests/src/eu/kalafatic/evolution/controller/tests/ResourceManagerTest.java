package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.EvoRepository;
import eu.kalafatic.evolution.controller.resource.EvoService;
import eu.kalafatic.evolution.controller.resource.ResourceManager;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class ResourceManagerTest {

    private ResourceManager resourceManager;

    @Before
    public void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
    }

    @Test
    public void testEmfOrchestratorBinding() {
        Orchestrator orchestrator = resourceManager.getOrchestrator();
        assertNotNull("EMF Orchestrator source of truth should not be null", orchestrator);
    }

    @Test
    public void testPathResolution() {
        Path userHome = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize();

        Path evoRoot = resourceManager.getPath(EvoPath.EVO_ROOT);
        assertNotNull("EVO_ROOT path should not be null", evoRoot);
        assertTrue("EVO_ROOT must reside under user.home/git/", evoRoot.startsWith(userHome.resolve("git")));

        Path workspace = resourceManager.getPath(EvoPath.WORKSPACE);
        assertNotNull("WORKSPACE path should not be null", workspace);
        assertTrue("WORKSPACE must reside under user.home/workspace/", workspace.startsWith(userHome.resolve("workspace")));

        Path supervisorSource = resourceManager.getPath(EvoPath.SUPERVISOR_SOURCE);
        assertNotNull("SUPERVISOR_SOURCE path should not be null", supervisorSource);
        assertTrue("SUPERVISOR_SOURCE should point to supervisor module", supervisorSource.toFile().getName().contains("supervisor"));

        Path genomeDir = resourceManager.getPath(EvoPath.GENOME);
        assertNotNull("GENOME path should not be null", genomeDir);
        assertTrue("GENOME should point to genome module", genomeDir.toFile().getName().contains("genome"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testOutofBoundsPathFailsFast() {
        File tempFile = new File(System.getProperty("java.io.tmpdir"), "evo_test_abs_path");
        resourceManager.resolvePath(EvoPath.EVO_ROOT, tempFile.getAbsolutePath());
    }

    @Test
    public void testCanonicalAbsolutePathResolution() {
        Path userHome = Paths.get(System.getProperty("user.home")).toAbsolutePath().normalize();
        Path validWorkspaceFile = userHome.resolve("workspace/datasets/sample.txt");

        Path resolved = resourceManager.resolvePath(EvoPath.WORKSPACE, validWorkspaceFile.toString());
        assertEquals("Valid canonical absolute path resolution must return normalized path",
                validWorkspaceFile.toAbsolutePath().normalize(), resolved);
    }

    @Test
    public void testVariableExpansionInPathResolution() {
        Path resolvedEvoRoot = resourceManager.resolvePath("${EVO_ROOT}/eu.kalafatic.evolution.supervisor");
        assertNotNull("Expanded ${EVO_ROOT} path should not be null", resolvedEvoRoot);
        assertTrue("Expanded path should end with supervisor module", resolvedEvoRoot.endsWith(Paths.get("eu.kalafatic.evolution.supervisor")));
    }

    @Test
    public void testServiceEnumerationAndUrlCalculation() {
        List<EvoService> services = resourceManager.getServices();
        assertNotNull("Services list should not be null", services);
        assertTrue("Services list should contain configured services", services.size() >= 4);

        EvoService supervisor = resourceManager.getService("SUPERVISOR");
        assertNotNull("SUPERVISOR service should exist", supervisor);
        assertEquals("SUPERVISOR service port must default to 8089", 8089, supervisor.getPort());
        assertEquals("http://127.0.0.1:8089", supervisor.getUrl());

        EvoService server = resourceManager.getService("SERVER");
        assertNotNull("SERVER service should exist", server);

        EvoService inference = resourceManager.getService("INFERENCE");
        assertNotNull("INFERENCE service should exist", inference);

        List<String> portIssues = resourceManager.validatePorts();
        assertNotNull("Port validation result should not be null", portIssues);
    }

    @Test
    public void testRepositoryEnumeration() {
        List<EvoRepository> repositories = resourceManager.getRepositories();
        assertNotNull("Repositories list should not be null", repositories);
        assertTrue("Repositories list should contain at least primary EVO repository", !repositories.isEmpty());

        EvoRepository evoRepo = resourceManager.getRepository("EVOLUTION");
        assertNotNull("EVOLUTION repository descriptor should exist", evoRepo);
        assertTrue("EVOLUTION repository local path should exist", evoRepo.isExists());
    }

    @Test
    public void testModelsAndDatasetsEnumeration() {
        List<AIProvider> models = resourceManager.getModels();
        assertNotNull("Models list should not be null", models);

        List<Path> datasets = resourceManager.getDatasets();
        assertNotNull("Datasets list should not be null", datasets);
    }
}
