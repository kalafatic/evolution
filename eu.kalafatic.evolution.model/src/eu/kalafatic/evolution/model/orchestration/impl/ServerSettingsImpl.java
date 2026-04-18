/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.ServerSettings;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Server Settings</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ServerSettingsImpl#getPort <em>Port</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ServerSettingsImpl#isAutoStart <em>Auto Start</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ServerSettingsImpl#isGitAutomation <em>Git Automation</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ServerSettingsImpl extends MinimalEObjectImpl.Container implements ServerSettings {
	/**
	 * The default value of the '{@link #getPort() <em>Port</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPort()
	 * @generated
	 * @ordered
	 */
	protected static final int PORT_EDEFAULT = 88080;

	/**
	 * The cached value of the '{@link #getPort() <em>Port</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPort()
	 * @generated
	 * @ordered
	 */
	protected int port = PORT_EDEFAULT;

	/**
	 * The default value of the '{@link #isAutoStart() <em>Auto Start</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAutoStart()
	 * @generated
	 * @ordered
	 */
	protected static final boolean AUTO_START_EDEFAULT = true;

	/**
	 * The cached value of the '{@link #isAutoStart() <em>Auto Start</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAutoStart()
	 * @generated
	 * @ordered
	 */
	protected boolean autoStart = AUTO_START_EDEFAULT;

	/**
	 * The default value of the '{@link #isGitAutomation() <em>Git Automation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isGitAutomation()
	 * @generated
	 * @ordered
	 */
	protected static final boolean GIT_AUTOMATION_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isGitAutomation() <em>Git Automation</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isGitAutomation()
	 * @generated
	 * @ordered
	 */
	protected boolean gitAutomation = GIT_AUTOMATION_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ServerSettingsImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.SERVER_SETTINGS;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getPort() {
		return port;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPort(int newPort) {
		int oldPort = port;
		port = newPort;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SERVER_SETTINGS__PORT, oldPort, port));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isAutoStart() {
		return autoStart;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAutoStart(boolean newAutoStart) {
		boolean oldAutoStart = autoStart;
		autoStart = newAutoStart;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SERVER_SETTINGS__AUTO_START, oldAutoStart, autoStart));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isGitAutomation() {
		return gitAutomation;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setGitAutomation(boolean newGitAutomation) {
		boolean oldGitAutomation = gitAutomation;
		gitAutomation = newGitAutomation;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.SERVER_SETTINGS__GIT_AUTOMATION, oldGitAutomation, gitAutomation));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.SERVER_SETTINGS__PORT:
				return getPort();
			case OrchestrationPackage.SERVER_SETTINGS__AUTO_START:
				return isAutoStart();
			case OrchestrationPackage.SERVER_SETTINGS__GIT_AUTOMATION:
				return isGitAutomation();
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
			case OrchestrationPackage.SERVER_SETTINGS__PORT:
				setPort((Integer)newValue);
				return;
			case OrchestrationPackage.SERVER_SETTINGS__AUTO_START:
				setAutoStart((Boolean)newValue);
				return;
			case OrchestrationPackage.SERVER_SETTINGS__GIT_AUTOMATION:
				setGitAutomation((Boolean)newValue);
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
			case OrchestrationPackage.SERVER_SETTINGS__PORT:
				setPort(PORT_EDEFAULT);
				return;
			case OrchestrationPackage.SERVER_SETTINGS__AUTO_START:
				setAutoStart(AUTO_START_EDEFAULT);
				return;
			case OrchestrationPackage.SERVER_SETTINGS__GIT_AUTOMATION:
				setGitAutomation(GIT_AUTOMATION_EDEFAULT);
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
			case OrchestrationPackage.SERVER_SETTINGS__PORT:
				return port != PORT_EDEFAULT;
			case OrchestrationPackage.SERVER_SETTINGS__AUTO_START:
				return autoStart != AUTO_START_EDEFAULT;
			case OrchestrationPackage.SERVER_SETTINGS__GIT_AUTOMATION:
				return gitAutomation != GIT_AUTOMATION_EDEFAULT;
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
		result.append(" (port: ");
		result.append(port);
		result.append(", autoStart: ");
		result.append(autoStart);
		result.append(", gitAutomation: ");
		result.append(gitAutomation);
		result.append(')');
		return result.toString();
	}

} //ServerSettingsImpl
