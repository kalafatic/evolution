/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Maven</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Maven#getGoals <em>Goals</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Maven#getProfiles <em>Profiles</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Maven#getTestStatus <em>Test Status</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMaven()
 * @model
 * @generated
 */
public interface Maven extends EObject {
	/**
	 * Returns the value of the '<em><b>Goals</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Goals</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMaven_Goals()
	 * @model
	 * @generated
	 */
	EList<String> getGoals();

	/**
	 * Returns the value of the '<em><b>Profiles</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Profiles</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMaven_Profiles()
	 * @model
	 * @generated
	 */
	EList<String> getProfiles();

	/**
	 * Returns the value of the '<em><b>Test Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Test Status</em>' attribute.
	 * @see #setTestStatus(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getMaven_TestStatus()
	 * @model
	 * @generated
	 */
	String getTestStatus();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Maven#getTestStatus <em>Test Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Test Status</em>' attribute.
	 * @see #getTestStatus()
	 * @generated
	 */
	void setTestStatus(String value);

} // Maven
