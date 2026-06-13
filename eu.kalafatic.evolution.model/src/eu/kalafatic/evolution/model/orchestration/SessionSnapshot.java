package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

public interface SessionSnapshot extends EObject {
	String getId();
	void setId(String value);

	String getSessionId();
	void setSessionId(String value);

	String getGenomeSnapshotId();
	void setGenomeSnapshotId(String value);

	String getFullSerializedState();
	void setFullSerializedState(String value);

	long getTimestamp();
	void setTimestamp(long value);
}
