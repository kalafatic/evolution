/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each operation of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationFactory
 * @model kind="package"
 * @generated
 */
public interface OrchestrationPackage extends EPackage {
	/**
	 * The package name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNAME = "orchestration";

	/**
	 * The package namespace URI.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_URI = "http://example.com/orchestration";

	/**
	 * The package namespace name.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String eNS_PREFIX = "orchestration";

	/**
	 * The singleton instance of the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	OrchestrationPackage eINSTANCE = eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl.init();

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl <em>Task</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.TaskImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getTask()
	 * @generated
	 */
	int TASK = 0;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__ID = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__NAME = 1;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__TYPE = 2;

	/**
	 * The feature id for the '<em><b>Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__STATUS = 3;

	/**
	 * The feature id for the '<em><b>Next</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__NEXT = 4;

	/**
	 * The feature id for the '<em><b>Sub Tasks</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__SUB_TASKS = 5;

	/**
	 * The feature id for the '<em><b>Response</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__RESPONSE = 6;

	/**
	 * The feature id for the '<em><b>Feedback</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__FEEDBACK = 7;

	/**
	 * The number of structural features of the '<em>Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK_FEATURE_COUNT = 8;

	/**
	 * The number of operations of the '<em>Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.AgentImpl <em>Agent</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.AgentImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getAgent()
	 * @generated
	 */
	int AGENT = 1;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT__ID = 0;

	/**
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT__TYPE = 1;

	/**
	 * The feature id for the '<em><b>Tasks</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT__TASKS = 2;

	/**
	 * The feature id for the '<em><b>Execution Mode</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT__EXECUTION_MODE = 3;

	/**
	 * The feature id for the '<em><b>Rules</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT__RULES = 4;

	/**
	 * The number of structural features of the '<em>Agent</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT_FEATURE_COUNT = 5;

	/**
	 * The number of operations of the '<em>Agent</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl <em>Orchestrator</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getOrchestrator()
	 * @generated
	 */
	int ORCHESTRATOR = 2;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__ID = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__NAME = 1;

	/**
	 * The feature id for the '<em><b>Agents</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__AGENTS = 2;

	/**
	 * The feature id for the '<em><b>Tasks</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__TASKS = 3;

	/**
	 * The feature id for the '<em><b>Git</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__GIT = 4;

	/**
	 * The feature id for the '<em><b>Maven</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__MAVEN = 5;

	/**
	 * The feature id for the '<em><b>Llm</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__LLM = 6;

	/**
	 * The feature id for the '<em><b>Compiler</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__COMPILER = 7;

	/**
	 * The feature id for the '<em><b>Ollama</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__OLLAMA = 8;

	/**
	 * The feature id for the '<em><b>Ai Chat</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__AI_CHAT = 9;

	/**
	 * The feature id for the '<em><b>Neuron AI</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__NEURON_AI = 10;

	/**
	 * The number of structural features of the '<em>Orchestrator</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR_FEATURE_COUNT = 11;

	/**
	 * The number of operations of the '<em>Orchestrator</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.GitImpl <em>Git</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.GitImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getGit()
	 * @generated
	 */
	int GIT = 3;

	/**
	 * The feature id for the '<em><b>Repository Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GIT__REPOSITORY_URL = 0;

	/**
	 * The feature id for the '<em><b>Branch</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GIT__BRANCH = 1;

	/**
	 * The feature id for the '<em><b>Username</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GIT__USERNAME = 2;

	/**
	 * The feature id for the '<em><b>Local Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GIT__LOCAL_PATH = 3;

	/**
	 * The number of structural features of the '<em>Git</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GIT_FEATURE_COUNT = 4;

	/**
	 * The number of operations of the '<em>Git</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GIT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.MavenImpl <em>Maven</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.MavenImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getMaven()
	 * @generated
	 */
	int MAVEN = 4;

