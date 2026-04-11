package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.ChangeSet;
import eu.kalafatic.evolution.model.orchestration.FileChange;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
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

public class ChangeSetImpl extends MinimalEObjectImpl.Container implements ChangeSet {
    protected String commitId;
    protected EList<FileChange> files;

    protected ChangeSetImpl() { super(); }

    @Override protected EClass eStaticClass() { return OrchestrationPackage.Literals.CHANGE_SET; }

    @Override public String getCommitId() { return commitId; }
    @Override public void setCommitId(String newCommitId) {
        String oldCommitId = commitId; commitId = newCommitId;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHANGE_SET__COMMIT_ID, oldCommitId, commitId));
    }

    @Override public EList<FileChange> getFiles() {
        if (files == null) {
            files = new EObjectContainmentEList<FileChange>(FileChange.class, this, OrchestrationPackage.CHANGE_SET__FILES);
        }
        return files;
    }

    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        if (featureID == OrchestrationPackage.CHANGE_SET__FILES) return ((InternalEList<?>)getFiles()).basicRemove(otherEnd, msgs);
        return super.eInverseRemove(otherEnd, featureID, msgs);
    }

    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case OrchestrationPackage.CHANGE_SET__COMMIT_ID: return getCommitId();
            case OrchestrationPackage.CHANGE_SET__FILES: return getFiles();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case OrchestrationPackage.CHANGE_SET__COMMIT_ID: setCommitId((String)newValue); return;
            case OrchestrationPackage.CHANGE_SET__FILES: getFiles().clear(); getFiles().addAll((Collection<? extends FileChange>)newValue); return;
        }
        super.eSet(featureID, newValue);
    }
}
