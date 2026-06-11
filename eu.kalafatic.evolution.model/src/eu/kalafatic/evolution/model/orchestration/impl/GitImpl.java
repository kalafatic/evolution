/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.Git;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Git</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GitImpl#getRepositoryUrl <em>Repository Url</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GitImpl#getBranch <em>Branch</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GitImpl#getUsername <em>Username</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GitImpl#getLocalPath <em>Local Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GitImpl#getTestStatus <em>Test Status</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GitImpl#getBranchName <em>Branch Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GitImpl#getCommitMsg <em>Commit Msg</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.GitImpl#getPassword <em>Password</em>}</li>
 * </ul>
 *
 * @generated
 */
public class GitImpl extends MinimalEObjectImpl.Container implements Git {
	/**
	 * The default value of the '{@link #getRepositoryUrl() <em>Repository Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRepositoryUrl()
	 * @generated
	 * @ordered
	 */
	protected static final String REPOSITORY_URL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getRepositoryUrl() <em>Repository Url</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getRepositoryUrl()
	 * @generated
	 * @ordered
	 */
	protected String repositoryUrl = REPOSITORY_URL_EDEFAULT;

	/**
	 * The default value of the '{@link #getBranch() <em>Branch</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBranch()
	 * @generated
	 * @ordered
	 */
	protected static final String BRANCH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getBranch() <em>Branch</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBranch()
	 * @generated
	 * @ordered
	 */
	protected String branch = BRANCH_EDEFAULT;

	/**
	 * The default value of the '{@link #getUsername() <em>Username</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUsername()
	 * @generated
	 * @ordered
	 */
	protected static final String USERNAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getUsername() <em>Username</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getUsername()
	 * @generated
	 * @ordered
	 */
	protected String username = USERNAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getLocalPath() <em>Local Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLocalPath()
	 * @generated
	 * @ordered
	 */
	protected static final String LOCAL_PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getLocalPath() <em>Local Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getLocalPath()
	 * @generated
	 * @ordered
	 */
	protected String localPath = LOCAL_PATH_EDEFAULT;

	/**
	 * The default value of the '{@link #getTestStatus() <em>Test Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTestStatus()
	 * @generated
	 * @ordered
	 */
	protected static final String TEST_STATUS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTestStatus() <em>Test Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTestStatus()
	 * @generated
	 * @ordered
	 */
	protected String testStatus = TEST_STATUS_EDEFAULT;

	/**
	 * The default value of the '{@link #getBranchName() <em>Branch Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBranchName()
	 * @generated
	 * @ordered
	 */
	protected static final String BRANCH_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getBranchName() <em>Branch Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getBranchName()
	 * @generated
	 * @ordered
	 */
	protected String branchName = BRANCH_NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getCommitMsg() <em>Commit Msg</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCommitMsg()
	 * @generated
	 * @ordered
	 */
	protected static final String COMMIT_MSG_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getCommitMsg() <em>Commit Msg</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCommitMsg()
	 * @generated
	 * @ordered
	 */
	protected String commitMsg = COMMIT_MSG_EDEFAULT;

