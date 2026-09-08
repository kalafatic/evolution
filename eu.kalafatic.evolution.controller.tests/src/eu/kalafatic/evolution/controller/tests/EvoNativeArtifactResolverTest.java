package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.forge.model.inference.NativeEvoArtifactResolver;
import eu.kalafatic.evolution.forge.model.inference.NativeEvoArtifactResolver.ArtifactResolutionResult;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;

public class EvoNativeArtifactResolverTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Path validEvoFile;
    private Path emptyDir;
    private Path singleEvoDir;
    private Path multiEvoDir;
    private Path ggufFile;
    private Path missingPath;

    @Before
    public void setUp() throws Exception {
        File baseDir = tempFolder.newFolder("resolver-test");

        // 1. Valid .evo file
        EvoLlmModel model = new EvoLlmModel(100, 32, 2, 1, 128, 8);
        Map<String, Integer> vocab = new LinkedHashMap<>();
        vocab.put("<unk>", 0);
        vocab.put("<s>", 1);
        vocab.put("</s>", 2);
        vocab.put("hi", 3);

        EvoModelArtifact artifact = new EvoModelArtifact();
        artifact.initializeFromModel("test-resolver-model", model, vocab);

        validEvoFile = baseDir.toPath().resolve("test-model.evo");
        artifact.saveToFile(validEvoFile);

        // 2. Empty directory
        emptyDir = tempFolder.newFolder("empty-dir").toPath();

        // 3. Directory with single .evo file
        singleEvoDir = tempFolder.newFolder("single-evo-dir").toPath();
        artifact.saveToFile(singleEvoDir.resolve("model1.evo"));

        // 4. Directory with multiple .evo files
        multiEvoDir = tempFolder.newFolder("multi-evo-dir").toPath();
        artifact.saveToFile(multiEvoDir.resolve("model1.evo"));
        artifact.saveToFile(multiEvoDir.resolve("model2.evo"));

        // 5. GGUF file
        ggufFile = baseDir.toPath().resolve("test-model.gguf");
        Files.writeString(ggufFile, "GGUF_DUMMY_HEADER");

        // 6. Missing path
        missingPath = baseDir.toPath().resolve("non-existent-path.evo");
    }

    @Test
    public void testResolveValidEvoFile() throws Exception {
        ArtifactResolutionResult res = NativeEvoArtifactResolver.resolveNativeArtifact(validEvoFile);
        assertNotNull(res);
        assertEquals(validEvoFile.toAbsolutePath(), res.getResolvedPath().toAbsolutePath());
        assertNotNull(res.getArtifact());
        assertEquals("test-resolver-model", res.getArtifact().getModelName());
    }

    @Test
    public void testResolveDirectoryWithSingleEvoFile() throws Exception {
        ArtifactResolutionResult res = NativeEvoArtifactResolver.resolveNativeArtifact(singleEvoDir);
        assertNotNull(res);
        assertTrue(res.getResolvedPath().getFileName().toString().endsWith(".evo"));
        assertEquals("test-resolver-model", res.getArtifact().getModelName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectEmptyDirectory() throws Exception {
        NativeEvoArtifactResolver.resolveNativeArtifact(emptyDir);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectMultiEvoDirectory() throws Exception {
        NativeEvoArtifactResolver.resolveNativeArtifact(multiEvoDir);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRejectGgufFile() throws Exception {
        NativeEvoArtifactResolver.resolveNativeArtifact(ggufFile);
    }

    @Test(expected = FileNotFoundException.class)
    public void testRejectMissingFile() throws Exception {
        NativeEvoArtifactResolver.resolveNativeArtifact(missingPath);
    }
}
