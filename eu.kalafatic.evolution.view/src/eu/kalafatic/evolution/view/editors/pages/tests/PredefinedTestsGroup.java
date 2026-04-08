package eu.kalafatic.evolution.view.editors.pages.tests;

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
import eu.kalafatic.evolution.view.editors.pages.TestsPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class PredefinedTestsGroup {
	private Composite group;
	private Orchestrator orchestrator;
	private TestsPage page;
	private FormToolkit toolkit;

	public PredefinedTestsGroup(FormToolkit toolkit, Composite parent, Orchestrator orchestrator, TestsPage page) {
		this.toolkit = toolkit;
		this.orchestrator = orchestrator;
		this.page = page;
		createControl(parent);
	}

	private void createControl(Composite parent) {
		group = SWTFactory.createExpandableGroup(parent, "Predefined Tests", 1, true);

		Composite tableComposite = toolkit.createComposite(group);
		tableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		TableColumnLayout layout = new TableColumnLayout();
		tableComposite.setLayout(layout);

		final Table table = toolkit.createTable(tableComposite,
				SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		addColumn(table, layout, "Sel", 40, 5);
		addColumn(table, layout, "Name", 150, 25);
		addColumn(table, layout, "Path", 250, 40);
		addColumn(table, layout, "Status", 100, 15);
		addColumn(table, layout, "Actions", 150, 15);

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
		if (group.getLayoutData() != null && group.getLayoutData() instanceof GridData) {
			((GridData) group.getLayoutData()).heightHint = 250;
		}
	}

	private void addColumn(Table table, TableColumnLayout layout, String text, int width, int weight) {
		TableColumn col = new TableColumn(table, SWT.NONE);
		col.setText(text);
		layout.setColumnData(col, new ColumnWeightData(weight, width, true));
	}
}
