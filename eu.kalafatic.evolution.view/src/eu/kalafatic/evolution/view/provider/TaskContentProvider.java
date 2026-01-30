package eu.kalafatic.evolution.view.provider;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import eu.kalafatic.evolution.model.orchestration.Task;




public class TaskContentProvider implements ITreeContentProvider {

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }

    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof Object[]) {
            return (Object[]) inputElement;
        }
        return new Object[0];
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof Task) {
            Task task = (Task) parentElement;
            if (task.getSubTasks() != null) {
                return task.getSubTasks().toArray();
            }
        }
        return new Object[0];
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof Task) {
            Task task = (Task) element;
            return task.getSubTasks() != null && !task.getSubTasks().isEmpty();
        }
        return false;
    }
}
