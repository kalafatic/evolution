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
		if (url.isEmpty()) {
			eu.kalafatic.evolution.controller.log.Log.log("MCP Server URL cannot be empty.");
			return;
		}
		new Thread(() -> {
			try {
				McpClient client = new McpClient(url);
				String response = client.initialize();
				configGroup.setStatus(true, "Connected");

				String additionalInfo = "";
				if (url.contains("38080")) {
					try {
						String docContent = client.readResource("docs://README.md");
						if (docContent != null && !docContent.isEmpty()) {
							additionalInfo = "\n\nDemo Resource (README.md):\n"
									+ (docContent.length() > 200 ? docContent.substring(0, 200) + "..." : docContent);
						}
					} catch (Exception e) {
						additionalInfo = "\n\nCould not read demo resource: " + e.getMessage();
					}
				}

				String finalAdditionalInfo = additionalInfo;
				Display.getDefault().asyncExec(() -> {
					if (isDisposed())
						return;
					MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
					mb.setText("Success");
					mb.setMessage("Connected to MCP server successfully.\n" + response + finalAdditionalInfo);
					mb.open();
				});
			} catch (Exception ex) {
				String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
				configGroup.setStatus(false, "Error: " + errorMsg);
				eu.kalafatic.evolution.controller.log.Log.log(this, ex);
			}
		}).start();
	}

	public void startDemoServer() {
		new Thread(() -> {
			try {
				eu.kalafatic.evolution.controller.orchestration.mcp.McpDemoServerManager.getInstance().start();
				Display.getDefault().asyncExec(() -> {
					if (isDisposed())
						return;
					configGroup.updateDemoStatus();
					refreshUI();
					MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
					mb.setText("Success");
					mb.setMessage("MCP Demo Documentation Server started on port 38080.");
					mb.open();
				});
			} catch (Exception ex) {
				Display.getDefault().asyncExec(() -> {
					if (isDisposed())
						return;
					configGroup.updateDemoStatus();
				});
				eu.kalafatic.evolution.controller.log.Log.log(this, ex);
			}
		}).start();
	}

	public void openRequestDialog(String url) {
		if (url.isEmpty()) {
			eu.kalafatic.evolution.controller.log.Log.log("MCP Server URL cannot be empty.");
			return;
		}

		String defaultMethod = "ping";
		String defaultParams = "{}";

		if (url.contains("38080")) {
			defaultMethod = "resources/read";
			defaultParams = "{\"uri\": \"docs://README.md\"}";
		}

		McpRequestDialog dialog = new McpRequestDialog(getShell(), defaultMethod, defaultParams);
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
				// We need a way to send generic request in McpClient or use the existing ones
				// if they match
				// For simplicity, let's assume we can use a generic method if we add it to
				// McpClient
				// But since I don't want to change McpClient too much, I'll use reflection or
				// just call the right one
				String response = client.sendGenericRequest(method, jsonParams);

				String finalResponse = response;
				Display.getDefault().asyncExec(() -> {
					if (isDisposed())
						return;
					MessageBox mb = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
					mb.setText("Request Success");
					mb.setMessage("Method: " + method + "\nResponse:\n" + finalResponse);
					mb.open();
				});
			} catch (Exception ex) {
				eu.kalafatic.evolution.controller.log.Log.log(this, ex);
			}
		}).start();
	}

	public void refreshResources() {
		if (resourcesGroup == null || configGroup == null)
			return;
		String url = configGroup.getUrl();
		if (url.isEmpty())
			return;
		resourcesGroup.clear();
		new Thread(() -> {
			try {
				McpClient client = new McpClient(url);
				client.initialize();
				String resourcesJson = client.listResources();
				JSONArray resources = new JSONArray(resourcesJson);
				Display.getDefault().asyncExec(() -> {
					if (resourcesGroup == null || resourcesGroup.isDisposed())
						return;
					resourcesGroup.getGroup().setBackground(null);
					for (int i = 0; i < resources.length(); i++) {
						JSONObject res = resources.getJSONObject(i);
						resourcesGroup.addItem(res.optString("name", "N/A"), res.optString("uri", "N/A"),
								res.optString("mimeType", "N/A"), res.optString("description", ""));
					}
				});
			} catch (Exception ex) {
				Display.getDefault().asyncExec(() -> {
					if (resourcesGroup == null || resourcesGroup.isDisposed())
						return;
					resourcesGroup.getGroup().setBackground(lightRed);
					handleRefreshError("Failed to list resources", ex);
				});
			}
		}).start();
	}

	public void refreshTools() {
		if (toolsGroup == null || configGroup == null)
			return;
		String url = configGroup.getUrl();
		if (url.isEmpty())
			return;
		toolsGroup.clear();
		new Thread(() -> {
			try {
				McpClient client = new McpClient(url);
				client.initialize();
				String toolsJson = client.listTools();
				JSONArray tools = new JSONArray(toolsJson);
				Display.getDefault().asyncExec(() -> {
					if (toolsGroup == null || toolsGroup.isDisposed())
						return;
					toolsGroup.getGroup().setBackground(null);
					for (int i = 0; i < tools.length(); i++) {
						JSONObject tool = tools.getJSONObject(i);
						toolsGroup.addItem(tool.optString("name", "N/A"), tool.optString("description", ""),
								tool.optJSONObject("inputSchema") != null ? tool.optJSONObject("inputSchema").toString()
										: "{}");
					}
				});
			} catch (Exception ex) {
				Display.getDefault().asyncExec(() -> {
					if (toolsGroup == null || toolsGroup.isDisposed())
						return;
					toolsGroup.getGroup().setBackground(lightRed);
					handleRefreshError("Failed to list tools", ex);
				});
			}
		}).start();
	}

	public void refreshPrompts() {
		if (promptsGroup == null || configGroup == null)
			return;
		String url = configGroup.getUrl();
		if (url.isEmpty())
			return;
		promptsGroup.clear();
		new Thread(() -> {
			try {
				McpClient client = new McpClient(url);
				client.initialize();
				String promptsJson = client.listPrompts();
				JSONArray prompts = new JSONArray(promptsJson);
				Display.getDefault().asyncExec(() -> {
					if (promptsGroup == null || promptsGroup.isDisposed())
						return;
					promptsGroup.getGroup().setBackground(null);
					for (int i = 0; i < prompts.length(); i++) {
						JSONObject prompt = prompts.getJSONObject(i);
						promptsGroup.addItem(prompt.optString("name", "N/A"), prompt.optString("description", ""),
								prompt.optJSONArray("arguments") != null ? prompt.optJSONArray("arguments").toString()
										: "[]");
					}
				});
			} catch (Exception ex) {
				Display.getDefault().asyncExec(() -> {
					if (promptsGroup == null || promptsGroup.isDisposed())
						return;
					promptsGroup.getGroup().setBackground(lightRed);
					handleRefreshError("Failed to list prompts", ex);
				});
			}
		}).start();
	}

	private void handleRefreshError(String prefix, Exception ex) {
		if (isDisposed())
			return;
		String message = prefix + ": " + (ex.getMessage() != null ? ex.getMessage() : ex.toString());
		if (ex instanceof java.net.ConnectException || ex.getCause() instanceof java.net.ConnectException) {
			message = prefix + ": Connection refused. Is the MCP server running at " + configGroup.getUrl() + "?";
			eu.kalafatic.evolution.controller.log.Log.log(message);
		} else {
			eu.kalafatic.evolution.controller.log.Log.log(this, ex);
			eu.kalafatic.evolution.controller.log.Log.log(message);
		}
	}

	@Override
	public void refreshUI() {
		if (orchestrator == null || isUpdating)
			return;
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

	public void updateMcpInfo() {
		scheduleRefresh();
	}

	@Override
	public void setOrchestrator(Orchestrator orchestrator) {
		super.setOrchestrator(orchestrator);
		if (configGroup != null)
			configGroup.setOrchestrator(orchestrator);
	}

	public void setDirty(boolean dirty) {
		editor.setDirty(dirty);
	}
}
