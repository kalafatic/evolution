package eu.kalafatic.evolution.model.orchestration.impl;

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

import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.ForgeStatus;
import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.SessionExperiment;
import eu.kalafatic.evolution.model.orchestration.SessionModelState;
import eu.kalafatic.evolution.model.orchestration.SessionSnapshot;

public class ForgeSessionImpl extends MinimalEObjectImpl.Container implements ForgeSession {
	protected static final String SESSION_ID_EDEFAULT = null;
	protected String sessionId = SESSION_ID_EDEFAULT;
	protected static final String NAME_EDEFAULT = null;
	protected String name = NAME_EDEFAULT;
	protected static final long CREATED_AT_EDEFAULT = 0L;
	protected long createdAt = CREATED_AT_EDEFAULT;
	protected static final long LAST_MODIFIED_EDEFAULT = 0L;
	protected long lastModified = LAST_MODIFIED_EDEFAULT;
	protected static final ForgeStatus STATUS_EDEFAULT = eu.kalafatic.evolution.model.orchestration.ForgeStatus.IDLE;
	protected ForgeStatus status = STATUS_EDEFAULT;
	protected static final String ACTIVE_MODEL_ID_EDEFAULT = null;
	protected String activeModelId = ACTIVE_MODEL_ID_EDEFAULT;
	protected static final String SELECTED_MODEL_TYPE_EDEFAULT = null;
	protected String selectedModelType = SELECTED_MODEL_TYPE_EDEFAULT;
	protected Git git;
	protected SessionModelState modelState;
	protected EList<SessionExperiment> experiments;
	protected EList<SessionSnapshot> snapshots;

