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
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.COMPILER__SOURCE_VERSION:
				return getSourceVersion();
			case OrchestrationPackage.COMPILER__TARGET_VERSION:
				return getTargetVersion();
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
		result.append(')');
		return result.toString();
	}

} //CompilerImpl
