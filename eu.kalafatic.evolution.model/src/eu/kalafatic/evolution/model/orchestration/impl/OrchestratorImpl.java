/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.AiChat;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.LLM;
import eu.kalafatic.evolution.model.orchestration.Maven;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.Ollama;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

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
	protected EList<eu.kalafatic.evolution.model.orchestration.Task> tasks;

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
	 * @generated NOT
	 */
	@Override
	public EList<eu.kalafatic.evolution.model.orchestration.Task> getTasks() {
		if (tasks == null) {
			tasks = new EObjectContainmentEList<eu.kalafatic.evolution.model.orchestration.Task>(eu.kalafatic.evolution.model.orchestration.Task.class, this, OrchestrationPackage.ORCHESTRATOR__TASKS);
		}
		return tasks;
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
				getTasks().addAll((Collection<? extends eu.kalafatic.evolution.model.orchestration.Task>)newValue);
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
		result.append(')');
		return result.toString();
	}

} //OrchestratorImpl
