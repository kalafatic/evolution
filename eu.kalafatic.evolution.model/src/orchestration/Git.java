/**
 */
package orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Git</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link orchestration.Git#getRepositoryUrl <em>Repository Url</em>}</li>
 *   <li>{@link orchestration.Git#getBranch <em>Branch</em>}</li>
 *   <li>{@link orchestration.Git#getUsername <em>Username</em>}</li>
 *   <li>{@link orchestration.Git#getLocalPath <em>Local Path</em>}</li>
 * </ul>
 *
 * @see orchestration.OrchestrationPackage#getGit()
 * @model
 * @generated
 */
public interface Git extends EObject {
	/**
	 * Returns the value of the '<em><b>Repository Url</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Repository Url</em>' attribute.
	 * @see #setRepositoryUrl(String)
	 * @see orchestration.OrchestrationPackage#getGit_RepositoryUrl()
	 * @model
	 * @generated
	 */
	String getRepositoryUrl();

	/**
	 * Sets the value of the '{@link orchestration.Git#getRepositoryUrl <em>Repository Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Repository Url</em>' attribute.
	 * @see #getRepositoryUrl()
	 * @generated
	 */
	void setRepositoryUrl(String value);

	/**
	 * Returns the value of the '<em><b>Branch</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Branch</em>' attribute.
	 * @see #setBranch(String)
	 * @see orchestration.OrchestrationPackage#getGit_Branch()
	 * @model
	 * @generated
	 */
	String getBranch();

	/**
	 * Sets the value of the '{@link orchestration.Git#getBranch <em>Branch</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Branch</em>' attribute.
	 * @see #getBranch()
	 * @generated
	 */
	void setBranch(String value);

	/**
	 * Returns the value of the '<em><b>Username</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Username</em>' attribute.
	 * @see #setUsername(String)
	 * @see orchestration.OrchestrationPackage#getGit_Username()
	 * @model
	 * @generated
	 */
	String getUsername();

	/**
	 * Sets the value of the '{@link orchestration.Git#getUsername <em>Username</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Username</em>' attribute.
	 * @see #getUsername()
	 * @generated
	 */
	void setUsername(String value);

	/**
	 * Returns the value of the '<em><b>Local Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Local Path</em>' attribute.
	 * @see #setLocalPath(String)
	 * @see orchestration.OrchestrationPackage#getGit_LocalPath()
	 * @model
	 * @generated
	 */
	String getLocalPath();

	/**
	 * Sets the value of the '{@link orchestration.Git#getLocalPath <em>Local Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Local Path</em>' attribute.
	 * @see #getLocalPath()
	 * @generated
	 */
	void setLocalPath(String value);

} // Git
