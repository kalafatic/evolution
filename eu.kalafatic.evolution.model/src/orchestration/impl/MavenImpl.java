/**
 */
package orchestration.impl;

import java.util.Collection;

import orchestration.Maven;
import orchestration.OrchestrationPackage;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Maven</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link orchestration.impl.MavenImpl#getGoals <em>Goals</em>}</li>
 *   <li>{@link orchestration.impl.MavenImpl#getProfiles <em>Profiles</em>}</li>
 * </ul>
 *
 * @generated
 */
public class MavenImpl extends MinimalEObjectImpl.Container implements Maven {
	/**
	 * The cached value of the '{@link #getGoals() <em>Goals</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getGoals()
	 * @generated
	 * @ordered
	 */
	protected EList<String> goals;

	/**
	 * The cached value of the '{@link #getProfiles() <em>Profiles</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getProfiles()
	 * @generated
	 * @ordered
	 */
	protected EList<String> profiles;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MavenImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.MAVEN;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<String> getGoals() {
		if (goals == null) {
			goals = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.MAVEN__GOALS);
		}
		return goals;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public EList<String> getProfiles() {
		if (profiles == null) {
			profiles = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.MAVEN__PROFILES);
		}
		return profiles;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.MAVEN__GOALS:
				return getGoals();
			case OrchestrationPackage.MAVEN__PROFILES:
				return getProfiles();
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
			case OrchestrationPackage.MAVEN__GOALS:
				getGoals().clear();
				getGoals().addAll((Collection<? extends String>)newValue);
				return;
			case OrchestrationPackage.MAVEN__PROFILES:
				getProfiles().clear();
				getProfiles().addAll((Collection<? extends String>)newValue);
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
			case OrchestrationPackage.MAVEN__GOALS:
				getGoals().clear();
				return;
			case OrchestrationPackage.MAVEN__PROFILES:
				getProfiles().clear();
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
			case OrchestrationPackage.MAVEN__GOALS:
				return goals != null && !goals.isEmpty();
			case OrchestrationPackage.MAVEN__PROFILES:
				return profiles != null && !profiles.isEmpty();
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
		result.append(" (goals: ");
		result.append(goals);
		result.append(", profiles: ");
		result.append(profiles);
		result.append(')');
		return result.toString();
	}

} //MavenImpl
