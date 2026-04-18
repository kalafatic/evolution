package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Platform Mode</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.PlatformMode#getType <em>Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.PlatformMode#getAutonomyLevel <em>Autonomy Level</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.PlatformMode#getIterationLimit <em>Iteration Limit</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.PlatformMode#isAllowSelfModify <em>Allow Self Modify</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.PlatformMode#getAllowedPaths <em>Allowed Paths</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPlatformMode()
 * @model
 * @generated
 */
public interface PlatformMode extends EObject {
	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.PlatformType}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.PlatformType
	 * @see #setType(PlatformType)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPlatformMode_Type()
	 * @model
	 * @generated
	 */
	PlatformType getType();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.PlatformMode#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.PlatformType
	 * @see #getType()
	 * @generated
	 */
	void setType(PlatformType value);

	/**
	 * Returns the value of the '<em><b>Autonomy Level</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.AutonomyLevel}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Autonomy Level</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.AutonomyLevel
	 * @see #setAutonomyLevel(AutonomyLevel)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPlatformMode_AutonomyLevel()
	 * @model
	 * @generated
	 */
	AutonomyLevel getAutonomyLevel();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.PlatformMode#getAutonomyLevel <em>Autonomy Level</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Autonomy Level</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.AutonomyLevel
	 * @see #getAutonomyLevel()
	 * @generated
	 */
	void setAutonomyLevel(AutonomyLevel value);

	/**
	 * Returns the value of the '<em><b>Iteration Limit</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Iteration Limit</em>' attribute.
	 * @see #setIterationLimit(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPlatformMode_IterationLimit()
	 * @model
	 * @generated
	 */
	int getIterationLimit();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.PlatformMode#getIterationLimit <em>Iteration Limit</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Iteration Limit</em>' attribute.
	 * @see #getIterationLimit()
	 * @generated
	 */
	void setIterationLimit(int value);

	/**
	 * Returns the value of the '<em><b>Allow Self Modify</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Allow Self Modify</em>' attribute.
	 * @see #setAllowSelfModify(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPlatformMode_AllowSelfModify()
	 * @model
	 * @generated
	 */
	boolean isAllowSelfModify();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.PlatformMode#isAllowSelfModify <em>Allow Self Modify</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Allow Self Modify</em>' attribute.
	 * @see #isAllowSelfModify()
	 * @generated
	 */
	void setAllowSelfModify(boolean value);

	/**
	 * Returns the value of the '<em><b>Allowed Paths</b></em>' attribute list.
	 * The list contents are of type {@link java.lang.String}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Allowed Paths</em>' attribute list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getPlatformMode_AllowedPaths()
	 * @model
	 * @generated
	 */
	EList<String> getAllowedPaths();

} // PlatformMode
