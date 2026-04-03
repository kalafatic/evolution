/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.Eclipse;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Eclipse</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.EclipseImpl#getWorkspace <em>Workspace</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.EclipseImpl#getInstallation <em>Installation</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.EclipseImpl#getTargetPlatform <em>Target Platform</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.EclipseImpl#getTestStatus <em>Test Status</em>}</li>
 * </ul>
 *
 * @generated
 */
public class EclipseImpl extends MinimalEObjectImpl.Container implements Eclipse {
	/**
	 * The default value of the '{@link #getWorkspace() <em>Workspace</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getWorkspace()
	 * @generated
	 * @ordered
	 */
	protected static final String WORKSPACE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getWorkspace() <em>Workspace</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getWorkspace()
	 * @generated
	 * @ordered
	 */
	protected String workspace = WORKSPACE_EDEFAULT;

	/**
	 * The default value of the '{@link #getInstallation() <em>Installation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInstallation()
	 * @generated
	 * @ordered
	 */
	protected static final String INSTALLATION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getInstallation() <em>Installation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getInstallation()
	 * @generated
	 * @ordered
	 */
	protected String installation = INSTALLATION_EDEFAULT;

	/**
	 * The default value of the '{@link #getTargetPlatform() <em>Target Platform</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetPlatform()
	 * @generated
	 * @ordered
	 */
	protected static final String TARGET_PLATFORM_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTargetPlatform() <em>Target Platform</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetPlatform()
	 * @generated
	 * @ordered
	 */
	protected String targetPlatform = TARGET_PLATFORM_EDEFAULT;

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
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected EclipseImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.ECLIPSE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getWorkspace() {
		return workspace;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setWorkspace(String newWorkspace) {
		String oldWorkspace = workspace;
		workspace = newWorkspace;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ECLIPSE__WORKSPACE, oldWorkspace, workspace));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getInstallation() {
		return installation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setInstallation(String newInstallation) {
		String oldInstallation = installation;
		installation = newInstallation;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ECLIPSE__INSTALLATION, oldInstallation, installation));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTargetPlatform() {
		return targetPlatform;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTargetPlatform(String newTargetPlatform) {
		String oldTargetPlatform = targetPlatform;
		targetPlatform = newTargetPlatform;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ECLIPSE__TARGET_PLATFORM, oldTargetPlatform, targetPlatform));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.ECLIPSE__TEST_STATUS, oldTestStatus, testStatus));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.ECLIPSE__WORKSPACE:
				return getWorkspace();
			case OrchestrationPackage.ECLIPSE__INSTALLATION:
				return getInstallation();
			case OrchestrationPackage.ECLIPSE__TARGET_PLATFORM:
				return getTargetPlatform();
			case OrchestrationPackage.ECLIPSE__TEST_STATUS:
				return getTestStatus();
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
			case OrchestrationPackage.ECLIPSE__WORKSPACE:
				setWorkspace((String)newValue);
				return;
			case OrchestrationPackage.ECLIPSE__INSTALLATION:
				setInstallation((String)newValue);
				return;
			case OrchestrationPackage.ECLIPSE__TARGET_PLATFORM:
				setTargetPlatform((String)newValue);
				return;
			case OrchestrationPackage.ECLIPSE__TEST_STATUS:
				setTestStatus((String)newValue);
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
			case OrchestrationPackage.ECLIPSE__WORKSPACE:
				setWorkspace(WORKSPACE_EDEFAULT);
				return;
			case OrchestrationPackage.ECLIPSE__INSTALLATION:
				setInstallation(INSTALLATION_EDEFAULT);
				return;
			case OrchestrationPackage.ECLIPSE__TARGET_PLATFORM:
				setTargetPlatform(TARGET_PLATFORM_EDEFAULT);
				return;
			case OrchestrationPackage.ECLIPSE__TEST_STATUS:
				setTestStatus(TEST_STATUS_EDEFAULT);
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
			case OrchestrationPackage.ECLIPSE__WORKSPACE:
				return WORKSPACE_EDEFAULT == null ? workspace != null : !WORKSPACE_EDEFAULT.equals(workspace);
			case OrchestrationPackage.ECLIPSE__INSTALLATION:
				return INSTALLATION_EDEFAULT == null ? installation != null : !INSTALLATION_EDEFAULT.equals(installation);
			case OrchestrationPackage.ECLIPSE__TARGET_PLATFORM:
				return TARGET_PLATFORM_EDEFAULT == null ? targetPlatform != null : !TARGET_PLATFORM_EDEFAULT.equals(targetPlatform);
			case OrchestrationPackage.ECLIPSE__TEST_STATUS:
				return TEST_STATUS_EDEFAULT == null ? testStatus != null : !TEST_STATUS_EDEFAULT.equals(testStatus);
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
		result.append(" (workspace: ");
		result.append(workspace);
		result.append(", installation: ");
		result.append(installation);
		result.append(", targetPlatform: ");
		result.append(targetPlatform);
		result.append(", testStatus: ");
		result.append(testStatus);
		result.append(')');
		return result.toString();
	}

} //EclipseImpl