	/**
	 * The default value of the '{@link #getPassword() <em>Password</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPassword()
	 * @generated
	 * @ordered
	 */
	protected static final String PASSWORD_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getPassword() <em>Password</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPassword()
	 * @generated
	 * @ordered
	 */
	protected String password = PASSWORD_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected GitImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.GIT;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getRepositoryUrl() {
		return repositoryUrl;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setRepositoryUrl(String newRepositoryUrl) {
		String oldRepositoryUrl = repositoryUrl;
		repositoryUrl = newRepositoryUrl;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GIT__REPOSITORY_URL, oldRepositoryUrl, repositoryUrl));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getBranch() {
		return branch;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setBranch(String newBranch) {
		String oldBranch = branch;
		branch = newBranch;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GIT__BRANCH, oldBranch, branch));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getUsername() {
		return username;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setUsername(String newUsername) {
		String oldUsername = username;
		username = newUsername;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GIT__USERNAME, oldUsername, username));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getLocalPath() {
		return localPath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setLocalPath(String newLocalPath) {
		String oldLocalPath = localPath;
		localPath = newLocalPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GIT__LOCAL_PATH, oldLocalPath, localPath));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTestStatus() {
		return testStatus;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTestStatus(String newTestStatus) {
		String oldTestStatus = testStatus;
		testStatus = newTestStatus;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GIT__TEST_STATUS, oldTestStatus, testStatus));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getBranchName() {
		return branchName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setBranchName(String newBranchName) {
		String oldBranchName = branchName;
		branchName = newBranchName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GIT__BRANCH_NAME, oldBranchName, branchName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getCommitMsg() {
		return commitMsg;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCommitMsg(String newCommitMsg) {
		String oldCommitMsg = commitMsg;
		commitMsg = newCommitMsg;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GIT__COMMIT_MSG, oldCommitMsg, commitMsg));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getPassword() {
		return password;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPassword(String newPassword) {
		String oldPassword = password;
		password = newPassword;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.GIT__PASSWORD, oldPassword, password));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.GIT__REPOSITORY_URL:
				return getRepositoryUrl();
			case OrchestrationPackage.GIT__BRANCH:
				return getBranch();
			case OrchestrationPackage.GIT__USERNAME:
				return getUsername();
			case OrchestrationPackage.GIT__LOCAL_PATH:
				return getLocalPath();
			case OrchestrationPackage.GIT__TEST_STATUS:
				return getTestStatus();
			case OrchestrationPackage.GIT__BRANCH_NAME:
				return getBranchName();
			case OrchestrationPackage.GIT__COMMIT_MSG:
				return getCommitMsg();
			case OrchestrationPackage.GIT__PASSWORD:
				return getPassword();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.GIT__REPOSITORY_URL:
				setRepositoryUrl((String)newValue);
				return;
			case OrchestrationPackage.GIT__BRANCH:
				setBranch((String)newValue);
				return;
			case OrchestrationPackage.GIT__USERNAME:
				setUsername((String)newValue);
				return;
			case OrchestrationPackage.GIT__LOCAL_PATH:
				setLocalPath((String)newValue);
				return;
			case OrchestrationPackage.GIT__TEST_STATUS:
				setTestStatus((String)newValue);
				return;
			case OrchestrationPackage.GIT__BRANCH_NAME:
				setBranchName((String)newValue);
				return;
			case OrchestrationPackage.GIT__COMMIT_MSG:
				setCommitMsg((String)newValue);
				return;
			case OrchestrationPackage.GIT__PASSWORD:
				setPassword((String)newValue);
				return;
		}
		super.eSet(featureID, newValue);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eUnset(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.GIT__REPOSITORY_URL:
				setRepositoryUrl(REPOSITORY_URL_EDEFAULT);
				return;
			case OrchestrationPackage.GIT__BRANCH:
				setBranch(BRANCH_EDEFAULT);
				return;
			case OrchestrationPackage.GIT__USERNAME:
				setUsername(USERNAME_EDEFAULT);
				return;
			case OrchestrationPackage.GIT__LOCAL_PATH:
				setLocalPath(LOCAL_PATH_EDEFAULT);
				return;
			case OrchestrationPackage.GIT__TEST_STATUS:
				setTestStatus(TEST_STATUS_EDEFAULT);
				return;
			case OrchestrationPackage.GIT__BRANCH_NAME:
				setBranchName(BRANCH_NAME_EDEFAULT);
				return;
			case OrchestrationPackage.GIT__COMMIT_MSG:
				setCommitMsg(COMMIT_MSG_EDEFAULT);
				return;
			case OrchestrationPackage.GIT__PASSWORD:
				setPassword(PASSWORD_EDEFAULT);
				return;
		}
		super.eUnset(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean eIsSet(int featureID) {
		switch (featureID) {
			case OrchestrationPackage.GIT__REPOSITORY_URL:
				return REPOSITORY_URL_EDEFAULT == null ? repositoryUrl != null : !REPOSITORY_URL_EDEFAULT.equals(repositoryUrl);
			case OrchestrationPackage.GIT__BRANCH:
				return BRANCH_EDEFAULT == null ? branch != null : !BRANCH_EDEFAULT.equals(branch);
			case OrchestrationPackage.GIT__USERNAME:
				return USERNAME_EDEFAULT == null ? username != null : !USERNAME_EDEFAULT.equals(username);
			case OrchestrationPackage.GIT__LOCAL_PATH:
				return LOCAL_PATH_EDEFAULT == null ? localPath != null : !LOCAL_PATH_EDEFAULT.equals(localPath);
			case OrchestrationPackage.GIT__TEST_STATUS:
				return TEST_STATUS_EDEFAULT == null ? testStatus != null : !TEST_STATUS_EDEFAULT.equals(testStatus);
			case OrchestrationPackage.GIT__BRANCH_NAME:
				return BRANCH_NAME_EDEFAULT == null ? branchName != null : !BRANCH_NAME_EDEFAULT.equals(branchName);
			case OrchestrationPackage.GIT__COMMIT_MSG:
				return COMMIT_MSG_EDEFAULT == null ? commitMsg != null : !COMMIT_MSG_EDEFAULT.equals(commitMsg);
			case OrchestrationPackage.GIT__PASSWORD:
				return PASSWORD_EDEFAULT == null ? password != null : !PASSWORD_EDEFAULT.equals(password);
		}
		return super.eIsSet(featureID);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String toString() {
		if (eIsProxy()) return super.toString();

		StringBuilder result = new StringBuilder(super.toString());
		result.append(" (repositoryUrl: ");
		result.append(repositoryUrl);
		result.append(", branch: ");
		result.append(branch);
		result.append(", username: ");
		result.append(username);
		result.append(", localPath: ");
		result.append(localPath);
		result.append(", testStatus: ");
		result.append(testStatus);
		result.append(", branchName: ");
		result.append(branchName);
		result.append(", commitMsg: ");
		result.append(commitMsg);
		result.append(", password: ");
		result.append(password);
		result.append(')');
		return result.toString();
	}

} //GitImpl
