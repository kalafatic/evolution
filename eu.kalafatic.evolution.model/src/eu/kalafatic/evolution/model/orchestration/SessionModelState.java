package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

public interface SessionModelState extends EObject {
	String getSessionId();
	void setSessionId(String value);

	String getModelGraph();
	void setModelGraph(String value);

	String getHyperparameters();
	void setHyperparameters(String value);

	String getDatasetBindings();
	void setDatasetBindings(String value);

	String getRuntimeState();
	void setRuntimeState(String value);
}
