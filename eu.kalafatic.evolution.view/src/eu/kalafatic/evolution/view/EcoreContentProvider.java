package eu.kalafatic.evolution.view;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;

public class EcoreContentProvider extends ArrayContentProvider implements IGraphEntityContentProvider {

    @Override
    public Object[] getConnectedTo(Object entity) {
        if (entity instanceof EClass) {
            EClass eClass = (EClass) entity;
            return eClass.getEReferences().stream().map(EReference::getEReferenceType).toArray();
        }
        return null;
    }
}
