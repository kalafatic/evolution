/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Monitoring Data</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.MonitoringData#getTimestamp <em>Timestamp</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.MonitoringData#getCpuUsage <em>Cpu Usage</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.MonitoringData#getMemoryUsage <em>Memory Usage</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.MonitoringData#getTotalMemory <em>Total Memory</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMonitoringData()
 * @model
 * @generated
 */
public interface MonitoringData extends EObject {
	/**
	 * Returns the value of the '<em><b>Timestamp</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Timestamp</em>' attribute.
	 * @see #setTimestamp(long)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMonitoringData_Timestamp()
	 * @model
	 * @generated
	 */
	long getTimestamp();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.MonitoringData#getTimestamp <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Timestamp</em>' attribute.
	 * @see #getTimestamp()
	 * @generated
	 */
	void setTimestamp(long value);

	/**
	 * Returns the value of the '<em><b>Cpu Usage</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Cpu Usage</em>' attribute.
	 * @see #setCpuUsage(double)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMonitoringData_CpuUsage()
	 * @model
	 * @generated
	 */
	double getCpuUsage();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.MonitoringData#getCpuUsage <em>Cpu Usage</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Cpu Usage</em>' attribute.
	 * @see #getCpuUsage()
	 * @generated
	 */
	void setCpuUsage(double value);

	/**
	 * Returns the value of the '<em><b>Memory Usage</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Memory Usage</em>' attribute.
	 * @see #setMemoryUsage(long)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMonitoringData_MemoryUsage()
	 * @model
	 * @generated
	 */
	long getMemoryUsage();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.MonitoringData#getMemoryUsage <em>Memory Usage</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Memory Usage</em>' attribute.
	 * @see #getMemoryUsage()
	 * @generated
	 */
	void setMemoryUsage(long value);

	/**
	 * Returns the value of the '<em><b>Total Memory</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Total Memory</em>' attribute.
	 * @see #setTotalMemory(long)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMonitoringData_TotalMemory()
	 * @model
	 * @generated
	 */
	long getTotalMemory();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.MonitoringData#getTotalMemory <em>Total Memory</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Total Memory</em>' attribute.
	 * @see #getTotalMemory()
	 * @generated
	 */
	void setTotalMemory(long value);

} // MonitoringData
