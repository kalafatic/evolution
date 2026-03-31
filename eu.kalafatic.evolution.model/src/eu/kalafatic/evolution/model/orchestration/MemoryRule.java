/**
 */
package eu.kalafatic.evolution.model.orchestration;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Memory Rule</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.MemoryRule#getStorageLimit <em>Storage Limit</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.MemoryRule#getRetentionPeriod <em>Retention Period</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMemoryRule()
 * @model
 * @generated
 */
public interface MemoryRule extends Rule {
	/**
	 * Returns the value of the '<em><b>Storage Limit</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Storage Limit</em>' attribute.
	 * @see #setStorageLimit(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMemoryRule_StorageLimit()
	 * @model
	 * @generated
	 */
	int getStorageLimit();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.MemoryRule#getStorageLimit <em>Storage Limit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Storage Limit</em>' attribute.
	 * @see #getStorageLimit()
	 * @generated
	 */
	void setStorageLimit(int value);

	/**
	 * Returns the value of the '<em><b>Retention Period</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Retention Period</em>' attribute.
	 * @see #setRetentionPeriod(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMemoryRule_RetentionPeriod()
	 * @model
	 * @generated
	 */
	int getRetentionPeriod();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.MemoryRule#getRetentionPeriod <em>Retention Period</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Retention Period</em>' attribute.
	 * @see #getRetentionPeriod()
	 * @generated
	 */
	void setRetentionPeriod(int value);

} // MemoryRule
