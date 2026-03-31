/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Access Rule</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AccessRule#getAllowedPaths <em>Allowed Paths</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.AccessRule#getDeniedPaths <em>Denied Paths</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAccessRule()
 * @model
 * @generated
 */
public interface AccessRule extends Rule {
	/**
	 * Returns the value of the '<em><b>Allowed Paths</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Allowed Paths</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAccessRule_AllowedPaths()
	 * @model
	 * @generated
	 */
	EList<String> getAllowedPaths();

	/**
	 * Returns the value of the '<em><b>Denied Paths</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Denied Paths</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getAccessRule_DeniedPaths()
	 * @model
	 * @generated
	 */
	EList<String> getDeniedPaths();

} // AccessRule
