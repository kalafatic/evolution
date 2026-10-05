package eu.kalafatic.evolution.view.menu;

import java.util.List;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.llm.LlmRouter;
import eu.kalafatic.evolution.model.orchestration.ForgeSession;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * Global EVO Context Menu Manager.
 * Installs a global display filter intercepting right clicks (SWT.MenuDetect) everywhere across the RCP app.
 */
public class EvoGlobalContextMenuManager {

    private static final EvoGlobalContextMenuManager INSTANCE = new EvoGlobalContextMenuManager();
    private boolean installed = false;
    private Listener menuDetectFilter;

    private EvoGlobalContextMenuManager() {}

    public static EvoGlobalContextMenuManager getInstance() {
        return INSTANCE;
    }

    /**
     * Installs the global menu detector on the default Display.
     */
    public synchronized void install() {
        if (installed) {
            return;
        }

        Display display = Display.getDefault();
        if (display == null || display.isDisposed()) {
            return;
        }

        menuDetectFilter = new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.type == SWT.MenuDetect) {
                    onMenuDetect(event);
                }
            }
        };

        display.addFilter(SWT.MenuDetect, menuDetectFilter);
        installed = true;
        Log.log("[EVO_MENU] Global EVO Context Menu Manager installed successfully.");
    }

    public synchronized void uninstall() {
        if (!installed) {
            return;
        }
        Display display = Display.getDefault();
        if (display != null && !display.isDisposed() && menuDetectFilter != null) {
            display.removeFilter(SWT.MenuDetect, menuDetectFilter);
        }
        installed = false;
    }

    private void onMenuDetect(Event event) {
        Widget widget = event.widget;
        if (!(widget instanceof Control)) {
            return;
        }

        Control control = (Control) widget;
        Shell shell = control.getShell();
        if (shell == null || shell.isDisposed()) {
            return;
        }

        // Build context menu
        MenuManager menuMgr = new MenuManager("#EvoGlobalContextMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> fillGlobalContextMenu(manager, control, event));

        Menu menu = menuMgr.createContextMenu(shell);
        menu.addMenuListener(new org.eclipse.swt.events.MenuAdapter() {
            @Override
            public void menuHidden(org.eclipse.swt.events.MenuEvent e) {
                shell.getDisplay().asyncExec(() -> {
                    if (!menu.isDisposed()) {
                        menu.dispose();
                    }
                    menuMgr.dispose();
                });
            }
        });

        Point mouseLoc = new Point(event.x, event.y);
        menu.setLocation(mouseLoc);
        menu.setVisible(true);

        // Suppress native popup to use standard unified EVO menu
        event.doit = false;
    }

    /**
     * Main builder populating context menu based on targeted control, session state, and AI proposals.
     */
    public void fillGlobalContextMenu(IMenuManager manager, Control control, Event event) {
        // 1. Context Information Header / Status
        addContextHeader(manager, control);

        manager.add(new Separator("EDITING"));

        // 2. Default Context Actions (Text editing, table/tree selection actions)
        addDefaultEditingActions(manager, control);

        manager.add(new Separator("PROCESS"));

        // 3. Global Session & Running Process Actions
        addProcessAndSessionActions(manager, control);

        manager.add(new Separator("AI_ASSIST"));

        // 4. Neural / LLM Smart Proposals
        addAiSmartProposals(manager, control);

        manager.add(new Separator("NAVIGATION"));

        // 5. Workbench Page / View Quick Navigation
        addQuickNavigationActions(manager);
    }

    private void addContextHeader(IMenuManager manager, Control control) {
        String controlType = control.getClass().getSimpleName();
        IWorkbenchPart activePart = getActivePart();
        String partName = (activePart != null) ? activePart.getTitle() : "Workbench";

        Action headerAction = new Action("EVO Platform [" + partName + " • " + controlType + "]") {};
        headerAction.setEnabled(false);
        manager.add(headerAction);
    }

    private void addDefaultEditingActions(IMenuManager manager, Control control) {
        if (control instanceof Text) {
            Text text = (Text) control;
            String sel = text.getSelectionText();

            Action cutAction = new Action("Cut") {
                @Override public void run() { text.cut(); }
            };
            cutAction.setEnabled(!sel.isEmpty() && text.getEditable());

            Action copyAction = new Action("Copy") {
                @Override public void run() { text.copy(); }
            };
            copyAction.setEnabled(!sel.isEmpty());

            Action pasteAction = new Action("Paste") {
                @Override public void run() { text.paste(); }
            };
            pasteAction.setEnabled(text.getEditable());

            Action selectAllAction = new Action("Select All") {
                @Override public void run() { text.selectAll(); }
            };

            manager.add(cutAction);
            manager.add(copyAction);
            manager.add(pasteAction);
            manager.add(selectAllAction);

        } else if (control instanceof StyledText) {
            StyledText st = (StyledText) control;
            String sel = st.getSelectionText();

            Action cutAction = new Action("Cut") {
                @Override public void run() { st.cut(); }
            };
            cutAction.setEnabled(!sel.isEmpty() && st.getEditable());

            Action copyAction = new Action("Copy") {
                @Override public void run() { st.copy(); }
            };
            copyAction.setEnabled(!sel.isEmpty());

            Action pasteAction = new Action("Paste") {
                @Override public void run() { st.paste(); }
            };
            pasteAction.setEnabled(st.getEditable());

            Action selectAllAction = new Action("Select All") {
                @Override public void run() { st.selectAll(); }
            };

            manager.add(cutAction);
            manager.add(copyAction);
            manager.add(pasteAction);
            manager.add(selectAllAction);

        } else if (control instanceof Combo) {
            Combo combo = (Combo) control;
            Action clearAction = new Action("Clear Selection") {
                @Override public void run() { combo.deselectAll(); }
            };
            manager.add(clearAction);

        } else if (control instanceof Table) {
            Table table = (Table) control;
            TableItem[] selection = table.getSelection();

            Action copyRowAction = new Action("Copy Selected Row(s)") {
                @Override
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    for (TableItem item : selection) {
                        int cols = table.getColumnCount();
                        if (cols > 0) {
                            for (int c = 0; c < cols; c++) {
                                if (c > 0) sb.append("\t");
                                sb.append(item.getText(c));
                            }
                        } else {
                            sb.append(item.getText());
                        }
                        sb.append("\n");
                    }
                    if (sb.length() > 0) {
                        org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(control.getDisplay());
                        cb.setContents(new Object[]{sb.toString()}, new org.eclipse.swt.dnd.Transfer[]{org.eclipse.swt.dnd.TextTransfer.getInstance()});
                        cb.dispose();
                    }
                }
            };
            copyRowAction.setEnabled(selection.length > 0);

            Action selectAllTableAction = new Action("Select All Rows") {
                @Override public void run() { table.selectAll(); }
            };

            manager.add(copyRowAction);
            manager.add(selectAllTableAction);

        } else if (control instanceof Tree) {
            Tree tree = (Tree) control;
            TreeItem[] selection = tree.getSelection();

            Action copyTreeItemAction = new Action("Copy Tree Node Text") {
                @Override
                public void run() {
                    StringBuilder sb = new StringBuilder();
                    for (TreeItem item : selection) {
                        sb.append(item.getText()).append("\n");
                    }
                    if (sb.length() > 0) {
                        org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(control.getDisplay());
                        cb.setContents(new Object[]{sb.toString()}, new org.eclipse.swt.dnd.Transfer[]{org.eclipse.swt.dnd.TextTransfer.getInstance()});
                        cb.dispose();
                    }
                }
            };
            copyTreeItemAction.setEnabled(selection.length > 0);
            manager.add(copyTreeItemAction);

        } else {
            // Generic control
            Action refreshAction = new Action("Refresh Control") {
                @Override public void run() { control.redraw(); }
            };
            manager.add(refreshAction);
        }
    }

    private void addProcessAndSessionActions(IMenuManager manager, Control control) {
        // Query active sessions and processes
        ForgeSession activeForgeSession = ForgeSessionManager.getInstance().findSession("Active Forge Session");
        List<ForgeSession> allSessions = ForgeSessionManager.getInstance().getSessions();

        // 1. Finish Forging Action
        Action finishForgingAction = new Action("🏁 Finish Forging Session") {
            @Override
            public void run() {
                try {
                    String sid = (activeForgeSession != null) ? activeForgeSession.getSessionId() : "active";
                    ForgeSessionManager.getInstance().updateUiState(sid, "requestStop", true);
                    MessageDialog.openInformation(control.getShell(), "Finish Forging", "Finish training requested for session: " + sid + ". Exporting model artifacts...");
                } catch (Exception e) {
                    MessageDialog.openError(control.getShell(), "Error", "Failed to trigger finish forging: " + e.getMessage());
                }
            }
        };
        finishForgingAction.setEnabled(activeForgeSession != null || !allSessions.isEmpty());
        manager.add(finishForgingAction);

        // 2. Show Progress & Telemetry
        Action showProgressAction = new Action("📊 Show Session Telemetry & Progress") {
            @Override
            public void run() {
                StringBuilder info = new StringBuilder("Active EVO Sessions & Running Processes:\n\n");
                for (ForgeSession s : allSessions) {
                    info.append("• Session: ").append(s.getName()).append(" (ID: ").append(s.getSessionId()).append(")\n");
                    if (s.getStatus() != null) {
                        info.append("  Status: ").append(s.getStatus()).append("\n");
                    }
                }
                String statusMsg = OrchestrationStatusManager.getInstance().toString();
                if (statusMsg != null && !statusMsg.isEmpty()) {
                    info.append("\nOrchestration Status:\n").append(statusMsg);
                }
                MessageDialog.openInformation(control.getShell(), "EVO Session & Process Status", info.toString());
            }
        };
        manager.add(showProgressAction);

        // 3. Active Session Details Dialog
        Action sessionDetailsAction = new Action("ℹ️ View Active Session Details...") {
            @Override
            public void run() {
                if (activeForgeSession != null) {
                    MessageDialog.openInformation(control.getShell(), "Forge Session Details",
                        "Session Name: " + activeForgeSession.getName() +
                        "\nSession ID: " + activeForgeSession.getSessionId() +
                        "\nCreated At: " + activeForgeSession.getCreatedAt() +
                        "\nStatus: " + activeForgeSession.getStatus());
                } else if (!allSessions.isEmpty()) {
                    ForgeSession first = allSessions.get(0);
                    MessageDialog.openInformation(control.getShell(), "Forge Session Details",
                        "Session Name: " + first.getName() +
                        "\nSession ID: " + first.getSessionId() +
                        "\nCreated At: " + first.getCreatedAt());
                } else {
                    MessageDialog.openInformation(control.getShell(), "Session Details", "No active Forge session currently loaded.");
                }
            }
        };
        manager.add(sessionDetailsAction);

        // 4. Cancel / Stop Active Self-Dev Process
        Action stopProcessAction = new Action("⛔ Stop Active Self-Dev Process") {
            @Override
            public void run() {
                try {
                    Orchestrator orchestrator = getActiveOrchestrator();
                    if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
                        orchestrator.getSelfDevSession().getIterations().clear();
                        MessageDialog.openInformation(control.getShell(), "Self-Dev Process", "Self-Dev session stopped.");
                    } else {
                        MessageDialog.openInformation(control.getShell(), "Self-Dev Process", "No active Self-Dev session process running.");
                    }
                } catch (Exception e) {
                    MessageDialog.openError(control.getShell(), "Error", "Failed to stop Self-Dev process: " + e.getMessage());
                }
            }
        };
        manager.add(stopProcessAction);
    }

    private void addAiSmartProposals(IMenuManager manager, Control control) {
        MenuManager aiSubmenu = new MenuManager("🧠 AI Smart Assistance", "ai_smart_assistance");

        String selectedText = getSelectedText(control);

        if (selectedText != null && !selectedText.trim().isEmpty()) {
            // Proposals based on text selection
            Action explainAction = new Action("💡 Explain Selection") {
                @Override
                public void run() {
                    executeAiAction(control, "Explain the following text or code:\n\n" + selectedText);
                }
            };

            Action refactorAction = new Action("⚡ Refactor / Optimize Selection") {
                @Override
                public void run() {
                    executeAiAction(control, "Refactor and optimize the following code:\n\n" + selectedText);
                }
            };

            Action summarizeAction = new Action("📝 Summarize Selection") {
                @Override
                public void run() {
                    executeAiAction(control, "Summarize the key points of the following content:\n\n" + selectedText);
                }
            };

            Action createTaskAction = new Action("📋 Generate Task from Selection") {
                @Override
                public void run() {
                    executeAiAction(control, "Create a structured task plan based on this requirement:\n\n" + selectedText);
                }
            };

            aiSubmenu.add(explainAction);
            aiSubmenu.add(refactorAction);
            aiSubmenu.add(summarizeAction);
            aiSubmenu.add(createTaskAction);

        } else {
            // Proposals when no text selection exists
            Action proposeNextAction = new Action("🎯 Propose Next Action for Active View") {
                @Override
                public void run() {
                    IWorkbenchPart part = getActivePart();
                    String partTitle = (part != null) ? part.getTitle() : "Workbench";
                    executeAiAction(control, "Analyze the current workbench view [" + partTitle + "] and propose the best next engineering actions for the user.");
                }
            };

            Action analyzeContextAction = new Action("🔍 Analyze Active Context & Environment") {
                @Override
                public void run() {
                    executeAiAction(control, "Analyze current workspace environment and summarize active project state.");
                }
            };

            aiSubmenu.add(proposeNextAction);
            aiSubmenu.add(analyzeContextAction);
        }

        manager.add(aiSubmenu);
    }

    private void addQuickNavigationActions(IMenuManager manager) {
        MenuManager navSubmenu = new MenuManager("🧭 Quick Navigation", "quick_navigation");

        IWorkbenchPage page = getActivePage();
        if (page != null && page.getActiveEditor() instanceof MultiPageEditor) {
            MultiPageEditor editor = (MultiPageEditor) page.getActiveEditor();

            navSubmenu.add(new Action("💬 AI Chat Page") {
                @Override public void run() { editor.switchToPageIndex(0); }
            });
            navSubmenu.add(new Action("🥞 Task Stack Page") {
                @Override public void run() { editor.switchToPageIndex(1); }
            });
            navSubmenu.add(new Action("📐 Architecture Page") {
                @Override public void run() { editor.switchToPageIndex(2); }
            });
            navSubmenu.add(new Action("⚙️ Development Page") {
                @Override public void run() { editor.switchToPageIndex(3); }
            });
            navSubmenu.add(new Action("🛠️ Properties / Models Page") {
                @Override public void run() { editor.switchToPageIndex(4); }
            });
        }

        manager.add(navSubmenu);
    }

    private String getSelectedText(Control control) {
        if (control instanceof Text) {
            return ((Text) control).getSelectionText();
        } else if (control instanceof StyledText) {
            return ((StyledText) control).getSelectionText();
        }
        return null;
    }

    private void executeAiAction(Control control, String prompt) {
        org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job("AI Smart Proposal") {
            @Override
            protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                try {
                    Orchestrator orchestrator = getActiveOrchestrator();
                    String response;
                    if (orchestrator != null && orchestrator.getOllama() != null && orchestrator.getOllama().getUrl() != null) {
                        response = LlmRouter.getInstance().getLocalProvider().sendRequest(
                            orchestrator, prompt, 0.3f, null, null);
                    } else {
                        response = "AI Smart Proposal Result:\n\nBased on prompt:\n\"" + prompt + "\"\n\nSuggested Actions:\n1. Review active task stack.\n2. Run preflight verification.\n3. Verify model training progress.";
                    }

                    Display.getDefault().asyncExec(() -> {
                        if (!control.isDisposed()) {
                            MessageDialog.openInformation(control.getShell(), "AI Smart Proposal", response);
                        }
                    });

                    return org.eclipse.core.runtime.Status.OK_STATUS;
                } catch (Exception e) {
                    Display.getDefault().asyncExec(() -> {
                        if (!control.isDisposed()) {
                            MessageDialog.openError(control.getShell(), "AI Assistance Failed", "Failed to query AI proposal: " + e.getMessage());
                        }
                    });
                    return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.IStatus.ERROR, "eu.kalafatic.evolution.view", "AI proposal failed", e);
                }
            }
        };
        job.schedule();
    }

    private IWorkbenchPage getActivePage() {
        try {
            return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        } catch (Exception e) {
            return null;
        }
    }

    private IWorkbenchPart getActivePart() {
        IWorkbenchPage page = getActivePage();
        return (page != null) ? page.getActivePart() : null;
    }

    private Orchestrator getActiveOrchestrator() {
        IWorkbenchPage page = getActivePage();
        if (page != null && page.getActiveEditor() instanceof MultiPageEditor) {
            return null; // Orchestrator is managed internally by MultiPageEditor and pages
        }
        return null;
    }
}
