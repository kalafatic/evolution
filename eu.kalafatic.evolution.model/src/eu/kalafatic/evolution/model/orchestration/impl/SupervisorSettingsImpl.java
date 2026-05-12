/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.SupervisorSettings;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Supervisor Settings</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SupervisorSettingsImpl#getExecutablePath <em>Executable Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SupervisorSettingsImpl#isDeployed <em>Deployed</em>}</li>
 * </ul>
 *
 * @generated
 */
public class SupervisorSettingsImpl extends MinimalEObjectImpl.Container implements SupervisorSettings {
	/**
	 * The default value of the '{@link #getExecutablePath() <em>Executable Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExecutablePath()
	 * @generated
	 * @ordered
	 */
	protected static final String EXECUTABLE_PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getExecutablePath() <em>Executable Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getExecutablePath()
	 * @generated
	 * @ordered
	 */
	protected String executablePath = EXECUTABLE_PATH_EDEFAULT;

	/**
	 * The default value of the '{@link #isDeployed() <em>Deployed</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDeployed()
	 * @generated
	 * @ordered
	 */
	protected static final boolean DEPLOYED_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isDeployed() <em>Deployed</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isDeployed()
	 * @generated
	 * @ordered
	 */
	protected boolean deployed = DEPLOYED_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected SupervisorSettingsImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.SUPERVISOR_SETTINGS;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getExecutablePath() {
		return executablePath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setExecutablePath(String newExecutablePath) {
		String oldExecutablePath = executablePath;
		executablePath = newExecutablePath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SUPERVISOR_SETTINGS__EXECUTABLE_PATH, oldExecutablePath, executablePath));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isDeployed() {
		return deployed;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDeployed(boolean newDeployed) {
		boolean oldDeployed = deployed;
		deployed = newDeployed;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SUPERVISOR_SETTINGS__DEPLOYED, oldDeployed, deployed));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.SUPERVISOR_SETTINGS__EXECUTABLE_PATH:
				return getExecutablePath();
			case OrchestrationPackage.SUPERVISOR_SETTINGS__DEPLOYED:
				return isDeployed();
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
			case OrchestrationPackage.SUPERVISOR_SETTINGS__EXECUTABLE_PATH:
				setExecutablePath((String)newValue);
				return;
			case OrchestrationPackage.SUPERVISOR_SETTINGS__DEPLOYED:
				setDeployed((Boolean)newValue);
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
			case OrchestrationPackage.SUPERVISOR_SETTINGS__EXECUTABLE_PATH:
				setExecutablePath(EXECUTABLE_PATH_EDEFAULT);
				return;
			case OrchestrationPackage.SUPERVISOR_SETTINGS__DEPLOYED:
				setDeployed(DEPLOYED_EDEFAULT);
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
			case OrchestrationPackage.SUPERVISOR_SETTINGS__EXECUTABLE_PATH:
				return EXECUTABLE_PATH_EDEFAULT == null ? executablePath != null : !EXECUTABLE_PATH_EDEFAULT.equals(executablePath);
			case OrchestrationPackage.SUPERVISOR_SETTINGS__DEPLOYED:
				return deployed != DEPLOYED_EDEFAULT;
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
		result.append(" (executablePath: ");
		result.append(executablePath);
		result.append(", deployed: ");
		result.append(deployed);
		result.append(')');
		return result.toString();
	}

} //SupervisorSettingsImpl
