/**
 */
package orchestration;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Orchestrator</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link orchestration.Orchestrator#getId <em>Id</em>}</li>
 *   <li>{@link orchestration.Orchestrator#getName <em>Name</em>}</li>
 *   <li>{@link orchestration.Orchestrator#getAgents <em>Agents</em>}</li>
 *   <li>{@link orchestration.Orchestrator#getGit <em>Git</em>}</li>
 *   <li>{@link orchestration.Orchestrator#getMaven <em>Maven</em>}</li>
 *   <li>{@link orchestration.Orchestrator#getLlm <em>Llm</em>}</li>
 *   <li>{@link orchestration.Orchestrator#getCompiler <em>Compiler</em>}</li>
 * </ul>
 *
 * @see orchestration.OrchestrationPackage#getOrchestrator()
 * @model
 * @generated
 */
public interface Orchestrator extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see orchestration.OrchestrationPackage#getOrchestrator_Id()
	 * @model
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link orchestration.Orchestrator#getId <em>Id</em>}' attribute.
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
	 * @see orchestration.OrchestrationPackage#getOrchestrator_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link orchestration.Orchestrator#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Agents</b></em>' containment reference list.
	 * The list contents are of type {@link orchestration.Agent}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Agents</em>' containment reference list.
	 * @see orchestration.OrchestrationPackage#getOrchestrator_Agents()
	 * @model containment="true"
	 * @generated
	 */
	EList<Agent> getAgents();

	/**
	 * Returns the value of the '<em><b>Git</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Git</em>' containment reference.
	 * @see #setGit(Git)
	 * @see orchestration.OrchestrationPackage#getOrchestrator_Git()
	 * @model containment="true"
	 * @generated
	 */
	Git getGit();

	/**
	 * Sets the value of the '{@link orchestration.Orchestrator#getGit <em>Git</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Git</em>' containment reference.
	 * @see #getGit()
	 * @generated
	 */
	void setGit(Git value);

	/**
	 * Returns the value of the '<em><b>Maven</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Maven</em>' containment reference.
	 * @see #setMaven(Maven)
	 * @see orchestration.OrchestrationPackage#getOrchestrator_Maven()
	 * @model containment="true"
	 * @generated
	 */
	Maven getMaven();

	/**
	 * Sets the value of the '{@link orchestration.Orchestrator#getMaven <em>Maven</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Maven</em>' containment reference.
	 * @see #getMaven()
	 * @generated
	 */
	void setMaven(Maven value);

	/**
	 * Returns the value of the '<em><b>Llm</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Llm</em>' containment reference.
	 * @see #setLlm(LLM)
	 * @see orchestration.OrchestrationPackage#getOrchestrator_Llm()
	 * @model containment="true"
	 * @generated
	 */
	LLM getLlm();

	/**
	 * Sets the value of the '{@link orchestration.Orchestrator#getLlm <em>Llm</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Llm</em>' containment reference.
	 * @see #getLlm()
	 * @generated
	 */
	void setLlm(LLM value);

	/**
	 * Returns the value of the '<em><b>Compiler</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Compiler</em>' containment reference.
	 * @see #setCompiler(orchestration.Compiler)
	 * @see orchestration.OrchestrationPackage#getOrchestrator_Compiler()
	 * @model containment="true"
	 * @generated
	 */
	orchestration.Compiler getCompiler();

	/**
	 * Sets the value of the '{@link orchestration.Orchestrator#getCompiler <em>Compiler</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Compiler</em>' containment reference.
	 * @see #getCompiler()
	 * @generated
	 */
	void setCompiler(orchestration.Compiler value);

} // Orchestrator
