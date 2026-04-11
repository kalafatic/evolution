package eu.kalafatic.evolution.controller.review.model;

import java.util.ArrayList;
import java.util.List;

public class ReviewSession {
    private String id;
    private ChangeSet changeSet;
    private List<Comment> comments = new ArrayList<>();
    private ReviewDecision decision = ReviewDecision.OPEN;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public ChangeSet getChangeSet() { return changeSet; }
    public void setChangeSet(ChangeSet changeSet) { this.changeSet = changeSet; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }

    public ReviewDecision getDecision() { return decision; }
    public void setDecision(ReviewDecision decision) { this.decision = decision; }
}
