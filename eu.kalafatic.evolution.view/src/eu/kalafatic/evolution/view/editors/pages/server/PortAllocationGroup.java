package eu.kalafatic.evolution.view.editors.pages.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.kalafatic.evolution.controller.orchestration.ServerEntry;
import eu.kalafatic.evolution.controller.orchestration.ServerLifecycleListener;
import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import eu.kalafatic.evolution.controller.orchestration.ServerState;
import eu.kalafatic.evolution.controller.orchestration.WebApplicationDescriptor;
import eu.kalafatic.evolution.controller.resource.PortAllocationManager;
import eu.kalafatic.evolution.controller.resource.PortConfiguration;
import eu.kalafatic.evolution.controller.resource.PortRegistry;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * UI Group managing the authoritative EVO service-to-port allocation, lifecycle states,
 * and context-menu actions (Start, Stop, Restart, Open in Browser).
 */
public class PortAllocationGroup extends AEvoGroup implements ServerLifecycleListener {

    private TableViewer portTableViewer;

    public PortAllocationGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
        ServerManager.getInstance().addListener(this);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Port Allocations & Server Runtime Lifecycle");
        section.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        group = toolkit.createComposite(section);
        group.setLayout(new GridLayout(1, false));
        section.setClient(group);

        Composite actionsComp = toolkit.createComposite(group);
        actionsComp.setLayout(new GridLayout(5, false));
        actionsComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button refreshBtn = GUIFactory.INSTANCE.createButton(actionsComp, "Refresh Allocations");
        refreshBtn.addListener(SWT.Selection, e -> {
            ServerManager.getInstance().refreshRuntimeStates();
            refreshUI();
        });

        Button startAllBtn = GUIFactory.INSTANCE.createButton(actionsComp, "Start All Servers");
        startAllBtn.addListener(SWT.Selection, e -> {
            ServerManager.getInstance().autoStartAll();
            refreshUI();
        });

        Button openSelectedBtn = GUIFactory.INSTANCE.createButton(actionsComp, "Open Selected Port");
        openSelectedBtn.addListener(SWT.Selection, e -> openSelected());

        Button openAllPortsBtn = GUIFactory.INSTANCE.createButton(actionsComp, "Open All EVO Ports");
        openAllPortsBtn.addListener(SWT.Selection, e -> openAllEvoPorts());

        Button openAllWebPagesBtn = GUIFactory.INSTANCE.createButton(actionsComp, "Open All EVO Web Pages");
        openAllWebPagesBtn.addListener(SWT.Selection, e -> openAllEvoWebPages());

        portTableViewer = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = portTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableGd.heightHint = 200;
        table.setLayoutData(tableGd);

        createColumn("Service ID", 130, ServerEntry::getServiceId);
        createColumn("Service Name", 230, ServerEntry::getName);
        createColumn("Type", 75, ServerEntry::getServerType);
        createColumn("Port", 65, e -> String.valueOf(e.getPort()));
        createColumn("Address / Endpoint", 200, ServerEntry::getEndpointUrl);
        createColumn("Runtime State", 100, e -> e.getState().name());

        portTableViewer.addDoubleClickListener(event -> openSelected());

        createContextMenu();

        portTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        refreshUI();
    }

    private void createContextMenu() {
        MenuManager menuMgr = new MenuManager("#ServerPortPopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(portTableViewer.getControl());
        portTableViewer.getControl().setMenu(menu);
    }

    private void fillContextMenu(IMenuManager manager) {
        IStructuredSelection sel = (IStructuredSelection) portTableViewer.getSelection();
        if (sel == null || sel.isEmpty()) {
            return;
        }

        ServerEntry entry = (ServerEntry) sel.getFirstElement();
        ServerState state = entry.getState();
        int port = entry.getPort();

        // 1. Start action
        Action startAction = new Action("Start") {
            @Override
            public void run() {
                executeServerJob("Starting " + entry.getName(), () -> {
                    try {
                        ServerManager.getInstance().start(port);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        };
        startAction.setEnabled(entry.canStart());
        manager.add(startAction);

        // 2. Stop action
        Action stopAction = new Action("Stop") {
            @Override
            public void run() {
                executeServerJob("Stopping " + entry.getName(), () -> {
                    ServerManager.getInstance().stop(port);
                });
            }
        };
        stopAction.setEnabled(entry.canStop());
        manager.add(stopAction);

        // 3. Restart action
        Action restartAction = new Action("Restart") {
            @Override
            public void run() {
                executeServerJob("Restarting " + entry.getName(), () -> {
                    try {
                        ServerManager.getInstance().restart(port);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        };
        restartAction.setEnabled(entry.canRestart());
        manager.add(restartAction);

        manager.add(new Separator());

        // 4. Open in Browser
        Action openInBrowserAction = new Action("Open in Browser") {
            @Override
            public void run() {
                if (editor != null && entry.getEndpointUrl() != null) {
                    editor.showBrowserPage(entry.getEndpointUrl());
                }
            }
        };
        openInBrowserAction.setEnabled(entry.canOpenInBrowser() && state == ServerState.RUNNING);
        manager.add(openInBrowserAction);

        manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private void executeServerJob(String taskName, Runnable task) {
        Job job = new Job(taskName) {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    task.run();
                } finally {
                    Display.getDefault().asyncExec(() -> refreshUI());
                }
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
    }

    private void openSelected() {
        IStructuredSelection sel = (IStructuredSelection) portTableViewer.getSelection();
        if (sel != null && !sel.isEmpty()) {
            ServerEntry entry = (ServerEntry) sel.getFirstElement();
            if (editor != null && entry.getEndpointUrl() != null) {
                editor.showBrowserPage(entry.getEndpointUrl());
            }
        }
    }

    public void openAllEvoPorts() {
        List<String> urls = new ArrayList<>();
        for (ServerEntry entry : ServerManager.getInstance().getServerEntries()) {
            if ("TCP".equalsIgnoreCase(entry.getServerType())) {
                continue;
            }
            urls.add("http://127.0.0.1:" + entry.getPort() + "/");
        }
        if (editor != null) {
            editor.showBrowserPage(urls);
        }
    }

    public void openAllEvoWebPages() {
        List<String> urls = new ArrayList<>();
        for (WebApplicationDescriptor app : ServerManager.getInstance().getWebApplications()) {
            urls.add(app.getFullUrl());
        }
        if (editor != null) {
            editor.showBrowserPage(urls);
        }
    }

    private void createColumn(String title, int width, java.util.function.Function<ServerEntry, String> mapper) {
        TableViewerColumn col = new TableViewerColumn(portTableViewer, SWT.NONE);
        col.getColumn().setText(title);
        col.getColumn().setWidth(width);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return mapper.apply((ServerEntry) element);
            }
            @Override
            public Color getForeground(Object element) {
                if ("Runtime State".equals(title)) {
                    ServerEntry e = (ServerEntry) element;
                    switch (e.getState()) {
                        case RUNNING:
                            return group.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
                        case STARTING:
                        case STOPPING:
                            return group.getDisplay().getSystemColor(SWT.COLOR_DARK_YELLOW);
                        case FAILED:
                            return group.getDisplay().getSystemColor(SWT.COLOR_RED);
                        case STOPPED:
                        default:
                            return group.getDisplay().getSystemColor(SWT.COLOR_GRAY);
                    }
                }
                return super.getForeground(element);
            }
        });
    }

    @Override
    public void refreshUI() {
        if (portTableViewer != null && !portTableViewer.getControl().isDisposed()) {
            List<ServerEntry> entries = ServerManager.getInstance().getServerEntries();
            portTableViewer.setInput(entries);
        }
    }

    @Override
    public void onServerStateChanged(ServerEntry entry) {
        if (group != null && !group.isDisposed()) {
            group.getDisplay().asyncExec(this::refreshUI);
        }
    }

    @Override
    public void onAllServersStarted(List<ServerEntry> servers) {
        if (group != null && !group.isDisposed()) {
            group.getDisplay().asyncExec(this::refreshUI);
        }
    }

    @Override
    public void onWebApplicationReady(WebApplicationDescriptor webApp) {
        // No-op for port allocation table, state is covered by onServerStateChanged
    }

    @Override
    public void onWebApplicationUnavailable(WebApplicationDescriptor webApp) {
        // No-op for port allocation table
    }

    @Override
    public void dispose() {
        ServerManager.getInstance().removeListener(this);
        super.dispose();
    }
}
