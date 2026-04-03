/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Compiler</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.CompilerImpl#getSourceVersion <em>Source Version</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.CompilerImpl#getTargetVersion <em>Target Version</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.CompilerImpl#getCPath <em>CPath</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.CompilerImpl#getCppPath <em>Cpp Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.CompilerImpl#getMakePath <em>Make Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.CompilerImpl#getCmakePath <em>Cmake Path</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.CompilerImpl#getTestStatus <em>Test Status</em>}</li>
 * </ul>
 *
 * @generated
 */
public class CompilerImpl extends MinimalEObjectImpl.Container implements eu.kalafatic.evolution.model.orchestration.Compiler {
	/**
	 * The default value of the '{@link #getSourceVersion() <em>Source Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSourceVersion()
	 * @generated
	 * @ordered
	 */
	protected static final String SOURCE_VERSION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSourceVersion() <em>Source Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSourceVersion()
	 * @generated
	 * @ordered
	 */
	protected String sourceVersion = SOURCE_VERSION_EDEFAULT;

	/**
	 * The default value of the '{@link #getTargetVersion() <em>Target Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetVersion()
	 * @generated
	 * @ordered
	 */
	protected static final String TARGET_VERSION_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTargetVersion() <em>Target Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTargetVersion()
	 * @generated
	 * @ordered
	 */
	protected String targetVersion = TARGET_VERSION_EDEFAULT;

	/**
	 * The default value of the '{@link #getCPath() <em>CPath</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCPath()
	 * @generated
	 * @ordered
	 */
	protected static final String CPATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getCPath() <em>CPath</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCPath()
	 * @generated
	 * @ordered
	 */
	protected String cPath = CPATH_EDEFAULT;

	/**
	 * The default value of the '{@link #getCppPath() <em>Cpp Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCppPath()
	 * @generated
	 * @ordered
	 */
	protected static final String CPP_PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getCppPath() <em>Cpp Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCppPath()
	 * @generated
	 * @ordered
	 */
	protected String cppPath = CPP_PATH_EDEFAULT;

	/**
	 * The default value of the '{@link #getMakePath() <em>Make Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMakePath()
	 * @generated
	 * @ordered
	 */
	protected static final String MAKE_PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMakePath() <em>Make Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMakePath()
	 * @generated
	 * @ordered
	 */
	protected String makePath = MAKE_PATH_EDEFAULT;

