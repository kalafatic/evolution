package eu.kalafatic.evolution.view;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public class CommandStackView extends ViewPart {

    public static final String ID = "eu.kalafatic.evolution.view.commandStackView";
    private TableViewer viewer;

    public CommandStackView() {
    }

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TableViewer(parent);
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }
}
