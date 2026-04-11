package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.Comment;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

public class CommentImpl extends MinimalEObjectImpl.Container implements Comment {
    /**
	 * The default value of the '{@link #getId() <em>Id</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getId()
	 * @generated
	 * @ordered
	 */
	protected static final String ID_EDEFAULT = null;
				protected String id;
    /**
	 * The default value of the '{@link #getFilePath() <em>File Path</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getFilePath()
	 * @generated
	 * @ordered
	 */
	protected static final String FILE_PATH_EDEFAULT = null;
				protected String filePath;
    /**
	 * The default value of the '{@link #getStartLine() <em>Start Line</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getStartLine()
	 * @generated
	 * @ordered
	 */
	protected static final int START_LINE_EDEFAULT = 0;
				protected int startLine;
    /**
	 * The default value of the '{@link #getEndLine() <em>End Line</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getEndLine()
	 * @generated
	 * @ordered
	 */
	protected static final int END_LINE_EDEFAULT = 0;
				protected int endLine;
    /**
	 * The default value of the '{@link #getAuthor() <em>Author</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getAuthor()
	 * @generated
	 * @ordered
	 */
	protected static final String AUTHOR_EDEFAULT = null;
				protected String author;
    /**
	 * The default value of the '{@link #getContent() <em>Content</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getContent()
	 * @generated
	 * @ordered
	 */
	protected static final String CONTENT_EDEFAULT = null;
				protected String content;
    /**
	 * The default value of the '{@link #getTimestamp() <em>Timestamp</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #getTimestamp()
	 * @generated
	 * @ordered
	 */
	protected static final String TIMESTAMP_EDEFAULT = null;
				protected String timestamp;
    /**
	 * The default value of the '{@link #isResolved() <em>Resolved</em>}' attribute.
	 * <!-- begin-user-doc -->
	 * <!-- end-user-doc -->
	 * @see #isResolved()
	 * @generated
	 * @ordered
	 */
	protected static final boolean RESOLVED_EDEFAULT = false;
				protected boolean resolved;

    protected CommentImpl() { super(); }

    @Override protected EClass eStaticClass() { return OrchestrationPackage.Literals.COMMENT; }

