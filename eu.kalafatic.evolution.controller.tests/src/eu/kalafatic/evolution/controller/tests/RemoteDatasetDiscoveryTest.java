package eu.kalafatic.evolution.controller.tests;

import eu.kalafatic.evolution.controller.orchestration.DatasetCandidateManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.forge.data.api.discovery.DatasetCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.DatasetCompatibilityEvaluator;
import eu.kalafatic.evolution.forge.data.api.discovery.DatasetSearchRequest;
import eu.kalafatic.evolution.forge.data.impl.discovery.HuggingFaceDatasetProvider;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class RemoteDatasetDiscoveryTest {

    private DatasetCompatibilityEvaluator evaluator;

    @Before
    public void setUp() {
        evaluator = new DatasetCompatibilityEvaluator();
    }

    @Test
    public void testDatasetSearchRequestCreation() {
        DatasetSearchRequest request = new DatasetSearchRequest.Builder()
                .datasetName("tatsu-lab/alpaca")
                .task("INSTRUCTION")
                .domain("Instruction Tuning")
                .language("en")
                .format("INSTRUCTION")
                .requiredFields(List.of("instruction", "input", "output"))
                .preferredSplit("train")
                .targetSizeBytes(500 * 1024 * 1024L)
                .minimumSizeBytes(100 * 1024 * 1024L)
                .limit(10)
                .build();

        assertEquals("tatsu-lab/alpaca", request.getDatasetName());
        assertEquals("INSTRUCTION", request.getTask());
        assertEquals("Instruction Tuning", request.getDomain());
        assertEquals("en", request.getLanguage());
        assertEquals("INSTRUCTION", request.getFormat());
        assertEquals(3, request.getRequiredFields().size());
        assertEquals("train", request.getPreferredSplit());
        assertEquals(500 * 1024 * 1024L, request.getTargetSizeBytes());
        assertEquals(10, request.getLimit());
    }

    @Test
    public void testDatasetCandidateStableIdentityAndJsonRoundTrip() {
        DatasetCandidate candidate = new DatasetCandidate("Hugging Face", "Qwen/Qwen2.5-Coder-7B", "main");
        candidate.setDescription("Coding dataset for evaluation");
        candidate.setTask("CODE");
        candidate.setLanguage("en");
        candidate.setFormat("CODE");
        candidate.setSizeBytes(420 * 1024 * 1024L);
        candidate.setSplits(List.of("train", "validation"));
        candidate.setCompatibilityScore(88);
        candidate.setCompatibilityReasons(List.of("+ Exact task match", "+ English language"));
        candidate.setCompatibilityWarnings(List.of("- Smaller than 500MB target"));
        candidate.setStatus("READY");

        assertEquals("huggingface:Qwen/Qwen2.5-Coder-7B:main", candidate.getId());

        JSONObject json = candidate.toJsonObject();
        DatasetCandidate restored = DatasetCandidate.fromJsonObject(json);

        assertNotNull(restored);
        assertEquals(candidate.getId(), restored.getId());
        assertEquals(candidate.getProvider(), restored.getProvider());
        assertEquals(candidate.getRepository(), restored.getRepository());
        assertEquals(candidate.getRevision(), restored.getRevision());
        assertEquals(candidate.getDescription(), restored.getDescription());
        assertEquals(candidate.getTask(), restored.getTask());
        assertEquals(candidate.getLanguage(), restored.getLanguage());
        assertEquals(candidate.getSizeBytes(), restored.getSizeBytes());
        assertEquals(2, restored.getSplits().size());
        assertEquals(88, restored.getCompatibilityScore());
        assertEquals(2, restored.getCompatibilityReasons().size());
        assertEquals(1, restored.getCompatibilityWarnings().size());
        assertEquals("READY", restored.getStatus());
    }

    @Test
    public void testCompatibilityEvaluationScoring() {
        DatasetSearchRequest request = new DatasetSearchRequest.Builder()
                .datasetName("tatsu-lab/alpaca")
                .task("INSTRUCTION")
                .domain("Instruction Tuning")
                .language("en")
                .format("INSTRUCTION")
                .requiredFields(List.of("instruction", "input", "output"))
                .preferredSplit("train")
                .targetSizeBytes(100 * 1024 * 1024L)
                .build();

        DatasetCandidate candidate = new DatasetCandidate("Hugging Face", "tatsu-lab/alpaca", "main");
        candidate.setTask("INSTRUCTION");
        candidate.setDomain("Instruction Tuning");
        candidate.setLanguage("en");
        candidate.setFormat("INSTRUCTION");
        candidate.setSchema(List.of("instruction", "input", "output"));
        candidate.setSplits(List.of("train"));
        candidate.setSizeBytes(150 * 1024 * 1024L);

        DatasetCompatibilityEvaluator.CompatibilityResult result = evaluator.evaluate(candidate, request);

        assertTrue("Expected score >= 90 for exact match, got " + result.getScore(), result.getScore() >= 90);
        assertEquals("EXACT_MATCH", candidate.getStatus());
        assertFalse(result.getReasons().isEmpty());
    }

    @Test
    public void testHuggingFaceDatasetProviderSearch() throws Exception {
        DatasetSearchRequest request = new DatasetSearchRequest.Builder()
                .datasetName("wikitext")
                .task("GENERAL_TEXT")
                .domain("text")
                .language("en")
                .format("TEXT")
                .limit(5)
                .build();

        HuggingFaceDatasetProvider provider = new HuggingFaceDatasetProvider();
        List<DatasetCandidate> candidates = provider.search(request);

        assertNotNull(candidates);
        assertFalse("Expected discovered candidates from Hugging Face search", candidates.isEmpty());

        DatasetCandidate top = candidates.get(0);
        assertNotNull(top.getRepository());
        assertTrue(top.getCompatibilityScore() > 0);
        assertNotNull(top.getStatus());
    }

    @Test
    public void testDatasetCandidateManagerEmfPersistenceAndDeduplication() {
        ForgeSession session = OrchestrationFactory.eINSTANCE.createForgeSession();
        session.setSessionId("test-session-123");

        eu.kalafatic.evolution.model.orchestration.SessionModelState state = OrchestrationFactory.eINSTANCE.createSessionModelState();
        state.setSessionId("test-session-123");
        session.setModelState(state);

        DatasetCandidate candidate1 = new DatasetCandidate("Hugging Face", "Salesforce/wikitext", "main");
        candidate1.setTask("GENERAL_TEXT");
        candidate1.setCompatibilityScore(85);

        DatasetCandidate candidate2 = new DatasetCandidate("Hugging Face", "tatsu-lab/alpaca", "main");
        candidate2.setTask("INSTRUCTION");
        candidate2.setCompatibilityScore(92);

        DatasetCandidateManager manager = DatasetCandidateManager.getInstance();

        // 1. Initial save
        manager.saveCandidates(session, List.of(candidate1, candidate2));

        List<DatasetCandidate> restored = manager.getCandidates(session);
        assertEquals(2, restored.size());
        assertEquals("tatsu-lab/alpaca", restored.get(0).getRepository()); // Sorted descending by score

        // 2. Deduplication check: adding duplicate with updated score
        DatasetCandidate duplicateAlpaca = new DatasetCandidate("Hugging Face", "tatsu-lab/alpaca", "main");
        duplicateAlpaca.setTask("INSTRUCTION");
        duplicateAlpaca.setCompatibilityScore(95);

        manager.addCandidates(session, List.of(duplicateAlpaca));

        List<DatasetCandidate> deduplicated = manager.getCandidates(session);
        assertEquals(2, deduplicated.size());
        assertEquals(95, deduplicated.get(0).getCompatibilityScore());

        // 3. Fallback candidates retrieval
        List<DatasetCandidate> fallbacks = manager.getFallbackCandidates(session);
        assertEquals(2, fallbacks.size());
        assertTrue(fallbacks.get(0).getCompatibilityScore() >= fallbacks.get(1).getCompatibilityScore());

        // 4. Remove candidate
        boolean removed = manager.removeCandidate(session, "Salesforce/wikitext");
        assertTrue(removed);

        List<DatasetCandidate> afterRemove = manager.getCandidates(session);
        assertEquals(1, afterRemove.size());
        assertEquals("tatsu-lab/alpaca", afterRemove.get(0).getRepository());

        // 5. Clear candidates
        manager.clearCandidates(session);
        assertTrue(manager.getCandidates(session).isEmpty());
    }
}
