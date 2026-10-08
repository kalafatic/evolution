package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.memory.MemoryContextProvider;
import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryQuery;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemorySource;
import eu.kalafatic.evolution.controller.memory.MemoryType;

public class MemoryMenuIntegrationTest {

    private MemoryService memoryService;

    @Before
    public void setUp() {
        memoryService = MemoryService.getInstance();
        assertNotNull("MemoryService instance must be available", memoryService);
        memoryService.clearAll();
    }

    @After
    public void tearDown() {
        memoryService.clearAll();
    }

    @Test
    public void testSingleMemoryServiceAuthoritativeStore() {
        // Main menu entry point add
        MemoryEntry userEntry = new MemoryEntry("User prefers dark theme", MemoryScope.USER, MemoryType.PREFERENCE, MemorySource.USER, MemoryImportance.HIGH);
        memoryService.addEntry(userEntry);

        // Properties tab entry point add
        MemoryEntry projEntry = new MemoryEntry("Project requires Java 21 LTS", MemoryScope.PROJECT, MemoryType.CONSTRAINT, MemorySource.SYSTEM, MemoryImportance.CRITICAL);
        projEntry.setAssociatedId("test-project-123");
        memoryService.addEntry(projEntry);

        // Server tab entry point add
        MemoryEntry sessionEntry = new MemoryEntry("Server session running on port 8089", MemoryScope.SESSION, MemoryType.FACT, MemorySource.SYSTEM, MemoryImportance.MEDIUM);
        sessionEntry.setAssociatedId("SERVER");
        memoryService.addEntry(sessionEntry);

        // Verify all 3 entries stored in single service
        List<MemoryEntry> allEntries = memoryService.getAllEntries();
        assertEquals("MemoryService must contain exactly 3 entries", 3, allEntries.size());

        // Test scope filtering for Properties tab
        MemoryQuery projQuery = new MemoryQuery().setScope(MemoryScope.PROJECT).setAssociatedId("test-project-123");
        List<MemoryEntry> projResults = memoryService.retrieveRelevant(projQuery);
        assertEquals("Properties query should return 1 project entry", 1, projResults.size());
        assertEquals("Java 21 LTS constraint match", "Project requires Java 21 LTS", projResults.get(0).getContent());

        // Test scope filtering for Server tab
        MemoryQuery sessionQuery = new MemoryQuery().setScope(MemoryScope.SESSION);
        List<MemoryEntry> sessionResults = memoryService.retrieveRelevant(sessionQuery);
        assertEquals("Server query should return 1 session entry", 1, sessionResults.size());
        assertEquals("Server session fact match", "Server session running on port 8089", sessionResults.get(0).getContent());

        // Test Context Provider prompt generation
        MemoryContextProvider provider = new MemoryContextProvider();
        String promptContext = provider.buildMemoryContext("Java 21", MemoryScope.PROJECT, "test-project-123", 5);
        assertNotNull("Prompt context should not be null", promptContext);
        assertTrue("Prompt context should include Java 21 fact", promptContext.contains("Java 21 LTS"));
    }
}
