package eu.kalafatic.evolution.view.editors.pages.peerreview;

import java.io.File;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.review.service.PeerReviewService;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class DiffViewerGroup extends AEvoGroup {
    private Browser browser;
    private File currentFile;

    public DiffViewerGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Diff Viewer", 1, true);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        try {
            browser = new Browser(group, SWT.NONE);
            browser.setLayoutData(new GridData(GridData.FILL_BOTH));
            browser.setText("<html><body><h3>Select a file to view diff</h3></body></html>");
        } catch (Exception e) {
            toolkit.createLabel(group, "Browser not supported: " + e.getMessage());
        }
    }

    public void setFile(File file) {
        this.currentFile = file;
        updateUI();
    }

    @Override
    public void refreshUI() {
        if (browser == null || browser.isDisposed() || currentFile == null) return;

        Job job = new Job("Loading Diff") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    IProject project = null;
                    if (editor.getEditorInput() instanceof IFileEditorInput) {
                        project = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject();
                    }
                    if (project != null) {
                        File projectRoot = project.getLocation().toFile();
                        String diff = PeerReviewService.getInstance().getDiff(projectRoot, "HEAD");
                        final String html = getDiffHtml(diff);
                        Display.getDefault().asyncExec(() -> {
                            if (!browser.isDisposed()) browser.setText(html);
                        });
                    }
                } catch (Exception e) {
                    Display.getDefault().asyncExec(() -> {
                        if (!browser.isDisposed()) browser.setText("<html><body><h3>Error: " + e.getMessage() + "</h3></body></html>");
                    });
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    private String getDiffHtml(String diff) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><style>");
        html.append("body { font-family: 'Consolas', 'Monaco', 'Courier New', monospace; font-size: 12px; margin: 0; padding: 10px; background: #fff; }");
        html.append(".line { display: block; padding: 0 5px; min-height: 1.2em; border-bottom: 1px solid #f0f0f0; }");
        html.append(".added { background-color: #e6ffed; color: #22863a; }");
        html.append(".deleted { background-color: #ffeef0; color: #cb2431; }");
        html.append(".header { background-color: #f1f8ff; color: #005cc5; font-weight: bold; border-top: 1px solid #c0d3eb; border-bottom: 1px solid #c0d3eb; }");
        html.append(".info { color: #6a737d; }");
        html.append("</style></head><body>");

        if (diff == null || diff.isEmpty()) {
            html.append("<div class='info'>No changes detected in this file.</div>");
        } else {
            for (String line : diff.split("\n")) {
                String cls = "line";
                if (line.startsWith("+++") || line.startsWith("---") || line.startsWith("diff ") || line.startsWith("index ")) cls += " header";
                else if (line.startsWith("+")) cls += " added";
                else if (line.startsWith("-")) cls += " deleted";
                else if (line.startsWith("@@")) cls += " info";

                html.append("<div class='").append(cls).append("'>").append(escapeHtml(line)).append("</div>");
            }
        }

        html.append("</body></html>");
        return html.toString();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
