/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Task</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getName <em>Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getType <em>Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getStatus <em>Status</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getNext <em>Next</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getSubTasks <em>Sub Tasks</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getResponse <em>Response</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getFeedback <em>Feedback</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#isApprovalRequired <em>Approval Required</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getLoopToTaskId <em>Loop To Task Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getPriority <em>Priority</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getResultSummary <em>Result Summary</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getDescription <em>Description</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getRating <em>Rating</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#isLikes <em>Likes</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask()
 * @model
 * @generated
 */
public interface Task extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Id()
	 * @model
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see #setType(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Type()
	 * @model
	 * @generated
	 */
	String getType();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see #getType()
	 * @generated
	 */
	void setType(String value);

	/**
	 * Returns the value of the '<em><b>Status</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.TaskStatus}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Status</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.TaskStatus
	 * @see #setStatus(TaskStatus)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Status()
	 * @model
	 * @generated
	 */
	TaskStatus getStatus();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getStatus <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Status</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.TaskStatus
	 * @see #getStatus()
	 * @generated
	 */
	void setStatus(TaskStatus value);

	/**
	 * Returns the value of the '<em><b>Next</b></em>' reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.Task}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Next</em>' reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Next()
	 * @model
	 * @generated
	 */
	EList<Task> getNext();

	/**
	 * Returns the value of the '<em><b>Sub Tasks</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.Task}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Sub Tasks</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_SubTasks()
	 * @model containment="true"
	 * @generated
	 */
	EList<Task> getSubTasks();

	/**
	 * Returns the value of the '<em><b>Response</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Response</em>' attribute.
	 * @see #setResponse(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Response()
	 * @model
	 * @generated
	 */
	String getResponse();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getResponse <em>Response</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Response</em>' attribute.
	 * @see #getResponse()
	 * @generated
	 */
	void setResponse(String value);

	/**
	 * Returns the value of the '<em><b>Feedback</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Feedback</em>' attribute.
	 * @see #setFeedback(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Feedback()
	 * @model
	 * @generated
	 */
	String getFeedback();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getFeedback <em>Feedback</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Feedback</em>' attribute.
	 * @see #getFeedback()
	 * @generated
	 */
	void setFeedback(String value);

	/**
	 * Returns the value of the '<em><b>Approval Required</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Approval Required</em>' attribute.
	 * @see #setApprovalRequired(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_ApprovalRequired()
	 * @model default="true"
	 * @generated
	 */
	boolean isApprovalRequired();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#isApprovalRequired <em>Approval Required</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Approval Required</em>' attribute.
	 * @see #isApprovalRequired()
	 * @generated
	 */
	void setApprovalRequired(boolean value);

	/**
	 * Returns the value of the '<em><b>Loop To Task Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Loop To Task Id</em>' attribute.
	 * @see #setLoopToTaskId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_LoopToTaskId()
	 * @model
	 * @generated
	 */
	String getLoopToTaskId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getLoopToTaskId <em>Loop To Task Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Loop To Task Id</em>' attribute.
	 * @see #getLoopToTaskId()
	 * @generated
	 */
	void setLoopToTaskId(String value);

	/**
	 * Returns the value of the '<em><b>Priority</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Priority</em>' attribute.
	 * @see #setPriority(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Priority()
	 * @model
	 * @generated
	 */
	int getPriority();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getPriority <em>Priority</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Priority</em>' attribute.
	 * @see #getPriority()
	 * @generated
	 */
	void setPriority(int value);

	/**
	 * Returns the value of the '<em><b>Result Summary</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Result Summary</em>' attribute.
	 * @see #setResultSummary(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_ResultSummary()
	 * @model
	 * @generated
	 */
	String getResultSummary();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getResultSummary <em>Result Summary</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Result Summary</em>' attribute.
	 * @see #getResultSummary()
	 * @generated
	 */
	void setResultSummary(String value);

	/**
	 * Returns the value of the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Description</em>' attribute.
	 * @see #setDescription(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Description()
	 * @model
	 * @generated
	 */
	String getDescription();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getDescription <em>Description</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Description</em>' attribute.
	 * @see #getDescription()
	 * @generated
	 */
	void setDescription(String value);

	/**
	 * Returns the value of the '<em><b>Rating</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Rating</em>' attribute.
	 * @see #setRating(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Rating()
	 * @model
	 * @generated
	 */
	int getRating();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getRating <em>Rating</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Rating</em>' attribute.
	 * @see #getRating()
	 * @generated
	 */
	void setRating(int value);

	/**
	 * Returns the value of the '<em><b>Likes</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Likes</em>' attribute.
	 * @see #setLikes(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Likes()
	 * @model
	 * @generated
	 */
	boolean isLikes();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#isLikes <em>Likes</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Likes</em>' attribute.
	 * @see #isLikes()
	 * @generated
	 */
	void setLikes(boolean value);

} // Task
