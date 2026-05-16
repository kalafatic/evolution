/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Iteration</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Iteration#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Iteration#getBranchName <em>Branch Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Iteration#getTasks <em>Tasks</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Iteration#getEvaluationResult <em>Evaluation Result</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Iteration#getStatus <em>Status</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Iteration#getPhase <em>Phase</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Iteration#getComments <em>Comments</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Iteration#getRating <em>Rating</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Iteration#getRationale <em>Rationale</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration()
 * @model
 * @generated
 */
public interface Iteration extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_Id()
	 * @model
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Branch Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Branch Name</em>' attribute.
	 * @see #setBranchName(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_BranchName()
	 * @model
	 * @generated
	 */
	String getBranchName();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getBranchName <em>Branch Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Branch Name</em>' attribute.
	 * @see #getBranchName()
	 * @generated
	 */
	void setBranchName(String value);

	/**
	 * Returns the value of the '<em><b>Tasks</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.Task}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tasks</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_Tasks()
	 * @model containment="true"
	 * @generated
	 */
	EList<Task> getTasks();

	/**
	 * Returns the value of the '<em><b>Evaluation Result</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Evaluation Result</em>' containment reference.
	 * @see #setEvaluationResult(EvaluationResult)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_EvaluationResult()
	 * @model containment="true"
	 * @generated
	 */
	EvaluationResult getEvaluationResult();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getEvaluationResult <em>Evaluation Result</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Evaluation Result</em>' containment reference.
	 * @see #getEvaluationResult()
	 * @generated
	 */
	void setEvaluationResult(EvaluationResult value);

	/**
	 * Returns the value of the '<em><b>Status</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.IterationStatus}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Status</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.IterationStatus
	 * @see #setStatus(IterationStatus)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_Status()
	 * @model
	 * @generated
	 */
	IterationStatus getStatus();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getStatus <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Status</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.IterationStatus
	 * @see #getStatus()
	 * @generated
	 */
	void setStatus(IterationStatus value);

	/**
	 * Returns the value of the '<em><b>Phase</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Phase</em>' attribute.
	 * @see #setPhase(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_Phase()
	 * @model
	 * @generated
	 */
	String getPhase();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getPhase <em>Phase</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Phase</em>' attribute.
	 * @see #getPhase()
	 * @generated
	 */
	void setPhase(String value);

	/**
	 * Returns the value of the '<em><b>Comments</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Comments</em>' attribute.
	 * @see #setComments(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_Comments()
	 * @model
	 * @generated
	 */
	String getComments();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getComments <em>Comments</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Comments</em>' attribute.
	 * @see #getComments()
	 * @generated
	 */
	void setComments(String value);

	/**
	 * Returns the value of the '<em><b>Rating</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Rating</em>' attribute.
	 * @see #setRating(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_Rating()
	 * @model
	 * @generated
	 */
	int getRating();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getRating <em>Rating</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Rating</em>' attribute.
	 * @see #getRating()
	 * @generated
	 */
	void setRating(int value);

	/**
	 * Returns the value of the '<em><b>Rationale</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Rationale</em>' attribute.
	 * @see #setRationale(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_Rationale()
	 * @model
	 * @generated
	 */
	String getRationale();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getRationale <em>Rationale</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Rationale</em>' attribute.
	 * @see #getRationale()
	 * @generated
	 */
	void setRationale(String value);

	/**
	 * Returns the value of the '<em><b>Justification</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Justification</em>' attribute.
	 * @see #setJustification(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_Justification()
	 * @model
	 * @generated
	 */
	String getJustification();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getJustification <em>Justification</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Justification</em>' attribute.
	 * @see #getJustification()
	 * @generated
	 */
	void setJustification(String value);

	/**
	 * Returns the value of the '<em><b>Semantic Pressure</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Semantic Pressure</em>' attribute.
	 * @see #setSemanticPressure(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_SemanticPressure()
	 * @model
	 * @generated
	 */
	String getSemanticPressure();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getSemanticPressure <em>Semantic Pressure</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Semantic Pressure</em>' attribute.
	 * @see #getSemanticPressure()
	 * @generated
	 */
	void setSemanticPressure(String value);

	/**
	 * Returns the value of the '<em><b>Survival Argument</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Survival Argument</em>' attribute.
	 * @see #setSurvivalArgument(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_SurvivalArgument()
	 * @model
	 * @generated
	 */
	String getSurvivalArgument();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getSurvivalArgument <em>Survival Argument</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Survival Argument</em>' attribute.
	 * @see #getSurvivalArgument()
	 * @generated
	 */
	void setSurvivalArgument(String value);

	/**
	 * Returns the value of the '<em><b>Tradeoffs</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tradeoffs</em>' attribute.
	 * @see #setTradeoffs(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_Tradeoffs()
	 * @model
	 * @generated
	 */
	String getTradeoffs();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getTradeoffs <em>Tradeoffs</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Tradeoffs</em>' attribute.
	 * @see #getTradeoffs()
	 * @generated
	 */
	void setTradeoffs(String value);

	/**
	 * Returns the value of the '<em><b>Failure Risks</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Failure Risks</em>' attribute.
	 * @see #setFailureRisks(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getIteration_FailureRisks()
	 * @model
	 * @generated
	 */
	String getFailureRisks();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getFailureRisks <em>Failure Risks</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Failure Risks</em>' attribute.
	 * @see #getFailureRisks()
	 * @generated
	 */
	void setFailureRisks(String value);

} // Iteration
