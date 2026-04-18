/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Prompt Instructions</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.PromptInstructions#isAutoApprove <em>Auto Approve</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.PromptInstructions#isGitAutomation <em>Git Automation</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.PromptInstructions#getPreferredMaxIterations <em>Preferred Max Iterations</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.PromptInstructions#isIterativeMode <em>Iterative Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.PromptInstructions#isSelfIterativeMode <em>Self Iterative Mode</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPromptInstructions()
 * @model
 * @generated
 */
public interface PromptInstructions extends EObject {
	/**
	 * Returns the value of the '<em><b>Auto Approve</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Auto Approve</em>' attribute.
	 * @see #setAutoApprove(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPromptInstructions_AutoApprove()
	 * @model
	 * @generated
	 */
	boolean isAutoApprove();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.PromptInstructions#isAutoApprove <em>Auto Approve</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Auto Approve</em>' attribute.
	 * @see #isAutoApprove()
	 * @generated
	 */
	void setAutoApprove(boolean value);

	/**
	 * Returns the value of the '<em><b>Git Automation</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Git Automation</em>' attribute.
	 * @see #setGitAutomation(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPromptInstructions_GitAutomation()
	 * @model
	 * @generated
	 */
	boolean isGitAutomation();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.PromptInstructions#isGitAutomation <em>Git Automation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Git Automation</em>' attribute.
	 * @see #isGitAutomation()
	 * @generated
	 */
	void setGitAutomation(boolean value);

	/**
	 * Returns the value of the '<em><b>Preferred Max Iterations</b></em>' attribute.
	 * The default value is <code>"4"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Preferred Max Iterations</em>' attribute.
	 * @see #setPreferredMaxIterations(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPromptInstructions_PreferredMaxIterations()
	 * @model default="4" ordered="false"
	 * @generated
	 */
	int getPreferredMaxIterations();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.PromptInstructions#getPreferredMaxIterations <em>Preferred Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Preferred Max Iterations</em>' attribute.
	 * @see #getPreferredMaxIterations()
	 * @generated
	 */
	void setPreferredMaxIterations(int value);

	/**
	 * Returns the value of the '<em><b>Iterative Mode</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Iterative Mode</em>' attribute.
	 * @see #setIterativeMode(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPromptInstructions_IterativeMode()
	 * @model default="false"
	 * @generated
	 */
	boolean isIterativeMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.PromptInstructions#isIterativeMode <em>Iterative Mode</em>}' attribute.
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
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPromptInstructions_SelfIterativeMode()
	 * @model default="false"
	 * @generated
	 */
	boolean isSelfIterativeMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.PromptInstructions#isSelfIterativeMode <em>Self Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Self Iterative Mode</em>' attribute.
	 * @see #isSelfIterativeMode()
	 * @generated
	 */
	void setSelfIterativeMode(boolean value);

} // PromptInstructions
