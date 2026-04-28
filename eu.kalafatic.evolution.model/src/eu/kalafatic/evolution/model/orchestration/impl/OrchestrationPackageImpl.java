/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.AccessRule;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.ChangeSet;
import eu.kalafatic.evolution.model.orchestration.ChatMessage;
import eu.kalafatic.evolution.model.orchestration.ChatThread;
import eu.kalafatic.evolution.model.orchestration.Command;
import eu.kalafatic.evolution.model.orchestration.CommandStatus;
import eu.kalafatic.evolution.model.orchestration.Comment;
import eu.kalafatic.evolution.model.orchestration.Database;
import eu.kalafatic.evolution.model.orchestration.DiffHunk;
import eu.kalafatic.evolution.model.orchestration.Eclipse;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.ExecutionMode;
import eu.kalafatic.evolution.model.orchestration.FileChange;
import eu.kalafatic.evolution.model.orchestration.FileConfig;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.IterationStatus;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.MemoryRule;
import eu.kalafatic.evolution.model.orchestration.MonitoringData;
import eu.kalafatic.evolution.model.orchestration.NetworkRule;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.NeuronType;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.PromptInstructions;
import eu.kalafatic.evolution.model.orchestration.ReviewDecision;
import eu.kalafatic.evolution.model.orchestration.ReviewSession;
import eu.kalafatic.evolution.model.orchestration.Rule;
import eu.kalafatic.evolution.model.orchestration.SecretRule;
import eu.kalafatic.evolution.model.orchestration.SelfDevDecision;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.SelfDevStatus;
import eu.kalafatic.evolution.model.orchestration.ServerSession;
import eu.kalafatic.evolution.model.orchestration.ServerSettings;
import eu.kalafatic.evolution.model.orchestration.SessionType;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.model.orchestration.Test;
import eu.kalafatic.evolution.model.orchestration.TestStatus;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import org.eclipse.emf.ecore.impl.EPackageImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Package</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class OrchestrationPackageImpl extends EPackageImpl implements OrchestrationPackage {
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass taskEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Prompt() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(21);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Attachments() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(22);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass agentEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass orchestratorEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serverSettingsEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass serverSessionEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass monitoringDataEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass aiProviderEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass gitEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass mavenEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass llmEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass compilerEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass commandEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass ollamaEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass aiChatEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass neuronAIEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass evoProjectEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass ruleEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass accessRuleEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass networkRuleEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass memoryRuleEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass secretRuleEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass selfDevSessionEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass databaseEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass fileConfigEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass iterationEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass eclipseEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass evaluationResultEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass testEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass commentEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass diffHunkEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass fileChangeEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass changeSetEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass reviewSessionEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass chatThreadEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass chatMessageEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EClass promptInstructionsEClass = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum taskStatusEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum sessionTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum commandStatusEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum executionModeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum neuronTypeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum aiModeEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum selfDevStatusEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum iterationStatusEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum selfDevDecisionEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum testStatusEEnum = null;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private EEnum reviewDecisionEEnum = null;

	/**
	 * Creates an instance of the model <b>Package</b>, registered with
	 * {@link org.eclipse.emf.ecore.EPackage.Registry EPackage.Registry} by the package
	 * package URI value.
	 * <p>Note: the correct way to create the package is via the static
	 * factory method {@link #init init()}, which also performs
	 * initialization of the package, or returns the registered package,
	 * if one already exists.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see org.eclipse.emf.ecore.EPackage.Registry
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#eNS_URI
	 * @see #init()
	 * @generated
	 */
	private OrchestrationPackageImpl() {
		super(eNS_URI, OrchestrationFactory.eINSTANCE);
	}
	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private static boolean isInited = false;

	/**
	 * Creates, registers, and initializes the <b>Package</b> for this model, and for any others upon which it depends.
	 *
	 * <p>This method is used to initialize {@link OrchestrationPackage#eINSTANCE} when that field is accessed.
	 * Clients should not invoke it directly. Instead, they should simply access that field to obtain the package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #eNS_URI
	 * @see #createPackageContents()
	 * @see #initializePackageContents()
	 * @generated
	 */
	public static OrchestrationPackage init() {
		if (isInited) return (OrchestrationPackage)EPackage.Registry.INSTANCE.getEPackage(OrchestrationPackage.eNS_URI);

		// Obtain or create and register package
		Object registeredOrchestrationPackage = EPackage.Registry.INSTANCE.get(eNS_URI);
		OrchestrationPackageImpl theOrchestrationPackage = registeredOrchestrationPackage instanceof OrchestrationPackageImpl ? (OrchestrationPackageImpl)registeredOrchestrationPackage : new OrchestrationPackageImpl();

		isInited = true;

		// Create package meta-data objects
		theOrchestrationPackage.createPackageContents();

		// Initialize created meta-data
		theOrchestrationPackage.initializePackageContents();

		// Mark meta-data to indicate it can't be changed
		theOrchestrationPackage.freeze();

		// Update the registry and return the package
		EPackage.Registry.INSTANCE.put(OrchestrationPackage.eNS_URI, theOrchestrationPackage);
		return theOrchestrationPackage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Artifacts() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(20);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Goal() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(18);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Plan() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(19);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getTask() {
		return taskEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Id() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Name() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Type() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Status() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTask_Next() {
		return (EReference)taskEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getTask_SubTasks() {
		return (EReference)taskEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Response() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Feedback() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_ApprovalRequired() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_LoopToTaskId() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Priority() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_ResultSummary() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Description() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(12);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Rating() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(13);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Likes() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(14);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Rationale() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(15);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_ScheduledTime() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(16);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTask_Selected() {
		return (EAttribute)taskEClass.getEStructuralFeatures().get(17);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getAgent() {
		return agentEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAgent_Id() {
		return (EAttribute)agentEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAgent_Type() {
		return (EAttribute)agentEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getAgent_Tasks() {
		return (EReference)agentEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAgent_ExecutionMode() {
		return (EAttribute)agentEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getAgent_Rules() {
		return (EReference)agentEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getOrchestrator() {
		return orchestratorEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_Id() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_Name() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_Agents() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_Tasks() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_Tests() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_Git() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_Maven() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_Llm() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_Compiler() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_Ollama() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_AiChat() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_NeuronAI() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_RemoteModel() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(12);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_AiMode() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(13);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_McpServerUrl() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(14);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_OpenAiToken() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(15);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_OpenAiModel() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(16);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_LocalModel() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(17);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_HybridModel() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(18);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_OfflineMode() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(19);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_SelfDevSession() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(20);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_Database() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(21);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_FileConfig() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(22);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_SharedMemory() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(23);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_Eclipse() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(24);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOrchestrator_DarwinMode() {
		return (EAttribute)orchestratorEClass.getEStructuralFeatures().get(25);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_AiProviders() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(26);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_ServerSettings() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(27);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_ServerSessions() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(28);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getOrchestrator_MonitoringHistory() {
		return (EReference)orchestratorEClass.getEStructuralFeatures().get(29);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServerSettings() {
		return serverSettingsEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServerSettings_Port() {
		return (EAttribute)serverSettingsEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServerSettings_AutoStart() {
		return (EAttribute)serverSettingsEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServerSettings_GitAutomation() {
		return (EAttribute)serverSettingsEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getServerSession() {
		return serverSessionEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServerSession_Id() {
		return (EAttribute)serverSessionEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServerSession_Type() {
		return (EAttribute)serverSessionEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServerSession_StartTime() {
		return (EAttribute)serverSessionEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServerSession_LastActivity() {
		return (EAttribute)serverSessionEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getServerSession_ClientIp() {
		return (EAttribute)serverSessionEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getMonitoringData() {
		return monitoringDataEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMonitoringData_Timestamp() {
		return (EAttribute)monitoringDataEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMonitoringData_CpuUsage() {
		return (EAttribute)monitoringDataEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMonitoringData_MemoryUsage() {
		return (EAttribute)monitoringDataEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMonitoringData_TotalMemory() {
		return (EAttribute)monitoringDataEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getAIProvider() {
		return aiProviderEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_Name() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_Url() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_ApiKey() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_Format() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_Local() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_DefaultModel() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_ApiKeyEncrypted() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_UseEnvVar() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_EnvVarName() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_State() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(9);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_StateDescription() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(10);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_Rating() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(11);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_RatingAnalyze() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(12);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_RatingChat() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(13);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAIProvider_RatingProgramming() {
		return (EAttribute)aiProviderEClass.getEStructuralFeatures().get(14);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getGit() {
		return gitEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getGit_RepositoryUrl() {
		return (EAttribute)gitEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getGit_Branch() {
		return (EAttribute)gitEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getGit_Username() {
		return (EAttribute)gitEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getGit_LocalPath() {
		return (EAttribute)gitEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getGit_TestStatus() {
		return (EAttribute)gitEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getMaven() {
		return mavenEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMaven_Goals() {
		return (EAttribute)mavenEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMaven_Profiles() {
		return (EAttribute)mavenEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMaven_TestStatus() {
		return (EAttribute)mavenEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getLLM() {
		return llmEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getLLM_Model() {
		return (EAttribute)llmEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getLLM_Temperature() {
		return (EAttribute)llmEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getCompiler() {
		return compilerEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getCompiler_SourceVersion() {
		return (EAttribute)compilerEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getCompiler_TargetVersion() {
		return (EAttribute)compilerEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getCompiler_CPath() {
		return (EAttribute)compilerEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getCompiler_CppPath() {
		return (EAttribute)compilerEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getCompiler_MakePath() {
		return (EAttribute)compilerEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getCompiler_CmakePath() {
		return (EAttribute)compilerEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getCompiler_TestStatus() {
		return (EAttribute)compilerEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getCommand() {
		return commandEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getCommand_Name() {
		return (EAttribute)commandEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getCommand_Status() {
		return (EAttribute)commandEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getOllama() {
		return ollamaEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOllama_Url() {
		return (EAttribute)ollamaEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOllama_Model() {
		return (EAttribute)ollamaEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getOllama_Path() {
		return (EAttribute)ollamaEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getAiChat() {
		return aiChatEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAiChat_Url() {
		return (EAttribute)aiChatEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAiChat_Token() {
		return (EAttribute)aiChatEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAiChat_Prompt() {
		return (EAttribute)aiChatEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAiChat_ProxyUrl() {
		return (EAttribute)aiChatEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getAiChat_Threads() {
		return (EReference)aiChatEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getAiChat_PromptInstructions() {
		return (EReference)aiChatEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getNeuronAI() {
		return neuronAIEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getNeuronAI_Url() {
		return (EAttribute)neuronAIEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getNeuronAI_Model() {
		return (EAttribute)neuronAIEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getNeuronAI_Type() {
		return (EAttribute)neuronAIEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getNeuronAI_TrainingData() {
		return (EAttribute)neuronAIEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getEvoProject() {
		return evoProjectEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEvoProject_Name() {
		return (EAttribute)evoProjectEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getEvoProject_Orchestrations() {
		return (EReference)evoProjectEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getRule() {
		return ruleEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRule_Name() {
		return (EAttribute)ruleEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getRule_Description() {
		return (EAttribute)ruleEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getAccessRule() {
		return accessRuleEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAccessRule_AllowedPaths() {
		return (EAttribute)accessRuleEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getAccessRule_DeniedPaths() {
		return (EAttribute)accessRuleEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getNetworkRule() {
		return networkRuleEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getNetworkRule_AllowedDomains() {
		return (EAttribute)networkRuleEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getNetworkRule_AllowAll() {
		return (EAttribute)networkRuleEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getMemoryRule() {
		return memoryRuleEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMemoryRule_StorageLimit() {
		return (EAttribute)memoryRuleEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getMemoryRule_RetentionPeriod() {
		return (EAttribute)memoryRuleEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getSecretRule() {
		return secretRuleEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getSecretRule_AllowedSecrets() {
		return (EAttribute)secretRuleEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getSelfDevSession() {
		return selfDevSessionEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getSelfDevSession_Id() {
		return (EAttribute)selfDevSessionEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getSelfDevSession_StartTime() {
		return (EAttribute)selfDevSessionEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getSelfDevSession_MaxIterations() {
		return (EAttribute)selfDevSessionEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getSelfDevSession_Status() {
		return (EAttribute)selfDevSessionEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getSelfDevSession_Iterations() {
		return (EReference)selfDevSessionEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getSelfDevSession_Rationale() {
		return (EAttribute)selfDevSessionEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getSelfDevSession_InitialRequest() {
		return (EAttribute)selfDevSessionEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getDatabase() {
		return databaseEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDatabase_Url() {
		return (EAttribute)databaseEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDatabase_Username() {
		return (EAttribute)databaseEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDatabase_Password() {
		return (EAttribute)databaseEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDatabase_Driver() {
		return (EAttribute)databaseEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDatabase_TestStatus() {
		return (EAttribute)databaseEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getFileConfig() {
		return fileConfigEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFileConfig_LocalPath() {
		return (EAttribute)fileConfigEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFileConfig_TestStatus() {
		return (EAttribute)fileConfigEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getIteration() {
		return iterationEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getIteration_Id() {
		return (EAttribute)iterationEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getIteration_BranchName() {
		return (EAttribute)iterationEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getIteration_Tasks() {
		return (EReference)iterationEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getIteration_EvaluationResult() {
		return (EReference)iterationEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getIteration_Status() {
		return (EAttribute)iterationEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getIteration_Phase() {
		return (EAttribute)iterationEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getIteration_Comments() {
		return (EAttribute)iterationEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getIteration_Rating() {
		return (EAttribute)iterationEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getIteration_Rationale() {
		return (EAttribute)iterationEClass.getEStructuralFeatures().get(8);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getEclipse() {
		return eclipseEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEclipse_Workspace() {
		return (EAttribute)eclipseEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEclipse_Installation() {
		return (EAttribute)eclipseEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEclipse_TargetPlatform() {
		return (EAttribute)eclipseEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEclipse_TestStatus() {
		return (EAttribute)eclipseEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getEvaluationResult() {
		return evaluationResultEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEvaluationResult_Success() {
		return (EAttribute)evaluationResultEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEvaluationResult_TestPassRate() {
		return (EAttribute)evaluationResultEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEvaluationResult_CoverageChange() {
		return (EAttribute)evaluationResultEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEvaluationResult_Errors() {
		return (EAttribute)evaluationResultEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEvaluationResult_Decision() {
		return (EAttribute)evaluationResultEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getEvaluationResult_UserSatisfaction() {
		return (EAttribute)evaluationResultEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getTest() {
		return testEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTest_Id() {
		return (EAttribute)testEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTest_Name() {
		return (EAttribute)testEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTest_Type() {
		return (EAttribute)testEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTest_Path() {
		return (EAttribute)testEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTest_Status() {
		return (EAttribute)testEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getTest_Selected() {
		return (EAttribute)testEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getComment() {
		return commentEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComment_Id() {
		return (EAttribute)commentEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComment_FilePath() {
		return (EAttribute)commentEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComment_StartLine() {
		return (EAttribute)commentEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComment_EndLine() {
		return (EAttribute)commentEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComment_Author() {
		return (EAttribute)commentEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComment_Content() {
		return (EAttribute)commentEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComment_Timestamp() {
		return (EAttribute)commentEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getComment_Resolved() {
		return (EAttribute)commentEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getDiffHunk() {
		return diffHunkEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDiffHunk_Header() {
		return (EAttribute)diffHunkEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getDiffHunk_Lines() {
		return (EAttribute)diffHunkEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getFileChange() {
		return fileChangeEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFileChange_FilePath() {
		return (EAttribute)fileChangeEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getFileChange_Status() {
		return (EAttribute)fileChangeEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getFileChange_Hunks() {
		return (EReference)fileChangeEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getChangeSet() {
		return changeSetEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getChangeSet_CommitId() {
		return (EAttribute)changeSetEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getChangeSet_Files() {
		return (EReference)changeSetEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getReviewSession() {
		return reviewSessionEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getReviewSession_Id() {
		return (EAttribute)reviewSessionEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getReviewSession_ChangeSet() {
		return (EReference)reviewSessionEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getReviewSession_Comments() {
		return (EReference)reviewSessionEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getReviewSession_Decision() {
		return (EAttribute)reviewSessionEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getChatThread() {
		return chatThreadEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getChatThread_Id() {
		return (EAttribute)chatThreadEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EReference getChatThread_Messages() {
		return (EReference)chatThreadEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getChatMessage() {
		return chatMessageEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getChatMessage_Index() {
		return (EAttribute)chatMessageEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getChatMessage_Sender() {
		return (EAttribute)chatMessageEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getChatMessage_Text() {
		return (EAttribute)chatMessageEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getChatMessage_Color() {
		return (EAttribute)chatMessageEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getChatMessage_IsBold() {
		return (EAttribute)chatMessageEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getChatMessage_IsItalic() {
		return (EAttribute)chatMessageEClass.getEStructuralFeatures().get(5);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getChatMessage_AgentType() {
		return (EAttribute)chatMessageEClass.getEStructuralFeatures().get(6);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getChatMessage_Timestamp() {
		return (EAttribute)chatMessageEClass.getEStructuralFeatures().get(7);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EClass getPromptInstructions() {
		return promptInstructionsEClass;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getPromptInstructions_AutoApprove() {
		return (EAttribute)promptInstructionsEClass.getEStructuralFeatures().get(0);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getPromptInstructions_GitAutomation() {
		return (EAttribute)promptInstructionsEClass.getEStructuralFeatures().get(1);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getPromptInstructions_PreferredMaxIterations() {
		return (EAttribute)promptInstructionsEClass.getEStructuralFeatures().get(2);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getPromptInstructions_IterativeMode() {
		return (EAttribute)promptInstructionsEClass.getEStructuralFeatures().get(3);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EAttribute getPromptInstructions_SelfIterativeMode() {
		return (EAttribute)promptInstructionsEClass.getEStructuralFeatures().get(4);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getTaskStatus() {
		return taskStatusEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getSessionType() {
		return sessionTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getCommandStatus() {
		return commandStatusEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getExecutionMode() {
		return executionModeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getNeuronType() {
		return neuronTypeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getAiMode() {
		return aiModeEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getSelfDevStatus() {
		return selfDevStatusEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getIterationStatus() {
		return iterationStatusEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getSelfDevDecision() {
		return selfDevDecisionEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getTestStatus() {
		return testStatusEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EEnum getReviewDecision() {
		return reviewDecisionEEnum;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public OrchestrationFactory getOrchestrationFactory() {
		return (OrchestrationFactory)getEFactoryInstance();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isCreated = false;

	/**
	 * Creates the meta-model objects for the package.  This method is
	 * guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void createPackageContents() {
		if (isCreated) return;
		isCreated = true;

		// Create classes and their features
		taskEClass = createEClass(TASK);
		createEAttribute(taskEClass, TASK__ID);
		createEAttribute(taskEClass, TASK__NAME);
		createEAttribute(taskEClass, TASK__TYPE);
		createEAttribute(taskEClass, TASK__STATUS);
		createEReference(taskEClass, TASK__NEXT);
		createEReference(taskEClass, TASK__SUB_TASKS);
		createEAttribute(taskEClass, TASK__RESPONSE);
		createEAttribute(taskEClass, TASK__FEEDBACK);
		createEAttribute(taskEClass, TASK__APPROVAL_REQUIRED);
		createEAttribute(taskEClass, TASK__LOOP_TO_TASK_ID);
		createEAttribute(taskEClass, TASK__PRIORITY);
		createEAttribute(taskEClass, TASK__RESULT_SUMMARY);
		createEAttribute(taskEClass, TASK__DESCRIPTION);
		createEAttribute(taskEClass, TASK__RATING);
		createEAttribute(taskEClass, TASK__LIKES);
		createEAttribute(taskEClass, TASK__RATIONALE);
		createEAttribute(taskEClass, TASK__SCHEDULED_TIME);
		createEAttribute(taskEClass, TASK__SELECTED);
		createEAttribute(taskEClass, TASK__GOAL);
		createEAttribute(taskEClass, TASK__PLAN);
		createEAttribute(taskEClass, TASK__ARTIFACTS);
		createEAttribute(taskEClass, TASK__PROMPT);
		createEAttribute(taskEClass, TASK__ATTACHMENTS);

		agentEClass = createEClass(AGENT);
		createEAttribute(agentEClass, AGENT__ID);
		createEAttribute(agentEClass, AGENT__TYPE);
		createEReference(agentEClass, AGENT__TASKS);
		createEAttribute(agentEClass, AGENT__EXECUTION_MODE);
		createEReference(agentEClass, AGENT__RULES);

		orchestratorEClass = createEClass(ORCHESTRATOR);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__ID);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__NAME);
		createEReference(orchestratorEClass, ORCHESTRATOR__AGENTS);
		createEReference(orchestratorEClass, ORCHESTRATOR__TASKS);
		createEReference(orchestratorEClass, ORCHESTRATOR__TESTS);
		createEReference(orchestratorEClass, ORCHESTRATOR__GIT);
		createEReference(orchestratorEClass, ORCHESTRATOR__MAVEN);
		createEReference(orchestratorEClass, ORCHESTRATOR__LLM);
		createEReference(orchestratorEClass, ORCHESTRATOR__COMPILER);
		createEReference(orchestratorEClass, ORCHESTRATOR__OLLAMA);
		createEReference(orchestratorEClass, ORCHESTRATOR__AI_CHAT);
		createEReference(orchestratorEClass, ORCHESTRATOR__NEURON_AI);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__REMOTE_MODEL);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__AI_MODE);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__MCP_SERVER_URL);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__OPEN_AI_TOKEN);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__OPEN_AI_MODEL);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__LOCAL_MODEL);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__HYBRID_MODEL);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__OFFLINE_MODE);
		createEReference(orchestratorEClass, ORCHESTRATOR__SELF_DEV_SESSION);
		createEReference(orchestratorEClass, ORCHESTRATOR__DATABASE);
		createEReference(orchestratorEClass, ORCHESTRATOR__FILE_CONFIG);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__SHARED_MEMORY);
		createEReference(orchestratorEClass, ORCHESTRATOR__ECLIPSE);
		createEAttribute(orchestratorEClass, ORCHESTRATOR__DARWIN_MODE);
		createEReference(orchestratorEClass, ORCHESTRATOR__AI_PROVIDERS);
		createEReference(orchestratorEClass, ORCHESTRATOR__SERVER_SETTINGS);
		createEReference(orchestratorEClass, ORCHESTRATOR__SERVER_SESSIONS);
		createEReference(orchestratorEClass, ORCHESTRATOR__MONITORING_HISTORY);

		serverSettingsEClass = createEClass(SERVER_SETTINGS);
		createEAttribute(serverSettingsEClass, SERVER_SETTINGS__PORT);
		createEAttribute(serverSettingsEClass, SERVER_SETTINGS__AUTO_START);
		createEAttribute(serverSettingsEClass, SERVER_SETTINGS__GIT_AUTOMATION);

		serverSessionEClass = createEClass(SERVER_SESSION);
		createEAttribute(serverSessionEClass, SERVER_SESSION__ID);
		createEAttribute(serverSessionEClass, SERVER_SESSION__TYPE);
		createEAttribute(serverSessionEClass, SERVER_SESSION__START_TIME);
		createEAttribute(serverSessionEClass, SERVER_SESSION__LAST_ACTIVITY);
		createEAttribute(serverSessionEClass, SERVER_SESSION__CLIENT_IP);

		monitoringDataEClass = createEClass(MONITORING_DATA);
		createEAttribute(monitoringDataEClass, MONITORING_DATA__TIMESTAMP);
		createEAttribute(monitoringDataEClass, MONITORING_DATA__CPU_USAGE);
		createEAttribute(monitoringDataEClass, MONITORING_DATA__MEMORY_USAGE);
		createEAttribute(monitoringDataEClass, MONITORING_DATA__TOTAL_MEMORY);

		aiProviderEClass = createEClass(AI_PROVIDER);
		createEAttribute(aiProviderEClass, AI_PROVIDER__NAME);
		createEAttribute(aiProviderEClass, AI_PROVIDER__URL);
		createEAttribute(aiProviderEClass, AI_PROVIDER__API_KEY);
		createEAttribute(aiProviderEClass, AI_PROVIDER__FORMAT);
		createEAttribute(aiProviderEClass, AI_PROVIDER__LOCAL);
		createEAttribute(aiProviderEClass, AI_PROVIDER__DEFAULT_MODEL);
		createEAttribute(aiProviderEClass, AI_PROVIDER__API_KEY_ENCRYPTED);
		createEAttribute(aiProviderEClass, AI_PROVIDER__USE_ENV_VAR);
		createEAttribute(aiProviderEClass, AI_PROVIDER__ENV_VAR_NAME);
		createEAttribute(aiProviderEClass, AI_PROVIDER__STATE);
		createEAttribute(aiProviderEClass, AI_PROVIDER__STATE_DESCRIPTION);
		createEAttribute(aiProviderEClass, AI_PROVIDER__RATING);
		createEAttribute(aiProviderEClass, AI_PROVIDER__RATING_ANALYZE);
		createEAttribute(aiProviderEClass, AI_PROVIDER__RATING_CHAT);
		createEAttribute(aiProviderEClass, AI_PROVIDER__RATING_PROGRAMMING);

		gitEClass = createEClass(GIT);
		createEAttribute(gitEClass, GIT__REPOSITORY_URL);
		createEAttribute(gitEClass, GIT__BRANCH);
		createEAttribute(gitEClass, GIT__USERNAME);
		createEAttribute(gitEClass, GIT__LOCAL_PATH);
		createEAttribute(gitEClass, GIT__TEST_STATUS);

		mavenEClass = createEClass(MAVEN);
		createEAttribute(mavenEClass, MAVEN__GOALS);
		createEAttribute(mavenEClass, MAVEN__PROFILES);
		createEAttribute(mavenEClass, MAVEN__TEST_STATUS);

		llmEClass = createEClass(LLM);
		createEAttribute(llmEClass, LLM__MODEL);
		createEAttribute(llmEClass, LLM__TEMPERATURE);

		compilerEClass = createEClass(COMPILER);
		createEAttribute(compilerEClass, COMPILER__SOURCE_VERSION);
		createEAttribute(compilerEClass, COMPILER__TARGET_VERSION);
		createEAttribute(compilerEClass, COMPILER__CPATH);
		createEAttribute(compilerEClass, COMPILER__CPP_PATH);
		createEAttribute(compilerEClass, COMPILER__MAKE_PATH);
		createEAttribute(compilerEClass, COMPILER__CMAKE_PATH);
		createEAttribute(compilerEClass, COMPILER__TEST_STATUS);

		commandEClass = createEClass(COMMAND);
		createEAttribute(commandEClass, COMMAND__NAME);
		createEAttribute(commandEClass, COMMAND__STATUS);

		ollamaEClass = createEClass(OLLAMA);
		createEAttribute(ollamaEClass, OLLAMA__URL);
		createEAttribute(ollamaEClass, OLLAMA__MODEL);
		createEAttribute(ollamaEClass, OLLAMA__PATH);

		aiChatEClass = createEClass(AI_CHAT);
		createEAttribute(aiChatEClass, AI_CHAT__URL);
		createEAttribute(aiChatEClass, AI_CHAT__TOKEN);
		createEAttribute(aiChatEClass, AI_CHAT__PROMPT);
		createEAttribute(aiChatEClass, AI_CHAT__PROXY_URL);
		createEReference(aiChatEClass, AI_CHAT__THREADS);
		createEReference(aiChatEClass, AI_CHAT__PROMPT_INSTRUCTIONS);

		neuronAIEClass = createEClass(NEURON_AI);
		createEAttribute(neuronAIEClass, NEURON_AI__URL);
		createEAttribute(neuronAIEClass, NEURON_AI__MODEL);
		createEAttribute(neuronAIEClass, NEURON_AI__TYPE);
		createEAttribute(neuronAIEClass, NEURON_AI__TRAINING_DATA);

		evoProjectEClass = createEClass(EVO_PROJECT);
		createEAttribute(evoProjectEClass, EVO_PROJECT__NAME);
		createEReference(evoProjectEClass, EVO_PROJECT__ORCHESTRATIONS);

		ruleEClass = createEClass(RULE);
		createEAttribute(ruleEClass, RULE__NAME);
		createEAttribute(ruleEClass, RULE__DESCRIPTION);

		accessRuleEClass = createEClass(ACCESS_RULE);
		createEAttribute(accessRuleEClass, ACCESS_RULE__ALLOWED_PATHS);
		createEAttribute(accessRuleEClass, ACCESS_RULE__DENIED_PATHS);

		networkRuleEClass = createEClass(NETWORK_RULE);
		createEAttribute(networkRuleEClass, NETWORK_RULE__ALLOWED_DOMAINS);
		createEAttribute(networkRuleEClass, NETWORK_RULE__ALLOW_ALL);

		memoryRuleEClass = createEClass(MEMORY_RULE);
		createEAttribute(memoryRuleEClass, MEMORY_RULE__STORAGE_LIMIT);
		createEAttribute(memoryRuleEClass, MEMORY_RULE__RETENTION_PERIOD);

		secretRuleEClass = createEClass(SECRET_RULE);
		createEAttribute(secretRuleEClass, SECRET_RULE__ALLOWED_SECRETS);

		selfDevSessionEClass = createEClass(SELF_DEV_SESSION);
		createEAttribute(selfDevSessionEClass, SELF_DEV_SESSION__ID);
		createEAttribute(selfDevSessionEClass, SELF_DEV_SESSION__START_TIME);
		createEAttribute(selfDevSessionEClass, SELF_DEV_SESSION__MAX_ITERATIONS);
		createEAttribute(selfDevSessionEClass, SELF_DEV_SESSION__STATUS);
		createEReference(selfDevSessionEClass, SELF_DEV_SESSION__ITERATIONS);
		createEAttribute(selfDevSessionEClass, SELF_DEV_SESSION__RATIONALE);
		createEAttribute(selfDevSessionEClass, SELF_DEV_SESSION__INITIAL_REQUEST);

		databaseEClass = createEClass(DATABASE);
		createEAttribute(databaseEClass, DATABASE__URL);
		createEAttribute(databaseEClass, DATABASE__USERNAME);
		createEAttribute(databaseEClass, DATABASE__PASSWORD);
		createEAttribute(databaseEClass, DATABASE__DRIVER);
		createEAttribute(databaseEClass, DATABASE__TEST_STATUS);

		fileConfigEClass = createEClass(FILE_CONFIG);
		createEAttribute(fileConfigEClass, FILE_CONFIG__LOCAL_PATH);
		createEAttribute(fileConfigEClass, FILE_CONFIG__TEST_STATUS);

		iterationEClass = createEClass(ITERATION);
		createEAttribute(iterationEClass, ITERATION__ID);
		createEAttribute(iterationEClass, ITERATION__BRANCH_NAME);
		createEReference(iterationEClass, ITERATION__TASKS);
		createEReference(iterationEClass, ITERATION__EVALUATION_RESULT);
		createEAttribute(iterationEClass, ITERATION__STATUS);
		createEAttribute(iterationEClass, ITERATION__PHASE);
		createEAttribute(iterationEClass, ITERATION__COMMENTS);
		createEAttribute(iterationEClass, ITERATION__RATING);
		createEAttribute(iterationEClass, ITERATION__RATIONALE);

		eclipseEClass = createEClass(ECLIPSE);
		createEAttribute(eclipseEClass, ECLIPSE__WORKSPACE);
		createEAttribute(eclipseEClass, ECLIPSE__INSTALLATION);
		createEAttribute(eclipseEClass, ECLIPSE__TARGET_PLATFORM);
		createEAttribute(eclipseEClass, ECLIPSE__TEST_STATUS);

		evaluationResultEClass = createEClass(EVALUATION_RESULT);
		createEAttribute(evaluationResultEClass, EVALUATION_RESULT__SUCCESS);
		createEAttribute(evaluationResultEClass, EVALUATION_RESULT__TEST_PASS_RATE);
		createEAttribute(evaluationResultEClass, EVALUATION_RESULT__COVERAGE_CHANGE);
		createEAttribute(evaluationResultEClass, EVALUATION_RESULT__ERRORS);
		createEAttribute(evaluationResultEClass, EVALUATION_RESULT__DECISION);
		createEAttribute(evaluationResultEClass, EVALUATION_RESULT__USER_SATISFACTION);

		testEClass = createEClass(TEST);
		createEAttribute(testEClass, TEST__ID);
		createEAttribute(testEClass, TEST__NAME);
		createEAttribute(testEClass, TEST__TYPE);
		createEAttribute(testEClass, TEST__PATH);
		createEAttribute(testEClass, TEST__STATUS);
		createEAttribute(testEClass, TEST__SELECTED);

		commentEClass = createEClass(COMMENT);
		createEAttribute(commentEClass, COMMENT__ID);
		createEAttribute(commentEClass, COMMENT__FILE_PATH);
		createEAttribute(commentEClass, COMMENT__START_LINE);
		createEAttribute(commentEClass, COMMENT__END_LINE);
		createEAttribute(commentEClass, COMMENT__AUTHOR);
		createEAttribute(commentEClass, COMMENT__CONTENT);
		createEAttribute(commentEClass, COMMENT__TIMESTAMP);
		createEAttribute(commentEClass, COMMENT__RESOLVED);

		diffHunkEClass = createEClass(DIFF_HUNK);
		createEAttribute(diffHunkEClass, DIFF_HUNK__HEADER);
		createEAttribute(diffHunkEClass, DIFF_HUNK__LINES);

		fileChangeEClass = createEClass(FILE_CHANGE);
		createEAttribute(fileChangeEClass, FILE_CHANGE__FILE_PATH);
		createEAttribute(fileChangeEClass, FILE_CHANGE__STATUS);
		createEReference(fileChangeEClass, FILE_CHANGE__HUNKS);

		changeSetEClass = createEClass(CHANGE_SET);
		createEAttribute(changeSetEClass, CHANGE_SET__COMMIT_ID);
		createEReference(changeSetEClass, CHANGE_SET__FILES);

		reviewSessionEClass = createEClass(REVIEW_SESSION);
		createEAttribute(reviewSessionEClass, REVIEW_SESSION__ID);
		createEReference(reviewSessionEClass, REVIEW_SESSION__CHANGE_SET);
		createEReference(reviewSessionEClass, REVIEW_SESSION__COMMENTS);
		createEAttribute(reviewSessionEClass, REVIEW_SESSION__DECISION);

		chatThreadEClass = createEClass(CHAT_THREAD);
		createEAttribute(chatThreadEClass, CHAT_THREAD__ID);
		createEReference(chatThreadEClass, CHAT_THREAD__MESSAGES);

		chatMessageEClass = createEClass(CHAT_MESSAGE);
		createEAttribute(chatMessageEClass, CHAT_MESSAGE__INDEX);
		createEAttribute(chatMessageEClass, CHAT_MESSAGE__SENDER);
		createEAttribute(chatMessageEClass, CHAT_MESSAGE__TEXT);
		createEAttribute(chatMessageEClass, CHAT_MESSAGE__COLOR);
		createEAttribute(chatMessageEClass, CHAT_MESSAGE__IS_BOLD);
		createEAttribute(chatMessageEClass, CHAT_MESSAGE__IS_ITALIC);
		createEAttribute(chatMessageEClass, CHAT_MESSAGE__AGENT_TYPE);
		createEAttribute(chatMessageEClass, CHAT_MESSAGE__TIMESTAMP);

		promptInstructionsEClass = createEClass(PROMPT_INSTRUCTIONS);
		createEAttribute(promptInstructionsEClass, PROMPT_INSTRUCTIONS__AUTO_APPROVE);
		createEAttribute(promptInstructionsEClass, PROMPT_INSTRUCTIONS__GIT_AUTOMATION);
		createEAttribute(promptInstructionsEClass, PROMPT_INSTRUCTIONS__PREFERRED_MAX_ITERATIONS);
		createEAttribute(promptInstructionsEClass, PROMPT_INSTRUCTIONS__ITERATIVE_MODE);
		createEAttribute(promptInstructionsEClass, PROMPT_INSTRUCTIONS__SELF_ITERATIVE_MODE);

		// Create enums
		taskStatusEEnum = createEEnum(TASK_STATUS);
		sessionTypeEEnum = createEEnum(SESSION_TYPE);
		commandStatusEEnum = createEEnum(COMMAND_STATUS);
		executionModeEEnum = createEEnum(EXECUTION_MODE);
		neuronTypeEEnum = createEEnum(NEURON_TYPE);
		aiModeEEnum = createEEnum(AI_MODE);
		selfDevStatusEEnum = createEEnum(SELF_DEV_STATUS);
		iterationStatusEEnum = createEEnum(ITERATION_STATUS);
		selfDevDecisionEEnum = createEEnum(SELF_DEV_DECISION);
		testStatusEEnum = createEEnum(TEST_STATUS);
		reviewDecisionEEnum = createEEnum(REVIEW_DECISION);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	private boolean isInitialized = false;

	/**
	 * Complete the initialization of the package and its meta-model.  This
	 * method is guarded to have no affect on any invocation but its first.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void initializePackageContents() {
		if (isInitialized) return;
		isInitialized = true;

		// Initialize package
		setName(eNAME);
		setNsPrefix(eNS_PREFIX);
		setNsURI(eNS_URI);

		// Create type parameters

		// Set bounds for type parameters

		// Add supertypes to classes
		accessRuleEClass.getESuperTypes().add(this.getRule());
		networkRuleEClass.getESuperTypes().add(this.getRule());
		memoryRuleEClass.getESuperTypes().add(this.getRule());
		secretRuleEClass.getESuperTypes().add(this.getRule());

		// Initialize classes, features, and operations; add parameters
		initEClass(taskEClass, Task.class, "Task", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getTask_Id(), ecorePackage.getEString(), "id", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Name(), ecorePackage.getEString(), "name", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Type(), ecorePackage.getEString(), "type", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Status(), this.getTaskStatus(), "status", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTask_Next(), this.getTask(), null, "next", null, 0, -1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getTask_SubTasks(), this.getTask(), null, "subTasks", null, 0, -1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Response(), ecorePackage.getEString(), "response", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Feedback(), ecorePackage.getEString(), "feedback", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_ApprovalRequired(), ecorePackage.getEBoolean(), "approvalRequired", "true", 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_LoopToTaskId(), ecorePackage.getEString(), "loopToTaskId", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Priority(), ecorePackage.getEInt(), "priority", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_ResultSummary(), ecorePackage.getEString(), "resultSummary", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Description(), ecorePackage.getEString(), "description", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Rating(), ecorePackage.getEInt(), "rating", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Likes(), ecorePackage.getEBoolean(), "likes", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Rationale(), ecorePackage.getEString(), "rationale", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_ScheduledTime(), ecorePackage.getEString(), "scheduledTime", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Selected(), ecorePackage.getEBoolean(), "selected", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Goal(), ecorePackage.getEString(), "goal", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Plan(), ecorePackage.getEString(), "plan", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Artifacts(), ecorePackage.getEString(), "artifacts", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Prompt(), ecorePackage.getEString(), "prompt", null, 0, 1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTask_Attachments(), ecorePackage.getEString(), "attachments", null, 0, -1, Task.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(agentEClass, Agent.class, "Agent", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getAgent_Id(), ecorePackage.getEString(), "id", null, 0, 1, Agent.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAgent_Type(), ecorePackage.getEString(), "type", null, 0, 1, Agent.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getAgent_Tasks(), this.getTask(), null, "tasks", null, 0, -1, Agent.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_COMPOSITE, IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAgent_ExecutionMode(), this.getExecutionMode(), "executionMode", null, 0, 1, Agent.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getAgent_Rules(), this.getRule(), null, "rules", null, 0, -1, Agent.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(orchestratorEClass, Orchestrator.class, "Orchestrator", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getOrchestrator_Id(), ecorePackage.getEString(), "id", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOrchestrator_Name(), ecorePackage.getEString(), "name", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_Agents(), this.getAgent(), null, "agents", null, 0, -1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_Tasks(), this.getTask(), null, "tasks", null, 0, -1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_Tests(), this.getTest(), null, "tests", null, 0, -1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_Git(), this.getGit(), null, "git", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_Maven(), this.getMaven(), null, "maven", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_Llm(), this.getLLM(), null, "llm", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_Compiler(), this.getCompiler(), null, "compiler", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_Ollama(), this.getOllama(), null, "ollama", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_AiChat(), this.getAiChat(), null, "aiChat", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_NeuronAI(), this.getNeuronAI(), null, "neuronAI", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOrchestrator_RemoteModel(), ecorePackage.getEString(), "remoteModel", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOrchestrator_AiMode(), this.getAiMode(), "aiMode", null, 1, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOrchestrator_McpServerUrl(), ecorePackage.getEString(), "mcpServerUrl", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOrchestrator_OpenAiToken(), ecorePackage.getEString(), "openAiToken", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOrchestrator_OpenAiModel(), ecorePackage.getEString(), "openAiModel", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOrchestrator_LocalModel(), ecorePackage.getEString(), "localModel", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOrchestrator_HybridModel(), ecorePackage.getEString(), "hybridModel", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOrchestrator_OfflineMode(), ecorePackage.getEBoolean(), "offlineMode", "false", 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_SelfDevSession(), this.getSelfDevSession(), null, "selfDevSession", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_Database(), this.getDatabase(), null, "database", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_FileConfig(), this.getFileConfig(), null, "fileConfig", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOrchestrator_SharedMemory(), ecorePackage.getEString(), "sharedMemory", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_Eclipse(), this.getEclipse(), null, "eclipse", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOrchestrator_DarwinMode(), ecorePackage.getEBoolean(), "darwinMode", "false", 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_AiProviders(), this.getAIProvider(), null, "aiProviders", null, 0, -1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_ServerSettings(), this.getServerSettings(), null, "serverSettings", null, 0, 1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_ServerSessions(), this.getServerSession(), null, "serverSessions", null, 0, -1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getOrchestrator_MonitoringHistory(), this.getMonitoringData(), null, "monitoringHistory", null, 0, -1, Orchestrator.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serverSettingsEClass, ServerSettings.class, "ServerSettings", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServerSettings_Port(), ecorePackage.getEInt(), "port", "48080", 0, 1, ServerSettings.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerSettings_AutoStart(), ecorePackage.getEBoolean(), "autoStart", "true", 0, 1, ServerSettings.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerSettings_GitAutomation(), ecorePackage.getEBoolean(), "gitAutomation", "false", 0, 1, ServerSettings.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(serverSessionEClass, ServerSession.class, "ServerSession", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getServerSession_Id(), ecorePackage.getEString(), "id", null, 0, 1, ServerSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerSession_Type(), this.getSessionType(), "type", null, 0, 1, ServerSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerSession_StartTime(), ecorePackage.getELong(), "startTime", null, 0, 1, ServerSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerSession_LastActivity(), ecorePackage.getELong(), "lastActivity", null, 0, 1, ServerSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getServerSession_ClientIp(), ecorePackage.getEString(), "clientIp", null, 0, 1, ServerSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(monitoringDataEClass, MonitoringData.class, "MonitoringData", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getMonitoringData_Timestamp(), ecorePackage.getELong(), "timestamp", null, 0, 1, MonitoringData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMonitoringData_CpuUsage(), ecorePackage.getEDouble(), "cpuUsage", null, 0, 1, MonitoringData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMonitoringData_MemoryUsage(), ecorePackage.getELong(), "memoryUsage", null, 0, 1, MonitoringData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMonitoringData_TotalMemory(), ecorePackage.getELong(), "totalMemory", null, 0, 1, MonitoringData.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(aiProviderEClass, AIProvider.class, "AIProvider", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getAIProvider_Name(), ecorePackage.getEString(), "name", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_Url(), ecorePackage.getEString(), "url", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_ApiKey(), ecorePackage.getEString(), "apiKey", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_Format(), ecorePackage.getEString(), "format", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_Local(), ecorePackage.getEBoolean(), "local", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_DefaultModel(), ecorePackage.getEString(), "defaultModel", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_ApiKeyEncrypted(), ecorePackage.getEBoolean(), "apiKeyEncrypted", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_UseEnvVar(), ecorePackage.getEBoolean(), "useEnvVar", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_EnvVarName(), ecorePackage.getEString(), "envVarName", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_State(), ecorePackage.getEString(), "state", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_StateDescription(), ecorePackage.getEString(), "stateDescription", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_Rating(), ecorePackage.getEInt(), "rating", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_RatingAnalyze(), ecorePackage.getEInt(), "ratingAnalyze", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_RatingChat(), ecorePackage.getEInt(), "ratingChat", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAIProvider_RatingProgramming(), ecorePackage.getEInt(), "ratingProgramming", null, 0, 1, AIProvider.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(gitEClass, Git.class, "Git", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getGit_RepositoryUrl(), ecorePackage.getEString(), "repositoryUrl", null, 0, 1, Git.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getGit_Branch(), ecorePackage.getEString(), "branch", null, 0, 1, Git.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getGit_Username(), ecorePackage.getEString(), "username", null, 0, 1, Git.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getGit_LocalPath(), ecorePackage.getEString(), "localPath", null, 0, 1, Git.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getGit_TestStatus(), ecorePackage.getEString(), "testStatus", null, 0, 1, Git.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(mavenEClass, Maven.class, "Maven", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getMaven_Goals(), ecorePackage.getEString(), "goals", null, 0, -1, Maven.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMaven_Profiles(), ecorePackage.getEString(), "profiles", null, 0, -1, Maven.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMaven_TestStatus(), ecorePackage.getEString(), "testStatus", null, 0, 1, Maven.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(llmEClass, eu.kalafatic.evolution.model.orchestration.LLM.class, "LLM", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getLLM_Model(), ecorePackage.getEString(), "model", null, 0, 1, eu.kalafatic.evolution.model.orchestration.LLM.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getLLM_Temperature(), ecorePackage.getEFloat(), "temperature", "1.0", 0, 1, eu.kalafatic.evolution.model.orchestration.LLM.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(compilerEClass, eu.kalafatic.evolution.model.orchestration.Compiler.class, "Compiler", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getCompiler_SourceVersion(), ecorePackage.getEString(), "sourceVersion", null, 0, 1, eu.kalafatic.evolution.model.orchestration.Compiler.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCompiler_TargetVersion(), ecorePackage.getEString(), "targetVersion", null, 0, 1, eu.kalafatic.evolution.model.orchestration.Compiler.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCompiler_CPath(), ecorePackage.getEString(), "cPath", null, 0, 1, eu.kalafatic.evolution.model.orchestration.Compiler.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCompiler_CppPath(), ecorePackage.getEString(), "cppPath", null, 0, 1, eu.kalafatic.evolution.model.orchestration.Compiler.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCompiler_MakePath(), ecorePackage.getEString(), "makePath", null, 0, 1, eu.kalafatic.evolution.model.orchestration.Compiler.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCompiler_CmakePath(), ecorePackage.getEString(), "cmakePath", null, 0, 1, eu.kalafatic.evolution.model.orchestration.Compiler.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCompiler_TestStatus(), ecorePackage.getEString(), "testStatus", null, 0, 1, eu.kalafatic.evolution.model.orchestration.Compiler.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(commandEClass, Command.class, "Command", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getCommand_Name(), ecorePackage.getEString(), "name", null, 0, 1, Command.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getCommand_Status(), this.getCommandStatus(), "status", null, 0, 1, Command.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(ollamaEClass, Ollama.class, "Ollama", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getOllama_Url(), ecorePackage.getEString(), "url", null, 0, 1, Ollama.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOllama_Model(), ecorePackage.getEString(), "model", null, 0, 1, Ollama.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getOllama_Path(), ecorePackage.getEString(), "path", null, 0, 1, Ollama.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(aiChatEClass, AiChat.class, "AiChat", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getAiChat_Url(), ecorePackage.getEString(), "url", null, 0, 1, AiChat.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAiChat_Token(), ecorePackage.getEString(), "token", null, 0, 1, AiChat.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAiChat_Prompt(), ecorePackage.getEString(), "prompt", null, 0, 1, AiChat.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAiChat_ProxyUrl(), ecorePackage.getEString(), "proxyUrl", null, 0, 1, AiChat.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getAiChat_Threads(), this.getChatThread(), null, "threads", null, 0, -1, AiChat.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getAiChat_PromptInstructions(), this.getPromptInstructions(), null, "promptInstructions", null, 0, 1, AiChat.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(neuronAIEClass, NeuronAI.class, "NeuronAI", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getNeuronAI_Url(), ecorePackage.getEString(), "url", null, 0, 1, NeuronAI.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getNeuronAI_Model(), ecorePackage.getEString(), "model", null, 0, 1, NeuronAI.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getNeuronAI_Type(), this.getNeuronType(), "type", null, 0, 1, NeuronAI.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getNeuronAI_TrainingData(), ecorePackage.getEString(), "trainingData", null, 0, 1, NeuronAI.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(evoProjectEClass, EvoProject.class, "EvoProject", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEvoProject_Name(), ecorePackage.getEString(), "name", null, 0, 1, EvoProject.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getEvoProject_Orchestrations(), this.getOrchestrator(), null, "orchestrations", null, 0, -1, EvoProject.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(ruleEClass, Rule.class, "Rule", IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getRule_Name(), ecorePackage.getEString(), "name", null, 0, 1, Rule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getRule_Description(), ecorePackage.getEString(), "description", null, 0, 1, Rule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(accessRuleEClass, AccessRule.class, "AccessRule", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getAccessRule_AllowedPaths(), ecorePackage.getEString(), "allowedPaths", null, 0, -1, AccessRule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getAccessRule_DeniedPaths(), ecorePackage.getEString(), "deniedPaths", null, 0, -1, AccessRule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(networkRuleEClass, NetworkRule.class, "NetworkRule", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getNetworkRule_AllowedDomains(), ecorePackage.getEString(), "allowedDomains", null, 0, -1, NetworkRule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getNetworkRule_AllowAll(), ecorePackage.getEBoolean(), "allowAll", "false", 0, 1, NetworkRule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(memoryRuleEClass, MemoryRule.class, "MemoryRule", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getMemoryRule_StorageLimit(), ecorePackage.getEInt(), "storageLimit", null, 0, 1, MemoryRule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getMemoryRule_RetentionPeriod(), ecorePackage.getEInt(), "retentionPeriod", null, 0, 1, MemoryRule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(secretRuleEClass, SecretRule.class, "SecretRule", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getSecretRule_AllowedSecrets(), ecorePackage.getEString(), "allowedSecrets", null, 0, -1, SecretRule.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(selfDevSessionEClass, SelfDevSession.class, "SelfDevSession", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getSelfDevSession_Id(), ecorePackage.getEString(), "id", null, 0, 1, SelfDevSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSelfDevSession_StartTime(), ecorePackage.getELong(), "startTime", null, 0, 1, SelfDevSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSelfDevSession_MaxIterations(), ecorePackage.getEInt(), "maxIterations", null, 0, 1, SelfDevSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSelfDevSession_Status(), this.getSelfDevStatus(), "status", null, 0, 1, SelfDevSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getSelfDevSession_Iterations(), this.getIteration(), null, "iterations", null, 0, -1, SelfDevSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSelfDevSession_Rationale(), ecorePackage.getEString(), "rationale", null, 0, 1, SelfDevSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getSelfDevSession_InitialRequest(), ecorePackage.getEString(), "initialRequest", null, 0, 1, SelfDevSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(databaseEClass, Database.class, "Database", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getDatabase_Url(), ecorePackage.getEString(), "url", null, 0, 1, Database.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDatabase_Username(), ecorePackage.getEString(), "username", null, 0, 1, Database.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDatabase_Password(), ecorePackage.getEString(), "password", null, 0, 1, Database.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDatabase_Driver(), ecorePackage.getEString(), "driver", null, 0, 1, Database.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDatabase_TestStatus(), ecorePackage.getEString(), "testStatus", null, 0, 1, Database.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(fileConfigEClass, FileConfig.class, "FileConfig", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFileConfig_LocalPath(), ecorePackage.getEString(), "localPath", null, 0, 1, FileConfig.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFileConfig_TestStatus(), ecorePackage.getEString(), "testStatus", null, 0, 1, FileConfig.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(iterationEClass, Iteration.class, "Iteration", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getIteration_Id(), ecorePackage.getEString(), "id", null, 0, 1, Iteration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getIteration_BranchName(), ecorePackage.getEString(), "branchName", null, 0, 1, Iteration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getIteration_Tasks(), this.getTask(), null, "tasks", null, 0, -1, Iteration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getIteration_EvaluationResult(), this.getEvaluationResult(), null, "evaluationResult", null, 0, 1, Iteration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getIteration_Status(), this.getIterationStatus(), "status", null, 0, 1, Iteration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getIteration_Phase(), ecorePackage.getEString(), "phase", null, 0, 1, Iteration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getIteration_Comments(), ecorePackage.getEString(), "comments", null, 0, 1, Iteration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getIteration_Rating(), ecorePackage.getEInt(), "rating", null, 0, 1, Iteration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getIteration_Rationale(), ecorePackage.getEString(), "rationale", null, 0, 1, Iteration.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(eclipseEClass, Eclipse.class, "Eclipse", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEclipse_Workspace(), ecorePackage.getEString(), "workspace", null, 0, 1, Eclipse.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEclipse_Installation(), ecorePackage.getEString(), "installation", null, 0, 1, Eclipse.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEclipse_TargetPlatform(), ecorePackage.getEString(), "targetPlatform", null, 0, 1, Eclipse.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEclipse_TestStatus(), ecorePackage.getEString(), "testStatus", null, 0, 1, Eclipse.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(evaluationResultEClass, EvaluationResult.class, "EvaluationResult", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getEvaluationResult_Success(), ecorePackage.getEBoolean(), "success", null, 0, 1, EvaluationResult.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEvaluationResult_TestPassRate(), ecorePackage.getEDouble(), "testPassRate", null, 0, 1, EvaluationResult.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEvaluationResult_CoverageChange(), ecorePackage.getEDouble(), "coverageChange", null, 0, 1, EvaluationResult.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEvaluationResult_Errors(), ecorePackage.getEString(), "errors", null, 0, -1, EvaluationResult.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEvaluationResult_Decision(), this.getSelfDevDecision(), "decision", null, 0, 1, EvaluationResult.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getEvaluationResult_UserSatisfaction(), ecorePackage.getEInt(), "userSatisfaction", null, 0, 1, EvaluationResult.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(testEClass, Test.class, "Test", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getTest_Id(), ecorePackage.getEString(), "id", null, 0, 1, Test.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTest_Name(), ecorePackage.getEString(), "name", null, 0, 1, Test.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTest_Type(), ecorePackage.getEString(), "type", null, 0, 1, Test.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTest_Path(), ecorePackage.getEString(), "path", null, 0, 1, Test.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTest_Status(), this.getTestStatus(), "status", null, 0, 1, Test.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getTest_Selected(), ecorePackage.getEBoolean(), "selected", null, 0, 1, Test.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(commentEClass, Comment.class, "Comment", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getComment_Id(), ecorePackage.getEString(), "id", null, 0, 1, Comment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComment_FilePath(), ecorePackage.getEString(), "filePath", null, 0, 1, Comment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComment_StartLine(), ecorePackage.getEInt(), "startLine", null, 0, 1, Comment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComment_EndLine(), ecorePackage.getEInt(), "endLine", null, 0, 1, Comment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComment_Author(), ecorePackage.getEString(), "author", null, 0, 1, Comment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComment_Content(), ecorePackage.getEString(), "content", null, 0, 1, Comment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComment_Timestamp(), ecorePackage.getEString(), "timestamp", null, 0, 1, Comment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getComment_Resolved(), ecorePackage.getEBoolean(), "resolved", null, 0, 1, Comment.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(diffHunkEClass, DiffHunk.class, "DiffHunk", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getDiffHunk_Header(), ecorePackage.getEString(), "header", null, 0, 1, DiffHunk.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getDiffHunk_Lines(), ecorePackage.getEString(), "lines", null, 0, -1, DiffHunk.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(fileChangeEClass, FileChange.class, "FileChange", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getFileChange_FilePath(), ecorePackage.getEString(), "filePath", null, 0, 1, FileChange.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getFileChange_Status(), ecorePackage.getEString(), "status", null, 0, 1, FileChange.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getFileChange_Hunks(), this.getDiffHunk(), null, "hunks", null, 0, -1, FileChange.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(changeSetEClass, ChangeSet.class, "ChangeSet", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getChangeSet_CommitId(), ecorePackage.getEString(), "commitId", null, 0, 1, ChangeSet.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getChangeSet_Files(), this.getFileChange(), null, "files", null, 0, -1, ChangeSet.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(reviewSessionEClass, ReviewSession.class, "ReviewSession", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getReviewSession_Id(), ecorePackage.getEString(), "id", null, 0, 1, ReviewSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getReviewSession_ChangeSet(), this.getChangeSet(), null, "changeSet", null, 0, 1, ReviewSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getReviewSession_Comments(), this.getComment(), null, "comments", null, 0, -1, ReviewSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getReviewSession_Decision(), this.getReviewDecision(), "decision", null, 0, 1, ReviewSession.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(chatThreadEClass, ChatThread.class, "ChatThread", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getChatThread_Id(), ecorePackage.getEString(), "id", null, 0, 1, ChatThread.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEReference(getChatThread_Messages(), this.getChatMessage(), null, "messages", null, 0, -1, ChatThread.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, IS_COMPOSITE, !IS_RESOLVE_PROXIES, !IS_UNSETTABLE, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(chatMessageEClass, ChatMessage.class, "ChatMessage", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getChatMessage_Index(), ecorePackage.getEInt(), "index", null, 0, 1, ChatMessage.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getChatMessage_Sender(), ecorePackage.getEString(), "sender", null, 0, 1, ChatMessage.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getChatMessage_Text(), ecorePackage.getEString(), "text", null, 0, 1, ChatMessage.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getChatMessage_Color(), ecorePackage.getEString(), "color", null, 0, 1, ChatMessage.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getChatMessage_IsBold(), ecorePackage.getEBoolean(), "isBold", null, 0, 1, ChatMessage.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getChatMessage_IsItalic(), ecorePackage.getEBoolean(), "isItalic", null, 0, 1, ChatMessage.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getChatMessage_AgentType(), ecorePackage.getEString(), "agentType", null, 0, 1, ChatMessage.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getChatMessage_Timestamp(), ecorePackage.getEString(), "timestamp", null, 0, 1, ChatMessage.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		initEClass(promptInstructionsEClass, PromptInstructions.class, "PromptInstructions", !IS_ABSTRACT, !IS_INTERFACE, IS_GENERATED_INSTANCE_CLASS);
		initEAttribute(getPromptInstructions_AutoApprove(), ecorePackage.getEBoolean(), "autoApprove", null, 0, 1, PromptInstructions.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPromptInstructions_GitAutomation(), ecorePackage.getEBoolean(), "gitAutomation", null, 0, 1, PromptInstructions.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPromptInstructions_PreferredMaxIterations(), ecorePackage.getEInt(), "preferredMaxIterations", "-1", 0, 1, PromptInstructions.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, !IS_ORDERED);
		initEAttribute(getPromptInstructions_IterativeMode(), ecorePackage.getEBoolean(), "iterativeMode", "false", 0, 1, PromptInstructions.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);
		initEAttribute(getPromptInstructions_SelfIterativeMode(), ecorePackage.getEBoolean(), "selfIterativeMode", "false", 0, 1, PromptInstructions.class, !IS_TRANSIENT, !IS_VOLATILE, IS_CHANGEABLE, !IS_UNSETTABLE, !IS_ID, IS_UNIQUE, !IS_DERIVED, IS_ORDERED);

		// Initialize enums and add enum literals
		initEEnum(taskStatusEEnum, TaskStatus.class, "TaskStatus");
		addEEnumLiteral(taskStatusEEnum, TaskStatus.PENDING);
		addEEnumLiteral(taskStatusEEnum, TaskStatus.RUNNING);
		addEEnumLiteral(taskStatusEEnum, TaskStatus.DONE);
		addEEnumLiteral(taskStatusEEnum, TaskStatus.FAILED);
		addEEnumLiteral(taskStatusEEnum, TaskStatus.WAITING_FOR_APPROVAL);
		addEEnumLiteral(taskStatusEEnum, TaskStatus.PLANNING);
		addEEnumLiteral(taskStatusEEnum, TaskStatus.EXECUTING);
		addEEnumLiteral(taskStatusEEnum, TaskStatus.VERIFYING);

		initEEnum(sessionTypeEEnum, SessionType.class, "SessionType");
		addEEnumLiteral(sessionTypeEEnum, SessionType.HTTPD);
		addEEnumLiteral(sessionTypeEEnum, SessionType.UI);

		initEEnum(commandStatusEEnum, CommandStatus.class, "CommandStatus");
		addEEnumLiteral(commandStatusEEnum, CommandStatus.PENDING);
		addEEnumLiteral(commandStatusEEnum, CommandStatus.RUNNING);
		addEEnumLiteral(commandStatusEEnum, CommandStatus.COMPLETED);
		addEEnumLiteral(commandStatusEEnum, CommandStatus.FAILED);

		initEEnum(executionModeEEnum, ExecutionMode.class, "ExecutionMode");
		addEEnumLiteral(executionModeEEnum, ExecutionMode.SERIAL);
		addEEnumLiteral(executionModeEEnum, ExecutionMode.PARALLEL);

		initEEnum(neuronTypeEEnum, NeuronType.class, "NeuronType");
		addEEnumLiteral(neuronTypeEEnum, NeuronType.MLP);
		addEEnumLiteral(neuronTypeEEnum, NeuronType.CNN);
		addEEnumLiteral(neuronTypeEEnum, NeuronType.RNN);
		addEEnumLiteral(neuronTypeEEnum, NeuronType.LSTM);
		addEEnumLiteral(neuronTypeEEnum, NeuronType.TRANSFORMER);

		initEEnum(aiModeEEnum, AiMode.class, "AiMode");
		addEEnumLiteral(aiModeEEnum, AiMode.LOCAL);
		addEEnumLiteral(aiModeEEnum, AiMode.HYBRID);
		addEEnumLiteral(aiModeEEnum, AiMode.REMOTE);

		initEEnum(selfDevStatusEEnum, SelfDevStatus.class, "SelfDevStatus");
		addEEnumLiteral(selfDevStatusEEnum, SelfDevStatus.RUNNING);
		addEEnumLiteral(selfDevStatusEEnum, SelfDevStatus.STOPPED);
		addEEnumLiteral(selfDevStatusEEnum, SelfDevStatus.FAILED);
		addEEnumLiteral(selfDevStatusEEnum, SelfDevStatus.COMPLETED);

		initEEnum(iterationStatusEEnum, IterationStatus.class, "IterationStatus");
		addEEnumLiteral(iterationStatusEEnum, IterationStatus.PENDING);
		addEEnumLiteral(iterationStatusEEnum, IterationStatus.RUNNING);
		addEEnumLiteral(iterationStatusEEnum, IterationStatus.DONE);
		addEEnumLiteral(iterationStatusEEnum, IterationStatus.FAILED);

		initEEnum(selfDevDecisionEEnum, SelfDevDecision.class, "SelfDevDecision");
		addEEnumLiteral(selfDevDecisionEEnum, SelfDevDecision.CONTINUE);
		addEEnumLiteral(selfDevDecisionEEnum, SelfDevDecision.ROLLBACK);
		addEEnumLiteral(selfDevDecisionEEnum, SelfDevDecision.STOP);

		initEEnum(testStatusEEnum, TestStatus.class, "TestStatus");
		addEEnumLiteral(testStatusEEnum, TestStatus.PENDING);
		addEEnumLiteral(testStatusEEnum, TestStatus.RUNNING);
		addEEnumLiteral(testStatusEEnum, TestStatus.PASSED);
		addEEnumLiteral(testStatusEEnum, TestStatus.FAILED);

		initEEnum(reviewDecisionEEnum, ReviewDecision.class, "ReviewDecision");
		addEEnumLiteral(reviewDecisionEEnum, ReviewDecision.OPEN);
		addEEnumLiteral(reviewDecisionEEnum, ReviewDecision.IN_REVIEW);
		addEEnumLiteral(reviewDecisionEEnum, ReviewDecision.APPROVED);
		addEEnumLiteral(reviewDecisionEEnum, ReviewDecision.REJECTED);
		addEEnumLiteral(reviewDecisionEEnum, ReviewDecision.CHANGES_REQUESTED);

		// Create resource
		createResource(eNS_URI);
	}

} //OrchestrationPackageImpl
