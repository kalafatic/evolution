/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.SessionSnapshot;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Session Snapshot</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SessionSnapshotImpl#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SessionSnapshotImpl#getSessionId <em>Session Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SessionSnapshotImpl#getGenomeSnapshotId <em>Genome Snapshot Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SessionSnapshotImpl#getFullSerializedState <em>Full Serialized State</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SessionSnapshotImpl#getTimestamp <em>Timestamp</em>}</li>
 * </ul>
 *
 * @generated
 */
public class SessionSnapshotImpl extends MinimalEObjectImpl.Container implements SessionSnapshot {
	/**
	 * The default value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected static final String ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected String id = ID_EDEFAULT;

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
	 * The default value of the '{@link #getGenomeSnapshotId() <em>Genome Snapshot Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGenomeSnapshotId()
	 * @generated
	 * @ordered
	 */
	protected static final String GENOME_SNAPSHOT_ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getGenomeSnapshotId() <em>Genome Snapshot Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGenomeSnapshotId()
	 * @generated
	 * @ordered
	 */
	protected String genomeSnapshotId = GENOME_SNAPSHOT_ID_EDEFAULT;

	/**
	 * The default value of the '{@link #getFullSerializedState() <em>Full Serialized State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFullSerializedState()
	 * @generated
	 * @ordered
	 */
	protected static final String FULL_SERIALIZED_STATE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getFullSerializedState() <em>Full Serialized State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFullSerializedState()
	 * @generated
	 * @ordered
	 */
	protected String fullSerializedState = FULL_SERIALIZED_STATE_EDEFAULT;

	/**
	 * The default value of the '{@link #getTimestamp() <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTimestamp()
	 * @generated
	 * @ordered
	 */
	protected static final long TIMESTAMP_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getTimestamp() <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTimestamp()
	 * @generated
	 * @ordered
	 */
	protected long timestamp = TIMESTAMP_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected SessionSnapshotImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.SESSION_SNAPSHOT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setId(String newId) {
		String oldId = id;
		id = newId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_SNAPSHOT__ID, oldId, id));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_SNAPSHOT__SESSION_ID, oldSessionId, sessionId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getGenomeSnapshotId() {
		return genomeSnapshotId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setGenomeSnapshotId(String newGenomeSnapshotId) {
		String oldGenomeSnapshotId = genomeSnapshotId;
		genomeSnapshotId = newGenomeSnapshotId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_SNAPSHOT__GENOME_SNAPSHOT_ID, oldGenomeSnapshotId, genomeSnapshotId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getFullSerializedState() {
		return fullSerializedState;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setFullSerializedState(String newFullSerializedState) {
		String oldFullSerializedState = fullSerializedState;
		fullSerializedState = newFullSerializedState;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_SNAPSHOT__FULL_SERIALIZED_STATE, oldFullSerializedState, fullSerializedState));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTimestamp(long newTimestamp) {
		long oldTimestamp = timestamp;
		timestamp = newTimestamp;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SESSION_SNAPSHOT__TIMESTAMP, oldTimestamp, timestamp));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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

} //SessionSnapshotImpl
