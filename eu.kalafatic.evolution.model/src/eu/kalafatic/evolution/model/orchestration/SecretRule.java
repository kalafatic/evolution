/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Secret Rule</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SecretRule#getAllowedSecrets <em>Allowed Secrets</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSecretRule()
 * @model
 * @generated
 */
public interface SecretRule extends Rule {
	/**
	 * Returns the value of the '<em><b>Allowed Secrets</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Allowed Secrets</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSecretRule_AllowedSecrets()
	 * @model
	 * @generated
	 */
	EList<String> getAllowedSecrets();

} // SecretRule
