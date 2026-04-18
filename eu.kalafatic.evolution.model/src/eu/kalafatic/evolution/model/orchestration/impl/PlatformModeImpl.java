package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.AutonomyLevel;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.PlatformMode;
import eu.kalafatic.evolution.model.orchestration.PlatformType;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Platform Mode</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.PlatformModeImpl#getType <em>Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.PlatformModeImpl#getAutonomyLevel <em>Autonomy Level</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.PlatformModeImpl#getIterationLimit <em>Iteration Limit</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.PlatformModeImpl#isAllowSelfModify <em>Allow Self Modify</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.PlatformModeImpl#getAllowedPaths <em>Allowed Paths</em>}</li>
 * </ul>
 *
 * @generated
 */
public class PlatformModeImpl extends MinimalEObjectImpl.Container implements PlatformMode {
	/**
	 * The default value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected static final PlatformType TYPE_EDEFAULT = PlatformType.SIMPLE_CHAT;

	/**
	 * The cached value of the '{@link #getType() <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getType()
	 * @generated
	 * @ordered
	 */
	protected PlatformType type = TYPE_EDEFAULT;

	/**
	 * The default value of the '{@link #getAutonomyLevel() <em>Autonomy Level</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAutonomyLevel()
	 * @generated
	 * @ordered
	 */
	protected static final AutonomyLevel AUTONOMY_LEVEL_EDEFAULT = AutonomyLevel.LOW;

	/**
	 * The cached value of the '{@link #getAutonomyLevel() <em>Autonomy Level</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAutonomyLevel()
	 * @generated
	 * @ordered
	 */
	protected AutonomyLevel autonomyLevel = AUTONOMY_LEVEL_EDEFAULT;

	/**
	 * The default value of the '{@link #getIterationLimit() <em>Iteration Limit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIterationLimit()
	 * @generated
	 * @ordered
	 */
	protected static final int ITERATION_LIMIT_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getIterationLimit() <em>Iteration Limit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIterationLimit()
	 * @generated
	 * @ordered
	 */
	protected int iterationLimit = ITERATION_LIMIT_EDEFAULT;

	/**
	 * The default value of the '{@link #isAllowSelfModify() <em>Allow Self Modify</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAllowSelfModify()
	 * @generated
	 * @ordered
	 */
	protected static final boolean ALLOW_SELF_MODIFY_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isAllowSelfModify() <em>Allow Self Modify</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isAllowSelfModify()
	 * @generated
	 * @ordered
	 */
	protected boolean allowSelfModify = ALLOW_SELF_MODIFY_EDEFAULT;

