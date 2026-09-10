package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public interface Deployer<T> {
    TaskResult deploy(SelfDevContext context, T artifact);
    TaskResult validateDeployment(SelfDevContext context, File deployedLocation);
}
