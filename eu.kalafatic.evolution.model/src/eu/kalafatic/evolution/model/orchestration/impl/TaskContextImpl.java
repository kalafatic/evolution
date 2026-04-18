package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.PlatformMode;
import eu.kalafatic.evolution.model.orchestration.TaskContext;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Task Context</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskContextImpl#getCurrentTaskName <em>Current Task Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskContextImpl#getThreadId <em>Thread Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskContextImpl#isPaused <em>Paused</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskContextImpl#isAutoApprove <em>Auto Approve</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskContextImpl#getPlatformMode <em>Platform Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.TaskContextImpl#getInstructionFiles <em>Instruction Files</em>}</li>
 * </ul>
 *
 * @generated
 */
public class TaskContextImpl extends MinimalEObjectImpl.Container implements TaskContext {
	/**
	 * The default value of the '{@link #getCurrentTaskName() <em>Current Task Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCurrentTaskName()
	 * @generated
	 * @ordered
	 */
	protected static final String CURRENT_TASK_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getCurrentTaskName() <em>Current Task Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCurrentTaskName()
	 * @generated
	 * @ordered
	 */
	protected String currentTaskName = CURRENT_TASK_NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getThreadId() <em>Thread Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getThreadId()
	 * @generated
	 * @ordered
	 */
	protected static final String THREAD_ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getThreadId() <em>Thread Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getThreadId()
	 * @generated
	 * @ordered
	 */
	protected String threadId = THREAD_ID_EDEFAULT;

	/**
	 * The default value of the '{@link #isPaused() <em>Paused</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isPaused()
	 * @generated
	 * @ordered
	 */
	protected static final boolean PAUSED_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isPaused() <em>Paused</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isPaused()
	 * @generated
	 * @ordered
	 */
	protected boolean paused = PAUSED_EDEFAULT;

	/**
	 * The default value of the '{@link #isAutoApprove() <em>Auto Approve</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAutoApprove()
	 * @generated
	 * @ordered
	 */
	protected static final boolean AUTO_APPROVE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isAutoApprove() <em>Auto Approve</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAutoApprove()
	 * @generated
	 * @ordered
	 */
	protected boolean autoApprove = AUTO_APPROVE_EDEFAULT;

	/**
	 * The cached value of the '{@link #getPlatformMode() <em>Platform Mode</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPlatformMode()
	 * @generated
	 * @ordered
	 */
	protected PlatformMode platformMode;

