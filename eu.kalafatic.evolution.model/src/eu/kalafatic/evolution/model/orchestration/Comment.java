package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.ecore.EObject;

public interface Comment extends EObject {
    String getId();
    void setId(String value);
    String getFilePath();
    void setFilePath(String value);
    int getStartLine();
    void setStartLine(int value);
    int getEndLine();
    void setEndLine(int value);
    String getAuthor();
    void setAuthor(String value);
    String getContent();
    void setContent(String value);
    String getTimestamp();
    void setTimestamp(String value);
    boolean isResolved();
    void setResolved(boolean value);
}
