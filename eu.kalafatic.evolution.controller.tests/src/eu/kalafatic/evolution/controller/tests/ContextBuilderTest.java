package eu.kalafatic.evolution.controller.tests;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;
import eu.kalafatic.evolution.controller.orchestration.SessionManager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import eu.kalafatic.evolution.controller.orchestration.ContextBuilder;
import eu.kalafatic.evolution.controller.orchestration.ContextPackage;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;

public class ContextBuilderTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Orchestrator orchestrator;
    private TaskContext context;
    private File projectRoot;

    @Before
    public void setUp() throws Exception {
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        projectRoot = tempFolder.newFolder("testProject");
        context = new TaskContext(orchestrator, projectRoot);

        // Create a dummy Java file
        File javaFile = new File(projectRoot, "MyClass.java");
        try (FileWriter writer = new FileWriter(javaFile)) {
            writer.write("package com.test;\n\n");
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;
            writer.write("import java.util.List;\n\n");
            writer.write("public class MyClass {\n");
            writer.write("    public void myMethod() {\n");
            writer.write("        System.out.println(\"Hello\");\n");
            writer.write("    }\n");
            writer.write("}\n");
        }
    }

    @Test
    public void testBuildMinimalContext() {
        Task task = OrchestrationFactory.eINSTANCE.createTask();
        task.setName("Update MyClass.java");
        task.setGoal("Add a new method to MyClass");

        ContextPackage pkg = ContextBuilder.build(task, context);

        assertNotNull(pkg);
        assertEquals("Update MyClass.java", pkg.getStep());
        assertEquals("Add a new method to MyClass", pkg.getGoal());
        assertTrue("Scope should contain MyClass.java", pkg.getScope().contains("MyClass.java"));
        assertTrue("Code should contain package info", pkg.getCode().contains("package com.test;"));
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;
        assertTrue("Dependencies should contain MyClass.java", pkg.getDependencies().contains("MyClass.java"));
        assertTrue("Constraints should not be empty", !pkg.getConstraints().isEmpty());
    }

    @Test
    public void testBuildPrompt() {
        ContextPackage pkg = new ContextPackage();
        pkg.setGoal("Test Goal");
        pkg.setStep("Test Step");
        pkg.setCode("public class Test {}");
        pkg.getConstraints().add("Constraint 1");
        pkg.setDependencies("- File: Test.java\n  - uses java.util.List");

        String prompt = ContextBuilder.buildPrompt(pkg);

        assertNotNull(prompt);
        assertTrue(prompt.contains("### GOAL\nTest Goal"));
        assertTrue(prompt.contains("### CURRENT STEP\nTest Step"));
        assertTrue(prompt.contains("Constraint 1"));
        assertTrue(prompt.contains("public class Test {}"));
    }

    @Test
    public void testFileSelectionWithMarkers() {
        Task task = OrchestrationFactory.eINSTANCE.createTask();
        task.setName("Refactor code");
        task.setDescription("Check [FILE:src/App.java] and [FILE:src/Utils.java]");

        ContextPackage pkg = ContextBuilder.build(task, context);

        assertTrue(pkg.getScope().contains("src/App.java"));
        assertTrue(pkg.getScope().contains("src/Utils.java"));
    }

    private void assertEquals(String expected, String actual) {
        org.junit.Assert.assertEquals(expected, actual);
    }
}