	/**
	 * The feature id for the '<em><b>Goals</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAVEN__GOALS = 0;

	/**
	 * The feature id for the '<em><b>Profiles</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAVEN__PROFILES = 1;

	/**
	 * The number of structural features of the '<em>Maven</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAVEN_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Maven</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAVEN_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.LLMImpl <em>LLM</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.LLMImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getLLM()
	 * @generated
	 */
	int LLM = 5;

	/**
	 * The feature id for the '<em><b>Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LLM__MODEL = 0;

	/**
	 * The feature id for the '<em><b>Temperature</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LLM__TEMPERATURE = 1;

	/**
	 * The number of structural features of the '<em>LLM</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LLM_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>LLM</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int LLM_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.CompilerImpl <em>Compiler</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.CompilerImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getCompiler()
	 * @generated
	 */
	int COMPILER = 6;

	/**
	 * The feature id for the '<em><b>Source Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPILER__SOURCE_VERSION = 0;

	/**
	 * The feature id for the '<em><b>Target Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPILER__TARGET_VERSION = 1;

	/**
	 * The number of structural features of the '<em>Compiler</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPILER_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Compiler</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPILER_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.CommandImpl <em>Command</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.CommandImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getCommand()
	 * @generated
	 */
	int COMMAND = 7;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMMAND__NAME = 0;

	/**
	 * The feature id for the '<em><b>Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMMAND__STATUS = 1;

	/**
	 * The number of structural features of the '<em>Command</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMMAND_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Command</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMMAND_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.OllamaImpl <em>Ollama</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OllamaImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getOllama()
	 * @generated
	 */
	int OLLAMA = 8;

	/**
	 * The feature id for the '<em><b>Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OLLAMA__URL = 0;

	/**
	 * The feature id for the '<em><b>Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OLLAMA__MODEL = 1;

	/**
	 * The feature id for the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OLLAMA__PATH = 2;

	/**
	 * The number of structural features of the '<em>Ollama</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OLLAMA_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Ollama</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int OLLAMA_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.AiChatImpl <em>Ai Chat</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.AiChatImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getAiChat()
	 * @generated
	 */
	int AI_CHAT = 9;

	/**
	 * The feature id for the '<em><b>Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AI_CHAT__URL = 0;

	/**
	 * The feature id for the '<em><b>Token</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AI_CHAT__TOKEN = 1;

	/**
	 * The feature id for the '<em><b>Prompt</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AI_CHAT__PROMPT = 2;

	/**
	 * The number of structural features of the '<em>Ai Chat</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AI_CHAT_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Ai Chat</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AI_CHAT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.EvoProjectImpl <em>Evo Project</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.EvoProjectImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getEvoProject()
	 * @generated
	 */
	int EVO_PROJECT = 10;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVO_PROJECT__NAME = 0;

	/**
	 * The feature id for the '<em><b>Orchestrations</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVO_PROJECT__ORCHESTRATIONS = 1;

	/**
	 * The number of structural features of the '<em>Evo Project</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVO_PROJECT_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Evo Project</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVO_PROJECT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.TaskStatus <em>Task Status</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.TaskStatus
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getTaskStatus()
	 * @generated
	 */
	int TASK_STATUS = 11;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.CommandStatus <em>Command Status</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.CommandStatus
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getCommandStatus()
	 * @generated
	 */
	int COMMAND_STATUS = 12;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.ExecutionMode <em>Execution Mode</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.ExecutionMode
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getExecutionMode()
	 * @generated
	 */
	int EXECUTION_MODE = 13;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.RuleImpl <em>Rule</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.RuleImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getRule()
	 * @generated
	 */
	int RULE = 14;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE__NAME = 0;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE__DESCRIPTION = 1;

	/**
	 * The number of structural features of the '<em>Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_FEATURE_COUNT = 2;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.AccessRuleImpl <em>Access Rule</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.AccessRuleImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getAccessRule()
	 * @generated
	 */
	int ACCESS_RULE = 15;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ACCESS_RULE__NAME = RULE__NAME;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ACCESS_RULE__DESCRIPTION = RULE__DESCRIPTION;

