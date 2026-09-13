/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.SessionModelState;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Session Model State</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SessionModelStateImpl#getSessionId <em>Session Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SessionModelStateImpl#getModelGraph <em>Model Graph</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SessionModelStateImpl#getHyperparameters <em>Hyperparameters</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SessionModelStateImpl#getDatasetBindings <em>Dataset Bindings</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SessionModelStateImpl#getRuntimeState <em>Runtime State</em>}</li>
 * </ul>
 *
 * @generated
 */
public class SessionModelStateImpl extends MinimalEObjectImpl.Container implements SessionModelState {
	/**
	 * The default value of the '{@link #getSessionId() <em>Session Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSessionId()
	 * @generated
	 * @ordered
	 */
	protected static final String SESSION_ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSessionId() <em>Session Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSessionId()
	 * @generated
	 * @ordered
	 */
	protected String sessionId = SESSION_ID_EDEFAULT;

	/**
	 * The default value of the '{@link #getModelGraph() <em>Model Graph</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getModelGraph()
	 * @generated
	 * @ordered
	 */
	protected static final String MODEL_GRAPH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getModelGraph() <em>Model Graph</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getModelGraph()
	 * @generated
	 * @ordered
	 */
	protected String modelGraph = MODEL_GRAPH_EDEFAULT;

	/**
	 * The default value of the '{@link #getHyperparameters() <em>Hyperparameters</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getHyperparameters()
	 * @generated
	 * @ordered
	 */
	protected static final String HYPERPARAMETERS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getHyperparameters() <em>Hyperparameters</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getHyperparameters()
	 * @generated
	 * @ordered
	 */
	protected String hyperparameters = HYPERPARAMETERS_EDEFAULT;

	/**
	 * The default value of the '{@link #getDatasetBindings() <em>Dataset Bindings</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDatasetBindings()
	 * @generated
	 * @ordered
	 */
	protected static final String DATASET_BINDINGS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDatasetBindings() <em>Dataset Bindings</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDatasetBindings()
	 * @generated
	 * @ordered
	 */
	protected String datasetBindings = DATASET_BINDINGS_EDEFAULT;

	/**
	 * The default value of the '{@link #getRuntimeState() <em>Runtime State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRuntimeState()
	 * @generated
	 * @ordered
	 */
	protected static final String RUNTIME_STATE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRuntimeState() <em>Runtime State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRuntimeState()
	 * @generated
	 * @ordered
	 */
	protected String runtimeState = RUNTIME_STATE_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected SessionModelStateImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.SESSION_MODEL_STATE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSessionId(String newSessionId) {
		String oldSessionId = sessionId;
		sessionId = newSessionId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_MODEL_STATE__SESSION_ID, oldSessionId, sessionId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getModelGraph() {
		return modelGraph;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setModelGraph(String newModelGraph) {
		String oldModelGraph = modelGraph;
		modelGraph = newModelGraph;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_MODEL_STATE__MODEL_GRAPH, oldModelGraph, modelGraph));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getHyperparameters() {
		return hyperparameters;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setHyperparameters(String newHyperparameters) {
		String oldHyperparameters = hyperparameters;
		hyperparameters = newHyperparameters;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_MODEL_STATE__HYPERPARAMETERS, oldHyperparameters, hyperparameters));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getDatasetBindings() {
		return datasetBindings;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDatasetBindings(String newDatasetBindings) {
		String oldDatasetBindings = datasetBindings;
		datasetBindings = newDatasetBindings;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_MODEL_STATE__DATASET_BINDINGS, oldDatasetBindings, datasetBindings));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getRuntimeState() {
		return runtimeState;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRuntimeState(String newRuntimeState) {
		String oldRuntimeState = runtimeState;
		runtimeState = newRuntimeState;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_MODEL_STATE__RUNTIME_STATE, oldRuntimeState, runtimeState));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_MODEL_STATE__SESSION_ID:
				return getSessionId();
			case OrchestrationPackage.SESSION_MODEL_STATE__MODEL_GRAPH:
				return getModelGraph();
			case OrchestrationPackage.SESSION_MODEL_STATE__HYPERPARAMETERS:
				return getHyperparameters();
			case OrchestrationPackage.SESSION_MODEL_STATE__DATASET_BINDINGS:
				return getDatasetBindings();
			case OrchestrationPackage.SESSION_MODEL_STATE__RUNTIME_STATE:
				return getRuntimeState();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_MODEL_STATE__SESSION_ID:
				setSessionId((String)newValue);
				return;
			case OrchestrationPackage.SESSION_MODEL_STATE__MODEL_GRAPH:
				setModelGraph((String)newValue);
				return;
			case OrchestrationPackage.SESSION_MODEL_STATE__HYPERPARAMETERS:
				setHyperparameters((String)newValue);
				return;
			case OrchestrationPackage.SESSION_MODEL_STATE__DATASET_BINDINGS:
				setDatasetBindings((String)newValue);
				return;
			case OrchestrationPackage.SESSION_MODEL_STATE__RUNTIME_STATE:
				setRuntimeState((String)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_MODEL_STATE__SESSION_ID:
				setSessionId(SESSION_ID_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_MODEL_STATE__MODEL_GRAPH:
				setModelGraph(MODEL_GRAPH_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_MODEL_STATE__HYPERPARAMETERS:
				setHyperparameters(HYPERPARAMETERS_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_MODEL_STATE__DATASET_BINDINGS:
				setDatasetBindings(DATASET_BINDINGS_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_MODEL_STATE__RUNTIME_STATE:
				setRuntimeState(RUNTIME_STATE_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_MODEL_STATE__SESSION_ID:
				return SESSION_ID_EDEFAULT == null ? sessionId != null : !SESSION_ID_EDEFAULT.equals(sessionId);
			case OrchestrationPackage.SESSION_MODEL_STATE__MODEL_GRAPH:
				return MODEL_GRAPH_EDEFAULT == null ? modelGraph != null : !MODEL_GRAPH_EDEFAULT.equals(modelGraph);
			case OrchestrationPackage.SESSION_MODEL_STATE__HYPERPARAMETERS:
				return HYPERPARAMETERS_EDEFAULT == null ? hyperparameters != null : !HYPERPARAMETERS_EDEFAULT.equals(hyperparameters);
			case OrchestrationPackage.SESSION_MODEL_STATE__DATASET_BINDINGS:
				return DATASET_BINDINGS_EDEFAULT == null ? datasetBindings != null : !DATASET_BINDINGS_EDEFAULT.equals(datasetBindings);
			case OrchestrationPackage.SESSION_MODEL_STATE__RUNTIME_STATE:
				return RUNTIME_STATE_EDEFAULT == null ? runtimeState != null : !RUNTIME_STATE_EDEFAULT.equals(runtimeState);
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (sessionId: ");
		result.append(sessionId);
		result.append(", modelGraph: ");
		result.append(modelGraph);
		result.append(", hyperparameters: ");
		result.append(hyperparameters);
		result.append(", datasetBindings: ");
		result.append(datasetBindings);
		result.append(", runtimeState: ");
		result.append(runtimeState);
		result.append(')');
		return result.toString();
	}

} //SessionModelStateImpl
