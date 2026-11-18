package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.cognitive.CognitiveContextService;
import eu.kalafatic.evolution.controller.cognitive.model.ContextPackage;
import eu.kalafatic.evolution.controller.cognitive.model.ContextRequest;
import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryRelationship;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemorySource;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.servers.database.DatabaseManager;
import eu.kalafatic.evolution.servers.service.CognitiveScopes;
import eu.kalafatic.evolution.servers.service.ValidatedPrincipal;

import java.util.List;
import java.util.Set;

/**
 * Context reconstruction: memory selection, scope isolation, budget
 * enforcement, conflict surfacing and provenance.
 */
public class ContextReconstructionTest {

    private MemoryService memoryService;

    @Before
    public void setUp() {
        memoryService = MemoryService.getInstance();
        memoryService.clearAll();
    }

    @After
    public void tearDown() {
        memoryService.clearAll();
    }

    private ContextPackage reconstruct(String project, String intent, int budget, String userId) {
        ContextRequest request = new ContextRequest();
        request.setProject(project);
        request.setIntent(intent);
        request.setBudgetTokens(budget);
        request.setUserId(userId);
        return CognitiveContextService.getInstance().getContext(request);
    }

    @Test
    public void relevantMemoryIsSelectedByIntent() {
        MemoryEntry relevant = new MemoryEntry("The MCP server runs on port 28280",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        relevant.setProjectRef("/tmp/ctxproj");
        memoryService.addEntry(relevant);
        MemoryEntry unrelated = new MemoryEntry("The garden has tulips",
                MemoryScope.USER, MemoryType.OBSERVATION, MemorySource.USER, MemoryImportance.LOW);
        memoryService.addEntry(unrelated);

        ContextPackage pkg = reconstruct("/tmp/ctxproj", "which port does the MCP server use", 12000, null);
        assertNotNull(pkg);
        assertTrue("Relevant memory must be selected",
                pkg.getMemories().stream().anyMatch(i -> i.getContent().contains("28280")));
        assertEquals("1.0", pkg.getSchemaVersion());
    }

    @Test
    public void projectIsolationFiltersOtherProjects() {
        MemoryEntry projectA = new MemoryEntry("project A secret decision",
                MemoryScope.PROJECT, MemoryType.DECISION, MemorySource.USER, MemoryImportance.HIGH);
        projectA.setProjectRef("/tmp/project-a");
        memoryService.addEntry(projectA);

        ContextPackage pkg = reconstruct("/tmp/project-b", "secret decision", 12000, null);
        assertTrue("Project B context must not contain project A memories",
                pkg.getMemories().stream().noneMatch(i -> i.getContent().contains("project A secret")));
    }

    @Test
    public void contradictionsAreSurfacedNotHidden() {
        MemoryEntry a = new MemoryEntry("The API uses JSON",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        a.setProjectRef("/tmp/conflictproj");
        memoryService.addEntry(a);
        MemoryEntry b = new MemoryEntry("The API uses XML",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        b.setProjectRef("/tmp/conflictproj");
        memoryService.addEntry(b);
        memoryService.addRelationship(a.getId(), b.getId(), MemoryRelationship.Kind.CONTRADICTS);

        ContextPackage pkg = reconstruct("/tmp/conflictproj", "which format does the API use", 12000, null);
        assertTrue("Conflicts must appear in warnings",
                pkg.getWarnings().stream().anyMatch(w -> w.toLowerCase().contains("conflict")));
    }

    @Test
    public void budgetIsRespected() {
        MemoryEntry big = new MemoryEntry("large content entry " + "x".repeat(2000),
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        big.setProjectRef("/tmp/budgetproj");
        memoryService.addEntry(big);

        ContextPackage pkg = reconstruct("/tmp/budgetproj", "large content", 600, null);
        assertTrue("Token estimate must respect a tight budget (estimate " + pkg.getTokenEstimate()
                + " vs budget " + pkg.getBudgetTokens() + ")",
                pkg.getTokenEstimate() <= pkg.getBudgetTokens());
    }

    @Test
    public void identityParamsDoNotOverridePrincipal() {
        MemoryEntry ownedByAdmin = new MemoryEntry("admin's private working notes",
                MemoryScope.USER, MemoryType.WORKING, MemorySource.USER, MemoryImportance.MEDIUM);
        ownedByAdmin.setUserId("admin");
        memoryService.addEntry(ownedByAdmin);

        ValidatedPrincipal trystPrincipal = new ValidatedPrincipal("tryst-user", "tryst", 1L,
                Set.of(CognitiveScopes.CONTEXT_GET, CognitiveScopes.MEMORY_READ));

        ContextRequest request = new ContextRequest();
        request.setIntent("private working notes");
        request.setBudgetTokens(12000);
        request.setUserId("admin"); // spoofing attempt
        ContextPackage pkg = CognitiveContextService.getInstance().getContext(request, trystPrincipal);
        assertTrue("A spoofed userId must never leak another user's USER-scoped memory",
                pkg.getMemories().stream().noneMatch(i -> i.getContent().contains("private working notes")));
    }

    @Test
    public void packageCarriesProvenanceAndAssessment() {
        MemoryEntry entry = new MemoryEntry("verified knowledge about context",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        entry.setProjectRef("/tmp/provenanceproj");
        entry.setLastVerifiedAt(System.currentTimeMillis());
        memoryService.addEntry(entry);

        ContextPackage pkg = reconstruct("/tmp/provenanceproj", "verified knowledge", 12000, null);
        assertNotNull(pkg.getContextId());
        assertTrue("Sources must carry provenance", !pkg.getSources().isEmpty());
        assertTrue("Token estimate must be measured", pkg.getTokenEstimate() > 0);
        assertNotNull("Assessment must be attached", pkg.getAssessment());
        assertTrue("Assessment quality in range", pkg.getAssessment().getQuality() >= 0.0
                && pkg.getAssessment().getQuality() <= 1.0);
    }

    @Test
    public void sessionScopedEntriesRequireMatchingSession() {
        // Session-scoped memory tied to session A must never appear in a
        // request without a session or with a different session.
        MemoryEntry privateNote = new MemoryEntry("session A private scratchpad note",
                MemoryScope.SESSION, MemoryType.WORKING, MemorySource.SESSION, MemoryImportance.MEDIUM);
        privateNote.setProjectRef("/tmp/sessionscopeproj");
        privateNote.setSessionRef("session-A");
        memoryService.addEntry(privateNote);

        ContextRequest withoutSession = new ContextRequest();
        withoutSession.setProject("/tmp/sessionscopeproj");
        withoutSession.setIntent("private scratchpad note");
        withoutSession.setBudgetTokens(12000);
        ContextPackage pkgNoSession = CognitiveContextService.getInstance().getContext(withoutSession);
        assertTrue("Session-scoped entry must not leak to requests without a session",
                pkgNoSession.getMemories().stream()
                        .noneMatch(i -> i.getContent().contains("session A private scratchpad")));

        ContextRequest otherSession = new ContextRequest();
        otherSession.setProject("/tmp/sessionscopeproj");
        otherSession.setSessionId("session-B");
        otherSession.setIntent("private scratchpad note");
        otherSession.setBudgetTokens(12000);
        ContextPackage pkgOther = CognitiveContextService.getInstance().getContext(otherSession);
        assertTrue("Session-scoped entry must not leak to a different session",
                pkgOther.getMemories().stream()
                        .noneMatch(i -> i.getContent().contains("session A private scratchpad")));

        ContextRequest sameSession = new ContextRequest();
        sameSession.setProject("/tmp/sessionscopeproj");
        sameSession.setSessionId("session-A");
        sameSession.setIntent("private scratchpad note");
        sameSession.setBudgetTokens(12000);
        ContextPackage pkgSame = CognitiveContextService.getInstance().getContext(sameSession);
        assertTrue("The owning session must see its session-scoped entry",
                pkgSame.getMemories().stream()
                        .anyMatch(i -> i.getContent().contains("session A private scratchpad")));
    }
}
