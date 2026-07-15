package eu.kalafatic.evolution.controller.tests;

import static org.junit.Assert.*;
import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import eu.kalafatic.evolution.controller.orchestration.*;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class FinalResponseAssemblerTest {

    private Orchestrator orchestrator;
    private File tempDir;

    @Before
    public void setUp() throws Exception {
        orchestrator = OrchestrationFactory.eINSTANCE.createOrchestrator();
        tempDir = File.createTempFile("evo-test-root", "");
        tempDir.delete();
        tempDir.mkdirs();
    }

    @Test
    public void testAssembleWithSimpleGoalComplexity() throws Exception {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-simple");

        // Setup GoalModel in metadata with SIMPLE complexity
        GoalModel goalModel = new GoalModel();
        goalModel.setComplexity("SIMPLE");
        goalModel.setGoalType("CODE_GENERATION");
        context.getOrchestrationState().getMetadata().put("goalModel", goalModel);

        // Put a fake file change to ensure tracker has files
        context.getFileChangeTracker().recordChange("src/Test.java", FileChangeTracker.ChangeType.NEW);
        File f = new File(tempDir, "src/Test.java");
        f.getParentFile().mkdirs();
        f.createNewFile();

        FinalResponseAssembler assembler = new FinalResponseAssembler();
        FinalResponse response = assembler.assemble(context, "Completed simple test", true, Instant.now());

        assertNotNull(response);
        // Ensure files list in final response is completely empty for SIMPLE complexity
        assertTrue("Files should be empty for simple complexity", response.getFiles().isEmpty());

        // Ensure the string representation does not contain any file references
        String output = response.toString();
        assertFalse("Output should not contain Modified Files block", output.contains("Modified Files"));
        assertFalse("Output should not contain file links", output.contains("file://"));
        assertFalse("Output should not contain Repository Changes block", output.contains("Repository Changes"));
        assertFalse("Output should not contain Verification block", output.contains("Verification"));
    }

    @Test
    public void testAssembleWithSimpleChatMode() throws Exception {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-chat");

        // Set platform mode to SIMPLE_CHAT
        PlatformMode platformMode = new PlatformMode(PlatformType.SIMPLE_CHAT, null, 1, false);
        context.setPlatformMode(platformMode);

        // Put a fake file change
        context.getFileChangeTracker().recordChange("src/Test.java", FileChangeTracker.ChangeType.NEW);
        File f = new File(tempDir, "src/Test.java");
        f.getParentFile().mkdirs();
        f.createNewFile();

        FinalResponseAssembler assembler = new FinalResponseAssembler();
        FinalResponse response = assembler.assemble(context, "Completed chat test", true, Instant.now());

        assertNotNull(response);
        // Ensure files list in final response is completely empty for SIMPLE_CHAT mode
        assertTrue("Files should be empty for SIMPLE_CHAT", response.getFiles().isEmpty());

        String output = response.toString();
        assertFalse("Output should not contain Modified Files block", output.contains("Modified Files"));
        assertFalse("Output should not contain file links", output.contains("file://"));
    }

    @Test
    public void testAssembleStandardWorkflow() throws Exception {
        TaskContext context = new TaskContext(orchestrator, tempDir);
        context.setSessionId("test-session-standard");

        // Setup GoalModel in metadata with MEDIUM complexity
        GoalModel goalModel = new GoalModel();
        goalModel.setComplexity("MEDIUM");
        goalModel.setGoalType("CODE_GENERATION");
        context.getOrchestrationState().getMetadata().put("goalModel", goalModel);

        PlatformMode platformMode = new PlatformMode(PlatformType.ASSISTED_CODING, null, 1, false);
        context.setPlatformMode(platformMode);

        // Track files
        context.getFileChangeTracker().recordChange("src/Test.java", FileChangeTracker.ChangeType.NEW);
        File f = new File(tempDir, "src/Test.java");
        f.getParentFile().mkdirs();
        f.createNewFile();

        FinalResponseAssembler assembler = new FinalResponseAssembler();
        FinalResponse response = assembler.assemble(context, "Completed standard test", true, Instant.now());

        assertNotNull(response);
        // Ensure files are collected for non-simple workflows
        assertFalse("Files should not be empty for standard complexity", response.getFiles().isEmpty());

        String output = response.toString();
        assertTrue("Output should contain Modified Files block", output.contains("Modified Files"));
        assertTrue("Output should contain file links", output.contains("file://"));
    }
}
