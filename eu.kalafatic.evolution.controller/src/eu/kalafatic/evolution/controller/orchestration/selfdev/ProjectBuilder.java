package eu.kalafatic.evolution.controller.orchestration.selfdev;

public interface ProjectBuilder {
    TaskResult build(SelfDevContext context);
    BuildArtifact getArtifact(SelfDevContext context);
}
