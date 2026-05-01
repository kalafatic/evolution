package eu.kalafatic.evolution.supervisor;

public class Bootstrap {
    private String sourcePath;
    private String targetPath;
    private String action;

    public Bootstrap() {}

    public String getSourcePath() { return sourcePath; }
    public void setSourcePath(String sourcePath) { this.sourcePath = sourcePath; }

    public String getTargetPath() { return targetPath; }
    public void setTargetPath(String targetPath) { this.targetPath = targetPath; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}
