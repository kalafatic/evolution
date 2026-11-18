package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.kalafatic.evolution.controller.cognitive.CognitiveContextBridge;
import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryRelationship;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemoryService;
import eu.kalafatic.evolution.controller.memory.MemorySource;
import eu.kalafatic.evolution.controller.memory.MemoryType;
import eu.kalafatic.evolution.controller.orchestration.ContextBuilder;
import eu.kalafatic.evolution.controller.orchestration.ContextPackage;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;

/**
 * Darwin <-> Cognitive Services native integration: ContextBuilder consumes
 * the platform context through the native facade (no HTTP), merges it into
 * the existing pipeline, respects the priority model, and degrades safely
 * when the services are unavailable.
 */
public class DarwinCognitiveIntegrationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Orchestrator orchestrator;
    private TaskContext context;
    private File projectRoot;
    private MemoryService memoryService;

    @Before
    public void setUp() throws Exception {
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        projectRoot = tempFolder.newFolder("darwin-cognitive-project");
        context = new TaskContext(orchestrator, projectRoot);
        File javaFile = new File(projectRoot, "MyClass.java");
        try (FileWriter writer = new FileWriter(javaFile)) {
            writer.write("package com.test;\n\npublic class MyClass {\n}\n");
        }
        memoryService = MemoryService.getInstance();
        memoryService.clearAll();
    }

    @After
    public void tearDown() {
        memoryService.clearAll();
    }

    private Task task(String name, String goal) {
        Task task = OrchestrationFactory.eINSTANCE.createTask();
        task.setName(name);
        task.setGoal(goal);
        return task;
    }

    @Test
    public void cognitiveContextIsRequestedAndMergedIntoPipeline() {
        MemoryEntry entry = new MemoryEntry("The Darwin engine uses IterationManager as the single state authority",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        entry.setProjectRef(projectRoot.getAbsolutePath());
        memoryService.addEntry(entry);

        ContextPackage pkg = ContextBuilder.build(task("Update IterationManager",
                "Inspect iteration transitions"), context);
        assertNotNull(pkg);
        String memoryContext = pkg.getMemoryContext();
        assertNotNull("Cognitive context must be merged into the Darwin package", memoryContext);
        assertTrue("Merged context must contain the platform knowledge: " + memoryContext,
                memoryContext.contains("single state authority"));
    }

    @Test
    public void projectScopeIsUsed() {
        MemoryEntry otherProject = new MemoryEntry("other-project-only secret architecture note",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        otherProject.setProjectRef(tempFolder.getRoot().getAbsolutePath() + "/other-project");
        memoryService.addEntry(otherProject);

        ContextPackage pkg = ContextBuilder.build(task("Update MyClass.java",
                "Add a method to MyClass"), context);
        String memoryContext = pkg.getMemoryContext() != null ? pkg.getMemoryContext() : "";
        assertTrue("Context must not leak other-project knowledge: " + memoryContext,
                !memoryContext.contains("other-project-only secret"));
    }

    @Test
    public void explicitInstructionsKeepPriorityOverCognitiveMemory() {
        MemoryEntry entry = new MemoryEntry("Prefer minimal changes in this module",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        entry.setProjectRef(projectRoot.getAbsolutePath());
        memoryService.addEntry(entry);

        Task task = task("Update MyClass.java", "Rewrite the module completely");
        ContextPackage pkg = ContextBuilder.build(task, context);
        String prompt = ContextBuilder.buildPrompt(pkg);

        int taskPosition = prompt.indexOf("### CURRENT STEP");
        int memoryPosition = prompt.indexOf("Prefer minimal changes");
        assertTrue("Prompt must contain both task and cognitive memory", taskPosition >= 0 && memoryPosition >= 0);
        assertTrue("Task instructions must come before cognitive memory (priority model)",
                taskPosition < memoryPosition);
    }

    @Test
    public void conflictsAreSurfacedInPrompt() {
        MemoryEntry a = new MemoryEntry("The configured HTTP port is 28080",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        a.setProjectRef(projectRoot.getAbsolutePath());
        memoryService.addEntry(a);
        MemoryEntry b = new MemoryEntry("The configured HTTP port is 28180",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        b.setProjectRef(projectRoot.getAbsolutePath());
        memoryService.addEntry(b);
        memoryService.addRelationship(a.getId(), b.getId(), MemoryRelationship.Kind.CONTRADICTS);

        ContextPackage pkg = ContextBuilder.build(task("Verify port configuration",
                "Check which port the HTTP server uses"), context);
        String memoryContext = pkg.getMemoryContext() != null ? pkg.getMemoryContext() : "";
        assertTrue("Conflicts must be surfaced in the Darwin prompt: " + memoryContext,
                memoryContext.contains("CONTEXT WARNINGS") && memoryContext.contains("conflict"));
    }

    @Test
    public void duplicateInformationIsNotRepeated() {
        MemoryEntry entry = new MemoryEntry("Unique Darwin note about IterationManager authority",
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        entry.setProjectRef(projectRoot.getAbsolutePath());
        memoryService.addEntry(entry);

        ContextPackage pkg = ContextBuilder.build(task("Update IterationManager",
                "Unique Darwin note about IterationManager authority"), context);
        String memoryContext = pkg.getMemoryContext() != null ? pkg.getMemoryContext() : "";
        int first = memoryContext.indexOf("Unique Darwin note");
        int last = memoryContext.lastIndexOf("Unique Darwin note");
        assertTrue("Cognitive memory must be present", first >= 0);
        assertEquals("Duplicate content must not be repeated", first, last);
    }

    @Test
    public void emptyStorePreservesExistingBehavior() {
        memoryService.clearAll();
        ContextPackage pkg = ContextBuilder.build(task("Update MyClass.java",
                "Add a method to MyClass"), context);
        assertNotNull(pkg);
        assertTrue("Scope must still contain the file",
                pkg.getScope().contains("MyClass.java"));
        String prompt = ContextBuilder.buildPrompt(pkg);
        assertTrue("Prompt must keep the standard structure", prompt.contains("### GOAL"));
    }

    @Test
    public void bridgeFailureDoesNotBreakContextBuild() {
        // Degradation guards: a null TaskContext yields an empty section
        // without throwing, and ContextBuilder stays usable end-to-end.
        String section = CognitiveContextBridge.buildCognitiveContext(null, null, new ContextPackage());
        assertEquals("Bridge must degrade to an empty section when context is unavailable", "", section);

        ContextPackage pkg = ContextBuilder.build(task("Update MyClass.java", "Add a method"), context);
        assertNotNull(pkg);
        String prompt = ContextBuilder.buildPrompt(pkg);
        assertTrue("Prompt must remain well-formed after any failure", prompt.contains("### GOAL"));
    }

    @Test
    public void bridgeSectionRespectsTokenBudget() {
        MemoryEntry entry = new MemoryEntry("budget test entry " + "knowledge ".repeat(300),
                MemoryScope.PROJECT, MemoryType.FACT, MemorySource.USER, MemoryImportance.HIGH);
        entry.setProjectRef(projectRoot.getAbsolutePath());
        memoryService.addEntry(entry);

        int budget = 1200;
        String section = CognitiveContextBridge.buildCognitiveContext(
                task("knowledge", "budget test entry"), context, new ContextPackage(), budget);
        int estimated = section.length() / 4;
        assertTrue("Bridge section must respect its token budget (est " + estimated
                + " vs budget " + budget + ")", estimated <= budget + 50);
    }
}
