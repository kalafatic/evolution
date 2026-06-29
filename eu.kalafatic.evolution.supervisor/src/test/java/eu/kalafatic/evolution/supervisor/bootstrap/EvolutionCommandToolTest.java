package eu.kalafatic.evolution.supervisor.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;

public class EvolutionCommandToolTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testRequestEvolution() throws IOException {
        File runDir = folder.newFolder("run");
        SupervisorCommandTool commandTool = new SupervisorCommandTool(runDir);
        EvolutionCommandTool tool = new EvolutionCommandTool(commandTool);

        EvolutionRequest request = new EvolutionRequest("architecture");
        request.setIterationBudget(5);

        // Mock response
        new Thread(() -> {
            try {
                Thread.sleep(200);
                SupervisorResponse response = new SupervisorResponse(true, "Evolving...");
                response.addData("evolutionId", "evo-123");
                new ObjectMapper().writeValue(new File(runDir, "response.json"), response);
            } catch (Exception e) {}
        }).start();

        EvolutionResponse response = tool.requestEvolution(request);

        assertTrue(response.isAcknowledged());
        assertEquals("evo-123", response.getEvolutionId());

        // Verify command.json
        File commandFile = new File(runDir, "command.json");
        assertTrue(commandFile.exists());
        SupervisorCommand command = new ObjectMapper().readValue(commandFile, SupervisorCommand.class);
        assertEquals("EVOLVE", command.getType());
        assertEquals("architecture", command.getParameters().get("target"));
    }
}