	protected ForgeSessionImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.FORGE_SESSION;
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	@Override
	public void setSessionId(String newSessionId) {
		String oldSessionId = sessionId;
		sessionId = newSessionId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FORGE_SESSION__SESSION_ID, oldSessionId, sessionId));
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FORGE_SESSION__NAME, oldName, name));
	}

	@Override
	public long getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(long newCreatedAt) {
		long oldCreatedAt = createdAt;
		createdAt = newCreatedAt;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FORGE_SESSION__CREATED_AT, oldCreatedAt, createdAt));
	}

	@Override
	public long getLastModified() {
		return lastModified;
	}

	@Override
	public void setLastModified(long newLastModified) {
		long oldLastModified = lastModified;
		lastModified = newLastModified;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FORGE_SESSION__LAST_MODIFIED, oldLastModified, lastModified));
	}

	@Override
	public ForgeStatus getStatus() {
		return status;
	}

	@Override
	public void setStatus(ForgeStatus newStatus) {
		ForgeStatus oldStatus = status;
		status = newStatus == null ? STATUS_EDEFAULT : newStatus;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FORGE_SESSION__STATUS, oldStatus, status));
	}

	@Override
	public String getActiveModelId() {
		return activeModelId;
	}

	@Override
	public void setActiveModelId(String newActiveModelId) {
		String oldActiveModelId = activeModelId;
		activeModelId = newActiveModelId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FORGE_SESSION__ACTIVE_MODEL_ID, oldActiveModelId, activeModelId));
	}

	@Override
	public String getSelectedModelType() {
		return selectedModelType;
	}

	@Override
	public void setSelectedModelType(String newSelectedModelType) {
		String oldSelectedModelType = selectedModelType;
		selectedModelType = newSelectedModelType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FORGE_SESSION__SELECTED_MODEL_TYPE, oldSelectedModelType, selectedModelType));
	}

	@Override
	public Git getGit() {
		return git;
	}

	public NotificationChain basicSetGit(Git newGit, NotificationChain msgs) {
		Git oldGit = git;
		git = newGit;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FORGE_SESSION__GIT, oldGit, newGit);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	@Override
	public void setGit(Git newGit) {
		if (newGit != git) {
			NotificationChain msgs = null;
			if (git != null)
				msgs = ((InternalEObject)git).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.FORGE_SESSION__GIT, null, msgs);
			if (newGit != null)
				msgs = ((InternalEObject)newGit).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.FORGE_SESSION__GIT, null, msgs);
			msgs = basicSetGit(newGit, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FORGE_SESSION__GIT, newGit, newGit));
	}

	@Override
	public SessionModelState getModelState() {
		return modelState;
	}

	public NotificationChain basicSetModelState(SessionModelState newModelState, NotificationChain msgs) {
		SessionModelState oldModelState = modelState;
		modelState = newModelState;
		if (eNotificationRequired()) {
			ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FORGE_SESSION__MODEL_STATE, oldModelState, newModelState);
			if (msgs == null) msgs = notification; else msgs.add(notification);
		}
		return msgs;
	}

	@Override
	public void setModelState(SessionModelState newModelState) {
		if (newModelState != modelState) {
			NotificationChain msgs = null;
			if (modelState != null)
				msgs = ((InternalEObject)modelState).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.FORGE_SESSION__MODEL_STATE, null, msgs);
			if (newModelState != null)
				msgs = ((InternalEObject)newModelState).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - OrchestrationPackage.FORGE_SESSION__MODEL_STATE, null, msgs);
			msgs = basicSetModelState(newModelState, msgs);
			if (msgs != null) msgs.dispatch();
		}
		else if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.FORGE_SESSION__MODEL_STATE, newModelState, newModelState));
	}

	@Override
	public EList<SessionExperiment> getExperiments() {
		if (experiments == null) {
			experiments = new EObjectContainmentEList<SessionExperiment>(SessionExperiment.class, this, OrchestrationPackage.FORGE_SESSION__EXPERIMENTS);
		}
		return experiments;
	}

	@Override
	public EList<SessionSnapshot> getSnapshots() {
		if (snapshots == null) {
			snapshots = new EObjectContainmentEList<SessionSnapshot>(SessionSnapshot.class, this, OrchestrationPackage.FORGE_SESSION__SNAPSHOTS);
		}
		return snapshots;
	}

	@Override
	public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
		switch (featureID) {
			case OrchestrationPackage.FORGE_SESSION__GIT:
				return basicSetGit(null, msgs);
			case OrchestrationPackage.FORGE_SESSION__MODEL_STATE:
				return basicSetModelState(null, msgs);
			case OrchestrationPackage.FORGE_SESSION__EXPERIMENTS:
				return ((InternalEList<?>)getExperiments()).basicRemove(otherEnd, msgs);
			case OrchestrationPackage.FORGE_SESSION__SNAPSHOTS:
				return ((InternalEList<?>)getSnapshots()).basicRemove(otherEnd, msgs);
		}
		return super.eInverseRemove(otherEnd, featureID, msgs);
	}

	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.FORGE_SESSION__SESSION_ID:
				return getSessionId();
			case OrchestrationPackage.FORGE_SESSION__NAME:
				return getName();
			case OrchestrationPackage.FORGE_SESSION__CREATED_AT:
				return getCreatedAt();
			case OrchestrationPackage.FORGE_SESSION__LAST_MODIFIED:
				return getLastModified();
			case OrchestrationPackage.FORGE_SESSION__STATUS:
				return getStatus();
			case OrchestrationPackage.FORGE_SESSION__ACTIVE_MODEL_ID:
				return getActiveModelId();
			case OrchestrationPackage.FORGE_SESSION__SELECTED_MODEL_TYPE:
				return getSelectedModelType();
			case OrchestrationPackage.FORGE_SESSION__GIT:
				return getGit();
			case OrchestrationPackage.FORGE_SESSION__MODEL_STATE:
				return getModelState();
			case OrchestrationPackage.FORGE_SESSION__EXPERIMENTS:
				return getExperiments();
			case OrchestrationPackage.FORGE_SESSION__SNAPSHOTS:
				return getSnapshots();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.FORGE_SESSION__SESSION_ID:
				setSessionId((String)newValue);
				return;
			case OrchestrationPackage.FORGE_SESSION__NAME:
				setName((String)newValue);
				return;
			case OrchestrationPackage.FORGE_SESSION__CREATED_AT:
				setCreatedAt((Long)newValue);
				return;
			case OrchestrationPackage.FORGE_SESSION__LAST_MODIFIED:
				setLastModified((Long)newValue);
				return;
			case OrchestrationPackage.FORGE_SESSION__STATUS:
				setStatus((ForgeStatus)newValue);
				return;
			case OrchestrationPackage.FORGE_SESSION__ACTIVE_MODEL_ID:
				setActiveModelId((String)newValue);
				return;
			case OrchestrationPackage.FORGE_SESSION__SELECTED_MODEL_TYPE:
				setSelectedModelType((String)newValue);
				return;
			case OrchestrationPackage.FORGE_SESSION__GIT:
				setGit((Git)newValue);
				return;
			case OrchestrationPackage.FORGE_SESSION__MODEL_STATE:
				setModelState((SessionModelState)newValue);
				return;
			case OrchestrationPackage.FORGE_SESSION__EXPERIMENTS:
				getExperiments().clear();
				getExperiments().addAll((Collection<? extends SessionExperiment>)newValue);
				return;
			case OrchestrationPackage.FORGE_SESSION__SNAPSHOTS:
				getSnapshots().clear();
				getSnapshots().addAll((Collection<? extends SessionSnapshot>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.FORGE_SESSION__SESSION_ID:
				setSessionId(SESSION_ID_EDEFAULT);
				return;
			case OrchestrationPackage.FORGE_SESSION__NAME:
				setName(NAME_EDEFAULT);
				return;
			case OrchestrationPackage.FORGE_SESSION__CREATED_AT:
				setCreatedAt(CREATED_AT_EDEFAULT);
				return;
			case OrchestrationPackage.FORGE_SESSION__LAST_MODIFIED:
				setLastModified(LAST_MODIFIED_EDEFAULT);
				return;
			case OrchestrationPackage.FORGE_SESSION__STATUS:
				setStatus(STATUS_EDEFAULT);
				return;
			case OrchestrationPackage.FORGE_SESSION__ACTIVE_MODEL_ID:
				setActiveModelId(ACTIVE_MODEL_ID_EDEFAULT);
				return;
			case OrchestrationPackage.FORGE_SESSION__SELECTED_MODEL_TYPE:
				setSelectedModelType(SELECTED_MODEL_TYPE_EDEFAULT);
				return;
			case OrchestrationPackage.FORGE_SESSION__GIT:
				setGit((Git)null);
				return;
			case OrchestrationPackage.FORGE_SESSION__MODEL_STATE:
				setModelState((SessionModelState)null);
				return;
			case OrchestrationPackage.FORGE_SESSION__EXPERIMENTS:
				getExperiments().clear();
				return;
			case OrchestrationPackage.FORGE_SESSION__SNAPSHOTS:
				getSnapshots().clear();
				return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.FORGE_SESSION__SESSION_ID:
				return SESSION_ID_EDEFAULT == null ? sessionId != null : !SESSION_ID_EDEFAULT.equals(sessionId);
			case OrchestrationPackage.FORGE_SESSION__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case OrchestrationPackage.FORGE_SESSION__CREATED_AT:
				return createdAt != CREATED_AT_EDEFAULT;
			case OrchestrationPackage.FORGE_SESSION__LAST_MODIFIED:
				return lastModified != LAST_MODIFIED_EDEFAULT;
			case OrchestrationPackage.FORGE_SESSION__STATUS:
				return status != STATUS_EDEFAULT;
			case OrchestrationPackage.FORGE_SESSION__ACTIVE_MODEL_ID:
				return ACTIVE_MODEL_ID_EDEFAULT == null ? activeModelId != null : !ACTIVE_MODEL_ID_EDEFAULT.equals(activeModelId);
			case OrchestrationPackage.FORGE_SESSION__SELECTED_MODEL_TYPE:
				return SELECTED_MODEL_TYPE_EDEFAULT == null ? selectedModelType != null : !SELECTED_MODEL_TYPE_EDEFAULT.equals(selectedModelType);
			case OrchestrationPackage.FORGE_SESSION__GIT:
				return git != null;
			case OrchestrationPackage.FORGE_SESSION__MODEL_STATE:
				return modelState != null;
			case OrchestrationPackage.FORGE_SESSION__EXPERIMENTS:
				return experiments != null && !experiments.isEmpty();
			case OrchestrationPackage.FORGE_SESSION__SNAPSHOTS:
				return snapshots != null && !snapshots.isEmpty();
		}
		return super.eIsSet(featureID);
	}

	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();
		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (sessionId: ");
		result.append(sessionId);
		result.append(", name: ");
		result.append(name);
		result.append(", createdAt: ");
		result.append(createdAt);
		result.append(", lastModified: ");
		result.append(lastModified);
		result.append(", status: ");
		result.append(status);
		result.append(", activeModelId: ");
		result.append(activeModelId);
		result.append(", selectedModelType: ");
		result.append(selectedModelType);
		result.append(')');
		return result.toString();
	}
}
