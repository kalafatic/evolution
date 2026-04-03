/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>File Config</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.FileConfig#getLocalPath <em>Local Path</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getFileConfig()
 * @model
 * @generated
 */
public interface FileConfig extends EObject {
	/**
	 * Returns the value of the '<em><b>Local Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Local Path</em>' attribute.
	 * @see #setLocalPath(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getFileConfig_LocalPath()
	 * @model
	 * @generated
	 */
	String getLocalPath();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.FileConfig#getLocalPath <em>Local Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Local Path</em>' attribute.
	 * @see #getLocalPath()
	 * @generated
	 */
	void setLocalPath(String value);

	/**
	 * Returns the value of the '<em><b>Test Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Test Status</em>' attribute.
	 * @see #setTestStatus(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getFileConfig_TestStatus()
	 * @model
	 * @generated
	 */
	String getTestStatus();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.FileConfig#getTestStatus <em>Test Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Test Status</em>' attribute.
	 * @see #getTestStatus()
	 * @generated
	 */
	void setTestStatus(String value);

} // FileConfig
