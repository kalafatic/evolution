package eu.kalafatic.evolution.view.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.part.ViewPart;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.TaskStatus;
import eu.kalafatic.evolution.model.orchestration.impl.TaskImpl;


public class TaskTreeView extends ViewPart {

    public static final String ID = "eu.kalafatic.evolution.view.taskTreeView";
    private TreeViewer viewer;

    public TaskTreeView() {
    }

    @Override
    public void createPartControl(Composite parent) {
        viewer = new TreeViewer(parent);
        viewer.setContentProvider(new TaskContentProvider());
        viewer.setLabelProvider(new TaskLabelProvider());
        viewer.setInput(createSampleTasks());
        
     // HERE you can use getViewSite()
        IViewSite iViewSite = getViewSite();
        iViewSite.setSelectionProvider(viewer);

        MenuManager menuMgr = new MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        Menu menu = menuMgr.createContextMenu(viewer.getControl());
        viewer.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, viewer);
    }

    private Task[] createSampleTasks() {
        Task task1 = OrchestrationFactory.eINSTANCE.createTask();
        task1.setName("Task 1");
        task1.setStatus(TaskStatus.PENDING);

        Task task2 = OrchestrationFactory.eINSTANCE.createTask();
        task2.setName("Task 2");
        task2.setStatus(TaskStatus.RUNNING);

        Task task3 = OrchestrationFactory.eINSTANCE.createTask();
        task3.setName("Task 3");
        task3.setStatus(TaskStatus.DONE);

        Task subTask1 = OrchestrationFactory.eINSTANCE.createTask();
        subTask1.setName("Sub-task 1.1");
        subTask1.setStatus(TaskStatus.PENDING);

        Task subTask2 = OrchestrationFactory.eINSTANCE.createTask();
        subTask2.setName("Sub-task 1.2");
        subTask2.setStatus(TaskStatus.PENDING);

        task1.getSubTasks().add(subTask1);
        task1.getSubTasks().add(subTask2);

        return new Task[] { task1, task2, task3 };
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }
}
