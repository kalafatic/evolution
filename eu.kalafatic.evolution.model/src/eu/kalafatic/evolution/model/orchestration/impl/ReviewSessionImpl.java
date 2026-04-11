package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.ChangeSet;
import eu.kalafatic.evolution.model.orchestration.Comment;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.ReviewDecision;
import eu.kalafatic.evolution.model.orchestration.ReviewSession;
import java.util.Collection;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

public class ReviewSessionImpl extends MinimalEObjectImpl.Container implements ReviewSession {
    protected String id;
    protected ChangeSet changeSet;
    protected EList<Comment> comments;
    protected ReviewDecision decision = ReviewDecision.OPEN;

    protected ReviewSessionImpl() { super(); }

    @Override protected EClass eStaticClass() { return OrchestrationPackage.Literals.REVIEW_SESSION; }

    @Override public String getId() { return id; }
    @Override public void setId(String newId) {
        String oldId = id; id = newId;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.REVIEW_SESSION__ID, oldId, id));
    }

    @Override public ChangeSet getChangeSet() { return changeSet; }
    public NotificationChain basicSetChangeSet(ChangeSet newChangeSet, NotificationChain msgs) {
        ChangeSet oldChangeSet = changeSet; changeSet = newChangeSet;
        if (eNotificationRequired()) {
            ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.REVIEW_SESSION__CHANGE_SET, oldChangeSet, newChangeSet);
            if (msgs == null) msgs = notification; else msgs.add(notification);
        }
        return msgs;
    }
    @Override public void setChangeSet(ChangeSet newChangeSet) {
        if (newChangeSet != changeSet) {
            NotificationChain msgs = null;
            if (changeSet != null) msgs = ((InternalEObject)changeSet).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.REVIEW_SESSION__CHANGE_SET, null, msgs);
            if (newChangeSet != null) msgs = ((InternalEObject)newChangeSet).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.REVIEW_SESSION__CHANGE_SET, null, msgs);
            msgs = basicSetChangeSet(newChangeSet, msgs);
            if (msgs != null) msgs.dispatch();
        } else if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.REVIEW_SESSION__CHANGE_SET, newChangeSet, newChangeSet));
    }

    @Override public EList<Comment> getComments() {
        if (comments == null) {
            comments = new EObjectContainmentEList<Comment>(Comment.class, this, OrchestrationPackage.REVIEW_SESSION__COMMENTS);
        }
        return comments;
    }

    @Override public ReviewDecision getDecision() { return decision; }
    @Override public void setDecision(ReviewDecision newDecision) {
        ReviewDecision oldDecision = decision; decision = newDecision == null ? ReviewDecision.OPEN : newDecision;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.REVIEW_SESSION__DECISION, oldDecision, decision));
    }

    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        switch (featureID) {
            case OrchestrationPackage.REVIEW_SESSION__CHANGE_SET: return basicSetChangeSet(null, msgs);
            case OrchestrationPackage.REVIEW_SESSION__COMMENTS: return ((InternalEList<?>)getComments()).basicRemove(otherEnd, msgs);
        }
        return super.eInverseRemove(otherEnd, featureID, msgs);
    }

    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case OrchestrationPackage.REVIEW_SESSION__ID: return getId();
            case OrchestrationPackage.REVIEW_SESSION__CHANGE_SET: return getChangeSet();
            case OrchestrationPackage.REVIEW_SESSION__COMMENTS: return getComments();
            case OrchestrationPackage.REVIEW_SESSION__DECISION: return getDecision();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case OrchestrationPackage.REVIEW_SESSION__ID: setId((String)newValue); return;
            case OrchestrationPackage.REVIEW_SESSION__CHANGE_SET: setChangeSet((ChangeSet)newValue); return;
            case OrchestrationPackage.REVIEW_SESSION__COMMENTS: getComments().clear(); getComments().addAll((Collection<? extends Comment>)newValue); return;
            case OrchestrationPackage.REVIEW_SESSION__DECISION: setDecision((ReviewDecision)newValue); return;
        }
        super.eSet(featureID, newValue);
    }
}
