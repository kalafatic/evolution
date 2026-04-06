package eu.kalafatic.evolution.controller.orchestration;

/**
 * Specialized agent for C and C++ development tasks.
 */
public class CppDevAgent extends BaseAiAgent {
    public CppDevAgent() {
        super("CppDev", "CppDev");
        addTool(new FileTool());
        addTool(new CppTool());
        addTool(new GitTool());
        addTool(new ShellTool());
    }

    @Override
    protected String getAgentInstructions() {
        return "You are acting as a Senior C/C++ Developer Agent.\n" +
               "Generate C/C++ source code, headers, Makefiles or CMakeLists.txt content as requested. Provide ONLY the code content for files.";
    }
}
