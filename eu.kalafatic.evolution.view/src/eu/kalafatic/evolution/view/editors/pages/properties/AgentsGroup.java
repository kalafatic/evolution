package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class AgentsGroup extends AEvoGroup {
    private Table agentsTable;

    public AgentsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "Agents", 1, false);
        agentsTable = new Table(group, SWT.BORDER | SWT.FULL_SELECTION);
        agentsTable.setHeaderVisible(true);
        agentsTable.setLinesVisible(true);
        agentsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
        String[] headers = { "ID", "Type", "Execution Mode" };
        int[] widths = { 100, 100, 120 };
        for (int i = 0; i < headers.length; i++) {
            TableColumn col = new TableColumn(agentsTable, SWT.NONE);
            col.setText(headers[i]);
            col.setWidth(widths[i]);
        }
    }

    @Override
    public void updateUI() {
        if (orchestrator != null) {
            agentsTable.removeAll();
            for (Agent a : orchestrator.getAgents()) {
                TableItem item = new TableItem(agentsTable, SWT.NONE);
                item.setText(0, a.getId() != null ? a.getId() : "");
                item.setText(1, a.getType() != null ? a.getType() : "");
                item.setText(2, a.getExecutionMode() != null ? a.getExecutionMode().name() : "");
            }
        }
    }
}
