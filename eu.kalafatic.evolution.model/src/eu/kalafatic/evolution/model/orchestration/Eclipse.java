/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Eclipse</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getWorkspace <em>Workspace</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getInstallation <em>Installation</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getTargetPlatform <em>Target Platform</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getTestStatus <em>Test Status</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getEclipse()
 * @model
 * @generated
 */
public interface Eclipse extends EObject {
	/**
	 * Returns the value of the '<em><b>Workspace</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Workspace</em>' attribute.
	 * @see #setWorkspace(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getEclipse_Workspace()
	 * @model
	 * @generated
	 */
	String getWorkspace();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getWorkspace <em>Workspace</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Workspace</em>' attribute.
	 * @see #getWorkspace()
	 * @generated
	 */
	void setWorkspace(String value);

	/**
	 * Returns the value of the '<em><b>Installation</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Installation</em>' attribute.
	 * @see #setInstallation(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getEclipse_Installation()
	 * @model
	 * @generated
	 */
	String getInstallation();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getInstallation <em>Installation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Installation</em>' attribute.
	 * @see #getInstallation()
	 * @generated
	 */
	void setInstallation(String value);

	/**
	 * Returns the value of the '<em><b>Target Platform</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target Platform</em>' attribute.
	 * @see #setTargetPlatform(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getEclipse_TargetPlatform()
	 * @model
	 * @generated
	 */
	String getTargetPlatform();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getTargetPlatform <em>Target Platform</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target Platform</em>' attribute.
	 * @see #getTargetPlatform()
	 * @generated
	 */
	void setTargetPlatform(String value);

	/**
	 * Returns the value of the '<em><b>Test Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Test Status</em>' attribute.
	 * @see #setTestStatus(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getEclipse_TestStatus()
	 * @model
	 * @generated
	 */
	String getTestStatus();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getTestStatus <em>Test Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Test Status</em>' attribute.
	 * @see #getTestStatus()
	 * @generated
	 */
	void setTestStatus(String value);

} // Eclipse