	/**
	 * The feature id for the '<em><b>Allowed Paths</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ACCESS_RULE__ALLOWED_PATHS = RULE_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Denied Paths</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ACCESS_RULE__DENIED_PATHS = RULE_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Access Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ACCESS_RULE_FEATURE_COUNT = RULE_FEATURE_COUNT + 2;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.NetworkRuleImpl <em>Network Rule</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.NetworkRuleImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getNetworkRule()
	 * @generated
	 */
	int NETWORK_RULE = 16;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NETWORK_RULE__NAME = RULE__NAME;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.MemoryRuleImpl <em>Memory Rule</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.MemoryRuleImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getMemoryRule()
	 * @generated
	 */
	int MEMORY_RULE = 17;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.SecretRuleImpl <em>Secret Rule</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.SecretRuleImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getSecretRule()
	 * @generated
	 */
	int SECRET_RULE = 18;

	// Note: Shortened for brevity, but I need to be careful with feature IDs.
	// Actually, I should define them all.

	int NETWORK_RULE__DESCRIPTION = RULE__DESCRIPTION;
	int NETWORK_RULE__ALLOWED_DOMAINS = RULE_FEATURE_COUNT + 0;
	int NETWORK_RULE__ALLOW_ALL = RULE_FEATURE_COUNT + 1;
	int NETWORK_RULE_FEATURE_COUNT = RULE_FEATURE_COUNT + 2;

	int MEMORY_RULE__NAME = RULE__NAME;
	int MEMORY_RULE__DESCRIPTION = RULE__DESCRIPTION;
	int MEMORY_RULE__STORAGE_LIMIT = RULE_FEATURE_COUNT + 0;
	int MEMORY_RULE__RETENTION_PERIOD = RULE_FEATURE_COUNT + 1;
	int MEMORY_RULE_FEATURE_COUNT = RULE_FEATURE_COUNT + 2;

	int SECRET_RULE__NAME = RULE__NAME;
	int SECRET_RULE__DESCRIPTION = RULE__DESCRIPTION;
	int SECRET_RULE__ALLOWED_SECRETS = RULE_FEATURE_COUNT + 0;
	int SECRET_RULE_FEATURE_COUNT = RULE_FEATURE_COUNT + 1;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.NeuronAIImpl <em>Neuron AI</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.NeuronAIImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getNeuronAI()
	 * @generated
	 */
	int NEURON_AI = 19;

	/**
	 * The feature id for the '<em><b>Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NEURON_AI__URL = 0;

	/**
	 * The feature id for the '<em><b>Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NEURON_AI__MODEL = 1;

	/**
	 * The number of structural features of the '<em>Neuron AI</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NEURON_AI_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>Neuron AI</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NEURON_AI_OPERATION_COUNT = 0;

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Task <em>Task</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Task</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task
	 * @generated
	 */
	EClass getTask();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getId()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Id();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getName()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Name();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getType()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Type();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getStatus <em>Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getStatus()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Status();

	/**
	 * Returns the meta object for the reference list '{@link eu.kalafatic.evolution.model.orchestration.Task#getNext <em>Next</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Next</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getNext()
	 * @see #getTask()
	 * @generated
	 */
	EReference getTask_Next();

	/**
	 * Returns the meta object for the containment reference list '{@link eu.kalafatic.evolution.model.orchestration.Task#getSubTasks <em>Sub Tasks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Sub Tasks</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getSubTasks()
	 * @see #getTask()
	 * @generated
	 */
	EReference getTask_SubTasks();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getResponse <em>Response</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Response</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getResponse()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Response();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getFeedback <em>Feedback</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Feedback</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getFeedback()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Feedback();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Agent <em>Agent</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Agent</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Agent
	 * @generated
	 */
	EClass getAgent();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Agent#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Agent#getId()
	 * @see #getAgent()
	 * @generated
	 */
	EAttribute getAgent_Id();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Agent#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Agent#getType()
	 * @see #getAgent()
	 * @generated
	 */
	EAttribute getAgent_Type();

