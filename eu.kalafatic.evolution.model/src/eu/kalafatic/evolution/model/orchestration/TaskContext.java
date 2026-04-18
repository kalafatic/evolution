package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Task Context</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.TaskContext#getCurrentTaskName <em>Current Task Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.TaskContext#getThreadId <em>Thread Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.TaskContext#isPaused <em>Paused</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.TaskContext#isAutoApprove <em>Auto Approve</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.TaskContext#getPlatformMode <em>Platform Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.TaskContext#getInstructionFiles <em>Instruction Files</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTaskContext()
 * @model
 * @generated
 */
public interface TaskContext extends EObject {
	/**
	 * Returns the value of the '<em><b>Current Task Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Current Task Name</em>' attribute.
	 * @see #setCurrentTaskName(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTaskContext_CurrentTaskName()
	 * @model
	 * @generated
	 */
	String getCurrentTaskName();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.TaskContext#getCurrentTaskName <em>Current Task Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Current Task Name</em>' attribute.
	 * @see #getCurrentTaskName()
	 * @generated
	 */
	void setCurrentTaskName(String value);

	/**
	 * Returns the value of the '<em><b>Thread Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Thread Id</em>' attribute.
	 * @see #setThreadId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTaskContext_ThreadId()
	 * @model
	 * @generated
	 */
	String getThreadId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.TaskContext#getThreadId <em>Thread Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Thread Id</em>' attribute.
	 * @see #getThreadId()
	 * @generated
	 */
	void setThreadId(String value);

	/**
	 * Returns the value of the '<em><b>Paused</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Paused</em>' attribute.
	 * @see #setPaused(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTaskContext_Paused()
	 * @model
	 * @generated
	 */
	boolean isPaused();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.TaskContext#isPaused <em>Paused</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Paused</em>' attribute.
	 * @see #isPaused()
	 * @generated
	 */
	void setPaused(boolean value);

	/**
	 * Returns the value of the '<em><b>Auto Approve</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Auto Approve</em>' attribute.
	 * @see #setAutoApprove(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTaskContext_AutoApprove()
	 * @model
	 * @generated
	 */
	boolean isAutoApprove();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.TaskContext#isAutoApprove <em>Auto Approve</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Auto Approve</em>' attribute.
	 * @see #isAutoApprove()
	 * @generated
	 */
	void setAutoApprove(boolean value);

	/**
	 * Returns the value of the '<em><b>Platform Mode</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Platform Mode</em>' containment reference.
	 * @see #setPlatformMode(PlatformMode)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTaskContext_PlatformMode()
	 * @model containment="true"
	 * @generated
	 */
	PlatformMode getPlatformMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.TaskContext#getPlatformMode <em>Platform Mode</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Platform Mode</em>' containment reference.
	 * @see #getPlatformMode()
	 * @generated
	 */
	void setPlatformMode(PlatformMode value);

	/**
	 * Returns the value of the '<em><b>Instruction Files</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Instruction Files</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getTaskContext_InstructionFiles()
	 * @model
	 * @generated
	 */
	EList<String> getInstructionFiles();

} // TaskContext