    @Override public String getId() { return id; }
    @Override public void setId(String newId) {
        String oldId = id; id = newId;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMMENT__ID, oldId, id));
    }

    @Override public String getFilePath() { return filePath; }
    @Override public void setFilePath(String newFilePath) {
        String oldFilePath = filePath; filePath = newFilePath;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMMENT__FILE_PATH, oldFilePath, filePath));
    }

    @Override public int getStartLine() { return startLine; }
    @Override public void setStartLine(int newStartLine) {
        int oldStartLine = startLine; startLine = newStartLine;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMMENT__START_LINE, oldStartLine, startLine));
    }

    @Override public int getEndLine() { return endLine; }
    @Override public void setEndLine(int newEndLine) {
        int oldEndLine = endLine; endLine = newEndLine;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMMENT__END_LINE, oldEndLine, endLine));
    }

    @Override public String getAuthor() { return author; }
    @Override public void setAuthor(String newAuthor) {
        String oldAuthor = author; author = newAuthor;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMMENT__AUTHOR, oldAuthor, author));
    }

    @Override public String getContent() { return content; }
    @Override public void setContent(String newContent) {
        String oldContent = content; content = newContent;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMMENT__CONTENT, oldContent, content));
    }

    @Override public String getTimestamp() { return timestamp; }
    @Override public void setTimestamp(String newTimestamp) {
        String oldTimestamp = timestamp; timestamp = newTimestamp;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMMENT__TIMESTAMP, oldTimestamp, timestamp));
    }

    @Override public boolean isResolved() { return resolved; }
    @Override public void setResolved(boolean newResolved) {
        boolean oldResolved = resolved; resolved = newResolved;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.COMMENT__RESOLVED, oldResolved, resolved));
    }

    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case OrchestrationPackage.COMMENT__ID: return getId();
            case OrchestrationPackage.COMMENT__FILE_PATH: return getFilePath();
            case OrchestrationPackage.COMMENT__START_LINE: return getStartLine();
            case OrchestrationPackage.COMMENT__END_LINE: return getEndLine();
            case OrchestrationPackage.COMMENT__AUTHOR: return getAuthor();
            case OrchestrationPackage.COMMENT__CONTENT: return getContent();
            case OrchestrationPackage.COMMENT__TIMESTAMP: return getTimestamp();
            case OrchestrationPackage.COMMENT__RESOLVED: return isResolved();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    @Override
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case OrchestrationPackage.COMMENT__ID: setId((String)newValue); return;
            case OrchestrationPackage.COMMENT__FILE_PATH: setFilePath((String)newValue); return;
            case OrchestrationPackage.COMMENT__START_LINE: setStartLine((Integer)newValue); return;
            case OrchestrationPackage.COMMENT__END_LINE: setEndLine((Integer)newValue); return;
            case OrchestrationPackage.COMMENT__AUTHOR: setAuthor((String)newValue); return;
            case OrchestrationPackage.COMMENT__CONTENT: setContent((String)newValue); return;
            case OrchestrationPackage.COMMENT__TIMESTAMP: setTimestamp((String)newValue); return;
            case OrchestrationPackage.COMMENT__RESOLVED: setResolved((Boolean)newValue); return;
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
			case OrchestrationPackage.COMMENT__ID:
				setId(ID_EDEFAULT);
				return;
			case OrchestrationPackage.COMMENT__FILE_PATH:
				setFilePath(FILE_PATH_EDEFAULT);
				return;
			case OrchestrationPackage.COMMENT__START_LINE:
				setStartLine(START_LINE_EDEFAULT);
				return;
			case OrchestrationPackage.COMMENT__END_LINE:
				setEndLine(END_LINE_EDEFAULT);
				return;
			case OrchestrationPackage.COMMENT__AUTHOR:
				setAuthor(AUTHOR_EDEFAULT);
				return;
			case OrchestrationPackage.COMMENT__CONTENT:
				setContent(CONTENT_EDEFAULT);
				return;
			case OrchestrationPackage.COMMENT__TIMESTAMP:
				setTimestamp(TIMESTAMP_EDEFAULT);
				return;
			case OrchestrationPackage.COMMENT__RESOLVED:
				setResolved(RESOLVED_EDEFAULT);
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
			case OrchestrationPackage.COMMENT__ID:
				return ID_EDEFAULT == null ? id != null : !ID_EDEFAULT.equals(id);
			case OrchestrationPackage.COMMENT__FILE_PATH:
				return FILE_PATH_EDEFAULT == null ? filePath != null : !FILE_PATH_EDEFAULT.equals(filePath);
			case OrchestrationPackage.COMMENT__START_LINE:
				return startLine != START_LINE_EDEFAULT;
			case OrchestrationPackage.COMMENT__END_LINE:
				return endLine != END_LINE_EDEFAULT;
			case OrchestrationPackage.COMMENT__AUTHOR:
				return AUTHOR_EDEFAULT == null ? author != null : !AUTHOR_EDEFAULT.equals(author);
			case OrchestrationPackage.COMMENT__CONTENT:
				return CONTENT_EDEFAULT == null ? content != null : !CONTENT_EDEFAULT.equals(content);
			case OrchestrationPackage.COMMENT__TIMESTAMP:
				return TIMESTAMP_EDEFAULT == null ? timestamp != null : !TIMESTAMP_EDEFAULT.equals(timestamp);
			case OrchestrationPackage.COMMENT__RESOLVED:
				return resolved != RESOLVED_EDEFAULT;
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
		result.append(" (id: ");
		result.append(id);
		result.append(", filePath: ");
		result.append(filePath);
		result.append(", startLine: ");
		result.append(startLine);
		result.append(", endLine: ");
		result.append(endLine);
		result.append(", author: ");
		result.append(author);
		result.append(", content: ");
		result.append(content);
		result.append(", timestamp: ");
		result.append(timestamp);
		result.append(", resolved: ");
		result.append(resolved);
		result.append(')');
		return result.toString();
	}
}
