package eu.kalafatic.evolution.view.provider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import eu.kalafatic.evolution.view.application.Activator;

public class EvoNavigatorLabelProvider extends LabelProvider {
	
	private static ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	// FILE STRUCTURE
	/** The Constant FOLDER_IMG. */
	public static final Image FOLDER_IMG = sharedImages.getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER).createImage();

	/** The Constant FILE_IMG. */
	public static final Image FILE_IMG = sharedImages.getImageDescriptor(ISharedImages.IMG_OBJ_FILE).createImage();

	
	ImageDescriptor imageDescriptor = Activator.getImageDescriptor("eu.kalafatic.utils", "icons/ovr16/error_co.gif");
	
	IProject[] projects;

	public EvoNavigatorLabelProvider(IProject[] projects) {
		this.projects = projects;
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof IFolder) {
			return FOLDER_IMG;
		} else if (element instanceof IFile) {
			return FILE_IMG;
		}
		return imageDescriptor.createImage();
	}

}
