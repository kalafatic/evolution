/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Server Session</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ServerSession#getId <em>Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ServerSession#getType <em>Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ServerSession#getStartTime <em>Start Time</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ServerSession#getLastActivity <em>Last Activity</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ServerSession#getClientIp <em>Client Ip</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSession()
 * @model
 * @generated
 */
public interface ServerSession extends EObject {
	/**
	 * Returns the value of the '<em><b>Id</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Id</em>' attribute.
	 * @see #setId(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSession_Id()
	 * @model
	 * @generated
	 */
	String getId();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ServerSession#getId <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Id</em>' attribute.
	 * @see #getId()
	 * @generated
	 */
	void setId(String value);

	/**
	 * Returns the value of the '<em><b>Type</b></em>' attribute.
	 * The literals are from the enumeration {@link eu.kalafatic.evolution.model.orchestration.SessionType}.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Type</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.SessionType
	 * @see #setType(SessionType)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSession_Type()
	 * @model
	 * @generated
	 */
	SessionType getType();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ServerSession#getType <em>Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Type</em>' attribute.
	 * @see eu.kalafatic.evolution.model.orchestration.SessionType
	 * @see #getType()
	 * @generated
	 */
	void setType(SessionType value);

	/**
	 * Returns the value of the '<em><b>Start Time</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Start Time</em>' attribute.
	 * @see #setStartTime(long)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSession_StartTime()
	 * @model
	 * @generated
	 */
	long getStartTime();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ServerSession#getStartTime <em>Start Time</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Start Time</em>' attribute.
	 * @see #getStartTime()
	 * @generated
	 */
	void setStartTime(long value);

	/**
	 * Returns the value of the '<em><b>Last Activity</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Last Activity</em>' attribute.
	 * @see #setLastActivity(long)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSession_LastActivity()
	 * @model
	 * @generated
	 */
	long getLastActivity();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ServerSession#getLastActivity <em>Last Activity</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Last Activity</em>' attribute.
	 * @see #getLastActivity()
	 * @generated
	 */
	void setLastActivity(long value);

	/**
	 * Returns the value of the '<em><b>Client Ip</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Client Ip</em>' attribute.
	 * @see #setClientIp(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getServerSession_ClientIp()
	 * @model
	 * @generated
	 */
	String getClientIp();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ServerSession#getClientIp <em>Client Ip</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Client Ip</em>' attribute.
	 * @see #getClientIp()
	 * @generated
	 */
	void setClientIp(String value);

} // ServerSession
