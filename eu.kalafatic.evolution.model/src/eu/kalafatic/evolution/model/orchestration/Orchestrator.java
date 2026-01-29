/**
 */
package eu.kalafatic.evolution.model.orchestration;

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
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getName <em>Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getAgents <em>Agents</em>}</li>
	 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getTasks <em>Tasks</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getGit <em>Git</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getMaven <em>Maven</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getLlm <em>Llm</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getCompiler <em>Compiler</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getOllama <em>Ollama</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getAiChat <em>Ai Chat</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator()
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
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Id()
	 * @model
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getId <em>Id</em>}' attribute.
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
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Agents</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.Agent}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Agents</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Agents()
	 * @model containment="true"
	 * @generated
	 */
	EList<Agent> getAgents();

	/**
	 * Returns the value of the '<em><b>Tasks</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.Task}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tasks</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Tasks()
	 * @model containment="true"
	 * @generated NOT
	 */
	EList<Task> getTasks();

	/**
	 * Returns the value of the '<em><b>Git</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Git</em>' containment reference.
	 * @see #setGit(Git)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Git()
	 * @model containment="true"
	 * @generated
	 */
	Git getGit();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getGit <em>Git</em>}' containment reference.
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
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Maven()
	 * @model containment="true"
	 * @generated
	 */
	Maven getMaven();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getMaven <em>Maven</em>}' containment reference.
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
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Llm()
	 * @model containment="true"
	 * @generated
	 */
	LLM getLlm();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getLlm <em>Llm</em>}' containment reference.
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
	 * @see #setCompiler(eu.kalafatic.evolution.model.orchestration.Compiler)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Compiler()
	 * @model containment="true"
	 * @generated
	 */
	eu.kalafatic.evolution.model.orchestration.Compiler getCompiler();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getCompiler <em>Compiler</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Compiler</em>' containment reference.
	 * @see #getCompiler()
	 * @generated
	 */
	void setCompiler(eu.kalafatic.evolution.model.orchestration.Compiler value);

	/**
	 * Returns the value of the '<em><b>Ollama</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ollama</em>' containment reference.
	 * @see #setOllama(Ollama)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Ollama()
	 * @model containment="true"
	 * @generated
	 */
	Ollama getOllama();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getOllama <em>Ollama</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ollama</em>' containment reference.
	 * @see #getOllama()
	 * @generated
	 */
	void setOllama(Ollama value);

	/**
	 * Returns the value of the '<em><b>Ai Chat</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ai Chat</em>' containment reference.
	 * @see #setAiChat(AiChat)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_AiChat()
	 * @model containment="true"
	 * @generated
	 */
	AiChat getAiChat();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getAiChat <em>Ai Chat</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ai Chat</em>' containment reference.
	 * @see #getAiChat()
	 * @generated
	 */
	void setAiChat(AiChat value);

	/**
	 * Returns the value of the '<em><b>Neuron AI</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Neuron AI</em>' containment reference.
	 * @see #setNeuronAI(NeuronAI)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_NeuronAI()
	 * @model containment="true"
	 * @generated
	 */
	NeuronAI getNeuronAI();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getNeuronAI <em>Neuron AI</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Neuron AI</em>' containment reference.
	 * @see #getNeuronAI()
	 * @generated
	 */
	void setNeuronAI(NeuronAI value);

} // Orchestrator
