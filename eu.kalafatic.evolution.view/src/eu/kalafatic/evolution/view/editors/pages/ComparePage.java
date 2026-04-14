package eu.kalafatic.evolution.view.editors.pages;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.compare.ResourceCompareInput;

public class ComparePage extends Composite {

	private MultiPageEditor editor;
	private Orchestrator orchestrator;
	private ResourceCompareInput input;
	private Control compareControl;

	public ComparePage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, SWT.NONE);
		this.editor = editor;
		this.orchestrator = orchestrator;
		setLayout(new FillLayout());
		initDragAndDrop();
	}

	private void initDragAndDrop() {
		DropTarget target = new DropTarget(this, DND.DROP_COPY | DND.DROP_MOVE | DND.DROP_LINK);
		target.setTransfer(new Transfer[] { LocalSelectionTransfer.getTransfer(), FileTransfer.getInstance() });
		target.addDropListener(new DropTargetAdapter() {
			@Override
			public void dragEnter(DropTargetEvent event) {
				event.detail = DND.DROP_COPY;
			}

			@Override
			public void drop(DropTargetEvent event) {
				List<IFile> files = new ArrayList<>();
				if (LocalSelectionTransfer.getTransfer().isSupportedType(event.currentDataType)) {
					if (event.data instanceof IStructuredSelection) {
						IStructuredSelection sel = (IStructuredSelection) event.data;
						for (Object obj : sel.toList()) {
							if (obj instanceof IFile) {
								files.add((IFile) obj);
							}
						}
					}
				} else if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
					String[] fileNames = (String[]) event.data;
					if (fileNames != null) {
						for (String fileName : fileNames) {
							IPath path = new Path(fileName);
							IFile[] workspaceFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(path);
							if (workspaceFiles.length > 0) {
								files.add(workspaceFiles[0]);
							}
						}
					}
				}

				if (files.size() == 2) {
					IFile f1 = files.get(0);
					IFile f2 = files.get(1);
					setInput(f1, f2, f1.getName(), f2.getName());
				}
			}
		});
	}

	public void setInput(Object left, Object right, String leftLabel, String rightLabel) {
		CompareConfiguration config = new CompareConfiguration();
		config.setLeftEditable(false);
		config.setRightEditable(false);
		config.setLeftLabel(leftLabel);
		config.setRightLabel(rightLabel);

		input = new ResourceCompareInput(config, left, right);

		Job job = new Job("Preparing comparison") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					input.run(monitor);
					Display.getDefault().asyncExec(() -> {
						if (!isDisposed()) {
							if (compareControl != null) {
								compareControl.dispose();
							}
							compareControl = input.createContents(ComparePage.this);
							layout(true);
						}
					});
				} catch (Exception e) {
					return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", "Error preparing compare", e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}
}
