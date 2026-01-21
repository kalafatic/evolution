/**
 */
package orchestration;

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
 *   <li>{@link orchestration.Maven#getGoals <em>Goals</em>}</li>
 *   <li>{@link orchestration.Maven#getProfiles <em>Profiles</em>}</li>
 * </ul>
 *
 * @see orchestration.OrchestrationPackage#getMaven()
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
	 * @see orchestration.OrchestrationPackage#getMaven_Goals()
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
	 * @see orchestration.OrchestrationPackage#getMaven_Profiles()
	 * @model
	 * @generated
	 */
	EList<String> getProfiles();

} // Maven