	/**
	 * Returns the meta object for the reference list '{@link eu.kalafatic.evolution.model.orchestration.Agent#getTasks <em>Tasks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Tasks</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Agent#getTasks()
	 * @see #getAgent()
	 * @generated
	 */
	EReference getAgent_Tasks();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Agent#getExecutionMode <em>Execution Mode</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Execution Mode</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Agent#getExecutionMode()
	 * @see #getAgent()
	 * @generated
	 */
	EAttribute getAgent_ExecutionMode();

	/**
	 * Returns the meta object for the containment reference list '{@link eu.kalafatic.evolution.model.orchestration.Agent#getRules <em>Rules</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Rules</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Agent#getRules()
	 * @see #getAgent()
	 * @generated
	 */
	EReference getAgent_Rules();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator <em>Orchestrator</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Orchestrator</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator
	 * @generated
	 */
	EClass getOrchestrator();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getId()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_Id();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getName()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_Name();

	/**
	 * Returns the meta object for the containment reference list '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getAgents <em>Agents</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Agents</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getAgents()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Agents();

	/**
	 * Returns the meta object for the containment reference list '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getTasks <em>Tasks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Tasks</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getTasks()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Tasks();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getGit <em>Git</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Git</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getGit()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Git();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getMaven <em>Maven</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Maven</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getMaven()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Maven();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getLlm <em>Llm</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Llm</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getLlm()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Llm();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getCompiler <em>Compiler</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Compiler</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getCompiler()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Compiler();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getOllama <em>Ollama</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Ollama</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getOllama()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Ollama();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getAiChat <em>Ai Chat</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Ai Chat</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getAiChat()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_AiChat();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getNeuronAI <em>Neuron AI</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Neuron AI</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getNeuronAI()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_NeuronAI();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Git <em>Git</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Git</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Git
	 * @generated
	 */
	EClass getGit();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Git#getRepositoryUrl <em>Repository Url</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Repository Url</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Git#getRepositoryUrl()
	 * @see #getGit()
	 * @generated
	 */
	EAttribute getGit_RepositoryUrl();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Git#getBranch <em>Branch</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Branch</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Git#getBranch()
	 * @see #getGit()
	 * @generated
	 */
	EAttribute getGit_Branch();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Git#getUsername <em>Username</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Username</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Git#getUsername()
	 * @see #getGit()
	 * @generated
	 */
	EAttribute getGit_Username();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Git#getLocalPath <em>Local Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Local Path</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Git#getLocalPath()
	 * @see #getGit()
	 * @generated
	 */
	EAttribute getGit_LocalPath();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Maven <em>Maven</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Maven</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Maven
	 * @generated
	 */
	EClass getMaven();

	/**
	 * Returns the meta object for the attribute list '{@link eu.kalafatic.evolution.model.orchestration.Maven#getGoals <em>Goals</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Goals</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Maven#getGoals()
	 * @see #getMaven()
	 * @generated
	 */
	EAttribute getMaven_Goals();

