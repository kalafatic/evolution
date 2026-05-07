package eu.kalafatic.evolution.controller.orchestration.util;

/**
 * Centralized constants for the Evolution orchestration system.
 *
 * @evo:1:1 reason=introduce-evolution-constants
 */
public final class EvolutionConstants {

    private EvolutionConstants() {}

    // Tool Names
    public static final String TOOL_FILE = "FileTool";
    public static final String TOOL_MAVEN = "MavenTool";
    public static final String TOOL_GIT = "GitTool";
    public static final String TOOL_SHELL = "ShellTool";
    public static final String TOOL_ECLIPSE = "EclipseTool";
    public static final String TOOL_CPP = "CppTool";
    public static final String TOOL_DATABASE = "DatabaseTool";

    // Agent Types
    public static final String AGENT_ANALYTIC = "Analytic";
    public static final String AGENT_ARCHITECT = "Architect";
    public static final String AGENT_JAVA_DEV = "JavaDev";
    public static final String AGENT_TESTER = "Tester";
    public static final String AGENT_VALIDATOR = "Validator";
    public static final String AGENT_GENERAL = "General";
    public static final String AGENT_TERMINAL = "Terminal";
    public static final String AGENT_FILE = "File";
    public static final String AGENT_MAVEN = "Maven";
    public static final String AGENT_GIT = "Git";
    public static final String AGENT_STRUCTURE = "Structure";
    public static final String AGENT_WEB_SEARCH = "WebSearch";
    public static final String AGENT_QUALITY = "Quality";
    public static final String AGENT_OBSERVABILITY = "Observability";
    public static final String AGENT_REPAIR = "Repair";
    public static final String AGENT_FINAL_RESPONSE = "FinalResponse";
    public static final String AGENT_PLANNER = "Planner";
    public static final String AGENT_PROPOSAL_CONSOLIDATOR = "ProposalConsolidator";
    public static final String AGENT_CRITIC = "Critic";

    // Task Types
    public static final String TASK_FILE = "file";
    public static final String TASK_MAVEN = "maven";
    public static final String TASK_GIT = "git";
    public static final String TASK_SHELL = "shell";
    public static final String TASK_TERMINAL = "terminal";
    public static final String TASK_APPROVAL = "approval";

    // Default Values
    public static final float DEFAULT_TEMPERATURE = 0.7f;
    public static final int MAX_TASK_RETRIES = 3;
    public static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.75;

    public static final int MAX_PLANNING_ITERATIONS = 3;
    public static final double PLANNING_QUALITY_THRESHOLD = 0.9;
}
