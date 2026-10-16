package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.io.FileWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import eu.kalafatic.evolution.controller.mediation.model.TargetSnapshot.TargetType;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetClassificationDetector;
import eu.kalafatic.evolution.controller.mediation.scanner.TargetClassificationDetector.ClassificationResult;

public class TargetClassificationDetectorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testJavaSoftwareRepositoryClassification() throws Exception {
        File repo = tempFolder.newFolder("my-java-app");
        new File(repo, "src/main/java/com/app").mkdirs();
        File pom = new File(repo, "pom.xml");
        try (FileWriter fw = new FileWriter(pom)) {
            fw.write("<project><groupId>com.app</groupId><artifactId>my-app</artifactId></project>");
        }

        ClassificationResult res = TargetClassificationDetector.detect(repo);
        assertNotNull(res);
        assertEquals(TargetType.PROJECT, res.getCategory());
        assertTrue(res.getConfidence() > 0.5);
    }

    @Test
    public void testNonJavaSoftwareRepositoryClassification() throws Exception {
        File repo = tempFolder.newFolder("my-python-service");
        new File(repo, "app/api").mkdirs();
        File reqs = new File(repo, "requirements.txt");
        try (FileWriter fw = new FileWriter(reqs)) {
            fw.write("fastapi==0.100.0\nuvicorn==0.22.0\n");
        }

        ClassificationResult res = TargetClassificationDetector.detect(repo);
        assertNotNull(res);
        assertEquals(TargetType.PROJECT, res.getCategory());
    }

    @Test
    public void testDatasetRepositoryClassification() throws Exception {
        File dataDir = tempFolder.newFolder("my-dataset");
        File p1 = new File(dataDir, "train.parquet");
        try (FileWriter fw = new FileWriter(p1)) { fw.write("PAR1_DATA"); }
        File p2 = new File(dataDir, "val.jsonl");
        try (FileWriter fw = new FileWriter(p2)) { fw.write("{\"text\":\"hello\"}"); }

        ClassificationResult res = TargetClassificationDetector.detect(dataDir);
        assertNotNull(res);
        assertEquals(TargetType.DATASET, res.getCategory());
    }

    @Test
    public void testKnowledgeBaseClassification() throws Exception {
        File docDir = tempFolder.newFolder("my-docs");
        File readme = new File(docDir, "README.md");
        try (FileWriter fw = new FileWriter(readme)) { fw.write("# System Architecture\nWelcome to docs"); }
        File guide = new File(docDir, "guide.adoc");
        try (FileWriter fw = new FileWriter(guide)) { fw.write("= User Guide\nContent"); }

        ClassificationResult res = TargetClassificationDetector.detect(docDir);
        assertNotNull(res);
        assertEquals(TargetType.KNOWLEDGE_BASE, res.getCategory());
    }

    @Test
    public void testAiModelClassification() throws Exception {
        File modelDir = tempFolder.newFolder("my-llm");
        File cfg = new File(modelDir, "config.json");
        try (FileWriter fw = new FileWriter(cfg)) { fw.write("{\"architectures\":[\"LlamaForCausalLM\"]}"); }
        File bin = new File(modelDir, "model.safetensors");
        try (FileWriter fw = new FileWriter(bin)) { fw.write("MODEL_WEIGHTS"); }

        ClassificationResult res = TargetClassificationDetector.detect(modelDir);
        assertNotNull(res);
        assertEquals(TargetType.AI_MODEL, res.getCategory());
    }

    @Test
    public void testPathIndependence() throws Exception {
        // Path contains "evolution" substring but content is a dataset
        File dataDir = tempFolder.newFolder("evolution-data-sample");
        File p1 = new File(dataDir, "0000.parquet");
        try (FileWriter fw = new FileWriter(p1)) { fw.write("PAR1_DATA"); }

        ClassificationResult res = TargetClassificationDetector.detect(dataDir);
        assertNotNull(res);
        assertEquals("Target category must be derived from content evidence, not path substring",
                TargetType.DATASET, res.getCategory());
    }

    @Test
    public void testEmptyOrMissingTargetDiagnostics() throws Exception {
        File emptyDir = tempFolder.newFolder("empty-dir");
        ClassificationResult resEmpty = TargetClassificationDetector.detect(emptyDir);
        assertNotNull(resEmpty);
        assertEquals(TargetType.UNKNOWN, resEmpty.getCategory());
        assertEquals(0.0, resEmpty.getConfidence(), 0.01);

        File missingDir = new File(tempFolder.getRoot(), "non-existent-dir");
        ClassificationResult resMissing = TargetClassificationDetector.detect(missingDir);
        assertNotNull(resMissing);
        assertEquals(TargetType.UNKNOWN, resMissing.getCategory());
        assertEquals(0.0, resMissing.getConfidence(), 0.01);
    }
}
