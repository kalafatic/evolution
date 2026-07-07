package eu.kalafatic.evolution.view.editors.pages.mcpsettings;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.McpSettingsPage;
import eu.kalafatic.utils.factories.GUIFactory;

public class McpConfigGroup extends AEvoGroup {
    private Text mcpUrlText;
    private Button mcpEnabledBtn;
    private Text mcpPortText;
    private org.eclipse.swt.widgets.Label statusLabel;
    private org.eclipse.swt.widgets.Label demoStatusLabel;
    private McpSettingsPage page;

    public McpConfigGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, McpSettingsPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "MCP Configuration", 2, true);

        mcpEnabledBtn = toolkit.createButton(group, "Enable Local MCP Server", org.eclipse.swt.SWT.CHECK);
        mcpEnabledBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (orchestrator != null && orchestrator.getServerSettings() != null) {
                    orchestrator.getServerSettings().setMcpEnabled(mcpEnabledBtn.getSelection());
                    page.setDirty(true);
                }
            }
        });
        GUIFactory.INSTANCE.createLabel(group, "");

        org.eclipse.swt.widgets.Label portLabel = GUIFactory.INSTANCE.createLabel(group, "Local MCP Port:");
        portLabel.setToolTipText("The port on which the local Evolution MCP server runs (default: 58080).");
        mcpPortText = GUIFactory.INSTANCE.createText(group);
        mcpPortText.addModifyListener(e -> {
            if (orchestrator != null && orchestrator.getServerSettings() != null) {
                try {
                    orchestrator.getServerSettings().setMcpPort(Integer.parseInt(mcpPortText.getText()));
                    page.setDirty(true);
                } catch (NumberFormatException ex) {}
            }
        });

        org.eclipse.swt.widgets.Label urlLabel = GUIFactory.INSTANCE.createLabel(group, "Remote MCP Server URL:");
        urlLabel.setToolTipText("The URL of the MCP server to connect to (e.g., http://localhost:38080/mcp).");
        mcpUrlText = GUIFactory.INSTANCE.createText(group);
        mcpUrlText.addModifyListener(e -> {
            if (orchestrator != null) {
                orchestrator.setMcpServerUrl(mcpUrlText.getText());
                page.setDirty(true);
            }
        });

        Button testBtn = GUIFactory.INSTANCE.createButton(group, "Test Connection", 150);
        testBtn.setToolTipText("Initialize connection and verify protocol compatibility with the MCP server.");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.testConnection(mcpUrlText.getText());
            }
        });

        Button requestBtn = GUIFactory.INSTANCE.createButton(group, "Test Request", 150);
        requestBtn.setToolTipText("Send a custom JSON-RPC request to the MCP server.");
        requestBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.openRequestDialog(mcpUrlText.getText());
            }
        });

        Button loadDemoBtn = GUIFactory.INSTANCE.createButton(group, "Load Demo Settings", 150);
        loadDemoBtn.setToolTipText("Set the URL to the local demo documentation MCP server (localhost:38080).");
        loadDemoBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                mcpUrlText.setText("http://localhost:38080/mcp");
                if (orchestrator != null) {
                    orchestrator.setMcpServerUrl(mcpUrlText.getText());
                    page.setDirty(true);
                }
            }
        });

        Button startDemoBtn = GUIFactory.INSTANCE.createButton(group, "START SERVER", 150);
        startDemoBtn.setToolTipText("Start the local MCP Demo Documentation Server on port 38080.");
        startDemoBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.startDemoServer();
            }
        });

        Button refreshAllBtn = GUIFactory.INSTANCE.createButton(group, "Connect & Refresh All", 180);
        refreshAllBtn.setToolTipText("Initialize connection and refresh all tools, resources, and prompts.");
        refreshAllBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.testConnection(mcpUrlText.getText());
                page.refreshResources();
                page.refreshTools();
                page.refreshPrompts();
            }
        });

        GUIFactory.INSTANCE.createLabel(group, "Status:");
        statusLabel = GUIFactory.INSTANCE.createLabel(group, "Unknown");

        GUIFactory.INSTANCE.createLabel(group, "Demo Server:");
        demoStatusLabel = GUIFactory.INSTANCE.createLabel(group, "Stopped");
    }

    public void setStatus(boolean success, String message) {
        if (group == null || group.isDisposed()) return;
        org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> {
            if (group.isDisposed()) return;
            statusLabel.setText(message != null ? message : (success ? "Connected" : "Disconnected"));
            group.setBackground(success ? lightGreen : lightRed);
            // Also set background for child labels/controls if needed, but group background usually propagates
        });
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            setTextSafe(mcpUrlText, orchestrator.getMcpServerUrl());
            if (orchestrator.getServerSettings() != null) {
                mcpEnabledBtn.setSelection(orchestrator.getServerSettings().isMcpEnabled());
                setTextSafe(mcpPortText, String.valueOf(orchestrator.getServerSettings().getMcpPort()));
            }
        }
        updateDemoStatus();
    }

    public void updateDemoStatus() {
        if (demoStatusLabel == null || demoStatusLabel.isDisposed()) return;
        boolean running = eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().isRunning();
        demoStatusLabel.setText(running ? "Running" : "Stopped");
        demoStatusLabel.setForeground(running ? lightGreen : lightRed);
    }

    public String getUrl() {
        return mcpUrlText.getText();
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { mcpUrlText };
    }
}
