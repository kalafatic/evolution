package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.json.JSONObject;
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

public class UserMemorySubsystemTest {

    private MemoryService service;

    @Before
    public void setUp() {
        service = MemoryService.getInstance();
        service.clearAll();
    }

    @Test
    public void testMemoryEntryJsonRoundtrip() {
        MemoryEntry entry = new MemoryEntry(
                "User prefers incremental refactoring over big rewrites",
                MemoryScope.USER,
                MemoryType.PREFERENCE,
                MemorySource.USER,
                MemoryImportance.HIGH
        );
        entry.getMetadata().put("author", "Jules");

        JSONObject json = entry.toJsonObject();
        assertNotNull("JSON output should not be null", json);
        assertEquals("USER", json.getString("scope"));
        assertEquals("PREFERENCE", json.getString("type"));

        MemoryEntry restored = MemoryEntry.fromJsonObject(json);
        assertNotNull("Restored entry should not be null", restored);
        assertEquals(entry.getId(), restored.getId());
        assertEquals(entry.getContent(), restored.getContent());
        assertEquals(entry.getScope(), restored.getScope());
        assertEquals(entry.getType(), restored.getType());
        assertEquals(entry.getSource(), restored.getSource());
        assertEquals(entry.getImportance(), restored.getImportance());
        assertEquals("Jules", restored.getMetadata().get("author"));
    }

    @Test
    public void testMemoryServiceCrudOperations() {
        assertEquals(0, service.getAllEntries().size());

        MemoryEntry entry1 = new MemoryEntry("EVO uses Java 21", MemoryScope.PROJECT, MemoryType.FACT, MemorySource.SYSTEM, MemoryImportance.HIGH);
        MemoryEntry saved1 = service.addEntry(entry1);
        assertNotNull("Saved entry should have an ID", saved1.getId());
        assertEquals(1, service.getAllEntries().size());

        MemoryEntry entry2 = new MemoryEntry("User prefers XML over YAML", MemoryScope.USER, MemoryType.PREFERENCE, MemorySource.USER, MemoryImportance.MEDIUM);
        service.addEntry(entry2);
        assertEquals(2, service.getAllEntries().size());

        MemoryEntry fetched = service.getEntryById(saved1.getId());
        assertNotNull("Fetched entry should not be null", fetched);
        assertEquals("EVO uses Java 21", fetched.getContent());

        // Update
        fetched.setContent("EVO core platform uses Java 21");
        service.updateEntry(fetched);

        MemoryEntry updated = service.getEntryById(saved1.getId());
        assertEquals("EVO core platform uses Java 21", updated.getContent());

        // Delete
        boolean deleted = service.deleteEntry(saved1.getId());
        assertTrue("Delete should return true for existing entry", deleted);
        assertEquals(1, service.getAllEntries().size());
        assertNull("Deleted entry should not be fetchable", service.getEntryById(saved1.getId()));
    }

    @Test
    public void testMemoryQueryFiltering() {
        service.addEntry("EVO uses Java 21", MemoryScope.PROJECT, MemoryType.FACT, MemorySource.SYSTEM, MemoryImportance.HIGH);
        service.addEntry("User prefers incremental refactoring", MemoryScope.USER, MemoryType.PREFERENCE, MemorySource.USER, MemoryImportance.CRITICAL);
        service.addEntry("Do not modify public API without review", MemoryScope.PROJECT, MemoryType.CONSTRAINT, MemorySource.USER, MemoryImportance.HIGH);

        MemoryQuery userQuery = new MemoryQuery().setScope(MemoryScope.USER);
        List<MemoryEntry> userResults = service.retrieveRelevant(userQuery);
        assertEquals(1, userResults.size());
        assertEquals(MemoryType.PREFERENCE, userResults.get(0).getType());

        MemoryQuery typeQuery = new MemoryQuery().setType(MemoryType.FACT);
        List<MemoryEntry> typeResults = service.retrieveRelevant(typeQuery);
        assertEquals(1, typeResults.size());
        assertEquals(MemoryScope.PROJECT, typeResults.get(0).getScope());

        MemoryQuery searchQuery = new MemoryQuery().setSearchText("refactoring");
        List<MemoryEntry> searchResults = service.retrieveRelevant(searchQuery);
        assertEquals(1, searchResults.size());
        assertTrue(searchResults.get(0).getContent().contains("incremental refactoring"));
    }

    @Test
    public void testMemoryContextProviderFormatting() {
        service.addEntry("User prefers concise git commit messages", MemoryScope.USER, MemoryType.PREFERENCE, MemorySource.USER, MemoryImportance.HIGH);
        service.addEntry("Project constraint: Do not introduce Spring framework", MemoryScope.PROJECT, MemoryType.CONSTRAINT, MemorySource.USER, MemoryImportance.CRITICAL);

        MemoryContextProvider provider = new MemoryContextProvider();
        String context = provider.buildMemoryContext("git commit");

        assertNotNull("Context should not be null", context);
        assertTrue("Context should contain header", context.contains("### USER & PROJECT MEMORY"));
        assertTrue("Context should contain preference", context.contains("concise git commit messages"));

        // Delete entry and verify context updates
        service.clearAll();
        String emptyContext = provider.buildMemoryContext("git commit");
        assertEquals("", emptyContext);
    }
}
