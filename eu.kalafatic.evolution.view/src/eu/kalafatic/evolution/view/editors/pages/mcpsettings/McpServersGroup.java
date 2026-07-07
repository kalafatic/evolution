package eu.kalafatic.evolution.view.editors.pages.mcpsettings;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.NetworkEntry;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.McpSettingsPage;
import eu.kalafatic.utils.factories.GUIFactory;

import java.util.List;
import java.util.stream.Collectors;

public class McpServersGroup extends AEvoGroup {
    private Table serversTable;
    private McpSettingsPage page;

    public McpServersGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, McpSettingsPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Managed MCP Servers", 1, true);

        Composite main =  GUIFactory.INSTANCE.createComposite(group, 2);

        serversTable = new Table(main, SWT.BORDER | SWT.FULL_SELECTION);
        serversTable.setHeaderVisible(true);
        serversTable.setLinesVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 150;
        serversTable.setLayoutData(gd);

        String[] headers = { "Host", "Address", "Port", "Note" };
        int[] widths = { 150, 200, 80, 250 };
        for (int i = 0; i < headers.length; i++) {
            TableColumn col = new TableColumn(serversTable, SWT.NONE);
            col.setText(headers[i]);
            col.setWidth(widths[i]);
        }

        Composite btnComp = GUIFactory.INSTANCE.createComposite(main);
        
        Button addBtn = GUIFactory.INSTANCE.createButton(btnComp, "Add");
        addBtn.setToolTipText("Add a new MCP server configuration to the managed list.");
        addBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleAdd();
            }
        });

        Button editBtn = GUIFactory.INSTANCE.createButton(btnComp, "Edit");
        editBtn.setToolTipText("Edit the selected MCP server configuration.");
        editBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleEdit();
            }
        });

        Button removeBtn = GUIFactory.INSTANCE.createButton(btnComp, "Remove");
        removeBtn.setToolTipText("Remove the selected MCP server configuration.");
        removeBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleRemove();
            }
        });

        Button setActiveBtn = GUIFactory.INSTANCE.createButton(btnComp, "Set Active");
        setActiveBtn.setToolTipText("Set the selected server as the active Remote MCP Server URL.");
        setActiveBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleSetActive();
            }
        });

        Button testBtn = GUIFactory.INSTANCE.createButton(btnComp, "Test");
        testBtn.setToolTipText("Test the connection to the selected MCP server.");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleTest();
            }
        });
    }

    private void handleAdd() {
        NetworkEntry entry = OrchestrationFactory.eINSTANCE.createNetworkEntry();
        McpServerDialog dialog = new McpServerDialog(group.getShell(), entry);
        if (dialog.open() == Window.OK) {
            orchestrator.getNetworkEntries().add(entry);
            page.setDirty(true);
            refreshUI();
        }
    }

    private void handleEdit() {
        int index = serversTable.getSelectionIndex();
        if (index < 0) return;
        NetworkEntry entry = (NetworkEntry) serversTable.getItem(index).getData();
        McpServerDialog dialog = new McpServerDialog(group.getShell(), entry);
        if (dialog.open() == Window.OK) {
            page.setDirty(true);
            refreshUI();
        }
    }

    private void handleRemove() {
        int index = serversTable.getSelectionIndex();
        if (index < 0) return;
        NetworkEntry entry = (NetworkEntry) serversTable.getItem(index).getData();
        orchestrator.getNetworkEntries().remove(entry);
        page.setDirty(true);
        refreshUI();
    }

    private void handleSetActive() {
        int index = serversTable.getSelectionIndex();
        if (index < 0) return;
        NetworkEntry entry = (NetworkEntry) serversTable.getItem(index).getData();
        String url = "http://" + entry.getAddress() + ":" + entry.getPort() + entry.getPath();
        orchestrator.setMcpServerUrl(url);
        page.setDirty(true);
        page.refreshUI();
    }

    private void handleTest() {
        int index = serversTable.getSelectionIndex();
        if (index < 0) return;
        NetworkEntry entry = (NetworkEntry) serversTable.getItem(index).getData();
        String url = "http://" + entry.getAddress() + ":" + entry.getPort() + entry.getPath();
        page.testConnection(url);
    }

    @Override
    protected void refreshUI() {
        if (orchestrator == null) return;
        serversTable.removeAll();
        List<NetworkEntry> mcpServers = orchestrator.getNetworkEntries().stream()
                .filter(e -> "MCP".equals(e.getType()))
                .collect(Collectors.toList());

        for (NetworkEntry entry : mcpServers) {
            TableItem item = new TableItem(serversTable, SWT.NONE);
            item.setText(0, entry.getHost() != null ? entry.getHost() : "");
            item.setText(1, entry.getAddress() != null ? entry.getAddress() : "");
            item.setText(2, String.valueOf(entry.getPort()));
            item.setText(3, entry.getNote() != null ? entry.getNote() : "");
            item.setData(entry);

            String currentUrl = orchestrator.getMcpServerUrl();
            String entryUrl = "http://" + entry.getAddress() + ":" + entry.getPort() + entry.getPath();
            if (entryUrl.equals(currentUrl)) {
                item.setBackground(lightGreen);
            }
        }
    }
}