	/**
	 * The default value of the '{@link #getCmakePath() <em>Cmake Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCmakePath()
	 * @generated
	 * @ordered
	 */
	protected static final String CMAKE_PATH_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getCmakePath() <em>Cmake Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getCmakePath()
	 * @generated
	 * @ordered
	 */
	protected String cmakePath = CMAKE_PATH_EDEFAULT;

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
	protected CompilerImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.COMPILER;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getSourceVersion() {
		return sourceVersion;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSourceVersion(String newSourceVersion) {
		String oldSourceVersion = sourceVersion;
		sourceVersion = newSourceVersion;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMPILER__SOURCE_VERSION, oldSourceVersion, sourceVersion));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTargetVersion() {
		return targetVersion;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTargetVersion(String newTargetVersion) {
		String oldTargetVersion = targetVersion;
		targetVersion = newTargetVersion;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMPILER__TARGET_VERSION, oldTargetVersion, targetVersion));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getCPath() {
		return cPath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCPath(String newCPath) {
		String oldCPath = cPath;
		cPath = newCPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMPILER__CPATH, oldCPath, cPath));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getCppPath() {
		return cppPath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCppPath(String newCppPath) {
		String oldCppPath = cppPath;
		cppPath = newCppPath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMPILER__CPP_PATH, oldCppPath, cppPath));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getMakePath() {
		return makePath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMakePath(String newMakePath) {
		String oldMakePath = makePath;
		makePath = newMakePath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMPILER__MAKE_PATH, oldMakePath, makePath));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getCmakePath() {
		return cmakePath;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setCmakePath(String newCmakePath) {
		String oldCmakePath = cmakePath;
		cmakePath = newCmakePath;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMPILER__CMAKE_PATH, oldCmakePath, cmakePath));
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
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMPILER__TEST_STATUS, oldTestStatus, testStatus));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.COMPILER__SOURCE_VERSION:
				return getSourceVersion();
			case OrchestrationPackage.COMPILER__TARGET_VERSION:
				return getTargetVersion();
			case OrchestrationPackage.COMPILER__CPATH:
				return getCPath();
			case OrchestrationPackage.COMPILER__CPP_PATH:
				return getCppPath();
			case OrchestrationPackage.COMPILER__MAKE_PATH:
				return getMakePath();
			case OrchestrationPackage.COMPILER__CMAKE_PATH:
				return getCmakePath();
			case OrchestrationPackage.COMPILER__TEST_STATUS:
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
			case OrchestrationPackage.COMPILER__SOURCE_VERSION:
				setSourceVersion((String)newValue);
				return;
			case OrchestrationPackage.COMPILER__TARGET_VERSION:
				setTargetVersion((String)newValue);
				return;
			case OrchestrationPackage.COMPILER__CPATH:
				setCPath((String)newValue);
				return;
			case OrchestrationPackage.COMPILER__CPP_PATH:
				setCppPath((String)newValue);
				return;
			case OrchestrationPackage.COMPILER__MAKE_PATH:
				setMakePath((String)newValue);
				return;
			case OrchestrationPackage.COMPILER__CMAKE_PATH:
				setCmakePath((String)newValue);
				return;
			case OrchestrationPackage.COMPILER__TEST_STATUS:
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
			case OrchestrationPackage.COMPILER__SOURCE_VERSION:
				setSourceVersion(SOURCE_VERSION_EDEFAULT);
				return;
			case OrchestrationPackage.COMPILER__TARGET_VERSION:
				setTargetVersion(TARGET_VERSION_EDEFAULT);
				return;
			case OrchestrationPackage.COMPILER__CPATH:
				setCPath(CPATH_EDEFAULT);
				return;
			case OrchestrationPackage.COMPILER__CPP_PATH:
				setCppPath(CPP_PATH_EDEFAULT);
				return;
			case OrchestrationPackage.COMPILER__MAKE_PATH:
				setMakePath(MAKE_PATH_EDEFAULT);
				return;
			case OrchestrationPackage.COMPILER__CMAKE_PATH:
				setCmakePath(CMAKE_PATH_EDEFAULT);
				return;
			case OrchestrationPackage.COMPILER__TEST_STATUS:
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
			case OrchestrationPackage.COMPILER__SOURCE_VERSION:
				return SOURCE_VERSION_EDEFAULT == null ? sourceVersion != null : !SOURCE_VERSION_EDEFAULT.equals(sourceVersion);
			case OrchestrationPackage.COMPILER__TARGET_VERSION:
				return TARGET_VERSION_EDEFAULT == null ? targetVersion != null : !TARGET_VERSION_EDEFAULT.equals(targetVersion);
			case OrchestrationPackage.COMPILER__CPATH:
				return CPATH_EDEFAULT == null ? cPath != null : !CPATH_EDEFAULT.equals(cPath);
			case OrchestrationPackage.COMPILER__CPP_PATH:
				return CPP_PATH_EDEFAULT == null ? cppPath != null : !CPP_PATH_EDEFAULT.equals(cppPath);
			case OrchestrationPackage.COMPILER__MAKE_PATH:
				return MAKE_PATH_EDEFAULT == null ? makePath != null : !MAKE_PATH_EDEFAULT.equals(makePath);
			case OrchestrationPackage.COMPILER__CMAKE_PATH:
				return CMAKE_PATH_EDEFAULT == null ? cmakePath != null : !CMAKE_PATH_EDEFAULT.equals(cmakePath);
			case OrchestrationPackage.COMPILER__TEST_STATUS:
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
		result.append(" (sourceVersion: ");
		result.append(sourceVersion);
		result.append(", targetVersion: ");
		result.append(targetVersion);
		result.append(", cPath: ");
		result.append(cPath);
		result.append(", cppPath: ");
		result.append(cppPath);
		result.append(", makePath: ");
		result.append(makePath);
		result.append(", cmakePath: ");
		result.append(cmakePath);
		result.append(", testStatus: ");
		result.append(testStatus);
		result.append(')');
		return result.toString();
	}

} //CompilerImpl
