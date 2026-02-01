package eu.kalafatic.evolution.view.provider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
import org.eclipse.ui.progress.UIJob;

import eu.kalafatic.evolution.view.application.Activator;

public class EvoLabelDecorator extends LabelProvider implements ILightweightLabelDecorator {

	private UIJob fNotifierJob;

	private Set<Object> fChangedResources = new HashSet<Object>();

	ImageDescriptor imageDescriptor;

	public EvoLabelDecorator() {
		imageDescriptor = Activator.getImageDescriptor("icons/ovr16/error_co.gif");
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
		if (element instanceof EObject) {
			EObject el = (EObject) element;

//			if (device.getHost().equals("localhost")) {
				decoration.addOverlay(imageDescriptor);
//			}
		}
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
