/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Network Entry</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getAddress <em>Address</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getHost <em>Host</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getPort <em>Port</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getLogin <em>Login</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getNote <em>Note</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getType <em>Type</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetworkEntry()
 * @model
 * @generated
 */
public interface NetworkEntry extends EObject {
	/**
	 * Returns the value of the '<em><b>Address</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Address</em>' attribute.
	 * @see #setAddress(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetworkEntry_Address()
	 * @model
	 * @generated
	 */
	String getAddress();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getAddress <em>Address</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Address</em>' attribute.
	 * @see #getAddress()
	 * @generated
	 */
	void setAddress(String value);

	/**
	 * Returns the value of the '<em><b>Host</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Host</em>' attribute.
	 * @see #setHost(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetworkEntry_Host()
	 * @model
	 * @generated
	 */
	String getHost();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getHost <em>Host</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Host</em>' attribute.
	 * @see #getHost()
	 * @generated
	 */
	void setHost(String value);

	/**
	 * Returns the value of the '<em><b>Port</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Port</em>' attribute.
	 * @see #setPort(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetworkEntry_Port()
	 * @model
	 * @generated
	 */
	int getPort();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getPort <em>Port</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Port</em>' attribute.
	 * @see #getPort()
	 * @generated
	 */
	void setPort(int value);

	/**
	 * Returns the value of the '<em><b>Login</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Login</em>' attribute.
	 * @see #setLogin(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetworkEntry_Login()
	 * @model
	 * @generated
	 */
	String getLogin();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getLogin <em>Login</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Login</em>' attribute.
	 * @see #getLogin()
	 * @generated
	 */
	void setLogin(String value);

	/**
	 * Returns the value of the '<em><b>Note</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Note</em>' attribute.
	 * @see #setNote(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetworkEntry_Note()
	 * @model
	 * @generated
	 */
	String getNote();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getNote <em>Note</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Note</em>' attribute.
	 * @see #getNote()
	 * @generated
	 */
	void setNote(String value);

	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see #setType(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetworkEntry_Type()
	 * @model
	 * @generated
	 */
	String getType();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetworkEntry#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see #getType()
	 * @generated
	 */
	void setType(String value);

} // NetworkEntry
