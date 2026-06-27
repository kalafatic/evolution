package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

import eu.kalafatic.evolution.controller.orchestration.AiService;
import eu.kalafatic.evolution.controller.orchestration.IterationManager;
import eu.kalafatic.evolution.controller.orchestration.OrchestratorResponse;
import eu.kalafatic.evolution.controller.orchestration.SessionContainer;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.orchestration.TaskRequest;
import eu.kalafatic.evolution.controller.orchestration.goal.GoalModel;
import eu.kalafatic.evolution.controller.orchestration.intent.EvolutionAssessment;
import eu.kalafatic.evolution.controller.orchestration.capability.ICapability;
import eu.kalafatic.evolution.controller.orchestration.capability.contracts.IMutationContract;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityStatus;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityContext;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityException;
import eu.kalafatic.evolution.controller.orchestration.capability.CapabilityHealth;
import eu.kalafatic.evolution.controller.trajectory.Trajectory;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;

/**
 * DarwinEngine (Legacy) - For backward compatibility only. @Deprecated
 * Delegates to StandardDarwinEngine internally.
 */
@Deprecated
public class DarwinEngine extends AbstractBaseDarwinEngine implements ICapability, IMutationContract {
 
    private final StandardDarwinEngine delegate;
    private CapabilityStatus status = CapabilityStatus.STOPPED;

    public DarwinEngine(TaskContext context, IterationMemoryService memoryService,
                        SystemStateSignalProvider stateProvider, SessionContainer sessionContainer) {
        super(context, memoryService, sessionContainer);
        this.delegate = new StandardDarwinEngine(context, memoryService, context.getAiService());
        this.delegate.setSessionContainer(sessionContainer);
    }

    public DarwinEngine(TaskContext context, IterationMemoryService memoryService, SystemStateSignalProvider stateProvider) {
        this(context, memoryService, stateProvider, null);
    }

    @Override
    public OrchestratorResponse orchestrateEvolution(TaskRequest taskRequest, IterationManager iterationManager) throws Exception {
        return delegate.orchestrateEvolution(taskRequest, iterationManager);
    }

    @Override
    public OrchestratorResponse evolve(String request, IterationManager iterationManager, EvolutionAssessment initialAssessment) throws Exception {
        return delegate.evolve(request, iterationManager, initialAssessment);
    }

    @Override
    public EvaluationResult runIteration(GoalModel goal, IterationManager manager) throws Exception {
        return delegate.runIteration(goal, manager);
    }

    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, IterationManager manager) throws Exception {
        return delegate.generateVariants(goal, manager);
    }

    @Override
    public List<BranchVariant> validateVariants(List<BranchVariant> variants, IterationManager manager) {
        return delegate.validateVariants(variants, manager);
    }

    @Override
    public EvaluationResult executeWinner(BranchVariant winner, IterationManager manager) throws Exception {
        return delegate.executeWinner(winner, manager);
    }

    @Override
    public String getMode() {
        return delegate.getMode();
    }

    // ICapability METHODS
    @Override public String getCapabilityId() { return "capability.mutation"; }
    @Override public String getVersion() { return "1.0.0"; }
    @Override public CapabilityStatus getStatus() { return status; }
    @Override public void initialize(CapabilityContext context) throws CapabilityException { status = CapabilityStatus.INITIALIZED; }
    @Override public void start() throws CapabilityException { status = CapabilityStatus.STARTED; }
    @Override public void stop() throws CapabilityException { status = CapabilityStatus.STOPPED; }
    @Override public List<String> getSupportedContracts() { return Collections.singletonList(IMutationContract.ID); }
    @Override public List<String> getDependencies() { return Collections.emptyList(); }
    @Override public CapabilityHealth getHealth() { return new CapabilityHealth(1.0, "Healthy", 0); }

    // Legacy methods that might still be called by DarwinFlow
    public List<BranchVariant> generateProposals(TaskContext context, GoalModel goal, IterationManager manager) throws Exception {
        return delegate.generateVariants(goal, manager);
    }

    public EvaluationResult executeWinner(TaskContext context, eu.kalafatic.evolution.controller.supervision.EvolutionDecision decision, List<BranchVariant> variants, GoalModel goal, IterationManager manager) throws Exception {
        String winnerId = decision.getSelectedVariantId();
        BranchVariant winner = variants.stream().filter(v -> v.getId().equals(winnerId)).findFirst().orElse(null);
        if (winner != null) {
            return delegate.executeWinner(winner, manager);
        }
        return failedResult("No winner selected");
    }

    @Override
    public List<BranchVariant> generateVariants(GoalModel goal, StateSnapshot snapshot, FailureMemory failureMemory,
            Trajectory trajectory, EvolutionaryPressureVector pressure) throws Exception {
        // Full bridge to new engine
        return delegate.generateVariants(goal, null);
    }
}
