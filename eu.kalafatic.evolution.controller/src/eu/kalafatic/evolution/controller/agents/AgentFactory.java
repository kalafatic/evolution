package eu.kalafatic.evolution.controller.agents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import eu.kalafatic.evolution.controller.orchestration.WebSearchAgent;
import eu.kalafatic.evolution.controller.orchestration.util.EvolutionConstants;

/**
 * Factory and Registry for AI Agents.
 *
 * @evo:1:1 reason=introduce-agent-factory
 */
public class AgentFactory {

    private static final Map<String, IAgent> agents = new ConcurrentHashMap<>();

    static {
        registerDefaultAgents();
    }

    public static synchronized void registerDefaultAgents() {
        // Clear existing to avoid state contamination between tests
        agents.clear();
        registerAgent(EvolutionConstants.AGENT_ANALYTIC, new AnalyticAgent());
        registerAgent(EvolutionConstants.AGENT_ARCHITECT, new ArchitectAgent());
        registerAgent(EvolutionConstants.AGENT_JAVA_DEV, new JavaDevAgent());
        registerAgent(EvolutionConstants.AGENT_TESTER, new TesterAgent());
        registerAgent(EvolutionConstants.AGENT_VALIDATOR, new ValidatorAgent());
        registerAgent(EvolutionConstants.AGENT_GENERAL, new GeneralAgent());
        registerAgent(EvolutionConstants.AGENT_TERMINAL, new TerminalAgent());
        registerAgent(EvolutionConstants.AGENT_FILE, new FileAgent());
        registerAgent(EvolutionConstants.AGENT_MAVEN, new MavenAgent());
        registerAgent(EvolutionConstants.AGENT_GIT, new GitAgent());
        registerAgent(EvolutionConstants.AGENT_STRUCTURE, new StructureAgent());
        registerAgent(EvolutionConstants.AGENT_WEB_SEARCH, new WebSearchAgent());
        registerAgent(EvolutionConstants.AGENT_QUALITY, new QualityAgent());
        registerAgent(EvolutionConstants.AGENT_OBSERVABILITY, new ObservabilityAgent());
        registerAgent(EvolutionConstants.AGENT_REPAIR, new RepairAgent());
        registerAgent(EvolutionConstants.AGENT_PLANNER, new PlannerAgent());
        registerAgent(EvolutionConstants.AGENT_PROPOSAL_CONSOLIDATOR, new ProposalConsolidatorAgent());
        registerAgent(EvolutionConstants.AGENT_CRITIC, new CriticAgent());
        registerAgent(EvolutionConstants.AGENT_FINAL_RESPONSE, new FinalResponseAgent());
    }

    public static void registerAgent(String type, IAgent agent) {
        agents.put(type, agent);
    }

    public static IAgent getAgent(String type) {
        return agents.get(type);
    }

    public static List<IAgent> getAllAgents() {
        return new ArrayList<>(agents.values());
    }
}
