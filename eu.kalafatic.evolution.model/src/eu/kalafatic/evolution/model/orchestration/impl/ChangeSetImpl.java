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
    /**
	 * The default value of the '{@link #getCommitId() <em>Commit Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCommitId()
	 * @generated
	 * @ordered
	 */
	protected static final String COMMIT_ID_EDEFAULT = null;
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

				/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.CHANGE_SET__COMMIT_ID:
				setCommitId(COMMIT_ID_EDEFAULT);
				return;
			case OrchestrationPackage.CHANGE_SET__FILES:
				getFiles().clear();
				return;
		}
		super.eUnset(featureID);
	}

				/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.CHANGE_SET__COMMIT_ID:
				return COMMIT_ID_EDEFAULT == null ? commitId != null : !COMMIT_ID_EDEFAULT.equals(commitId);
			case OrchestrationPackage.CHANGE_SET__FILES:
				return files != null && !files.isEmpty();
		}
		return super.eIsSet(featureID);
	}

				/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (commitId: ");
		result.append(commitId);
		result.append(')');
		return result.toString();
	}
}
