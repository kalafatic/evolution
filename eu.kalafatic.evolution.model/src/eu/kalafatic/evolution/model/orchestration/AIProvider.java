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

} // AIProvider
