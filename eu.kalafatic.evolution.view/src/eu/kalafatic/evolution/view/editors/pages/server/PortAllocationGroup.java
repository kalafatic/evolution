package eu.kalafatic.evolution.view.editors.pages.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import eu.kalafatic.evolution.controller.resource.PortAllocationManager;
import eu.kalafatic.evolution.controller.resource.PortConfiguration;
import eu.kalafatic.evolution.controller.resource.PortRegistry;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * UI Group managing the authoritative EVO service-to-port allocation and protocol status.
 */
public class PortAllocationGroup extends AEvoGroup {

    private TableViewer portTableViewer;

    public static class ServicePortEntry {
        private final String serviceId;
        private final String serviceName;
        private final int port;
        private final int defaultPort;
        private final String protocol;
        private final String host;
        private final boolean running;

        public ServicePortEntry(String serviceId, String serviceName, int port, int defaultPort, String protocol, String host, boolean running) {
            this.serviceId = serviceId;
            this.serviceName = serviceName;
            this.port = port;
            this.defaultPort = defaultPort;
            this.protocol = protocol;
            this.host = host;
            this.running = running;
        }

        public String getServiceId() { return serviceId; }
        public String getServiceName() { return serviceName; }
        public int getPort() { return port; }
        public int getDefaultPort() { return defaultPort; }
        public String getProtocol() { return protocol; }
        public String getHost() { return host; }
        public boolean isRunning() { return running; }
    }

    public PortAllocationGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Port Allocations & Protocol Registry");
        section.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        group = toolkit.createComposite(section);
        group.setLayout(new GridLayout(1, false));
        section.setClient(group);

        Composite actionsComp = toolkit.createComposite(group);
        actionsComp.setLayout(new GridLayout(2, false));
        actionsComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Button refreshBtn = GUIFactory.INSTANCE.createButton(actionsComp, "Refresh Allocations");
        refreshBtn.addListener(SWT.Selection, e -> refreshUI());

        portTableViewer = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = portTableViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableGd.heightHint = 180;
        table.setLayoutData(tableGd);

        createColumn("Service ID", 130, ServicePortEntry::getServiceId);
        createColumn("Service Name", 240, ServicePortEntry::getServiceName);
        createColumn("Protocol", 80, ServicePortEntry::getProtocol);
        createColumn("Port", 70, e -> String.valueOf(e.getPort()));
        createColumn("Default", 70, e -> String.valueOf(e.getDefaultPort()));
        createColumn("Address", 130, e -> e.getHost() + ":" + e.getPort());
        createColumn("Status", 90, e -> e.isRunning() ? "LISTENING" : "STOPPED");

        portTableViewer.setContentProvider(ArrayContentProvider.getInstance());
        refreshUI();
    }

    private void createColumn(String title, int width, java.util.function.Function<ServicePortEntry, String> mapper) {
        TableViewerColumn col = new TableViewerColumn(portTableViewer, SWT.NONE);
        col.getColumn().setText(title);
        col.getColumn().setWidth(width);
        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return mapper.apply((ServicePortEntry) element);
            }
            @Override
            public Color getForeground(Object element) {
                if ("Status".equals(title)) {
                    ServicePortEntry e = (ServicePortEntry) element;
                    return e.isRunning() ? group.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN) : group.getDisplay().getSystemColor(SWT.COLOR_RED);
                }
                return super.getForeground(element);
            }
        });
    }

    @Override
    protected void refreshUI() {
        if (portTableViewer != null && !portTableViewer.getControl().isDisposed()) {
            PortAllocationManager pam = PortAllocationManager.getInstance();
            ServerManager sm = ServerManager.getInstance();
            Map<Integer, Boolean> statuses = sm.getServerStatuses();

            List<ServicePortEntry> entries = new ArrayList<>();
            for (PortRegistry reg : PortRegistry.values()) {
                PortConfiguration cfg = pam.getConfiguration(reg.getServiceId());
                int assignedPort = (cfg != null) ? cfg.getAssignedPort() : reg.getDefaultPort();
                boolean isListening = statuses.getOrDefault(assignedPort, sm.isRunning(assignedPort));
                entries.add(new ServicePortEntry(
                        reg.getServiceId(),
                        reg.getServiceName(),
                        assignedPort,
                        reg.getDefaultPort(),
                        reg.getProtocol().toUpperCase(),
                        reg.getDefaultHost(),
                        isListening
                ));
            }
            portTableViewer.setInput(entries);
        }
    }
}
