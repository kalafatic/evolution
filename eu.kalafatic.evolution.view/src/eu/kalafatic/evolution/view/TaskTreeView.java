package eu.kalafatic.evolution.view;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import eu.kalafatic.evolution.model.orchestration.Task;


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
    }

    private Task[] createSampleTasks() {
        Task task1 = new Task();
        task1.setName("Task 1");
        task1.setStatus(TaskStatus.PENDING);

        Task task2 = new Task();
        task2.setName("Task 2");
        task2.setStatus(TaskStatus.RUNNING);

        Task task3 = new Task();
        task3.setName("Task 3");
        task3.setStatus(TaskStatus.DONE);

        Task subTask1 = new Task();
        subTask1.setName("Sub-task 1.1");
        subTask1.setStatus(TaskStatus.PENDING);

        Task subTask2 = new Task();
        subTask2.setName("Sub-task 1.2");
        subTask2.setStatus(TaskStatus.PENDING);

        List<Task> subTasks = new ArrayList<Task>();
        subTasks.add(subTask1);
        subTasks.add(subTask2);
        task1.setSubTasks(subTasks);

        return new Task[] { task1, task2, task3 };
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }
}
