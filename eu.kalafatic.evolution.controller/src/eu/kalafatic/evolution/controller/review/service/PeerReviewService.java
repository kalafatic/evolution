package eu.kalafatic.evolution.controller.review.service;

import java.io.File;
import java.util.UUID;
import eu.kalafatic.evolution.model.orchestration.ReviewSession;
import eu.kalafatic.evolution.model.orchestration.ReviewDecision;
import eu.kalafatic.evolution.model.orchestration.ChangeSet;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.controller.vcs.VersionControlProvider;
import eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider;

public class PeerReviewService {
    private static PeerReviewService instance;
    private VersionControlProvider vcsProvider;
    private ReviewSession activeSession;

    private PeerReviewService() {
        this.vcsProvider = new GitVersionControlProvider();
    }

    public static synchronized PeerReviewService getInstance() {
        if (instance == null) {
            instance = new PeerReviewService();
        }
        return instance;
    }

    public ReviewSession createSession(File workingDir, String commitId) throws Exception {
        ReviewSession session = OrchestrationFactory.eINSTANCE.createReviewSession();
        session.setId(UUID.randomUUID().toString());
        session.setDecision(ReviewDecision.OPEN);

        ChangeSet changeSet = OrchestrationFactory.eINSTANCE.createChangeSet();
        changeSet.setCommitId(commitId);

        String diff = getDiff(workingDir, commitId);
        parseDiff(diff, changeSet);

        session.setChangeSet(changeSet);
        this.activeSession = session;
        return session;
    }

    private void parseDiff(String diff, ChangeSet changeSet) {
        if (diff == null || diff.isEmpty()) return;

        String[] lines = diff.split("\n");
        eu.kalafatic.evolution.model.orchestration.FileChange currentFile = null;
        eu.kalafatic.evolution.model.orchestration.DiffHunk currentHunk = null;

        for (String line : lines) {
            if (line.startsWith("diff --git")) {
                currentFile = OrchestrationFactory.eINSTANCE.createFileChange();
                String[] parts = line.split(" ");
                if (parts.length > 3) {
                    currentFile.setFilePath(parts[3].substring(2)); // remove b/
                }
                currentFile.setStatus("MODIFIED");
                changeSet.getFiles().add(currentFile);
                currentHunk = null;
            } else if (line.startsWith("@@") && currentFile != null) {
                currentHunk = OrchestrationFactory.eINSTANCE.createDiffHunk();
                currentHunk.setHeader(line);
                currentFile.getHunks().add(currentHunk);
            } else if (currentHunk != null) {
                currentHunk.getLines().add(line);
            }
        }
    }

    public ReviewSession getActiveSession() {
        return activeSession;
    }

    public String getDiff(File workingDir, String commitId) throws Exception {
        return vcsProvider.getDiff(workingDir, commitId);
    }

    public String getFileDiff(File workingDir, String commitId, String filePath) throws Exception {
        return vcsProvider.getFileDiff(workingDir, commitId, filePath);
    }

    public java.util.List<String> getChangedFiles(File workingDir) throws Exception {
        return vcsProvider.getChangedFiles(workingDir, "HEAD");
    }

    public void startReview(ReviewSession session) {
        session.setDecision(ReviewDecision.IN_REVIEW);
    }

    public void addComment(ReviewSession session, String filePath, int lineNum, String content) {
        if (session == null) return;
        eu.kalafatic.evolution.model.orchestration.Comment comment = OrchestrationFactory.eINSTANCE.createComment();
        comment.setId(UUID.randomUUID().toString());
        comment.setFilePath(filePath);
        comment.setStartLine(lineNum);
        comment.setEndLine(lineNum);
        comment.setContent(content);
        comment.setTimestamp(java.time.LocalDateTime.now().toString());
        comment.setAuthor(System.getProperty("user.name", "AI Reviewer"));
        session.getComments().add(comment);
    }

    public void approve(ReviewSession session, File workingDir, String message) throws Exception {
        session.setDecision(ReviewDecision.APPROVED);
        if (workingDir != null && workingDir.exists()) {
            vcsProvider.commitChanges(workingDir, "Approved Review " + session.getId() + ": " + message);
            vcsProvider.push(workingDir);
        }
    }

    public void reject(ReviewSession session) {
        session.setDecision(ReviewDecision.REJECTED);
    }

    public void requestChanges(ReviewSession session) {
        session.setDecision(ReviewDecision.CHANGES_REQUESTED);
    }

    public void setVcsProvider(VersionControlProvider vcsProvider) {
        this.vcsProvider = vcsProvider;
    }
}
