package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.orchestration.selfdev.CopySourceTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.GitCheckTask;
import eu.kalafatic.evolution.controller.orchestration.selfdev.ResolvedSelfDevResources;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevOrchestrator;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflight;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevPreflightResult;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevTask;
import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.ResourceManager;

public class CanonicalEvoRepositoryPreflightTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private ResourceManager resourceManager;

    @Before
    public void setUp() {
        resourceManager = ResourceManager.getInstance();
        resourceManager.refresh();
    }

    @Test
    public void testValidCanonicalRepositoryPassesPreflight() throws IOException {
        File repo = tempFolder.newFolder("valid_evo_repo");
        new File(repo, ".git").mkdirs();
        Files.writeString(new File(repo, "pom.xml").toPath(), "<project></project>");

        SelfDevContext context = new SelfDevContext(repo, resourceManager.getOrchestrator());
        SelfDevPreflight preflight = new SelfDevPreflight(resourceManager);
        SelfDevPreflightResult result = preflight.executePreflight(context, null);

        assertNotNull(result);
        assertEquals(SelfDevPreflightResult.PreflightStatus.SUCCESS, result.getStatus());
        assertNotNull(result.getResolvedResources());
        assertEquals(repo.getCanonicalFile(), result.getResolvedResources().getRepositoryRoot().getCanonicalFile());
    }

    @Test
    public void testMissingCanonicalRepositoryFailsPreflightWithoutDiscovery() {
        File missingRepo = new File(tempFolder.getRoot(), "non_existent_evo_repo");

        SelfDevContext context = new SelfDevContext(missingRepo, resourceManager.getOrchestrator());
        SelfDevPreflight preflight = new SelfDevPreflight(resourceManager);
        SelfDevPreflightResult result = preflight.executePreflight(context, null);

        assertNotNull(result);
        assertEquals(SelfDevPreflightResult.PreflightStatus.BLOCKED, result.getStatus());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Canonical EVO Git repository is invalid")));
        assertTrue(result.getRecoveries().isEmpty());
    }

    @Test
    public void testRuntimeProductDirectoryRejectedAsRepository() throws IOException {
        File productDir = tempFolder.newFolder("runtime-eu.kalafatic.evolution.view.product");
        new File(productDir, ".git").mkdirs();
        Files.writeString(new File(productDir, "pom.xml").toPath(), "<project></project>");

        SelfDevContext context = new SelfDevContext(productDir, resourceManager.getOrchestrator());
        SelfDevPreflight preflight = new SelfDevPreflight(resourceManager);
        SelfDevPreflightResult result = preflight.executePreflight(context, null);

        assertNotNull(result);
        assertEquals(SelfDevPreflightResult.PreflightStatus.BLOCKED, result.getStatus());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("runtime product directory")));
    }

    @Test
    public void testAbsolutePathNeverPrefixed() throws IOException {
        File absDir = tempFolder.newFolder("abs_repo");
        new File(absDir, ".git").mkdirs();
        Files.writeString(new File(absDir, "pom.xml").toPath(), "<project></project>");

        Path resolved = resourceManager.resolvePath(tempFolder.getRoot().toPath(), absDir.getAbsolutePath());
        assertEquals(absDir.getCanonicalPath(), resolved.toFile().getCanonicalPath());
        assertFalse(resolved.toString().startsWith(tempFolder.getRoot().getAbsolutePath() + File.separator + absDir.getAbsolutePath()));
    }

    @Test
    public void testLayerSourceConflictTriggersPathConflictBlock() throws IOException {
        File repo = tempFolder.newFolder("canonical_repo");
        new File(repo, ".git").mkdirs();
        Files.writeString(new File(repo, "pom.xml").toPath(), "<project></project>");

        File conflictingSource = tempFolder.newFolder("other_source");

        SelfDevContext context = new SelfDevContext(repo, resourceManager.getOrchestrator());

        // Manually set a conflicting source path on context
        ResolvedSelfDevResources conflictingSnapshot = new ResolvedSelfDevResources(
                repo, repo, conflictingSource, conflictingSource,
                new File(repo, "build"), new File(repo, "export"), new File(repo, "runtime"), new File(repo, "logs"),
                new File(repo, "supervisor"), new File(repo, "genome"),
                "win32", "win32", "x86_64", "evolution", "evo",
                new File("java"), new File("mvn")
        );
        context.setResolvedResources(conflictingSnapshot);

        SelfDevPreflight preflight = new SelfDevPreflight(resourceManager);
        SelfDevPreflightResult result = preflight.executePreflight(context, null);

        assertNotNull(result);
        assertEquals(SelfDevPreflightResult.PreflightStatus.BLOCKED, result.getStatus());
        assertTrue(result.getConflicts().stream().anyMatch(c -> c.contains("PATH_CONFLICT")));
    }

    @Test
    public void testRepoWithoutPomPassesPreflightIfSourceReactorConfiguredWithPom() throws IOException {
        File gitRepo = tempFolder.newFolder("git_only_repo");
        new File(gitRepo, ".git").mkdirs();

        File sourceReactor = tempFolder.newFolder("source_reactor");
        Files.writeString(new File(sourceReactor, "pom.xml").toPath(), "<project></project>");

        SelfDevContext context = new SelfDevContext(gitRepo, resourceManager.getOrchestrator());
        ResolvedSelfDevResources res = new ResolvedSelfDevResources(
                gitRepo, gitRepo, sourceReactor, new File(context.getBuildDirectory(), "evo-rcp"),
                context.getBuildDirectory(), context.getExportDirectory(), context.getRuntimeDirectory(),
                context.getLogDirectory(), context.getSupervisorDirectory(), context.getGenomeDirectory(),
                "win32", "win32", "x86_64", "evolution", "evo", new File("java"), new File("mvn")
        );
        context.setResolvedResources(res);

        SelfDevPreflight preflight = new SelfDevPreflight(resourceManager);
        SelfDevPreflightResult result = preflight.executePreflight(context, null);

        assertNotNull(result);
        assertEquals(SelfDevPreflightResult.PreflightStatus.SUCCESS, result.getStatus());
    }

    @Test
    public void testInvalidSourceReactorFailsPreflightImmediatelyWithoutSearching() throws IOException {
        File gitRepo = tempFolder.newFolder("git_repo");
        new File(gitRepo, ".git").mkdirs();

        File invalidSourceReactor = tempFolder.newFolder("invalid_source_reactor");

        SelfDevContext context = new SelfDevContext(gitRepo, resourceManager.getOrchestrator());
        ResolvedSelfDevResources res = new ResolvedSelfDevResources(
                gitRepo, gitRepo, invalidSourceReactor, new File(context.getBuildDirectory(), "evo-rcp"),
                context.getBuildDirectory(), context.getExportDirectory(), context.getRuntimeDirectory(),
                context.getLogDirectory(), context.getSupervisorDirectory(), context.getGenomeDirectory(),
                "win32", "win32", "x86_64", "evolution", "evo", new File("java"), new File("mvn")
        );
        context.setResolvedResources(res);

        SelfDevPreflight preflight = new SelfDevPreflight(resourceManager);
        SelfDevPreflightResult result = preflight.executePreflight(context, null);

        assertNotNull(result);
        assertEquals(SelfDevPreflightResult.PreflightStatus.BLOCKED, result.getStatus());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Configured EVO_SOURCE_REACTOR is invalid: root pom.xml missing")));
    }

    @Test
    public void testTaskSnapshotPropagation() throws IOException {
        File repo = tempFolder.newFolder("snapshot_propagation_repo");
        new File(repo, ".git").mkdirs();
        Files.writeString(new File(repo, "pom.xml").toPath(), "<project></project>");

        SelfDevContext context = new SelfDevContext(repo, resourceManager.getOrchestrator());
        SelfDevPreflight preflight = new SelfDevPreflight(resourceManager);
        SelfDevPreflightResult result = preflight.executePreflight(context, null);

        assertEquals(SelfDevPreflightResult.PreflightStatus.SUCCESS, result.getStatus());
        context.setResolvedResources(result.getResolvedResources());

        CopySourceTask copyTask = new CopySourceTask("COPY");
        copyTask.execute(context);

        GitCheckTask gitTask = new GitCheckTask("GIT_EVO");
        gitTask.execute(context);

        assertEquals(result.getResolvedResources().getSourceDirectory().getCanonicalPath(), context.getSourceDirectory().getCanonicalPath());
        assertEquals(result.getResolvedResources().getRepositoryRoot().getCanonicalPath(), context.getRepositoryRoot().getCanonicalPath());
    }
}
