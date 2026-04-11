package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.DiffHunk;
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

public class FileChangeImpl extends MinimalEObjectImpl.Container implements FileChange {
    protected String filePath;
    protected String status;
    protected EList<DiffHunk> hunks;

    protected FileChangeImpl() { super(); }

    @Override protected EClass eStaticClass() { return OrchestrationPackage.Literals.FILE_CHANGE; }

    @Override public String getFilePath() { return filePath; }
    @Override public void setFilePath(String newFilePath) {
        String oldFilePath = filePath; filePath = newFilePath;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FILE_CHANGE__FILE_PATH, oldFilePath, filePath));
    }

    @Override public String getStatus() { return status; }
    @Override public void setStatus(String newStatus) {
        String oldStatus = status; status = newStatus;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FILE_CHANGE__STATUS, oldStatus, status));
    }

    @Override public EList<DiffHunk> getHunks() {
        if (hunks == null) {
            hunks = new EObjectContainmentEList<DiffHunk>(DiffHunk.class, this, OrchestrationPackage.FILE_CHANGE__HUNKS);
        }
        return hunks;
    }

    @Override
    public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
        if (featureID == OrchestrationPackage.FILE_CHANGE__HUNKS) return ((InternalEList<?>)getHunks()).basicRemove(otherEnd, msgs);
        return super.eInverseRemove(otherEnd, featureID, msgs);
    }

    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case OrchestrationPackage.FILE_CHANGE__FILE_PATH: return getFilePath();
            case OrchestrationPackage.FILE_CHANGE__STATUS: return getStatus();
            case OrchestrationPackage.FILE_CHANGE__HUNKS: return getHunks();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case OrchestrationPackage.FILE_CHANGE__FILE_PATH: setFilePath((String)newValue); return;
            case OrchestrationPackage.FILE_CHANGE__STATUS: setStatus((String)newValue); return;
            case OrchestrationPackage.FILE_CHANGE__HUNKS: getHunks().clear(); getHunks().addAll((Collection<? extends DiffHunk>)newValue); return;
        }
        super.eSet(featureID, newValue);
    }
}
