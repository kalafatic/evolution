package eu.kalafatic.evolution.controller.agents;

import eu.kalafatic.evolution.controller.tools.FileTool;
import eu.kalafatic.evolution.controller.tools.ShellTool;

/**
 * Agent specialized in Project Structure and Analysis.
 */
public class StructureAgent extends BaseAiAgent {
    public StructureAgent() {
        super("Structure", "Structure");
        addTool(new ShellTool());
        addTool(new FileTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are an AI Structure Agent. Your goal is to analyze the project's directory structure and codebase organization.\n" +
               "Use tools like ls-tree or file reading to understand and report on how the project is built.\n\n" +
               "CRITICAL: When analyzing a project, you MUST determine its nature by inspecting key files and layout:\n" +
               "- Maven: presence of pom.xml\n" +
               "- Git: presence of .git directory\n" +
               "- Java: presence of .java files and src/main/java layout\n" +
               "- Python: presence of .py files, requirements.txt, or pyproject.toml\n" +
               "- Web/HTML: presence of .html, .css, .js files\n" +
               "- OSGi/Eclipse: presence of MANIFEST.MF or plugin.xml\n\n" +
               "Propose a summary that includes the detected technology stack and project type.";
    }
}
