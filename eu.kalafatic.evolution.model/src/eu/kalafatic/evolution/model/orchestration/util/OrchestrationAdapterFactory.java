/**
 */
package eu.kalafatic.evolution.model.orchestration.util;

import eu.kalafatic.evolution.model.orchestration.AccessRule;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.EvaluationResult;
import eu.kalafatic.evolution.model.orchestration.Command;
import eu.kalafatic.evolution.model.orchestration.Database;
import eu.kalafatic.evolution.model.orchestration.Eclipse;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.FileConfig;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.LLM;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.MemoryRule;
import eu.kalafatic.evolution.model.orchestration.NetworkRule;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Rule;
import eu.kalafatic.evolution.model.orchestration.SecretRule;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.Task;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notifier;

import org.eclipse.emf.common.notify.impl.AdapterFactoryImpl;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * The <b>Adapter Factory</b> for the model.
 * It provides an adapter <code>createXXX</code> method for each class of the model.
 * <!-- end-user-doc -->
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage
 * @generated
 */
public class OrchestrationAdapterFactory extends AdapterFactoryImpl {
	/**
	 * The cached model package.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected static OrchestrationPackage modelPackage;

	/**
	 * Creates an instance of the adapter factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public OrchestrationAdapterFactory() {
		if (modelPackage == null) {
			modelPackage = OrchestrationPackage.eINSTANCE;
		}
	}

	/**
	 * Returns whether this factory is applicable for the type of the object.
	 * <!-- begin-user-doc -->
	 * This implementation returns <code>true</code> if the object is either the model's package or is an instance object of the model.
	 * <!-- end-user-doc -->
	 * @return whether this factory is applicable for the type of the object.
	 * @generated
	 */
	@Override
	public boolean isFactoryForType(Object object) {
		if (object == modelPackage) {
			return true;
		}
		if (object instanceof EObject) {
			return ((EObject)object).eClass().getEPackage() == modelPackage;
		}
		return false;
	}

