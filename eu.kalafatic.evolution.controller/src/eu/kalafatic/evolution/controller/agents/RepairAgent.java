package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.tools.MavenTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;

/**
 * Specialized agent for fixing build failures and technical errors.
 * It analyzes logs and provides surgical code corrections.
 */
public class RepairAgent extends BaseAiAgent {

    public RepairAgent() {
        super("Repair", "Repair");
        addTool(new FileTool());
        addTool(new MavenTool());
        addTool(new ShellTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Repair Specialist. Your goal is to analyze build failures, compilation errors, or test failures and provide minimal, surgical fixes.\n\n" +
               "STRATEGY:\n" +
               "1. Analyze the provided error logs carefully.\n" +
               "2. Identify the exact file and line number causing the issue.\n" +
               "3. Propose a fix that addresses the root cause without refactoring unrelated code.\n" +
               "4. If a dependency is missing, suggest adding it to the POM.\n" +
               "5. If a method signature is mismatched, correct the caller or the definition based on project context.\n\n" +
               "Evo-Way: Always include the full code implementation of the fix in your response or in the 'implementation' field of a JSON plan.";
    }

    @Override
    protected String getFooterInstructions() {
        return "Provide ONLY the corrected code content or a concise explanation with the fix. " +
               "If providing code for a file, do not include markdown backticks unless it's a markdown file.";
    }
}
