package eu.kalafatic.evolution.model.orchestration;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;

public interface DiffHunk extends EObject {
    String getHeader();
    void setHeader(String value);
    EList<String> getLines();
}
