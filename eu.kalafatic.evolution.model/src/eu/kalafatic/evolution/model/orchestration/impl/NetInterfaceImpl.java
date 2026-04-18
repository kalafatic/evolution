package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.NetInterface;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Net Interface</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.NetInterfaceImpl#getName <em>Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.NetInterfaceImpl#getDisplayName <em>Display Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.NetInterfaceImpl#getMac <em>Mac</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.NetInterfaceImpl#getAddress <em>Address</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.NetInterfaceImpl#isUp <em>Up</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.NetInterfaceImpl#isVirtual <em>Virtual</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.NetInterfaceImpl#isMulticast <em>Multicast</em>}</li>
 * </ul>
 *
 * @generated
 */
public class NetInterfaceImpl extends MinimalEObjectImpl.Container implements NetInterface {
	/**
	 * The default value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected static final String NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getName() <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getName()
	 * @generated
	 * @ordered
	 */
	protected String name = NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getDisplayName() <em>Display Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDisplayName()
	 * @generated
	 * @ordered
	 */
	protected static final String DISPLAY_NAME_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getDisplayName() <em>Display Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getDisplayName()
	 * @generated
	 * @ordered
	 */
	protected String displayName = DISPLAY_NAME_EDEFAULT;

	/**
	 * The default value of the '{@link #getMac() <em>Mac</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMac()
	 * @generated
	 * @ordered
	 */
	protected static final String MAC_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getMac() <em>Mac</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getMac()
	 * @generated
	 * @ordered
	 */
	protected String mac = MAC_EDEFAULT;

	/**
	 * The cached value of the '{@link #getAddress() <em>Address</em>}' attribute list.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAddress()
	 * @generated
	 * @ordered
	 */
	protected EList<String> address;

	/**
	 * The default value of the '{@link #isUp() <em>Up</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isUp()
	 * @generated
	 * @ordered
	 */
	protected static final boolean UP_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isUp() <em>Up</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isUp()
	 * @generated
	 * @ordered
	 */
	protected boolean up = UP_EDEFAULT;

	/**
	 * The default value of the '{@link #isVirtual() <em>Virtual</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isVirtual()
	 * @generated
	 * @ordered
	 */
	protected static final boolean VIRTUAL_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isVirtual() <em>Virtual</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isVirtual()
	 * @generated
	 * @ordered
	 */
	protected boolean virtual = VIRTUAL_EDEFAULT;

