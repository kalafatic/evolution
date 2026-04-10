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
import eu.kalafatic.evolution.view.editors.pages.McpSettingsPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class McpResourcesGroup extends AEvoGroup {
    private Table resourcesTable;
    private McpSettingsPage page;

    public McpResourcesGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, McpSettingsPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
        // Refresh handled by page.refreshResources()
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Available Resources", 1, false);

        resourcesTable = new Table(group, SWT.BORDER | SWT.FULL_SELECTION);
        resourcesTable.setHeaderVisible(true);
        resourcesTable.setLinesVisible(true);
        resourcesTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        String[] headers = { "Name", "URI", "Description" };
        int[] widths = { 150, 250, 300 };
        for (int i = 0; i < headers.length; i++) {
            TableColumn col = new TableColumn(resourcesTable, SWT.NONE);
            col.setText(headers[i]);
            col.setWidth(widths[i]);
        }

        Button refreshBtn = SWTFactory.createButton(group, "Refresh Resources", 150);
        refreshBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.refreshResources();
            }
        });
    }

    public void clear() {
        resourcesTable.removeAll();
    }

    public void addItem(String name, String uri, String desc) {
        if (resourcesTable.isDisposed()) return;
        TableItem item = new TableItem(resourcesTable, SWT.NONE);
        item.setText(0, name);
        item.setText(1, uri);
        item.setText(2, desc);
    }
}
