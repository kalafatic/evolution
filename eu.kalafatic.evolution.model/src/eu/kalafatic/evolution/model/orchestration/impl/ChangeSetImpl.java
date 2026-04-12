/**
 */
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

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Change Set</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChangeSetImpl#getCommitId <em>Commit Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChangeSetImpl#getFiles <em>Files</em>}</li>
 * </ul>
 *
 * @generated
 */
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

	/**
	 * The cached value of the '{@link #getCommitId() <em>Commit Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCommitId()
	 * @generated
	 * @ordered
	 */
	protected String commitId = COMMIT_ID_EDEFAULT;

	/**
	 * The cached value of the '{@link #getFiles() <em>Files</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFiles()
	 * @generated
	 * @ordered
	 */
	protected EList<FileChange> files;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ChangeSetImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.CHANGE_SET;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getCommitId() {
		return commitId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCommitId(String newCommitId) {
		String oldCommitId = commitId;
		commitId = newCommitId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHANGE_SET__COMMIT_ID, oldCommitId, commitId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<FileChange> getFiles() {
		if (files == null) {
			files = new EObjectContainmentEList<FileChange>(FileChange.class, this, OrchestrationPackage.CHANGE_SET__FILES);
		}
		return files;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case OrchestrationPackage.CHANGE_SET__FILES:
				return ((InternalEList<?>)getFiles()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.CHANGE_SET__COMMIT_ID:
				return getCommitId();
			case OrchestrationPackage.CHANGE_SET__FILES:
				return getFiles();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.CHANGE_SET__COMMIT_ID:
				setCommitId((String)newValue);
				return;
			case OrchestrationPackage.CHANGE_SET__FILES:
				getFiles().clear();
				getFiles().addAll((Collection<? extends FileChange>)newValue);
				return;
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

} //ChangeSetImpl
