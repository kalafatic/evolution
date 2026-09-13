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
import eu.kalafatic.evolution.view.editors.pages.PeerReviewPage;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;

public class DiffViewerGroup extends AEvoGroup {
    private Browser browser;
    private File currentFile;
    private String relativeFilePath;

    public DiffViewerGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Diff Viewer", 1, true);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        try {
            browser = new Browser(group, SWT.NONE);
            browser.setLayoutData(new GridData(GridData.FILL_BOTH));
            browser.setText("<html><body><h3>Select a file to view diff</h3></body></html>");

            new org.eclipse.swt.browser.BrowserFunction(browser, "addLineComment") {
                @Override
                public Object function(Object[] arguments) {
                    if (arguments.length > 0 && arguments[0] instanceof Double) {
                        int lineNum = ((Double) arguments[0]).intValue();
                        handleLineClick(lineNum);
                    }
                    return null;
                }
            };
        } catch (Exception e) {
            toolkit.createLabel(group, "Browser not supported: " + e.getMessage());
        }
    }

    public void setFile(File file) {
        this.currentFile = file;
        updateUI();
    }

    public void setFilePath(String path) {
        // Strip status prefix if present
        if (path != null && path.length() > 2 && (path.startsWith("M ") || path.startsWith("A ") || path.startsWith("D "))) {
            path = path.substring(2);
        }

        IProject project = null;
        if (editor.getEditorInput() instanceof IFileEditorInput) {
            project = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject();
        }
        if (project != null) {
            this.currentFile = project.getFile(path).getLocation().toFile();
            this.relativeFilePath = path;
            updateUI();
        }
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
                        String diff = PeerReviewService.getInstance().getFileDiff(projectRoot, "HEAD", relativeFilePath);
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
        html.append(eu.kalafatic.evolution.controller.ui.EvoStyleManager.getInstance().getEvoStyleCss());
        html.append("body { font-family: var(--evo-font-mono); font-size: 11px; margin: 0; padding: 10px; background: var(--evo-surface); }");
        html.append(".line { display: block; padding: 0 5px; min-height: 1.2em; border-bottom: 1px solid var(--evo-border); }");
        html.append(".added { background-color: var(--evo-success-light); color: var(--evo-success); }");
        html.append(".deleted { background-color: var(--evo-danger-light); color: var(--evo-danger); }");
        html.append(".header { background-color: var(--evo-primary-light); color: var(--evo-primary); font-weight: bold; border-top: 1px solid var(--evo-border); border-bottom: 1px solid var(--evo-border); }");
        html.append(".info { color: var(--evo-text-secondary); }");
        html.append(".line:hover { background-color: var(--evo-surface-hover); cursor: pointer; }");
        html.append("</style></head><body>");

        if (diff == null || diff.isEmpty()) {
            html.append("<div class='info'>No changes detected in this file.</div>");
        } else {
            int lineNum = 0;
            for (String line : diff.split("\n")) {
                lineNum++;
                String cls = "line";
                if (line.startsWith("+++") || line.startsWith("---") || line.startsWith("diff ") || line.startsWith("index ")) cls += " header";
                else if (line.startsWith("+")) cls += " added";
                else if (line.startsWith("-")) cls += " deleted";
                else if (line.startsWith("@@")) cls += " info";

                html.append("<div class='").append(cls).append("' onclick='addLineComment(").append(lineNum).append(")'>").append(escapeHtml(line)).append("</div>");
            }
        }

        html.append("</body></html>");
        return html.toString();
    }

    private void handleLineClick(int lineNum) {
        Display.getDefault().asyncExec(() -> {
            if (editor.getActivePageInstance() instanceof PeerReviewPage) {
                ((PeerReviewPage) editor.getActivePageInstance()).notifyLineSelected(relativeFilePath != null ? relativeFilePath : "unknown", lineNum);
            }
        });
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
