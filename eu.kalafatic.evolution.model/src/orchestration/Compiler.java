/**
 */
package orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Compiler</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link orchestration.Compiler#getSourceVersion <em>Source Version</em>}</li>
 *   <li>{@link orchestration.Compiler#getTargetVersion <em>Target Version</em>}</li>
 * </ul>
 *
 * @see orchestration.OrchestrationPackage#getCompiler()
 * @model
 * @generated
 */
public interface Compiler extends EObject {
	/**
	 * Returns the value of the '<em><b>Source Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Source Version</em>' attribute.
	 * @see #setSourceVersion(String)
	 * @see orchestration.OrchestrationPackage#getCompiler_SourceVersion()
	 * @model
	 * @generated
	 */
	String getSourceVersion();

	/**
	 * Sets the value of the '{@link orchestration.Compiler#getSourceVersion <em>Source Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Source Version</em>' attribute.
	 * @see #getSourceVersion()
	 * @generated
	 */
	void setSourceVersion(String value);

	/**
	 * Returns the value of the '<em><b>Target Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target Version</em>' attribute.
	 * @see #setTargetVersion(String)
	 * @see orchestration.OrchestrationPackage#getCompiler_TargetVersion()
	 * @model
	 * @generated
	 */
	String getTargetVersion();

	/**
	 * Sets the value of the '{@link orchestration.Compiler#getTargetVersion <em>Target Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target Version</em>' attribute.
	 * @see #getTargetVersion()
	 * @generated
	 */
	void setTargetVersion(String value);

} // Compiler
