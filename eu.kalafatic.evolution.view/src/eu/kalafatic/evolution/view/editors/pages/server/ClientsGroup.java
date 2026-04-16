package eu.kalafatic.evolution.view.editors.pages.server;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.ServerSession;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.tools.AToolGroup;

import java.util.HashSet;
import java.util.Set;

public class ClientsGroup extends AToolGroup {

    private TableViewer viewer;

    public ClientsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Connected Clients");
        section.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        group = toolkit.createComposite(section);
        group.setLayout(new GridLayout(1, false));
        section.setClient(group);

        Table table = toolkit.createTable(group, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.heightHint = 100;
        table.setLayoutData(gd);

        TableColumn col = new TableColumn(table, SWT.NONE);
        col.setText("Client IP");
        col.setWidth(200);

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new ClientLabelProvider());
    }

    @Override
    protected void refreshUI() {
        if (orchestrator == null) return;
        Set<String> ips = new HashSet<>();
        for (ServerSession s : orchestrator.getServerSessions()) {
            ips.add(s.getClientIp());
        }
        viewer.setInput(ips.toArray());
    }

    @Override
    protected String getTestStatus() { return null; }
    @Override
    protected void clearTestStatus() {}

    private class ClientLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(Object element, int columnIndex) { return null; }
        @Override
        public String getColumnText(Object element, int columnIndex) {
            return (String) element;
        }
    }
}
