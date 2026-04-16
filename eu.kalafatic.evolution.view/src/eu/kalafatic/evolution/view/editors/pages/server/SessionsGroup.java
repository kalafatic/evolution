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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.ServerSession;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.tools.AToolGroup;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SessionsGroup extends AToolGroup {

    private TableViewer viewer;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

    public SessionsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Active Sessions");
        section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        group = toolkit.createComposite(section);
        group.setLayout(new GridLayout(1, false));
        section.setClient(group);

        Table table = toolkit.createTable(group, SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 150;
        table.setLayoutData(gd);

        String[] titles = { "ID", "Type", "Start Time", "Last Activity", "Client IP" };
        for (String title : titles) {
            TableColumn col = new TableColumn(table, SWT.NONE);
            col.setText(title);
            col.setWidth(100);
        }

        viewer = new TableViewer(table);
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new SessionLabelProvider());
        
     
        
        Display.getDefault().asyncExec(() -> {
            if (!viewer.getControl().isDisposed()) {
            	 if (orchestrator == null) return;
                  viewer.setInput(orchestrator.getServerSessions());
            }
        });
    }

    @Override
    protected void refreshUI() {
    	 if (viewer == null || viewer.getControl().isDisposed()) return;
    	    viewer.refresh(); // ✅ safe
    	
    }

    @Override
    protected String getTestStatus() { return null; }
    @Override
    protected void clearTestStatus() {}

    private class SessionLabelProvider extends LabelProvider implements ITableLabelProvider {
        @Override
        public Image getColumnImage(Object element, int columnIndex) { return null; }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            ServerSession s = (ServerSession) element;
            switch (columnIndex) {
                case 0: return s.getId();
                case 1: return s.getType().getName();
                case 2: return sdf.format(new Date(s.getStartTime()));
                case 3: return sdf.format(new Date(s.getLastActivity()));
                case 4: return s.getClientIp();
                default: return "";
            }
        }
    }
}
