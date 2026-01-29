package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.NetworkRule;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import java.util.Collection;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

/**
 * @generated NOT
 */
public class NetworkRuleImpl extends RuleImpl implements NetworkRule {
	protected EList<String> allowedDomains;
	protected boolean allowAll = false;

	protected NetworkRuleImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.NETWORK_RULE;
	}

	@Override
	public EList<String> getAllowedDomains() {
		if (allowedDomains == null) {
			allowedDomains = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.NETWORK_RULE__ALLOWED_DOMAINS);
		}
		return allowedDomains;
	}

	@Override
	public boolean isAllowAll() {
		return allowAll;
	}

	@Override
	public void setAllowAll(boolean newAllowAll) {
		boolean oldAllowAll = allowAll;
		allowAll = newAllowAll;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.NETWORK_RULE__ALLOW_ALL, oldAllowAll, allowAll));
	}

	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.NETWORK_RULE__ALLOWED_DOMAINS:
				return getAllowedDomains();
			case OrchestrationPackage.NETWORK_RULE__ALLOW_ALL:
				return isAllowAll();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.NETWORK_RULE__ALLOWED_DOMAINS:
				getAllowedDomains().clear();
				getAllowedDomains().addAll((Collection<? extends String>)newValue);
				return;
			case OrchestrationPackage.NETWORK_RULE__ALLOW_ALL:
				setAllowAll((Boolean)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.NETWORK_RULE__ALLOWED_DOMAINS:
				getAllowedDomains().clear();
				return;
			case OrchestrationPackage.NETWORK_RULE__ALLOW_ALL:
				setAllowAll(false);
				return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.NETWORK_RULE__ALLOWED_DOMAINS:
				return allowedDomains != null && !allowedDomains.isEmpty();
			case OrchestrationPackage.NETWORK_RULE__ALLOW_ALL:
				return allowAll != false;
		}
		return super.eIsSet(featureID);
	}

} // NetworkRuleImpl
