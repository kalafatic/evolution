package eu.kalafatic.evolution.view.editors.pages.tests;

import java.util.List;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Test;
import eu.kalafatic.evolution.view.editors.pages.TestsPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class TestTableGroup {
    private Composite group;
    private FormToolkit toolkit;
    private TestsPage page;

    public TestTableGroup(FormToolkit toolkit, Composite parent, String title, List<Test> tests, boolean expanded, TestsPage page) {
        this.toolkit = toolkit;
        this.page = page;
        createControl(parent, title, tests, expanded);
    }

    private void createControl(Composite parent, String title, List<Test> tests, boolean expanded) {
        group = SWTFactory.createExpandableGroup(parent, title, 1, expanded);

        Composite tableComposite = toolkit.createComposite(group);
        tableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        TableColumnLayout layout = new TableColumnLayout();
        tableComposite.setLayout(layout);

        Table table = toolkit.createTable(tableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        addColumn(table, layout, "Name", 150, 30);
        addColumn(table, layout, "Path", 250, 40);
        addColumn(table, layout, "Status", 100, 15);
        addColumn(table, layout, "Execute", 100, 15);

        for (final Test test : tests) {
            final TableItem item = new TableItem(table, SWT.NONE);
            item.setText(0, test.getName() != null ? test.getName() : "New Test");
            item.setText(1, test.getPath() != null ? test.getPath() : "");
            item.setText(2, test.getStatus().toString());

            TableEditor execEditor = new TableEditor(table);
            Button execBtn = toolkit.createButton(table, "Execute", SWT.PUSH);
            execBtn.pack();
            execEditor.minimumWidth = execBtn.getSize().x;
            execEditor.horizontalAlignment = SWT.CENTER;
            execEditor.setEditor(execBtn, item, 3);
            execBtn.addSelectionListener(new SelectionAdapter() {
                @Override public void widgetSelected(SelectionEvent e) { page.executeTest(test); }
            });
            page.registerTestRow(test, execBtn, item);
        }
        GridData groupGd = (GridData) group.getLayoutData();
        groupGd.heightHint = 200;
    }

    private void addColumn(Table table, TableColumnLayout layout, String text, int width, int weight) {
        TableColumn col = new TableColumn(table, SWT.NONE);
        col.setText(text);
        layout.setColumnData(col, new ColumnWeightData(weight, width, true));
    }
}
