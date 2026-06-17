package eu.kalafatic.evolution.view.editors.pages.server;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.ServerSettings;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.tools.AToolGroup;
import eu.kalafatic.utils.factories.GUIFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SettingsGroup extends AToolGroup {

    private Text portText;
    private Button autoStartCheck;
    private Runnable onStateChange;
    private TableViewer serverViewer;

    public SettingsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor, Runnable onStateChange) {
        super(editor, orchestrator, successColor);
        this.onStateChange = onStateChange;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Server Settings");
        section.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        group = toolkit.createComposite(section);
        group.setLayout(new GridLayout(2, false));
        section.setClient(group);

        toolkit.createLabel(group, "Server Port:");
        portText = toolkit.createText(group, "48080");
        portText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        toolkit.createLabel(group, "Auto-start with UI:");
        autoStartCheck = toolkit.createButton(group, "", SWT.CHECK);

        Composite btnComp = toolkit.createComposite(group);
        btnComp.setLayout(new GridLayout(3, false));
        btnComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Button startBtn = GUIFactory.INSTANCE.createButton(btnComp, "Start");
        startBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    int port = Integer.parseInt(portText.getText());
                    ServerManager.getInstance().start(port);
                    if (onStateChange != null) onStateChange.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        Button stopBtn = GUIFactory.INSTANCE.createButton(btnComp, "Stop");
        stopBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    int port = Integer.parseInt(portText.getText());
                    ServerManager.getInstance().stop(port);
                    if (onStateChange != null) onStateChange.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        Button restartBtn = GUIFactory.INSTANCE.createButton(btnComp, "Restart");
        restartBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    int port = Integer.parseInt(portText.getText());
                    ServerManager.getInstance().restart(port);
                    if (onStateChange != null) onStateChange.run();
                    refreshUI();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        toolkit.createLabel(group, "Active Servers:");
        serverViewer = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = serverViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
        tableGd.heightHint = 150;
        table.setLayoutData(tableGd);

        createServerColumn("Action", 100, s -> "");
        createServerColumn("State", 80, s -> s.running ? "RUNNING" : "STOPPED");
        createServerColumn("Address", 150, s -> "localhost:" + s.port);
        createServerColumn("Note", 200, s -> s.port == 48080 ? "Primary Port" : "Evolution Node");

        // Tooltips for columns
        serverViewer.getTable().getColumn(0).setToolTipText("Click 'Start' or 'Stop' in this column to manage the server lifecycle.");
        serverViewer.getTable().getColumn(1).setToolTipText("Current execution state of the Evolution server instance.");
        serverViewer.getTable().getColumn(2).setToolTipText("Local network address and port for connecting to this instance.");
        serverViewer.getTable().getColumn(3).setToolTipText("Functional role or specific identification note for this server.");

        serverViewer.setContentProvider(ArrayContentProvider.getInstance());

        // Add button listener for the "Action" column
        table.addListener(SWT.MouseDown, event -> {
            org.eclipse.swt.widgets.TableItem item = table.getItem(new org.eclipse.swt.graphics.Point(event.x, event.y));
            if (item != null) {
                ServerStatus s = (ServerStatus) item.getData();
                if (event.x < 100) { // Action column
                    try {
                        if (s.running) {
                            ServerManager.getInstance().stop(s.port);
                        } else {
                            ServerManager.getInstance().start(s.port);
                        }
                        if (onStateChange != null) onStateChange.run();
                        refreshUI();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
    }

    private void createServerColumn(String title, int width, java.util.function.Function<ServerStatus, String> mapper) {
        TableViewerColumn col = new TableViewerColumn(serverViewer, SWT.NONE);
        col.getColumn().setText(title);
        col.getColumn().setWidth(width);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return mapper.apply((ServerStatus) element);
            }
            @Override
            public Color getForeground(Object element) {
                if ("State".equals(title)) {
                    ServerStatus s = (ServerStatus) element;
                    return s.running ? group.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN) : group.getDisplay().getSystemColor(SWT.COLOR_RED);
                }
                if ("Action".equals(title)) {
                    return group.getDisplay().getSystemColor(SWT.COLOR_BLUE);
                }
                return super.getForeground(element);
            }
        });
    }

    private static class ServerStatus {
        int port;
        boolean running;
        ServerStatus(int p, boolean r) { this.port = p; this.running = r; }
    }

    @Override
    protected void refreshUI() {
        if (orchestrator == null) return;
        ServerSettings settings = orchestrator.getServerSettings();
        if (settings != null) {
            setTextSafe(portText, String.valueOf(settings.getPort()));
            setSelectionSafe(autoStartCheck, settings.isAutoStart());
        }

        if (serverViewer != null && !serverViewer.getControl().isDisposed()) {
            Map<Integer, Boolean> statuses = ServerManager.getInstance().getServerStatuses();
            List<ServerStatus> list = new ArrayList<>();
            statuses.forEach((p, r) -> list.add(new ServerStatus(p, r)));

            // Ensure primary port is in the list even if not running
            int primary = 48080;
            try { primary = Integer.parseInt(portText.getText()); } catch(Exception e) {}
            final int finalPrimary = primary;
            if (list.stream().noneMatch(s -> s.port == finalPrimary)) {
                list.add(new ServerStatus(finalPrimary, false));
            }

            serverViewer.setInput(list);
        }
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
}
