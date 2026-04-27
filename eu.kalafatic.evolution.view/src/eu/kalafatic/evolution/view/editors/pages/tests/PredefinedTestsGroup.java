package eu.kalafatic.evolution.view.editors.pages.tests;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
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
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Test;
import eu.kalafatic.evolution.model.orchestration.TestStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.TestsPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class PredefinedTestsGroup extends AEvoGroup {
	private TestsPage page;
	private FormToolkit toolkit;
	private List<TableEditor> editors = new ArrayList<>();
	private Table table;
	private TableColumnLayout tableLayout;
	private Composite tableComposite;

	public PredefinedTestsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, TestsPage page) {
		super(editor, orchestrator);
		this.toolkit = toolkit;
		this.page = page;
		createControl(parent);
	}

	@Override
	protected void refreshUI() {
		if (table == null || table.isDisposed()) return;

		table.setRedraw(false);
		try {
			for (TableEditor editor : editors) {
				if (editor.getEditor() != null && !editor.getEditor().isDisposed()) {
					editor.getEditor().dispose();
				}
				editor.setEditor(null, null, -1);
			}
			editors.clear();

			for (TableItem item : table.getItems()) {
				item.dispose();
			}
			// Disposing items doesn't dispose custom editors automatically in some SWT versions,
			// but here they are children of the table, so they should be fine if we manage them.
			for (org.eclipse.swt.widgets.Control child : table.getChildren()) {
				child.dispose();
			}

			if (orchestrator != null) {
				for (Class<?> testClass : page.getDiscoveredTestClasses()) {
					String name = testClass.getSimpleName();
					Test existing = null;
					for (Test t : orchestrator.getTests()) {
						if (name.equals(t.getName()) && "Predefined".equals(t.getType())) {
							existing = t;
							break;
						}
					}
					if (existing == null) {
						existing = OrchestrationFactory.eINSTANCE.createTest();
						existing.setName(name);
						existing.setType("Predefined");
						existing.setStatus(TestStatus.PENDING);
						page.addTestToModel(existing);
					}
					final Test finalTest = existing;
					final TableItem item = new TableItem(table, SWT.NONE);
					item.setText(1, finalTest.getName());
					item.setText(2, finalTest.getPath() != null ? finalTest.getPath() : "");
					item.setText(3, finalTest.getStatus().toString());

					TableEditor selEditor = new TableEditor(table);
					editors.add(selEditor);
					final Button radio = new Button(table, SWT.RADIO);
					radio.setSelection(finalTest.isSelected());
					radio.pack();
					selEditor.minimumWidth = radio.getSize().x;
					selEditor.horizontalAlignment = SWT.CENTER;
					selEditor.setEditor(radio, item, 0);
					radio.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							if (radio.getSelection())
								page.handleTestSelection(finalTest);
						}
					});

					TableEditor actionEditor = new TableEditor(table);
					editors.add(actionEditor);
					Composite actionComp = toolkit.createComposite(table);
					GridLayout actionLayout = new GridLayout(2, false);
					actionLayout.marginHeight = 0;
					actionLayout.marginWidth = 0;
					actionComp.setLayout(actionLayout);

					Button editBtn = toolkit.createButton(actionComp, "Edit", SWT.PUSH);
					Button execBtn = toolkit.createButton(actionComp, "Execute", SWT.PUSH);
					execBtn.setEnabled(finalTest.isSelected());
					execBtn.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							page.executeTest(finalTest);
						}
					});
					actionComp.pack();
					actionEditor.minimumWidth = actionComp.getSize().x;
					actionEditor.setEditor(actionComp, item, 4);

					page.registerTestRow(finalTest, execBtn, item);
				}
			}
		} finally {
			table.setRedraw(true);
		}
		tableComposite.layout(true, true);
	}

	private void createControl(Composite parent) {
		group = SWTFactory.createExpandableGroup(toolkit, parent, "Predefined Tests", 1, true, true);

		tableComposite = toolkit.createComposite(group);
		tableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		tableLayout = new TableColumnLayout();
		tableComposite.setLayout(tableLayout);

		table = toolkit.createTable(tableComposite,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData tableGd = new GridData(GridData.FILL_BOTH);
		tableGd.heightHint = 200;
		table.setLayoutData(tableGd);

		addColumn(table, tableLayout, "Sel", 40, 5);
		addColumn(table, tableLayout, "Name", 150, 25);
		addColumn(table, tableLayout, "Path", 250, 40);
		addColumn(table, tableLayout, "Status", 100, 15);
		addColumn(table, tableLayout, "Actions", 150, 15);

		refreshUI();
	}

	private void addColumn(Table table, TableColumnLayout layout, String text, int width, int weight) {
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText(text);
		layout.setColumnData(col, new ColumnWeightData(weight, width, true));
	}
}
