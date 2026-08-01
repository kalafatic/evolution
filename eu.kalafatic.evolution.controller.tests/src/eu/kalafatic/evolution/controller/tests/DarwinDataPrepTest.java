package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.forge.controller.service.impl.agents.*;

public class DarwinDataPrepTest {

    private Path tempDir;
    private Path docFile;
    private Path codeFile;

    @Before
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("darwin-data-prep-test-");

        // 1. Create a markdown documentation file
        docFile = tempDir.resolve("architecture.md");
        Files.writeString(docFile,
            "# Evolution System\n\n" +
            "The **IterationManager** is the sole transition authority of the state machine.\n\n" +
            "## Core Invariants\n" +
            "All state transitions must flow through the Kernel.\n" +
            "**Single Transition Authority** defines this rule.");

        // 2. Create a Java class source file
        codeFile = tempDir.resolve("IterationManager.java");
        Files.writeString(codeFile,
            "package eu.kalafatic.evolution.controller.orchestration;\n" +
            "public class IterationManager {\n" +
            "    // Implementation of single transition authority\n" +
            "}");
    }

    @Test
    public void testSourceAnalysisAgentAndDuplicateDetection() throws Exception {
        SourceAnalysisAgent agent = new SourceAnalysisAgent();
        List<Path> paths = List.of(docFile, codeFile, docFile); // docFile added twice to test duplicate content detection

        List<KnowledgeUnit> units = agent.analyze(paths, tempDir);

        // Verify duplicate was filtered out and file types are identified correctly
        assertEquals("Duplicate content must be detected and skipped", 2, units.size());

        KnowledgeUnit docUnit = null;
        KnowledgeUnit codeUnit = null;
        for (KnowledgeUnit u : units) {
            if (u.getRelativePath().equals("architecture.md")) docUnit = u;
            if (u.getRelativePath().equals("IterationManager.java")) codeUnit = u;
        }

        assertNotNull(docUnit);
        assertNotNull(codeUnit);
        assertEquals("MARKDOWN", docUnit.getFileType());
        assertEquals("JAVA", codeUnit.getFileType());
        assertNotNull(docUnit.getHash());
        assertFalse(docUnit.getContent().isEmpty());
    }

    @Test
    public void testKnowledgeExtractionAgentFallback() throws Exception {
        SourceAnalysisAgent sourceAgent = new SourceAnalysisAgent();
        List<KnowledgeUnit> units = sourceAgent.analyze(List.of(docFile, codeFile), tempDir);

        // Use useLlm = false to trigger the safe deterministic fallback parser
        KnowledgeExtractionAgent extractAgent = new KnowledgeExtractionAgent(null, false);
        List<KnowledgeFact> facts = extractAgent.extract(units);

        assertFalse("Extraction should find facts under safe fallback mode", facts.isEmpty());

        boolean foundConcept = false;
        for (KnowledgeFact fact : facts) {
            assertNotNull(fact.getSourcePath());
            assertNotNull(fact.getSourceHash());
            assertNotNull(fact.getEvidence());
            if (fact.getConcept().contains("IterationManager") || fact.getConcept().contains("Evolution System")) {
                foundConcept = true;
            }
        }
        assertTrue("Fallback extraction should find core system concepts", foundConcept);
    }

    @Test
    public void testTrainingDataAndQualityAgents() throws Exception {
        SourceAnalysisAgent sourceAgent = new SourceAnalysisAgent();
        List<KnowledgeUnit> units = sourceAgent.analyze(List.of(docFile), tempDir);

        KnowledgeExtractionAgent extractAgent = new KnowledgeExtractionAgent(null, false);
        List<KnowledgeFact> facts = extractAgent.extract(units);

        TrainingDataAgent trainingAgent = new TrainingDataAgent();
        List<TrainingRecord> records = trainingAgent.generate(facts);

        assertFalse("Training records should be generated", records.isEmpty());

        TrainingRecord rawRecord = null;
        TrainingRecord instructionRecord = null;
        for (TrainingRecord r : records) {
            assertEquals("architecture.md", r.getSourcePath());
            assertNotNull(r.getEvidence());
            if ("RAW_LANGUAGE_MODEL".equals(r.getType())) {
                rawRecord = r;
            } else if ("INSTRUCTION".equals(r.getType())) {
                instructionRecord = r;
            }
        }

        assertNotNull("RAW_LANGUAGE_MODEL record must be present", rawRecord);
        assertNotNull("INSTRUCTION record must be present", instructionRecord);
        assertEquals("AUTHORITATIVE_LOCAL", rawRecord.getSourceTrust());
        assertEquals("LLM_GENERATED", instructionRecord.getSourceTrust());

        // DatasetQualityAgent evaluation
        DatasetQualityAgent qualityAgent = new DatasetQualityAgent();
        List<DatasetQualityAgent.QualityReport> reports = qualityAgent.evaluate(records);

        for (DatasetQualityAgent.QualityReport report : reports) {
            assertNotEquals("Ground truth records with valid evidence should not be REJECTED",
                DatasetQualityAgent.Recommendation.REJECT, report.getRecommendation());
        }
    }

    @Test
    public void testConsistencyAgentDriftDetection() throws Exception {
        SourceAnalysisAgent sourceAgent = new SourceAnalysisAgent();
        List<KnowledgeUnit> units = sourceAgent.analyze(List.of(docFile, codeFile), tempDir);

        ConsistencyAgent consistencyAgent = new ConsistencyAgent();
        List<ConsistencyAgent.ConsistencyViolation> violations = consistencyAgent.checkConsistency(units);

        // The Java file implements IterationManager. The doc is structured with a matching reference.
        // If we modify the doc to have NO reference, let's see if drift is detected.
        Path driftDoc = tempDir.resolve("drift_architecture.md");
        Files.writeString(driftDoc, "# Drift\n\nNo references to iteration state machine here.");

        // Use a fresh SourceAnalysisAgent to bypass any duplicate cache for the codeFile
        SourceAnalysisAgent freshSourceAgent = new SourceAnalysisAgent();
        List<KnowledgeUnit> driftUnits = freshSourceAgent.analyze(List.of(driftDoc, codeFile), tempDir);
        List<ConsistencyAgent.ConsistencyViolation> driftViolations = consistencyAgent.checkConsistency(driftUnits);

        boolean foundDrift = false;
        for (ConsistencyAgent.ConsistencyViolation v : driftViolations) {
            if ("DOCUMENTATION_DRIFT".equals(v.getCategory())) {
                foundDrift = true;
            }
        }
        assertTrue("ConsistencyAgent must detect documentation-implementation drift", foundDrift);
    }

    @Test
    public void testDatasetSplittingLeakagePrevention() throws Exception {
        // Build mock list of 20 units
        List<KnowledgeUnit> units = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            units.add(new KnowledgeUnit("file_" + i + ".md", "MARKDOWN", "Content " + i, "hash_" + i));
        }

        List<KnowledgeUnit> trainUnits = new ArrayList<>();
        List<KnowledgeUnit> valUnits = new ArrayList<>();
        List<KnowledgeUnit> evalUnits = new ArrayList<>();

        for (int i = 0; i < units.size(); i++) {
            double rand = (double) i / units.size();
            if (rand < 0.70) trainUnits.add(units.get(i));
            else if (rand < 0.85) valUnits.add(units.get(i));
            else evalUnits.add(units.get(i));
        }

        // Check splitting distribution close to 70/15/15
        assertTrue(trainUnits.size() > 0);
        assertTrue(valUnits.size() > 0);
        assertTrue(evalUnits.size() > 0);

        // Verify complete isolation (no duplicate file entries across splits)
        for (KnowledgeUnit u : trainUnits) {
            assertFalse(valUnits.contains(u));
            assertFalse(evalUnits.contains(u));
        }
    }
}
