package eu.kalafatic.evolution.view.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreEMap;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class EvoNavigatorContentProvider implements ITreeContentProvider {

	IProject[] projects;

	public EvoNavigatorContentProvider(IProject[] projects) {
		this.projects = projects;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface
	 * .viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		System.err.println("inputChanged");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.
	 * Object)
	 */

	@Override
	public Object[] getChildren(Object parentElement) {
		try {
			if (parentElement instanceof IProject[]) {
				IProject[] el = (IProject[]) parentElement;
				return el;

			} else if (parentElement instanceof IProject) {
				IProject el = (IProject) parentElement;
				return el.members();
			} else if (parentElement instanceof Project) {
				Project el = (Project) parentElement;
				return el.members();
			} else if (parentElement instanceof IFolder) {
				IFolder el = (IFolder) parentElement;
				return el.members();
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new Object[0];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object )
	 */
	@Override
	public Object getParent(Object element) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.
	 * Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		Object[] obj = getChildren(element);
		// Return whether the parent has children
		return obj == null ? false : obj.length > 0;
	}

}
