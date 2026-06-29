package eu.kalafatic.evolution.supervisor.bootstrap;

import java.io.IOException;

public class EvolutionCommandTool {
    private final SupervisorCommandTool commandTool;

    public EvolutionCommandTool(SupervisorCommandTool commandTool) {
        this.commandTool = commandTool;
    }

    public EvolutionResponse requestEvolution(EvolutionRequest request) {
        SupervisorCommand command = new SupervisorCommand("EVOLVE");
        command.addParameter("target", request.getTarget());
        command.addParameter("iterationBudget", request.getIterationBudget());
        if (request.getWorkspacePath() != null) {
            command.addParameter("workspace", request.getWorkspacePath());
        }

        try {
            SupervisorResponse response = commandTool.sendCommand(command);
            EvolutionResponse evoResponse = new EvolutionResponse(response.isSuccess(), response.getMessage());
            if (response.getData().containsKey("evolutionId")) {
                evoResponse.setEvolutionId((String) response.getData().get("evolutionId"));
            }
            return evoResponse;
        } catch (IOException e) {
            return new EvolutionResponse(false, "Failed to send evolution command: " + e.getMessage());
        }
    }
}
