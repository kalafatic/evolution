/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

import eu.kalafatic.evolution.model.orchestration.NetworkRule;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Network Rule</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.NetworkRuleImpl#getAllowedDomains <em>Allowed Domains</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.NetworkRuleImpl#isAllowAll <em>Allow All</em>}</li>
 * </ul>
 *
 * @generated
 */
public class NetworkRuleImpl extends RuleImpl implements NetworkRule {
	/**
	 * The cached value of the '{@link #getAllowedDomains() <em>Allowed Domains</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAllowedDomains()
	 * @generated
	 * @ordered
	 */
	protected EList<String> allowedDomains;

	/**
	 * The default value of the '{@link #isAllowAll() <em>Allow All</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAllowAll()
	 * @generated
	 * @ordered
	 */
	protected static final boolean ALLOW_ALL_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isAllowAll() <em>Allow All</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAllowAll()
	 * @generated
	 * @ordered
	 */
	protected boolean allowAll = ALLOW_ALL_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected NetworkRuleImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.NETWORK_RULE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getAllowedDomains() {
		if (allowedDomains == null) {
			allowedDomains = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.NETWORK_RULE__ALLOWED_DOMAINS);
		}
		return allowedDomains;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isAllowAll() {
		return allowAll;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAllowAll(boolean newAllowAll) {
		boolean oldAllowAll = allowAll;
		allowAll = newAllowAll;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.NETWORK_RULE__ALLOW_ALL, oldAllowAll, allowAll));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.NETWORK_RULE__ALLOWED_DOMAINS:
				getAllowedDomains().clear();
				return;
			case OrchestrationPackage.NETWORK_RULE__ALLOW_ALL:
				setAllowAll(ALLOW_ALL_EDEFAULT);
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
			case OrchestrationPackage.NETWORK_RULE__ALLOWED_DOMAINS:
				return allowedDomains != null && !allowedDomains.isEmpty();
			case OrchestrationPackage.NETWORK_RULE__ALLOW_ALL:
				return allowAll != ALLOW_ALL_EDEFAULT;
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
		result.append(" (allowedDomains: ");
		result.append(allowedDomains);
		result.append(", allowAll: ");
		result.append(allowAll);
		result.append(')');
		return result.toString();
	}

} //NetworkRuleImpl
