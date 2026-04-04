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
	String eNS_URI = "http://eu.kalafatic.evolution/orchestration";

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
	 * The feature id for the '<em><b>Approval Required</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__APPROVAL_REQUIRED = 8;

	/**
	 * The feature id for the '<em><b>Loop To Task Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__LOOP_TO_TASK_ID = 9;

	/**
	 * The feature id for the '<em><b>Priority</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__PRIORITY = 10;

	/**
	 * The feature id for the '<em><b>Result Summary</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__RESULT_SUMMARY = 11;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__DESCRIPTION = 12;

	/**
	 * The feature id for the '<em><b>Rating</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__RATING = 13;

	/**
	 * The feature id for the '<em><b>Likes</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__LIKES = 14;

	/**
	 * The feature id for the '<em><b>Rationale</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__RATIONALE = 15;

	/**
	 * The feature id for the '<em><b>Scheduled Time</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__SCHEDULED_TIME = 16;

	/**
	 * The feature id for the '<em><b>Selected</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__SELECTED = 17;

	/**
	 * The number of structural features of the '<em>Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK_FEATURE_COUNT = 18;

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
	 * The feature id for the '<em><b>Remote Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__REMOTE_MODEL = 11;

	/**
	 * The feature id for the '<em><b>Ai Mode</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__AI_MODE = 12;

	/**
	 * The feature id for the '<em><b>Mcp Server Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__MCP_SERVER_URL = 13;

	/**
	 * The feature id for the '<em><b>Open Ai Token</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__OPEN_AI_TOKEN = 14;

	/**
	 * The feature id for the '<em><b>Open Ai Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__OPEN_AI_MODEL = 15;

	/**
	 * The feature id for the '<em><b>Local Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__LOCAL_MODEL = 16;

	/**
	 * The feature id for the '<em><b>Hybrid Model</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__HYBRID_MODEL = 17;

	/**
	 * The feature id for the '<em><b>Offline Mode</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__OFFLINE_MODE = 18;

	/**
	 * The feature id for the '<em><b>Self Dev Session</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__SELF_DEV_SESSION = 19;

	/**
	 * The feature id for the '<em><b>Database</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__DATABASE = 20;

	/**
	 * The feature id for the '<em><b>File Config</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__FILE_CONFIG = 21;
	int ORCHESTRATOR__SHARED_MEMORY = 22;

	/**
	 * The feature id for the '<em><b>Eclipse</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__ECLIPSE = 23;

	/**
	 * The feature id for the '<em><b>Tests</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__TESTS = 24;

	/**
	 * The number of structural features of the '<em>Orchestrator</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR_FEATURE_COUNT = 25;

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
	 * The feature id for the '<em><b>Test Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GIT__TEST_STATUS = 4;

	/**
	 * The number of structural features of the '<em>Git</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int GIT_FEATURE_COUNT = 5;

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
	 * The feature id for the '<em><b>Test Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAVEN__TEST_STATUS = 2;

	/**
	 * The number of structural features of the '<em>Maven</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MAVEN_FEATURE_COUNT = 3;

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
	 * The feature id for the '<em><b>CPath</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPILER__CPATH = 2;

	/**
	 * The feature id for the '<em><b>Cpp Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPILER__CPP_PATH = 3;

	/**
	 * The feature id for the '<em><b>Make Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPILER__MAKE_PATH = 4;

	/**
	 * The feature id for the '<em><b>Cmake Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPILER__CMAKE_PATH = 5;

	/**
	 * The feature id for the '<em><b>Test Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPILER__TEST_STATUS = 6;

	/**
	 * The number of structural features of the '<em>Compiler</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int COMPILER_FEATURE_COUNT = 7;

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
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TEST__TYPE = 2;

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
	 * The feature id for the '<em><b>Proxy Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AI_CHAT__PROXY_URL = 3;

	/**
	 * The number of structural features of the '<em>Ai Chat</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AI_CHAT_FEATURE_COUNT = 4;

	/**
	 * The number of operations of the '<em>Ai Chat</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AI_CHAT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.NeuronAIImpl <em>Neuron AI</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.NeuronAIImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getNeuronAI()
	 * @generated
	 */
	int NEURON_AI = 10;

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
	 * The feature id for the '<em><b>Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NEURON_AI__TYPE = 2;

	/**
	 * The feature id for the '<em><b>Training Data</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NEURON_AI__TRAINING_DATA = 3;

	/**
	 * The number of structural features of the '<em>Neuron AI</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NEURON_AI_FEATURE_COUNT = 4;

	/**
	 * The number of operations of the '<em>Neuron AI</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NEURON_AI_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.EvoProjectImpl <em>Evo Project</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.EvoProjectImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getEvoProject()
	 * @generated
	 */
	int EVO_PROJECT = 11;

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
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.RuleImpl <em>Rule</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.RuleImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getRule()
	 * @generated
	 */
	int RULE = 12;

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
	 * The number of operations of the '<em>Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int RULE_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.AccessRuleImpl <em>Access Rule</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.AccessRuleImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getAccessRule()
	 * @generated
	 */
	int ACCESS_RULE = 13;

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
	 * The number of operations of the '<em>Access Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ACCESS_RULE_OPERATION_COUNT = RULE_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.NetworkRuleImpl <em>Network Rule</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.NetworkRuleImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getNetworkRule()
	 * @generated
	 */
	int NETWORK_RULE = 14;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NETWORK_RULE__NAME = RULE__NAME;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NETWORK_RULE__DESCRIPTION = RULE__DESCRIPTION;

	/**
	 * The feature id for the '<em><b>Allowed Domains</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NETWORK_RULE__ALLOWED_DOMAINS = RULE_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Allow All</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NETWORK_RULE__ALLOW_ALL = RULE_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Network Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NETWORK_RULE_FEATURE_COUNT = RULE_FEATURE_COUNT + 2;

	/**
	 * The number of operations of the '<em>Network Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int NETWORK_RULE_OPERATION_COUNT = RULE_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.MemoryRuleImpl <em>Memory Rule</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.MemoryRuleImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getMemoryRule()
	 * @generated
	 */
	int MEMORY_RULE = 15;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MEMORY_RULE__NAME = RULE__NAME;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MEMORY_RULE__DESCRIPTION = RULE__DESCRIPTION;

	/**
	 * The feature id for the '<em><b>Storage Limit</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MEMORY_RULE__STORAGE_LIMIT = RULE_FEATURE_COUNT + 0;

	/**
	 * The feature id for the '<em><b>Retention Period</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MEMORY_RULE__RETENTION_PERIOD = RULE_FEATURE_COUNT + 1;

	/**
	 * The number of structural features of the '<em>Memory Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MEMORY_RULE_FEATURE_COUNT = RULE_FEATURE_COUNT + 2;

	/**
	 * The number of operations of the '<em>Memory Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int MEMORY_RULE_OPERATION_COUNT = RULE_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.SecretRuleImpl <em>Secret Rule</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.SecretRuleImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getSecretRule()
	 * @generated
	 */
	int SECRET_RULE = 16;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECRET_RULE__NAME = RULE__NAME;

	/**
	 * The feature id for the '<em><b>Description</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECRET_RULE__DESCRIPTION = RULE__DESCRIPTION;

	/**
	 * The feature id for the '<em><b>Allowed Secrets</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECRET_RULE__ALLOWED_SECRETS = RULE_FEATURE_COUNT + 0;

	/**
	 * The number of structural features of the '<em>Secret Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECRET_RULE_FEATURE_COUNT = RULE_FEATURE_COUNT + 1;

	/**
	 * The number of operations of the '<em>Secret Rule</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SECRET_RULE_OPERATION_COUNT = RULE_OPERATION_COUNT + 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.SelfDevSessionImpl <em>Self Dev Session</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.SelfDevSessionImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getSelfDevSession()
	 * @generated
	 */
	int SELF_DEV_SESSION = 17;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SELF_DEV_SESSION__ID = 0;

	/**
	 * The feature id for the '<em><b>Start Time</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SELF_DEV_SESSION__START_TIME = 1;

	/**
	 * The feature id for the '<em><b>Max Iterations</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SELF_DEV_SESSION__MAX_ITERATIONS = 2;

	/**
	 * The feature id for the '<em><b>Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SELF_DEV_SESSION__STATUS = 3;

	/**
	 * The feature id for the '<em><b>Iterations</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SELF_DEV_SESSION__ITERATIONS = 4;

	/**
	 * The feature id for the '<em><b>Rationale</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SELF_DEV_SESSION__RATIONALE = 5;

	/**
	 * The number of structural features of the '<em>Self Dev Session</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SELF_DEV_SESSION_FEATURE_COUNT = 6;

	/**
	 * The number of operations of the '<em>Self Dev Session</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int SELF_DEV_SESSION_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl <em>Iteration</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.IterationImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getIteration()
	 * @generated
	 */
	int ITERATION = 20;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.EvaluationResultImpl <em>Evaluation Result</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.EvaluationResultImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getEvaluationResult()
	 * @generated
	 */
	int EVALUATION_RESULT = 22;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.TaskStatus <em>Task Status</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.TaskStatus
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getTaskStatus()
	 * @generated
	 */
	int TASK_STATUS = 23;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.CommandStatus <em>Command Status</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.CommandStatus
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getCommandStatus()
	 * @generated
	 */
	int COMMAND_STATUS = 24;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.ExecutionMode <em>Execution Mode</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.ExecutionMode
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getExecutionMode()
	 * @generated
	 */
	int EXECUTION_MODE = 25;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.NeuronType <em>Neuron Type</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.NeuronType
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getNeuronType()
	 * @generated
	 */
	int NEURON_TYPE = 26;


	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.AiMode <em>Ai Mode</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.AiMode
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getAiMode()
	 * @generated
	 */
	int AI_MODE = 27;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.SelfDevStatus <em>Self Dev Status</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevStatus
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getSelfDevStatus()
	 * @generated
	 */
	int SELF_DEV_STATUS = 28;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.IterationStatus <em>Iteration Status</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.IterationStatus
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getIterationStatus()
	 * @generated
	 */
	int ITERATION_STATUS = 29;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.SelfDevDecision <em>Self Dev Decision</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevDecision
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getSelfDevDecision()
	 * @generated
	 */
	int SELF_DEV_DECISION = 30;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.TestImpl <em>Test</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.TestImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getTest()
	 * @generated
	 */
	int TEST = 31;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TEST__ID = 0;

	/**
	 * The feature id for the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TEST__NAME = 1;

	/**
	 * The feature id for the '<em><b>Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TEST__PATH = 3;

	/**
	 * The feature id for the '<em><b>Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TEST__STATUS = 4;

	/**
	 * The number of structural features of the '<em>Test</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TEST_FEATURE_COUNT = 5;

	/**
	 * The number of operations of the '<em>Test</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TEST_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.TestStatus <em>Test Status</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.TestStatus
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getTestStatus()
	 * @generated
	 */
	int TEST_STATUS = 32;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.DatabaseImpl <em>Database</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.DatabaseImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getDatabase()
	 * @generated
	 */
	int DATABASE = 18;

	/**
	 * The feature id for the '<em><b>Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DATABASE__URL = 0;

	/**
	 * The feature id for the '<em><b>Username</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DATABASE__USERNAME = 1;

	/**
	 * The feature id for the '<em><b>Password</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DATABASE__PASSWORD = 2;

	/**
	 * The feature id for the '<em><b>Driver</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DATABASE__DRIVER = 3;

	/**
	 * The feature id for the '<em><b>Test Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DATABASE__TEST_STATUS = 4;

	/**
	 * The number of structural features of the '<em>Database</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DATABASE_FEATURE_COUNT = 5;

	/**
	 * The number of operations of the '<em>Database</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int DATABASE_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.FileConfigImpl <em>File Config</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.FileConfigImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getFileConfig()
	 * @generated
	 */
	int FILE_CONFIG = 19;

	/**
	 * The feature id for the '<em><b>Local Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILE_CONFIG__LOCAL_PATH = 0;

	/**
	 * The feature id for the '<em><b>Test Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILE_CONFIG__TEST_STATUS = 1;

	/**
	 * The number of structural features of the '<em>File Config</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILE_CONFIG_FEATURE_COUNT = 2;

	/**
	 * The number of operations of the '<em>File Config</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int FILE_CONFIG_OPERATION_COUNT = 0;

	/**
	 * The feature id for the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ITERATION__ID = 0;

	/**
	 * The feature id for the '<em><b>Branch Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ITERATION__BRANCH_NAME = 1;

	/**
	 * The feature id for the '<em><b>Tasks</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ITERATION__TASKS = 2;

	/**
	 * The feature id for the '<em><b>Evaluation Result</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ITERATION__EVALUATION_RESULT = 3;

	/**
	 * The feature id for the '<em><b>Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ITERATION__STATUS = 4;

	/**
	 * The feature id for the '<em><b>Phase</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ITERATION__PHASE = 5;

	/**
	 * The feature id for the '<em><b>Comments</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ITERATION__COMMENTS = 6;

	/**
	 * The feature id for the '<em><b>Rating</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ITERATION__RATING = 7;

	/**
	 * The feature id for the '<em><b>Rationale</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ITERATION__RATIONALE = 8;

	/**
	 * The number of structural features of the '<em>Iteration</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ITERATION_FEATURE_COUNT = 9;

	/**
	 * The number of operations of the '<em>Iteration</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ITERATION_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link eu.kalafatic.evolution.model.orchestration.impl.EclipseImpl <em>Eclipse</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see eu.kalafatic.evolution.model.orchestration.impl.EclipseImpl
	 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getEclipse()
	 * @generated
	 */
	int ECLIPSE = 21;

	/**
	 * The feature id for the '<em><b>Workspace</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ECLIPSE__WORKSPACE = 0;

	/**
	 * The feature id for the '<em><b>Installation</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ECLIPSE__INSTALLATION = 1;

	/**
	 * The feature id for the '<em><b>Target Platform</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ECLIPSE__TARGET_PLATFORM = 2;

	/**
	 * The feature id for the '<em><b>Test Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ECLIPSE__TEST_STATUS = 3;

	/**
	 * The number of structural features of the '<em>Eclipse</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ECLIPSE_FEATURE_COUNT = 4;

	/**
	 * The number of operations of the '<em>Eclipse</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ECLIPSE_OPERATION_COUNT = 0;

	/**
	 * The feature id for the '<em><b>Success</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVALUATION_RESULT__SUCCESS = 0;

	/**
	 * The feature id for the '<em><b>Test Pass Rate</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVALUATION_RESULT__TEST_PASS_RATE = 1;

	/**
	 * The feature id for the '<em><b>Coverage Change</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVALUATION_RESULT__COVERAGE_CHANGE = 2;

	/**
	 * The feature id for the '<em><b>Errors</b></em>' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVALUATION_RESULT__ERRORS = 3;

	/**
	 * The feature id for the '<em><b>Decision</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVALUATION_RESULT__DECISION = 4;

	/**
	 * The feature id for the '<em><b>User Satisfaction</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVALUATION_RESULT__USER_SATISFACTION = 5;

	/**
	 * The number of structural features of the '<em>Evaluation Result</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVALUATION_RESULT_FEATURE_COUNT = 6;

	/**
	 * The number of operations of the '<em>Evaluation Result</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int EVALUATION_RESULT_OPERATION_COUNT = 0;


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
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#isApprovalRequired <em>Approval Required</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Approval Required</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#isApprovalRequired()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_ApprovalRequired();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getLoopToTaskId <em>Loop To Task Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Loop To Task Id</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getLoopToTaskId()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_LoopToTaskId();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getPriority <em>Priority</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Priority</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getPriority()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Priority();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getResultSummary <em>Result Summary</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Result Summary</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getResultSummary()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_ResultSummary();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getDescription()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Description();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getRating <em>Rating</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Rating</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getRating()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Rating();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#isLikes <em>Likes</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Likes</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#isLikes()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Likes();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getRationale <em>Rationale</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Rationale</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getRationale()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Rationale();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#getScheduledTime <em>Scheduled Time</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Scheduled Time</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#getScheduledTime()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_ScheduledTime();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Task#isSelected <em>Selected</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Selected</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Task#isSelected()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Selected();

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
	 * Returns the meta object for the containment reference list '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getTests <em>Tests</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Tests</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getTests()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Tests();

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
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getAiMode <em>Ai Mode</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Ai Mode</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getAiMode()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_AiMode();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getMcpServerUrl <em>Mcp Server Url</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Mcp Server Url</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getMcpServerUrl()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_McpServerUrl();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getRemoteModel <em>Remote Model</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Remote Model</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getRemoteModel()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_RemoteModel();

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
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Git#getTestStatus <em>Test Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Test Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Git#getTestStatus()
	 * @see #getGit()
	 * @generated
	 */
	EAttribute getGit_TestStatus();

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
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Maven#getTestStatus <em>Test Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Test Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Maven#getTestStatus()
	 * @see #getMaven()
	 * @generated
	 */
	EAttribute getMaven_TestStatus();

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
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getCPath <em>CPath</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>CPath</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Compiler#getCPath()
	 * @see #getCompiler()
	 * @generated
	 */
	EAttribute getCompiler_CPath();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getCppPath <em>Cpp Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Cpp Path</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Compiler#getCppPath()
	 * @see #getCompiler()
	 * @generated
	 */
	EAttribute getCompiler_CppPath();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getMakePath <em>Make Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Make Path</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Compiler#getMakePath()
	 * @see #getCompiler()
	 * @generated
	 */
	EAttribute getCompiler_MakePath();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getCmakePath <em>Cmake Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Cmake Path</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Compiler#getCmakePath()
	 * @see #getCompiler()
	 * @generated
	 */
	EAttribute getCompiler_CmakePath();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getTestStatus <em>Test Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Test Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Compiler#getTestStatus()
	 * @see #getCompiler()
	 * @generated
	 */
	EAttribute getCompiler_TestStatus();

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
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.AiChat#getProxyUrl <em>Proxy Url</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Proxy Url</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.AiChat#getProxyUrl()
	 * @see #getAiChat()
	 * @generated
	 */
	EAttribute getAiChat_ProxyUrl();

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
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.NeuronAI#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.NeuronAI#getType()
	 * @see #getNeuronAI()
	 * @generated
	 */
	EAttribute getNeuronAI_Type();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.NeuronAI#getTrainingData <em>Training Data</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Training Data</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.NeuronAI#getTrainingData()
	 * @see #getNeuronAI()
	 * @generated
	 */
	EAttribute getNeuronAI_TrainingData();

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
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Rule <em>Rule</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Rule</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Rule
	 * @generated
	 */
	EClass getRule();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Rule#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Rule#getName()
	 * @see #getRule()
	 * @generated
	 */
	EAttribute getRule_Name();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Rule#getDescription <em>Description</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Description</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Rule#getDescription()
	 * @see #getRule()
	 * @generated
	 */
	EAttribute getRule_Description();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.AccessRule <em>Access Rule</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Access Rule</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.AccessRule
	 * @generated
	 */
	EClass getAccessRule();

	/**
	 * Returns the meta object for the attribute list '{@link eu.kalafatic.evolution.model.orchestration.AccessRule#getAllowedPaths <em>Allowed Paths</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Allowed Paths</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.AccessRule#getAllowedPaths()
	 * @see #getAccessRule()
	 * @generated
	 */
	EAttribute getAccessRule_AllowedPaths();

	/**
	 * Returns the meta object for the attribute list '{@link eu.kalafatic.evolution.model.orchestration.AccessRule#getDeniedPaths <em>Denied Paths</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Denied Paths</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.AccessRule#getDeniedPaths()
	 * @see #getAccessRule()
	 * @generated
	 */
	EAttribute getAccessRule_DeniedPaths();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.NetworkRule <em>Network Rule</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Network Rule</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.NetworkRule
	 * @generated
	 */
	EClass getNetworkRule();

	/**
	 * Returns the meta object for the attribute list '{@link eu.kalafatic.evolution.model.orchestration.NetworkRule#getAllowedDomains <em>Allowed Domains</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Allowed Domains</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.NetworkRule#getAllowedDomains()
	 * @see #getNetworkRule()
	 * @generated
	 */
	EAttribute getNetworkRule_AllowedDomains();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.NetworkRule#isAllowAll <em>Allow All</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Allow All</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.NetworkRule#isAllowAll()
	 * @see #getNetworkRule()
	 * @generated
	 */
	EAttribute getNetworkRule_AllowAll();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.MemoryRule <em>Memory Rule</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Memory Rule</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.MemoryRule
	 * @generated
	 */
	EClass getMemoryRule();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.MemoryRule#getStorageLimit <em>Storage Limit</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Storage Limit</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.MemoryRule#getStorageLimit()
	 * @see #getMemoryRule()
	 * @generated
	 */
	EAttribute getMemoryRule_StorageLimit();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.MemoryRule#getRetentionPeriod <em>Retention Period</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Retention Period</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.MemoryRule#getRetentionPeriod()
	 * @see #getMemoryRule()
	 * @generated
	 */
	EAttribute getMemoryRule_RetentionPeriod();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.SecretRule <em>Secret Rule</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Secret Rule</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.SecretRule
	 * @generated
	 */
	EClass getSecretRule();

	/**
	 * Returns the meta object for the attribute list '{@link eu.kalafatic.evolution.model.orchestration.SecretRule#getAllowedSecrets <em>Allowed Secrets</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Allowed Secrets</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.SecretRule#getAllowedSecrets()
	 * @see #getSecretRule()
	 * @generated
	 */
	EAttribute getSecretRule_AllowedSecrets();

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
	 * Returns the meta object for enum '{@link eu.kalafatic.evolution.model.orchestration.NeuronType <em>Neuron Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Neuron Type</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.NeuronType
	 * @generated
	 */
	EEnum getNeuronType();

	/**
	 * Returns the meta object for enum '{@link eu.kalafatic.evolution.model.orchestration.AiMode <em>Ai Mode</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Ai Mode</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.AiMode
	 * @generated
	 */
	EEnum getAiMode();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Test <em>Test</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Test</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Test
	 * @generated
	 */
	EClass getTest();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Test#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Test#getId()
	 * @see #getTest()
	 * @generated
	 */
	EAttribute getTest_Id();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Test#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Test#getName()
	 * @see #getTest()
	 * @generated
	 */
	EAttribute getTest_Name();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Test#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Test#getType()
	 * @see #getTest()
	 * @generated
	 */
	EAttribute getTest_Type();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Test#getPath <em>Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Path</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Test#getPath()
	 * @see #getTest()
	 * @generated
	 */
	EAttribute getTest_Path();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Test#getStatus <em>Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Test#getStatus()
	 * @see #getTest()
	 * @generated
	 */
	EAttribute getTest_Status();

	/**
	 * Returns the meta object for enum '{@link eu.kalafatic.evolution.model.orchestration.TestStatus <em>Test Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Test Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.TestStatus
	 * @generated
	 */
	EEnum getTestStatus();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.SelfDevSession <em>Self Dev Session</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Self Dev Session</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevSession
	 * @generated
	 */
	EClass getSelfDevSession();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.SelfDevSession#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevSession#getId()
	 * @see #getSelfDevSession()
	 * @generated
	 */
	EAttribute getSelfDevSession_Id();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.SelfDevSession#getStartTime <em>Start Time</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Start Time</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevSession#getStartTime()
	 * @see #getSelfDevSession()
	 * @generated
	 */
	EAttribute getSelfDevSession_StartTime();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.SelfDevSession#getMaxIterations <em>Max Iterations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Max Iterations</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevSession#getMaxIterations()
	 * @see #getSelfDevSession()
	 * @generated
	 */
	EAttribute getSelfDevSession_MaxIterations();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.SelfDevSession#getStatus <em>Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevSession#getStatus()
	 * @see #getSelfDevSession()
	 * @generated
	 */
	EAttribute getSelfDevSession_Status();

	/**
	 * Returns the meta object for the containment reference list '{@link eu.kalafatic.evolution.model.orchestration.SelfDevSession#getIterations <em>Iterations</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Iterations</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevSession#getIterations()
	 * @see #getSelfDevSession()
	 * @generated
	 */
	EReference getSelfDevSession_Iterations();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.SelfDevSession#getRationale <em>Rationale</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Rationale</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevSession#getRationale()
	 * @see #getSelfDevSession()
	 * @generated
	 */
	EAttribute getSelfDevSession_Rationale();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Iteration <em>Iteration</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Iteration</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Iteration
	 * @generated
	 */
	EClass getIteration();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Iteration#getId()
	 * @see #getIteration()
	 * @generated
	 */
	EAttribute getIteration_Id();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getBranchName <em>Branch Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Branch Name</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Iteration#getBranchName()
	 * @see #getIteration()
	 * @generated
	 */
	EAttribute getIteration_BranchName();

	/**
	 * Returns the meta object for the containment reference list '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getTasks <em>Tasks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Tasks</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Iteration#getTasks()
	 * @see #getIteration()
	 * @generated
	 */
	EReference getIteration_Tasks();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getEvaluationResult <em>Evaluation Result</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Evaluation Result</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Iteration#getEvaluationResult()
	 * @see #getIteration()
	 * @generated
	 */
	EReference getIteration_EvaluationResult();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getStatus <em>Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Iteration#getStatus()
	 * @see #getIteration()
	 * @generated
	 */
	EAttribute getIteration_Status();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getPhase <em>Phase</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Phase</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Iteration#getPhase()
	 * @see #getIteration()
	 * @generated
	 */
	EAttribute getIteration_Phase();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getComments <em>Comments</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Comments</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Iteration#getComments()
	 * @see #getIteration()
	 * @generated
	 */
	EAttribute getIteration_Comments();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getRating <em>Rating</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Rating</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Iteration#getRating()
	 * @see #getIteration()
	 * @generated
	 */
	EAttribute getIteration_Rating();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Iteration#getRationale <em>Rationale</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Rationale</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Iteration#getRationale()
	 * @see #getIteration()
	 * @generated
	 */
	EAttribute getIteration_Rationale();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult <em>Evaluation Result</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Evaluation Result</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.EvaluationResult
	 * @generated
	 */
	EClass getEvaluationResult();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#isSuccess <em>Success</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Success</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.EvaluationResult#isSuccess()
	 * @see #getEvaluationResult()
	 * @generated
	 */
	EAttribute getEvaluationResult_Success();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getTestPassRate <em>Test Pass Rate</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Test Pass Rate</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.EvaluationResult#getTestPassRate()
	 * @see #getEvaluationResult()
	 * @generated
	 */
	EAttribute getEvaluationResult_TestPassRate();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getCoverageChange <em>Coverage Change</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Coverage Change</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.EvaluationResult#getCoverageChange()
	 * @see #getEvaluationResult()
	 * @generated
	 */
	EAttribute getEvaluationResult_CoverageChange();

	/**
	 * Returns the meta object for the attribute list '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getErrors <em>Errors</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Errors</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.EvaluationResult#getErrors()
	 * @see #getEvaluationResult()
	 * @generated
	 */
	EAttribute getEvaluationResult_Errors();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getDecision <em>Decision</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Decision</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.EvaluationResult#getDecision()
	 * @see #getEvaluationResult()
	 * @generated
	 */
	EAttribute getEvaluationResult_Decision();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult#getUserSatisfaction <em>User Satisfaction</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>User Satisfaction</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.EvaluationResult#getUserSatisfaction()
	 * @see #getEvaluationResult()
	 * @generated
	 */
	EAttribute getEvaluationResult_UserSatisfaction();

	/**
	 * Returns the meta object for enum '{@link eu.kalafatic.evolution.model.orchestration.SelfDevStatus <em>Self Dev Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Self Dev Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevStatus
	 * @generated
	 */
	EEnum getSelfDevStatus();

	/**
	 * Returns the meta object for enum '{@link eu.kalafatic.evolution.model.orchestration.IterationStatus <em>Iteration Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Iteration Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.IterationStatus
	 * @generated
	 */
	EEnum getIterationStatus();

	/**
	 * Returns the meta object for enum '{@link eu.kalafatic.evolution.model.orchestration.SelfDevDecision <em>Self Dev Decision</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Self Dev Decision</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevDecision
	 * @generated
	 */
	EEnum getSelfDevDecision();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getDatabase <em>Database</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Database</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getDatabase()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Database();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getFileConfig <em>File Config</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>File Config</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getFileConfig()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_FileConfig();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getEclipse <em>Eclipse</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Eclipse</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getEclipse()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Eclipse();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Database <em>Database</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Database</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Database
	 * @generated
	 */
	EClass getDatabase();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Database#getUrl <em>Url</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Url</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Database#getUrl()
	 * @see #getDatabase()
	 * @generated
	 */
	EAttribute getDatabase_Url();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Database#getUsername <em>Username</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Username</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Database#getUsername()
	 * @see #getDatabase()
	 * @generated
	 */
	EAttribute getDatabase_Username();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Database#getPassword <em>Password</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Password</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Database#getPassword()
	 * @see #getDatabase()
	 * @generated
	 */
	EAttribute getDatabase_Password();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Database#getDriver <em>Driver</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Driver</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Database#getDriver()
	 * @see #getDatabase()
	 * @generated
	 */
	EAttribute getDatabase_Driver();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Database#getTestStatus <em>Test Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Test Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Database#getTestStatus()
	 * @see #getDatabase()
	 * @generated
	 */
	EAttribute getDatabase_TestStatus();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.FileConfig <em>File Config</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>File Config</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.FileConfig
	 * @generated
	 */
	EClass getFileConfig();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.FileConfig#getLocalPath <em>Local Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Local Path</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.FileConfig#getLocalPath()
	 * @see #getFileConfig()
	 * @generated
	 */
	EAttribute getFileConfig_LocalPath();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.FileConfig#getTestStatus <em>Test Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Test Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.FileConfig#getTestStatus()
	 * @see #getFileConfig()
	 * @generated
	 */
	EAttribute getFileConfig_TestStatus();

	/**
	 * Returns the meta object for class '{@link eu.kalafatic.evolution.model.orchestration.Eclipse <em>Eclipse</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Eclipse</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Eclipse
	 * @generated
	 */
	EClass getEclipse();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getWorkspace <em>Workspace</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Workspace</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Eclipse#getWorkspace()
	 * @see #getEclipse()
	 * @generated
	 */
	EAttribute getEclipse_Workspace();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getInstallation <em>Installation</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Installation</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Eclipse#getInstallation()
	 * @see #getEclipse()
	 * @generated
	 */
	EAttribute getEclipse_Installation();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getTargetPlatform <em>Target Platform</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Target Platform</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Eclipse#getTargetPlatform()
	 * @see #getEclipse()
	 * @generated
	 */
	EAttribute getEclipse_TargetPlatform();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Eclipse#getTestStatus <em>Test Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Test Status</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Eclipse#getTestStatus()
	 * @see #getEclipse()
	 * @generated
	 */
	EAttribute getEclipse_TestStatus();

	/**
	 * Returns the meta object for the containment reference '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getSelfDevSession <em>Self Dev Session</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Self Dev Session</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getSelfDevSession()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_SelfDevSession();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getSharedMemory <em>Shared Memory</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Shared Memory</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getSharedMemory()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_SharedMemory();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getOpenAiToken <em>Open Ai Token</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Open Ai Token</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getOpenAiToken()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_OpenAiToken();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getOpenAiModel <em>Open Ai Model</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Open Ai Model</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getOpenAiModel()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_OpenAiModel();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getLocalModel <em>Local Model</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Local Model</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getLocalModel()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_LocalModel();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#getHybridModel <em>Hybrid Model</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Hybrid Model</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#getHybridModel()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_HybridModel();

	/**
	 * Returns the meta object for the attribute '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator#isOfflineMode <em>Offline Mode</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Offline Mode</em>'.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator#isOfflineMode()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_OfflineMode();

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
		 * The meta object literal for the '<em><b>Tests</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__TESTS = eINSTANCE.getOrchestrator_Tests();

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
		 * The meta object literal for the '<em><b>Approval Required</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__APPROVAL_REQUIRED = eINSTANCE.getTask_ApprovalRequired();

		/**
		 * The meta object literal for the '<em><b>Loop To Task Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__LOOP_TO_TASK_ID = eINSTANCE.getTask_LoopToTaskId();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__DESCRIPTION = eINSTANCE.getTask_Description();

		/**
		 * The meta object literal for the '<em><b>Rating</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__RATING = eINSTANCE.getTask_Rating();

		/**
		 * The meta object literal for the '<em><b>Likes</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__LIKES = eINSTANCE.getTask_Likes();

		/**
		 * The meta object literal for the '<em><b>Rationale</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__RATIONALE = eINSTANCE.getTask_Rationale();

		/**
		 * The meta object literal for the '<em><b>Scheduled Time</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__SCHEDULED_TIME = eINSTANCE.getTask_ScheduledTime();

		/**
		 * The meta object literal for the '<em><b>Selected</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__SELECTED = eINSTANCE.getTask_Selected();

		/**
		 * The meta object literal for the '<em><b>Priority</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__PRIORITY = eINSTANCE.getTask_Priority();

		/**
		 * The meta object literal for the '<em><b>Result Summary</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TASK__RESULT_SUMMARY = eINSTANCE.getTask_ResultSummary();

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
		 * The meta object literal for the '<em><b>Rules</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
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
		 * The meta object literal for the '<em><b>Ai Mode</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ORCHESTRATOR__AI_MODE = eINSTANCE.getOrchestrator_AiMode();

		/**
		 * The meta object literal for the '<em><b>Mcp Server Url</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ORCHESTRATOR__MCP_SERVER_URL = eINSTANCE.getOrchestrator_McpServerUrl();

		/**
		 * The meta object literal for the '<em><b>Open Ai Token</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ORCHESTRATOR__OPEN_AI_TOKEN = eINSTANCE.getOrchestrator_OpenAiToken();

		/**
		 * The meta object literal for the '<em><b>Open Ai Model</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ORCHESTRATOR__OPEN_AI_MODEL = eINSTANCE.getOrchestrator_OpenAiModel();

		/**
		 * The meta object literal for the '<em><b>Local Model</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ORCHESTRATOR__LOCAL_MODEL = eINSTANCE.getOrchestrator_LocalModel();

		/**
		 * The meta object literal for the '<em><b>Hybrid Model</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ORCHESTRATOR__HYBRID_MODEL = eINSTANCE.getOrchestrator_HybridModel();

		/**
		 * The meta object literal for the '<em><b>Offline Mode</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ORCHESTRATOR__OFFLINE_MODE = eINSTANCE.getOrchestrator_OfflineMode();

		/**
		 * The meta object literal for the '<em><b>Remote Model</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ORCHESTRATOR__REMOTE_MODEL = eINSTANCE.getOrchestrator_RemoteModel();

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
		 * The meta object literal for the '<em><b>Test Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute GIT__TEST_STATUS = eINSTANCE.getGit_TestStatus();

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
		 * The meta object literal for the '<em><b>Test Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MAVEN__TEST_STATUS = eINSTANCE.getMaven_TestStatus();

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
		 * The meta object literal for the '<em><b>CPath</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPILER__CPATH = eINSTANCE.getCompiler_CPath();

		/**
		 * The meta object literal for the '<em><b>Cpp Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPILER__CPP_PATH = eINSTANCE.getCompiler_CppPath();

		/**
		 * The meta object literal for the '<em><b>Make Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPILER__MAKE_PATH = eINSTANCE.getCompiler_MakePath();

		/**
		 * The meta object literal for the '<em><b>Cmake Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPILER__CMAKE_PATH = eINSTANCE.getCompiler_CmakePath();

		/**
		 * The meta object literal for the '<em><b>Test Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute COMPILER__TEST_STATUS = eINSTANCE.getCompiler_TestStatus();

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
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TEST__TYPE = eINSTANCE.getTest_Type();

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
		 * The meta object literal for the '<em><b>Proxy Url</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute AI_CHAT__PROXY_URL = eINSTANCE.getAiChat_ProxyUrl();

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
		 * The meta object literal for the '<em><b>Type</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute NEURON_AI__TYPE = eINSTANCE.getNeuronAI_Type();

		/**
		 * The meta object literal for the '<em><b>Training Data</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute NEURON_AI__TRAINING_DATA = eINSTANCE.getNeuronAI_TrainingData();

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
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.RuleImpl <em>Rule</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.RuleImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getRule()
		 * @generated
		 */
		EClass RULE = eINSTANCE.getRule();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute RULE__NAME = eINSTANCE.getRule_Name();

		/**
		 * The meta object literal for the '<em><b>Description</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute RULE__DESCRIPTION = eINSTANCE.getRule_Description();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.AccessRuleImpl <em>Access Rule</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.AccessRuleImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getAccessRule()
		 * @generated
		 */
		EClass ACCESS_RULE = eINSTANCE.getAccessRule();

		/**
		 * The meta object literal for the '<em><b>Allowed Paths</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ACCESS_RULE__ALLOWED_PATHS = eINSTANCE.getAccessRule_AllowedPaths();

		/**
		 * The meta object literal for the '<em><b>Denied Paths</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ACCESS_RULE__DENIED_PATHS = eINSTANCE.getAccessRule_DeniedPaths();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.NetworkRuleImpl <em>Network Rule</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.NetworkRuleImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getNetworkRule()
		 * @generated
		 */
		EClass NETWORK_RULE = eINSTANCE.getNetworkRule();

		/**
		 * The meta object literal for the '<em><b>Allowed Domains</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute NETWORK_RULE__ALLOWED_DOMAINS = eINSTANCE.getNetworkRule_AllowedDomains();

		/**
		 * The meta object literal for the '<em><b>Allow All</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute NETWORK_RULE__ALLOW_ALL = eINSTANCE.getNetworkRule_AllowAll();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.MemoryRuleImpl <em>Memory Rule</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.MemoryRuleImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getMemoryRule()
		 * @generated
		 */
		EClass MEMORY_RULE = eINSTANCE.getMemoryRule();

		/**
		 * The meta object literal for the '<em><b>Storage Limit</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MEMORY_RULE__STORAGE_LIMIT = eINSTANCE.getMemoryRule_StorageLimit();

		/**
		 * The meta object literal for the '<em><b>Retention Period</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute MEMORY_RULE__RETENTION_PERIOD = eINSTANCE.getMemoryRule_RetentionPeriod();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.SecretRuleImpl <em>Secret Rule</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.SecretRuleImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getSecretRule()
		 * @generated
		 */
		EClass SECRET_RULE = eINSTANCE.getSecretRule();

		/**
		 * The meta object literal for the '<em><b>Allowed Secrets</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SECRET_RULE__ALLOWED_SECRETS = eINSTANCE.getSecretRule_AllowedSecrets();

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
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.NeuronType <em>Neuron Type</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.NeuronType
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getNeuronType()
		 * @generated
		 */
		EEnum NEURON_TYPE = eINSTANCE.getNeuronType();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.AiMode <em>Ai Mode</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.AiMode
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getAiMode()
		 * @generated
		 */
		EEnum AI_MODE = eINSTANCE.getAiMode();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.SelfDevSessionImpl <em>Self Dev Session</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.SelfDevSessionImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getSelfDevSession()
		 * @generated
		 */
		EClass SELF_DEV_SESSION = eINSTANCE.getSelfDevSession();

		/**
		 * The meta object literal for the '<em><b>Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SELF_DEV_SESSION__ID = eINSTANCE.getSelfDevSession_Id();

		/**
		 * The meta object literal for the '<em><b>Start Time</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SELF_DEV_SESSION__START_TIME = eINSTANCE.getSelfDevSession_StartTime();

		/**
		 * The meta object literal for the '<em><b>Max Iterations</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SELF_DEV_SESSION__MAX_ITERATIONS = eINSTANCE.getSelfDevSession_MaxIterations();

		/**
		 * The meta object literal for the '<em><b>Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SELF_DEV_SESSION__STATUS = eINSTANCE.getSelfDevSession_Status();

		/**
		 * The meta object literal for the '<em><b>Iterations</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference SELF_DEV_SESSION__ITERATIONS = eINSTANCE.getSelfDevSession_Iterations();

		/**
		 * The meta object literal for the '<em><b>Rationale</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute SELF_DEV_SESSION__RATIONALE = eINSTANCE.getSelfDevSession_Rationale();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.IterationImpl <em>Iteration</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.IterationImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getIteration()
		 * @generated
		 */
		EClass ITERATION = eINSTANCE.getIteration();

		/**
		 * The meta object literal for the '<em><b>Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ITERATION__ID = eINSTANCE.getIteration_Id();

		/**
		 * The meta object literal for the '<em><b>Branch Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ITERATION__BRANCH_NAME = eINSTANCE.getIteration_BranchName();

		/**
		 * The meta object literal for the '<em><b>Tasks</b></em>' containment reference list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ITERATION__TASKS = eINSTANCE.getIteration_Tasks();

		/**
		 * The meta object literal for the '<em><b>Evaluation Result</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ITERATION__EVALUATION_RESULT = eINSTANCE.getIteration_EvaluationResult();

		/**
		 * The meta object literal for the '<em><b>Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ITERATION__STATUS = eINSTANCE.getIteration_Status();

		/**
		 * The meta object literal for the '<em><b>Phase</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ITERATION__PHASE = eINSTANCE.getIteration_Phase();

		/**
		 * The meta object literal for the '<em><b>Comments</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ITERATION__COMMENTS = eINSTANCE.getIteration_Comments();

		/**
		 * The meta object literal for the '<em><b>Rating</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ITERATION__RATING = eINSTANCE.getIteration_Rating();

		/**
		 * The meta object literal for the '<em><b>Rationale</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ITERATION__RATIONALE = eINSTANCE.getIteration_Rationale();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.EvaluationResultImpl <em>Evaluation Result</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.EvaluationResultImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getEvaluationResult()
		 * @generated
		 */
		EClass EVALUATION_RESULT = eINSTANCE.getEvaluationResult();

		/**
		 * The meta object literal for the '<em><b>Success</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EVALUATION_RESULT__SUCCESS = eINSTANCE.getEvaluationResult_Success();

		/**
		 * The meta object literal for the '<em><b>Test Pass Rate</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EVALUATION_RESULT__TEST_PASS_RATE = eINSTANCE.getEvaluationResult_TestPassRate();

		/**
		 * The meta object literal for the '<em><b>Coverage Change</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EVALUATION_RESULT__COVERAGE_CHANGE = eINSTANCE.getEvaluationResult_CoverageChange();

		/**
		 * The meta object literal for the '<em><b>Errors</b></em>' attribute list feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EVALUATION_RESULT__ERRORS = eINSTANCE.getEvaluationResult_Errors();

		/**
		 * The meta object literal for the '<em><b>Decision</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EVALUATION_RESULT__DECISION = eINSTANCE.getEvaluationResult_Decision();

		/**
		 * The meta object literal for the '<em><b>User Satisfaction</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute EVALUATION_RESULT__USER_SATISFACTION = eINSTANCE.getEvaluationResult_UserSatisfaction();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.SelfDevStatus <em>Self Dev Status</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.SelfDevStatus
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getSelfDevStatus()
		 * @generated
		 */
		EEnum SELF_DEV_STATUS = eINSTANCE.getSelfDevStatus();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.IterationStatus <em>Iteration Status</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.IterationStatus
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getIterationStatus()
		 * @generated
		 */
		EEnum ITERATION_STATUS = eINSTANCE.getIterationStatus();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.SelfDevDecision <em>Self Dev Decision</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.SelfDevDecision
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getSelfDevDecision()
		 * @generated
		 */
		EEnum SELF_DEV_DECISION = eINSTANCE.getSelfDevDecision();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.DatabaseImpl <em>Database</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.DatabaseImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getDatabase()
		 * @generated
		 */
		EClass DATABASE = eINSTANCE.getDatabase();

		/**
		 * The meta object literal for the '<em><b>Url</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DATABASE__URL = eINSTANCE.getDatabase_Url();

		/**
		 * The meta object literal for the '<em><b>Username</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DATABASE__USERNAME = eINSTANCE.getDatabase_Username();

		/**
		 * The meta object literal for the '<em><b>Password</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DATABASE__PASSWORD = eINSTANCE.getDatabase_Password();

		/**
		 * The meta object literal for the '<em><b>Driver</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DATABASE__DRIVER = eINSTANCE.getDatabase_Driver();

		/**
		 * The meta object literal for the '<em><b>Test Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute DATABASE__TEST_STATUS = eINSTANCE.getDatabase_TestStatus();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.FileConfigImpl <em>File Config</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.FileConfigImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getFileConfig()
		 * @generated
		 */
		EClass FILE_CONFIG = eINSTANCE.getFileConfig();

		/**
		 * The meta object literal for the '<em><b>Local Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FILE_CONFIG__LOCAL_PATH = eINSTANCE.getFileConfig_LocalPath();

		/**
		 * The meta object literal for the '<em><b>Test Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute FILE_CONFIG__TEST_STATUS = eINSTANCE.getFileConfig_TestStatus();

		/**
		 * The meta object literal for the '<em><b>Database</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__DATABASE = eINSTANCE.getOrchestrator_Database();

		/**
		 * The meta object literal for the '<em><b>File Config</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__FILE_CONFIG = eINSTANCE.getOrchestrator_FileConfig();

		/**
		 * The meta object literal for the '<em><b>Eclipse</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__ECLIPSE = eINSTANCE.getOrchestrator_Eclipse();

		/**
		 * The meta object literal for the '<em><b>Self Dev Session</b></em>' containment reference feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EReference ORCHESTRATOR__SELF_DEV_SESSION = eINSTANCE.getOrchestrator_SelfDevSession();

		/**
		 * The meta object literal for the '<em><b>Shared Memory</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ORCHESTRATOR__SHARED_MEMORY = eINSTANCE.getOrchestrator_SharedMemory();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.EclipseImpl <em>Eclipse</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.EclipseImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getEclipse()
		 * @generated
		 */
		EClass ECLIPSE = eINSTANCE.getEclipse();

		/**
		 * The meta object literal for the '<em><b>Workspace</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ECLIPSE__WORKSPACE = eINSTANCE.getEclipse_Workspace();

		/**
		 * The meta object literal for the '<em><b>Installation</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ECLIPSE__INSTALLATION = eINSTANCE.getEclipse_Installation();

		/**
		 * The meta object literal for the '<em><b>Target Platform</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ECLIPSE__TARGET_PLATFORM = eINSTANCE.getEclipse_TargetPlatform();

		/**
		 * The meta object literal for the '<em><b>Test Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute ECLIPSE__TEST_STATUS = eINSTANCE.getEclipse_TestStatus();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.impl.TestImpl <em>Test</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.impl.TestImpl
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getTest()
		 * @generated
		 */
		EClass TEST = eINSTANCE.getTest();

		/**
		 * The meta object literal for the '<em><b>Id</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TEST__ID = eINSTANCE.getTest_Id();

		/**
		 * The meta object literal for the '<em><b>Name</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TEST__NAME = eINSTANCE.getTest_Name();

		/**
		 * The meta object literal for the '<em><b>Path</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TEST__PATH = eINSTANCE.getTest_Path();

		/**
		 * The meta object literal for the '<em><b>Status</b></em>' attribute feature.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @generated
		 */
		EAttribute TEST__STATUS = eINSTANCE.getTest_Status();

		/**
		 * The meta object literal for the '{@link eu.kalafatic.evolution.model.orchestration.TestStatus <em>Test Status</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see eu.kalafatic.evolution.model.orchestration.TestStatus
		 * @see eu.kalafatic.evolution.model.orchestration.impl.OrchestrationPackageImpl#getTestStatus()
		 * @generated
		 */
		EEnum TEST_STATUS = eINSTANCE.getTestStatus();

	}

} //OrchestrationPackage
