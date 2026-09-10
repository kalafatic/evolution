package eu.kalafatic.evolution.controller.orchestration.selfdev;

public interface SelfDevTask {
    String getId();
    String getName();
    TaskResult execute(SelfDevContext context);
    TaskResult validate(SelfDevContext context);
    TaskStatus getStatus();
    void cancel();
}
