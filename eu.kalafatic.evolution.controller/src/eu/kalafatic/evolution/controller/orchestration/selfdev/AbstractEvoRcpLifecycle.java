package eu.kalafatic.evolution.controller.orchestration.selfdev;

public abstract class AbstractEvoRcpLifecycle implements EvoRcpLifecycle {
    protected final SourceProvider sourceProvider;
    protected final EvoRcpBuilder builder;
    protected final Deployer<BuildArtifact> deployer;
    protected final EvoRcpRuntime runtime;

    protected AbstractEvoRcpLifecycle(SourceProvider sourceProvider,
                                     EvoRcpBuilder builder,
                                     Deployer<BuildArtifact> deployer,
                                     EvoRcpRuntime runtime) {
        this.sourceProvider = sourceProvider;
        this.builder = builder;
        this.deployer = deployer;
        this.runtime = runtime;
    }
}
