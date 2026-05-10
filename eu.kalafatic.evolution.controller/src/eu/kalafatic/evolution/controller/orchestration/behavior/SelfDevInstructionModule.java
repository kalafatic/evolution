package eu.kalafatic.evolution.controller.orchestration.behavior;

public class SelfDevInstructionModule implements InstructionModule {
    @Override
    public String getInstructions() {
        return "WORKFLOW: SELF_DEV\n" +
               "→ Your task is to IMPROVE decision quality and system intelligence.\n" +
               "→ Think in terms of: STATE TRANSITIONS, SYSTEM IMPROVEMENT, LONG-TERM EFFECTS.\n" +
               "→ Each iteration must improve the system state, not just produce isolated code.";
    }
}
