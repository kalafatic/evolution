package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.SessionExperiment;
import eu.kalafatic.evolution.model.orchestration.SessionExperiment;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

public class SessionExperimentImpl extends MinimalEObjectImpl.Container implements SessionExperiment {
	protected static final String ID_EDEFAULT = null;
	protected String id = ID_EDEFAULT;
	protected static final String SESSION_ID_EDEFAULT = null;
	protected String sessionId = SESSION_ID_EDEFAULT;
	protected static final String MODEL_ID_EDEFAULT = null;
	protected String modelId = MODEL_ID_EDEFAULT;
	protected static final String DATASET_ID_EDEFAULT = null;
	protected String datasetId = DATASET_ID_EDEFAULT;
	protected static final String METRICS_EDEFAULT = null;
	protected String metrics = METRICS_EDEFAULT;
	protected static final String LOGS_EDEFAULT = null;
	protected String logs = LOGS_EDEFAULT;

	protected SessionExperimentImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.SESSION_EXPERIMENT;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String newId) {
		String oldId = id;
		id = newId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_EXPERIMENT__ID, oldId, id));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_EXPERIMENT__SESSION_ID, oldSessionId, sessionId));
	}

	@Override
	public String getModelId() {
		return modelId;
	}

	@Override
	public void setModelId(String newModelId) {
		String oldModelId = modelId;
		modelId = newModelId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_EXPERIMENT__MODEL_ID, oldModelId, modelId));
	}

	@Override
	public String getDatasetId() {
		return datasetId;
	}

	@Override
	public void setDatasetId(String newDatasetId) {
		String oldDatasetId = datasetId;
		datasetId = newDatasetId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_EXPERIMENT__DATASET_ID, oldDatasetId, datasetId));
	}

	@Override
	public String getMetrics() {
		return metrics;
	}

	@Override
	public void setMetrics(String newMetrics) {
		String oldMetrics = metrics;
		metrics = newMetrics;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_EXPERIMENT__METRICS, oldMetrics, metrics));
	}

	@Override
	public String getLogs() {
		return logs;
	}

	@Override
	public void setLogs(String newLogs) {
		String oldLogs = logs;
		logs = newLogs;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_EXPERIMENT__LOGS, oldLogs, logs));
	}

	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_EXPERIMENT__ID:
				return getId();
			case OrchestrationPackage.SESSION_EXPERIMENT__SESSION_ID:
				return getSessionId();
			case OrchestrationPackage.SESSION_EXPERIMENT__MODEL_ID:
				return getModelId();
			case OrchestrationPackage.SESSION_EXPERIMENT__DATASET_ID:
				return getDatasetId();
			case OrchestrationPackage.SESSION_EXPERIMENT__METRICS:
				return getMetrics();
			case OrchestrationPackage.SESSION_EXPERIMENT__LOGS:
				return getLogs();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_EXPERIMENT__ID:
				setId((String)newValue);
				return;
			case OrchestrationPackage.SESSION_EXPERIMENT__SESSION_ID:
				setSessionId((String)newValue);
				return;
			case OrchestrationPackage.SESSION_EXPERIMENT__MODEL_ID:
				setModelId((String)newValue);
				return;
			case OrchestrationPackage.SESSION_EXPERIMENT__DATASET_ID:
				setDatasetId((String)newValue);
				return;
			case OrchestrationPackage.SESSION_EXPERIMENT__METRICS:
				setMetrics((String)newValue);
				return;
			case OrchestrationPackage.SESSION_EXPERIMENT__LOGS:
				setLogs((String)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_EXPERIMENT__ID:
				setId(ID_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_EXPERIMENT__SESSION_ID:
				setSessionId(SESSION_ID_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_EXPERIMENT__MODEL_ID:
				setModelId(MODEL_ID_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_EXPERIMENT__DATASET_ID:
				setDatasetId(DATASET_ID_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_EXPERIMENT__METRICS:
				setMetrics(METRICS_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_EXPERIMENT__LOGS:
				setLogs(LOGS_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_EXPERIMENT__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case OrchestrationPackage.SESSION_EXPERIMENT__SESSION_ID:
				return SESSION_ID_EDEFAULT == null ? sessionId != null : !SESSION_ID_EDEFAULT.equals(sessionId);
			case OrchestrationPackage.SESSION_EXPERIMENT__MODEL_ID:
				return MODEL_ID_EDEFAULT == null ? modelId != null : !MODEL_ID_EDEFAULT.equals(modelId);
			case OrchestrationPackage.SESSION_EXPERIMENT__DATASET_ID:
				return DATASET_ID_EDEFAULT == null ? datasetId != null : !DATASET_ID_EDEFAULT.equals(datasetId);
			case OrchestrationPackage.SESSION_EXPERIMENT__METRICS:
				return METRICS_EDEFAULT == null ? metrics != null : !METRICS_EDEFAULT.equals(metrics);
			case OrchestrationPackage.SESSION_EXPERIMENT__LOGS:
				return LOGS_EDEFAULT == null ? logs != null : !LOGS_EDEFAULT.equals(logs);
		}
		return super.eIsSet(featureID);
	}

	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();
		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (id: ");
		result.append(id);
		result.append(", sessionId: ");
		result.append(sessionId);
		result.append(", modelId: ");
		result.append(modelId);
		result.append(", datasetId: ");
		result.append(datasetId);
		result.append(", metrics: ");
		result.append(metrics);
		result.append(", logs: ");
		result.append(logs);
		result.append(')');
		return result.toString();
	}
}
