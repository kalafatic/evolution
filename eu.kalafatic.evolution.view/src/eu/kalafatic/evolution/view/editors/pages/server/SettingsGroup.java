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
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.kalafatic.evolution.controller.orchestration.ServerEntry;
import eu.kalafatic.evolution.controller.orchestration.ServerLifecycleListener;
import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import eu.kalafatic.evolution.controller.orchestration.ServerState;
import eu.kalafatic.evolution.controller.orchestration.WebApplicationDescriptor;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.ServerSettings;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.tools.AToolGroup;
import eu.kalafatic.utils.factories.GUIFactory;

public class SettingsGroup extends AToolGroup implements ServerLifecycleListener {

    private Text portText;
    private Button autoStartCheck;
    private Button authenticateCheck;
    private Runnable onStateChange;
    private TableViewer serverViewer;

    public SettingsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor, Runnable onStateChange) {
        super(editor, orchestrator, successColor);
        this.onStateChange = onStateChange;
        createControl(toolkit, parent);
        ServerManager.getInstance().addListener(this);
    }

    public void addModifyListener(ModifyListener listener) {
        if (portText != null) portText.addModifyListener(listener);
        if (autoStartCheck != null) autoStartCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                listener.modifyText(null);
            }
        });
        if (authenticateCheck != null) authenticateCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                listener.modifyText(null);
            }
        });
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Server Settings & Instances");
        section.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        group = toolkit.createComposite(section);
        group.setLayout(new GridLayout(2, false));
        section.setClient(group);

        toolkit.createLabel(group, "Server Port:");
        portText = toolkit.createText(group, "28080");
        portText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        autoStartCheck = toolkit.createButton(group, "Auto-start Server on Startup", SWT.CHECK);
        autoStartCheck.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        authenticateCheck = toolkit.createButton(group, "Enable API Authentication", SWT.CHECK);
        authenticateCheck.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Composite btnComp = toolkit.createComposite(group);
        btnComp.setLayout(new GridLayout(4, false));
        btnComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Button startBtn = GUIFactory.INSTANCE.createButton(btnComp, "Start All Servers");
        startBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ServerManager.getInstance().autoStartAll();
                if (onStateChange != null) onStateChange.run();
                refreshUI();
            }
        });

        Button stopBtn = GUIFactory.INSTANCE.createButton(btnComp, "Stop All Servers");
        stopBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ServerManager.getInstance().stopAll();
                if (onStateChange != null) onStateChange.run();
                refreshUI();
            }
        });

        Button openAllWebPagesBtn = GUIFactory.INSTANCE.createButton(btnComp, "Open All EVO Web Pages");
        openAllWebPagesBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openAllEvoWebPages();
            }
        });

        Button openAllPortsBtn = GUIFactory.INSTANCE.createButton(btnComp, "Open All EVO Ports");
        openAllPortsBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                openAllEvoPorts();
            }
        });

        toolkit.createLabel(group, "Configured Server Instances & Runtime State:");
        serverViewer = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = serverViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        tableGd.heightHint = 160;
        table.setLayoutData(tableGd);

        createServerColumn("Action", 75, s -> s.getState() == ServerState.RUNNING ? "Stop" : "Start");
        createServerColumn("Runtime State", 95, s -> s.getState().name());
        createServerColumn("Port", 60, s -> String.valueOf(s.getPort()));
        createServerColumn("Bind Address", 95, ServerEntry::getBindAddress);
        createServerColumn("LAN Access URL", 200, ServerEntry::getLanUrl);
        createServerColumn("Advertised URL", 200, ServerEntry::getAdvertisedUrl);
        createServerColumn("Service / Description", 200, ServerEntry::getName);

        serverViewer.setContentProvider(ArrayContentProvider.getInstance());

        // Context menu
        createContextMenu();

        // Mouse click on "Action" column
        table.addListener(SWT.MouseDown, event -> {
            org.eclipse.swt.widgets.TableItem item = table.getItem(new org.eclipse.swt.graphics.Point(event.x, event.y));
            if (item != null) {
                ServerEntry entry = (ServerEntry) item.getData();
                if (event.x < 80) { // Action column
                    executeServerJob((entry.getState() == ServerState.RUNNING ? "Stopping " : "Starting ") + entry.getName(), () -> {
                        try {
                            if (entry.getState() == ServerState.RUNNING) {
                                ServerManager.getInstance().stop(entry.getPort());
                            } else {
                                ServerManager.getInstance().start(entry.getPort());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            }
        });

        refreshUI();
    }

    private void createContextMenu() {
        MenuManager menuMgr = new MenuManager("#SettingsServerPopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        Menu menu = menuMgr.createContextMenu(serverViewer.getControl());
        serverViewer.getControl().setMenu(menu);
    }

    private void fillContextMenu(IMenuManager manager) {
        IStructuredSelection sel = (IStructuredSelection) serverViewer.getSelection();
        if (sel == null || sel.isEmpty()) {
            return;
        }

        ServerEntry entry = (ServerEntry) sel.getFirstElement();
        ServerState state = entry.getState();
        int port = entry.getPort();

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
                    Display.getDefault().asyncExec(() -> {
                        if (onStateChange != null) onStateChange.run();
                        refreshUI();
                    });
                }
                return Status.OK_STATUS;
            }
        };
        job.setUser(true);
        job.schedule();
    }

    private void createServerColumn(String title, int width, java.util.function.Function<ServerEntry, String> mapper) {
        TableViewerColumn col = new TableViewerColumn(serverViewer, SWT.NONE);
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
                if ("Action".equals(title)) {
                    return group.getDisplay().getSystemColor(SWT.COLOR_BLUE);
                }
                return super.getForeground(element);
            }
        });
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

    @Override
    protected void refreshUI() {
        if (orchestrator == null) return;
        ServerSettings settings = orchestrator.getServerSettings();
        if (settings != null) {
            setTextSafe(portText, String.valueOf(settings.getPort()));
            setSelectionSafe(autoStartCheck, settings.isAutoStart());
            setSelectionSafe(authenticateCheck, settings.isAuthenticate());
        }

        if (serverViewer != null && !serverViewer.getControl().isDisposed()) {
            List<ServerEntry> entries = ServerManager.getInstance().getServerEntries();
            serverViewer.setInput(entries);
        }
    }

    @Override
    public void onServerStateChanged(ServerEntry entry) {
        if (group != null && !group.isDisposed()) {
            group.getDisplay().asyncExec(() -> {
                if (onStateChange != null) onStateChange.run();
                refreshUI();
            });
        }
    }

    @Override
    public void onAllServersStarted(List<ServerEntry> servers) {
        if (group != null && !group.isDisposed()) {
            group.getDisplay().asyncExec(() -> {
                if (onStateChange != null) onStateChange.run();
                refreshUI();
            });
        }
    }

    @Override
    public void onWebApplicationReady(WebApplicationDescriptor webApp) {
        // Handled by state changes
    }

    @Override
    public void onWebApplicationUnavailable(WebApplicationDescriptor webApp) {
        // Handled by state changes
    }

    @Override
    public void updateModel() {
        if (orchestrator == null) return;
        ServerSettings settings = orchestrator.getServerSettings();
        if (settings == null) {
            settings = OrchestrationFactory.eINSTANCE.createServerSettings();
            orchestrator.setServerSettings(settings);
        }
        try {
            settings.setPort(Integer.parseInt(portText.getText()));
        } catch (NumberFormatException e) {
            // keep old value
        }
        settings.setAutoStart(autoStartCheck.getSelection());
        boolean auth = authenticateCheck.getSelection();
        settings.setAuthenticate(auth);
        System.setProperty("evolution.api.authenticate", String.valueOf(auth));
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { portText };
    }

    @Override
    protected String getTestStatus() {
        return null;
    }

    @Override
    protected void clearTestStatus() {
    }

    @Override
    public void dispose() {
        ServerManager.getInstance().removeListener(this);
        super.dispose();
    }
}
