/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage
 * @generated
 */
public interface OrchestrationFactory extends EFactory {
	/**
	 * The singleton instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	OrchestrationFactory eINSTANCE = eu.kalafatic.evolution.model.orchestration.impl.OrchestrationFactoryImpl.init();

	/**
	 * Returns a new object of class '<em>Task</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Task</em>'.
	 * @generated
	 */
	Task createTask();

	/**
	 * Returns a new object of class '<em>Agent</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Agent</em>'.
	 * @generated
	 */
	Agent createAgent();

	/**
	 * Returns a new object of class '<em>Orchestrator</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Orchestrator</em>'.
	 * @generated
	 */
	Orchestrator createOrchestrator();

	/**
	 * Returns a new object of class '<em>Server Settings</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Server Settings</em>'.
	 * @generated
	 */
	ServerSettings createServerSettings();

	/**
	 * Returns a new object of class '<em>Server Session</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Server Session</em>'.
	 * @generated
	 */
	ServerSession createServerSession();

	/**
	 * Returns a new object of class '<em>Monitoring Data</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Monitoring Data</em>'.
	 * @generated
	 */
	MonitoringData createMonitoringData();

	/**
	 * Returns a new object of class '<em>AI Provider</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>AI Provider</em>'.
	 * @generated
	 */
	AIProvider createAIProvider();

	/**
	 * Returns a new object of class '<em>Git</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Git</em>'.
	 * @generated
	 */
	Git createGit();

	/**
	 * Returns a new object of class '<em>Maven</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Maven</em>'.
	 * @generated
	 */
	Maven createMaven();

	/**
	 * Returns a new object of class '<em>LLM</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>LLM</em>'.
	 * @generated
	 */
	LLM createLLM();

	/**
	 * Returns a new object of class '<em>Compiler</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Compiler</em>'.
	 * @generated
	 */
	Compiler createCompiler();

	/**
	 * Returns a new object of class '<em>Command</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Command</em>'.
	 * @generated
	 */
	Command createCommand();

	/**
	 * Returns a new object of class '<em>Ollama</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Ollama</em>'.
	 * @generated
	 */
	Ollama createOllama();

	/**
	 * Returns a new object of class '<em>Ai Chat</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Ai Chat</em>'.
	 * @generated
	 */
	AiChat createAiChat();

	/**
	 * Returns a new object of class '<em>Neuron AI</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Neuron AI</em>'.
	 * @generated
	 */
	NeuronAI createNeuronAI();

	/**
	 * Returns a new object of class '<em>Evo Project</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Evo Project</em>'.
	 * @generated
	 */
	EvoProject createEvoProject();

	/**
	 * Returns a new object of class '<em>Access Rule</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Access Rule</em>'.
	 * @generated
	 */
	AccessRule createAccessRule();

	/**
	 * Returns a new object of class '<em>Network Rule</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Network Rule</em>'.
	 * @generated
	 */
	NetworkRule createNetworkRule();

	/**
	 * Returns a new object of class '<em>Memory Rule</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Memory Rule</em>'.
	 * @generated
	 */
	MemoryRule createMemoryRule();

	/**
	 * Returns a new object of class '<em>Secret Rule</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Secret Rule</em>'.
	 * @generated
	 */
	SecretRule createSecretRule();

	/**
	 * Returns a new object of class '<em>Self Dev Session</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Self Dev Session</em>'.
	 * @generated
	 */
	SelfDevSession createSelfDevSession();

	/**
	 * Returns a new object of class '<em>Database</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Database</em>'.
	 * @generated
	 */
	Database createDatabase();

	/**
	 * Returns a new object of class '<em>File Config</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>File Config</em>'.
	 * @generated
	 */
	FileConfig createFileConfig();

	/**
	 * Returns a new object of class '<em>Iteration</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Iteration</em>'.
	 * @generated
	 */
	Iteration createIteration();

	/**
	 * Returns a new object of class '<em>Eclipse</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Eclipse</em>'.
	 * @generated
	 */
	Eclipse createEclipse();

	/**
	 * Returns a new object of class '<em>Evaluation Result</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Evaluation Result</em>'.
	 * @generated
	 */
	EvaluationResult createEvaluationResult();

	/**
	 * Returns a new object of class '<em>Test</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Test</em>'.
	 * @generated
	 */
	Test createTest();

	/**
	 * Returns a new object of class '<em>Comment</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Comment</em>'.
	 * @generated
	 */
	Comment createComment();

	/**
	 * Returns a new object of class '<em>Diff Hunk</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Diff Hunk</em>'.
	 * @generated
	 */
	DiffHunk createDiffHunk();

	/**
	 * Returns a new object of class '<em>File Change</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>File Change</em>'.
	 * @generated
	 */
	FileChange createFileChange();

	/**
	 * Returns a new object of class '<em>Change Set</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Change Set</em>'.
	 * @generated
	 */
	ChangeSet createChangeSet();

	/**
	 * Returns a new object of class '<em>Review Session</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Review Session</em>'.
	 * @generated
	 */
	ReviewSession createReviewSession();

	/**
	 * Returns a new object of class '<em>Chat Thread</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Chat Thread</em>'.
	 * @generated
	 */
	ChatThread createChatThread();

	/**
	 * Returns a new object of class '<em>Chat Message</em>'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return a new object of class '<em>Chat Message</em>'.
	 * @generated
	 */
	ChatMessage createChatMessage();

	/**
	 * Returns the package supported by this factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the package supported by this factory.
	 * @generated
	 */
	OrchestrationPackage getOrchestrationPackage();

} //OrchestrationFactory
