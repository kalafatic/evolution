/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>AI Provider</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getName <em>Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getUrl <em>Url</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getApiKey <em>Api Key</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getFormat <em>Format</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#isLocal <em>Local</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getDefaultModel <em>Default Model</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#isApiKeyEncrypted <em>Api Key Encrypted</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#isUseEnvVar <em>Use Env Var</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getEnvVarName <em>Env Var Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getState <em>State</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getStateDescription <em>State Description</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getRating <em>Rating</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider()
 * @model
 * @generated
 */
public interface AIProvider extends EObject {
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Url</em>' attribute.
	 * @see #setUrl(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_Url()
	 * @model
	 * @generated
	 */
	String getUrl();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getUrl <em>Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Url</em>' attribute.
	 * @see #getUrl()
	 * @generated
	 */
	void setUrl(String value);

	/**
	 * Returns the value of the '<em><b>Api Key</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Api Key</em>' attribute.
	 * @see #setApiKey(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_ApiKey()
	 * @model
	 * @generated
	 */
	String getApiKey();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getApiKey <em>Api Key</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Api Key</em>' attribute.
	 * @see #getApiKey()
	 * @generated
	 */
	void setApiKey(String value);

	/**
	 * Returns the value of the '<em><b>Format</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Format</em>' attribute.
	 * @see #setFormat(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_Format()
	 * @model
	 * @generated
	 */
	String getFormat();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getFormat <em>Format</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Format</em>' attribute.
	 * @see #getFormat()
	 * @generated
	 */
	void setFormat(String value);

	/**
	 * Returns the value of the '<em><b>Local</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Local</em>' attribute.
	 * @see #setLocal(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_Local()
	 * @model
	 * @generated
	 */
	boolean isLocal();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#isLocal <em>Local</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Local</em>' attribute.
	 * @see #isLocal()
	 * @generated
	 */
	void setLocal(boolean value);

	/**
	 * Returns the value of the '<em><b>Default Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Default Model</em>' attribute.
	 * @see #setDefaultModel(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_DefaultModel()
	 * @model
	 * @generated
	 */
	String getDefaultModel();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getDefaultModel <em>Default Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Default Model</em>' attribute.
	 * @see #getDefaultModel()
	 * @generated
	 */
	void setDefaultModel(String value);

	/**
	 * Returns the value of the '<em><b>Api Key Encrypted</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Api Key Encrypted</em>' attribute.
	 * @see #setApiKeyEncrypted(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_ApiKeyEncrypted()
	 * @model
	 * @generated
	 */
	boolean isApiKeyEncrypted();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#isApiKeyEncrypted <em>Api Key Encrypted</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Api Key Encrypted</em>' attribute.
	 * @see #isApiKeyEncrypted()
	 * @generated
	 */
	void setApiKeyEncrypted(boolean value);

	/**
	 * Returns the value of the '<em><b>Use Env Var</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Use Env Var</em>' attribute.
	 * @see #setUseEnvVar(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_UseEnvVar()
	 * @model
	 * @generated
	 */
	boolean isUseEnvVar();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#isUseEnvVar <em>Use Env Var</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Use Env Var</em>' attribute.
	 * @see #isUseEnvVar()
	 * @generated
	 */
	void setUseEnvVar(boolean value);

	/**
	 * Returns the value of the '<em><b>Env Var Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Env Var Name</em>' attribute.
	 * @see #setEnvVarName(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_EnvVarName()
	 * @model
	 * @generated
	 */
	String getEnvVarName();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getEnvVarName <em>Env Var Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Env Var Name</em>' attribute.
	 * @see #getEnvVarName()
	 * @generated
	 */
	void setEnvVarName(String value);

	/**
	 * Returns the value of the '<em><b>State</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>State</em>' attribute.
	 * @see #setState(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_State()
	 * @model
	 * @generated
	 */
	String getState();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getState <em>State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>State</em>' attribute.
	 * @see #getState()
	 * @generated
	 */
	void setState(String value);

	/**
	 * Returns the value of the '<em><b>State Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>State Description</em>' attribute.
	 * @see #setStateDescription(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_StateDescription()
	 * @model
	 * @generated
	 */
	String getStateDescription();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getStateDescription <em>State Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>State Description</em>' attribute.
	 * @see #getStateDescription()
	 * @generated
	 */
	void setStateDescription(String value);

	/**
	 * Returns the value of the '<em><b>Rating</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Rating</em>' attribute.
	 * @see #setRating(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAIProvider_Rating()
	 * @model
	 * @generated
	 */
	int getRating();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.AIProvider#getRating <em>Rating</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Rating</em>' attribute.
	 * @see #getRating()
	 * @generated
	 */
	void setRating(int value);

} // AIProvider
