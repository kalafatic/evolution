package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.json.JSONArray;
import org.json.JSONObject;
import eu.kalafatic.evolution.controller.manager.OrchestrationStatusManager;
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
		Job job = new Job("MCP Test Connection: " + url) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				long startTime = System.currentTimeMillis();
				System.out.println("[MCP][START] Test connection URL=" + url);
				String orchId = orchestrator != null ? orchestrator.getName() : "MCP";
				OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.2, "Testing MCP connection to " + url);

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

					long duration = System.currentTimeMillis() - startTime;
					System.out.println("[MCP][END] Test connection success durationMs=" + duration);
					OrchestrationStatusManager.getInstance().updateStatus(orchId, 1.0, "MCP Connection verified");
					return Status.OK_STATUS;
				} catch (Exception ex) {
					long duration = System.currentTimeMillis() - startTime;
					String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
					configGroup.setStatus(false, "Error: " + errorMsg);
					System.out.println("[MCP][END] Test connection failed durationMs=" + duration + " Error=" + errorMsg);
					OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.0, "MCP Connection error: " + errorMsg);
					eu.kalafatic.evolution.controller.log.Log.log(McpSettingsPage.this, ex);
					return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", errorMsg, ex);
				}
			}
		};
		job.schedule();
	}

	public void startDemoServer() {
		Job job = new Job("Start MCP Demo Server") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				long startTime = System.currentTimeMillis();
				System.out.println("[MCP][START] Starting demo server");
				String orchId = orchestrator != null ? orchestrator.getName() : "MCP";
				OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.2, "Starting MCP Demo Server");

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

					long duration = System.currentTimeMillis() - startTime;
					System.out.println("[MCP][END] Demo server started durationMs=" + duration);
					OrchestrationStatusManager.getInstance().updateStatus(orchId, 1.0, "MCP Demo Server running");
					return Status.OK_STATUS;
				} catch (Exception ex) {
					Display.getDefault().asyncExec(() -> {
						if (isDisposed())
							return;
						configGroup.updateDemoStatus();
					});
					long duration = System.currentTimeMillis() - startTime;
					System.out.println("[MCP][END] Demo server failed durationMs=" + duration + " Error=" + ex.getMessage());
					OrchestrationStatusManager.getInstance().updateStatus(orchId, 0.0, "MCP Demo Server start failed");
					eu.kalafatic.evolution.controller.log.Log.log(McpSettingsPage.this, ex);
					return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", ex.getMessage(), ex);
				}
			}
		};
		job.schedule();
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
		Job job = new Job("MCP Request: " + method) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				long startTime = System.currentTimeMillis();
				System.out.println("[MCP][START] Request method=" + method + " URL=" + url);

				try {
					McpClient client = new McpClient(url);
					JSONObject jsonParams = new JSONObject(params);
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

					long duration = System.currentTimeMillis() - startTime;
					System.out.println("[MCP][END] Request success method=" + method + " durationMs=" + duration);
					return Status.OK_STATUS;
				} catch (Exception ex) {
					eu.kalafatic.evolution.controller.log.Log.log(McpSettingsPage.this, ex);
					return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", ex.getMessage(), ex);
				}
			}
		};
		job.schedule();
	}

	public void refreshResources() {
		if (resourcesGroup == null || configGroup == null)
			return;
		String url = configGroup.getUrl();
		if (url.isEmpty())
			return;
		resourcesGroup.clear();
		Job job = new Job("MCP Refresh Resources") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				long startTime = System.currentTimeMillis();
				System.out.println("[MCP][START] Refresh resources URL=" + url);

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

					long duration = System.currentTimeMillis() - startTime;
					System.out.println("[MCP][END] Refresh resources count=" + resources.length() + " durationMs=" + duration);
					return Status.OK_STATUS;
				} catch (Exception ex) {
					Display.getDefault().asyncExec(() -> {
						if (resourcesGroup == null || resourcesGroup.isDisposed())
							return;
						resourcesGroup.getGroup().setBackground(lightRed);
						handleRefreshError("Failed to list resources", ex);
					});
					return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", ex.getMessage(), ex);
				}
			}
		};
		job.schedule();
	}

	public void refreshTools() {
		if (toolsGroup == null || configGroup == null)
			return;
		String url = configGroup.getUrl();
		if (url.isEmpty())
			return;
		toolsGroup.clear();
		Job job = new Job("MCP Refresh Tools") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				long startTime = System.currentTimeMillis();
				System.out.println("[MCP][START] Refresh tools URL=" + url);

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

					long duration = System.currentTimeMillis() - startTime;
					System.out.println("[MCP][END] Refresh tools count=" + tools.length() + " durationMs=" + duration);
					return Status.OK_STATUS;
				} catch (Exception ex) {
					Display.getDefault().asyncExec(() -> {
						if (toolsGroup == null || toolsGroup.isDisposed())
							return;
						toolsGroup.getGroup().setBackground(lightRed);
						handleRefreshError("Failed to list tools", ex);
					});
					return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", ex.getMessage(), ex);
				}
			}
		};
		job.schedule();
	}

	public void refreshPrompts() {
		if (promptsGroup == null || configGroup == null)
			return;
		String url = configGroup.getUrl();
		if (url.isEmpty())
			return;
		promptsGroup.clear();
		Job job = new Job("MCP Refresh Prompts") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				long startTime = System.currentTimeMillis();
				System.out.println("[MCP][START] Refresh prompts URL=" + url);

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

					long duration = System.currentTimeMillis() - startTime;
					System.out.println("[MCP][END] Refresh prompts count=" + prompts.length() + " durationMs=" + duration);
					return Status.OK_STATUS;
				} catch (Exception ex) {
					Display.getDefault().asyncExec(() -> {
						if (promptsGroup == null || promptsGroup.isDisposed())
							return;
						promptsGroup.getGroup().setBackground(lightRed);
						handleRefreshError("Failed to list prompts", ex);
					});
					return new Status(IStatus.ERROR, "eu.kalafatic.evolution.view", ex.getMessage(), ex);
				}
			}
		};
		job.schedule();
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
		resourcesGroup.addItem("Server Info", "server://info", "text/plain", "Information about the MCP server");
		resourcesGroup.addItem("Server Configuration", "server://config", "application/json", "Current server settings");
		resourcesGroup.addItem("Available Connectors", "connectors://list", "application/json", "List of integrated enterprise connectors");
		resourcesGroup.addItem("Documentation README", "docs://README.md", "text/markdown", "Local MCP Demo README Documentation");

		toolsGroup.clear();
		toolsGroup.addItem("ping", "Ping the server to check availability", "{}");
		toolsGroup.addItem("echo", "Echo back the provided message", "{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"}}}");
		toolsGroup.addItem("systemInfo", "Get host system information and metrics", "{}");
		toolsGroup.addItem("dummyAction", "Execute dummy action on enterprise connector", "{\"type\":\"object\",\"properties\":{\"action\":{\"type\":\"string\"}}}");

		promptsGroup.clear();
		promptsGroup.addItem("codeReview", "Generate a code review template", "[]");
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
