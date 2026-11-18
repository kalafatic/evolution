package eu.kalafatic.evolution.controller.contextual;

/**
 * Singleton holder for the EVO contextual action registry (Darwin Family Contextual
 * Action Contract v1.0). Initialized lazily with the connected-application harness
 * registry; consumed by the RCP surfaces and the EvolutionServer web surface.
 */
public final class EvoContextualRegistry {

    private static final ContextualActionRegistry INSTANCE = create();

    private EvoContextualRegistry() {
    }

    public static ContextualActionRegistry getInstance() {
        return INSTANCE;
    }

    private static ContextualActionRegistry create() {
        ContextualActionRegistry registry = new DefaultContextualActionRegistry();
        try {
            eu.kalafatic.evolution.controller.harness.ConnectedApplicationRegistry
                    harnessRegistry =
                    eu.kalafatic.evolution.controller.harness.ConnectedApplicationRegistry
                            .getInstance();
            EvoContextualActions.install(registry, harnessRegistry);
        } catch (Exception e) {
            System.err.println("[EVO] Contextual action registry init failed: "
                    + e.getMessage());
        }
        return registry;
    }
}
