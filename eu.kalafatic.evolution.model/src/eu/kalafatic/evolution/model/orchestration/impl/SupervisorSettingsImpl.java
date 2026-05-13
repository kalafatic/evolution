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
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SupervisorSettingsImpl#getSourcePath <em>Source Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SupervisorSettingsImpl#getCommands <em>Commands</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.SupervisorSettingsImpl#getSettings <em>Settings</em>}</li>
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
	 * The default value of the '{@link #getSourcePath() <em>Source Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSourcePath()
	 * @generated
	 * @ordered
	 */
	protected static final String SOURCE_PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSourcePath() <em>Source Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSourcePath()
	 * @generated
	 * @ordered
	 */
	protected String sourcePath = SOURCE_PATH_EDEFAULT;

	/**
	 * The default value of the '{@link #getCommands() <em>Commands</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCommands()
	 * @generated
	 * @ordered
	 */
	protected static final String COMMANDS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getCommands() <em>Commands</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCommands()
	 * @generated
	 * @ordered
	 */
	protected String commands = COMMANDS_EDEFAULT;

	/**
	 * The default value of the '{@link #getSettings() <em>Settings</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSettings()
	 * @generated
	 * @ordered
	 */
	protected static final String SETTINGS_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSettings() <em>Settings</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSettings()
	 * @generated
	 * @ordered
	 */
	protected String settings = SETTINGS_EDEFAULT;

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
	public String getSourcePath() {
		return sourcePath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSourcePath(String newSourcePath) {
		String oldSourcePath = sourcePath;
		sourcePath = newSourcePath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SUPERVISOR_SETTINGS__SOURCE_PATH, oldSourcePath, sourcePath));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getCommands() {
		return commands;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCommands(String newCommands) {
		String oldCommands = commands;
		commands = newCommands;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SUPERVISOR_SETTINGS__COMMANDS, oldCommands, commands));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getSettings() {
		return settings;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSettings(String newSettings) {
		String oldSettings = settings;
		settings = newSettings;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SUPERVISOR_SETTINGS__SETTINGS, oldSettings, settings));
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
			case OrchestrationPackage.SUPERVISOR_SETTINGS__SOURCE_PATH:
				return getSourcePath();
			case OrchestrationPackage.SUPERVISOR_SETTINGS__COMMANDS:
				return getCommands();
			case OrchestrationPackage.SUPERVISOR_SETTINGS__SETTINGS:
				return getSettings();
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
			case OrchestrationPackage.SUPERVISOR_SETTINGS__SOURCE_PATH:
				setSourcePath((String)newValue);
				return;
			case OrchestrationPackage.SUPERVISOR_SETTINGS__COMMANDS:
				setCommands((String)newValue);
				return;
			case OrchestrationPackage.SUPERVISOR_SETTINGS__SETTINGS:
				setSettings((String)newValue);
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
			case OrchestrationPackage.SUPERVISOR_SETTINGS__SOURCE_PATH:
				setSourcePath(SOURCE_PATH_EDEFAULT);
				return;
			case OrchestrationPackage.SUPERVISOR_SETTINGS__COMMANDS:
				setCommands(COMMANDS_EDEFAULT);
				return;
			case OrchestrationPackage.SUPERVISOR_SETTINGS__SETTINGS:
				setSettings(SETTINGS_EDEFAULT);
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
			case OrchestrationPackage.SUPERVISOR_SETTINGS__SOURCE_PATH:
				return SOURCE_PATH_EDEFAULT == null ? sourcePath != null : !SOURCE_PATH_EDEFAULT.equals(sourcePath);
			case OrchestrationPackage.SUPERVISOR_SETTINGS__COMMANDS:
				return COMMANDS_EDEFAULT == null ? commands != null : !COMMANDS_EDEFAULT.equals(commands);
			case OrchestrationPackage.SUPERVISOR_SETTINGS__SETTINGS:
				return SETTINGS_EDEFAULT == null ? settings != null : !SETTINGS_EDEFAULT.equals(settings);
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
		result.append(", sourcePath: ");
		result.append(sourcePath);
		result.append(", commands: ");
		result.append(commands);
		result.append(", settings: ");
		result.append(settings);
		result.append(')');
		return result.toString();
	}

} //SupervisorSettingsImpl
