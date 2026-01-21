/**
 */
package orchestration;

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
 * @see orchestration.OrchestrationFactory
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
	OrchestrationPackage eINSTANCE = orchestration.impl.OrchestrationPackageImpl.init();

	/**
	 * The meta object id for the '{@link orchestration.impl.TaskImpl <em>Task</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see orchestration.impl.TaskImpl
	 * @see orchestration.impl.OrchestrationPackageImpl#getTask()
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
	 * The feature id for the '<em><b>Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__STATUS = 2;

	/**
	 * The feature id for the '<em><b>Next</b></em>' reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__NEXT = 3;

	/**
	 * The feature id for the '<em><b>Sub Tasks</b></em>' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__SUB_TASKS = 4;

	/**
	 * The feature id for the '<em><b>Response</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK__RESPONSE = 5;

	/**
	 * The number of structural features of the '<em>Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK_FEATURE_COUNT = 6;

	/**
	 * The number of operations of the '<em>Task</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int TASK_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link orchestration.impl.AgentImpl <em>Agent</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see orchestration.impl.AgentImpl
	 * @see orchestration.impl.OrchestrationPackageImpl#getAgent()
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
	 * The number of structural features of the '<em>Agent</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT_FEATURE_COUNT = 3;

	/**
	 * The number of operations of the '<em>Agent</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int AGENT_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link orchestration.impl.OrchestratorImpl <em>Orchestrator</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see orchestration.impl.OrchestratorImpl
	 * @see orchestration.impl.OrchestrationPackageImpl#getOrchestrator()
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
	 * The feature id for the '<em><b>Git</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__GIT = 3;

	/**
	 * The feature id for the '<em><b>Maven</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__MAVEN = 4;

	/**
	 * The feature id for the '<em><b>Llm</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__LLM = 5;

	/**
	 * The feature id for the '<em><b>Compiler</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR__COMPILER = 6;

	/**
	 * The number of structural features of the '<em>Orchestrator</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR_FEATURE_COUNT = 7;

	/**
	 * The number of operations of the '<em>Orchestrator</em>' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 * @ordered
	 */
	int ORCHESTRATOR_OPERATION_COUNT = 0;

	/**
	 * The meta object id for the '{@link orchestration.impl.GitImpl <em>Git</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see orchestration.impl.GitImpl
	 * @see orchestration.impl.OrchestrationPackageImpl#getGit()
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
	 * The meta object id for the '{@link orchestration.impl.MavenImpl <em>Maven</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see orchestration.impl.MavenImpl
	 * @see orchestration.impl.OrchestrationPackageImpl#getMaven()
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
	 * The meta object id for the '{@link orchestration.impl.LLMImpl <em>LLM</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see orchestration.impl.LLMImpl
	 * @see orchestration.impl.OrchestrationPackageImpl#getLLM()
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
	 * The meta object id for the '{@link orchestration.impl.CompilerImpl <em>Compiler</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see orchestration.impl.CompilerImpl
	 * @see orchestration.impl.OrchestrationPackageImpl#getCompiler()
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
	 * The meta object id for the '{@link orchestration.impl.CommandImpl <em>Command</em>}' class.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see orchestration.impl.CommandImpl
	 * @see orchestration.impl.OrchestrationPackageImpl#getCommand()
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
	 * The meta object id for the '{@link orchestration.TaskStatus <em>Task Status</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see orchestration.TaskStatus
	 * @see orchestration.impl.OrchestrationPackageImpl#getTaskStatus()
	 * @generated
	 */
	int TASK_STATUS = 8;

	/**
	 * The meta object id for the '{@link orchestration.CommandStatus <em>Command Status</em>}' enum.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see orchestration.CommandStatus
	 * @see orchestration.impl.OrchestrationPackageImpl#getCommandStatus()
	 * @generated
	 */
	int COMMAND_STATUS = 9;


	/**
	 * Returns the meta object for class '{@link orchestration.Task <em>Task</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Task</em>'.
	 * @see orchestration.Task
	 * @generated
	 */
	EClass getTask();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Task#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see orchestration.Task#getId()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Id();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Task#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see orchestration.Task#getName()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Name();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Task#getStatus <em>Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Status</em>'.
	 * @see orchestration.Task#getStatus()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Status();

	/**
	 * Returns the meta object for the reference list '{@link orchestration.Task#getNext <em>Next</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Next</em>'.
	 * @see orchestration.Task#getNext()
	 * @see #getTask()
	 * @generated
	 */
	EReference getTask_Next();

	/**
	 * Returns the meta object for the containment reference list '{@link orchestration.Task#getSubTasks <em>Sub Tasks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Sub Tasks</em>'.
	 * @see orchestration.Task#getSubTasks()
	 * @see #getTask()
	 * @generated
	 */
	EReference getTask_SubTasks();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Task#getResponse <em>Response</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Response</em>'.
	 * @see orchestration.Task#getResponse()
	 * @see #getTask()
	 * @generated
	 */
	EAttribute getTask_Response();

	/**
	 * Returns the meta object for class '{@link orchestration.Agent <em>Agent</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Agent</em>'.
	 * @see orchestration.Agent
	 * @generated
	 */
	EClass getAgent();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Agent#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see orchestration.Agent#getId()
	 * @see #getAgent()
	 * @generated
	 */
	EAttribute getAgent_Id();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Agent#getType <em>Type</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Type</em>'.
	 * @see orchestration.Agent#getType()
	 * @see #getAgent()
	 * @generated
	 */
	EAttribute getAgent_Type();

	/**
	 * Returns the meta object for the reference list '{@link orchestration.Agent#getTasks <em>Tasks</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the reference list '<em>Tasks</em>'.
	 * @see orchestration.Agent#getTasks()
	 * @see #getAgent()
	 * @generated
	 */
	EReference getAgent_Tasks();

	/**
	 * Returns the meta object for class '{@link orchestration.Orchestrator <em>Orchestrator</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Orchestrator</em>'.
	 * @see orchestration.Orchestrator
	 * @generated
	 */
	EClass getOrchestrator();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Orchestrator#getId <em>Id</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Id</em>'.
	 * @see orchestration.Orchestrator#getId()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_Id();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Orchestrator#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see orchestration.Orchestrator#getName()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EAttribute getOrchestrator_Name();

	/**
	 * Returns the meta object for the containment reference list '{@link orchestration.Orchestrator#getAgents <em>Agents</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference list '<em>Agents</em>'.
	 * @see orchestration.Orchestrator#getAgents()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Agents();

	/**
	 * Returns the meta object for the containment reference '{@link orchestration.Orchestrator#getGit <em>Git</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Git</em>'.
	 * @see orchestration.Orchestrator#getGit()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Git();

	/**
	 * Returns the meta object for the containment reference '{@link orchestration.Orchestrator#getMaven <em>Maven</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Maven</em>'.
	 * @see orchestration.Orchestrator#getMaven()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Maven();

	/**
	 * Returns the meta object for the containment reference '{@link orchestration.Orchestrator#getLlm <em>Llm</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Llm</em>'.
	 * @see orchestration.Orchestrator#getLlm()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Llm();

	/**
	 * Returns the meta object for the containment reference '{@link orchestration.Orchestrator#getCompiler <em>Compiler</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the containment reference '<em>Compiler</em>'.
	 * @see orchestration.Orchestrator#getCompiler()
	 * @see #getOrchestrator()
	 * @generated
	 */
	EReference getOrchestrator_Compiler();

	/**
	 * Returns the meta object for class '{@link orchestration.Git <em>Git</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Git</em>'.
	 * @see orchestration.Git
	 * @generated
	 */
	EClass getGit();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Git#getRepositoryUrl <em>Repository Url</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Repository Url</em>'.
	 * @see orchestration.Git#getRepositoryUrl()
	 * @see #getGit()
	 * @generated
	 */
	EAttribute getGit_RepositoryUrl();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Git#getBranch <em>Branch</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Branch</em>'.
	 * @see orchestration.Git#getBranch()
	 * @see #getGit()
	 * @generated
	 */
	EAttribute getGit_Branch();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Git#getUsername <em>Username</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Username</em>'.
	 * @see orchestration.Git#getUsername()
	 * @see #getGit()
	 * @generated
	 */
	EAttribute getGit_Username();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Git#getLocalPath <em>Local Path</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Local Path</em>'.
	 * @see orchestration.Git#getLocalPath()
	 * @see #getGit()
	 * @generated
	 */
	EAttribute getGit_LocalPath();

	/**
	 * Returns the meta object for class '{@link orchestration.Maven <em>Maven</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Maven</em>'.
	 * @see orchestration.Maven
	 * @generated
	 */
	EClass getMaven();

	/**
	 * Returns the meta object for the attribute list '{@link orchestration.Maven#getGoals <em>Goals</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Goals</em>'.
	 * @see orchestration.Maven#getGoals()
	 * @see #getMaven()
	 * @generated
	 */
	EAttribute getMaven_Goals();

	/**
	 * Returns the meta object for the attribute list '{@link orchestration.Maven#getProfiles <em>Profiles</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute list '<em>Profiles</em>'.
	 * @see orchestration.Maven#getProfiles()
	 * @see #getMaven()
	 * @generated
	 */
	EAttribute getMaven_Profiles();

	/**
	 * Returns the meta object for class '{@link orchestration.LLM <em>LLM</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>LLM</em>'.
	 * @see orchestration.LLM
	 * @generated
	 */
	EClass getLLM();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.LLM#getModel <em>Model</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Model</em>'.
	 * @see orchestration.LLM#getModel()
	 * @see #getLLM()
	 * @generated
	 */
	EAttribute getLLM_Model();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.LLM#getTemperature <em>Temperature</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Temperature</em>'.
	 * @see orchestration.LLM#getTemperature()
	 * @see #getLLM()
	 * @generated
	 */
	EAttribute getLLM_Temperature();

	/**
	 * Returns the meta object for class '{@link orchestration.Compiler <em>Compiler</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Compiler</em>'.
	 * @see orchestration.Compiler
	 * @generated
	 */
	EClass getCompiler();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Compiler#getSourceVersion <em>Source Version</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Source Version</em>'.
	 * @see orchestration.Compiler#getSourceVersion()
	 * @see #getCompiler()
	 * @generated
	 */
	EAttribute getCompiler_SourceVersion();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Compiler#getTargetVersion <em>Target Version</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Target Version</em>'.
	 * @see orchestration.Compiler#getTargetVersion()
	 * @see #getCompiler()
	 * @generated
	 */
	EAttribute getCompiler_TargetVersion();

	/**
	 * Returns the meta object for class '{@link orchestration.Command <em>Command</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for class '<em>Command</em>'.
	 * @see orchestration.Command
	 * @generated
	 */
	EClass getCommand();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Command#getName <em>Name</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Name</em>'.
	 * @see orchestration.Command#getName()
	 * @see #getCommand()
	 * @generated
	 */
	EAttribute getCommand_Name();

	/**
	 * Returns the meta object for the attribute '{@link orchestration.Command#getStatus <em>Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for the attribute '<em>Status</em>'.
	 * @see orchestration.Command#getStatus()
	 * @see #getCommand()
	 * @generated
	 */
	EAttribute getCommand_Status();

	/**
	 * Returns the meta object for enum '{@link orchestration.TaskStatus <em>Task Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Task Status</em>'.
	 * @see orchestration.TaskStatus
	 * @generated
	 */
	EEnum getTaskStatus();

	/**
	 * Returns the meta object for enum '{@link orchestration.CommandStatus <em>Command Status</em>}'.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the meta object for enum '<em>Command Status</em>'.
	 * @see orchestration.CommandStatus
	 * @generated
	 */
	EEnum getCommandStatus();

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
		 * The meta object literal for the '{@link orchestration.impl.TaskImpl <em>Task</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see orchestration.impl.TaskImpl
		 * @see orchestration.impl.OrchestrationPackageImpl#getTask()
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
		 * The meta object literal for the '{@link orchestration.impl.AgentImpl <em>Agent</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see orchestration.impl.AgentImpl
		 * @see orchestration.impl.OrchestrationPackageImpl#getAgent()
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
		 * The meta object literal for the '{@link orchestration.impl.OrchestratorImpl <em>Orchestrator</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see orchestration.impl.OrchestratorImpl
		 * @see orchestration.impl.OrchestrationPackageImpl#getOrchestrator()
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
		 * The meta object literal for the '{@link orchestration.impl.GitImpl <em>Git</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see orchestration.impl.GitImpl
		 * @see orchestration.impl.OrchestrationPackageImpl#getGit()
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
		 * The meta object literal for the '{@link orchestration.impl.MavenImpl <em>Maven</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see orchestration.impl.MavenImpl
		 * @see orchestration.impl.OrchestrationPackageImpl#getMaven()
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
		 * The meta object literal for the '{@link orchestration.impl.LLMImpl <em>LLM</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see orchestration.impl.LLMImpl
		 * @see orchestration.impl.OrchestrationPackageImpl#getLLM()
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
		 * The meta object literal for the '{@link orchestration.impl.CompilerImpl <em>Compiler</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see orchestration.impl.CompilerImpl
		 * @see orchestration.impl.OrchestrationPackageImpl#getCompiler()
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
		 * The meta object literal for the '{@link orchestration.impl.CommandImpl <em>Command</em>}' class.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see orchestration.impl.CommandImpl
		 * @see orchestration.impl.OrchestrationPackageImpl#getCommand()
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
		 * The meta object literal for the '{@link orchestration.TaskStatus <em>Task Status</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see orchestration.TaskStatus
		 * @see orchestration.impl.OrchestrationPackageImpl#getTaskStatus()
		 * @generated
		 */
		EEnum TASK_STATUS = eINSTANCE.getTaskStatus();

		/**
		 * The meta object literal for the '{@link orchestration.CommandStatus <em>Command Status</em>}' enum.
		 * <!-- begin-user-doc -->
		 * <!-- end-user-doc -->
		 * @see orchestration.CommandStatus
		 * @see orchestration.impl.OrchestrationPackageImpl#getCommandStatus()
		 * @generated
		 */
		EEnum COMMAND_STATUS = eINSTANCE.getCommandStatus();

	}

} //OrchestrationPackage
