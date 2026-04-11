package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.Comment;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

public class CommentImpl extends MinimalEObjectImpl.Container implements Comment {
    protected String id;
    protected String filePath;
    protected int startLine;
    protected int endLine;
    protected String author;
    protected String content;
    protected String timestamp;
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
}
