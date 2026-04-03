/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Compiler</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Compiler#getSourceVersion <em>Source Version</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.Compiler#getTargetVersion <em>Target Version</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getCompiler()
 * @model
 * @generated
 */
public interface Compiler extends EObject {
	/**
	 * Returns the value of the '<em><b>Source Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Source Version</em>' attribute.
	 * @see #setSourceVersion(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getCompiler_SourceVersion()
	 * @model
	 * @generated
	 */
	String getSourceVersion();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getSourceVersion <em>Source Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Source Version</em>' attribute.
	 * @see #getSourceVersion()
	 * @generated
	 */
	void setSourceVersion(String value);

	/**
	 * Returns the value of the '<em><b>Target Version</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Target Version</em>' attribute.
	 * @see #setTargetVersion(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getCompiler_TargetVersion()
	 * @model
	 * @generated
	 */
	String getTargetVersion();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getTargetVersion <em>Target Version</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Target Version</em>' attribute.
	 * @see #getTargetVersion()
	 * @generated
	 */
	void setTargetVersion(String value);

	/**
	 * Returns the value of the '<em><b>CPath</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>CPath</em>' attribute.
	 * @see #setCPath(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getCompiler_CPath()
	 * @model
	 * @generated
	 */
	String getCPath();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getCPath <em>CPath</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>CPath</em>' attribute.
	 * @see #getCPath()
	 * @generated
	 */
	void setCPath(String value);

	/**
	 * Returns the value of the '<em><b>Cpp Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Cpp Path</em>' attribute.
	 * @see #setCppPath(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getCompiler_CppPath()
	 * @model
	 * @generated
	 */
	String getCppPath();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getCppPath <em>Cpp Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Cpp Path</em>' attribute.
	 * @see #getCppPath()
	 * @generated
	 */
	void setCppPath(String value);

	/**
	 * Returns the value of the '<em><b>Make Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Make Path</em>' attribute.
	 * @see #setMakePath(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getCompiler_MakePath()
	 * @model
	 * @generated
	 */
	String getMakePath();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getMakePath <em>Make Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Make Path</em>' attribute.
	 * @see #getMakePath()
	 * @generated
	 */
	void setMakePath(String value);

	/**
	 * Returns the value of the '<em><b>Cmake Path</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Cmake Path</em>' attribute.
	 * @see #setCmakePath(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getCompiler_CmakePath()
	 * @model
	 * @generated
	 */
	String getCmakePath();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getCmakePath <em>Cmake Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Cmake Path</em>' attribute.
	 * @see #getCmakePath()
	 * @generated
	 */
	void setCmakePath(String value);

	/**
	 * Returns the value of the '<em><b>Test Status</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Test Status</em>' attribute.
	 * @see #setTestStatus(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getCompiler_TestStatus()
	 * @model
	 * @generated
	 */
	String getTestStatus();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.Compiler#getTestStatus <em>Test Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Test Status</em>' attribute.
	 * @see #getTestStatus()
	 * @generated
	 */
	void setTestStatus(String value);

} // Compiler
