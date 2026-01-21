/**
 */
package orchestration.impl;

import orchestration.Git;
import orchestration.OrchestrationPackage;

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
 *   <li>{@link orchestration.impl.GitImpl#getRepositoryUrl <em>Repository Url</em>}</li>
 *   <li>{@link orchestration.impl.GitImpl#getBranch <em>Branch</em>}</li>
 *   <li>{@link orchestration.impl.GitImpl#getUsername <em>Username</em>}</li>
 *   <li>{@link orchestration.impl.GitImpl#getLocalPath <em>Local Path</em>}</li>
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
	public String getRepositoryUrl() {
		return repositoryUrl;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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
	public String getBranch() {
		return branch;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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
	public String getUsername() {
		return username;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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
	public String getLocalPath() {
		return localPath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
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
		result.append(')');
		return result.toString();
	}

} //GitImpl
