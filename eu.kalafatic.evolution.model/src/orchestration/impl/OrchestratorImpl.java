/**
 */
package orchestration.impl;

import java.util.Collection;

import orchestration.Agent;
import orchestration.Git;
import orchestration.LLM;
import orchestration.Maven;
import orchestration.OrchestrationPackage;
import orchestration.Orchestrator;

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
 *   <li>{@link orchestration.impl.OrchestratorImpl#getId <em>Id</em>}</li>
 *   <li>{@link orchestration.impl.OrchestratorImpl#getName <em>Name</em>}</li>
 *   <li>{@link orchestration.impl.OrchestratorImpl#getAgents <em>Agents</em>}</li>
 *   <li>{@link orchestration.impl.OrchestratorImpl#getGit <em>Git</em>}</li>
 *   <li>{@link orchestration.impl.OrchestratorImpl#getMaven <em>Maven</em>}</li>
 *   <li>{@link orchestration.impl.OrchestratorImpl#getLlm <em>Llm</em>}</li>
 *   <li>{@link orchestration.impl.OrchestratorImpl#getCompiler <em>Compiler</em>}</li>
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
	protected orchestration.Compiler compiler;

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
	public String getId() {
		return id;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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
	public orchestration.Compiler getCompiler() {
		return compiler;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public NotificationChain basicSetCompiler(orchestration.Compiler newCompiler, NotificationChain msgs) {
		orchestration.Compiler oldCompiler = compiler;
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
	public void setCompiler(orchestration.Compiler newCompiler) {
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
			case OrchestrationPackage.ORCHESTRATOR__GIT:
				return basicSetGit(null, msgs);
			case OrchestrationPackage.ORCHESTRATOR__MAVEN:
				return basicSetMaven(null, msgs);
			case OrchestrationPackage.ORCHESTRATOR__LLM:
				return basicSetLlm(null, msgs);
			case OrchestrationPackage.ORCHESTRATOR__COMPILER:
				return basicSetCompiler(null, msgs);
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
			case OrchestrationPackage.ORCHESTRATOR__GIT:
				return getGit();
			case OrchestrationPackage.ORCHESTRATOR__MAVEN:
				return getMaven();
			case OrchestrationPackage.ORCHESTRATOR__LLM:
				return getLlm();
			case OrchestrationPackage.ORCHESTRATOR__COMPILER:
				return getCompiler();
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
				setCompiler((orchestration.Compiler)newValue);
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
				setCompiler((orchestration.Compiler)null);
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
			case OrchestrationPackage.ORCHESTRATOR__GIT:
				return git != null;
			case OrchestrationPackage.ORCHESTRATOR__MAVEN:
				return maven != null;
			case OrchestrationPackage.ORCHESTRATOR__LLM:
				return llm != null;
			case OrchestrationPackage.ORCHESTRATOR__COMPILER:
				return compiler != null;
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
