package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

public interface ForgeSession extends EObject {
	String getSessionId();
	void setSessionId(String value);

	String getName();
	void setName(String value);

	long getCreatedAt();
	void setCreatedAt(long value);

	long getLastModified();
	void setLastModified(long value);

	ForgeStatus getStatus();
	void setStatus(ForgeStatus value);

	String getActiveModelId();
	void setActiveModelId(String value);

	String getSelectedModelType();
	void setSelectedModelType(String value);

	SessionModelState getModelState();
	void setModelState(SessionModelState value);

	EList<SessionExperiment> getExperiments();

	EList<SessionSnapshot> getSnapshots();
}
