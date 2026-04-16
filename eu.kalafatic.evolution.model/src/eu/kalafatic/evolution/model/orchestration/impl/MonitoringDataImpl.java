/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.MonitoringData;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Monitoring Data</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.MonitoringDataImpl#getTimestamp <em>Timestamp</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.MonitoringDataImpl#getCpuUsage <em>Cpu Usage</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.MonitoringDataImpl#getMemoryUsage <em>Memory Usage</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.MonitoringDataImpl#getTotalMemory <em>Total Memory</em>}</li>
 * </ul>
 *
 * @generated
 */
public class MonitoringDataImpl extends MinimalEObjectImpl.Container implements MonitoringData {
	/**
	 * The default value of the '{@link #getTimestamp() <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTimestamp()
	 * @generated
	 * @ordered
	 */
	protected static final long TIMESTAMP_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getTimestamp() <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTimestamp()
	 * @generated
	 * @ordered
	 */
	protected long timestamp = TIMESTAMP_EDEFAULT;

	/**
	 * The default value of the '{@link #getCpuUsage() <em>Cpu Usage</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCpuUsage()
	 * @generated
	 * @ordered
	 */
	protected static final double CPU_USAGE_EDEFAULT = 0.0;

	/**
	 * The cached value of the '{@link #getCpuUsage() <em>Cpu Usage</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCpuUsage()
	 * @generated
	 * @ordered
	 */
	protected double cpuUsage = CPU_USAGE_EDEFAULT;

	/**
	 * The default value of the '{@link #getMemoryUsage() <em>Memory Usage</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMemoryUsage()
	 * @generated
	 * @ordered
	 */
	protected static final long MEMORY_USAGE_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getMemoryUsage() <em>Memory Usage</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMemoryUsage()
	 * @generated
	 * @ordered
	 */
	protected long memoryUsage = MEMORY_USAGE_EDEFAULT;

	/**
	 * The default value of the '{@link #getTotalMemory() <em>Total Memory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTotalMemory()
	 * @generated
	 * @ordered
	 */
	protected static final long TOTAL_MEMORY_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getTotalMemory() <em>Total Memory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTotalMemory()
	 * @generated
	 * @ordered
	 */
	protected long totalMemory = TOTAL_MEMORY_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected MonitoringDataImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.MONITORING_DATA;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTimestamp(long newTimestamp) {
		long oldTimestamp = timestamp;
		timestamp = newTimestamp;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.MONITORING_DATA__TIMESTAMP, oldTimestamp, timestamp));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public double getCpuUsage() {
		return cpuUsage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCpuUsage(double newCpuUsage) {
		double oldCpuUsage = cpuUsage;
		cpuUsage = newCpuUsage;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.MONITORING_DATA__CPU_USAGE, oldCpuUsage, cpuUsage));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public long getMemoryUsage() {
		return memoryUsage;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMemoryUsage(long newMemoryUsage) {
		long oldMemoryUsage = memoryUsage;
		memoryUsage = newMemoryUsage;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.MONITORING_DATA__MEMORY_USAGE, oldMemoryUsage, memoryUsage));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public long getTotalMemory() {
		return totalMemory;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTotalMemory(long newTotalMemory) {
		long oldTotalMemory = totalMemory;
		totalMemory = newTotalMemory;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.MONITORING_DATA__TOTAL_MEMORY, oldTotalMemory, totalMemory));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.MONITORING_DATA__TIMESTAMP:
				return getTimestamp();
			case OrchestrationPackage.MONITORING_DATA__CPU_USAGE:
				return getCpuUsage();
			case OrchestrationPackage.MONITORING_DATA__MEMORY_USAGE:
				return getMemoryUsage();
			case OrchestrationPackage.MONITORING_DATA__TOTAL_MEMORY:
				return getTotalMemory();
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
			case OrchestrationPackage.MONITORING_DATA__TIMESTAMP:
				setTimestamp((Long)newValue);
				return;
			case OrchestrationPackage.MONITORING_DATA__CPU_USAGE:
				setCpuUsage((Double)newValue);
				return;
			case OrchestrationPackage.MONITORING_DATA__MEMORY_USAGE:
				setMemoryUsage((Long)newValue);
				return;
			case OrchestrationPackage.MONITORING_DATA__TOTAL_MEMORY:
				setTotalMemory((Long)newValue);
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
			case OrchestrationPackage.MONITORING_DATA__TIMESTAMP:
				setTimestamp(TIMESTAMP_EDEFAULT);
				return;
			case OrchestrationPackage.MONITORING_DATA__CPU_USAGE:
				setCpuUsage(CPU_USAGE_EDEFAULT);
				return;
			case OrchestrationPackage.MONITORING_DATA__MEMORY_USAGE:
				setMemoryUsage(MEMORY_USAGE_EDEFAULT);
				return;
			case OrchestrationPackage.MONITORING_DATA__TOTAL_MEMORY:
				setTotalMemory(TOTAL_MEMORY_EDEFAULT);
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
			case OrchestrationPackage.MONITORING_DATA__TIMESTAMP:
				return timestamp != TIMESTAMP_EDEFAULT;
			case OrchestrationPackage.MONITORING_DATA__CPU_USAGE:
				return cpuUsage != CPU_USAGE_EDEFAULT;
			case OrchestrationPackage.MONITORING_DATA__MEMORY_USAGE:
				return memoryUsage != MEMORY_USAGE_EDEFAULT;
			case OrchestrationPackage.MONITORING_DATA__TOTAL_MEMORY:
				return totalMemory != TOTAL_MEMORY_EDEFAULT;
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
		result.append(" (timestamp: ");
		result.append(timestamp);
		result.append(", cpuUsage: ");
		result.append(cpuUsage);
		result.append(", memoryUsage: ");
		result.append(memoryUsage);
		result.append(", totalMemory: ");
		result.append(totalMemory);
		result.append(')');
		return result.toString();
	}

} //MonitoringDataImpl
