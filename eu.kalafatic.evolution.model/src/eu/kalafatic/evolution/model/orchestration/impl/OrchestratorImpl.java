/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.LLM;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.model.orchestration.Task;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Orchestrator</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getName <em>Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getAgents <em>Agents</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getTasks <em>Tasks</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getGit <em>Git</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getMaven <em>Maven</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getLlm <em>Llm</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getCompiler <em>Compiler</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getOllama <em>Ollama</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getAiChat <em>Ai Chat</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getNeuronAI <em>Neuron AI</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getRemoteModel <em>Remote Model</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getAiMode <em>Ai Mode</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.OrchestratorImpl#getMcpServerUrl <em>Mcp Server Url</em>}</li>
 * </ul>
 *
 * @generated
 */
public class OrchestratorImpl extends MinimalEObjectImpl.Container implements Orchestrator {
	/**
	 * The default value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected static final String ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected String id = ID_EDEFAULT;

	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The cached value of the '{@link #getAgents() <em>Agents</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAgents()
	 * @generated
	 * @ordered
	 */
	protected EList<Agent> agents;

	/**
	 * The cached value of the '{@link #getTasks() <em>Tasks</em>}' containment reference list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTasks()
	 * @generated
	 * @ordered
	 */
	protected EList<Task> tasks;

	/**
	 * The cached value of the '{@link #getGit() <em>Git</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGit()
	 * @generated
	 * @ordered
	 */
	protected Git git;

	/**
	 * The cached value of the '{@link #getMaven() <em>Maven</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMaven()
	 * @generated
	 * @ordered
	 */
	protected Maven maven;

	/**
	 * The cached value of the '{@link #getLlm() <em>Llm</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLlm()
	 * @generated
	 * @ordered
	 */
	protected LLM llm;

	/**
	 * The cached value of the '{@link #getCompiler() <em>Compiler</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCompiler()
	 * @generated
	 * @ordered
	 */
	protected eu.kalafatic.evolution.model.orchestration.Compiler compiler;

	/**
	 * The cached value of the '{@link #getOllama() <em>Ollama</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOllama()
	 * @generated
	 * @ordered
	 */
	protected Ollama ollama;

	/**
	 * The cached value of the '{@link #getAiChat() <em>Ai Chat</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAiChat()
	 * @generated
	 * @ordered
	 */
	protected AiChat aiChat;

	/**
	 * The cached value of the '{@link #getNeuronAI() <em>Neuron AI</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getNeuronAI()
	 * @generated
	 * @ordered
	 */
	protected NeuronAI neuronAI;

	/**
	 * The default value of the '{@link #getRemoteModel() <em>Remote Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRemoteModel()
	 * @generated
	 * @ordered
	 */
	protected static final String REMOTE_MODEL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRemoteModel() <em>Remote Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRemoteModel()
	 * @generated
	 * @ordered
	 */
	protected String remoteModel = REMOTE_MODEL_EDEFAULT;

	/**
	 * The default value of the '{@link #getAiMode() <em>Ai Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAiMode()
	 * @generated
	 * @ordered
	 */
	protected static final AiMode AI_MODE_EDEFAULT = AiMode.LOCAL;

	/**
	 * The cached value of the '{@link #getAiMode() <em>Ai Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAiMode()
	 * @generated
	 * @ordered
	 */
	protected AiMode aiMode = AI_MODE_EDEFAULT;

	/**
	 * The default value of the '{@link #getMcpServerUrl() <em>Mcp Server Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMcpServerUrl()
	 * @generated
	 * @ordered
	 */
	protected static final String MCP_SERVER_URL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMcpServerUrl() <em>Mcp Server Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMcpServerUrl()
	 * @generated
	 * @ordered
	 */
	protected String mcpServerUrl = MCP_SERVER_URL_EDEFAULT;

