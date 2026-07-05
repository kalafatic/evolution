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
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#isMcpEnabled <em>Mcp Enabled</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#getMcpPort <em>Mcp Port</em>}</li>
	 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#isAuthenticate <em>Authenticate</em>}</li>
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

	/**
	 * Returns the value of the '<em><b>Mcp Enabled</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mcp Enabled</em>' attribute.
	 * @see #setMcpEnabled(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSettings_McpEnabled()
	 * @model default="true"
	 * @generated
	 */
	boolean isMcpEnabled();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#isMcpEnabled <em>Mcp Enabled</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Mcp Enabled</em>' attribute.
	 * @see #isMcpEnabled()
	 * @generated
	 */
	void setMcpEnabled(boolean value);

	/**
	 * Returns the value of the '<em><b>Mcp Port</b></em>' attribute.
	 * The default value is <code>"58080"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mcp Port</em>' attribute.
	 * @see #setMcpPort(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSettings_McpPort()
	 * @model default="58080"
	 * @generated
	 */
	int getMcpPort();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#getMcpPort <em>Mcp Port</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Mcp Port</em>' attribute.
	 * @see #getMcpPort()
	 * @generated
	 */
	void setMcpPort(int value);

	/**
	 * Returns the value of the '<em><b>Authenticate</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Authenticate</em>' attribute.
	 * @see #setAuthenticate(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSettings_Authenticate()
	 * @model default="false"
	 * @generated
	 */
	boolean isAuthenticate();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ServerSettings#isAuthenticate <em>Authenticate</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Authenticate</em>' attribute.
	 * @see #isAuthenticate()
	 * @generated
	 */
	void setAuthenticate(boolean value);

} // ServerSettings
