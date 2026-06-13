package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

public interface SessionExperiment extends EObject {
	String getId();
	void setId(String value);

	String getSessionId();
	void setSessionId(String value);

	String getModelId();
	void setModelId(String value);

	String getDatasetId();
	void setDatasetId(String value);

	String getMetrics();
	void setMetrics(String value);

	String getLogs();
	void setLogs(String value);
}
