package eu.kalafatic.evolution.controller.orchestration.selfdev;

public interface ProcessLifecycle {
    TaskResult start(SelfDevContext context);
    boolean isAlive();
    TaskResult waitUntilReady(SelfDevContext context, long timeoutSeconds);
    TaskResult stop(SelfDevContext context);
    long getPid();
}
