/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Chat Session</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getMessages <em>Messages</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isIterativeMode <em>Iterative Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isSelfIterativeMode <em>Self Iterative Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isDarwinMode <em>Darwin Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isGitAutomation <em>Git Automation</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getMaxIterations <em>Max Iterations</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isStepMode <em>Step Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getTargetPath <em>Target Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getTargetType <em>Target Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getOutputPath <em>Output Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isAutoApprove <em>Auto Approve</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getAiMode <em>Ai Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getLocalModel <em>Local Model</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getRemoteModel <em>Remote Model</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getBitState <em>Bit State</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getExpansion <em>Expansion</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession()
 * @model
 * @generated
 */
public interface ChatSession extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_Id()
	 * @model
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Messages</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.ChatMessage}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Messages</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_Messages()
	 * @model containment="true"
	 * @generated
	 */
	EList<ChatMessage> getMessages();

	/**
	 * Returns the value of the '<em><b>Iterative Mode</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Iterative Mode</em>' attribute.
	 * @see #setIterativeMode(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_IterativeMode()
	 * @model default="true"
	 * @generated
	 */
	boolean isIterativeMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isIterativeMode <em>Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Iterative Mode</em>' attribute.
	 * @see #isIterativeMode()
	 * @generated
	 */
	void setIterativeMode(boolean value);

	/**
	 * Returns the value of the '<em><b>Self Iterative Mode</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Self Iterative Mode</em>' attribute.
	 * @see #setSelfIterativeMode(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_SelfIterativeMode()
	 * @model default="false"
	 * @generated
	 */
	boolean isSelfIterativeMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isSelfIterativeMode <em>Self Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Self Iterative Mode</em>' attribute.
	 * @see #isSelfIterativeMode()
	 * @generated
	 */
	void setSelfIterativeMode(boolean value);

	/**
	 * Returns the value of the '<em><b>Darwin Mode</b></em>' attribute.
	 * The default value is <code>"true"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Darwin Mode</em>' attribute.
	 * @see #setDarwinMode(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_DarwinMode()
	 * @model default="true"
	 * @generated
	 */
	boolean isDarwinMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isDarwinMode <em>Darwin Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Darwin Mode</em>' attribute.
	 * @see #isDarwinMode()
	 * @generated
	 */
	void setDarwinMode(boolean value);

	/**
	 * Returns the value of the '<em><b>Git Automation</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Git Automation</em>' attribute.
	 * @see #setGitAutomation(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_GitAutomation()
	 * @model default="false"
	 * @generated
	 */
	boolean isGitAutomation();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isGitAutomation <em>Git Automation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Git Automation</em>' attribute.
	 * @see #isGitAutomation()
	 * @generated
	 */
	void setGitAutomation(boolean value);

	/**
	 * Returns the value of the '<em><b>Max Iterations</b></em>' attribute.
	 * The default value is <code>"1"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Max Iterations</em>' attribute.
	 * @see #setMaxIterations(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_MaxIterations()
	 * @model default="1"
	 * @generated
	 */
	int getMaxIterations();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getMaxIterations <em>Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Max Iterations</em>' attribute.
	 * @see #getMaxIterations()
	 * @generated
	 */
	void setMaxIterations(int value);

	/**
	 * Returns the value of the '<em><b>Step Mode</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Step Mode</em>' attribute.
	 * @see #setStepMode(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_StepMode()
	 * @model default="false"
	 * @generated
	 */
	boolean isStepMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isStepMode <em>Step Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Step Mode</em>' attribute.
	 * @see #isStepMode()
	 * @generated
	 */
	void setStepMode(boolean value);

	/**
	 * Returns the value of the '<em><b>Target Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target Path</em>' attribute.
	 * @see #setTargetPath(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_TargetPath()
	 * @model
	 * @generated
	 */
	String getTargetPath();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getTargetPath <em>Target Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target Path</em>' attribute.
	 * @see #getTargetPath()
	 * @generated
	 */
	void setTargetPath(String value);

	/**
	 * Returns the value of the '<em><b>Target Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target Type</em>' attribute.
	 * @see #setTargetType(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_TargetType()
	 * @model
	 * @generated
	 */
	String getTargetType();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getTargetType <em>Target Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target Type</em>' attribute.
	 * @see #getTargetType()
	 * @generated
	 */
	void setTargetType(String value);

	/**
	 * Returns the value of the '<em><b>Output Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Output Path</em>' attribute.
	 * @see #setOutputPath(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_OutputPath()
	 * @model
	 * @generated
	 */
	String getOutputPath();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getOutputPath <em>Output Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Output Path</em>' attribute.
	 * @see #getOutputPath()
	 * @generated
	 */
	void setOutputPath(String value);

	/**
	 * Returns the value of the '<em><b>Auto Approve</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Auto Approve</em>' attribute.
	 * @see #setAutoApprove(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_AutoApprove()
	 * @model default="false"
	 * @generated
	 */
	boolean isAutoApprove();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#isAutoApprove <em>Auto Approve</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Auto Approve</em>' attribute.
	 * @see #isAutoApprove()
	 * @generated
	 */
	void setAutoApprove(boolean value);

	/**
	 * Returns the value of the '<em><b>Ai Mode</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.AiMode}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ai Mode</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.AiMode
	 * @see #setAiMode(AiMode)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_AiMode()
	 * @model
	 * @generated
	 */
	AiMode getAiMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getAiMode <em>Ai Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ai Mode</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.AiMode
	 * @see #getAiMode()
	 * @generated
	 */
	void setAiMode(AiMode value);

	/**
	 * Returns the value of the '<em><b>Local Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Local Model</em>' attribute.
	 * @see #setLocalModel(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_LocalModel()
	 * @model
	 * @generated
	 */
	String getLocalModel();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getLocalModel <em>Local Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Local Model</em>' attribute.
	 * @see #getLocalModel()
	 * @generated
	 */
	void setLocalModel(String value);

	/**
	 * Returns the value of the '<em><b>Remote Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Remote Model</em>' attribute.
	 * @see #setRemoteModel(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_RemoteModel()
	 * @model
	 * @generated
	 */
	String getRemoteModel();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getRemoteModel <em>Remote Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Remote Model</em>' attribute.
	 * @see #getRemoteModel()
	 * @generated
	 */
	void setRemoteModel(String value);

	/**
	 * Returns the value of the '<em><b>Bit State</b></em>' attribute.
	 * The default value is <code>"0"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Bit State</em>' attribute.
	 * @see #setBitState(long)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_BitState()
	 * @model default="0"
	 * @generated
	 */
	long getBitState();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getBitState <em>Bit State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Bit State</em>' attribute.
	 * @see #getBitState()
	 * @generated
	 */
	void setBitState(long value);

	/**
	 * Returns the value of the '<em><b>Expansion</b></em>' attribute.
	 * The default value is <code>"5"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Expansion</em>' attribute.
	 * @see #setExpansion(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatSession_Expansion()
	 * @model default="5"
	 * @generated
	 */
	int getExpansion();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatSession#getExpansion <em>Expansion</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Expansion</em>' attribute.
	 * @see #getExpansion()
	 * @generated
	 */
	void setExpansion(int value);

} // ChatSession
