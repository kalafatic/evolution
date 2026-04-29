package eu.kalafatic.evolution.view.provider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;

import eu.kalafatic.evolution.controller.orchestration.FileChangeTracker;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.view.application.Activator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class EvoLabelDecorator extends LabelProvider implements ILightweightLabelDecorator {

	private UIJob fNotifierJob;

	private Set<Object> fChangedResources = new HashSet<Object>();

	private ImageDescriptor addIcon, editIcon, removeIcon;

	public EvoLabelDecorator() {
		addIcon = Activator.getImageDescriptor("eu.kalafatic.utils", "icons/ovr16/constr_ovr.gif");
		editIcon = Activator.getImageDescriptor("eu.kalafatic.utils", "icons/ovr16/write.gif");
		removeIcon = Activator.getImageDescriptor("eu.kalafatic.utils", "icons/ovr16/error_co.gif");
	}

	public void update(Object... collection) {
		boolean hasChanges = false;
		synchronized (fChangedResources) {
			hasChanges = fChangedResources.addAll(Arrays.asList(collection));
		}
		if (hasChanges) {
			if (fNotifierJob == null) {
				fNotifierJob = new UIJob(Display.getDefault(), "Update Java test decorations") {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						runPendingUpdates();
						return Status.OK_STATUS;
					}
				};
				fNotifierJob.setSystem(true);
			}
			fNotifierJob.schedule();
		}
	}

	private void runPendingUpdates() {
		Object[] resourceToUpdate = null;
		synchronized (fChangedResources) {
			resourceToUpdate = fChangedResources.toArray();
			fChangedResources.clear();
		}
		if (resourceToUpdate.length > 0) {
			LabelProviderChangedEvent event = new LabelProviderChangedEvent(this, resourceToUpdate);
			fireLabelProviderChanged(event);
		}
	}

	@Override
	public void addListener(ILabelProviderListener listener) {
		super.addListener(listener);
	}

	@Override
	public void decorate(Object element, IDecoration decoration) {
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			String path = resource.getProjectRelativePath().toString();

			TaskContext context = getCurrentContext();
			if (context != null && context.getFileChangeTracker() != null) {
				FileChangeTracker.ChangeType type = context.getFileChangeTracker().getChangeType(path);
				if (type != null) {
					switch (type) {
					case NEW:
						decoration.addOverlay(addIcon, IDecoration.BOTTOM_RIGHT);
						decoration.addPrefix("[NEW] ");
						break;
					case EDITED:
						decoration.addOverlay(editIcon, IDecoration.BOTTOM_RIGHT);
						decoration.addPrefix("[EDITED] ");
						break;
					case REMOVED:
						decoration.addOverlay(removeIcon, IDecoration.BOTTOM_RIGHT);
						decoration.addPrefix("[REMOVED] ");
						break;
					}
				}
			}
		}
	}

	private TaskContext getCurrentContext() {
		IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (activeEditor instanceof MultiPageEditor) {
			return ((MultiPageEditor) activeEditor).getCurrentContext();
		}
		return null;
	}

	@Override
	public void dispose() {
		super.dispose();
	}

	@Override
	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	@Override
	public void removeListener(ILabelProviderListener listener) {
		super.removeListener(listener);
	}
}
