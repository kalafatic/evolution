package eu.kalafatic.evolution.view.editors.pages.mcpsettings;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.teip.dto.TeipExecutionStatusResponse;
import eu.kalafatic.evolution.controller.teip.mcp.TrystMcpClient;
import eu.kalafatic.evolution.controller.teip.service.TeipExecutionManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.McpSettingsPage;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * MVC UI Group providing TRYST–EVOLUTION Integration Protocol (TEIP 1.0)
 * connection management, MCP discovery, and remote execution monitoring.
 */
public class TrystIntegrationGroup extends AEvoGroup {

	private Text trystUrlText;
	private Text authTokenText;
	private Button enableTeipCheck;
	private Label statusLabel;
	private Table executionsTable;
	private Text detailsText;
	private final McpSettingsPage page;

	public TrystIntegrationGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, McpSettingsPage page) {
		super(editor, orchestrator);
		this.page = page;
		createControl(toolkit, parent);
	}

	@Override
	public void refreshUI() {
		if (executionsTable != null && !executionsTable.isDisposed()) {
			refreshExecutionsTable();
			updateStatusLabel();
		}
	}

	private void createControl(FormToolkit toolkit, Composite parent) {
		group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "TRYST Integration (TEIP 1.0)", 1, true);

		Composite topComp = GUIFactory.INSTANCE.createComposite(group, 2);

		enableTeipCheck = GUIFactory.INSTANCE.createButton(topComp, "Enable TEIP 1.0 / TRYST Bridge", SWT.CHECK);
		enableTeipCheck.setSelection(TeipExecutionManager.getInstance().isTeipEnabled());
		enableTeipCheck.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TeipExecutionManager.getInstance().setTeipEnabled(enableTeipCheck.getSelection());
				updateStatusLabel();
			}
		});

		Label emptyLbl = GUIFactory.INSTANCE.createLabel(topComp, "");

		GUIFactory.INSTANCE.createLabel(topComp, "TRYST MCP URL:");
		trystUrlText = GUIFactory.INSTANCE.createText(topComp);
		trystUrlText.setText("http://localhost:3000/mcp");
		trystUrlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GUIFactory.INSTANCE.createLabel(topComp, "Auth Token (Bearer):");
		authTokenText = new Text(topComp, SWT.BORDER | SWT.PASSWORD);
		authTokenText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite actionComp = GUIFactory.INSTANCE.createComposite(group, 4);

		Button testConnBtn = GUIFactory.INSTANCE.createButton(actionComp, "Test TRYST Connection", SWT.PUSH);
		testConnBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				testTrystConnection();
			}
		});

		Button discoverToolsBtn = GUIFactory.INSTANCE.createButton(actionComp, "Discover TRYST Tools", SWT.PUSH);
		discoverToolsBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				discoverTrystTools();
			}
		});

		Button refreshExecBtn = GUIFactory.INSTANCE.createButton(actionComp, "Refresh Executions", SWT.PUSH);
		refreshExecBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshExecutionsTable();
			}
		});

		Button cancelExecBtn = GUIFactory.INSTANCE.createButton(actionComp, "Cancel Selected", SWT.PUSH);
		cancelExecBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cancelSelectedExecution();
			}
		});

		statusLabel = GUIFactory.INSTANCE.createLabel(group, "Status: Ready");
		statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Executions Table
		Label tableLabel = GUIFactory.INSTANCE.createLabel(group, "Active & Historical Remote TEIP Executions:");
		executionsTable = new Table(group, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
		executionsTable.setHeaderVisible(true);
		executionsTable.setLinesVisible(true);
		GridData gdTable = new GridData(GridData.FILL_BOTH);
		gdTable.heightHint = 120;
		executionsTable.setLayoutData(gdTable);

		String[] titles = { "Execution ID", "Status", "Progress", "Correlation ID", "Message" };
		int[] widths = { 140, 100, 70, 120, 250 };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(executionsTable, SWT.NONE);
			column.setText(titles[i]);
			column.setWidth(widths[i]);
		}

		executionsTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem[] items = executionsTable.getSelection();
				if (items != null && items.length > 0) {
					String execId = items[0].getText(0);
					showExecutionDetails(execId);
				}
			}
		});

		detailsText = new Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.WRAP);
		GridData gdDetails = new GridData(GridData.FILL_HORIZONTAL);
		gdDetails.heightHint = 80;
		detailsText.setLayoutData(gdDetails);
		detailsText.setEditable(false);

		refreshExecutionsTable();
	}

	private void updateStatusLabel() {
		boolean enabled = TeipExecutionManager.getInstance().isTeipEnabled();
		statusLabel.setText("Status: " + (enabled ? "TEIP Enabled / Ready" : "TEIP Disabled"));
	}

	private void testTrystConnection() {
		String url = trystUrlText.getText();
		String token = authTokenText.getText();
		Job job = new Job("Test TRYST MCP Connection") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					TrystMcpClient client = new TrystMcpClient(url, token, List.of("*"));
					JSONObject initRes = client.initialize();
					Display.getDefault().asyncExec(() -> {
						if (!statusLabel.isDisposed()) {
							statusLabel.setText("Connected to TRYST MCP successfully: " + initRes.toString());
						}
					});
					return Status.OK_STATUS;
				} catch (Exception ex) {
					Display.getDefault().asyncExec(() -> {
						if (!statusLabel.isDisposed()) {
							statusLabel.setText("Connection Failed: " + ex.getMessage());
						}
					});
					return Status.OK_STATUS;
				}
			}
		};
		job.schedule();
	}

	private void discoverTrystTools() {
		String url = trystUrlText.getText();
		String token = authTokenText.getText();
		Job job = new Job("Discover TRYST Tools") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					TrystMcpClient client = new TrystMcpClient(url, token, List.of("*"));
					JSONArray tools = client.listTools();
					Display.getDefault().asyncExec(() -> {
						if (!detailsText.isDisposed()) {
							detailsText.setText("Discovered TRYST Tools (" + tools.length() + "):\n" + tools.toString(2));
						}
					});
					return Status.OK_STATUS;
				} catch (Exception ex) {
					Display.getDefault().asyncExec(() -> {
						if (!detailsText.isDisposed()) {
							detailsText.setText("Tool Discovery Failed: " + ex.getMessage());
						}
					});
					return Status.OK_STATUS;
				}
			}
		};
		job.schedule();
	}

	private void refreshExecutionsTable() {
		executionsTable.removeAll();
		List<TeipExecutionStatusResponse> list = TeipExecutionManager.getInstance().listAllExecutions();
		for (TeipExecutionStatusResponse item : list) {
			TableItem row = new TableItem(executionsTable, SWT.NONE);
			row.setText(0, item.getExecutionId() != null ? item.getExecutionId() : "");
			row.setText(1, item.getStatus() != null ? item.getStatus().name() : "");
			row.setText(2, String.format("%.0f%%", item.getProgress() * 100));
			row.setText(3, item.getCorrelationId() != null ? item.getCorrelationId() : "");
			row.setText(4, item.getStatusMessage() != null ? item.getStatusMessage() : "");
		}
	}

	private void showExecutionDetails(String executionId) {
		TeipExecutionStatusResponse st = TeipExecutionManager.getInstance().getStatus(executionId);
		if (st == null) {
			detailsText.setText("Execution not found: " + executionId);
			return;
		}
		StringBuilder sb = new StringBuilder();
		sb.append("Execution ID: ").append(st.getExecutionId()).append("\n");
		sb.append("Status: ").append(st.getStatus()).append("\n");
		sb.append("Progress: ").append(String.format("%.1f%%", st.getProgress() * 100)).append("\n");
		sb.append("Message: ").append(st.getStatusMessage()).append("\n");
		sb.append("Created At: ").append(st.getCreatedAt()).append("\n");
		if (st.getCompletedAt() != null) sb.append("Completed At: ").append(st.getCompletedAt()).append("\n");

		var res = TeipExecutionManager.getInstance().getResult(executionId);
		if (res != null) {
			sb.append("\nResult Summary: ").append(res.getSummary()).append("\n");
		}
		detailsText.setText(sb.toString());
	}

	private void cancelSelectedExecution() {
		TableItem[] items = executionsTable.getSelection();
		if (items != null && items.length > 0) {
			String execId = items[0].getText(0);
			TeipExecutionManager.getInstance().cancelExecution(execId, "Cancelled via UI operator action");
			refreshExecutionsTable();
			showExecutionDetails(execId);
		}
	}
}
