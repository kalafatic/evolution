package eu.kalafatic.utils.providers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import eu.kalafatic.utils.constants.EProject;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ProjectContentProvider implements ITreeContentProvider {

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	@Override
	public Object[] getChildren(Object arg0) {
		return getElements(arg0);
	}

	// ---------------------------------------------------------------

	@Override
	public Object getParent(Object arg0) {
		return null;
	}

	// ---------------------------------------------------------------

	@Override
	public boolean hasChildren(Object element) {
		try {
			if (element instanceof IWorkspaceRoot) {
				return ((IWorkspaceRoot) element).members().length > 0;
			} else if (element instanceof IProject) {
				return ((IProject) element).members().length > 0;
			} else if (element instanceof IFolder) {
				return ((IFolder) element).members().length > 0;

			} else if (element instanceof Map) {
				return !((Map) element).isEmpty();
			} else if (element instanceof List) {
				return !((List) element).isEmpty();
			} else if (element instanceof Entry) {
				return hasChildren(((Entry) element).getValue());
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return false;
	}

	// ---------------------------------------------------------------

	@Override
	public Object[] getElements(Object element) {
		try {
			if (element instanceof IWorkspaceRoot) {
				return ((IWorkspaceRoot) element).members();
			} else if (element instanceof IProject) {
				return ((IProject) element).members();
			} else if (element instanceof IFolder) {
				return ((IFolder) element).members();

			} else if (element instanceof Map) {
				Map<String, Object> map = (Map<String, Object>) element;
				Map<String, Object> filteredMap = new LinkedHashMap<String, Object>(map);
				filteredMap.remove(EProject.KEY.name());

				if (filteredMap.containsKey(EProject.CHILDREN.name())) {

					if (((List) filteredMap.get(EProject.CHILDREN.name())).isEmpty()) {
						filteredMap.remove(EProject.CHILDREN.name());
					}
				}
				return filteredMap.entrySet().toArray();

			} else if (element instanceof List) {
				return ((List) element).toArray();
			} else if (element instanceof Entry) {
				return getElements(((Entry) element).getValue());

			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return new Object[] { element };
	}

	// ---------------------------------------------------------------

	@Override
	public void dispose() {}

	// ---------------------------------------------------------------

	@Override
	public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		// Nothing to change
	}
}
