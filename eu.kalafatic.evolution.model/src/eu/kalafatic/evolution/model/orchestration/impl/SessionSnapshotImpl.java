package eu.kalafatic.evolution.model.orchestration.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.SessionSnapshot;

public class SessionSnapshotImpl extends MinimalEObjectImpl.Container implements SessionSnapshot {
	protected static final String ID_EDEFAULT = null;
	protected String id = ID_EDEFAULT;
	protected static final String SESSION_ID_EDEFAULT = null;
	protected String sessionId = SESSION_ID_EDEFAULT;
	protected static final String GENOME_SNAPSHOT_ID_EDEFAULT = null;
	protected String genomeSnapshotId = GENOME_SNAPSHOT_ID_EDEFAULT;
	protected static final String FULL_SERIALIZED_STATE_EDEFAULT = null;
	protected String fullSerializedState = FULL_SERIALIZED_STATE_EDEFAULT;
	protected static final long TIMESTAMP_EDEFAULT = 0L;
	protected long timestamp = TIMESTAMP_EDEFAULT;

	protected SessionSnapshotImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.SESSION_SNAPSHOT;
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_SNAPSHOT__ID, oldId, id));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_SNAPSHOT__SESSION_ID, oldSessionId, sessionId));
	}

	@Override
	public String getGenomeSnapshotId() {
		return genomeSnapshotId;
	}

	@Override
	public void setGenomeSnapshotId(String newGenomeSnapshotId) {
		String oldGenomeSnapshotId = genomeSnapshotId;
		genomeSnapshotId = newGenomeSnapshotId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_SNAPSHOT__GENOME_SNAPSHOT_ID, oldGenomeSnapshotId, genomeSnapshotId));
	}

	@Override
	public String getFullSerializedState() {
		return fullSerializedState;
	}

	@Override
	public void setFullSerializedState(String newFullSerializedState) {
		String oldFullSerializedState = fullSerializedState;
		fullSerializedState = newFullSerializedState;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_SNAPSHOT__FULL_SERIALIZED_STATE, oldFullSerializedState, fullSerializedState));
	}

	@Override
	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public void setTimestamp(long newTimestamp) {
		long oldTimestamp = timestamp;
		timestamp = newTimestamp;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_SNAPSHOT__TIMESTAMP, oldTimestamp, timestamp));
	}

	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_SNAPSHOT__ID:
				return getId();
			case OrchestrationPackage.SESSION_SNAPSHOT__SESSION_ID:
				return getSessionId();
			case OrchestrationPackage.SESSION_SNAPSHOT__GENOME_SNAPSHOT_ID:
				return getGenomeSnapshotId();
			case OrchestrationPackage.SESSION_SNAPSHOT__FULL_SERIALIZED_STATE:
				return getFullSerializedState();
			case OrchestrationPackage.SESSION_SNAPSHOT__TIMESTAMP:
				return getTimestamp();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_SNAPSHOT__ID:
				setId((String)newValue);
				return;
			case OrchestrationPackage.SESSION_SNAPSHOT__SESSION_ID:
				setSessionId((String)newValue);
				return;
			case OrchestrationPackage.SESSION_SNAPSHOT__GENOME_SNAPSHOT_ID:
				setGenomeSnapshotId((String)newValue);
				return;
			case OrchestrationPackage.SESSION_SNAPSHOT__FULL_SERIALIZED_STATE:
				setFullSerializedState((String)newValue);
				return;
			case OrchestrationPackage.SESSION_SNAPSHOT__TIMESTAMP:
				setTimestamp((Long)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_SNAPSHOT__ID:
				setId(ID_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_SNAPSHOT__SESSION_ID:
				setSessionId(SESSION_ID_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_SNAPSHOT__GENOME_SNAPSHOT_ID:
				setGenomeSnapshotId(GENOME_SNAPSHOT_ID_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_SNAPSHOT__FULL_SERIALIZED_STATE:
				setFullSerializedState(FULL_SERIALIZED_STATE_EDEFAULT);
				return;
			case OrchestrationPackage.SESSION_SNAPSHOT__TIMESTAMP:
				setTimestamp(TIMESTAMP_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.SESSION_SNAPSHOT__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case OrchestrationPackage.SESSION_SNAPSHOT__SESSION_ID:
				return SESSION_ID_EDEFAULT == null ? sessionId != null : !SESSION_ID_EDEFAULT.equals(sessionId);
			case OrchestrationPackage.SESSION_SNAPSHOT__GENOME_SNAPSHOT_ID:
				return GENOME_SNAPSHOT_ID_EDEFAULT == null ? genomeSnapshotId != null : !GENOME_SNAPSHOT_ID_EDEFAULT.equals(genomeSnapshotId);
			case OrchestrationPackage.SESSION_SNAPSHOT__FULL_SERIALIZED_STATE:
				return FULL_SERIALIZED_STATE_EDEFAULT == null ? fullSerializedState != null : !FULL_SERIALIZED_STATE_EDEFAULT.equals(fullSerializedState);
			case OrchestrationPackage.SESSION_SNAPSHOT__TIMESTAMP:
				return timestamp != TIMESTAMP_EDEFAULT;
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
		result.append(", genomeSnapshotId: ");
		result.append(genomeSnapshotId);
		result.append(", fullSerializedState: ");
		result.append(fullSerializedState);
		result.append(", timestamp: ");
		result.append(timestamp);
		result.append(')');
		return result.toString();
	}
}
