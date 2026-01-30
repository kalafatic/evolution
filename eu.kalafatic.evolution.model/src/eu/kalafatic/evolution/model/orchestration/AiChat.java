/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Ai Chat</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AiChat#getUrl <em>Url</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AiChat#getToken <em>Token</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AiChat#getPrompt <em>Prompt</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AiChat#getProxyUrl <em>Proxy Url</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAiChat()
 * @model
 * @generated
 */
public interface AiChat extends EObject {
	/**
	 * Returns the value of the '<em><b>Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Url</em>' attribute.
	 * @see #setUrl(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAiChat_Url()
	 * @model
	 * @generated
	 */
	String getUrl();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AiChat#getUrl <em>Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Url</em>' attribute.
	 * @see #getUrl()
	 * @generated
	 */
	void setUrl(String value);

	/**
	 * Returns the value of the '<em><b>Token</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Token</em>' attribute.
	 * @see #setToken(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAiChat_Token()
	 * @model
	 * @generated
	 */
	String getToken();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AiChat#getToken <em>Token</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Token</em>' attribute.
	 * @see #getToken()
	 * @generated
	 */
	void setToken(String value);

	/**
	 * Returns the value of the '<em><b>Prompt</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Prompt</em>' attribute.
	 * @see #setPrompt(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAiChat_Prompt()
	 * @model
	 * @generated
	 */
	String getPrompt();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AiChat#getPrompt <em>Prompt</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Prompt</em>' attribute.
	 * @see #getPrompt()
	 * @generated
	 */
	void setPrompt(String value);

	/**
	 * Returns the value of the '<em><b>Proxy Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Proxy Url</em>' attribute.
	 * @see #setProxyUrl(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAiChat_ProxyUrl()
	 * @model
	 * @generated NOT
	 */
	String getProxyUrl();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AiChat#getProxyUrl <em>Proxy Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Proxy Url</em>' attribute.
	 * @see #getProxyUrl()
	 * @generated NOT
	 */
	void setProxyUrl(String value);

} // AiChat
