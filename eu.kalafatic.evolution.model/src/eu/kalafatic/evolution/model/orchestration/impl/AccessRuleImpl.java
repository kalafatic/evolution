/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.AccessRule;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import java.util.Collection;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Access Rule</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AccessRuleImpl#getAllowedPaths <em>Allowed Paths</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.AccessRuleImpl#getDeniedPaths <em>Denied Paths</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AccessRuleImpl extends RuleImpl implements AccessRule {
	/**
	 * The cached value of the '{@link #getAllowedPaths() <em>Allowed Paths</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAllowedPaths()
	 * @generated
	 * @ordered
	 */
	protected EList<String> allowedPaths;

	/**
	 * The cached value of the '{@link #getDeniedPaths() <em>Denied Paths</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDeniedPaths()
	 * @generated
	 * @ordered
	 */
	protected EList<String> deniedPaths;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected AccessRuleImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.ACCESS_RULE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getAllowedPaths() {
		if (allowedPaths == null) {
			allowedPaths = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.ACCESS_RULE__ALLOWED_PATHS);
		}
		return allowedPaths;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getDeniedPaths() {
		if (deniedPaths == null) {
			deniedPaths = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.ACCESS_RULE__DENIED_PATHS);
		}
		return deniedPaths;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (allowedPaths: ");
		result.append(allowedPaths);
		result.append(", deniedPaths: ");
		result.append(deniedPaths);
		result.append(')');
		return result.toString();
	}

} //AccessRuleImpl
