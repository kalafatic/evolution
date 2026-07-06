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

public class McpPromptsGroup extends AEvoGroup {
    private Table promptsTable;
    private McpSettingsPage page;

    public McpPromptsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, McpSettingsPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    @Override
    protected void refreshUI() {
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Available Prompts", 1, false);

        promptsTable = new Table(group, SWT.BORDER | SWT.FULL_SELECTION);
        promptsTable.setHeaderVisible(true);
        promptsTable.setLinesVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.heightHint = 150;
        promptsTable.setLayoutData(gd);

        String[] headers = { "Name", "Description", "Arguments" };
        int[] widths = { 150, 350, 300 };
        for (int i = 0; i < headers.length; i++) {
            TableColumn col = new TableColumn(promptsTable, SWT.NONE);
            col.setText(headers[i]);
            col.setWidth(widths[i]);
        }

        Button refreshBtn = GUIFactory.INSTANCE.createButton(group, "Refresh Prompts", 150);
        refreshBtn.setToolTipText("Fetch the latest list of prompts from the connected MCP server.");
        refreshBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.refreshPrompts();
            }
        });
    }

    public void clear() {
        promptsTable.removeAll();
    }

    public boolean isDisposed() {
        return promptsTable == null || promptsTable.isDisposed();
    }

    public void addItem(String name, String desc, String args) {
        if (isDisposed()) return;
        TableItem item = new TableItem(promptsTable, SWT.NONE);
        item.setText(0, name);
        item.setText(1, desc);
        item.setText(2, args);
    }
}
