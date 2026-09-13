package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.util.Set;

public interface SelfDevTask {
    String getId();
    String getName();
    Set<String> getDependencies();
    void addDependency(String dependencyTaskId);
    TaskResult execute(SelfDevContext context);
    TaskResult validate(SelfDevContext context);
    TaskStatus getStatus();
    void cancel();
}
