/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Network Rule</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetworkRule#getAllowedDomains <em>Allowed Domains</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetworkRule#isAllowAll <em>Allow All</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetworkRule()
 * @model
 * @generated
 */
public interface NetworkRule extends Rule {
	/**
	 * Returns the value of the '<em><b>Allowed Domains</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Allowed Domains</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetworkRule_AllowedDomains()
	 * @model
	 * @generated
	 */
	EList<String> getAllowedDomains();

	/**
	 * Returns the value of the '<em><b>Allow All</b></em>' attribute.
	 * The default value is <code>"false"</code>.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Allow All</em>' attribute.
	 * @see #setAllowAll(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetworkRule_AllowAll()
	 * @model default="false"
	 * @generated
	 */
	boolean isAllowAll();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetworkRule#isAllowAll <em>Allow All</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Allow All</em>' attribute.
	 * @see #isAllowAll()
	 * @generated
	 */
	void setAllowAll(boolean value);

} // NetworkRule