	/**
	 * Returns the meta object for the attribute list '{@link eu.kalafatic.evolution.model.orchestration.Maven#getProfiles <em>Profiles</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Profiles</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Maven#getProfiles()
	 * @see #getMaven()
	 * @generated
	 */
	EAttribute getMaven_Profiles();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.LLM <em>LLM</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>LLM</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.LLM
	 * @generated
	 */
	EClass getLLM();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.LLM#getModel <em>Model</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Model</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.LLM#getModel()
	 * @see #getLLM()
	 * @generated
	 */
	EAttribute getLLM_Model();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.LLM#getTemperature <em>Temperature</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Temperature</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.LLM#getTemperature()
	 * @see #getLLM()
	 * @generated
	 */
	EAttribute getLLM_Temperature();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Compiler <em>Compiler</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Compiler</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Compiler
	 * @generated
	 */
	EClass getCompiler();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getSourceVersion <em>Source Version</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Source Version</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Compiler#getSourceVersion()
	 * @see #getCompiler()
	 * @generated
	 */
	EAttribute getCompiler_SourceVersion();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getTargetVersion <em>Target Version</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Target Version</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Compiler#getTargetVersion()
	 * @see #getCompiler()
	 * @generated
	 */
	EAttribute getCompiler_TargetVersion();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Command <em>Command</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Command</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Command
	 * @generated
	 */
	EClass getCommand();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Command#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Command#getName()
	 * @see #getCommand()
	 * @generated
	 */
	EAttribute getCommand_Name();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Command#getStatus <em>Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Command#getStatus()
	 * @see #getCommand()
	 * @generated
	 */
	EAttribute getCommand_Status();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Ollama <em>Ollama</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Ollama</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Ollama
	 * @generated
	 */
	EClass getOllama();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Ollama#getUrl <em>Url</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Url</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Ollama#getUrl()
	 * @see #getOllama()
	 * @generated
	 */
	EAttribute getOllama_Url();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Ollama#getModel <em>Model</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Model</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Ollama#getModel()
	 * @see #getOllama()
	 * @generated
	 */
	EAttribute getOllama_Model();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Ollama#getPath <em>Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Path</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Ollama#getPath()
	 * @see #getOllama()
	 * @generated
	 */
	EAttribute getOllama_Path();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.AiChat <em>Ai Chat</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Ai Chat</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.AiChat
	 * @generated
	 */
	EClass getAiChat();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.AiChat#getUrl <em>Url</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Url</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.AiChat#getUrl()
	 * @see #getAiChat()
	 * @generated
	 */
	EAttribute getAiChat_Url();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.AiChat#getToken <em>Token</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Token</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.AiChat#getToken()
	 * @see #getAiChat()
	 * @generated
	 */
	EAttribute getAiChat_Token();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.AiChat#getPrompt <em>Prompt</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Prompt</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.AiChat#getPrompt()
	 * @see #getAiChat()
	 * @generated
	 */
	EAttribute getAiChat_Prompt();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.NeuronAI <em>Neuron AI</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Neuron AI</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.NeuronAI
	 * @generated
	 */
	EClass getNeuronAI();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.NeuronAI#getUrl <em>Url</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Url</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.NeuronAI#getUrl()
	 * @see #getNeuronAI()
	 * @generated
	 */
	EAttribute getNeuronAI_Url();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.NeuronAI#getModel <em>Model</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Model</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.NeuronAI#getModel()
	 * @see #getNeuronAI()
	 * @generated
	 */
	EAttribute getNeuronAI_Model();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.EvoProject <em>Evo Project</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Evo Project</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.EvoProject
	 * @generated
	 */
	EClass getEvoProject();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.EvoProject#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.EvoProject#getName()
	 * @see #getEvoProject()
	 * @generated
	 */
	EAttribute getEvoProject_Name();

	/**
	 * Returns the meta object for the containment reference list '{@link eu.kalafatic.evolution.model.orchestration.EvoProject#getOrchestrations <em>Orchestrations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Orchestrations</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.EvoProject#getOrchestrations()
	 * @see #getEvoProject()
	 * @generated
	 */
	EReference getEvoProject_Orchestrations();

	/**
	 * Returns the meta object for enum '{@link eu.kalafatic.evolution.model.orchestration.TaskStatus <em>Task Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Task Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.TaskStatus
	 * @generated
	 */
	EEnum getTaskStatus();

	/**
	 * Returns the meta object for enum '{@link eu.kalafatic.evolution.model.orchestration.CommandStatus <em>Command Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Command Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.CommandStatus
	 * @generated
	 */
	EEnum getCommandStatus();

