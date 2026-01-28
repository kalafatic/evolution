package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Rule;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * @generated NOT
 */
public abstract class RuleImpl extends MinimalEObjectImpl.Container implements Rule {
	protected String name = null;
	protected String description = null;

	protected RuleImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.RULE;
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.RULE__NAME, oldName, name));
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String newDescription) {
		String oldDescription = description;
		description = newDescription;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.RULE__DESCRIPTION, oldDescription, description));
	}

    @Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.RULE__NAME:
				return getName();
			case OrchestrationPackage.RULE__DESCRIPTION:
				return getDescription();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.RULE__NAME:
				setName((String)newValue);
				return;
			case OrchestrationPackage.RULE__DESCRIPTION:
				setDescription((String)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.RULE__NAME:
				setName((String)null);
				return;
			case OrchestrationPackage.RULE__DESCRIPTION:
				setDescription((String)null);
				return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.RULE__NAME:
				return name != null;
			case OrchestrationPackage.RULE__DESCRIPTION:
				return description != null;
		}
		return super.eIsSet(featureID);
	}

} // RuleImpl
