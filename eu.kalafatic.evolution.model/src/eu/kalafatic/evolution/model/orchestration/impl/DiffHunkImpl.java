package eu.kalafatic.evolution.model.orchestration.impl;

import eu.kalafatic.evolution.model.orchestration.DiffHunk;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import java.util.Collection;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;

public class DiffHunkImpl extends MinimalEObjectImpl.Container implements DiffHunk {
    protected String header;
    protected EList<String> lines;

    protected DiffHunkImpl() { super(); }

    @Override protected EClass eStaticClass() { return OrchestrationPackage.Literals.DIFF_HUNK; }

    @Override public String getHeader() { return header; }
    @Override public void setHeader(String newHeader) {
        String oldHeader = header; header = newHeader;
        if (eNotificationRequired()) eNotify(new ENotificationImpl(this, Notification.SET, OrchestrationPackage.DIFF_HUNK__HEADER, oldHeader, header));
    }

    @Override public EList<String> getLines() {
        if (lines == null) {
            lines = new EDataTypeUniqueEList<String>(String.class, this, OrchestrationPackage.DIFF_HUNK__LINES);
        }
        return lines;
    }

    @Override
    public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case OrchestrationPackage.DIFF_HUNK__HEADER: return getHeader();
            case OrchestrationPackage.DIFF_HUNK__LINES: return getLines();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case OrchestrationPackage.DIFF_HUNK__HEADER: setHeader((String)newValue); return;
            case OrchestrationPackage.DIFF_HUNK__LINES: getLines().clear(); getLines().addAll((Collection<? extends String>)newValue); return;
        }
        super.eSet(featureID, newValue);
    }
}
