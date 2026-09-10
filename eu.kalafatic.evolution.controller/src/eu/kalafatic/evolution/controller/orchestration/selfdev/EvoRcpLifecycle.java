package eu.kalafatic.evolution.controller.orchestration.selfdev;

public interface EvoRcpLifecycle {
    TaskResult fetchSource(SelfDevContext context);
    TaskResult build(SelfDevContext context);
    TaskResult deploy(SelfDevContext context);
    TaskResult start(SelfDevContext context);
    TaskResult verifyReady(SelfDevContext context, long timeoutSeconds);
    TaskResult stop(SelfDevContext context);
    TaskResult runFullLifecycle(SelfDevContext context);
}