	/**
	 * The cached value of the '{@link #getInstructionFiles() <em>Instruction Files</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInstructionFiles()
	 * @generated
	 * @ordered
	 */
	protected EList<String> instructionFiles;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected TaskContextImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.TASK_CONTEXT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getCurrentTaskName() {
		return currentTaskName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCurrentTaskName(String newCurrentTaskName) {
		String oldCurrentTaskName = currentTaskName;
		currentTaskName = newCurrentTaskName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK_CONTEXT__CURRENT_TASK_NAME, oldCurrentTaskName, currentTaskName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getThreadId() {
		return threadId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setThreadId(String newThreadId) {
		String oldThreadId = threadId;
		threadId = newThreadId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK_CONTEXT__THREAD_ID, oldThreadId, threadId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isPaused() {
		return paused;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPaused(boolean newPaused) {
		boolean oldPaused = paused;
		paused = newPaused;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK_CONTEXT__PAUSED, oldPaused, paused));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isAutoApprove() {
		return autoApprove;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAutoApprove(boolean newAutoApprove) {
		boolean oldAutoApprove = autoApprove;
		autoApprove = newAutoApprove;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK_CONTEXT__AUTO_APPROVE, oldAutoApprove, autoApprove));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public PlatformMode getPlatformMode() {
		return platformMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetPlatformMode(PlatformMode newPlatformMode, NotificationChain msgs) {
		PlatformMode oldPlatformMode = platformMode;
		platformMode = newPlatformMode;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK_CONTEXT__PLATFORM_MODE, oldPlatformMode, newPlatformMode);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPlatformMode(PlatformMode newPlatformMode) {
		if (newPlatformMode != platformMode) {
			NotificationChain msgs = null;
			if (platformMode != null)
				msgs = ((InternalEObject)platformMode).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.TASK_CONTEXT__PLATFORM_MODE, null, msgs);
			if (newPlatformMode != null)
				msgs = ((InternalEObject)newPlatformMode).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.TASK_CONTEXT__PLATFORM_MODE, null, msgs);
			msgs = basicSetPlatformMode(newPlatformMode, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TASK_CONTEXT__PLATFORM_MODE, newPlatformMode, newPlatformMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getInstructionFiles() {
		if (instructionFiles == null) {
			instructionFiles = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.TASK_CONTEXT__INSTRUCTION_FILES);
		}
		return instructionFiles;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case OrchestrationPackage.TASK_CONTEXT__PLATFORM_MODE:
				return basicSetPlatformMode(null, msgs);
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
			case OrchestrationPackage.TASK_CONTEXT__CURRENT_TASK_NAME:
				return getCurrentTaskName();
			case OrchestrationPackage.TASK_CONTEXT__THREAD_ID:
				return getThreadId();
			case OrchestrationPackage.TASK_CONTEXT__PAUSED:
				return isPaused();
			case OrchestrationPackage.TASK_CONTEXT__AUTO_APPROVE:
				return isAutoApprove();
			case OrchestrationPackage.TASK_CONTEXT__PLATFORM_MODE:
				return getPlatformMode();
			case OrchestrationPackage.TASK_CONTEXT__INSTRUCTION_FILES:
				return getInstructionFiles();
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
			case OrchestrationPackage.TASK_CONTEXT__CURRENT_TASK_NAME:
				setCurrentTaskName((String)newValue);
				return;
			case OrchestrationPackage.TASK_CONTEXT__THREAD_ID:
				setThreadId((String)newValue);
				return;
			case OrchestrationPackage.TASK_CONTEXT__PAUSED:
				setPaused((Boolean)newValue);
				return;
			case OrchestrationPackage.TASK_CONTEXT__AUTO_APPROVE:
				setAutoApprove((Boolean)newValue);
				return;
			case OrchestrationPackage.TASK_CONTEXT__PLATFORM_MODE:
				setPlatformMode((PlatformMode)newValue);
				return;
			case OrchestrationPackage.TASK_CONTEXT__INSTRUCTION_FILES:
				getInstructionFiles().clear();
				getInstructionFiles().addAll((Collection<? extends String>)newValue);
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
			case OrchestrationPackage.TASK_CONTEXT__CURRENT_TASK_NAME:
				setCurrentTaskName(CURRENT_TASK_NAME_EDEFAULT);
				return;
			case OrchestrationPackage.TASK_CONTEXT__THREAD_ID:
				setThreadId(THREAD_ID_EDEFAULT);
				return;
			case OrchestrationPackage.TASK_CONTEXT__PAUSED:
				setPaused(PAUSED_EDEFAULT);
				return;
			case OrchestrationPackage.TASK_CONTEXT__AUTO_APPROVE:
				setAutoApprove(AUTO_APPROVE_EDEFAULT);
				return;
			case OrchestrationPackage.TASK_CONTEXT__PLATFORM_MODE:
				setPlatformMode((PlatformMode)null);
				return;
			case OrchestrationPackage.TASK_CONTEXT__INSTRUCTION_FILES:
				getInstructionFiles().clear();
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
			case OrchestrationPackage.TASK_CONTEXT__CURRENT_TASK_NAME:
				return CURRENT_TASK_NAME_EDEFAULT == null ? currentTaskName != null : !CURRENT_TASK_NAME_EDEFAULT.equals(currentTaskName);
			case OrchestrationPackage.TASK_CONTEXT__THREAD_ID:
				return THREAD_ID_EDEFAULT == null ? threadId != null : !THREAD_ID_EDEFAULT.equals(threadId);
			case OrchestrationPackage.TASK_CONTEXT__PAUSED:
				return paused != PAUSED_EDEFAULT;
			case OrchestrationPackage.TASK_CONTEXT__AUTO_APPROVE:
				return autoApprove != AUTO_APPROVE_EDEFAULT;
			case OrchestrationPackage.TASK_CONTEXT__PLATFORM_MODE:
				return platformMode != null;
			case OrchestrationPackage.TASK_CONTEXT__INSTRUCTION_FILES:
				return instructionFiles != null && !instructionFiles.isEmpty();
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
		result.append(" (currentTaskName: ");
		result.append(currentTaskName);
		result.append(", threadId: ");
		result.append(threadId);
		result.append(", paused: ");
		result.append(paused);
		result.append(", autoApprove: ");
		result.append(autoApprove);
		result.append(')');
		return result.toString();
	}

} //TaskContextImpl
