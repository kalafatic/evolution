package eu.kalafatic.evolution.controller.orchestration.selfdev;

public interface SupervisorLifecycle {
    TaskResult build(SelfDevContext context);
    TaskResult deploy(SelfDevContext context);
    TaskResult start(SelfDevContext context);
    TaskResult verifyReady(SelfDevContext context, long timeoutSeconds);
    TaskResult sendCommand(String command, String param);
    TaskResult stop(SelfDevContext context);
    TaskResult runFullLifecycle(SelfDevContext context);
}
