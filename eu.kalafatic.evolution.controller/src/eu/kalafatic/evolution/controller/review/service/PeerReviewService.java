package eu.kalafatic.evolution.controller.review.service;

import java.io.File;
import java.util.UUID;
import eu.kalafatic.evolution.controller.review.model.ReviewSession;
import eu.kalafatic.evolution.controller.review.model.ReviewDecision;
import eu.kalafatic.evolution.controller.review.model.ChangeSet;
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
        ReviewSession session = new ReviewSession();
        session.setId(UUID.randomUUID().toString());
        session.setDecision(ReviewDecision.OPEN);

        ChangeSet changeSet = new ChangeSet();
        changeSet.setCommitId(commitId);
        // Mock data for prototype
        session.setChangeSet(changeSet);
        this.activeSession = session;
        return session;
    }

    public ReviewSession getActiveSession() {
        return activeSession;
    }

    public String getDiff(File workingDir, String commitId) throws Exception {
        return vcsProvider.getDiff(workingDir, commitId);
    }

    public void startReview(ReviewSession session) {
        session.setDecision(ReviewDecision.IN_REVIEW);
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