	/**
	 * The cached value of the '{@link #getAllowedPaths() <em>Allowed Paths</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAllowedPaths()
	 * @generated
	 * @ordered
	 */
	protected EList<String> allowedPaths;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected PlatformModeImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.PLATFORM_MODE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public PlatformType getType() {
		return type;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setType(PlatformType newType) {
		PlatformType oldType = type;
		type = newType == null ? TYPE_EDEFAULT : newType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.PLATFORM_MODE__TYPE, oldType, type));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public AutonomyLevel getAutonomyLevel() {
		return autonomyLevel;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAutonomyLevel(AutonomyLevel newAutonomyLevel) {
		AutonomyLevel oldAutonomyLevel = autonomyLevel;
		autonomyLevel = newAutonomyLevel == null ? AUTONOMY_LEVEL_EDEFAULT : newAutonomyLevel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.PLATFORM_MODE__AUTONOMY_LEVEL, oldAutonomyLevel, autonomyLevel));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getIterationLimit() {
		return iterationLimit;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setIterationLimit(int newIterationLimit) {
		int oldIterationLimit = iterationLimit;
		iterationLimit = newIterationLimit;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.PLATFORM_MODE__ITERATION_LIMIT, oldIterationLimit, iterationLimit));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isAllowSelfModify() {
		return allowSelfModify;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAllowSelfModify(boolean newAllowSelfModify) {
		boolean oldAllowSelfModify = allowSelfModify;
		allowSelfModify = newAllowSelfModify;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.PLATFORM_MODE__ALLOW_SELF_MODIFY, oldAllowSelfModify, allowSelfModify));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getAllowedPaths() {
		if (allowedPaths == null) {
			allowedPaths = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.PLATFORM_MODE__ALLOWED_PATHS);
		}
		return allowedPaths;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.PLATFORM_MODE__TYPE:
				return getType();
			case OrchestrationPackage.PLATFORM_MODE__AUTONOMY_LEVEL:
				return getAutonomyLevel();
			case OrchestrationPackage.PLATFORM_MODE__ITERATION_LIMIT:
				return getIterationLimit();
			case OrchestrationPackage.PLATFORM_MODE__ALLOW_SELF_MODIFY:
				return isAllowSelfModify();
			case OrchestrationPackage.PLATFORM_MODE__ALLOWED_PATHS:
				return getAllowedPaths();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.PLATFORM_MODE__TYPE:
				setType((PlatformType)newValue);
				return;
			case OrchestrationPackage.PLATFORM_MODE__AUTONOMY_LEVEL:
				setAutonomyLevel((AutonomyLevel)newValue);
				return;
			case OrchestrationPackage.PLATFORM_MODE__ITERATION_LIMIT:
				setIterationLimit((Integer)newValue);
				return;
			case OrchestrationPackage.PLATFORM_MODE__ALLOW_SELF_MODIFY:
				setAllowSelfModify((Boolean)newValue);
				return;
			case OrchestrationPackage.PLATFORM_MODE__ALLOWED_PATHS:
				getAllowedPaths().clear();
				getAllowedPaths().addAll((Collection<? extends String>)newValue);
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
			case OrchestrationPackage.PLATFORM_MODE__TYPE:
				setType(TYPE_EDEFAULT);
				return;
			case OrchestrationPackage.PLATFORM_MODE__AUTONOMY_LEVEL:
				setAutonomyLevel(AUTONOMY_LEVEL_EDEFAULT);
				return;
			case OrchestrationPackage.PLATFORM_MODE__ITERATION_LIMIT:
				setIterationLimit(ITERATION_LIMIT_EDEFAULT);
				return;
			case OrchestrationPackage.PLATFORM_MODE__ALLOW_SELF_MODIFY:
				setAllowSelfModify(ALLOW_SELF_MODIFY_EDEFAULT);
				return;
			case OrchestrationPackage.PLATFORM_MODE__ALLOWED_PATHS:
				getAllowedPaths().clear();
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
			case OrchestrationPackage.PLATFORM_MODE__TYPE:
				return type != TYPE_EDEFAULT;
			case OrchestrationPackage.PLATFORM_MODE__AUTONOMY_LEVEL:
				return autonomyLevel != AUTONOMY_LEVEL_EDEFAULT;
			case OrchestrationPackage.PLATFORM_MODE__ITERATION_LIMIT:
				return iterationLimit != ITERATION_LIMIT_EDEFAULT;
			case OrchestrationPackage.PLATFORM_MODE__ALLOW_SELF_MODIFY:
				return allowSelfModify != ALLOW_SELF_MODIFY_EDEFAULT;
			case OrchestrationPackage.PLATFORM_MODE__ALLOWED_PATHS:
				return allowedPaths != null && !allowedPaths.isEmpty();
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
		result.append(" (type: ");
		result.append(type);
		result.append(", autonomyLevel: ");
		result.append(autonomyLevel);
		result.append(", iterationLimit: ");
		result.append(iterationLimit);
		result.append(", allowSelfModify: ");
		result.append(allowSelfModify);
		result.append(", allowedPaths: ");
		result.append(allowedPaths);
		result.append(')');
		return result.toString();
	}

} //PlatformModeImpl
