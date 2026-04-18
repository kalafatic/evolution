package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Combo Data</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ComboData#getData <em>Data</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ComboData#getDefaultSelection <em>Default Selection</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComboData()
 * @model
 * @generated
 */
public interface ComboData extends EObject {
	/**
	 * Returns the value of the '<em><b>Data</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.ComboElement}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Data</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComboData_Data()
	 * @model containment="true"
	 * @generated
	 */
	EList<ComboElement> getData();

	/**
	 * Returns the value of the '<em><b>Default Selection</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Default Selection</em>' attribute.
	 * @see #setDefaultSelection(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getComboData_DefaultSelection()
	 * @model
	 * @generated
	 */
	int getDefaultSelection();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ComboData#getDefaultSelection <em>Default Selection</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Default Selection</em>' attribute.
	 * @see #getDefaultSelection()
	 * @generated
	 */
	void setDefaultSelection(int value);

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	String[] getItems();

} // ComboData
