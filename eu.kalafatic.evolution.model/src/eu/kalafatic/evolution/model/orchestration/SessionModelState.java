/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Session Model State</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionModelState#getSessionId <em>Session Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionModelState#getModelGraph <em>Model Graph</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionModelState#getHyperparameters <em>Hyperparameters</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionModelState#getDatasetBindings <em>Dataset Bindings</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionModelState#getRuntimeState <em>Runtime State</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionModelState()
 * @model
 * @generated
 */
public interface SessionModelState extends EObject {
	/**
	 * Returns the value of the '<em><b>Session Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Session Id</em>' attribute.
	 * @see #setSessionId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionModelState_SessionId()
	 * @model
	 * @generated
	 */
	String getSessionId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionModelState#getSessionId <em>Session Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Session Id</em>' attribute.
	 * @see #getSessionId()
	 * @generated
	 */
	void setSessionId(String value);

	/**
	 * Returns the value of the '<em><b>Model Graph</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Model Graph</em>' attribute.
	 * @see #setModelGraph(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionModelState_ModelGraph()
	 * @model
	 * @generated
	 */
	String getModelGraph();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionModelState#getModelGraph <em>Model Graph</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Model Graph</em>' attribute.
	 * @see #getModelGraph()
	 * @generated
	 */
	void setModelGraph(String value);

	/**
	 * Returns the value of the '<em><b>Hyperparameters</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Hyperparameters</em>' attribute.
	 * @see #setHyperparameters(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionModelState_Hyperparameters()
	 * @model
	 * @generated
	 */
	String getHyperparameters();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionModelState#getHyperparameters <em>Hyperparameters</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Hyperparameters</em>' attribute.
	 * @see #getHyperparameters()
	 * @generated
	 */
	void setHyperparameters(String value);

	/**
	 * Returns the value of the '<em><b>Dataset Bindings</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Dataset Bindings</em>' attribute.
	 * @see #setDatasetBindings(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionModelState_DatasetBindings()
	 * @model
	 * @generated
	 */
	String getDatasetBindings();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionModelState#getDatasetBindings <em>Dataset Bindings</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Dataset Bindings</em>' attribute.
	 * @see #getDatasetBindings()
	 * @generated
	 */
	void setDatasetBindings(String value);

	/**
	 * Returns the value of the '<em><b>Runtime State</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Runtime State</em>' attribute.
	 * @see #setRuntimeState(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionModelState_RuntimeState()
	 * @model
	 * @generated
	 */
	String getRuntimeState();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionModelState#getRuntimeState <em>Runtime State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Runtime State</em>' attribute.
	 * @see #getRuntimeState()
	 * @generated
	 */
	void setRuntimeState(String value);

} // SessionModelState
