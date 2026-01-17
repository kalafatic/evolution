package eu.kalafatic.evolution.view;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.jface.viewers.LabelProvider;

public class EcoreLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof EClass) {
            return ((EClass) element).getName();
        }
        return super.getText(element);
    }
}
