/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Session Experiment</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getSessionId <em>Session Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getModelId <em>Model Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getDatasetId <em>Dataset Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getMetrics <em>Metrics</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getLogs <em>Logs</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionExperiment()
 * @model
 * @generated
 */
public interface SessionExperiment extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionExperiment_Id()
	 * @model
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Session Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Session Id</em>' attribute.
	 * @see #setSessionId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionExperiment_SessionId()
	 * @model
	 * @generated
	 */
	String getSessionId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getSessionId <em>Session Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Session Id</em>' attribute.
	 * @see #getSessionId()
	 * @generated
	 */
	void setSessionId(String value);

	/**
	 * Returns the value of the '<em><b>Model Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Model Id</em>' attribute.
	 * @see #setModelId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionExperiment_ModelId()
	 * @model
	 * @generated
	 */
	String getModelId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getModelId <em>Model Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Model Id</em>' attribute.
	 * @see #getModelId()
	 * @generated
	 */
	void setModelId(String value);

	/**
	 * Returns the value of the '<em><b>Dataset Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Dataset Id</em>' attribute.
	 * @see #setDatasetId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionExperiment_DatasetId()
	 * @model
	 * @generated
	 */
	String getDatasetId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getDatasetId <em>Dataset Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Dataset Id</em>' attribute.
	 * @see #getDatasetId()
	 * @generated
	 */
	void setDatasetId(String value);

	/**
	 * Returns the value of the '<em><b>Metrics</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Metrics</em>' attribute.
	 * @see #setMetrics(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionExperiment_Metrics()
	 * @model
	 * @generated
	 */
	String getMetrics();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getMetrics <em>Metrics</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Metrics</em>' attribute.
	 * @see #getMetrics()
	 * @generated
	 */
	void setMetrics(String value);

	/**
	 * Returns the value of the '<em><b>Logs</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Logs</em>' attribute.
	 * @see #setLogs(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionExperiment_Logs()
	 * @model
	 * @generated
	 */
	String getLogs();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionExperiment#getLogs <em>Logs</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Logs</em>' attribute.
	 * @see #getLogs()
	 * @generated
	 */
	void setLogs(String value);

} // SessionExperiment
