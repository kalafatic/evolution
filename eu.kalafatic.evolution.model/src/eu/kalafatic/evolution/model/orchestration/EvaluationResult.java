/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Evaluation Result</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#isSuccess <em>Success</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getTestPassRate <em>Test Pass Rate</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getCoverageChange <em>Coverage Change</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getErrors <em>Errors</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getDecision <em>Decision</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getEvaluationResult()
 * @model
 * @generated
 */
public interface EvaluationResult extends EObject {
	/**
	 * Returns the value of the '<em><b>Success</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Success</em>' attribute.
	 * @see #setSuccess(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getEvaluationResult_Success()
	 * @model
	 * @generated
	 */
	boolean isSuccess();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#isSuccess <em>Success</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Success</em>' attribute.
	 * @see #isSuccess()
	 * @generated
	 */
	void setSuccess(boolean value);

	/**
	 * Returns the value of the '<em><b>Test Pass Rate</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Test Pass Rate</em>' attribute.
	 * @see #setTestPassRate(double)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getEvaluationResult_TestPassRate()
	 * @model
	 * @generated
	 */
	double getTestPassRate();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getTestPassRate <em>Test Pass Rate</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Test Pass Rate</em>' attribute.
	 * @see #getTestPassRate()
	 * @generated
	 */
	void setTestPassRate(double value);

	/**
	 * Returns the value of the '<em><b>Coverage Change</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Coverage Change</em>' attribute.
	 * @see #setCoverageChange(double)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getEvaluationResult_CoverageChange()
	 * @model
	 * @generated
	 */
	double getCoverageChange();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getCoverageChange <em>Coverage Change</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Coverage Change</em>' attribute.
	 * @see #getCoverageChange()
	 * @generated
	 */
	void setCoverageChange(double value);

	/**
	 * Returns the value of the '<em><b>Errors</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Errors</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getEvaluationResult_Errors()
	 * @model
	 * @generated
	 */
	EList<String> getErrors();

	/**
	 * Returns the value of the '<em><b>Decision</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.SelfDevDecision}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Decision</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevDecision
	 * @see #setDecision(SelfDevDecision)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getEvaluationResult_Decision()
	 * @model
	 * @generated
	 */
	SelfDevDecision getDecision();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getDecision <em>Decision</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Decision</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevDecision
	 * @see #getDecision()
	 * @generated
	 */
	void setDecision(SelfDevDecision value);

} // EvaluationResult
