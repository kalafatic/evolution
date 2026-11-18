package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.cognitive.ConversationHistoryService;
import eu.kalafatic.evolution.controller.cognitive.KnowledgeExtractionService;
import eu.kalafatic.evolution.controller.cognitive.MemoryConsolidationService;
import eu.kalafatic.evolution.controller.cognitive.MemoryConsolidationService.ConsolidationJob;
import eu.kalafatic.evolution.controller.cognitive.PersonalizationService;
import eu.kalafatic.evolution.controller.cognitive.ProjectKnowledgeService;
import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemoryType;

/**
 * Direct unit tests for the four services that previously had only indirect
 * coverage: PersonalizationService, ProjectKnowledgeService,
 * MemoryConsolidationService, KnowledgeExtractionService.
 */
public class CognitivePlatformServicesTest {

    private static final long USER_ID = 1L;

    private MemoryService memoryService;
    private PersonalizationService personalizationService;
    private ProjectKnowledgeService projectKnowledgeService;

    @Before
    public void setUp() {
        memoryService = MemoryService.getInstance();
        memoryService.clearAll();
        personalizationService = PersonalizationService.getInstance();
        projectKnowledgeService = ProjectKnowledgeService.getInstance();
    }

    @After
    public void tearDown() {
        memoryService.clearAll();
    }

    // ------------------------------------------------------------------
    // PersonalizationService
    // ------------------------------------------------------------------

    @Test
    public void explicitBeatsInferred() {
        String project = "/tmp/pref-project-" + System.nanoTime();
        personalizationService.updateExplicit(USER_ID, "USER", null, "style", "minimal", 1.0);
        JSONObject inferred = personalizationService.recordInference(USER_ID, "USER", null,
                "style", "verbose", 0.9, false, "session-1");
        assertFalse("Inferred write must be rejected when an explicit value exists",
                inferred.optBoolean("accepted", true));

        JSONObject effective = personalizationService.getEffectivePreference("admin", project, "style");
        assertNotNull(effective);
        assertEquals("minimal", effective.optString("value"));
        assertEquals("EXPLICIT", effective.optString("source"));
    }

    @Test
    public void projectScopeBeatsUserScopeForProjectContext() {
        String project = "/tmp/pref-project-scope-" + System.nanoTime();
        personalizationService.updateExplicit(USER_ID, "USER", null, "format", "xml", 1.0);
        personalizationService.updateExplicit(USER_ID, "PROJECT", project, "format", "json", 1.0);

        List<JSONObject> effective = personalizationService.getEffectivePreferencesByUserId(USER_ID, project);
        JSONObject projectPref = effective.stream()
                .filter(p -> "format".equals(p.optString("key")))
                .findFirst().orElse(null);
        assertNotNull(projectPref);
        assertEquals("json", projectPref.optString("value"));
        assertEquals("PROJECT", projectPref.optString("scope"));
    }

    @Test
    public void correctionUpgradesInferredToExplicitWithResetConfidence() {
        String project = "/tmp/pref-correction-" + System.nanoTime();
        personalizationService.recordInference(USER_ID, "USER", null, "layout", "tabs", 0.7, false, "s1");
        JSONObject corrected = personalizationService.handleCorrection(USER_ID, "USER", null,
                "layout", "split");
        assertTrue(corrected.optBoolean("accepted", false));
        JSONObject effective = personalizationService.getEffectivePreference("admin", project, "layout");
        assertEquals("split", effective.optString("value"));
        assertEquals("EXPLICIT", effective.optString("source"));
    }

    @Test
    public void explicitConflictsAreDetected() {
        String project = "/tmp/pref-conflict-" + System.nanoTime();
        personalizationService.updateExplicit(USER_ID, "USER", null, "color", "dark", 1.0);
        personalizationService.updateExplicit(USER_ID, "USER", null, "color", "light", 1.0);
        List<JSONObject> conflicts = personalizationService.detectExplicitConflicts(USER_ID, project);
        assertTrue("Two explicit values for the same key must be detected as a conflict",
                conflicts.size() >= 1);
    }

    // ------------------------------------------------------------------
    // ProjectKnowledgeService
    // ------------------------------------------------------------------

    @Test
    public void buildAndTestResultsAreRecordedAndReported() {
        String project = "/tmp/pk-project-" + System.nanoTime();
        long buildId = projectKnowledgeService.recordBuild(project, "mvn verify", true, 42000,
                "BUILD SUCCESS", "session-x");
        assertTrue(buildId > 0);
        projectKnowledgeService.recordTest(project, "cognitive.suite", 1, 0, 0, 0, 1.0, "[]", buildId);

        JSONObject state = projectKnowledgeService.getState(project);
        assertTrue(state.optBoolean("known", false));
        assertNotNull(state.optJSONObject("lastBuild"));
        assertTrue(state.getJSONObject("lastBuild").optBoolean("success"));
        assertNotNull(state.optJSONObject("lastTest"));
        assertEquals(1.0, state.getJSONObject("lastTest").optDouble("passRate"), 0.001);
    }

