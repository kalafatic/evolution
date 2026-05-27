/**
 */
package eu.kalafatic.evolution.model.orchestration;

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
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Git#getRepositoryUrl <em>Repository Url</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Git#getBranch <em>Branch</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Git#getUsername <em>Username</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Git#getLocalPath <em>Local Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Git#getTestStatus <em>Test Status</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Git#getBranchName <em>Branch Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Git#getCommitMsg <em>Commit Msg</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Git#getPassword <em>Password</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGit()
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
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGit_RepositoryUrl()
	 * @model
	 * @generated
	 */
	String getRepositoryUrl();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Git#getRepositoryUrl <em>Repository Url</em>}' attribute.
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
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGit_Branch()
	 * @model
	 * @generated
	 */
	String getBranch();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Git#getBranch <em>Branch</em>}' attribute.
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
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGit_Username()
	 * @model
	 * @generated
	 */
	String getUsername();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Git#getUsername <em>Username</em>}' attribute.
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
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGit_LocalPath()
	 * @model
	 * @generated
	 */
	String getLocalPath();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Git#getLocalPath <em>Local Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Local Path</em>' attribute.
	 * @see #getLocalPath()
	 * @generated
	 */
	void setLocalPath(String value);

	/**
	 * Returns the value of the '<em><b>Test Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Test Status</em>' attribute.
	 * @see #setTestStatus(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGit_TestStatus()
	 * @model
	 * @generated
	 */
	String getTestStatus();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Git#getTestStatus <em>Test Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Test Status</em>' attribute.
	 * @see #getTestStatus()
	 * @generated
	 */
	void setTestStatus(String value);

	/**
	 * Returns the value of the '<em><b>Branch Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Branch Name</em>' attribute.
	 * @see #setBranchName(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGit_BranchName()
	 * @model
	 * @generated
	 */
	String getBranchName();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Git#getBranchName <em>Branch Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Branch Name</em>' attribute.
	 * @see #getBranchName()
	 * @generated
	 */
	void setBranchName(String value);

	/**
	 * Returns the value of the '<em><b>Password</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Password</em>' attribute.
	 * @see #setPassword(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGit_Password()
	 * @model
	 * @generated
	 */
	String getPassword();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Git#getPassword <em>Password</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Password</em>' attribute.
	 * @see #getPassword()
	 * @generated
	 */
	void setPassword(String value);

	/**
	 * Returns the value of the '<em><b>Commit Msg</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Commit Msg</em>' attribute.
	 * @see #setCommitMsg(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getGit_CommitMsg()
	 * @model
	 * @generated
	 */
	String getCommitMsg();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Git#getCommitMsg <em>Commit Msg</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Commit Msg</em>' attribute.
	 * @see #getCommitMsg()
	 * @generated
	 */
	void setCommitMsg(String value);

} // Git
