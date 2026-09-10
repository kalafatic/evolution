package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public abstract class AbstractProjectBuilder implements ProjectBuilder {
    protected final MavenBuildExecutor mavenExecutor;

    protected AbstractProjectBuilder(String taskId) {
        this.mavenExecutor = new MavenBuildExecutor(taskId);
    }

    protected File getLogFile(SelfDevContext context, String logName) {
        if (context == null || context.getLogDirectory() == null) {
            return new File(logName);
        }
        return new File(context.getLogDirectory(), logName);
    }
}
