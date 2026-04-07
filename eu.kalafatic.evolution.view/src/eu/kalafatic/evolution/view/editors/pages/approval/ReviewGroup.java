package eu.kalafatic.evolution.view.editors.pages.approval;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.ide.IDE;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.ShellTool;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class ReviewGroup {
    private Composite group;
    private Browser browser;
    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private String lastHtml = "";

    public ReviewGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        this.editor = editor;
        this.orchestrator = orchestrator;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Review Changes", 1, true);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        try {
            browser = new Browser(group, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 400;
            browser.setLayoutData(gd);

            new BrowserFunction(browser, "openFileInEclipse") {
                @Override
                public Object function(Object[] arguments) {
                    if (arguments.length > 0 && arguments[0] instanceof String) {
                        openFile((String) arguments[0]);
                    }
                    return null;
                }
            };

            new BrowserFunction(browser, "copyToClipboard") {
                @Override
                public Object function(Object[] arguments) {
                    if (arguments.length > 0 && arguments[0] instanceof String) {
                        org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(Display.getDefault());
                        org.eclipse.swt.dnd.TextTransfer textTransfer = org.eclipse.swt.dnd.TextTransfer.getInstance();
                        cb.setContents(new Object[]{arguments[0]}, new org.eclipse.swt.dnd.Transfer[]{textTransfer});
                        cb.dispose();
                    }
                    return null;
                }
            };

        } catch (Exception e) {
            toolkit.createLabel(group, "Browser not supported: " + e.getMessage());
        }

        Button refreshBtn = toolkit.createButton(group, "Refresh Review", SWT.PUSH);
        refreshBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                updateDiff();
            }
        });
    }

    private void openFile(String path) {
        Display.getDefault().asyncExec(() -> {
            try {
                IProject project = null;
                if (editor.getEditorInput() instanceof IFileEditorInput) {
                    project = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject();
                }
                if (project != null) {
                    IFile file = project.getFile(new Path(path));
                    if (file.exists()) {
                        IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void updateUI(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        updateDiff();
    }

    private void updateDiff() {
        if (browser == null || browser.isDisposed()) return;

        CompletableFuture.runAsync(() -> {
            try {
                String diffResult = null;
                IProject project = null;
                if (editor.getEditorInput() instanceof IFileEditorInput) {
                    project = ((IFileEditorInput) editor.getEditorInput()).getFile().getProject();
                }

                if (project != null) {
                    File projectRoot = project.getLocation().toFile();
                    ShellTool shell = new ShellTool();
                    TaskContext context = editor.getCurrentContext();
                    if (context == null) {
                        context = new TaskContext(orchestrator, projectRoot);
                    }
                    try {
                        diffResult = shell.execute("git diff HEAD", projectRoot, context);
                    } catch (Exception e) {
                        // Git failed, fallback to internal
                    }
                }

                final String finalDiff = diffResult;
                final String internalDiffJson = generateInternalDiffJson();

                Display.getDefault().asyncExec(() -> {
                    if (!browser.isDisposed()) {
                        String html = getDiffHtml(finalDiff, internalDiffJson);
                        if (!html.equals(lastHtml)) {
                            try {
                                browser.setText(html);
                                lastHtml = html;
                            } catch (Exception e2) {
                                // Ignore browser busy/initializing
                            }
                        }
                    }
                });
            } catch (Exception e) {
                Display.getDefault().asyncExec(() -> {
                    if (!browser.isDisposed()) {
                        browser.setText("<html><body><h3>Error retrieving diff</h3><pre>" + e.getMessage() + "</pre></body></html>");
                    }
                });
            }
        });
    }

    private String generateInternalDiffJson() {
        JSONArray files = new JSONArray();
        if (orchestrator != null) {
            for (Task task : orchestrator.getTasks()) {
                if ("file".equalsIgnoreCase(task.getType()) && task.getResultSummary() != null) {
                    JSONObject file = new JSONObject();
                    file.put("path", task.getResultSummary());
                    file.put("before", task.getRationale() != null ? task.getRationale() : "");
                    String after = "";
                    String response = task.getResponse();
                    if (response != null) {
                        int index = response.indexOf("CONTENT:\n");
                        if (index != -1) {
                            after = response.substring(index + 9);
                        } else if (response.startsWith("SUCCESS: Wrote file")) {
                            // If we don't have CONTENT: tag but it was a success, we might have lost the content in the response string
                            // depending on how it was returned. In EvolutionOrchestrator it appends \nCONTENT:\ncontent
                        }
                    }
                    file.put("after", after);
                    file.put("status", task.getStatus().toString());
                    files.put(file);
                }
            }
        }
        return files.toString();
    }

    private String getDiffHtml(String gitDiff, String internalJson) {
        return "<!DOCTYPE html><html><head><style>"
            + "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 10px; background-color: #f9f9f9; color: #333; }"
            + ".file-container { border: 1px solid #ddd; border-radius: 4px; margin-bottom: 15px; background: #fff; overflow: hidden; }"
            + ".file-header { background: #f0f0f0; padding: 8px 12px; border-bottom: 1px solid #ddd; display: flex; justify-content: space-between; align-items: center; cursor: pointer; }"
            + ".file-path { font-weight: bold; color: #0056b3; text-decoration: underline; }"
            + ".diff-content { padding: 0; font-family: 'Consolas', 'Monaco', 'Courier New', monospace; font-size: 12px; white-space: pre; overflow-x: auto; display: block; }"
            + ".line { display: block; padding: 0 10px; min-height: 1.2em; }"
            + ".added { background-color: #e6ffed; color: #22863a; }"
            + ".deleted { background-color: #ffeef0; color: #cb2431; }"
            + ".info { background-color: #f1f8ff; color: #005cc5; }"
            + ".btn { padding: 3px 8px; font-size: 11px; cursor: pointer; border: 1px solid #ccc; border-radius: 3px; background: #fff; margin-left: 5px; }"
            + ".btn:hover { background: #e9e9e9; }"
            + "h3 { border-bottom: 2px solid #0056b3; padding-bottom: 5px; color: #0056b3; }"
            + ".fallback-tag { font-size: 10px; background: #ffeb3b; padding: 2px 5px; border-radius: 3px; margin-left: 10px; }"
            + "</style></head><body>"
            + "<h3>Review Changes</h3>"
            + (gitDiff != null && !gitDiff.trim().isEmpty() ? "<div><b>Source:</b> Git Diff</div><pre class='diff-content'>" + escapeHtml(gitDiff) + "</pre>" :
              "<div><b>Source:</b> Internal Tracker <span class='fallback-tag'>Git Unavailable</span></div><div id='internal-diff'></div>")
            + "<script>"
            + "const internalData = " + internalJson + ";"
            + "function init() {"
            + "  const container = document.getElementById('internal-diff');"
            + "  if (!container) return;"
            + "  if (internalData.length === 0) { container.innerHTML = '<p>No file changes tracked.</p>'; return; }"
            + "  internalData.forEach((file, index) => {"
            + "    const fileDiv = document.createElement('div');"
            + "    fileDiv.className = 'file-container';"
            + "    fileDiv.innerHTML = `<div class='file-header' onclick='toggleDiff(${index})'>"
            + "      <span><span class='file-path' onclick='event.stopPropagation(); openFileInEclipse(\"${file.path}\")'>${file.path}</span> [${file.status}]</span>"
            + "      <div>"
            + "        <button class='btn' onclick='event.stopPropagation(); copyFile(${index})'>Copy New</button>"
            + "        <button class='btn'>Toggle</button>"
            + "      </div>"
            + "    </div>"
            + "    <div id='diff-${index}' class='diff-content'>${generateDiff(file.before, file.after)}</div>`;"
            + "    container.appendChild(fileDiv);"
            + "  });"
            + "}"
            + "function toggleDiff(index) {"
            + "  const el = document.getElementById('diff-' + index);"
            + "  el.style.display = el.style.display === 'none' ? 'block' : 'none';"
            + "}"
            + "function copyFile(index) {"
            + "  copyToClipboard(internalData[index].after);"
            + "}"
            + "function generateDiff(before, after) {"
            + "  if (!before && after) return `<span class='line added'>+ (New File)</span>` + after.split('\\n').map(l => `<span class='line added'>+ ${escapeHtml(l)}</span>`).join('');"
            + "  if (before && !after) return `<span class='line deleted'>- (Deleted)</span>` + before.split('\\n').map(l => `<span class='line deleted'>- ${escapeHtml(l)}</span>`).join('');"
            + "  if (before === after) return `<span class='line'>(No changes)</span>`;"
            + "  // Simple line-based diff"
            + "  const bLines = before.split('\\n');"
            + "  const aLines = after.split('\\n');"
            + "  let res = '';"
            + "  let i = 0, j = 0;"
            + "  while (i < bLines.length || j < aLines.length) {"
            + "    if (i < bLines.length && j < aLines.length && bLines[i] === aLines[j]) {"
            + "      res += `<span class='line'>  ${escapeHtml(bLines[i])}</span>`; i++; j++;"
            + "    } else if (i < bLines.length && (j >= aLines.length || !aLines.includes(bLines[i], j))) {"
            + "      res += `<span class='line deleted'>- ${escapeHtml(bLines[i])}</span>`; i++;"
            + "    } else if (j < aLines.length) {"
            + "      res += `<span class='line added'>+ ${escapeHtml(aLines[j])}</span>`; j++;"
            + "    }"
            + "  }"
            + "  return res;"
            + "}"
            + "function escapeHtml(text) {"
            + "  const div = document.createElement('div');"
            + "  div.textContent = text;"
            + "  return div.innerHTML;"
            + "}"
            + "init();"
            + "</script></body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#039;");
    }
}
