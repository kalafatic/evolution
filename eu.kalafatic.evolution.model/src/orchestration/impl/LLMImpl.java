/**
 */
package orchestration.impl;

import orchestration.LLM;
import orchestration.OrchestrationPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>LLM</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link orchestration.impl.LLMImpl#getModel <em>Model</em>}</li>
 *   <li>{@link orchestration.impl.LLMImpl#getTemperature <em>Temperature</em>}</li>
 * </ul>
 *
 * @generated
 */
public class LLMImpl extends MinimalEObjectImpl.Container implements LLM {
	/**
	 * The default value of the '{@link #getModel() <em>Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getModel()
	 * @generated
	 * @ordered
	 */
	protected static final String MODEL_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getModel() <em>Model</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getModel()
	 * @generated
	 * @ordered
	 */
	protected String model = MODEL_EDEFAULT;

	/**
	 * The default value of the '{@link #getTemperature() <em>Temperature</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTemperature()
	 * @generated
	 * @ordered
	 */
	protected static final float TEMPERATURE_EDEFAULT = 0.0F;

	/**
	 * The cached value of the '{@link #getTemperature() <em>Temperature</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTemperature()
	 * @generated
	 * @ordered
	 */
	protected float temperature = TEMPERATURE_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected LLMImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.LLM;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public String getModel() {
		return model;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setModel(String newModel) {
		String oldModel = model;
		model = newModel;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.LLM__MODEL, oldModel, model));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public float getTemperature() {
		return temperature;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	public void setTemperature(float newTemperature) {
		float oldTemperature = temperature;
		temperature = newTemperature;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.LLM__TEMPERATURE, oldTemperature, temperature));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.LLM__MODEL:
				return getModel();
			case OrchestrationPackage.LLM__TEMPERATURE:
				return getTemperature();
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
			case OrchestrationPackage.LLM__MODEL:
				setModel((String)newValue);
				return;
			case OrchestrationPackage.LLM__TEMPERATURE:
				setTemperature((Float)newValue);
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
			case OrchestrationPackage.LLM__MODEL:
				setModel(MODEL_EDEFAULT);
				return;
			case OrchestrationPackage.LLM__TEMPERATURE:
				setTemperature(TEMPERATURE_EDEFAULT);
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
			case OrchestrationPackage.LLM__MODEL:
				return MODEL_EDEFAULT == null ? model != null : !MODEL_EDEFAULT.equals(model);
			case OrchestrationPackage.LLM__TEMPERATURE:
				return temperature != TEMPERATURE_EDEFAULT;
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
		result.append(" (model: ");
		result.append(model);
		result.append(", temperature: ");
		result.append(temperature);
		result.append(')');
		return result.toString();
	}

} //LLMImpl
