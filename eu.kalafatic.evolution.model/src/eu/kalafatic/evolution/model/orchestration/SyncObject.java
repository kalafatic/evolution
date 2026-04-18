package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Sync Object</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SyncObject#getName <em>Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SyncObject#getData <em>Data</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SyncObject#getType <em>Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SyncObject#getFlags <em>Flags</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SyncObject#getNotes <em>Notes</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSyncObject()
 * @model
 * @generated
 */
public interface SyncObject extends EObject {
	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSyncObject_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SyncObject#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Data</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Data</em>' attribute.
	 * @see #setData(Object)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSyncObject_Data()
	 * @model
	 * @generated
	 */
	Object getData();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SyncObject#getData <em>Data</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Data</em>' attribute.
	 * @see #getData()
	 * @generated
	 */
	void setData(Object value);

	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.SyncType}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.SyncType
	 * @see #setType(SyncType)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSyncObject_Type()
	 * @model
	 * @generated
	 */
	SyncType getType();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SyncObject#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.SyncType
	 * @see #getType()
	 * @generated
	 */
	void setType(SyncType value);

	/**
	 * Returns the value of the '<em><b>Flags</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Flags</em>' attribute.
	 * @see #setFlags(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSyncObject_Flags()
	 * @model
	 * @generated
	 */
	int getFlags();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SyncObject#getFlags <em>Flags</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Flags</em>' attribute.
	 * @see #getFlags()
	 * @generated
	 */
	void setFlags(int value);

	/**
	 * Returns the value of the '<em><b>Notes</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Notes</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSyncObject_Notes()
	 * @model
	 * @generated
	 */
	EList<String> getNotes();

} // SyncObject
