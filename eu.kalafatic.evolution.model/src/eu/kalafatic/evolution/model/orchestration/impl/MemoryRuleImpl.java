/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.MemoryRule;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Memory Rule</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.MemoryRuleImpl#getStorageLimit <em>Storage Limit</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.MemoryRuleImpl#getRetentionPeriod <em>Retention Period</em>}</li>
 * </ul>
 *
 * @generated
 */
public class MemoryRuleImpl extends RuleImpl implements MemoryRule {
	/**
	 * The default value of the '{@link #getStorageLimit() <em>Storage Limit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStorageLimit()
	 * @generated
	 * @ordered
	 */
	protected static final int STORAGE_LIMIT_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getStorageLimit() <em>Storage Limit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStorageLimit()
	 * @generated
	 * @ordered
	 */
	protected int storageLimit = STORAGE_LIMIT_EDEFAULT;

	/**
	 * The default value of the '{@link #getRetentionPeriod() <em>Retention Period</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRetentionPeriod()
	 * @generated
	 * @ordered
	 */
	protected static final int RETENTION_PERIOD_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getRetentionPeriod() <em>Retention Period</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRetentionPeriod()
	 * @generated
	 * @ordered
	 */
	protected int retentionPeriod = RETENTION_PERIOD_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MemoryRuleImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.MEMORY_RULE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getStorageLimit() {
		return storageLimit;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setStorageLimit(int newStorageLimit) {
		int oldStorageLimit = storageLimit;
		storageLimit = newStorageLimit;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.MEMORY_RULE__STORAGE_LIMIT, oldStorageLimit, storageLimit));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getRetentionPeriod() {
		return retentionPeriod;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRetentionPeriod(int newRetentionPeriod) {
		int oldRetentionPeriod = retentionPeriod;
		retentionPeriod = newRetentionPeriod;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.MEMORY_RULE__RETENTION_PERIOD, oldRetentionPeriod, retentionPeriod));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.MEMORY_RULE__STORAGE_LIMIT:
				return getStorageLimit();
			case OrchestrationPackage.MEMORY_RULE__RETENTION_PERIOD:
				return getRetentionPeriod();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.MEMORY_RULE__STORAGE_LIMIT:
				setStorageLimit((Integer)newValue);
				return;
			case OrchestrationPackage.MEMORY_RULE__RETENTION_PERIOD:
				setRetentionPeriod((Integer)newValue);
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
			case OrchestrationPackage.MEMORY_RULE__STORAGE_LIMIT:
				setStorageLimit(STORAGE_LIMIT_EDEFAULT);
				return;
			case OrchestrationPackage.MEMORY_RULE__RETENTION_PERIOD:
				setRetentionPeriod(RETENTION_PERIOD_EDEFAULT);
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
			case OrchestrationPackage.MEMORY_RULE__STORAGE_LIMIT:
				return storageLimit != STORAGE_LIMIT_EDEFAULT;
			case OrchestrationPackage.MEMORY_RULE__RETENTION_PERIOD:
				return retentionPeriod != RETENTION_PERIOD_EDEFAULT;
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
		result.append(" (storageLimit: ");
		result.append(storageLimit);
		result.append(", retentionPeriod: ");
		result.append(retentionPeriod);
		result.append(')');
		return result.toString();
	}

} //MemoryRuleImpl
