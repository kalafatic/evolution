package eu.kalafatic.evolution.controller.review.model;

import java.util.ArrayList;
import java.util.List;

public class ChangeSet {
    private String commitId;
    private List<FileChange> files = new ArrayList<>();

    public String getCommitId() { return commitId; }
    public void setCommitId(String commitId) { this.commitId = commitId; }

    public List<FileChange> getFiles() { return files; }
    public void setFiles(List<FileChange> files) { this.files = files; }
}
