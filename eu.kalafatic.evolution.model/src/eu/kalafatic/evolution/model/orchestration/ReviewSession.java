package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

public interface ReviewSession extends EObject {
    String getId();
    void setId(String value);
    ChangeSet getChangeSet();
    void setChangeSet(ChangeSet value);
    EList<Comment> getComments();
    ReviewDecision getDecision();
    void setDecision(ReviewDecision value);
}
