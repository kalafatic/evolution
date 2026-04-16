package eu.kalafatic.evolution.view.editors.pages.server;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.LightweightSystem;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.kalafatic.evolution.model.orchestration.MonitoringData;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.tools.AToolGroup;

import java.util.List;

public class MonitoringGroup extends AToolGroup {

    private Canvas canvas;

    public MonitoringGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Resource Monitoring");
        section.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        group = toolkit.createComposite(section);
        group.setLayout(new GridLayout(1, false));
        section.setClient(group);

        canvas = new Canvas(group, SWT.DOUBLE_BUFFERED);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 200;
        canvas.setLayoutData(gd);

        canvas.addPaintListener(e -> {
            if (orchestrator == null) return;
            List<MonitoringData> history = orchestrator.getMonitoringHistory();
            if (history.size() < 2) return;

            int w = canvas.getSize().x;
            int h = canvas.getSize().y;

            e.gc.setForeground(canvas.getDisplay().getSystemColor(SWT.COLOR_BLUE));
            int lastX = 0;
            int lastY = h;

            for (int i = 0; i < history.size(); i++) {
                int x = (i * w) / (history.size() - 1);
                int y = h - (int)(history.get(i).getCpuUsage() * h);
                if (i > 0) {
                    e.gc.drawLine(lastX, lastY, x, y);
                }
                lastX = x;
                lastY = y;
            }
        });
    }

    @Override
    protected void refreshUI() {
        if (canvas != null && !canvas.isDisposed()) {
            canvas.redraw();
        }
    }

    @Override
    protected String getTestStatus() { return null; }
    @Override
    protected void clearTestStatus() {}
}
