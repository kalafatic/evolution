/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Change Set</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChangeSet#getCommitId <em>Commit Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChangeSet#getFiles <em>Files</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChangeSet()
 * @model
 * @generated
 */
public interface ChangeSet extends EObject {
	/**
	 * Returns the value of the '<em><b>Commit Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Commit Id</em>' attribute.
	 * @see #setCommitId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChangeSet_CommitId()
	 * @model
	 * @generated
	 */
	String getCommitId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChangeSet#getCommitId <em>Commit Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Commit Id</em>' attribute.
	 * @see #getCommitId()
	 * @generated
	 */
	void setCommitId(String value);

	/**
	 * Returns the value of the '<em><b>Files</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.FileChange}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Files</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChangeSet_Files()
	 * @model containment="true"
	 * @generated
	 */
	EList<FileChange> getFiles();

} // ChangeSet