    @Test
    public void componentsAndIssuesRoundTrip() {
        String project = "/tmp/pk-components-" + System.nanoTime();
        projectKnowledgeService.recordComponent(project, "eu.kalafatic.evolution.controller",
                "BUNDLE", "Core orchestration", "[\"orchestration\"]", "HIGH");
        projectKnowledgeService.recordKnownIssue(project, "Known issue title",
                "description", "MEDIUM", "OPEN", "workaround");

        assertEquals(1, projectKnowledgeService.countComponents(project));
        assertEquals(1, projectKnowledgeService.listKnownIssues(project, null, 10).size());
        assertEquals(1, projectKnowledgeService.listComponents(project, 10).size());
    }

    @Test
    public void decisionSupersessionIsTracked() {
        String project = "/tmp/pk-supersede-" + System.nanoTime();
        JSONObject first = projectKnowledgeService.recordDecision(project, null, "First decision",
                null, "decision v1", null, "ACCEPTED");
        JSONObject second = projectKnowledgeService.recordDecision(project, null, "Second decision",
                null, "decision v2", null, "ACCEPTED");
        projectKnowledgeService.supersedeDecision(project,
                first.optString("decisionId"), second.optString("decisionId"));

        JSONObject chain = projectKnowledgeService.getDecision(project, first.optString("decisionId"));
        assertEquals(second.optString("decisionId"), chain.optJSONObject("decision").optString("supersededBy"));
    }

    // ------------------------------------------------------------------
    // MemoryConsolidationService
    // ------------------------------------------------------------------

    @Test
    public void consolidationCreatesEpisodeAndExperienceEntries() throws Exception {
        String conversationId = "conv-consolidation-" + System.nanoTime();
        String sessionId = "session-consolidation-" + System.nanoTime();
        String project = "/tmp/consolidation-project";

        ConversationHistoryService history = ConversationHistoryService.getInstance();
        history.append(conversationId, "USER", null, null, "please fix the build", null);
        history.appendToolCall(conversationId, "MavenTool", "mvn compile");
        history.appendToolResult(conversationId, "MavenTool", "BUILD SUCCESS");

        MemoryConsolidationService consolidation = MemoryConsolidationService.getInstance();
        ConsolidationJob job = new ConsolidationJob(conversationId, sessionId, project,
                "tryst", "admin", true, "Build fixed successfully");
        consolidation.consolidate(job);

        boolean found = waitFor(() -> memoryService.listByType(MemoryType.EPISODIC).stream()
                .anyMatch(e -> conversationId.equals(e.getMetadata().get("conversationId"))), 10000);
        assertTrue("Consolidation must produce an EPISODE entry for the conversation", found);

        MemoryEntry episode = memoryService.listByType(MemoryType.EPISODIC).stream()
                .filter(e -> conversationId.equals(e.getMetadata().get("conversationId")))
                .findFirst().orElse(null);
        assertNotNull(episode);
        assertEquals(MemoryScope.PROJECT, episode.getScope());
        assertTrue("Episode must carry provenance",
                episode.getProvenanceChain().stream().anyMatch(p -> p.contains(conversationId)));
    }

    // ------------------------------------------------------------------
    // KnowledgeExtractionService
    // ------------------------------------------------------------------

    @Test
    public void extractionFindsRepeatedPreferencesAndFacts() {
        String conversationId = "conv-extraction-" + System.nanoTime();
        String sessionId = "session-extraction-" + System.nanoTime();
        String project = "/tmp/extraction-project";

        ConversationHistoryService history = ConversationHistoryService.getInstance();
        history.append(conversationId, "USER", null, null,
                "always use tabs for indentation in this project", null);
        history.append(conversationId, "USER", null, null,
                "please always use tabs for indentation in this project", null);
        history.append(conversationId, "USER", null, null,
                "the fact is verified: the MCP port is 28280", null);

        KnowledgeExtractionService extraction = KnowledgeExtractionService.getInstance();
        List<MemoryEntry> extracted = extraction.extractFromConversation(conversationId, sessionId,
                project, "tryst", "admin");
        assertTrue("Extraction must find the verified fact",
                extracted.stream().anyMatch(e -> e.getContent().contains("the MCP port is 28280")));
        assertTrue("Extraction must find the repeated preference",
                extracted.stream().anyMatch(e -> e.getType() == MemoryType.PREFERENCE
                        && e.getContent().contains("tabs")));
    }

    private interface Condition {
        boolean holds();
    }

    private boolean waitFor(Condition condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.holds()) {
                return true;
            }
            Thread.sleep(100);
        }
        return condition.holds();
    }
}
