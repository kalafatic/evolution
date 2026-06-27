package eu.kalafatic.evolution.controller.agents;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.util.ArrayList;
import java.util.List;
import eu.kalafatic.evolution.controller.orchestration.WebSearchAgent;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;

/**
 * Factory for AI Agents.
 */
public class AgentFactory {

    /**
     * Creates new instances of all default agents for session-specific isolation.
     */
    public static List<IAgent> createIsolatedAgents(SessionContainer container) {
        List<IAgent> isolated = new ArrayList<>();
        isolated.add(new AnalyticAgent(container));
        isolated.add(new ArchitectAgent(container));
        isolated.add(new JavaDevAgent(container));
        isolated.add(new TesterAgent(container));
        isolated.add(new ValidatorAgent(container));
        isolated.add(new GeneralAgent(container));
        isolated.add(new TerminalAgent(container));
        isolated.add(new FileAgent(container));
        isolated.add(new MavenAgent(container));
        isolated.add(new GitAgent(container));
        isolated.add(new StructureAgent(container));
        isolated.add(new WebSearchAgent(container));
        isolated.add(new QualityAgent(container));
        isolated.add(new ObservabilityAgent(container));
        isolated.add(new RepairAgent(container));
        isolated.add(new PlannerAgent(container));
        isolated.add(new ProposalConsolidatorAgent(container));
        isolated.add(new CriticAgent(container));
        isolated.add(new FinalResponseAgent(container));
        return isolated;
    }

    /**
     * @deprecated Use createIsolatedAgents(SessionContainer) for strict session isolation.
     */
    @Deprecated
    public static List<IAgent> getAllAgents() {
        return new ArrayList<>();
    }
}
