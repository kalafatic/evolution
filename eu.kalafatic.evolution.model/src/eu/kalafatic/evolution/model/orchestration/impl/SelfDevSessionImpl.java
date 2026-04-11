/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Self Dev Session</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SelfDevSessionImpl#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SelfDevSessionImpl#getStartTime <em>Start Time</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SelfDevSessionImpl#getMaxIterations <em>Max Iterations</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SelfDevSessionImpl#getStatus <em>Status</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SelfDevSessionImpl#getIterations <em>Iterations</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SelfDevSessionImpl#getRationale <em>Rationale</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SelfDevSessionImpl#getInitialRequest <em>Initial Request</em>}</li>
 * </ul>
 *
 * @generated
 */
public class SelfDevSessionImpl extends MinimalEObjectImpl.Container implements SelfDevSession {
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
	 * The default value of the '{@link #getMaxIterations() <em>Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxIterations()
	 * @generated
	 * @ordered
	 */
	protected static final int MAX_ITERATIONS_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getMaxIterations() <em>Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxIterations()
	 * @generated
	 * @ordered
	 */
	protected int maxIterations = MAX_ITERATIONS_EDEFAULT;

	/**
	 * The default value of the '{@link #getStatus() <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStatus()
	 * @generated
	 * @ordered
	 */
	protected static final SelfDevStatus STATUS_EDEFAULT = SelfDevStatus.RUNNING;

	/**
	 * The cached value of the '{@link #getStatus() <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStatus()
	 * @generated
	 * @ordered
	 */
	protected SelfDevStatus status = STATUS_EDEFAULT;

	/**
	 * The cached value of the '{@link #getIterations() <em>Iterations</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIterations()
	 * @generated
	 * @ordered
	 */
	protected EList<Iteration> iterations;

	/**
	 * The default value of the '{@link #getRationale() <em>Rationale</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRationale()
	 * @generated
	 * @ordered
	 */
	protected static final String RATIONALE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRationale() <em>Rationale</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRationale()
	 * @generated
	 * @ordered
	 */
	protected String rationale = RATIONALE_EDEFAULT;

	protected static final String INITIAL_REQUEST_EDEFAULT = null;

	protected String initialRequest = INITIAL_REQUEST_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected SelfDevSessionImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.SELF_DEV_SESSION;
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SELF_DEV_SESSION__ID, oldId, id));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SELF_DEV_SESSION__START_TIME, oldStartTime, startTime));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getMaxIterations() {
		return maxIterations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMaxIterations(int newMaxIterations) {
		int oldMaxIterations = maxIterations;
		maxIterations = newMaxIterations;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SELF_DEV_SESSION__MAX_ITERATIONS, oldMaxIterations, maxIterations));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public SelfDevStatus getStatus() {
		return status;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setStatus(SelfDevStatus newStatus) {
		SelfDevStatus oldStatus = status;
		status = newStatus == null ? STATUS_EDEFAULT : newStatus;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SELF_DEV_SESSION__STATUS, oldStatus, status));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Iteration> getIterations() {
		if (iterations == null) {
			iterations = new EObjectContainmentEList<Iteration>(Iteration.class, this, OrchestrationPackage.SELF_DEV_SESSION__ITERATIONS);
		}
		return iterations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getRationale() {
		return rationale;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRationale(String newRationale) {
		String oldRationale = rationale;
		rationale = newRationale;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SELF_DEV_SESSION__RATIONALE, oldRationale, rationale));
	}

	@Override
	public String getInitialRequest() {
		return initialRequest;
	}

	@Override
	public void setInitialRequest(String newInitialRequest) {
		String oldInitialRequest = initialRequest;
		initialRequest = newInitialRequest;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SELF_DEV_SESSION__INITIAL_REQUEST, oldInitialRequest, initialRequest));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case OrchestrationPackage.SELF_DEV_SESSION__ITERATIONS:
				return ((InternalEList<?>)getIterations()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.SELF_DEV_SESSION__ID:
				return getId();
			case OrchestrationPackage.SELF_DEV_SESSION__START_TIME:
				return getStartTime();
			case OrchestrationPackage.SELF_DEV_SESSION__MAX_ITERATIONS:
				return getMaxIterations();
			case OrchestrationPackage.SELF_DEV_SESSION__STATUS:
				return getStatus();
			case OrchestrationPackage.SELF_DEV_SESSION__ITERATIONS:
				return getIterations();
			case OrchestrationPackage.SELF_DEV_SESSION__RATIONALE:
				return getRationale();
			case OrchestrationPackage.SELF_DEV_SESSION__INITIAL_REQUEST:
				return getInitialRequest();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.SELF_DEV_SESSION__ID:
				setId((String)newValue);
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__START_TIME:
				setStartTime((Long)newValue);
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__MAX_ITERATIONS:
				setMaxIterations((Integer)newValue);
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__STATUS:
				setStatus((SelfDevStatus)newValue);
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__ITERATIONS:
				getIterations().clear();
				getIterations().addAll((Collection<? extends Iteration>)newValue);
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__RATIONALE:
				setRationale((String)newValue);
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__INITIAL_REQUEST:
				setInitialRequest((String)newValue);
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
			case OrchestrationPackage.SELF_DEV_SESSION__ID:
				setId(ID_EDEFAULT);
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__START_TIME:
				setStartTime(START_TIME_EDEFAULT);
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__MAX_ITERATIONS:
				setMaxIterations(MAX_ITERATIONS_EDEFAULT);
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__STATUS:
				setStatus(STATUS_EDEFAULT);
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__ITERATIONS:
				getIterations().clear();
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__RATIONALE:
				setRationale(RATIONALE_EDEFAULT);
				return;
			case OrchestrationPackage.SELF_DEV_SESSION__INITIAL_REQUEST:
				setInitialRequest(INITIAL_REQUEST_EDEFAULT);
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
			case OrchestrationPackage.SELF_DEV_SESSION__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case OrchestrationPackage.SELF_DEV_SESSION__START_TIME:
				return startTime != START_TIME_EDEFAULT;
			case OrchestrationPackage.SELF_DEV_SESSION__MAX_ITERATIONS:
				return maxIterations != MAX_ITERATIONS_EDEFAULT;
			case OrchestrationPackage.SELF_DEV_SESSION__STATUS:
				return status != STATUS_EDEFAULT;
			case OrchestrationPackage.SELF_DEV_SESSION__ITERATIONS:
				return iterations != null && !iterations.isEmpty();
			case OrchestrationPackage.SELF_DEV_SESSION__RATIONALE:
				return RATIONALE_EDEFAULT == null ? rationale != null : !RATIONALE_EDEFAULT.equals(rationale);
			case OrchestrationPackage.SELF_DEV_SESSION__INITIAL_REQUEST:
				return INITIAL_REQUEST_EDEFAULT == null ? initialRequest != null : !INITIAL_REQUEST_EDEFAULT.equals(initialRequest);
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
		result.append(", startTime: ");
		result.append(startTime);
		result.append(", maxIterations: ");
		result.append(maxIterations);
		result.append(", status: ");
		result.append(status);
		result.append(", rationale: ");
		result.append(rationale);
		result.append(", initialRequest: ");
		result.append(initialRequest);
		result.append(')');
		return result.toString();
	}

} //SelfDevSessionImpl
