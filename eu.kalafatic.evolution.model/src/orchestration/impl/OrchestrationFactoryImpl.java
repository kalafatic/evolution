/**
 */
package orchestration.impl;

import orchestration.Agent;
import orchestration.Command;
import orchestration.CommandStatus;
import orchestration.Git;
import orchestration.LLM;
import orchestration.Maven;
import orchestration.OrchestrationFactory;
import orchestration.OrchestrationPackage;
import orchestration.Orchestrator;
import orchestration.Task;
import orchestration.TaskStatus;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class OrchestrationFactoryImpl extends EFactoryImpl implements OrchestrationFactory {
	/**
	 * Creates the default factory implementation.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public static OrchestrationFactory init() {
		try {
			OrchestrationFactory theOrchestrationFactory = (OrchestrationFactory)EPackage.Registry.INSTANCE.getEFactory(OrchestrationPackage.eNS_URI);
			if (theOrchestrationFactory != null) {
				return theOrchestrationFactory;
			}
		}
		catch (Exception exception) {
			EcorePlugin.INSTANCE.log(exception);
		}
		return new OrchestrationFactoryImpl();
	}

	/**
	 * Creates an instance of the factory.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public OrchestrationFactoryImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EObject create(EClass eClass) {
		switch (eClass.getClassifierID()) {
			case OrchestrationPackage.TASK: return createTask();
			case OrchestrationPackage.AGENT: return createAgent();
			case OrchestrationPackage.ORCHESTRATOR: return createOrchestrator();
			case OrchestrationPackage.GIT: return createGit();
			case OrchestrationPackage.MAVEN: return createMaven();
			case OrchestrationPackage.LLM: return createLLM();
			case OrchestrationPackage.COMPILER: return createCompiler();
			case OrchestrationPackage.COMMAND: return createCommand();
			default:
				throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object createFromString(EDataType eDataType, String initialValue) {
		switch (eDataType.getClassifierID()) {
			case OrchestrationPackage.TASK_STATUS:
				return createTaskStatusFromString(eDataType, initialValue);
			case OrchestrationPackage.COMMAND_STATUS:
				return createCommandStatusFromString(eDataType, initialValue);
			default:
				throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String convertToString(EDataType eDataType, Object instanceValue) {
		switch (eDataType.getClassifierID()) {
			case OrchestrationPackage.TASK_STATUS:
				return convertTaskStatusToString(eDataType, instanceValue);
			case OrchestrationPackage.COMMAND_STATUS:
				return convertCommandStatusToString(eDataType, instanceValue);
			default:
				throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
		}
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Task createTask() {
		TaskImpl task = new TaskImpl();
		return task;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Agent createAgent() {
		AgentImpl agent = new AgentImpl();
		return agent;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Orchestrator createOrchestrator() {
		OrchestratorImpl orchestrator = new OrchestratorImpl();
		return orchestrator;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Git createGit() {
		GitImpl git = new GitImpl();
		return git;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Maven createMaven() {
		MavenImpl maven = new MavenImpl();
		return maven;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public LLM createLLM() {
		LLMImpl llm = new LLMImpl();
		return llm;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public orchestration.Compiler createCompiler() {
		CompilerImpl compiler = new CompilerImpl();
		return compiler;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public Command createCommand() {
		CommandImpl command = new CommandImpl();
		return command;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public TaskStatus createTaskStatusFromString(EDataType eDataType, String initialValue) {
		TaskStatus result = TaskStatus.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertTaskStatusToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public CommandStatus createCommandStatusFromString(EDataType eDataType, String initialValue) {
		CommandStatus result = CommandStatus.get(initialValue);
		if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
		return result;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String convertCommandStatusToString(EDataType eDataType, Object instanceValue) {
		return instanceValue == null ? null : instanceValue.toString();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public OrchestrationPackage getOrchestrationPackage() {
		return (OrchestrationPackage)getEPackage();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @deprecated
	 * @generated
	 */
	@Deprecated
	public static OrchestrationPackage getPackage() {
		return OrchestrationPackage.eINSTANCE;
	}

} //OrchestrationFactoryImpl
