package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.AccessRule;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import java.util.Collection;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

/**
 * @generated NOT
 */
public class AccessRuleImpl extends RuleImpl implements AccessRule {
	protected EList<String> allowedPaths;
	protected EList<String> deniedPaths;

	protected AccessRuleImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.ACCESS_RULE;
	}

	@Override
	public EList<String> getAllowedPaths() {
		if (allowedPaths == null) {
			allowedPaths = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.ACCESS_RULE__ALLOWED_PATHS);
		}
		return allowedPaths;
	}

	@Override
	public EList<String> getDeniedPaths() {
		if (deniedPaths == null) {
			deniedPaths = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.ACCESS_RULE__DENIED_PATHS);
		}
		return deniedPaths;
	}

	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.ACCESS_RULE__ALLOWED_PATHS:
				return getAllowedPaths();
			case OrchestrationPackage.ACCESS_RULE__DENIED_PATHS:
				return getDeniedPaths();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.ACCESS_RULE__ALLOWED_PATHS:
				getAllowedPaths().clear();
				getAllowedPaths().addAll((Collection<? extends String>)newValue);
				return;
			case OrchestrationPackage.ACCESS_RULE__DENIED_PATHS:
				getDeniedPaths().clear();
				getDeniedPaths().addAll((Collection<? extends String>)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.ACCESS_RULE__ALLOWED_PATHS:
				getAllowedPaths().clear();
				return;
			case OrchestrationPackage.ACCESS_RULE__DENIED_PATHS:
				getDeniedPaths().clear();
				return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.ACCESS_RULE__ALLOWED_PATHS:
				return allowedPaths != null && !allowedPaths.isEmpty();
			case OrchestrationPackage.ACCESS_RULE__DENIED_PATHS:
				return deniedPaths != null && !deniedPaths.isEmpty();
		}
		return super.eIsSet(featureID);
	}

} // AccessRuleImpl
