package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.SecretRule;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import java.util.Collection;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

/**
 * @generated NOT
 */
public class SecretRuleImpl extends RuleImpl implements SecretRule {
	protected EList<String> allowedSecrets;

	protected SecretRuleImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.SECRET_RULE;
	}

	@Override
	public EList<String> getAllowedSecrets() {
		if (allowedSecrets == null) {
			allowedSecrets = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.SECRET_RULE__ALLOWED_SECRETS);
		}
		return allowedSecrets;
	}

	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.SECRET_RULE__ALLOWED_SECRETS:
				return getAllowedSecrets();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.SECRET_RULE__ALLOWED_SECRETS:
				getAllowedSecrets().clear();
				getAllowedSecrets().addAll((Collection<? extends String>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.SECRET_RULE__ALLOWED_SECRETS:
				getAllowedSecrets().clear();
				return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.SECRET_RULE__ALLOWED_SECRETS:
				return allowedSecrets != null && !allowedSecrets.isEmpty();
		}
		return super.eIsSet(featureID);
	}

} // SecretRuleImpl