	/**
	 * The switch that delegates to the <code>createXXX</code> methods.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected OrchestrationSwitch<Adapter> modelSwitch =
		new OrchestrationSwitch<Adapter>() {
			@Override
			public Adapter caseTask(Task object) {
				return createTaskAdapter();
			}
			@Override
			public Adapter caseAgent(Agent object) {
				return createAgentAdapter();
			}
			@Override
			public Adapter caseOrchestrator(Orchestrator object) {
				return createOrchestratorAdapter();
			}
			@Override
			public Adapter caseGit(Git object) {
				return createGitAdapter();
			}
			@Override
			public Adapter caseMaven(Maven object) {
				return createMavenAdapter();
			}
			@Override
			public Adapter caseLLM(LLM object) {
				return createLLMAdapter();
			}
			@Override
			public Adapter caseCompiler(eu.kalafatic.evolution.model.orchestration.Compiler object) {
				return createCompilerAdapter();
			}
			@Override
			public Adapter caseCommand(Command object) {
				return createCommandAdapter();
			}
			@Override
			public Adapter caseOllama(Ollama object) {
				return createOllamaAdapter();
			}
			@Override
			public Adapter caseAiChat(AiChat object) {
				return createAiChatAdapter();
			}
			@Override
			public Adapter caseNeuronAI(NeuronAI object) {
				return createNeuronAIAdapter();
			}
			@Override
			public Adapter caseEvoProject(EvoProject object) {
				return createEvoProjectAdapter();
			}
			@Override
			public Adapter caseRule(Rule object) {
				return createRuleAdapter();
			}
			@Override
			public Adapter caseAccessRule(AccessRule object) {
				return createAccessRuleAdapter();
			}
			@Override
			public Adapter caseNetworkRule(NetworkRule object) {
				return createNetworkRuleAdapter();
			}
			@Override
			public Adapter caseMemoryRule(MemoryRule object) {
				return createMemoryRuleAdapter();
			}
			@Override
			public Adapter caseSecretRule(SecretRule object) {
				return createSecretRuleAdapter();
			}
			@Override
			public Adapter caseSelfDevSession(SelfDevSession object) {
				return createSelfDevSessionAdapter();
			}
			@Override
			public Adapter caseDatabase(Database object) {
				return createDatabaseAdapter();
			}
			@Override
			public Adapter caseFileConfig(FileConfig object) {
				return createFileConfigAdapter();
			}
			@Override
			public Adapter caseIteration(Iteration object) {
				return createIterationAdapter();
			}
			@Override
			public Adapter caseEclipse(Eclipse object) {
				return createEclipseAdapter();
			}
			@Override
			public Adapter caseEvaluationResult(EvaluationResult object) {
				return createEvaluationResultAdapter();
			}
			@Override
			public Adapter defaultCase(EObject object) {
				return createEObjectAdapter();
			}
		};

	/**
	 * Creates an adapter for the <code>target</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param target the object to adapt.
	 * @return the adapter for the <code>target</code>.
	 * @generated
	 */
	@Override
	public Adapter createAdapter(Notifier target) {
		return modelSwitch.doSwitch((EObject)target);
	}


	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Task <em>Task</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Task
	 * @generated
	 */
	public Adapter createTaskAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Agent <em>Agent</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Agent
	 * @generated
	 */
	public Adapter createAgentAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Orchestrator <em>Orchestrator</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Orchestrator
	 * @generated
	 */
	public Adapter createOrchestratorAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Git <em>Git</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Git
	 * @generated
	 */
	public Adapter createGitAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Maven <em>Maven</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Maven
	 * @generated
	 */
	public Adapter createMavenAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.LLM <em>LLM</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.LLM
	 * @generated
	 */
	public Adapter createLLMAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Compiler <em>Compiler</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Compiler
	 * @generated
	 */
	public Adapter createCompilerAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Command <em>Command</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Command
	 * @generated
	 */
	public Adapter createCommandAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Ollama <em>Ollama</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Ollama
	 * @generated
	 */
	public Adapter createOllamaAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.AiChat <em>Ai Chat</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.AiChat
	 * @generated
	 */
	public Adapter createAiChatAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.NeuronAI <em>Neuron AI</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.NeuronAI
	 * @generated
	 */
	public Adapter createNeuronAIAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.EvoProject <em>Evo Project</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.EvoProject
	 * @generated
	 */
	public Adapter createEvoProjectAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Rule <em>Rule</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Rule
	 * @generated
	 */
	public Adapter createRuleAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.AccessRule <em>Access Rule</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.AccessRule
	 * @generated
	 */
	public Adapter createAccessRuleAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.NetworkRule <em>Network Rule</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.NetworkRule
	 * @generated
	 */
	public Adapter createNetworkRuleAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.MemoryRule <em>Memory Rule</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.MemoryRule
	 * @generated
	 */
	public Adapter createMemoryRuleAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.SecretRule <em>Secret Rule</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.SecretRule
	 * @generated
	 */
	public Adapter createSecretRuleAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.SelfDevSession <em>Self Dev Session</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.SelfDevSession
	 * @generated
	 */
	public Adapter createSelfDevSessionAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Database <em>Database</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Database
	 * @generated
	 */
	public Adapter createDatabaseAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.FileConfig <em>File Config</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.FileConfig
	 * @generated
	 */
	public Adapter createFileConfigAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Iteration <em>Iteration</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Iteration
	 * @generated
	 */
	public Adapter createIterationAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.Eclipse <em>Eclipse</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.Eclipse
	 * @generated
	 */
	public Adapter createEclipseAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for an object of class '{@link eu.kalafatic.evolution.model.orchestration.EvaluationResult <em>Evaluation Result</em>}'.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null so that we can easily ignore cases;
	 * it's useful to ignore a case when inheritance will catch all the cases anyway.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @see eu.kalafatic.evolution.model.orchestration.EvaluationResult
	 * @generated
	 */
	public Adapter createEvaluationResultAdapter() {
		return null;
	}

	/**
	 * Creates a new adapter for the default case.
	 * <!-- begin-user-doc -->
	 * This default implementation returns null.
	 * <!-- end-user-doc -->
	 * @return the new adapter.
	 * @generated
	 */
	public Adapter createEObjectAdapter() {
		return null;
	}

} //OrchestrationAdapterFactory
