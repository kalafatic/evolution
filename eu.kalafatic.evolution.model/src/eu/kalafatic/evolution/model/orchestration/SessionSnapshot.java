/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Session Snapshot</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionSnapshot#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionSnapshot#getSessionId <em>Session Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionSnapshot#getGenomeSnapshotId <em>Genome Snapshot Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionSnapshot#getFullSerializedState <em>Full Serialized State</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.SessionSnapshot#getTimestamp <em>Timestamp</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionSnapshot()
 * @model
 * @generated
 */
public interface SessionSnapshot extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionSnapshot_Id()
	 * @model
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionSnapshot#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Session Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Session Id</em>' attribute.
	 * @see #setSessionId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionSnapshot_SessionId()
	 * @model
	 * @generated
	 */
	String getSessionId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionSnapshot#getSessionId <em>Session Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Session Id</em>' attribute.
	 * @see #getSessionId()
	 * @generated
	 */
	void setSessionId(String value);

	/**
	 * Returns the value of the '<em><b>Genome Snapshot Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Genome Snapshot Id</em>' attribute.
	 * @see #setGenomeSnapshotId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionSnapshot_GenomeSnapshotId()
	 * @model
	 * @generated
	 */
	String getGenomeSnapshotId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionSnapshot#getGenomeSnapshotId <em>Genome Snapshot Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Genome Snapshot Id</em>' attribute.
	 * @see #getGenomeSnapshotId()
	 * @generated
	 */
	void setGenomeSnapshotId(String value);

	/**
	 * Returns the value of the '<em><b>Full Serialized State</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Full Serialized State</em>' attribute.
	 * @see #setFullSerializedState(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionSnapshot_FullSerializedState()
	 * @model
	 * @generated
	 */
	String getFullSerializedState();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionSnapshot#getFullSerializedState <em>Full Serialized State</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Full Serialized State</em>' attribute.
	 * @see #getFullSerializedState()
	 * @generated
	 */
	void setFullSerializedState(String value);

	/**
	 * Returns the value of the '<em><b>Timestamp</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Timestamp</em>' attribute.
	 * @see #setTimestamp(long)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getSessionSnapshot_Timestamp()
	 * @model
	 * @generated
	 */
	long getTimestamp();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.SessionSnapshot#getTimestamp <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Timestamp</em>' attribute.
	 * @see #getTimestamp()
	 * @generated
	 */
	void setTimestamp(long value);

} // SessionSnapshot
