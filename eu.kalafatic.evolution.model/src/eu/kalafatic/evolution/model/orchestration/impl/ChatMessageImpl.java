/**
 */
package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.ChatMessage;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Chat Message</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#getIndex <em>Index</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#getSender <em>Sender</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#getText <em>Text</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#getColor <em>Color</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#isIsBold <em>Is Bold</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#isIsItalic <em>Is Italic</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#getAgentType <em>Agent Type</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#getTimestamp <em>Timestamp</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#getPriority <em>Priority</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#getSequenceNumber <em>Sequence Number</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#getTurnId <em>Turn Id</em>}</li>
 *   <li>{@link eu.kalafatic.evolution.model.orchestration.impl.ChatMessageImpl#isIsTerminal <em>Is Terminal</em>}</li>
 * </ul>
 *
 * @generated
 */
public class ChatMessageImpl extends MinimalEObjectImpl.Container implements ChatMessage {
	/**
	 * The default value of the '{@link #getIndex() <em>Index</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIndex()
	 * @generated
	 * @ordered
	 */
	protected static final int INDEX_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getIndex() <em>Index</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getIndex()
	 * @generated
	 * @ordered
	 */
	protected int index = INDEX_EDEFAULT;

	/**
	 * The default value of the '{@link #getSender() <em>Sender</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSender()
	 * @generated
	 * @ordered
	 */
	protected static final String SENDER_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getSender() <em>Sender</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSender()
	 * @generated
	 * @ordered
	 */
	protected String sender = SENDER_EDEFAULT;

	/**
	 * The default value of the '{@link #getText() <em>Text</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getText()
	 * @generated
	 * @ordered
	 */
	protected static final String TEXT_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getText() <em>Text</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getText()
	 * @generated
	 * @ordered
	 */
	protected String text = TEXT_EDEFAULT;

	/**
	 * The default value of the '{@link #getColor() <em>Color</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getColor()
	 * @generated
	 * @ordered
	 */
	protected static final String COLOR_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getColor() <em>Color</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getColor()
	 * @generated
	 * @ordered
	 */
	protected String color = COLOR_EDEFAULT;

	/**
	 * The default value of the '{@link #isIsBold() <em>Is Bold</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIsBold()
	 * @generated
	 * @ordered
	 */
	protected static final boolean IS_BOLD_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isIsBold() <em>Is Bold</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIsBold()
	 * @generated
	 * @ordered
	 */
	protected boolean isBold = IS_BOLD_EDEFAULT;

	/**
	 * The default value of the '{@link #isIsItalic() <em>Is Italic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIsItalic()
	 * @generated
	 * @ordered
	 */
	protected static final boolean IS_ITALIC_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isIsItalic() <em>Is Italic</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIsItalic()
	 * @generated
	 * @ordered
	 */
	protected boolean isItalic = IS_ITALIC_EDEFAULT;

	/**
	 * The default value of the '{@link #getAgentType() <em>Agent Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAgentType()
	 * @generated
	 * @ordered
	 */
	protected static final String AGENT_TYPE_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getAgentType() <em>Agent Type</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAgentType()
	 * @generated
	 * @ordered
	 */
	protected String agentType = AGENT_TYPE_EDEFAULT;

	/**
	 * The default value of the '{@link #getTimestamp() <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTimestamp()
	 * @generated
	 * @ordered
	 */
	protected static final String TIMESTAMP_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTimestamp() <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTimestamp()
	 * @generated
	 * @ordered
	 */
	protected String timestamp = TIMESTAMP_EDEFAULT;

	/**
	 * The default value of the '{@link #getPriority() <em>Priority</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPriority()
	 * @generated
	 * @ordered
	 */
	protected static final int PRIORITY_EDEFAULT = 0;

	/**
	 * The cached value of the '{@link #getPriority() <em>Priority</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getPriority()
	 * @generated
	 * @ordered
	 */
	protected int priority = PRIORITY_EDEFAULT;

	/**
	 * The default value of the '{@link #getSequenceNumber() <em>Sequence Number</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSequenceNumber()
	 * @generated
	 * @ordered
	 */
	protected static final long SEQUENCE_NUMBER_EDEFAULT = 0L;

	/**
	 * The cached value of the '{@link #getSequenceNumber() <em>Sequence Number</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getSequenceNumber()
	 * @generated
	 * @ordered
	 */
	protected long sequenceNumber = SEQUENCE_NUMBER_EDEFAULT;

	/**
	 * The default value of the '{@link #getTurnId() <em>Turn Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTurnId()
	 * @generated
	 * @ordered
	 */
	protected static final String TURN_ID_EDEFAULT = null;

	/**
	 * The cached value of the '{@link #getTurnId() <em>Turn Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTurnId()
	 * @generated
	 * @ordered
	 */
	protected String turnId = TURN_ID_EDEFAULT;

	/**
	 * The default value of the '{@link #isIsTerminal() <em>Is Terminal</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIsTerminal()
	 * @generated
	 * @ordered
	 */
	protected static final boolean IS_TERMINAL_EDEFAULT = false;

	/**
	 * The cached value of the '{@link #isIsTerminal() <em>Is Terminal</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isIsTerminal()
	 * @generated
	 * @ordered
	 */
	protected boolean isTerminal = IS_TERMINAL_EDEFAULT;

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	protected ChatMessageImpl() {
		super();
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	protected EClass eStaticClass() {
		return OrchestrationPackage.Literals.CHAT_MESSAGE;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getIndex() {
		return index;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setIndex(int newIndex) {
		int oldIndex = index;
		index = newIndex;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__INDEX, oldIndex, index));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getSender() {
		return sender;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSender(String newSender) {
		String oldSender = sender;
		sender = newSender;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__SENDER, oldSender, sender));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getText() {
		return text;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setText(String newText) {
		String oldText = text;
		text = newText;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__TEXT, oldText, text));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getColor() {
		return color;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setColor(String newColor) {
		String oldColor = color;
		color = newColor;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__COLOR, oldColor, color));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isIsBold() {
		return isBold;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setIsBold(boolean newIsBold) {
		boolean oldIsBold = isBold;
		isBold = newIsBold;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__IS_BOLD, oldIsBold, isBold));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isIsItalic() {
		return isItalic;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setIsItalic(boolean newIsItalic) {
		boolean oldIsItalic = isItalic;
		isItalic = newIsItalic;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__IS_ITALIC, oldIsItalic, isItalic));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getAgentType() {
		return agentType;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setAgentType(String newAgentType) {
		String oldAgentType = agentType;
		agentType = newAgentType;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__AGENT_TYPE, oldAgentType, agentType));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTimestamp(String newTimestamp) {
		String oldTimestamp = timestamp;
		timestamp = newTimestamp;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__TIMESTAMP, oldTimestamp, timestamp));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public int getPriority() {
		return priority;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setPriority(int newPriority) {
		int oldPriority = priority;
		priority = newPriority;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__PRIORITY, oldPriority, priority));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public long getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setSequenceNumber(long newSequenceNumber) {
		long oldSequenceNumber = sequenceNumber;
		sequenceNumber = newSequenceNumber;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__SEQUENCE_NUMBER, oldSequenceNumber, sequenceNumber));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public String getTurnId() {
		return turnId;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setTurnId(String newTurnId) {
		String oldTurnId = turnId;
		turnId = newTurnId;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__TURN_ID, oldTurnId, turnId));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public boolean isIsTerminal() {
		return isTerminal;
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void setIsTerminal(boolean newIsTerminal) {
		boolean oldIsTerminal = isTerminal;
		isTerminal = newIsTerminal;
		if (eNotificationRequired())
			eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.CHAT_MESSAGE__IS_TERMINAL, oldIsTerminal, isTerminal));
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public Object eGet(int featureID, boolean resolve, boolean coreType) {
		switch (featureID) {
			case OrchestrationPackage.CHAT_MESSAGE__INDEX:
				return getIndex();
			case OrchestrationPackage.CHAT_MESSAGE__SENDER:
				return getSender();
			case OrchestrationPackage.CHAT_MESSAGE__TEXT:
				return getText();
			case OrchestrationPackage.CHAT_MESSAGE__COLOR:
				return getColor();
			case OrchestrationPackage.CHAT_MESSAGE__IS_BOLD:
				return isIsBold();
			case OrchestrationPackage.CHAT_MESSAGE__IS_ITALIC:
				return isIsItalic();
			case OrchestrationPackage.CHAT_MESSAGE__AGENT_TYPE:
				return getAgentType();
			case OrchestrationPackage.CHAT_MESSAGE__TIMESTAMP:
				return getTimestamp();
			case OrchestrationPackage.CHAT_MESSAGE__PRIORITY:
				return getPriority();
			case OrchestrationPackage.CHAT_MESSAGE__SEQUENCE_NUMBER:
				return getSequenceNumber();
			case OrchestrationPackage.CHAT_MESSAGE__TURN_ID:
				return getTurnId();
			case OrchestrationPackage.CHAT_MESSAGE__IS_TERMINAL:
				return isIsTerminal();
		}
		return super.eGet(featureID, resolve, coreType);
	}

	/**
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @generated
	 */
	@Override
	public void eSet(int featureID, Object newValue) {
		switch (featureID) {
			case OrchestrationPackage.CHAT_MESSAGE__INDEX:
				setIndex((Integer)newValue);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__SENDER:
				setSender((String)newValue);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__TEXT:
				setText((String)newValue);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__COLOR:
				setColor((String)newValue);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__IS_BOLD:
				setIsBold((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__IS_ITALIC:
				setIsItalic((Boolean)newValue);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__AGENT_TYPE:
				setAgentType((String)newValue);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__TIMESTAMP:
				setTimestamp((String)newValue);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__PRIORITY:
				setPriority((Integer)newValue);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__SEQUENCE_NUMBER:
				setSequenceNumber((Long)newValue);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__TURN_ID:
				setTurnId((String)newValue);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__IS_TERMINAL:
				setIsTerminal((Boolean)newValue);
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
			case OrchestrationPackage.CHAT_MESSAGE__INDEX:
				setIndex(INDEX_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__SENDER:
				setSender(SENDER_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__TEXT:
				setText(TEXT_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__COLOR:
				setColor(COLOR_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__IS_BOLD:
				setIsBold(IS_BOLD_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__IS_ITALIC:
				setIsItalic(IS_ITALIC_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__AGENT_TYPE:
				setAgentType(AGENT_TYPE_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__TIMESTAMP:
				setTimestamp(TIMESTAMP_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__PRIORITY:
				setPriority(PRIORITY_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__SEQUENCE_NUMBER:
				setSequenceNumber(SEQUENCE_NUMBER_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__TURN_ID:
				setTurnId(TURN_ID_EDEFAULT);
				return;
			case OrchestrationPackage.CHAT_MESSAGE__IS_TERMINAL:
				setIsTerminal(IS_TERMINAL_EDEFAULT);
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
			case OrchestrationPackage.CHAT_MESSAGE__INDEX:
				return index != INDEX_EDEFAULT;
			case OrchestrationPackage.CHAT_MESSAGE__SENDER:
				return SENDER_EDEFAULT == null ? sender != null : !SENDER_EDEFAULT.equals(sender);
			case OrchestrationPackage.CHAT_MESSAGE__TEXT:
				return TEXT_EDEFAULT == null ? text != null : !TEXT_EDEFAULT.equals(text);
			case OrchestrationPackage.CHAT_MESSAGE__COLOR:
				return COLOR_EDEFAULT == null ? color != null : !COLOR_EDEFAULT.equals(color);
			case OrchestrationPackage.CHAT_MESSAGE__IS_BOLD:
				return isBold != IS_BOLD_EDEFAULT;
			case OrchestrationPackage.CHAT_MESSAGE__IS_ITALIC:
				return isItalic != IS_ITALIC_EDEFAULT;
			case OrchestrationPackage.CHAT_MESSAGE__AGENT_TYPE:
				return AGENT_TYPE_EDEFAULT == null ? agentType != null : !AGENT_TYPE_EDEFAULT.equals(agentType);
			case OrchestrationPackage.CHAT_MESSAGE__TIMESTAMP:
				return TIMESTAMP_EDEFAULT == null ? timestamp != null : !TIMESTAMP_EDEFAULT.equals(timestamp);
			case OrchestrationPackage.CHAT_MESSAGE__PRIORITY:
				return priority != PRIORITY_EDEFAULT;
			case OrchestrationPackage.CHAT_MESSAGE__SEQUENCE_NUMBER:
				return sequenceNumber != SEQUENCE_NUMBER_EDEFAULT;
			case OrchestrationPackage.CHAT_MESSAGE__TURN_ID:
				return TURN_ID_EDEFAULT == null ? turnId != null : !TURN_ID_EDEFAULT.equals(turnId);
			case OrchestrationPackage.CHAT_MESSAGE__IS_TERMINAL:
				return isTerminal != IS_TERMINAL_EDEFAULT;
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
		result.append(" (index: ");
		result.append(index);
		result.append(", sender: ");
		result.append(sender);
		result.append(", text: ");
		result.append(text);
		result.append(", color: ");
		result.append(color);
		result.append(", isBold: ");
		result.append(isBold);
		result.append(", isItalic: ");
		result.append(isItalic);
		result.append(", agentType: ");
		result.append(agentType);
		result.append(", timestamp: ");
		result.append(timestamp);
		result.append(", priority: ");
		result.append(priority);
		result.append(", sequenceNumber: ");
		result.append(sequenceNumber);
		result.append(", turnId: ");
		result.append(turnId);
		result.append(", isTerminal: ");
		result.append(isTerminal);
		result.append(')');
		return result.toString();
	}

} //ChatMessageImpl
