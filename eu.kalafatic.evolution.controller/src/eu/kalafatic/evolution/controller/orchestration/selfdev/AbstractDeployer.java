package eu.kalafatic.evolution.controller.orchestration.selfdev;

import java.io.File;

public abstract class AbstractDeployer<T> implements Deployer<T> {
    protected final String deployerId;

    protected AbstractDeployer(String deployerId) {
        this.deployerId = deployerId;
    }

    protected void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.setWritable(true);
        file.delete();
    }
}
