package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.orchestration.mcp.McpClient;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.mcpsettings.*;

public class McpSettingsPage extends SharedScrolledComposite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private boolean isUpdating = false;
    private FormToolkit toolkit;

    private McpConfigGroup configGroup;
    private McpResourcesGroup resourcesGroup;

    public McpSettingsPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        this.toolkit = new FormToolkit(parent.getDisplay());
        createControl();
    }

    private void createControl() {
        Composite comp = toolkit.createComposite(this);
        comp.setLayout(new GridLayout(1, false));
        configGroup = new McpConfigGroup(toolkit, comp, orchestrator, this);
        resourcesGroup = new McpResourcesGroup(toolkit, comp, this);
        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        updateMcpInfo();
    }

    public void testConnection(String url) {
        if (url.isEmpty()) { MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("Error"); mb.setMessage("MCP Server URL cannot be empty."); mb.open(); return; }
        new Thread(() -> {
            try {
                McpClient client = new McpClient(url); String response = client.initialize();
                Display.getDefault().asyncExec(() -> { if (isDisposed()) return; MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK); mb.setText("Success"); mb.setMessage("Connected to MCP server successfully.\n" + response); mb.open(); });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> { if (isDisposed()) return; MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("Connection Failed"); mb.setMessage("Error connecting to MCP server: " + ex.getMessage()); mb.open(); });
            }
        }).start();
    }

    public void refreshResources() {
        String url = configGroup.getUrl(); if (url.isEmpty()) return;
        resourcesGroup.clear();
        new Thread(() -> {
            try {
                McpClient client = new McpClient(url); String resourcesJson = client.listResources(); JSONArray resources = new JSONArray(resourcesJson);
                Display.getDefault().asyncExec(() -> { for (int i = 0; i < resources.length(); i++) { JSONObject res = resources.getJSONObject(i); resourcesGroup.addItem(res.optString("name", "N/A"), res.optString("uri", "N/A"), res.optString("description", "")); } });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> { if (isDisposed()) return; MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("Error"); mb.setMessage("Failed to list resources: " + ex.getMessage()); mb.open(); });
            }
        }).start();
    }

    public void updateMcpInfo() { if (orchestrator == null || isUpdating) return; isUpdating = true; configGroup.updateUI(); isUpdating = false; refreshResources(); }

    public void setOrchestrator(Orchestrator orchestrator) { this.orchestrator = orchestrator; updateMcpInfo(); }

    public void setDirty(boolean dirty) { editor.setDirty(dirty); }

    @Override
    public void dispose() { if (toolkit != null) toolkit.dispose(); super.dispose(); }
}
