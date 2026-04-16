package eu.kalafatic.evolution.view.editors.pages.server;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.kalafatic.evolution.model.orchestration.MonitoringData;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.tools.AToolGroup;

import java.util.List;

public class ResourcesGroup extends AToolGroup {

    private Label cpuLabel;
    private Label memLabel;

    public ResourcesGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor) {
        super(editor, orchestrator, successColor);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Server Resources");
        section.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        group = toolkit.createComposite(section);
        group.setLayout(new GridLayout(2, false));
        section.setClient(group);

        toolkit.createLabel(group, "CPU Usage:");
        cpuLabel = toolkit.createLabel(group, "N/A");
        cpuLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        toolkit.createLabel(group, "Memory Usage:");
        memLabel = toolkit.createLabel(group, "N/A");
        memLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    @Override
    protected void refreshUI() {
        if (orchestrator == null) return;
        List<MonitoringData> history = orchestrator.getMonitoringHistory();
        if (!history.isEmpty()) {
            MonitoringData last = history.get(history.size() - 1);
            cpuLabel.setText(String.format("%.2f %%", last.getCpuUsage() * 100));
            memLabel.setText(String.format("%d MB / %d MB", last.getMemoryUsage() / 1024 / 1024, last.getTotalMemory() / 1024 / 1024));
        }
    }

    @Override
    protected String getTestStatus() { return null; }
    @Override
    protected void clearTestStatus() {}
}
