package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
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
	}

	public void setInput(Object left, Object right) {
		CompareConfiguration config = new CompareConfiguration();
		config.setLeftEditable(false);
		config.setRightEditable(false);
		config.setLeftLabel("Local File");
		config.setRightLabel("Git HEAD");

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
