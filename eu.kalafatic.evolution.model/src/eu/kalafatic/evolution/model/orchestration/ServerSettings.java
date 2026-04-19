/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Server Settings</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#getPort <em>Port</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#isAutoStart <em>Auto Start</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#isGitAutomation <em>Git Automation</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSettings()
 * @model
 * @generated
 */
public interface ServerSettings extends EObject {
	/**
	 * Returns the value of the '<em><b>Port</b></em>' attribute.
	 * The default value is <code>"48080"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Port</em>' attribute.
	 * @see #setPort(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSettings_Port()
	 * @model default="48080"
	 * @generated
	 */
	int getPort();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#getPort <em>Port</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Port</em>' attribute.
	 * @see #getPort()
	 * @generated
	 */
	void setPort(int value);

	/**
	 * Returns the value of the '<em><b>Auto Start</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Auto Start</em>' attribute.
	 * @see #setAutoStart(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSettings_AutoStart()
	 * @model default="true"
	 * @generated
	 */
	boolean isAutoStart();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#isAutoStart <em>Auto Start</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Auto Start</em>' attribute.
	 * @see #isAutoStart()
	 * @generated
	 */
	void setAutoStart(boolean value);

	/**
	 * Returns the value of the '<em><b>Git Automation</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Git Automation</em>' attribute.
	 * @see #setGitAutomation(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSettings_GitAutomation()
	 * @model default="false"
	 * @generated
	 */
	boolean isGitAutomation();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#isGitAutomation <em>Git Automation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Git Automation</em>' attribute.
	 * @see #isGitAutomation()
	 * @generated
	 */
	void setGitAutomation(boolean value);

} // ServerSettings
