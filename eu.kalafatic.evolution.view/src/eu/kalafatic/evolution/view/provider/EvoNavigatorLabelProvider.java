package eu.kalafatic.evolution.view.provider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class EvoNavigatorLabelProvider extends LabelProvider {

	private IProject[] projects;

	public EvoNavigatorLabelProvider(IProject[] projects) {
		this.projects = projects;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
	}

	@Override
	public Image getImage(Object element) {
		if (!PlatformUI.isWorkbenchRunning()) {
			return null;
		}
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		if (sharedImages == null) {
			return null;
		}

		if (element instanceof IFolder) {
			return sharedImages.getImage(ISharedImages.IMG_OBJ_FOLDER);
		} else if (element instanceof IFile) {
			return sharedImages.getImage(ISharedImages.IMG_OBJ_FILE);
		}
		return sharedImages.getImage(ISharedImages.IMG_OBJ_FILE);
	}
}
