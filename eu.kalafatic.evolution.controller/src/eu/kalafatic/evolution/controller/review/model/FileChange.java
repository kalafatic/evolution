package eu.kalafatic.evolution.controller.review.model;

import java.util.ArrayList;
import java.util.List;

public class FileChange {
    private String filePath;
    private String status; // ADDED, DELETED, MODIFIED
    private List<DiffHunk> hunks = new ArrayList<>();

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<DiffHunk> getHunks() { return hunks; }
    public void setHunks(List<DiffHunk> hunks) { this.hunks = hunks; }
}
