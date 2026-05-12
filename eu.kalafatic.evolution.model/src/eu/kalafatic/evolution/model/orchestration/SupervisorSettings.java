/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Supervisor Settings</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SupervisorSettings#getExecutablePath <em>Executable Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SupervisorSettings#isDeployed <em>Deployed</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSupervisorSettings()
 * @model
 * @generated
 */
public interface SupervisorSettings extends EObject {
	/**
	 * Returns the value of the '<em><b>Executable Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Executable Path</em>' attribute.
	 * @see #setExecutablePath(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSupervisorSettings_ExecutablePath()
	 * @model
	 * @generated
	 */
	String getExecutablePath();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SupervisorSettings#getExecutablePath <em>Executable Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Executable Path</em>' attribute.
	 * @see #getExecutablePath()
	 * @generated
	 */
	void setExecutablePath(String value);

	/**
	 * Returns the value of the '<em><b>Deployed</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Deployed</em>' attribute.
	 * @see #setDeployed(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSupervisorSettings_Deployed()
	 * @model default="false"
	 * @generated
	 */
	boolean isDeployed();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SupervisorSettings#isDeployed <em>Deployed</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Deployed</em>' attribute.
	 * @see #isDeployed()
	 * @generated
	 */
	void setDeployed(boolean value);

} // SupervisorSettings
