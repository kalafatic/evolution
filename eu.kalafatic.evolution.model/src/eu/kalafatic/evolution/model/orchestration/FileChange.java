package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

public interface FileChange extends EObject {
    String getFilePath();
    void setFilePath(String value);
    String getStatus();
    void setStatus(String value);
    EList<DiffHunk> getHunks();
}
