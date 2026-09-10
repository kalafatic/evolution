package eu.kalafatic.evolution.controller.orchestration.selfdev;

public abstract class AbstractSupervisorLifecycle implements SupervisorLifecycle {
    protected final SupervisorBuilder builder;
    protected final Deployer<BuildArtifact> deployer;
    protected final SupervisorRuntime runtime;

    protected AbstractSupervisorLifecycle(SupervisorBuilder builder,
                                          Deployer<BuildArtifact> deployer,
                                          SupervisorRuntime runtime) {
        this.builder = builder;
        this.deployer = deployer;
        this.runtime = runtime;
    }
}
