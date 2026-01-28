package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.MemoryRule;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * @generated NOT
 */
public class MemoryRuleImpl extends RuleImpl implements MemoryRule {
	protected int storageLimit = 0;
	protected int retentionPeriod = 0;

	protected MemoryRuleImpl() {
		super();
	}

	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.MEMORY_RULE;
	}

	@Override
	public int getStorageLimit() {
		return storageLimit;
	}

	@Override
	public void setStorageLimit(int newStorageLimit) {
		int oldStorageLimit = storageLimit;
		storageLimit = newStorageLimit;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.MEMORY_RULE__STORAGE_LIMIT, oldStorageLimit, storageLimit));
	}

	@Override
	public int getRetentionPeriod() {
		return retentionPeriod;
	}

	@Override
	public void setRetentionPeriod(int newRetentionPeriod) {
		int oldRetentionPeriod = retentionPeriod;
		retentionPeriod = newRetentionPeriod;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.MEMORY_RULE__RETENTION_PERIOD, oldRetentionPeriod, retentionPeriod));
	}

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

	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.MEMORY_RULE__STORAGE_LIMIT:
				setStorageLimit(0);
				return;
			case OrchestrationPackage.MEMORY_RULE__RETENTION_PERIOD:
				setRetentionPeriod(0);
				return;
		}
		super.eUnset(featureID);
	}

	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.MEMORY_RULE__STORAGE_LIMIT:
				return storageLimit != 0;
			case OrchestrationPackage.MEMORY_RULE__RETENTION_PERIOD:
				return retentionPeriod != 0;
		}
		return super.eIsSet(featureID);
	}

} // MemoryRuleImpl
