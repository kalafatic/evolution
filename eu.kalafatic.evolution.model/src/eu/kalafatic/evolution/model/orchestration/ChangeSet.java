package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

public interface ChangeSet extends EObject {
    String getCommitId();
    void setCommitId(String value);
    EList<FileChange> getFiles();
}
