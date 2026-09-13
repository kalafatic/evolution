/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Review Session</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ReviewSession#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ReviewSession#getChangeSet <em>Change Set</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ReviewSession#getComments <em>Comments</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ReviewSession#getDecision <em>Decision</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getReviewSession()
 * @model
 * @generated
 */
public interface ReviewSession extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getReviewSession_Id()
	 * @model
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ReviewSession#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Change Set</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Change Set</em>' containment reference.
	 * @see #setChangeSet(ChangeSet)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getReviewSession_ChangeSet()
	 * @model containment="true"
	 * @generated
	 */
	ChangeSet getChangeSet();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ReviewSession#getChangeSet <em>Change Set</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Change Set</em>' containment reference.
	 * @see #getChangeSet()
	 * @generated
	 */
	void setChangeSet(ChangeSet value);

	/**
	 * Returns the value of the '<em><b>Comments</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.Comment}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Comments</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getReviewSession_Comments()
	 * @model containment="true"
	 * @generated
	 */
	EList<Comment> getComments();

	/**
	 * Returns the value of the '<em><b>Decision</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.ReviewDecision}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Decision</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.ReviewDecision
	 * @see #setDecision(ReviewDecision)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getReviewSession_Decision()
	 * @model
	 * @generated
	 */
	ReviewDecision getDecision();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ReviewSession#getDecision <em>Decision</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Decision</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.ReviewDecision
	 * @see #getDecision()
	 * @generated
	 */
	void setDecision(ReviewDecision value);

} // ReviewSession
