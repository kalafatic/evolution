/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.ServerSession;
import eu.kalafatic.evolution.model.orchestration.SessionType;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Server Session</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ServerSessionImpl#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ServerSessionImpl#getType <em>Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ServerSessionImpl#getStartTime <em>Start Time</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ServerSessionImpl#getLastActivity <em>Last Activity</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ServerSessionImpl#getClientIp <em>Client Ip</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ServerSessionImpl extends MinimalEObjectImpl.Container implements ServerSession {
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
	 * The default value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected static final SessionType TYPE_EDEFAULT = SessionType.HTTPD;

	/**
	 * The cached value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected SessionType type = TYPE_EDEFAULT;

	/**
	 * The default value of the '{@link #getStartTime() <em>Start Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStartTime()
	 * @generated
	 * @ordered
	 */
	protected static final long START_TIME_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getStartTime() <em>Start Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStartTime()
	 * @generated
	 * @ordered
	 */
	protected long startTime = START_TIME_EDEFAULT;

	/**
	 * The default value of the '{@link #getLastActivity() <em>Last Activity</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLastActivity()
	 * @generated
	 * @ordered
	 */
	protected static final long LAST_ACTIVITY_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getLastActivity() <em>Last Activity</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLastActivity()
	 * @generated
	 * @ordered
	 */
	protected long lastActivity = LAST_ACTIVITY_EDEFAULT;

	/**
	 * The default value of the '{@link #getClientIp() <em>Client Ip</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getClientIp()
	 * @generated
	 * @ordered
	 */
	protected static final String CLIENT_IP_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getClientIp() <em>Client Ip</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getClientIp()
	 * @generated
	 * @ordered
	 */
	protected String clientIp = CLIENT_IP_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServerSessionImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.SERVER_SESSION;
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SERVER_SESSION__ID, oldId, id));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public SessionType getType() {
		return type;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setType(SessionType newType) {
		SessionType oldType = type;
		type = newType == null ? TYPE_EDEFAULT : newType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SERVER_SESSION__TYPE, oldType, type));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public long getStartTime() {
		return startTime;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setStartTime(long newStartTime) {
		long oldStartTime = startTime;
		startTime = newStartTime;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SERVER_SESSION__START_TIME, oldStartTime, startTime));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public long getLastActivity() {
		return lastActivity;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setLastActivity(long newLastActivity) {
		long oldLastActivity = lastActivity;
		lastActivity = newLastActivity;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SERVER_SESSION__LAST_ACTIVITY, oldLastActivity, lastActivity));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getClientIp() {
		return clientIp;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setClientIp(String newClientIp) {
		String oldClientIp = clientIp;
		clientIp = newClientIp;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SERVER_SESSION__CLIENT_IP, oldClientIp, clientIp));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.SERVER_SESSION__ID:
				return getId();
			case OrchestrationPackage.SERVER_SESSION__TYPE:
				return getType();
			case OrchestrationPackage.SERVER_SESSION__START_TIME:
				return getStartTime();
			case OrchestrationPackage.SERVER_SESSION__LAST_ACTIVITY:
				return getLastActivity();
			case OrchestrationPackage.SERVER_SESSION__CLIENT_IP:
				return getClientIp();
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
			case OrchestrationPackage.SERVER_SESSION__ID:
				setId((String)newValue);
				return;
			case OrchestrationPackage.SERVER_SESSION__TYPE:
				setType((SessionType)newValue);
				return;
			case OrchestrationPackage.SERVER_SESSION__START_TIME:
				setStartTime((Long)newValue);
				return;
			case OrchestrationPackage.SERVER_SESSION__LAST_ACTIVITY:
				setLastActivity((Long)newValue);
				return;
			case OrchestrationPackage.SERVER_SESSION__CLIENT_IP:
				setClientIp((String)newValue);
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
			case OrchestrationPackage.SERVER_SESSION__ID:
				setId(ID_EDEFAULT);
				return;
			case OrchestrationPackage.SERVER_SESSION__TYPE:
				setType(TYPE_EDEFAULT);
				return;
			case OrchestrationPackage.SERVER_SESSION__START_TIME:
				setStartTime(START_TIME_EDEFAULT);
				return;
			case OrchestrationPackage.SERVER_SESSION__LAST_ACTIVITY:
				setLastActivity(LAST_ACTIVITY_EDEFAULT);
				return;
			case OrchestrationPackage.SERVER_SESSION__CLIENT_IP:
				setClientIp(CLIENT_IP_EDEFAULT);
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
			case OrchestrationPackage.SERVER_SESSION__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case OrchestrationPackage.SERVER_SESSION__TYPE:
				return type != TYPE_EDEFAULT;
			case OrchestrationPackage.SERVER_SESSION__START_TIME:
				return startTime != START_TIME_EDEFAULT;
			case OrchestrationPackage.SERVER_SESSION__LAST_ACTIVITY:
				return lastActivity != LAST_ACTIVITY_EDEFAULT;
			case OrchestrationPackage.SERVER_SESSION__CLIENT_IP:
				return CLIENT_IP_EDEFAULT == null ? clientIp != null : !CLIENT_IP_EDEFAULT.equals(clientIp);
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
		result.append(", type: ");
		result.append(type);
		result.append(", startTime: ");
		result.append(startTime);
		result.append(", lastActivity: ");
		result.append(lastActivity);
		result.append(", clientIp: ");
		result.append(clientIp);
		result.append(')');
		return result.toString();
	}

} //ServerSessionImpl