	/**
	 * The default value of the '{@link #getOpenAiToken() <em>Open Ai Token</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOpenAiToken()
	 * @generated
	 * @ordered
	 */
	protected static final String OPEN_AI_TOKEN_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getOpenAiToken() <em>Open Ai Token</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOpenAiToken()
	 * @generated
	 * @ordered
	 */
	protected String openAiToken = OPEN_AI_TOKEN_EDEFAULT;

	/**
	 * The default value of the '{@link #getOpenAiModel() <em>Open Ai Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOpenAiModel()
	 * @generated
	 * @ordered
	 */
	protected static final String OPEN_AI_MODEL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getOpenAiModel() <em>Open Ai Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getOpenAiModel()
	 * @generated
	 * @ordered
	 */
	protected String openAiModel = OPEN_AI_MODEL_EDEFAULT;

	/**
	 * The default value of the '{@link #getLocalModel() <em>Local Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLocalModel()
	 * @generated
	 * @ordered
	 */
	protected static final String LOCAL_MODEL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getLocalModel() <em>Local Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLocalModel()
	 * @generated
	 * @ordered
	 */
	protected String localModel = LOCAL_MODEL_EDEFAULT;

	/**
	 * The default value of the '{@link #getHybridModel() <em>Hybrid Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getHybridModel()
	 * @generated
	 * @ordered
	 */
	protected static final String HYBRID_MODEL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getHybridModel() <em>Hybrid Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getHybridModel()
	 * @generated
	 * @ordered
	 */
	protected String hybridModel = HYBRID_MODEL_EDEFAULT;

	/**
	 * The default value of the '{@link #isOfflineMode() <em>Offline Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isOfflineMode()
	 * @generated
	 * @ordered
	 */
	protected static final boolean OFFLINE_MODE_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isOfflineMode() <em>Offline Mode</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isOfflineMode()
	 * @generated
	 * @ordered
	 */
	protected boolean offlineMode = OFFLINE_MODE_EDEFAULT;

	/**
	 * The cached value of the '{@link #getSelfDevSession() <em>Self Dev Session</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSelfDevSession()
	 * @generated
	 * @ordered
	 */
	protected SelfDevSession selfDevSession;

