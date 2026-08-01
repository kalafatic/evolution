package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public abstract class AbstractBootstrapTask {
    protected final String name;
    protected final File projectRoot;
    protected final File runDir;

    protected AbstractBootstrapTask(String name, File projectRoot, File runDir) {
        this.name = name;
        this.projectRoot = projectRoot;
        this.runDir = runDir;
    }

    public String getName() {
        return name;
    }

    public final String execute() {
        long startTime = System.currentTimeMillis();
        System.out.println("[TASK] >>> Starting " + name + " Task...");
        System.out.println("[TASK] >>> projectRoot: " + (projectRoot != null ? projectRoot.getAbsolutePath() : "null"));
        System.out.println("[TASK] >>> runDir: " + (runDir != null ? runDir.getAbsolutePath() : "null"));
        try {
            String result = run();
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("[TASK] <<< Finished " + name + " Task. Result: " + result + " (took " + duration + "ms)");
            return result;
        } catch (Throwable t) {
            long duration = System.currentTimeMillis() - startTime;
            System.err.println("[TASK] <<< Failed " + name + " Task with error: " + t.getMessage() + " (took " + duration + "ms)");
            t.printStackTrace();
            return "ERROR: " + t.getMessage();
        }
    }

    protected abstract String run() throws Exception;
}
