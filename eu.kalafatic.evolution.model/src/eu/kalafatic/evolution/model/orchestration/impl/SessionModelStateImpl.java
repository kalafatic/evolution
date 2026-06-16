package eu.kalafatic.evolution.model.orchestration.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.SessionModelState;

public class SessionModelStateImpl extends MinimalEObjectImpl.Container implements SessionModelState {
	protected static final String SESSION_ID_EDEFAULT = null;
	protected String sessionId = SESSION_ID_EDEFAULT;
	protected static final String MODEL_GRAPH_EDEFAULT = null;
	protected String modelGraph = MODEL_GRAPH_EDEFAULT;
	protected static final String HYPERPARAMETERS_EDEFAULT = null;
	protected String hyperparameters = HYPERPARAMETERS_EDEFAULT;
	protected static final String DATASET_BINDINGS_EDEFAULT = null;
	protected String datasetBindings = DATASET_BINDINGS_EDEFAULT;
	protected static final String RUNTIME_STATE_EDEFAULT = null;
	protected String runtimeState = RUNTIME_STATE_EDEFAULT;

	protected SessionModelStateImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.SESSION_MODEL_STATE;
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	@Override
	public void setSessionId(String newSessionId) {
		String oldSessionId = sessionId;
		sessionId = newSessionId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_MODEL_STATE__SESSION_ID, oldSessionId, sessionId));
	}

	@Override
	public String getModelGraph() {
		return modelGraph;
	}

	@Override
	public void setModelGraph(String newModelGraph) {
		String oldModelGraph = modelGraph;
		modelGraph = newModelGraph;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_MODEL_STATE__MODEL_GRAPH, oldModelGraph, modelGraph));
	}

	@Override
	public String getHyperparameters() {
		return hyperparameters;
	}

	@Override
	public void setHyperparameters(String newHyperparameters) {
		String oldHyperparameters = hyperparameters;
		hyperparameters = newHyperparameters;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_MODEL_STATE__HYPERPARAMETERS, oldHyperparameters, hyperparameters));
	}

	@Override
	public String getDatasetBindings() {
		return datasetBindings;
	}

	@Override
	public void setDatasetBindings(String newDatasetBindings) {
		String oldDatasetBindings = datasetBindings;
		datasetBindings = newDatasetBindings;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_MODEL_STATE__DATASET_BINDINGS, oldDatasetBindings, datasetBindings));
	}

	@Override
	public String getRuntimeState() {
		return runtimeState;
	}

	@Override
	public void setRuntimeState(String newRuntimeState) {
		String oldRuntimeState = runtimeState;
		runtimeState = newRuntimeState;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_MODEL_STATE__RUNTIME_STATE, oldRuntimeState, runtimeState));
	}

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
}
