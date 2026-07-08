package eu.kalafatic.evolution.view.editors.pages.settings;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.controller.discovery.NetworkDiscoveryService;
import eu.kalafatic.evolution.model.orchestration.NetworkEntry;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.SettingsPage;
import eu.kalafatic.utils.factories.GUIFactory;

public class NetworkSettingsGroup extends AEvoGroup {

    private TableViewer networkViewer;
    private NetworkDiscoveryService discoveryService = new NetworkDiscoveryService();
    @SuppressWarnings("unused")
	private final SettingsPage page;

    public NetworkSettingsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, SettingsPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Network & Addresses", 1, true);

        Composite actions = toolkit.createComposite(group);
        actions.setLayout(new org.eclipse.swt.layout.RowLayout());
        actions.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Button scanBtn = toolkit.createButton(actions, "Scan Codebase for Addresses", SWT.PUSH);
        scanBtn.addListener(SWT.Selection, e -> handleScan());

        networkViewer = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = networkViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        GridData tableGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        tableGd.heightHint = 200;
        table.setLayoutData(tableGd);

        createColumn("Address", 250, NetworkEntry::getAddress);
        createColumn("Host", 150, NetworkEntry::getHost);
        createColumn("Port", 80, ent -> String.valueOf(ent.getPort()));
        createColumn("Domain", 150, NetworkEntry::getPath);
        createColumn("Type", 100, NetworkEntry::getType);
        createColumn("Note", 300, NetworkEntry::getNote);

        networkViewer.setContentProvider(ArrayContentProvider.getInstance());
        refreshUI();
    }

    private void createColumn(String title, int width, java.util.function.Function<NetworkEntry, String> mapper) {
        TableViewerColumn col = new TableViewerColumn(networkViewer, SWT.NONE);
        col.getColumn().setText(title);
        col.getColumn().setWidth(width);

        String tooltip = switch(title) {
            case "Address" -> "The full connection URI or IP address of the remote node.";
            case "Host" -> "The hostname or DNS entry for this network node.";
            case "Port" -> "The TCP port on which the service is listening.";
            case "Domain" -> "The specific URI path or endpoint domain (e.g., /api/v1).";
            case "Type" -> "The protocol or service type (REST, SSH, OLLAMA).";
            case "Note" -> "Additional context or description for this address.";
            default -> null;
        };
        if (tooltip != null) col.getColumn().setToolTipText(tooltip);

        col.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return mapper.apply((NetworkEntry) element);
            }
        });
    }

    private void handleScan() {
        if (orchestrator == null || editor == null) return;

        org.eclipse.ui.IEditorInput input = editor.getEditorInput();
        if (input instanceof org.eclipse.ui.IFileEditorInput) {
            org.eclipse.core.resources.IProject project = ((org.eclipse.ui.IFileEditorInput) input).getFile().getProject();
            java.io.File root = project.getLocation().toFile();

            org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job("Scanning Network Addresses") {
                @Override
                protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                    discoveryService.discoverAndSync(orchestrator, root);
                    Display.getDefault().asyncExec(() -> refreshUI());
                    return org.eclipse.core.runtime.Status.OK_STATUS;
                }
            };
            job.schedule();
        }
    }

    @Override
    protected void refreshUI() {
        if (networkViewer != null && !networkViewer.getControl().isDisposed()) {
            if (orchestrator != null) {
                // Filter out null entries to prevent AssertionFailedException in JFace
                Object[] entries = orchestrator.getNetworkEntries().stream()
                        .filter(java.util.Objects::nonNull)
                        .toArray();
                networkViewer.setInput(entries);
            } else {
                networkViewer.setInput(new Object[0]);
            }
        }
    }
}
