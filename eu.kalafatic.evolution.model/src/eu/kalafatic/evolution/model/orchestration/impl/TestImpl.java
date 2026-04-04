package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Test;
import eu.kalafatic.evolution.model.orchestration.TestStatus;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * @generated NOT
 */
public class TestImpl extends MinimalEObjectImpl.Container implements Test {
	protected String id = null;
	protected String name = null;
	protected String type = null;
	protected String path = null;
	protected TestStatus status = TestStatus.PENDING;

	protected TestImpl() { super(); }

	@Override
	protected EClass eStaticClass() { return OrchestrationPackage.Literals.TEST; }

	@Override
	public String getId() { return id; }
	@Override
	public void setId(String newId) {
		String oldId = id;
		id = newId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TEST__ID, oldId, id));
	}

	@Override
	public String getName() { return name; }
	@Override
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TEST__NAME, oldName, name));
	}

	@Override
	public String getType() { return type; }
	@Override
	public void setType(String newType) {
		String oldType = type;
		type = newType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TEST__TYPE, oldType, type));
	}

	@Override
	public String getPath() { return path; }
	@Override
	public void setPath(String newPath) {
		String oldPath = path;
		path = newPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TEST__PATH, oldPath, path));
	}

	@Override
	public TestStatus getStatus() { return status; }
	@Override
	public void setStatus(TestStatus newStatus) {
		TestStatus oldStatus = status;
		status = newStatus == null ? TestStatus.PENDING : newStatus;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.TEST__STATUS, oldStatus, status));
	}

	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.TEST__ID: return getId();
			case OrchestrationPackage.TEST__NAME: return getName();
			case OrchestrationPackage.TEST__TYPE: return getType();
			case OrchestrationPackage.TEST__PATH: return getPath();
			case OrchestrationPackage.TEST__STATUS: return getStatus();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.TEST__ID: setId((String)newValue); return;
			case OrchestrationPackage.TEST__NAME: setName((String)newValue); return;
			case OrchestrationPackage.TEST__TYPE: setType((String)newValue); return;
			case OrchestrationPackage.TEST__PATH: setPath((String)newValue); return;
			case OrchestrationPackage.TEST__STATUS: setStatus((TestStatus)newValue); return;
		}
		super.eSet(featureID, newValue);
	}

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.TEST__ID: setId(null); return;
			case OrchestrationPackage.TEST__NAME: setName(null); return;
			case OrchestrationPackage.TEST__TYPE: setType(null); return;
			case OrchestrationPackage.TEST__PATH: setPath(null); return;
			case OrchestrationPackage.TEST__STATUS: setStatus(TestStatus.PENDING); return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.TEST__ID: return id != null;
			case OrchestrationPackage.TEST__NAME: return name != null;
			case OrchestrationPackage.TEST__TYPE: return type != null;
			case OrchestrationPackage.TEST__PATH: return path != null;
			case OrchestrationPackage.TEST__STATUS: return status != TestStatus.PENDING;
		}
		return super.eIsSet(featureID);
	}
}
