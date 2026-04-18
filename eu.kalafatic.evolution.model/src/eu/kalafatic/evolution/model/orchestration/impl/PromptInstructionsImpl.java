/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Prompt Instructions</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.PromptInstructionsImpl#isAutoApprove <em>Auto Approve</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.PromptInstructionsImpl#isGitAutomation <em>Git Automation</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.PromptInstructionsImpl#getPreferredMaxIterations <em>Preferred Max Iterations</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.PromptInstructionsImpl#isIterativeMode <em>Iterative Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.PromptInstructionsImpl#isSelfIterativeMode <em>Self Iterative Mode</em>}</li>
 * </ul>
 *
 * @generated
 */
public class PromptInstructionsImpl extends MinimalEObjectImpl.Container implements PromptInstructions {
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
	 * The default value of the '{@link #getPreferredMaxIterations() <em>Preferred Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPreferredMaxIterations()
	 * @generated
	 * @ordered
	 */
	protected static final int PREFERRED_MAX_ITERATIONS_EDEFAULT = 4;

	/**
	 * The cached value of the '{@link #getPreferredMaxIterations() <em>Preferred Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPreferredMaxIterations()
	 * @generated
	 * @ordered
	 */
	protected int preferredMaxIterations = PREFERRED_MAX_ITERATIONS_EDEFAULT;

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
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected PromptInstructionsImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.PROMPT_INSTRUCTIONS;
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.PROMPT_INSTRUCTIONS__AUTO_APPROVE, oldAutoApprove, autoApprove));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.PROMPT_INSTRUCTIONS__GIT_AUTOMATION, oldGitAutomation, gitAutomation));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getPreferredMaxIterations() {
		return preferredMaxIterations;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPreferredMaxIterations(int newPreferredMaxIterations) {
		int oldPreferredMaxIterations = preferredMaxIterations;
		preferredMaxIterations = newPreferredMaxIterations;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.PROMPT_INSTRUCTIONS__PREFERRED_MAX_ITERATIONS, oldPreferredMaxIterations, preferredMaxIterations));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.PROMPT_INSTRUCTIONS__ITERATIVE_MODE, oldIterativeMode, iterativeMode));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.PROMPT_INSTRUCTIONS__SELF_ITERATIVE_MODE, oldSelfIterativeMode, selfIterativeMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__AUTO_APPROVE:
				return isAutoApprove();
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__GIT_AUTOMATION:
				return isGitAutomation();
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__PREFERRED_MAX_ITERATIONS:
				return getPreferredMaxIterations();
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__ITERATIVE_MODE:
				return isIterativeMode();
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__SELF_ITERATIVE_MODE:
				return isSelfIterativeMode();
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
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__AUTO_APPROVE:
				setAutoApprove((Boolean)newValue);
				return;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__GIT_AUTOMATION:
				setGitAutomation((Boolean)newValue);
				return;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__PREFERRED_MAX_ITERATIONS:
				setPreferredMaxIterations((Integer)newValue);
				return;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__ITERATIVE_MODE:
				setIterativeMode((Boolean)newValue);
				return;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__SELF_ITERATIVE_MODE:
				setSelfIterativeMode((Boolean)newValue);
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
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__AUTO_APPROVE:
				setAutoApprove(AUTO_APPROVE_EDEFAULT);
				return;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__GIT_AUTOMATION:
				setGitAutomation(GIT_AUTOMATION_EDEFAULT);
				return;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__PREFERRED_MAX_ITERATIONS:
				setPreferredMaxIterations(PREFERRED_MAX_ITERATIONS_EDEFAULT);
				return;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__ITERATIVE_MODE:
				setIterativeMode(ITERATIVE_MODE_EDEFAULT);
				return;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__SELF_ITERATIVE_MODE:
				setSelfIterativeMode(SELF_ITERATIVE_MODE_EDEFAULT);
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
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__AUTO_APPROVE:
				return autoApprove != AUTO_APPROVE_EDEFAULT;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__GIT_AUTOMATION:
				return gitAutomation != GIT_AUTOMATION_EDEFAULT;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__PREFERRED_MAX_ITERATIONS:
				return preferredMaxIterations != PREFERRED_MAX_ITERATIONS_EDEFAULT;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__ITERATIVE_MODE:
				return iterativeMode != ITERATIVE_MODE_EDEFAULT;
			case OrchestrationPackage.PROMPT_INSTRUCTIONS__SELF_ITERATIVE_MODE:
				return selfIterativeMode != SELF_ITERATIVE_MODE_EDEFAULT;
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
		result.append(" (autoApprove: ");
		result.append(autoApprove);
		result.append(", gitAutomation: ");
		result.append(gitAutomation);
		result.append(", preferredMaxIterations: ");
		result.append(preferredMaxIterations);
		result.append(", iterativeMode: ");
		result.append(iterativeMode);
		result.append(", selfIterativeMode: ");
		result.append(selfIterativeMode);
		result.append(')');
		return result.toString();
	}

} //PromptInstructionsImpl
