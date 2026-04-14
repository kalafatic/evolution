/**
 */
package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Chat Message</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getIndex <em>Index</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getSender <em>Sender</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getText <em>Text</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getColor <em>Color</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#isIsBold <em>Is Bold</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#isIsItalic <em>Is Italic</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getAgentType <em>Agent Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getTimestamp <em>Timestamp</em>}</li>
 * </ul>
 *
 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatMessage()
 * @model
 * @generated
 */
public interface ChatMessage extends EObject {
	/**
	 * Returns the value of the '<em><b>Index</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Index</em>' attribute.
	 * @see #setIndex(int)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatMessage_Index()
	 * @model
	 * @generated
	 */
	int getIndex();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getIndex <em>Index</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Index</em>' attribute.
	 * @see #getIndex()
	 * @generated
	 */
	void setIndex(int value);

	/**
	 * Returns the value of the '<em><b>Sender</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Sender</em>' attribute.
	 * @see #setSender(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatMessage_Sender()
	 * @model
	 * @generated
	 */
	String getSender();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getSender <em>Sender</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Sender</em>' attribute.
	 * @see #getSender()
	 * @generated
	 */
	void setSender(String value);

	/**
	 * Returns the value of the '<em><b>Text</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Text</em>' attribute.
	 * @see #setText(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatMessage_Text()
	 * @model
	 * @generated
	 */
	String getText();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getText <em>Text</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Text</em>' attribute.
	 * @see #getText()
	 * @generated
	 */
	void setText(String value);

	/**
	 * Returns the value of the '<em><b>Color</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Color</em>' attribute.
	 * @see #setColor(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatMessage_Color()
	 * @model
	 * @generated
	 */
	String getColor();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getColor <em>Color</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Color</em>' attribute.
	 * @see #getColor()
	 * @generated
	 */
	void setColor(String value);

	/**
	 * Returns the value of the '<em><b>Is Bold</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Is Bold</em>' attribute.
	 * @see #setIsBold(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatMessage_IsBold()
	 * @model
	 * @generated
	 */
	boolean isIsBold();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#isIsBold <em>Is Bold</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Is Bold</em>' attribute.
	 * @see #isIsBold()
	 * @generated
	 */
	void setIsBold(boolean value);

	/**
	 * Returns the value of the '<em><b>Is Italic</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Is Italic</em>' attribute.
	 * @see #setIsItalic(boolean)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatMessage_IsItalic()
	 * @model
	 * @generated
	 */
	boolean isIsItalic();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#isIsItalic <em>Is Italic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Is Italic</em>' attribute.
	 * @see #isIsItalic()
	 * @generated
	 */
	void setIsItalic(boolean value);

	/**
	 * Returns the value of the '<em><b>Agent Type</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Agent Type</em>' attribute.
	 * @see #setAgentType(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatMessage_AgentType()
	 * @model
	 * @generated
	 */
	String getAgentType();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getAgentType <em>Agent Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Agent Type</em>' attribute.
	 * @see #getAgentType()
	 * @generated
	 */
	void setAgentType(String value);

	/**
	 * Returns the value of the '<em><b>Timestamp</b></em>' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @return the value of the '<em>Timestamp</em>' attribute.
	 * @see #setTimestamp(String)
	 * @see eu.kalafatic.evolution.model.orchestration.OrchestrationPackage#getChatMessage_Timestamp()
	 * @model
	 * @generated
	 */
	String getTimestamp();

	/**
	 * Sets the value of the '{@link eu.kalafatic.evolution.model.orchestration.ChatMessage#getTimestamp <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @param value the new value of the '<em>Timestamp</em>' attribute.
	 * @see #getTimestamp()
	 * @generated
	 */
	void setTimestamp(String value);

} // ChatMessage
