package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryRelationship;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemorySource;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.controller.memory.MemoryValidity;
import eu.kalafatic.evolution.controller.memory.MemoryVersion;

/**
 * Unified memory authority tests: versioning, validity lifecycle,
 * relationships, search and backward-compatible serialization.
 */
public class CognitiveMemoryServiceTest {

    private MemoryService service;

    @Before
    public void setUp() {
        service = MemoryService.getInstance();
        service.clearAll();
    }

    @After
    public void tearDown() {
        service.clearAll();
    }

    @Test
    public void storeCreatesVersionOneAndVersionRecord() {
        MemoryEntry entry = new MemoryEntry("EVO serves cognitive infrastructure",
                MemoryScope.USER, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        MemoryEntry stored = service.addEntry(entry);
        assertEquals(1, stored.getVersion());
        List<MemoryVersion> versions = service.getVersionHistory(stored.getId());
        assertEquals(1, versions.size());
        assertEquals(1, versions.get(0).getVersion());
        assertEquals(stored.getContent(), versions.get(0).getContentSnapshot());
    }

    @Test
    public void updateBumpsVersionAndAppendsHistory() {
        MemoryEntry entry = service.addEntry(new MemoryEntry("original content",
                MemoryScope.USER, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH));
        entry.setContent("updated content");
        MemoryEntry updated = service.updateEntry(entry);
        assertEquals(2, updated.getVersion());
        List<MemoryVersion> versions = service.getVersionHistory(updated.getId());
        assertEquals(2, versions.size());
        assertEquals(1, versions.get(0).getVersion());
        assertEquals(2, versions.get(1).getVersion());
    }

    @Test
    public void retractMarksRetractedButNeverDeletes() {
        MemoryEntry entry = service.addEntry(new MemoryEntry("temporary thought",
                MemoryScope.USER, MemoryType.OBSERVATION, MemorySource.SESSION, MemoryImportance.LOW));
        MemoryEntry retracted = service.retractEntry(entry.getId(), "no longer true", "admin");
        assertNotNull(retracted);
        assertEquals(MemoryValidity.RETRACTED, retracted.getValidity());
        assertNotNull("Retracted entry must remain persisted", service.getEntryById(entry.getId()));
        assertTrue("Retraction must leave a version record",
                service.getVersionHistory(entry.getId()).size() >= 2);
    }

    @Test
    public void supersedeUpdatesValidityAndRelationship() {
        MemoryEntry old = service.addEntry(new MemoryEntry("old fact",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.MEDIUM));
        MemoryEntry newer = service.addEntry(new MemoryEntry("new fact",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.MEDIUM));
        // "newer SUPERSEDES old"
        MemoryRelationship rel = service.addRelationship(newer.getId(), old.getId(),
                MemoryRelationship.Kind.SUPERSEDES);
        assertNotNull(rel);
        assertEquals(MemoryValidity.SUPERSEDED, service.getEntryById(old.getId()).getValidity());
        assertEquals(MemoryValidity.VALID, service.getEntryById(newer.getId()).getValidity());
    }

    @Test
    public void contradictionDetectionFindsRelationships() {
        MemoryEntry a = service.addEntry(new MemoryEntry("The port is 28080",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH));
        MemoryEntry b = service.addEntry(new MemoryEntry("The port is 28180",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH));
        service.addRelationship(a.getId(), b.getId(), MemoryRelationship.Kind.CONTRADICTS);
        List<MemoryRelationship> conflicts = service.findConflicts(a.getId());
        assertEquals(1, conflicts.size());
        assertEquals(MemoryRelationship.Kind.CONTRADICTS, conflicts.get(0).getKind());
    }

    @Test
    public void searchFindsStoredContent() {
        service.addEntry(new MemoryEntry("darwin orchestration memory",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH));
        service.addEntry(new MemoryEntry("completely unrelated note",
                MemoryScope.USER, MemoryType.OBSERVATION, MemorySource.USER, MemoryImportance.LOW));
        List<MemoryEntry> results = service.search("darwin orchestration", null, null, null, 10);
        assertTrue(results.size() >= 1);
        assertTrue(results.stream().anyMatch(e -> e.getContent().contains("darwin orchestration")));
    }

    @Test
    public void searchExcludesRetractedEntries() {
        MemoryEntry entry = service.addEntry(new MemoryEntry("secret retracted note",
                MemoryScope.USER, MemoryType.OBSERVATION, MemorySource.USER, MemoryImportance.LOW));
        service.retractEntry(entry.getId(), "test", "admin");
        List<MemoryEntry> results = service.search("secret retracted", null, null, null, 10);
        assertFalse(results.stream().anyMatch(e -> e.getId().equals(entry.getId())));
    }

    @Test
    public void legacyJsonLoadsWithSafeDefaults() {
        // A legacy entry without any of the unified-model fields
        String legacyJson = "{\"id\":\"legacy-1\",\"content\":\"legacy content\","
                + "\"scope\":\"USER\",\"type\":\"FACT\",\"source\":\"USER\","
                + "\"importance\":\"HIGH\",\"confidence\":0.9,"
                + "\"createdAt\":1700000000000,\"updatedAt\":1700000000000}";
        MemoryEntry restored = MemoryEntry.fromJsonObject(new org.json.JSONObject(legacyJson));
        assertNotNull(restored);
        assertEquals("legacy-1", restored.getId());
        assertEquals(MemoryValidity.VALID, restored.getValidity());
        assertEquals(1, restored.getVersion());
        assertTrue(restored.getRelationships().isEmpty());
        assertTrue(restored.getProvenanceChain().isEmpty());
    }

    @Test
    public void verifyRefreshesFreshnessAndValidity() {
        MemoryEntry entry = service.addEntry(new MemoryEntry("verifiable fact",
                MemoryScope.USER, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH));
        service.markOutdated(entry.getId(), "admin");
        assertEquals(MemoryValidity.OUTDATED, service.getEntryById(entry.getId()).getValidity());
        service.verifyEntry(entry.getId(), 0.95, "admin");
        MemoryEntry verified = service.getEntryById(entry.getId());
        assertEquals(MemoryValidity.VALID, verified.getValidity());
        assertTrue(verified.getLastVerifiedAt() > 0);
        assertTrue(verified.getFreshnessMs() < 60000);
    }
}
