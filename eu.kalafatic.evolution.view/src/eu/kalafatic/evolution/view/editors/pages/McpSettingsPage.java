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

public class McpSettingsPage extends AEvoPage {

    private boolean isUpdating = false;

    private McpConfigGroup configGroup;
    private McpServersGroup serversGroup;
    private McpResourcesGroup resourcesGroup;
    private McpToolsGroup toolsGroup;
    private McpPromptsGroup promptsGroup;

    public McpSettingsPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        createControl();
    }

    private void createControl() {
        Composite comp = toolkit.createComposite(this);
        comp.setLayout(new GridLayout(1, false));
        configGroup = new McpConfigGroup(toolkit, comp, editor, orchestrator, this);
        serversGroup = new McpServersGroup(toolkit, comp, editor, orchestrator, this);
        resourcesGroup = new McpResourcesGroup(toolkit, comp, editor, orchestrator, this);
        toolsGroup = new McpToolsGroup(toolkit, comp, editor, orchestrator, this);
        promptsGroup = new McpPromptsGroup(toolkit, comp, editor, orchestrator, this);
        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        updateMcpInfo();
    }

    public void testConnection(String url) {
        if (url.isEmpty()) { MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("Error"); mb.setMessage("MCP Server URL cannot be empty."); mb.open(); return; }
        new Thread(() -> {
            try {
                McpClient client = new McpClient(url); String response = client.initialize();
                configGroup.setStatus(true, "Connected");
                Display.getDefault().asyncExec(() -> { if (isDisposed()) return; MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK); mb.setText("Success"); mb.setMessage("Connected to MCP server successfully.\n" + response); mb.open(); });
            } catch (Exception ex) {
                configGroup.setStatus(false, "Error: " + ex.getMessage());
                Display.getDefault().asyncExec(() -> { if (isDisposed()) return; MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("Connection Failed"); mb.setMessage("Error connecting to MCP server: " + ex.getMessage()); mb.open(); });
            }
        }).start();
    }

    public void startDemoServer() {
        new Thread(() -> {
            try {
                eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().start();
                Display.getDefault().asyncExec(() -> {
                    if (isDisposed()) return;
                    configGroup.updateDemoStatus();
                    MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                    mb.setText("Success");
                    mb.setMessage("MCP Demo Documentation Server started on port 38080.");
                    mb.open();
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (isDisposed()) return;
                    configGroup.updateDemoStatus();
                    MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                    mb.setText("Error");
                    mb.setMessage("Failed to start MCP Demo Server: " + ex.getMessage());
                    mb.open();
                });
            }
        }).start();
    }

    public void openRequestDialog(String url) {
        if (url.isEmpty()) { MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("Error"); mb.setMessage("MCP Server URL cannot be empty."); mb.open(); return; }
        McpRequestDialog dialog = new McpRequestDialog(getShell());
        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            String method = dialog.getMethod();
            String params = dialog.getParams();
            sendCustomRequest(url, method, params);
        }
    }

    private void sendCustomRequest(String url, String method, String params) {
        new Thread(() -> {
            try {
                McpClient client = new McpClient(url);
                JSONObject jsonParams = new JSONObject(params);
                // We need a way to send generic request in McpClient or use the existing ones if they match
                // For simplicity, let's assume we can use a generic method if we add it to McpClient
                // But since I don't want to change McpClient too much, I'll use reflection or just call the right one
                String response = client.sendGenericRequest(method, jsonParams);

                String finalResponse = response;
                Display.getDefault().asyncExec(() -> {
                    if (isDisposed()) return;
                    MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                    mb.setText("Request Success");
                    mb.setMessage("Method: " + method + "\nResponse:\n" + finalResponse);
                    mb.open();
                });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> {
                    if (isDisposed()) return;
                    MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
                    mb.setText("Request Failed");
                    mb.setMessage("Error sending request: " + ex.getMessage());
                    mb.open();
                });
            }
        }).start();
    }

    public void refreshResources() {
        String url = configGroup.getUrl(); if (url.isEmpty()) return;
        resourcesGroup.clear();
        new Thread(() -> {
            try {
                McpClient client = new McpClient(url); String resourcesJson = client.listResources(); JSONArray resources = new JSONArray(resourcesJson);
                Display.getDefault().asyncExec(() -> { if (resourcesGroup.isDisposed()) return; resourcesGroup.getGroup().setBackground(null); for (int i = 0; i < resources.length(); i++) { JSONObject res = resources.getJSONObject(i); resourcesGroup.addItem(res.optString("name", "N/A"), res.optString("uri", "N/A"), res.optString("mimeType", "N/A"), res.optString("description", "")); } });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> { if (resourcesGroup.isDisposed()) return; resourcesGroup.getGroup().setBackground(lightRed); if (isDisposed()) return; MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("Error"); mb.setMessage("Failed to list resources: " + ex.getMessage()); mb.open(); });
            }
        }).start();
    }

    public void refreshTools() {
        String url = configGroup.getUrl(); if (url.isEmpty()) return;
        toolsGroup.clear();
        new Thread(() -> {
            try {
                McpClient client = new McpClient(url); String toolsJson = client.listTools(); JSONArray tools = new JSONArray(toolsJson);
                Display.getDefault().asyncExec(() -> { if (toolsGroup.isDisposed()) return; toolsGroup.getGroup().setBackground(null); for (int i = 0; i < tools.length(); i++) { JSONObject tool = tools.getJSONObject(i); toolsGroup.addItem(tool.optString("name", "N/A"), tool.optString("description", ""), tool.optJSONObject("inputSchema") != null ? tool.optJSONObject("inputSchema").toString() : "{}"); } });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> { if (toolsGroup.isDisposed()) return; toolsGroup.getGroup().setBackground(lightRed); if (isDisposed()) return; MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("Error"); mb.setMessage("Failed to list tools: " + ex.getMessage()); mb.open(); });
            }
        }).start();
    }

    public void refreshPrompts() {
        String url = configGroup.getUrl(); if (url.isEmpty()) return;
        promptsGroup.clear();
        new Thread(() -> {
            try {
                McpClient client = new McpClient(url); String promptsJson = client.listPrompts(); JSONArray prompts = new JSONArray(promptsJson);
                Display.getDefault().asyncExec(() -> { if (promptsGroup.isDisposed()) return; promptsGroup.getGroup().setBackground(null); for (int i = 0; i < prompts.length(); i++) { JSONObject prompt = prompts.getJSONObject(i); promptsGroup.addItem(prompt.optString("name", "N/A"), prompt.optString("description", ""), prompt.optJSONArray("arguments") != null ? prompt.optJSONArray("arguments").toString() : "[]"); } });
            } catch (Exception ex) {
                Display.getDefault().asyncExec(() -> { if (promptsGroup.isDisposed()) return; promptsGroup.getGroup().setBackground(lightRed); if (isDisposed()) return; MessageBox mb = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK); mb.setText("Error"); mb.setMessage("Failed to list prompts: " + ex.getMessage()); mb.open(); });
            }
        }).start();
    }

    @Override
    public void refreshUI() {
        if (orchestrator == null || isUpdating) return;
        isUpdating = true;

        String url = orchestrator.getMcpServerUrl();
        if (url == null || url.isEmpty()) {
            orchestrator.setMcpServerUrl("http://localhost:38080/mcp");
            setDirty(true);
        }

        configGroup.updateUI();
        serversGroup.updateUI();
        isUpdating = false;

        url = orchestrator.getMcpServerUrl();
        if (url == null || url.isEmpty()) {
            loadMockData();
        } else {
            refreshResources();
            refreshTools();
            refreshPrompts();
        }
    }

    private void loadMockData() {
        resourcesGroup.clear();
        resourcesGroup.addItem("Mock Resource", "mock://test", "text/plain", "Test resource for UI validation");

        toolsGroup.clear();
        toolsGroup.addItem("mockTool", "A tool that does nothing", "{\"type\":\"object\"}");

        promptsGroup.clear();
        promptsGroup.addItem("mockPrompt", "A prompt for testing", "[]");
    }

    public void updateMcpInfo() { scheduleRefresh(); }

    @Override
    public void setOrchestrator(Orchestrator orchestrator) {
        super.setOrchestrator(orchestrator);
        if (configGroup != null) configGroup.setOrchestrator(orchestrator);
    }

    public void setDirty(boolean dirty) { editor.setDirty(dirty); }
}