	/**
	 * Returns the meta object for enum '{@link eu.kalafatic.evolution.model.orchestration.ExecutionMode <em>Execution Mode</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Execution Mode</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.ExecutionMode
	 * @generated
	 */
	EEnum getExecutionMode();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Rule <em>Rule</em>}'.
	 * @generated
	 */
	EClass getRule();
	EAttribute getRule_Name();
	EAttribute getRule_Description();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.AccessRule <em>Access Rule</em>}'.
	 * @generated
	 */
	EClass getAccessRule();
	EAttribute getAccessRule_AllowedPaths();
	EAttribute getAccessRule_DeniedPaths();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.NetworkRule <em>Network Rule</em>}'.
	 * @generated
	 */
	EClass getNetworkRule();
	EAttribute getNetworkRule_AllowedDomains();
	EAttribute getNetworkRule_AllowAll();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.MemoryRule <em>Memory Rule</em>}'.
	 * @generated
	 */
	EClass getMemoryRule();
	EAttribute getMemoryRule_StorageLimit();
	EAttribute getMemoryRule_RetentionPeriod();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.SecretRule <em>Secret Rule</em>}'.
	 * @generated
	 */
	EClass getSecretRule();
	EAttribute getSecretRule_AllowedSecrets();

	/**
	 * Returns the factory that creates the instances of the model.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the factory that creates the instances of the model.
	 * @generated
	 */
	OrchestrationFactory getOrchestrationFactory();

	/**
	 * <!-- begin-user-doc -->
	 * Defines literals for the meta objects that represent
	 * <ul>
	 *   <li>each class,</li>
	 *   <li>each feature of each class,</li>
	 *   <li>each operation of each class,</li>
	 *   <li>each enum,</li>
	 *   <li>and each data type</li>
	 * </ul>
	 * <!-- end-user-doc -->
	 * @generated
	 */
	interface Literals {
		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.TaskImpl <em>Task</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.TaskImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getTask()
		 * @generated
		 */
		EClass TASK = eINSTANCE.getTask();

		/**
		 * The meta object literal for the '<em><b>Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__ID = eINSTANCE.getTask_Id();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__NAME = eINSTANCE.getTask_Name();

		/**
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__TYPE = eINSTANCE.getTask_Type();

		/**
		 * The meta object literal for the '<em><b>Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__STATUS = eINSTANCE.getTask_Status();

		/**
		 * The meta object literal for the '<em><b>Next</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TASK__NEXT = eINSTANCE.getTask_Next();

		/**
		 * The meta object literal for the '<em><b>Sub Tasks</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference TASK__SUB_TASKS = eINSTANCE.getTask_SubTasks();

		/**
		 * The meta object literal for the '<em><b>Response</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__RESPONSE = eINSTANCE.getTask_Response();

		/**
		 * The meta object literal for the '<em><b>Feedback</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__FEEDBACK = eINSTANCE.getTask_Feedback();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.AgentImpl <em>Agent</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.AgentImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getAgent()
		 * @generated
		 */
		EClass AGENT = eINSTANCE.getAgent();

		/**
		 * The meta object literal for the '<em><b>Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute AGENT__ID = eINSTANCE.getAgent_Id();

		/**
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute AGENT__TYPE = eINSTANCE.getAgent_Type();

		/**
		 * The meta object literal for the '<em><b>Tasks</b></em>' reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference AGENT__TASKS = eINSTANCE.getAgent_Tasks();

		/**
		 * The meta object literal for the '<em><b>Execution Mode</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute AGENT__EXECUTION_MODE = eINSTANCE.getAgent_ExecutionMode();

		/**
		 * @generated
		 */
		EReference AGENT__RULES = eINSTANCE.getAgent_Rules();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl <em>Orchestrator</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getOrchestrator()
		 * @generated
		 */
		EClass ORCHESTRATOR = eINSTANCE.getOrchestrator();

		/**
		 * The meta object literal for the '<em><b>Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ORCHESTRATOR__ID = eINSTANCE.getOrchestrator_Id();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ORCHESTRATOR__NAME = eINSTANCE.getOrchestrator_Name();

		/**
		 * The meta object literal for the '<em><b>Agents</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__AGENTS = eINSTANCE.getOrchestrator_Agents();

		/**
		 * The meta object literal for the '<em><b>Tasks</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__TASKS = eINSTANCE.getOrchestrator_Tasks();

		/**
		 * The meta object literal for the '<em><b>Git</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__GIT = eINSTANCE.getOrchestrator_Git();

		/**
		 * The meta object literal for the '<em><b>Maven</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__MAVEN = eINSTANCE.getOrchestrator_Maven();

		/**
		 * The meta object literal for the '<em><b>Llm</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__LLM = eINSTANCE.getOrchestrator_Llm();

		/**
		 * The meta object literal for the '<em><b>Compiler</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__COMPILER = eINSTANCE.getOrchestrator_Compiler();

		/**
		 * The meta object literal for the '<em><b>Ollama</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__OLLAMA = eINSTANCE.getOrchestrator_Ollama();

		/**
		 * The meta object literal for the '<em><b>Ai Chat</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__AI_CHAT = eINSTANCE.getOrchestrator_AiChat();

		/**
		 * The meta object literal for the '<em><b>Neuron AI</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__NEURON_AI = eINSTANCE.getOrchestrator_NeuronAI();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.GitImpl <em>Git</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.GitImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getGit()
		 * @generated
		 */
		EClass GIT = eINSTANCE.getGit();

		/**
		 * The meta object literal for the '<em><b>Repository Url</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute GIT__REPOSITORY_URL = eINSTANCE.getGit_RepositoryUrl();

		/**
		 * The meta object literal for the '<em><b>Branch</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute GIT__BRANCH = eINSTANCE.getGit_Branch();

		/**
		 * The meta object literal for the '<em><b>Username</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute GIT__USERNAME = eINSTANCE.getGit_Username();

		/**
		 * The meta object literal for the '<em><b>Local Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute GIT__LOCAL_PATH = eINSTANCE.getGit_LocalPath();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.MavenImpl <em>Maven</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.MavenImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getMaven()
		 * @generated
		 */
		EClass MAVEN = eINSTANCE.getMaven();

		/**
		 * The meta object literal for the '<em><b>Goals</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MAVEN__GOALS = eINSTANCE.getMaven_Goals();

		/**
		 * The meta object literal for the '<em><b>Profiles</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MAVEN__PROFILES = eINSTANCE.getMaven_Profiles();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.LLMImpl <em>LLM</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.LLMImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getLLM()
		 * @generated
		 */
		EClass LLM = eINSTANCE.getLLM();

		/**
		 * The meta object literal for the '<em><b>Model</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LLM__MODEL = eINSTANCE.getLLM_Model();

		/**
		 * The meta object literal for the '<em><b>Temperature</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute LLM__TEMPERATURE = eINSTANCE.getLLM_Temperature();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.CompilerImpl <em>Compiler</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.CompilerImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getCompiler()
		 * @generated
		 */
		EClass COMPILER = eINSTANCE.getCompiler();

		/**
		 * The meta object literal for the '<em><b>Source Version</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPILER__SOURCE_VERSION = eINSTANCE.getCompiler_SourceVersion();

		/**
		 * The meta object literal for the '<em><b>Target Version</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPILER__TARGET_VERSION = eINSTANCE.getCompiler_TargetVersion();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.CommandImpl <em>Command</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.CommandImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getCommand()
		 * @generated
		 */
		EClass COMMAND = eINSTANCE.getCommand();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMMAND__NAME = eINSTANCE.getCommand_Name();

		/**
		 * The meta object literal for the '<em><b>Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMMAND__STATUS = eINSTANCE.getCommand_Status();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.OllamaImpl <em>Ollama</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OllamaImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getOllama()
		 * @generated
		 */
		EClass OLLAMA = eINSTANCE.getOllama();

		/**
		 * The meta object literal for the '<em><b>Url</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute OLLAMA__URL = eINSTANCE.getOllama_Url();

		/**
		 * The meta object literal for the '<em><b>Model</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute OLLAMA__MODEL = eINSTANCE.getOllama_Model();

		/**
		 * The meta object literal for the '<em><b>Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute OLLAMA__PATH = eINSTANCE.getOllama_Path();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.AiChatImpl <em>Ai Chat</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.AiChatImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getAiChat()
		 * @generated
		 */
		EClass AI_CHAT = eINSTANCE.getAiChat();

		/**
		 * The meta object literal for the '<em><b>Url</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute AI_CHAT__URL = eINSTANCE.getAiChat_Url();

		/**
		 * The meta object literal for the '<em><b>Token</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute AI_CHAT__TOKEN = eINSTANCE.getAiChat_Token();

		/**
		 * The meta object literal for the '<em><b>Prompt</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute AI_CHAT__PROMPT = eINSTANCE.getAiChat_Prompt();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.NeuronAIImpl <em>Neuron AI</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.NeuronAIImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getNeuronAI()
		 * @generated
		 */
		EClass NEURON_AI = eINSTANCE.getNeuronAI();

		/**
		 * The meta object literal for the '<em><b>Url</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute NEURON_AI__URL = eINSTANCE.getNeuronAI_Url();

		/**
		 * The meta object literal for the '<em><b>Model</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute NEURON_AI__MODEL = eINSTANCE.getNeuronAI_Model();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.EvoProjectImpl <em>Evo Project</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.EvoProjectImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getEvoProject()
		 * @generated
		 */
		EClass EVO_PROJECT = eINSTANCE.getEvoProject();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EVO_PROJECT__NAME = eINSTANCE.getEvoProject_Name();

		/**
		 * The meta object literal for the '<em><b>Orchestrations</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference EVO_PROJECT__ORCHESTRATIONS = eINSTANCE.getEvoProject_Orchestrations();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.TaskStatus <em>Task Status</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.TaskStatus
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getTaskStatus()
		 * @generated
		 */
		EEnum TASK_STATUS = eINSTANCE.getTaskStatus();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.CommandStatus <em>Command Status</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.CommandStatus
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getCommandStatus()
		 * @generated
		 */
		EEnum COMMAND_STATUS = eINSTANCE.getCommandStatus();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.ExecutionMode <em>Execution Mode</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.ExecutionMode
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getExecutionMode()
		 * @generated
		 */
		EEnum EXECUTION_MODE = eINSTANCE.getExecutionMode();

		/**
		 * @generated
		 */
		EClass RULE = eINSTANCE.getRule();
		EAttribute RULE__NAME = eINSTANCE.getRule_Name();
		EAttribute RULE__DESCRIPTION = eINSTANCE.getRule_Description();

		EClass ACCESS_RULE = eINSTANCE.getAccessRule();
		EAttribute ACCESS_RULE__ALLOWED_PATHS = eINSTANCE.getAccessRule_AllowedPaths();
		EAttribute ACCESS_RULE__DENIED_PATHS = eINSTANCE.getAccessRule_DeniedPaths();

		EClass NETWORK_RULE = eINSTANCE.getNetworkRule();
		EAttribute NETWORK_RULE__ALLOWED_DOMAINS = eINSTANCE.getNetworkRule_AllowedDomains();
		EAttribute NETWORK_RULE__ALLOW_ALL = eINSTANCE.getNetworkRule_AllowAll();

		EClass MEMORY_RULE = eINSTANCE.getMemoryRule();
		EAttribute MEMORY_RULE__STORAGE_LIMIT = eINSTANCE.getMemoryRule_StorageLimit();
		EAttribute MEMORY_RULE__RETENTION_PERIOD = eINSTANCE.getMemoryRule_RetentionPeriod();

		EClass SECRET_RULE = eINSTANCE.getSecretRule();
		EAttribute SECRET_RULE__ALLOWED_SECRETS = eINSTANCE.getSecretRule_AllowedSecrets();
	}

} //OrchestrationPackage