	/**
	 * The default value of the '{@link #isMulticast() <em>Multicast</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isMulticast()
	 * @generated
	 * @ordered
	 */
	protected static final boolean MULTICAST_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isMulticast() <em>Multicast</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isMulticast()
	 * @generated
	 * @ordered
	 */
	protected boolean multicast = MULTICAST_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected NetInterfaceImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.NET_INTERFACE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setName(String newName) {
		String oldName = name;
		name = newName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.NET_INTERFACE__NAME, oldName, name));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setDisplayName(String newDisplayName) {
		String oldDisplayName = displayName;
		displayName = newDisplayName;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.NET_INTERFACE__DISPLAY_NAME, oldDisplayName, displayName));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getMac() {
		return mac;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMac(String newMac) {
		String oldMac = mac;
		mac = newMac;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.NET_INTERFACE__MAC, oldMac, mac));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public EList<String> getAddress() {
		if (address == null) {
			address = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.NET_INTERFACE__ADDRESS);
		}
		return address;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isUp() {
		return up;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setUp(boolean newUp) {
		boolean oldUp = up;
		up = newUp;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.NET_INTERFACE__UP, oldUp, up));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isVirtual() {
		return virtual;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setVirtual(boolean newVirtual) {
		boolean oldVirtual = virtual;
		virtual = newVirtual;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.NET_INTERFACE__VIRTUAL, oldVirtual, virtual));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isMulticast() {
		return multicast;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setMulticast(boolean newMulticast) {
		boolean oldMulticast = multicast;
		multicast = newMulticast;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.NET_INTERFACE__MULTICAST, oldMulticast, multicast));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.NET_INTERFACE__NAME:
				return getName();
			case OrchestrationPackage.NET_INTERFACE__DISPLAY_NAME:
				return getDisplayName();
			case OrchestrationPackage.NET_INTERFACE__MAC:
				return getMac();
			case OrchestrationPackage.NET_INTERFACE__ADDRESS:
				return getAddress();
			case OrchestrationPackage.NET_INTERFACE__UP:
				return isUp();
			case OrchestrationPackage.NET_INTERFACE__VIRTUAL:
				return isVirtual();
			case OrchestrationPackage.NET_INTERFACE__MULTICAST:
				return isMulticast();
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
			case OrchestrationPackage.NET_INTERFACE__NAME:
				setName((String)newValue);
				return;
			case OrchestrationPackage.NET_INTERFACE__DISPLAY_NAME:
				setDisplayName((String)newValue);
				return;
			case OrchestrationPackage.NET_INTERFACE__MAC:
				setMac((String)newValue);
				return;
			case OrchestrationPackage.NET_INTERFACE__ADDRESS:
				getAddress().clear();
				getAddress().addAll((Collection<? extends String>)newValue);
				return;
			case OrchestrationPackage.NET_INTERFACE__UP:
				setUp((Boolean)newValue);
				return;
			case OrchestrationPackage.NET_INTERFACE__VIRTUAL:
				setVirtual((Boolean)newValue);
				return;
			case OrchestrationPackage.NET_INTERFACE__MULTICAST:
				setMulticast((Boolean)newValue);
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
			case OrchestrationPackage.NET_INTERFACE__NAME:
				setName(NAME_EDEFAULT);
				return;
			case OrchestrationPackage.NET_INTERFACE__DISPLAY_NAME:
				setDisplayName(DISPLAY_NAME_EDEFAULT);
				return;
			case OrchestrationPackage.NET_INTERFACE__MAC:
				setMac(MAC_EDEFAULT);
				return;
			case OrchestrationPackage.NET_INTERFACE__ADDRESS:
				getAddress().clear();
				return;
			case OrchestrationPackage.NET_INTERFACE__UP:
				setUp(UP_EDEFAULT);
				return;
			case OrchestrationPackage.NET_INTERFACE__VIRTUAL:
				setVirtual(VIRTUAL_EDEFAULT);
				return;
			case OrchestrationPackage.NET_INTERFACE__MULTICAST:
				setMulticast(MULTICAST_EDEFAULT);
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
			case OrchestrationPackage.NET_INTERFACE__NAME:
				return NAME_EDEFAULT == null ? name != null : !NAME_EDEFAULT.equals(name);
			case OrchestrationPackage.NET_INTERFACE__DISPLAY_NAME:
				return DISPLAY_NAME_EDEFAULT == null ? displayName != null : !DISPLAY_NAME_EDEFAULT.equals(displayName);
			case OrchestrationPackage.NET_INTERFACE__MAC:
				return MAC_EDEFAULT == null ? mac != null : !MAC_EDEFAULT.equals(mac);
			case OrchestrationPackage.NET_INTERFACE__ADDRESS:
				return address != null && !address.isEmpty();
			case OrchestrationPackage.NET_INTERFACE__UP:
				return up != UP_EDEFAULT;
			case OrchestrationPackage.NET_INTERFACE__VIRTUAL:
				return virtual != VIRTUAL_EDEFAULT;
			case OrchestrationPackage.NET_INTERFACE__MULTICAST:
				return multicast != MULTICAST_EDEFAULT;
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
		result.append(" (name: ");
		result.append(name);
		result.append(", displayName: ");
		result.append(displayName);
		result.append(", mac: ");
		result.append(mac);
		result.append(", address: ");
		result.append(address);
		result.append(", up: ");
		result.append(up);
		result.append(", virtual: ");
		result.append(virtual);
		result.append(", multicast: ");
		result.append(multicast);
		result.append(')');
		return result.toString();
	}

} //NetInterfaceImpl
