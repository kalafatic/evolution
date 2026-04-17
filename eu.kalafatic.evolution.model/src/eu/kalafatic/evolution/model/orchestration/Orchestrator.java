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
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getTests <em>Tests</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getGit <em>Git</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getMaven <em>Maven</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getLlm <em>Llm</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getCompiler <em>Compiler</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getOllama <em>Ollama</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getAiChat <em>Ai Chat</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getNeuronAI <em>Neuron AI</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getRemoteModel <em>Remote Model</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getAiMode <em>Ai Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getMcpServerUrl <em>Mcp Server Url</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getOpenAiToken <em>Open Ai Token</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getOpenAiModel <em>Open Ai Model</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getLocalModel <em>Local Model</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getHybridModel <em>Hybrid Model</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#isOfflineMode <em>Offline Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getSelfDevSession <em>Self Dev Session</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getDatabase <em>Database</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getFileConfig <em>File Config</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getSharedMemory <em>Shared Memory</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getEclipse <em>Eclipse</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#isIterativeMode <em>Iterative Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#isSelfIterativeMode <em>Self Iterative Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#isDarwinMode <em>Darwin Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getAiProviders <em>Ai Providers</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getServerSettings <em>Server Settings</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getServerSessions <em>Server Sessions</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getMonitoringHistory <em>Monitoring History</em>}</li>
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
	 * @generated
	 */
	EList<Task> getTasks();

	/**
	 * Returns the value of the '<em><b>Tests</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.Test}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Tests</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Tests()
	 * @model containment="true"
	 * @generated
	 */
	EList<Test> getTests();

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

	/**
	 * Returns the value of the '<em><b>Remote Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Remote Model</em>' attribute.
	 * @see #setRemoteModel(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_RemoteModel()
	 * @model
	 * @generated
	 */
	String getRemoteModel();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getRemoteModel <em>Remote Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Remote Model</em>' attribute.
	 * @see #getRemoteModel()
	 * @generated
	 */
	void setRemoteModel(String value);

	/**
	 * Returns the value of the '<em><b>Ai Mode</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.AiMode}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ai Mode</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.AiMode
	 * @see #setAiMode(AiMode)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_AiMode()
	 * @model required="true"
	 * @generated
	 */
	AiMode getAiMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getAiMode <em>Ai Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Ai Mode</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.AiMode
	 * @see #getAiMode()
	 * @generated
	 */
	void setAiMode(AiMode value);

	/**
	 * Returns the value of the '<em><b>Mcp Server Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mcp Server Url</em>' attribute.
	 * @see #setMcpServerUrl(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_McpServerUrl()
	 * @model
	 * @generated
	 */
	String getMcpServerUrl();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getMcpServerUrl <em>Mcp Server Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Mcp Server Url</em>' attribute.
	 * @see #getMcpServerUrl()
	 * @generated
	 */
	void setMcpServerUrl(String value);

	/**
	 * Returns the value of the '<em><b>Open Ai Token</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Open Ai Token</em>' attribute.
	 * @see #setOpenAiToken(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_OpenAiToken()
	 * @model
	 * @generated
	 */
	String getOpenAiToken();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getOpenAiToken <em>Open Ai Token</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Open Ai Token</em>' attribute.
	 * @see #getOpenAiToken()
	 * @generated
	 */
	void setOpenAiToken(String value);

	/**
	 * Returns the value of the '<em><b>Open Ai Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Open Ai Model</em>' attribute.
	 * @see #setOpenAiModel(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_OpenAiModel()
	 * @model
	 * @generated
	 */
	String getOpenAiModel();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getOpenAiModel <em>Open Ai Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Open Ai Model</em>' attribute.
	 * @see #getOpenAiModel()
	 * @generated
	 */
	void setOpenAiModel(String value);

	/**
	 * Returns the value of the '<em><b>Local Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Local Model</em>' attribute.
	 * @see #setLocalModel(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_LocalModel()
	 * @model
	 * @generated
	 */
	String getLocalModel();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getLocalModel <em>Local Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Local Model</em>' attribute.
	 * @see #getLocalModel()
	 * @generated
	 */
	void setLocalModel(String value);

	/**
	 * Returns the value of the '<em><b>Hybrid Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Hybrid Model</em>' attribute.
	 * @see #setHybridModel(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_HybridModel()
	 * @model
	 * @generated
	 */
	String getHybridModel();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getHybridModel <em>Hybrid Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Hybrid Model</em>' attribute.
	 * @see #getHybridModel()
	 * @generated
	 */
	void setHybridModel(String value);

	/**
	 * Returns the value of the '<em><b>Offline Mode</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Offline Mode</em>' attribute.
	 * @see #setOfflineMode(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_OfflineMode()
	 * @model default="false"
	 * @generated
	 */
	boolean isOfflineMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#isOfflineMode <em>Offline Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Offline Mode</em>' attribute.
	 * @see #isOfflineMode()
	 * @generated
	 */
	void setOfflineMode(boolean value);

	/**
	 * Returns the value of the '<em><b>Self Dev Session</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Self Dev Session</em>' containment reference.
	 * @see #setSelfDevSession(SelfDevSession)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_SelfDevSession()
	 * @model containment="true"
	 * @generated
	 */
	SelfDevSession getSelfDevSession();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getSelfDevSession <em>Self Dev Session</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Self Dev Session</em>' containment reference.
	 * @see #getSelfDevSession()
	 * @generated
	 */
	void setSelfDevSession(SelfDevSession value);

	/**
	 * Returns the value of the '<em><b>Database</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Database</em>' containment reference.
	 * @see #setDatabase(Database)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Database()
	 * @model containment="true"
	 * @generated
	 */
	Database getDatabase();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getDatabase <em>Database</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Database</em>' containment reference.
	 * @see #getDatabase()
	 * @generated
	 */
	void setDatabase(Database value);

	/**
	 * Returns the value of the '<em><b>File Config</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>File Config</em>' containment reference.
	 * @see #setFileConfig(FileConfig)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_FileConfig()
	 * @model containment="true"
	 * @generated
	 */
	FileConfig getFileConfig();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getFileConfig <em>File Config</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>File Config</em>' containment reference.
	 * @see #getFileConfig()
	 * @generated
	 */
	void setFileConfig(FileConfig value);

	/**
	 * Returns the value of the '<em><b>Shared Memory</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Shared Memory</em>' attribute.
	 * @see #setSharedMemory(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_SharedMemory()
	 * @model
	 * @generated
	 */
	String getSharedMemory();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getSharedMemory <em>Shared Memory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Shared Memory</em>' attribute.
	 * @see #getSharedMemory()
	 * @generated
	 */
	void setSharedMemory(String value);

	/**
	 * Returns the value of the '<em><b>Eclipse</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Eclipse</em>' containment reference.
	 * @see #setEclipse(Eclipse)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_Eclipse()
	 * @model containment="true"
	 * @generated
	 */
	Eclipse getEclipse();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getEclipse <em>Eclipse</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Eclipse</em>' containment reference.
	 * @see #getEclipse()
	 * @generated
	 */
	void setEclipse(Eclipse value);

	/**
	 * Returns the value of the '<em><b>Iterative Mode</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Iterative Mode</em>' attribute.
	 * @see #setIterativeMode(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_IterativeMode()
	 * @model default="false"
	 * @generated
	 */
	boolean isIterativeMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#isIterativeMode <em>Iterative Mode</em>}' attribute.
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
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_SelfIterativeMode()
	 * @model default="false"
	 * @generated
	 */
	boolean isSelfIterativeMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#isSelfIterativeMode <em>Self Iterative Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Self Iterative Mode</em>' attribute.
	 * @see #isSelfIterativeMode()
	 * @generated
	 */
	void setSelfIterativeMode(boolean value);

	/**
	 * Returns the value of the '<em><b>Darwin Mode</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Darwin Mode</em>' attribute.
	 * @see #setDarwinMode(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_DarwinMode()
	 * @model default="false"
	 * @generated
	 */
	boolean isDarwinMode();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#isDarwinMode <em>Darwin Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Darwin Mode</em>' attribute.
	 * @see #isDarwinMode()
	 * @generated
	 */
	void setDarwinMode(boolean value);

	/**
	 * Returns the value of the '<em><b>Ai Providers</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.AIProvider}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Ai Providers</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_AiProviders()
	 * @model containment="true"
	 * @generated
	 */
	EList<AIProvider> getAiProviders();

	/**
	 * Returns the value of the '<em><b>Server Settings</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Server Settings</em>' containment reference.
	 * @see #setServerSettings(ServerSettings)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_ServerSettings()
	 * @model containment="true"
	 * @generated
	 */
	ServerSettings getServerSettings();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getServerSettings <em>Server Settings</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Server Settings</em>' containment reference.
	 * @see #getServerSettings()
	 * @generated
	 */
	void setServerSettings(ServerSettings value);

	/**
	 * Returns the value of the '<em><b>Server Sessions</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.ServerSession}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Server Sessions</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_ServerSessions()
	 * @model containment="true"
	 * @generated
	 */
	EList<ServerSession> getServerSessions();

	/**
	 * Returns the value of the '<em><b>Monitoring History</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.MonitoringData}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Monitoring History</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_MonitoringHistory()
	 * @model containment="true"
	 * @generated
	 */
	EList<MonitoringData> getMonitoringHistory();

	/**
	 * Returns the value of the '<em><b>Auto Approve</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Auto Approve</em>' attribute.
	 * @see #setAutoApprove(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_AutoApprove()
	 * @model
	 * @generated
	 */
	boolean isAutoApprove();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#isAutoApprove <em>Auto Approve</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Auto Approve</em>' attribute.
	 * @see #isAutoApprove()
	 * @generated
	 */
	void setAutoApprove(boolean value);

	/**
	 * Returns the value of the '<em><b>Preferred Max Iterations</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Preferred Max Iterations</em>' attribute.
	 * @see #setPreferredMaxIterations(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getOrchestrator_PreferredMaxIterations()
	 * @model
	 * @generated
	 */
	int getPreferredMaxIterations();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getPreferredMaxIterations <em>Preferred Max Iterations</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Preferred Max Iterations</em>' attribute.
	 * @see #getPreferredMaxIterations()
	 * @generated
	 */
	void setPreferredMaxIterations(int value);

} // Orchestrator
