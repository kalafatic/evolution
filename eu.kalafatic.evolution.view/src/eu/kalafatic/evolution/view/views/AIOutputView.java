package eu.kalafatic.evolution.view.views;

import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class AIOutputView extends ViewPart {

    public static final String ID = "eu.kalafatic.evolution.view.aiOutputView";
    private TextViewer viewer;

    public AIOutputView() {
    }

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TextViewer(parent, SWT.V_SCROLL | SWT.H_SCROLL);
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }
}
