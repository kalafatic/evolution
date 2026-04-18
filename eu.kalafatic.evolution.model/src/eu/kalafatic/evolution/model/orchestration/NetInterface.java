package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Net Interface</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetInterface#getName <em>Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetInterface#getDisplayName <em>Display Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetInterface#getMac <em>Mac</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetInterface#getAddress <em>Address</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetInterface#isUp <em>Up</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetInterface#isVirtual <em>Virtual</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.NetInterface#isMulticast <em>Multicast</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetInterface()
 * @model
 * @generated
 */
public interface NetInterface extends EObject {
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetInterface_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetInterface#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Display Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Display Name</em>' attribute.
	 * @see #setDisplayName(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetInterface_DisplayName()
	 * @model
	 * @generated
	 */
	String getDisplayName();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetInterface#getDisplayName <em>Display Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Display Name</em>' attribute.
	 * @see #getDisplayName()
	 * @generated
	 */
	void setDisplayName(String value);

	/**
	 * Returns the value of the '<em><b>Mac</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Mac</em>' attribute.
	 * @see #setMac(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetInterface_Mac()
	 * @model
	 * @generated
	 */
	String getMac();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetInterface#getMac <em>Mac</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Mac</em>' attribute.
	 * @see #getMac()
	 * @generated
	 */
	void setMac(String value);

	/**
	 * Returns the value of the '<em><b>Address</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Address</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetInterface_Address()
	 * @model
	 * @generated
	 */
	EList<String> getAddress();

	/**
	 * Returns the value of the '<em><b>Up</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Up</em>' attribute.
	 * @see #setUp(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetInterface_Up()
	 * @model
	 * @generated
	 */
	boolean isUp();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetInterface#isUp <em>Up</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Up</em>' attribute.
	 * @see #isUp()
	 * @generated
	 */
	void setUp(boolean value);

	/**
	 * Returns the value of the '<em><b>Virtual</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Virtual</em>' attribute.
	 * @see #setVirtual(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetInterface_Virtual()
	 * @model
	 * @generated
	 */
	boolean isVirtual();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetInterface#isVirtual <em>Virtual</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Virtual</em>' attribute.
	 * @see #isVirtual()
	 * @generated
	 */
	void setVirtual(boolean value);

	/**
	 * Returns the value of the '<em><b>Multicast</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Multicast</em>' attribute.
	 * @see #setMulticast(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getNetInterface_Multicast()
	 * @model
	 * @generated
	 */
	boolean isMulticast();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.NetInterface#isMulticast <em>Multicast</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Multicast</em>' attribute.
	 * @see #isMulticast()
	 * @generated
	 */
	void setMulticast(boolean value);

} // NetInterface
