package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.mcp.McpClient;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class McpSettingsPage extends ScrolledComposite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private Text mcpUrlText;
    private Table resourcesTable;
    private boolean isUpdating = false;
    private SashForm sashForm;

    public McpSettingsPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        createControl();
    }

    private void createControl() {
        Composite comp = new Composite(this, SWT.NONE);
        comp.setLayout(new GridLayout(1, false));

        sashForm = new SashForm(comp, SWT.VERTICAL);
        sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

        Group configGroup = SWTFactory.createMaximizableGroup(sashForm, "MCP Configuration", 3);

        SWTFactory.createLabel(configGroup, "Server URL:");
        mcpUrlText = new Text(configGroup, SWT.BORDER);
        mcpUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        mcpUrlText.addModifyListener(e -> {
            if (orchestrator != null && !isUpdating) {
                orchestrator.setMcpServerUrl(mcpUrlText.getText());
                editor.setDirty(true);
            }
        });

        Button testBtn = new Button(configGroup, SWT.PUSH);
        testBtn.setText("Test Connection");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                testConnection();
            }
        });

        Group resourcesGroup = SWTFactory.createMaximizableGroup(sashForm, "Available Resources", 1);

        resourcesTable = new Table(resourcesGroup, SWT.BORDER | SWT.FULL_SELECTION);
        resourcesTable.setHeaderVisible(true);
        resourcesTable.setLinesVisible(true);
        resourcesTable.setLayoutData(new GridData(GridData.FILL_BOTH));

        String[] headers = { "Name", "URI", "Description" };
        int[] widths = { 150, 250, 300 };
        for (int i = 0; i < headers.length; i++) {
            TableColumn col = new TableColumn(resourcesTable, SWT.NONE);
            col.setText(headers[i]);
            col.setWidth(widths[i]);
        }

        Button refreshBtn = new Button(resourcesGroup, SWT.PUSH);
        refreshBtn.setText("Refresh Resources");
        refreshBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                refreshResources();
            }
        });

        sashForm.setWeights(new int[] { 30, 70 });

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        updateMcpInfo();
    }

    private void testConnection() {
        String url = mcpUrlText.getText();
        if (url.isEmpty()) {
            MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
            mb.setText("Error");
            mb.setMessage("MCP Server URL cannot be empty.");
            mb.open();
            return;
        }

        new Thread(() -> {
            try {
                McpClient client = new McpClient(url);
                String response = client.initialize();
                Display.getDefault().asyncExec(() -> {
                    if (isDisposed()) return;
                    MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                    mb.setText("Success");
                    mb.setMessage("Connected to MCP server successfully.\n" + response);
                    mb.open();
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (isDisposed()) return;
                    MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                    mb.setText("Connection Failed");
                    mb.setMessage("Error connecting to MCP server: " + ex.getMessage());
                    mb.open();
                });
            }
        }).start();
    }

    private void refreshResources() {
        String url = mcpUrlText.getText();
        if (url.isEmpty()) return;

        resourcesTable.removeAll();
        new Thread(() -> {
            try {
                McpClient client = new McpClient(url);
                String resourcesJson = client.listResources();
                JSONArray resources = new JSONArray(resourcesJson);
                Display.getDefault().asyncExec(() -> {
                    if (resourcesTable.isDisposed()) return;
                    for (int i = 0; i < resources.length(); i++) {
                        JSONObject res = resources.getJSONObject(i);
                        TableItem item = new TableItem(resourcesTable, SWT.NONE);
                        item.setText(0, res.optString("name", "N/A"));
                        item.setText(1, res.optString("uri", "N/A"));
                        item.setText(2, res.optString("description", ""));
                    }
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (isDisposed()) return;
                    MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                    mb.setText("Error");
                    mb.setMessage("Failed to list resources: " + ex.getMessage());
                    mb.open();
                });
            }
        }).start();
    }

    public void updateMcpInfo() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;
        mcpUrlText.setText(orchestrator.getMcpServerUrl() != null ? orchestrator.getMcpServerUrl() : "");
        isUpdating = false;
        refreshResources();
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
        updateMcpInfo();
    }
}
