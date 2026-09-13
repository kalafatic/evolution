/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Forge Session</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getSessionId <em>Session Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getName <em>Name</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getCreatedAt <em>Created At</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getLastModified <em>Last Modified</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getStatus <em>Status</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getActiveModelId <em>Active Model Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getSelectedModelType <em>Selected Model Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getGit <em>Git</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getModelState <em>Model State</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getExperiments <em>Experiments</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getSnapshots <em>Snapshots</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession()
 * @model
 * @generated
 */
public interface ForgeSession extends EObject {
	/**
	 * Returns the value of the '<em><b>Session Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Session Id</em>' attribute.
	 * @see #setSessionId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession_SessionId()
	 * @model
	 * @generated
	 */
	String getSessionId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getSessionId <em>Session Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Session Id</em>' attribute.
	 * @see #getSessionId()
	 * @generated
	 */
	void setSessionId(String value);

	/**
	 * Returns the value of the '<em><b>Name</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Name</em>' attribute.
	 * @see #setName(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession_Name()
	 * @model
	 * @generated
	 */
	String getName();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getName <em>Name</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Name</em>' attribute.
	 * @see #getName()
	 * @generated
	 */
	void setName(String value);

	/**
	 * Returns the value of the '<em><b>Created At</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Created At</em>' attribute.
	 * @see #setCreatedAt(long)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession_CreatedAt()
	 * @model
	 * @generated
	 */
	long getCreatedAt();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getCreatedAt <em>Created At</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Created At</em>' attribute.
	 * @see #getCreatedAt()
	 * @generated
	 */
	void setCreatedAt(long value);

	/**
	 * Returns the value of the '<em><b>Last Modified</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Last Modified</em>' attribute.
	 * @see #setLastModified(long)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession_LastModified()
	 * @model
	 * @generated
	 */
	long getLastModified();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getLastModified <em>Last Modified</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Last Modified</em>' attribute.
	 * @see #getLastModified()
	 * @generated
	 */
	void setLastModified(long value);

	/**
	 * Returns the value of the '<em><b>Status</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.ForgeStatus}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Status</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.ForgeStatus
	 * @see #setStatus(ForgeStatus)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession_Status()
	 * @model
	 * @generated
	 */
	ForgeStatus getStatus();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getStatus <em>Status</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Status</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.ForgeStatus
	 * @see #getStatus()
	 * @generated
	 */
	void setStatus(ForgeStatus value);

	/**
	 * Returns the value of the '<em><b>Active Model Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Active Model Id</em>' attribute.
	 * @see #setActiveModelId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession_ActiveModelId()
	 * @model
	 * @generated
	 */
	String getActiveModelId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getActiveModelId <em>Active Model Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Active Model Id</em>' attribute.
	 * @see #getActiveModelId()
	 * @generated
	 */
	void setActiveModelId(String value);

	/**
	 * Returns the value of the '<em><b>Selected Model Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Selected Model Type</em>' attribute.
	 * @see #setSelectedModelType(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession_SelectedModelType()
	 * @model
	 * @generated
	 */
	String getSelectedModelType();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getSelectedModelType <em>Selected Model Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Selected Model Type</em>' attribute.
	 * @see #getSelectedModelType()
	 * @generated
	 */
	void setSelectedModelType(String value);

	/**
	 * Returns the value of the '<em><b>Git</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Git</em>' containment reference.
	 * @see #setGit(Git)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession_Git()
	 * @model containment="true"
	 * @generated
	 */
	Git getGit();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getGit <em>Git</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Git</em>' containment reference.
	 * @see #getGit()
	 * @generated
	 */
	void setGit(Git value);

	/**
	 * Returns the value of the '<em><b>Model State</b></em>' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Model State</em>' containment reference.
	 * @see #setModelState(SessionModelState)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession_ModelState()
	 * @model containment="true"
	 * @generated
	 */
	SessionModelState getModelState();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ForgeSession#getModelState <em>Model State</em>}' containment reference.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Model State</em>' containment reference.
	 * @see #getModelState()
	 * @generated
	 */
	void setModelState(SessionModelState value);

	/**
	 * Returns the value of the '<em><b>Experiments</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.SessionExperiment}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Experiments</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession_Experiments()
	 * @model containment="true"
	 * @generated
	 */
	EList<SessionExperiment> getExperiments();

	/**
	 * Returns the value of the '<em><b>Snapshots</b></em>' containment reference list.
	 * The list contents are of type {@link eu.kalafatic.evolution.model.orchestration.SessionSnapshot}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Snapshots</em>' containment reference list.
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getForgeSession_Snapshots()
	 * @model containment="true"
	 * @generated
	 */
	EList<SessionSnapshot> getSnapshots();

} // ForgeSession
