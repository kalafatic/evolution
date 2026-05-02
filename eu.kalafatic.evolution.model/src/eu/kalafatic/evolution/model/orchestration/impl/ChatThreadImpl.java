/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.ChatMessage;
import eu.kalafatic.evolution.model.orchestration.ChatThread;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

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

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Chat Thread</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatThreadImpl#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatThreadImpl#getMessages <em>Messages</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ChatThreadImpl extends MinimalEObjectImpl.Container implements ChatThread {
	/**
	 * The default value of the '{@link #isIterativeMode() <em>Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIterativeMode()
	 * @generated
	 * @ordered
	 */
	protected static final boolean ITERATIVE_MODE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isIterativeMode() <em>Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIterativeMode()
	 * @generated
	 * @ordered
	 */
	protected boolean iterativeMode = ITERATIVE_MODE_EDEFAULT;

	/**
	 * The default value of the '{@link #isSelfIterativeMode() <em>Self Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSelfIterativeMode()
	 * @generated
	 * @ordered
	 */
	protected static final boolean SELF_ITERATIVE_MODE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isSelfIterativeMode() <em>Self Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSelfIterativeMode()
	 * @generated
	 * @ordered
	 */
	protected boolean selfIterativeMode = SELF_ITERATIVE_MODE_EDEFAULT;

	/**
	 * The default value of the '{@link #isDarwinMode() <em>Darwin Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDarwinMode()
	 * @generated
	 * @ordered
	 */
	protected static final boolean DARWIN_MODE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isDarwinMode() <em>Darwin Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDarwinMode()
	 * @generated
	 * @ordered
	 */
	protected boolean darwinMode = DARWIN_MODE_EDEFAULT;

	/**
	 * The default value of the '{@link #isGitAutomation() <em>Git Automation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isGitAutomation()
	 * @generated
	 * @ordered
	 */
	protected static final boolean GIT_AUTOMATION_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isGitAutomation() <em>Git Automation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isGitAutomation()
	 * @generated
	 * @ordered
	 */
	protected boolean gitAutomation = GIT_AUTOMATION_EDEFAULT;

	/**
	 * The default value of the '{@link #getMaxIterations() <em>Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaxIterations()
	 * @generated
	 * @ordered
	 */
	protected static final int MAX_ITERATIONS_EDEFAULT = -1;

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
	 * The cached value of the '{@link #getMessages() <em>Messages</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMessages()
	 * @generated
	 * @ordered
	 */
	protected EList<ChatMessage> messages;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ChatThreadImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.CHAT_THREAD;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isIterativeMode() {
		return iterativeMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setIterativeMode(boolean newIterativeMode) {
		boolean oldIterativeMode = iterativeMode;
		iterativeMode = newIterativeMode;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_THREAD__ITERATIVE_MODE, oldIterativeMode, iterativeMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isSelfIterativeMode() {
		return selfIterativeMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSelfIterativeMode(boolean newSelfIterativeMode) {
		boolean oldSelfIterativeMode = selfIterativeMode;
		selfIterativeMode = newSelfIterativeMode;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_THREAD__SELF_ITERATIVE_MODE, oldSelfIterativeMode, selfIterativeMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isDarwinMode() {
		return darwinMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDarwinMode(boolean newDarwinMode) {
		boolean oldDarwinMode = darwinMode;
		darwinMode = newDarwinMode;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_THREAD__DARWIN_MODE, oldDarwinMode, darwinMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isGitAutomation() {
		return gitAutomation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setGitAutomation(boolean newGitAutomation) {
		boolean oldGitAutomation = gitAutomation;
		gitAutomation = newGitAutomation;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_THREAD__GIT_AUTOMATION, oldGitAutomation, gitAutomation));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_THREAD__MAX_ITERATIONS, oldMaxIterations, maxIterations));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_THREAD__ID, oldId, id));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<ChatMessage> getMessages() {
		if (messages == null) {
			messages = new EObjectContainmentEList<ChatMessage>(ChatMessage.class, this, OrchestrationPackage.CHAT_THREAD__MESSAGES);
		}
		return messages;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case OrchestrationPackage.CHAT_THREAD__MESSAGES:
				return ((InternalEList<?>)getMessages()).basicRemove(otherEnd, msgs);
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
			case OrchestrationPackage.CHAT_THREAD__ITERATIVE_MODE:
				return isIterativeMode();
			case OrchestrationPackage.CHAT_THREAD__SELF_ITERATIVE_MODE:
				return isSelfIterativeMode();
			case OrchestrationPackage.CHAT_THREAD__DARWIN_MODE:
				return isDarwinMode();
			case OrchestrationPackage.CHAT_THREAD__GIT_AUTOMATION:
				return isGitAutomation();
			case OrchestrationPackage.CHAT_THREAD__MAX_ITERATIONS:
				return getMaxIterations();
			case OrchestrationPackage.CHAT_THREAD__ID:
				return getId();
			case OrchestrationPackage.CHAT_THREAD__MESSAGES:
				return getMessages();
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
			case OrchestrationPackage.CHAT_THREAD__ITERATIVE_MODE:
				setIterativeMode((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_THREAD__SELF_ITERATIVE_MODE:
				setSelfIterativeMode((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_THREAD__DARWIN_MODE:
				setDarwinMode((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_THREAD__GIT_AUTOMATION:
				setGitAutomation((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_THREAD__MAX_ITERATIONS:
				setMaxIterations((Integer)newValue);
				return;
			case OrchestrationPackage.CHAT_THREAD__ID:
				setId((String)newValue);
				return;
			case OrchestrationPackage.CHAT_THREAD__MESSAGES:
				getMessages().clear();
				getMessages().addAll((Collection<? extends ChatMessage>)newValue);
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
			case OrchestrationPackage.CHAT_THREAD__ITERATIVE_MODE:
				setIterativeMode(ITERATIVE_MODE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_THREAD__SELF_ITERATIVE_MODE:
				setSelfIterativeMode(SELF_ITERATIVE_MODE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_THREAD__DARWIN_MODE:
				setDarwinMode(DARWIN_MODE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_THREAD__GIT_AUTOMATION:
				setGitAutomation(GIT_AUTOMATION_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_THREAD__MAX_ITERATIONS:
				setMaxIterations(MAX_ITERATIONS_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_THREAD__ID:
				setId(ID_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_THREAD__MESSAGES:
				getMessages().clear();
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
			case OrchestrationPackage.CHAT_THREAD__ITERATIVE_MODE:
				return iterativeMode != ITERATIVE_MODE_EDEFAULT;
			case OrchestrationPackage.CHAT_THREAD__SELF_ITERATIVE_MODE:
				return selfIterativeMode != SELF_ITERATIVE_MODE_EDEFAULT;
			case OrchestrationPackage.CHAT_THREAD__DARWIN_MODE:
				return darwinMode != DARWIN_MODE_EDEFAULT;
			case OrchestrationPackage.CHAT_THREAD__GIT_AUTOMATION:
				return gitAutomation != GIT_AUTOMATION_EDEFAULT;
			case OrchestrationPackage.CHAT_THREAD__MAX_ITERATIONS:
				return maxIterations != MAX_ITERATIONS_EDEFAULT;
			case OrchestrationPackage.CHAT_THREAD__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case OrchestrationPackage.CHAT_THREAD__MESSAGES:
				return messages != null && !messages.isEmpty();
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
		result.append(" (iterativeMode: ");
		result.append(iterativeMode);
		result.append(", selfIterativeMode: ");
		result.append(selfIterativeMode);
		result.append(", darwinMode: ");
		result.append(darwinMode);
		result.append(", gitAutomation: ");
		result.append(gitAutomation);
		result.append(", maxIterations: ");
		result.append(maxIterations);
		result.append(", id: ");
		result.append(id);
		result.append(')');
		return result.toString();
	}

} //ChatThreadImpl
