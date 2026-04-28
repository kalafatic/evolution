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
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getRationale <em>Rationale</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getScheduledTime <em>Scheduled Time</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#isSelected <em>Selected</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getGoal <em>Goal</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getPlan <em>Plan</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getArtifacts <em>Artifacts</em>}</li>
	 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getPrompt <em>Prompt</em>}</li>
	 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getAttachments <em>Attachments</em>}</li>
	 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Task#getLogLevel <em>Log Level</em>}</li>
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

	/**
	 * Returns the value of the '<em><b>Rationale</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Rationale</em>' attribute.
	 * @see #setRationale(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Rationale()
	 * @model
	 * @generated
	 */
	String getRationale();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getRationale <em>Rationale</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Rationale</em>' attribute.
	 * @see #getRationale()
	 * @generated
	 */
	void setRationale(String value);

	/**
	 * Returns the value of the '<em><b>Scheduled Time</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Scheduled Time</em>' attribute.
	 * @see #setScheduledTime(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_ScheduledTime()
	 * @model
	 * @generated
	 */
	String getScheduledTime();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getScheduledTime <em>Scheduled Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Scheduled Time</em>' attribute.
	 * @see #getScheduledTime()
	 * @generated
	 */
	void setScheduledTime(String value);

	/**
	 * Returns the value of the '<em><b>Selected</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Selected</em>' attribute.
	 * @see #setSelected(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Selected()
	 * @model
	 * @generated
	 */
	boolean isSelected();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#isSelected <em>Selected</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Selected</em>' attribute.
	 * @see #isSelected()
	 * @generated
	 */
	void setSelected(boolean value);

	/**
	 * Returns the value of the '<em><b>Goal</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Goal</em>' attribute.
	 * @see #setGoal(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Goal()
	 * @model
	 * @generated
	 */
	String getGoal();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getGoal <em>Goal</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Goal</em>' attribute.
	 * @see #getGoal()
	 * @generated
	 */
	void setGoal(String value);

	/**
	 * Returns the value of the '<em><b>Plan</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Plan</em>' attribute.
	 * @see #setPlan(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Plan()
	 * @model
	 * @generated
	 */
	String getPlan();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getPlan <em>Plan</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Plan</em>' attribute.
	 * @see #getPlan()
	 * @generated
	 */
	void setPlan(String value);

	/**
	 * Returns the value of the '<em><b>Artifacts</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Artifacts</em>' attribute.
	 * @see #setArtifacts(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Artifacts()
	 * @model
	 * @generated
	 */
	String getArtifacts();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getArtifacts <em>Artifacts</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Artifacts</em>' attribute.
	 * @see #getArtifacts()
	 * @generated
	 */
	void setArtifacts(String value);

	/**
	 * Returns the value of the '<em><b>Prompt</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Prompt</em>' attribute.
	 * @see #setPrompt(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Prompt()
	 * @model
	 * @generated
	 */
	String getPrompt();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getPrompt <em>Prompt</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Prompt</em>' attribute.
	 * @see #getPrompt()
	 * @generated
	 */
	void setPrompt(String value);

	/**
	 * Returns the value of the '<em><b>Log Level</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.LogLevel}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Log Level</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.LogLevel
	 * @see #setLogLevel(LogLevel)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_LogLevel()
	 * @model
	 * @generated
	 */
	LogLevel getLogLevel();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Task#getLogLevel <em>Log Level</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Log Level</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.LogLevel
	 * @see #getLogLevel()
	 * @generated
	 */
	void setLogLevel(LogLevel value);

	/**
	 * Returns the value of the '<em><b>Attachments</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Attachments</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTask_Attachments()
	 * @model
	 * @generated
	 */
	EList<String> getAttachments();

} // Task