	/**
	 * The default value of the '{@link #getSharedMemory() <em>Shared Memory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSharedMemory()
	 * @generated
	 * @ordered
	 */
	protected static final String SHARED_MEMORY_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSharedMemory() <em>Shared Memory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSharedMemory()
	 * @generated
	 * @ordered
	 */
	protected String sharedMemory = SHARED_MEMORY_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected OrchestratorImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.ORCHESTRATOR;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setId(String newId) {
		String oldId = id;
		id = newId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__ID, oldId, id));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Agent> getAgents() {
		if (agents == null) {
			agents = new EObjectContainmentEList<Agent>(Agent.class, this, OrchestrationPackage.ORCHESTRATOR__AGENTS);
		}
		return agents;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<Task> getTasks() {
		if (tasks == null) {
			tasks = new EObjectContainmentEList<Task>(Task.class, this, OrchestrationPackage.ORCHESTRATOR__TASKS);
		}
		return tasks;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Git getGit() {
		return git;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetGit(Git newGit, NotificationChain msgs) {
		Git oldGit = git;
		git = newGit;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__GIT, oldGit, newGit);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setGit(Git newGit) {
		if (newGit != git) {
			NotificationChain msgs = null;
			if (git != null)
				msgs = ((InternalEObject)git).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__GIT, null, msgs);
			if (newGit != null)
				msgs = ((InternalEObject)newGit).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__GIT, null, msgs);
			msgs = basicSetGit(newGit, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__GIT, newGit, newGit));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Maven getMaven() {
		return maven;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetMaven(Maven newMaven, NotificationChain msgs) {
		Maven oldMaven = maven;
		maven = newMaven;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__MAVEN, oldMaven, newMaven);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMaven(Maven newMaven) {
		if (newMaven != maven) {
			NotificationChain msgs = null;
			if (maven != null)
				msgs = ((InternalEObject)maven).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__MAVEN, null, msgs);
			if (newMaven != null)
				msgs = ((InternalEObject)newMaven).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__MAVEN, null, msgs);
			msgs = basicSetMaven(newMaven, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__MAVEN, newMaven, newMaven));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public LLM getLlm() {
		return llm;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetLlm(LLM newLlm, NotificationChain msgs) {
		LLM oldLlm = llm;
		llm = newLlm;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__LLM, oldLlm, newLlm);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setLlm(LLM newLlm) {
		if (newLlm != llm) {
			NotificationChain msgs = null;
			if (llm != null)
				msgs = ((InternalEObject)llm).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__LLM, null, msgs);
			if (newLlm != null)
				msgs = ((InternalEObject)newLlm).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__LLM, null, msgs);
			msgs = basicSetLlm(newLlm, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__LLM, newLlm, newLlm));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public eu.kalafatic.evolution.model.orchestration.Compiler getCompiler() {
		return compiler;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetCompiler(eu.kalafatic.evolution.model.orchestration.Compiler newCompiler, NotificationChain msgs) {
		eu.kalafatic.evolution.model.orchestration.Compiler oldCompiler = compiler;
		compiler = newCompiler;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__COMPILER, oldCompiler, newCompiler);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCompiler(eu.kalafatic.evolution.model.orchestration.Compiler newCompiler) {
		if (newCompiler != compiler) {
			NotificationChain msgs = null;
			if (compiler != null)
				msgs = ((InternalEObject)compiler).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__COMPILER, null, msgs);
			if (newCompiler != null)
				msgs = ((InternalEObject)newCompiler).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__COMPILER, null, msgs);
			msgs = basicSetCompiler(newCompiler, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__COMPILER, newCompiler, newCompiler));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Ollama getOllama() {
		return ollama;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetOllama(Ollama newOllama, NotificationChain msgs) {
		Ollama oldOllama = ollama;
		ollama = newOllama;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__OLLAMA, oldOllama, newOllama);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setOllama(Ollama newOllama) {
		if (newOllama != ollama) {
			NotificationChain msgs = null;
			if (ollama != null)
				msgs = ((InternalEObject)ollama).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__OLLAMA, null, msgs);
			if (newOllama != null)
				msgs = ((InternalEObject)newOllama).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__OLLAMA, null, msgs);
			msgs = basicSetOllama(newOllama, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__OLLAMA, newOllama, newOllama));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public AiChat getAiChat() {
		return aiChat;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetAiChat(AiChat newAiChat, NotificationChain msgs) {
		AiChat oldAiChat = aiChat;
		aiChat = newAiChat;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__AI_CHAT, oldAiChat, newAiChat);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAiChat(AiChat newAiChat) {
		if (newAiChat != aiChat) {
			NotificationChain msgs = null;
			if (aiChat != null)
				msgs = ((InternalEObject)aiChat).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__AI_CHAT, null, msgs);
			if (newAiChat != null)
				msgs = ((InternalEObject)newAiChat).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__AI_CHAT, null, msgs);
			msgs = basicSetAiChat(newAiChat, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__AI_CHAT, newAiChat, newAiChat));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NeuronAI getNeuronAI() {
		return neuronAI;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetNeuronAI(NeuronAI newNeuronAI, NotificationChain msgs) {
		NeuronAI oldNeuronAI = neuronAI;
		neuronAI = newNeuronAI;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__NEURON_AI, oldNeuronAI, newNeuronAI);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setNeuronAI(NeuronAI newNeuronAI) {
		if (newNeuronAI != neuronAI) {
			NotificationChain msgs = null;
			if (neuronAI != null)
				msgs = ((InternalEObject)neuronAI).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__NEURON_AI, null, msgs);
			if (newNeuronAI != null)
				msgs = ((InternalEObject)newNeuronAI).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__NEURON_AI, null, msgs);
			msgs = basicSetNeuronAI(newNeuronAI, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__NEURON_AI, newNeuronAI, newNeuronAI));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getRemoteModel() {
		return remoteModel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRemoteModel(String newRemoteModel) {
		String oldRemoteModel = remoteModel;
		remoteModel = newRemoteModel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__REMOTE_MODEL, oldRemoteModel, remoteModel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public AiMode getAiMode() {
		return aiMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAiMode(AiMode newAiMode) {
		AiMode oldAiMode = aiMode;
		aiMode = newAiMode == null ? AI_MODE_EDEFAULT : newAiMode;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__AI_MODE, oldAiMode, aiMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getMcpServerUrl() {
		return mcpServerUrl;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMcpServerUrl(String newMcpServerUrl) {
		String oldMcpServerUrl = mcpServerUrl;
		mcpServerUrl = newMcpServerUrl;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__MCP_SERVER_URL, oldMcpServerUrl, mcpServerUrl));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getOpenAiToken() {
		return openAiToken;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setOpenAiToken(String newOpenAiToken) {
		String oldOpenAiToken = openAiToken;
		openAiToken = newOpenAiToken;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__OPEN_AI_TOKEN, oldOpenAiToken, openAiToken));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getOpenAiModel() {
		return openAiModel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setOpenAiModel(String newOpenAiModel) {
		String oldOpenAiModel = openAiModel;
		openAiModel = newOpenAiModel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__OPEN_AI_MODEL, oldOpenAiModel, openAiModel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getLocalModel() {
		return localModel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setLocalModel(String newLocalModel) {
		String oldLocalModel = localModel;
		localModel = newLocalModel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__LOCAL_MODEL, oldLocalModel, localModel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getHybridModel() {
		return hybridModel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setHybridModel(String newHybridModel) {
		String oldHybridModel = hybridModel;
		hybridModel = newHybridModel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__HYBRID_MODEL, oldHybridModel, hybridModel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isOfflineMode() {
		return offlineMode;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setOfflineMode(boolean newOfflineMode) {
		boolean oldOfflineMode = offlineMode;
		offlineMode = newOfflineMode;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__OFFLINE_MODE, oldOfflineMode, offlineMode));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public SelfDevSession getSelfDevSession() {
		return selfDevSession;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetSelfDevSession(SelfDevSession newSelfDevSession, NotificationChain msgs) {
		SelfDevSession oldSelfDevSession = selfDevSession;
		selfDevSession = newSelfDevSession;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__SELF_DEV_SESSION, oldSelfDevSession, newSelfDevSession);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSelfDevSession(SelfDevSession newSelfDevSession) {
		if (newSelfDevSession != selfDevSession) {
			NotificationChain msgs = null;
			if (selfDevSession != null)
				msgs = ((InternalEObject)selfDevSession).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__SELF_DEV_SESSION, null, msgs);
			if (newSelfDevSession != null)
				msgs = ((InternalEObject)newSelfDevSession).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.ORCHESTRATOR__SELF_DEV_SESSION, null, msgs);
			msgs = basicSetSelfDevSession(newSelfDevSession, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__SELF_DEV_SESSION, newSelfDevSession, newSelfDevSession));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getSharedMemory() {
		return sharedMemory;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSharedMemory(String newSharedMemory) {
		String oldSharedMemory = sharedMemory;
		sharedMemory = newSharedMemory;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ORCHESTRATOR__SHARED_MEMORY, oldSharedMemory, sharedMemory));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case OrchestrationPackage.ORCHESTRATOR__AGENTS:
				return ((InternalEList<?>)getAgents()).basicRemove(otherEnd, msgs);
			case OrchestrationPackage.ORCHESTRATOR__TASKS:
				return ((InternalEList<?>)getTasks()).basicRemove(otherEnd, msgs);
			case OrchestrationPackage.ORCHESTRATOR__GIT:
				return basicSetGit(null, msgs);
			case OrchestrationPackage.ORCHESTRATOR__MAVEN:
				return basicSetMaven(null, msgs);
			case OrchestrationPackage.ORCHESTRATOR__LLM:
				return basicSetLlm(null, msgs);
			case OrchestrationPackage.ORCHESTRATOR__COMPILER:
				return basicSetCompiler(null, msgs);
			case OrchestrationPackage.ORCHESTRATOR__OLLAMA:
				return basicSetOllama(null, msgs);
			case OrchestrationPackage.ORCHESTRATOR__AI_CHAT:
				return basicSetAiChat(null, msgs);
			case OrchestrationPackage.ORCHESTRATOR__NEURON_AI:
				return basicSetNeuronAI(null, msgs);
			case OrchestrationPackage.ORCHESTRATOR__SELF_DEV_SESSION:
				return basicSetSelfDevSession(null, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.ORCHESTRATOR__ID:
				return getId();
			case OrchestrationPackage.ORCHESTRATOR__NAME:
				return getName();
			case OrchestrationPackage.ORCHESTRATOR__AGENTS:
				return getAgents();
			case OrchestrationPackage.ORCHESTRATOR__TASKS:
				return getTasks();
			case OrchestrationPackage.ORCHESTRATOR__GIT:
				return getGit();
			case OrchestrationPackage.ORCHESTRATOR__MAVEN:
				return getMaven();
			case OrchestrationPackage.ORCHESTRATOR__LLM:
				return getLlm();
			case OrchestrationPackage.ORCHESTRATOR__COMPILER:
				return getCompiler();
			case OrchestrationPackage.ORCHESTRATOR__OLLAMA:
				return getOllama();
			case OrchestrationPackage.ORCHESTRATOR__AI_CHAT:
				return getAiChat();
			case OrchestrationPackage.ORCHESTRATOR__NEURON_AI:
				return getNeuronAI();
			case OrchestrationPackage.ORCHESTRATOR__REMOTE_MODEL:
				return getRemoteModel();
			case OrchestrationPackage.ORCHESTRATOR__AI_MODE:
				return getAiMode();
			case OrchestrationPackage.ORCHESTRATOR__MCP_SERVER_URL:
				return getMcpServerUrl();
			case OrchestrationPackage.ORCHESTRATOR__OPEN_AI_TOKEN:
				return getOpenAiToken();
			case OrchestrationPackage.ORCHESTRATOR__OPEN_AI_MODEL:
				return getOpenAiModel();
			case OrchestrationPackage.ORCHESTRATOR__LOCAL_MODEL:
				return getLocalModel();
			case OrchestrationPackage.ORCHESTRATOR__HYBRID_MODEL:
				return getHybridModel();
			case OrchestrationPackage.ORCHESTRATOR__OFFLINE_MODE:
				return isOfflineMode();
			case OrchestrationPackage.ORCHESTRATOR__SELF_DEV_SESSION:
				return getSelfDevSession();
			case OrchestrationPackage.ORCHESTRATOR__SHARED_MEMORY:
				return getSharedMemory();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.ORCHESTRATOR__ID:
				setId((String)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__NAME:
				setName((String)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__AGENTS:
				getAgents().clear();
				getAgents().addAll((Collection<? extends Agent>)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__TASKS:
				getTasks().clear();
				getTasks().addAll((Collection<? extends Task>)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__GIT:
				setGit((Git)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__MAVEN:
				setMaven((Maven)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__LLM:
				setLlm((LLM)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__COMPILER:
				setCompiler((eu.kalafatic.evolution.model.orchestration.Compiler)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__OLLAMA:
				setOllama((Ollama)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__AI_CHAT:
				setAiChat((AiChat)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__NEURON_AI:
				setNeuronAI((NeuronAI)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__REMOTE_MODEL:
				setRemoteModel((String)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__AI_MODE:
				setAiMode((AiMode)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__MCP_SERVER_URL:
				setMcpServerUrl((String)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__OPEN_AI_TOKEN:
				setOpenAiToken((String)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__OPEN_AI_MODEL:
				setOpenAiModel((String)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__LOCAL_MODEL:
				setLocalModel((String)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__HYBRID_MODEL:
				setHybridModel((String)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__OFFLINE_MODE:
				setOfflineMode((Boolean)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__SELF_DEV_SESSION:
				setSelfDevSession((SelfDevSession)newValue);
				return;
			case OrchestrationPackage.ORCHESTRATOR__SHARED_MEMORY:
				setSharedMemory((String)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.ORCHESTRATOR__ID:
				setId(ID_EDEFAULT);
				return;
			case OrchestrationPackage.ORCHESTRATOR__NAME:
				setName(NAME_EDEFAULT);
				return;
			case OrchestrationPackage.ORCHESTRATOR__AGENTS:
				getAgents().clear();
				return;
			case OrchestrationPackage.ORCHESTRATOR__TASKS:
				getTasks().clear();
				return;
			case OrchestrationPackage.ORCHESTRATOR__GIT:
				setGit((Git)null);
				return;
			case OrchestrationPackage.ORCHESTRATOR__MAVEN:
				setMaven((Maven)null);
				return;
			case OrchestrationPackage.ORCHESTRATOR__LLM:
				setLlm((LLM)null);
				return;
			case OrchestrationPackage.ORCHESTRATOR__COMPILER:
				setCompiler((eu.kalafatic.evolution.model.orchestration.Compiler)null);
				return;
			case OrchestrationPackage.ORCHESTRATOR__OLLAMA:
				setOllama((Ollama)null);
				return;
			case OrchestrationPackage.ORCHESTRATOR__AI_CHAT:
				setAiChat((AiChat)null);
				return;
			case OrchestrationPackage.ORCHESTRATOR__NEURON_AI:
				setNeuronAI((NeuronAI)null);
				return;
			case OrchestrationPackage.ORCHESTRATOR__REMOTE_MODEL:
				setRemoteModel(REMOTE_MODEL_EDEFAULT);
				return;
			case OrchestrationPackage.ORCHESTRATOR__AI_MODE:
				setAiMode(AI_MODE_EDEFAULT);
				return;
			case OrchestrationPackage.ORCHESTRATOR__MCP_SERVER_URL:
				setMcpServerUrl(MCP_SERVER_URL_EDEFAULT);
				return;
			case OrchestrationPackage.ORCHESTRATOR__OPEN_AI_TOKEN:
				setOpenAiToken(OPEN_AI_TOKEN_EDEFAULT);
				return;
			case OrchestrationPackage.ORCHESTRATOR__OPEN_AI_MODEL:
				setOpenAiModel(OPEN_AI_MODEL_EDEFAULT);
				return;
			case OrchestrationPackage.ORCHESTRATOR__LOCAL_MODEL:
				setLocalModel(LOCAL_MODEL_EDEFAULT);
				return;
			case OrchestrationPackage.ORCHESTRATOR__HYBRID_MODEL:
				setHybridModel(HYBRID_MODEL_EDEFAULT);
				return;
			case OrchestrationPackage.ORCHESTRATOR__OFFLINE_MODE:
				setOfflineMode(OFFLINE_MODE_EDEFAULT);
				return;
			case OrchestrationPackage.ORCHESTRATOR__SELF_DEV_SESSION:
				setSelfDevSession((SelfDevSession)null);
				return;
			case OrchestrationPackage.ORCHESTRATOR__SHARED_MEMORY:
				setSharedMemory(SHARED_MEMORY_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.ORCHESTRATOR__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case OrchestrationPackage.ORCHESTRATOR__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case OrchestrationPackage.ORCHESTRATOR__AGENTS:
				return agents != null && !agents.isEmpty();
			case OrchestrationPackage.ORCHESTRATOR__TASKS:
				return tasks != null && !tasks.isEmpty();
			case OrchestrationPackage.ORCHESTRATOR__GIT:
				return git != null;
			case OrchestrationPackage.ORCHESTRATOR__MAVEN:
				return maven != null;
			case OrchestrationPackage.ORCHESTRATOR__LLM:
				return llm != null;
			case OrchestrationPackage.ORCHESTRATOR__COMPILER:
				return compiler != null;
			case OrchestrationPackage.ORCHESTRATOR__OLLAMA:
				return ollama != null;
			case OrchestrationPackage.ORCHESTRATOR__AI_CHAT:
				return aiChat != null;
			case OrchestrationPackage.ORCHESTRATOR__NEURON_AI:
				return neuronAI != null;
			case OrchestrationPackage.ORCHESTRATOR__REMOTE_MODEL:
				return REMOTE_MODEL_EDEFAULT == null ? remoteModel != null : !REMOTE_MODEL_EDEFAULT.equals(remoteModel);
			case OrchestrationPackage.ORCHESTRATOR__AI_MODE:
				return aiMode != AI_MODE_EDEFAULT;
			case OrchestrationPackage.ORCHESTRATOR__MCP_SERVER_URL:
				return MCP_SERVER_URL_EDEFAULT == null ? mcpServerUrl != null : !MCP_SERVER_URL_EDEFAULT.equals(mcpServerUrl);
			case OrchestrationPackage.ORCHESTRATOR__OPEN_AI_TOKEN:
				return OPEN_AI_TOKEN_EDEFAULT == null ? openAiToken != null : !OPEN_AI_TOKEN_EDEFAULT.equals(openAiToken);
			case OrchestrationPackage.ORCHESTRATOR__OPEN_AI_MODEL:
				return OPEN_AI_MODEL_EDEFAULT == null ? openAiModel != null : !OPEN_AI_MODEL_EDEFAULT.equals(openAiModel);
			case OrchestrationPackage.ORCHESTRATOR__LOCAL_MODEL:
				return LOCAL_MODEL_EDEFAULT == null ? localModel != null : !LOCAL_MODEL_EDEFAULT.equals(localModel);
			case OrchestrationPackage.ORCHESTRATOR__HYBRID_MODEL:
				return HYBRID_MODEL_EDEFAULT == null ? hybridModel != null : !HYBRID_MODEL_EDEFAULT.equals(hybridModel);
			case OrchestrationPackage.ORCHESTRATOR__OFFLINE_MODE:
				return offlineMode != OFFLINE_MODE_EDEFAULT;
			case OrchestrationPackage.ORCHESTRATOR__SELF_DEV_SESSION:
				return selfDevSession != null;
			case OrchestrationPackage.ORCHESTRATOR__SHARED_MEMORY:
				return SHARED_MEMORY_EDEFAULT == null ? sharedMemory != null : !SHARED_MEMORY_EDEFAULT.equals(sharedMemory);
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (id: ");
		result.append(id);
		result.append(", name: ");
		result.append(name);
		result.append(", remoteModel: ");
		result.append(remoteModel);
		result.append(", aiMode: ");
		result.append(aiMode);
		result.append(", mcpServerUrl: ");
		result.append(mcpServerUrl);
		result.append(", openAiToken: ");
		result.append(openAiToken);
		result.append(", openAiModel: ");
		result.append(openAiModel);
		result.append(", localModel: ");
		result.append(localModel);
		result.append(", hybridModel: ");
		result.append(hybridModel);
		result.append(", offlineMode: ");
		result.append(offlineMode);
		result.append(", sharedMemory: ");
		result.append(sharedMemory);
		result.append(')');
		return result.toString();
	}

} //OrchestratorImpl
