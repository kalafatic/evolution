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
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Diff Viewer", 1, true, true);
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
        if (path == null) return;
        // Strip status prefix if present
        if (path.length() > 2 && (path.startsWith("M ") || path.startsWith("A ") || path.startsWith("D ") || path.startsWith("? "))) {
            path = path.substring(2).trim();
        }

        File projectRoot = null;
        if (editor.getActivePageInstance() instanceof PeerReviewPage) {
            projectRoot = ((PeerReviewPage) editor.getActivePageInstance()).getProjectRoot();
        }
        if (projectRoot != null) {
            this.relativeFilePath = path;
            this.currentFile = new File(projectRoot, path);
            updateUI();
        }
    }

    @Override
    public void refreshUI() {
        if (browser == null || browser.isDisposed() || relativeFilePath == null) return;

        Job job = new Job("Loading Diff") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    File projectRoot = null;
                    if (editor.getActivePageInstance() instanceof PeerReviewPage) {
                        projectRoot = ((PeerReviewPage) editor.getActivePageInstance()).getProjectRoot();
                    }
                    if (projectRoot != null) {
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
        html.append("body { font-family: var(--evo-font-mono); font-size: 11px; margin: 0; padding: 10px; background: var(--evo-surface); color: var(--evo-text); }");
        html.append(".diff-table { width: 100%; border-collapse: collapse; font-family: var(--evo-font-mono); font-size: 11px; }");
        html.append(".diff-row { display: table-row; min-height: 1.3em; }");
        html.append(".diff-row:hover { background-color: var(--evo-surface-hover); cursor: pointer; }");
        html.append(".line-num { display: table-cell; width: 40px; padding: 0 5px; text-align: right; color: var(--evo-text-secondary); user-select: none; border-right: 1px solid var(--evo-border); font-size: 10px; }");
        html.append(".line-text { display: table-cell; padding: 0 8px; white-space: pre-wrap; word-break: break-all; }");
        html.append(".added { background-color: var(--evo-success-light); color: var(--evo-success); }");
        html.append(".deleted { background-color: var(--evo-danger-light); color: var(--evo-danger); }");
        html.append(".header { background-color: var(--evo-primary-light); color: var(--evo-primary); font-weight: bold; }");
        html.append(".info { color: var(--evo-text-secondary); background-color: var(--evo-surface-alt); }");
        html.append(".empty-msg { padding: 15px; color: var(--evo-text-secondary); font-style: italic; }");
        html.append("</style></head><body>");

        if (diff == null || diff.trim().isEmpty()) {
            html.append("<div class='empty-msg'>No changes detected in this file (or file matches HEAD).</div>");
        } else {
            html.append("<div class='diff-table'>");
            int lineNum = 0;
            for (String line : diff.split("\n")) {
                lineNum++;
                String cls = "line-text";
                if (line.startsWith("+++") || line.startsWith("---") || line.startsWith("diff ") || line.startsWith("index ")) cls += " header";
                else if (line.startsWith("+")) cls += " added";
                else if (line.startsWith("-")) cls += " deleted";
                else if (line.startsWith("@@")) cls += " info";

                html.append("<div class='diff-row' onclick='addLineComment(").append(lineNum).append(")'>")
                    .append("<div class='line-num'>").append(lineNum).append("</div>")
                    .append("<div class='").append(cls).append("'>").append(escapeHtml(line)).append("</div>")
                    .append("</div>");
            }
            html.append("</div>");
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
