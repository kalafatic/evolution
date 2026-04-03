/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Evaluation Result</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.EvaluationResultImpl#isSuccess <em>Success</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.EvaluationResultImpl#getTestPassRate <em>Test Pass Rate</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.EvaluationResultImpl#getCoverageChange <em>Coverage Change</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.EvaluationResultImpl#getErrors <em>Errors</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.EvaluationResultImpl#getDecision <em>Decision</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.EvaluationResultImpl#getUserSatisfaction <em>User Satisfaction</em>}</li>
 * </ul>
 *
 * @generated
 */
public class EvaluationResultImpl extends MinimalEObjectImpl.Container implements EvaluationResult {
	/**
	 * The default value of the '{@link #isSuccess() <em>Success</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSuccess()
	 * @generated
	 * @ordered
	 */
	protected static final boolean SUCCESS_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isSuccess() <em>Success</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isSuccess()
	 * @generated
	 * @ordered
	 */
	protected boolean success = SUCCESS_EDEFAULT;

	/**
	 * The default value of the '{@link #getTestPassRate() <em>Test Pass Rate</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTestPassRate()
	 * @generated
	 * @ordered
	 */
	protected static final double TEST_PASS_RATE_EDEFAULT = 0.0;

	/**
	 * The cached value of the '{@link #getTestPassRate() <em>Test Pass Rate</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTestPassRate()
	 * @generated
	 * @ordered
	 */
	protected double testPassRate = TEST_PASS_RATE_EDEFAULT;

	/**
	 * The default value of the '{@link #getCoverageChange() <em>Coverage Change</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCoverageChange()
	 * @generated
	 * @ordered
	 */
	protected static final double COVERAGE_CHANGE_EDEFAULT = 0.0;

	/**
	 * The cached value of the '{@link #getCoverageChange() <em>Coverage Change</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCoverageChange()
	 * @generated
	 * @ordered
	 */
	protected double coverageChange = COVERAGE_CHANGE_EDEFAULT;

	/**
	 * The cached value of the '{@link #getErrors() <em>Errors</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getErrors()
	 * @generated
	 * @ordered
	 */
	protected EList<String> errors;

	/**
	 * The default value of the '{@link #getDecision() <em>Decision</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDecision()
	 * @generated
	 * @ordered
	 */
	protected static final SelfDevDecision DECISION_EDEFAULT = SelfDevDecision.CONTINUE;

	/**
	 * The cached value of the '{@link #getDecision() <em>Decision</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDecision()
	 * @generated
	 * @ordered
	 */
	protected SelfDevDecision decision = DECISION_EDEFAULT;

