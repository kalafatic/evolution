/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>File Change</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.FileChange#getFilePath <em>File Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.FileChange#getStatus <em>Status</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.FileChange#getHunks <em>Hunks</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getFileChange()
 * @model
 * @generated
 */
public interface FileChange extends EObject {
	/**
	 * Returns the value of the '<em><b>File Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>File Path</em>' attribute.
	 * @see #setFilePath(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getFileChange_FilePath()
	 * @model
	 * @generated
	 */
	String getFilePath();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.FileChange#getFilePath <em>File Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>File Path</em>' attribute.
	 * @see #getFilePath()
	 * @generated
	 */
	void setFilePath(String value);

	/**
	 * Returns the value of the '<em><b>Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Status</em>' attribute.
	 * @see #setStatus(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getFileChange_Status()
	 * @model
	 * @generated
	 */
	String getStatus();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.FileChange#getStatus <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Status</em>' attribute.
	 * @see #getStatus()
	 * @generated
	 */
	void setStatus(String value);

	/**
	 * Returns the value of the '<em><b>Hunks</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.DiffHunk}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Hunks</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getFileChange_Hunks()
	 * @model containment="true"
	 * @generated
	 */
	EList<DiffHunk> getHunks();

} // FileChange
