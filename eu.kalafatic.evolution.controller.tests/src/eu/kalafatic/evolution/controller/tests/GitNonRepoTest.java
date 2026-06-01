package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;

public class GitNonRepoTest {

    private File tempDir;
    private GitVersionControlProvider provider;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("non-git-test").toFile();
        provider = new GitVersionControlProvider();
    }

    @Test
    public void testGetChangedFilesOnNonGitDirectory() throws Exception {
        // Assert that it does not throw exception and returns empty list
        List<String> changedFiles = provider.getChangedFiles(tempDir, "HEAD");
        assertNotNull(changedFiles);
        assertTrue(changedFiles.isEmpty());
    }

    @Test
    public void testFetchCommitsOnNonGitDirectory() throws Exception {
        // Assert that it does not throw exception and returns empty list
        List<String> commits = provider.fetchCommits(tempDir);
        assertNotNull(commits);
        assertTrue(commits.isEmpty());
    }
}
