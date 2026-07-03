package eu.kalafatic.evolution.view.editors.pages.mcpsettings;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.McpSettingsPage;
import eu.kalafatic.utils.factories.GUIFactory;

public class McpToolsGroup extends AEvoGroup {
    private Table toolsTable;
    private McpSettingsPage page;

    public McpToolsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, McpSettingsPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Available Tools", 1, false);

        toolsTable = new Table(group, SWT.BORDER | SWT.FULL_SELECTION);
        toolsTable.setHeaderVisible(true);
        toolsTable.setLinesVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 150;
        toolsTable.setLayoutData(gd);

        String[] headers = { "Name", "Description", "Input Schema" };
        int[] widths = { 150, 350, 300 };
        for (int i = 0; i < headers.length; i++) {
            TableColumn col = new TableColumn(toolsTable, SWT.NONE);
            col.setText(headers[i]);
            col.setWidth(widths[i]);
        }

        Button refreshBtn = GUIFactory.INSTANCE.createButton(group, "Refresh Tools", 150);
        refreshBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.refreshTools();
            }
        });
    }

    public void clear() {
        toolsTable.removeAll();
    }

    public void addItem(String name, String desc, String schema) {
        if (toolsTable.isDisposed()) return;
        TableItem item = new TableItem(toolsTable, SWT.NONE);
        item.setText(0, name);
        item.setText(1, desc);
        item.setText(2, schema);
    }
}