	/**
	 * The default value of the '{@link #getUserSatisfaction() <em>User Satisfaction</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUserSatisfaction()
	 * @generated
	 * @ordered
	 */
	protected static final int USER_SATISFACTION_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getUserSatisfaction() <em>User Satisfaction</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUserSatisfaction()
	 * @generated
	 * @ordered
	 */
	protected int userSatisfaction = USER_SATISFACTION_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EvaluationResultImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.EVALUATION_RESULT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isSuccess() {
		return success;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSuccess(boolean newSuccess) {
		boolean oldSuccess = success;
		success = newSuccess;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.EVALUATION_RESULT__SUCCESS, oldSuccess, success));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public double getTestPassRate() {
		return testPassRate;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTestPassRate(double newTestPassRate) {
		double oldTestPassRate = testPassRate;
		testPassRate = newTestPassRate;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.EVALUATION_RESULT__TEST_PASS_RATE, oldTestPassRate, testPassRate));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public double getCoverageChange() {
		return coverageChange;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCoverageChange(double newCoverageChange) {
		double oldCoverageChange = coverageChange;
		coverageChange = newCoverageChange;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.EVALUATION_RESULT__COVERAGE_CHANGE, oldCoverageChange, coverageChange));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getErrors() {
		if (errors == null) {
			errors = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.EVALUATION_RESULT__ERRORS);
		}
		return errors;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public SelfDevDecision getDecision() {
		return decision;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDecision(SelfDevDecision newDecision) {
		SelfDevDecision oldDecision = decision;
		decision = newDecision == null ? DECISION_EDEFAULT : newDecision;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.EVALUATION_RESULT__DECISION, oldDecision, decision));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getUserSatisfaction() {
		return userSatisfaction;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setUserSatisfaction(int newUserSatisfaction) {
		int oldUserSatisfaction = userSatisfaction;
		userSatisfaction = newUserSatisfaction;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.EVALUATION_RESULT__USER_SATISFACTION, oldUserSatisfaction, userSatisfaction));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.EVALUATION_RESULT__SUCCESS:
				return isSuccess();
			case OrchestrationPackage.EVALUATION_RESULT__TEST_PASS_RATE:
				return getTestPassRate();
			case OrchestrationPackage.EVALUATION_RESULT__COVERAGE_CHANGE:
				return getCoverageChange();
			case OrchestrationPackage.EVALUATION_RESULT__ERRORS:
				return getErrors();
			case OrchestrationPackage.EVALUATION_RESULT__DECISION:
				return getDecision();
			case OrchestrationPackage.EVALUATION_RESULT__USER_SATISFACTION:
				return getUserSatisfaction();
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
			case OrchestrationPackage.EVALUATION_RESULT__SUCCESS:
				setSuccess((Boolean)newValue);
				return;
			case OrchestrationPackage.EVALUATION_RESULT__TEST_PASS_RATE:
				setTestPassRate((Double)newValue);
				return;
			case OrchestrationPackage.EVALUATION_RESULT__COVERAGE_CHANGE:
				setCoverageChange((Double)newValue);
				return;
			case OrchestrationPackage.EVALUATION_RESULT__ERRORS:
				getErrors().clear();
				getErrors().addAll((Collection<? extends String>)newValue);
				return;
			case OrchestrationPackage.EVALUATION_RESULT__DECISION:
				setDecision((SelfDevDecision)newValue);
				return;
			case OrchestrationPackage.EVALUATION_RESULT__USER_SATISFACTION:
				setUserSatisfaction((Integer)newValue);
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
			case OrchestrationPackage.EVALUATION_RESULT__SUCCESS:
				setSuccess(SUCCESS_EDEFAULT);
				return;
			case OrchestrationPackage.EVALUATION_RESULT__TEST_PASS_RATE:
				setTestPassRate(TEST_PASS_RATE_EDEFAULT);
				return;
			case OrchestrationPackage.EVALUATION_RESULT__COVERAGE_CHANGE:
				setCoverageChange(COVERAGE_CHANGE_EDEFAULT);
				return;
			case OrchestrationPackage.EVALUATION_RESULT__ERRORS:
				getErrors().clear();
				return;
			case OrchestrationPackage.EVALUATION_RESULT__DECISION:
				setDecision(DECISION_EDEFAULT);
				return;
			case OrchestrationPackage.EVALUATION_RESULT__USER_SATISFACTION:
				setUserSatisfaction(USER_SATISFACTION_EDEFAULT);
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
			case OrchestrationPackage.EVALUATION_RESULT__SUCCESS:
				return success != SUCCESS_EDEFAULT;
			case OrchestrationPackage.EVALUATION_RESULT__TEST_PASS_RATE:
				return testPassRate != TEST_PASS_RATE_EDEFAULT;
			case OrchestrationPackage.EVALUATION_RESULT__COVERAGE_CHANGE:
				return coverageChange != COVERAGE_CHANGE_EDEFAULT;
			case OrchestrationPackage.EVALUATION_RESULT__ERRORS:
				return errors != null && !errors.isEmpty();
			case OrchestrationPackage.EVALUATION_RESULT__DECISION:
				return decision != DECISION_EDEFAULT;
			case OrchestrationPackage.EVALUATION_RESULT__USER_SATISFACTION:
				return userSatisfaction != USER_SATISFACTION_EDEFAULT;
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
		result.append(" (success: ");
		result.append(success);
		result.append(", testPassRate: ");
		result.append(testPassRate);
		result.append(", coverageChange: ");
		result.append(coverageChange);
		result.append(", errors: ");
		result.append(errors);
		result.append(", decision: ");
		result.append(decision);
		result.append(", userSatisfaction: ");
		result.append(userSatisfaction);
		result.append(')');
		return result.toString();
	}

} //EvaluationResultImpl
