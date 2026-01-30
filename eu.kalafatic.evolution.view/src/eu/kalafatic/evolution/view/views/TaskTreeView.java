package eu.kalafatic.evolution.view.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;


public class TaskTreeView extends ViewPart implements ISelectionListener {

    public static final String ID = "eu.kalafatic.evolution.view.taskTreeView";
    private TreeViewer viewer;

    public TaskTreeView() {
    }

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TreeViewer(parent);
        viewer.setContentProvider(new TaskContentProvider());
        viewer.setLabelProvider(new TaskLabelProvider());
        
        getSite().getPage().addSelectionListener(this);

        IViewSite iViewSite = getViewSite();
        iViewSite.setSelectionProvider(viewer);

        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object first = ((IStructuredSelection) selection).getFirstElement();
            if (first instanceof Orchestrator) {
                Orchestrator orch = (Orchestrator) first;
                viewer.setInput(orch.getTasks().toArray());
            } else if (first instanceof Task) {
                // Optionally handle task selection
            }
        }
    }

    @Override
    public void dispose() {
        getSite().getPage().removeSelectionListener(this);
        super.dispose();
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    public void refresh() {
        if (viewer != null && !viewer.getControl().isDisposed()) {
            viewer.refresh();
        }
    }
}
