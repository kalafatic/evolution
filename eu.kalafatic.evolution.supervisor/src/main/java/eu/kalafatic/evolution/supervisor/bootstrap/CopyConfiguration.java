package eu.kalafatic.evolution.supervisor.bootstrap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CopyConfiguration {
    private final File sourcePath;
    private final File targetPath;
    private final List<String> exclusions = new ArrayList<>();
    private boolean overwrite = true;

    public CopyConfiguration(File sourcePath, File targetPath) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
    }

    public File getSourcePath() {
        return sourcePath;
    }

    public File getTargetPath() {
        return targetPath;
    }

    public List<String> getExclusions() {
        return exclusions;
    }

    public void addExclusion(String exclusion) {
        this.exclusions.add(exclusion);
    }

    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }
}
