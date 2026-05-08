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

} // ChatSession
