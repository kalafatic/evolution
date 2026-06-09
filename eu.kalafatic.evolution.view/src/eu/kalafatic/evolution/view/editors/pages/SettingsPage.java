package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;

import eu.kalafatic.evolution.controller.discovery.NetworkDiscoveryService;
import eu.kalafatic.evolution.model.orchestration.NetworkEntry;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * Page for system settings and network discovery.
 * @evo:21:A reason=settings-page
 */
public class SettingsPage extends AEvoPage {

    private TableViewer networkViewer;
    private NetworkDiscoveryService discoveryService = new NetworkDiscoveryService();

    public SettingsPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        this.setLayout(new GridLayout(1, false));

        createNetworkGroup();
    }

    private void createNetworkGroup() {
        Group group = new Group(this, SWT.NONE);
        group.setText("Network & Addresses");
        group.setLayout(new GridLayout(1, false));
        group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite actions = new Composite(group, SWT.NONE);
        actions.setLayout(new org.eclipse.swt.layout.RowLayout());

        Button scanBtn = new Button(actions, SWT.PUSH);
        scanBtn.setText("Scan Codebase for Addresses");
        scanBtn.addListener(SWT.Selection, e -> handleScan());

        networkViewer = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        Table table = networkViewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        createColumn("Address", 250, NetworkEntry::getAddress);
        createColumn("Host", 150, NetworkEntry::getHost);
        createColumn("Port", 80, ent -> String.valueOf(ent.getPort()));
        createColumn("Type", 100, NetworkEntry::getType);
        createColumn("Note", 300, NetworkEntry::getNote);

        networkViewer.setContentProvider(ArrayContentProvider.getInstance());
        refreshUI();
    }

    private void createColumn(String title, int width, java.util.function.Function<NetworkEntry, String> mapper) {
        TableViewerColumn col = new TableViewerColumn(networkViewer, SWT.NONE);
        col.getColumn().setText(title);
        col.getColumn().setWidth(width);
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
                    org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> refreshUI());
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
                networkViewer.setInput(orchestrator.getNetworkEntries().toArray());
            } else {
                networkViewer.setInput(new Object[0]);
            }
        }
    }
}
