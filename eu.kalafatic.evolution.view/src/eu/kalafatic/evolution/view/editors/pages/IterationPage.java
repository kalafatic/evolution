package eu.kalafatic.evolution.view.editors.pages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;

import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.application.Activator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.utils.factories.GUIFactory;

/**
 * @evo:22:A reason=self-dev-bootstrap-ui
 */
public class IterationPage extends AEvoPage {

	private IterationMemoryService memoryService;
	private File projectRoot;

	private Map<Integer, List<IterationRecord>> iterationsMap = new TreeMap<>();
	private List<Integer> iterationNumbers = new ArrayList<>();
	private int currentIterationIndex = -1;

	private Combo iterationCombo;
	private Label goalLabel;
	private Label resultLabel;
	private Label sessionStatusLabel;
	private Label sessionProgressLabel;

	private TableViewer branchTable;
	private Combo logIterationFilter;
	private Combo logBranchFilter;
	private Combo logLevelFilter;
	private Composite flowComposite;
	private StyledText detailsText;
	private StyledText logText;

	private ImageRegistry imageRegistry;

	private Label[] flowSteps = new Label[6];
	private String[] stepNames = { "PLAN", "CODE", "TEST", "SCORE", "SELECT", "MERGE" };
	private java.util.Timer pollTimer;

	// Caching fields for high-performance log refreshes
	private long lastLogFileSize = 0;
	private long lastLogFileModified = 0;
	private String lastIterFilter = "";
	private String lastBranchFilter = "";
	private String lastLevelFilter = "";

	public IterationPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, editor, orchestrator);
		this.setLayout(new GridLayout(1, false));

		initImageRegistry();
		initMemoryService();
		createControl();
		refreshData();
		startPolling();
	}

	private void initImageRegistry() {
		this.imageRegistry = new ImageRegistry(Display.getDefault());
		registerImage("play", "eu.kalafatic.utils", "icons/actions/play.png");
		registerImage("pause", "eu.kalafatic.utils", "icons/actions/pause.png");
		registerImage("stop", "eu.kalafatic.utils", "icons/actions/stop.png");
		registerImage("resume", "eu.kalafatic.utils", "icons/actions/restart.png");
		registerImage("edit", "eu.kalafatic.utils", "icons/ovr16/write.gif");
	}

	private void registerImage(String key, String pluginId, String path) {
		ImageDescriptor desc = Activator.getImageDescriptor(pluginId, path);
		if (desc != null) {
			imageRegistry.put(key, desc);
		}
	}

	private void initMemoryService() {
		this.projectRoot = null;
		if (orchestrator != null && orchestrator.eResource() != null) {
			org.eclipse.emf.common.util.URI uri = orchestrator.eResource().getURI();
			if (uri.isPlatformResource()) {
				org.eclipse.core.resources.IResource res = org.eclipse.core.resources.ResourcesPlugin.getWorkspace()
						.getRoot().findMember(uri.toPlatformString(true));
				if (res != null)
					projectRoot = res.getProject().getLocation().toFile();
			} else if (uri.isFile()) {
				projectRoot = new File(uri.toFileString()).getParentFile();
			}
		}
		if (projectRoot == null) {
			IEditorInput input = editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				projectRoot = ((IFileEditorInput) input).getFile().getProject().getLocation().toFile();
			}
		}
		if (projectRoot != null) {
			this.memoryService = new IterationMemoryService(projectRoot);
		}
	}

	private void createControl() {
		Composite container = GUIFactory.INSTANCE.createComposite(this, 1);
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		this.setContent(container);

		// Session Status
		Composite statusLine = GUIFactory.INSTANCE.createComposite(container, 2);
		statusLine.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		sessionStatusLabel = GUIFactory.INSTANCE.createLabel(statusLine, "Session: READY");
		sessionProgressLabel = GUIFactory.INSTANCE.createLabel(statusLine, "Progress: 0%");
		sessionProgressLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));

		// 1. Iterations Section
		Composite iterComp = GUIFactory.INSTANCE.createExpandableGroup(toolkit, container, "Iterations", 1, true);
		Composite iterHeader = GUIFactory.INSTANCE.createComposite(iterComp, 2);
		iterHeader.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GUIFactory.INSTANCE.createLabel(iterHeader, "Select Iteration:");
		iterationCombo = new Combo(iterHeader, SWT.READ_ONLY | SWT.DROP_DOWN);
		iterationCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		iterationCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				currentIterationIndex = iterationCombo.getSelectionIndex();
				syncFiltersWithSelection();
				updateUI();
			}
		});

		goalLabel = GUIFactory.INSTANCE.createLabel(iterComp, "Goal: ");
		goalLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		resultLabel = GUIFactory.INSTANCE.createLabel(iterComp, "Result: ");
		resultLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// 2. Branches Section
		Composite branchesComp = GUIFactory.INSTANCE.createExpandableGroup(toolkit, container, "Branches", 1, true);
		branchTable = new TableViewer(branchesComp, SWT.BORDER | SWT.FULL_SELECTION);
		Table table = branchTable.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		GridData gdTable = new GridData(GridData.FILL_HORIZONTAL);
		gdTable.heightHint = 120;
		table.setLayoutData(gdTable);

		createColumns();
		branchTable.setContentProvider(ArrayContentProvider.getInstance());
		branchTable.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				syncFiltersWithSelection();
				updateDetails();
				refreshLogs();
			}
		});

		detailsText = new StyledText(branchesComp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP);
		GridData gdDetails = new GridData(GridData.FILL_HORIZONTAL);
		gdDetails.heightHint = 80;
		detailsText.setLayoutData(gdDetails);
		detailsText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

		// Flow View
		GUIFactory.INSTANCE.createLabel(branchesComp, "Evolution Flow:");
		flowComposite = GUIFactory.INSTANCE.createComposite(branchesComp, 11);
		flowComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		for (int i = 0; i < 6; i++) {
			flowSteps[i] = GUIFactory.INSTANCE.createLabel(flowComposite, stepNames[i], SWT.CENTER);
			flowSteps[i].setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false));
			if (i < 5) {
				Label arrow = GUIFactory.INSTANCE.createLabel(flowComposite, "\u2192", SWT.CENTER);
				arrow.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
			}
		}

		// 3. Logs Section
		Composite logComp = GUIFactory.INSTANCE.createExpandableGroup(toolkit, container, "Logs", 1, true, true);

		// Interactive Filters for Logs
		Composite filterBar = GUIFactory.INSTANCE.createComposite(logComp, 6);
		filterBar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GUIFactory.INSTANCE.createLabel(filterBar, "Iter:");
		logIterationFilter = new Combo(filterBar, SWT.READ_ONLY);
		logIterationFilter.add("All");

		GUIFactory.INSTANCE.createLabel(filterBar, "Branch:");
		logBranchFilter = new Combo(filterBar, SWT.READ_ONLY);
		logBranchFilter.add("All");

		GUIFactory.INSTANCE.createLabel(filterBar, "Level:");
		logLevelFilter = new Combo(filterBar, SWT.READ_ONLY);
		logLevelFilter.setItems("All", "INFO", "WARNING", "SEVERE");
		logLevelFilter.select(0);

		SelectionAdapter logFilterListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshLogs();
			}
		};
		logIterationFilter.addSelectionListener(logFilterListener);
		logBranchFilter.addSelectionListener(logFilterListener);
		logLevelFilter.addSelectionListener(logFilterListener);

		logText = new StyledText(logComp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
		logText.setLayoutData(new GridData(GridData.FILL_BOTH));
		logText.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		logText.setFont(org.eclipse.jface.resource.JFaceResources.getTextFont());
	}

	public void setDirty(boolean dirty) {
		if (editor != null) {
			editor.setDirty(dirty);
		}
	}

	private void createColumns() {
		String[] titles = { "Branch", "Strategy", "Result", "Score" };
		int[] bounds = { 150, 400, 100, 100 };

		TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				IterationRecord r = (IterationRecord) element;
				return r.getBranch();
			}
		});

		col = createTableViewerColumn(titles[1], bounds[1], 1);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				IterationRecord r = (IterationRecord) element;
				return r.getStrategy();
			}
		});

		col = createTableViewerColumn(titles[2], bounds[2], 2);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				IterationRecord r = (IterationRecord) element;
				String res = r.getResult() != null ? r.getResult() : "UNKNOWN";
				return res + ("SUCCESS".equals(res) ? " \u2705" : " \u274C");
			}
		});

		col = createTableViewerColumn(titles[3], bounds[3], 3);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				IterationRecord r = (IterationRecord) element;
				return String.format("%.2f", r.getScore());
			}
		});
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(branchTable, SWT.NONE);
		final org.eclipse.swt.widgets.TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}

	public synchronized void refreshData() {
		if (memoryService != null) {
			memoryService.refresh();
		}

		List<IterationRecord> records = (memoryService != null) ? new ArrayList<>(memoryService.getRecords())
				: new ArrayList<>();

		// Merge with active session iterations
		if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
			List<eu.kalafatic.evolution.model.orchestration.Iteration> sessionIters = orchestrator.getSelfDevSession()
					.getIterations();
			for (int i = 0; i < sessionIters.size(); i++) {
				eu.kalafatic.evolution.model.orchestration.Iteration iter = sessionIters.get(i);
				final int itNum = i + 1;
				boolean exists = records.stream().anyMatch(r -> r.getIteration() == itNum);
				if (!exists) {
					IterationRecord r = new IterationRecord();
					r.setIteration(itNum);
					r.setGoal(orchestrator.getSelfDevSession().getInitialRequest());
					r.setBranch(iter.getBranchName() != null ? iter.getBranchName() : "it-" + itNum);
					r.setStatus(iter.getStatus() != null ? iter.getStatus().getName() : "RUNNING");
					r.setResult("RUNNING");
					r.setStrategy(iter.getPhase() != null ? iter.getPhase() : "EVOLVING");
					records.add(r);
				}
			}
		}

		iterationsMap = records.stream()
				.collect(Collectors.groupingBy(IterationRecord::getIteration, TreeMap::new, Collectors.toList()));
		iterationNumbers = new ArrayList<>(iterationsMap.keySet());

		if (currentIterationIndex == -1 && !iterationNumbers.isEmpty()) {
			currentIterationIndex = iterationNumbers.size() - 1; // Last one
		} else if (currentIterationIndex >= iterationNumbers.size()) {
			currentIterationIndex = iterationNumbers.size() - 1;
		}

		// Update Iteration Combo
		if (iterationCombo != null && !iterationCombo.isDisposed()) {
			String[] items = iterationNumbers.stream().map(n -> "Iteration " + n).toArray(String[]::new);
			iterationCombo.setItems(items);
			if (currentIterationIndex >= 0 && currentIterationIndex < items.length) {
				iterationCombo.select(currentIterationIndex);
			}
		}

		// Update Log Iteration Filter
		if (logIterationFilter != null && !logIterationFilter.isDisposed()) {
			String selected = logIterationFilter.getText();
			logIterationFilter.removeAll();
			logIterationFilter.add("All");
			for (Integer num : iterationNumbers) {
				logIterationFilter.add(String.valueOf(num));
			}
			boolean hasSelected = false;
			for (String item : logIterationFilter.getItems()) {
				if (item.equals(selected)) {
					hasSelected = true;
					break;
				}
			}
			logIterationFilter.setText(hasSelected ? selected : "All");
		}
	}

	private void updateUI() {
		if (currentIterationIndex < 0 || currentIterationIndex >= iterationNumbers.size()) {
			goalLabel.setText("Goal: N/A");
			resultLabel.setText("Result: N/A");
			branchTable.setInput(Collections.emptyList());
			updateFlow(null);
			return;
		}

		int itNum = iterationNumbers.get(currentIterationIndex);
		List<IterationRecord> variants = iterationsMap.get(itNum);

		if (iterationCombo.getSelectionIndex() != currentIterationIndex) {
			iterationCombo.select(currentIterationIndex);
		}

		if (!variants.isEmpty()) {
			IterationRecord first = variants.get(0);
			goalLabel.setText("Goal: " + first.getGoal());

			boolean anySuccess = variants.stream().anyMatch(v -> "SUCCESS".equals(v.getResult()));
			resultLabel.setText("Result: " + (anySuccess ? "SUCCESS \u2705" : "FAILED \u274C"));
			resultLabel.setForeground(anySuccess ? Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN)
					: Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		}

		branchTable.setInput(variants);
		syncFiltersWithSelection();
		if (!variants.isEmpty()) {
			IterationRecord selected = variants.stream().filter(v -> "SUCCESS".equals(v.getResult())).findFirst()
					.orElse(variants.get(0));
			branchTable.setSelection(new org.eclipse.jface.viewers.StructuredSelection(selected));
		}
		updateDetails();
	}

	private void updateDetails() {
		IStructuredSelection selection = (IStructuredSelection) branchTable.getSelection();
		IterationRecord selected = (IterationRecord) selection.getFirstElement();

		if (selected == null) {
			detailsText.setText("");
			updateFlow(null);
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("Strategy: ").append(selected.getStrategy()).append("\n");
		sb.append("Branch: ").append(selected.getBranch()).append("\n");
		sb.append("Result: ").append(selected.getResult()).append("\n");
		sb.append("Score: ").append(selected.getScore()).append("\n");
		if (selected.getErrorMessage() != null && !selected.getErrorMessage().isEmpty()) {
			sb.append("\nError:\n").append(selected.getErrorMessage()).append("\n");
		}
		if (selected.getChangedFiles() != null && !selected.getChangedFiles().isEmpty()) {
			sb.append("\nChanged Files:\n");
			for (String f : selected.getChangedFiles()) {
				sb.append("- ").append(f).append("\n");
			}
		}
		detailsText.setText(sb.toString());

		updateFlow(selected);
	}

	private void syncFiltersWithSelection() {
		if (currentIterationIndex >= 0 && currentIterationIndex < iterationNumbers.size()) {
			int itNum = iterationNumbers.get(currentIterationIndex);
			logIterationFilter.setText(String.valueOf(itNum));

			// Update branch filter list
			List<IterationRecord> variants = iterationsMap.get(itNum);
			String selectedBranch = logBranchFilter.getText();
			logBranchFilter.removeAll();
			logBranchFilter.add("All");
			if (variants != null) {
				for (IterationRecord r : variants) {
					if (r != null && r.getBranch() != null && !r.getBranch().isEmpty()) {
						logBranchFilter.add(r.getBranch());
					}

				}
			}

			IStructuredSelection selection = (IStructuredSelection) branchTable.getSelection();
			IterationRecord selectedRecord = (IterationRecord) selection.getFirstElement();
			if (selectedRecord != null) {
				if (selectedRecord.getBranch() != null && !selectedRecord.getBranch().isEmpty()) {
					logBranchFilter.setText(selectedRecord.getBranch());
				}

			} else {
				logBranchFilter.setText("All");
			}
		}
	}

	private void refreshLogs() {
		if (projectRoot == null)
			return;

		String iterFilter = logIterationFilter.getText();
		String branchFilter = logBranchFilter.getText();
		String levelFilter = logLevelFilter.getText();

		File logFile = new File(Log.getLogFile());
		if (!logFile.exists()) {
			logText.setText("Log file not found: " + logFile.getAbsolutePath());
			return;
		}

		long currentSize = logFile.length();
		long currentModified = logFile.lastModified();

		// Highly optimized cache bypass check
		if (currentSize == lastLogFileSize &&
			currentModified == lastLogFileModified &&
			iterFilter.equals(lastIterFilter) &&
			branchFilter.equals(lastBranchFilter) &&
			levelFilter.equals(lastLevelFilter)) {
			return; // Skip parsing if nothing changed
		}

		lastLogFileSize = currentSize;
		lastLogFileModified = currentModified;
		lastIterFilter = iterFilter;
		lastBranchFilter = branchFilter;
		lastLevelFilter = levelFilter;

		StringBuilder sb = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (filterLogLine(line, iterFilter, branchFilter, levelFilter)) {
					sb.append(line).append("\n");
				}
			}
		} catch (Exception e) {
			sb.append("Error reading logs: ").append(e.getMessage());
		}

		if (sb.length() == 0) {
			sb.append("No logs found matching the current filters.");
		}

		logText.setText(sb.toString());
		logText.setSelection(logText.getCharCount()); // Scroll to bottom
	}

	private boolean filterLogLine(String line, String iter, String branch, String level) {
		// Level filter
		if (!"All".equals(level)) {
			// Standard Java logging levels or common tags
			if (!line.toUpperCase().contains(level.toUpperCase()))
				return false;
		}

		// Iteration filter
		if (!"All".equals(iter)) {
			String itTag = "it-" + iter;
			// Matches [it-1], it-1, Iteration 1
			boolean found = line.contains("[" + itTag + "]") || line.contains(" " + itTag + " ")
					|| line.contains("Iteration " + iter);
			if (!found)
				return false;
		}

		// Branch filter
		if (!"All".equals(branch)) {
			if (!line.contains(branch))
				return false;
		}

		return true;
	}

	private void updateFlow(IterationRecord selected) {
		if (selected == null) {
			boolean changed = false;
			for (int i = 0; i < 6; i++) {
				Color targetColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
				String targetText = stepNames[i];
				if (!targetColor.equals(flowSteps[i].getForeground())) {
					flowSteps[i].setForeground(targetColor);
					changed = true;
				}
				if (!targetText.equals(flowSteps[i].getText())) {
					flowSteps[i].setText(targetText);
					changed = true;
				}
			}
			if (changed) {
				flowComposite.layout();
			}
			return;
		}

		boolean isSuccess = "SUCCESS".equals(selected.getResult());
		boolean isFailed = "FAILED".equals(selected.getResult());
		String phase = selected.getStrategy() != null ? selected.getStrategy().toUpperCase() : "";

		int activeIndex = -1;
		if (phase.contains("PLAN")) activeIndex = 0;
		else if (phase.contains("CODE")) activeIndex = 1;
		else if (phase.contains("TEST") || phase.contains("VERIFY")) activeIndex = 2;
		else if (phase.contains("SCORE") || phase.contains("EVAL")) activeIndex = 3;
		else if (phase.contains("SELECT")) activeIndex = 4;
		else if (phase.contains("MERGE") || phase.contains("INTEGRAT")) activeIndex = 5;

		boolean changed = false;
		for (int i = 0; i < 6; i++) {
			Color targetColor;
			String targetText;

			if (isSuccess) {
				targetColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
				targetText = stepNames[i] + " \u2714";
			} else if (isFailed) {
				if (activeIndex != -1 && i == activeIndex) {
					targetColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED);
					targetText = stepNames[i] + " \u2718";
				} else if (activeIndex != -1 && i < activeIndex) {
					targetColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
					targetText = stepNames[i] + " \u2714";
				} else {
					targetColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
					targetText = stepNames[i];
				}
			} else {
				// Running / Active
				if (activeIndex != -1 && i < activeIndex) {
					targetColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GREEN);
					targetText = stepNames[i] + " \u2714";
				} else if (activeIndex != -1 && i == activeIndex) {
					targetColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE);
					targetText = stepNames[i] + " \u25B6";
				} else {
					targetColor = Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
					targetText = stepNames[i];
				}
			}

			if (!targetColor.equals(flowSteps[i].getForeground())) {
				flowSteps[i].setForeground(targetColor);
				changed = true;
			}
			if (!targetText.equals(flowSteps[i].getText())) {
				flowSteps[i].setText(targetText);
				changed = true;
			}
		}
		if (changed) {
			flowComposite.layout();
		}
	}

	private void startPolling() {
		pollTimer = new java.util.Timer(true);
		pollTimer.scheduleAtFixedRate(new java.util.TimerTask() {
			@Override
			public void run() {
				if (isDisposed()) {
					pollTimer.cancel();
					return;
				}

				boolean memoryChanged = (memoryService != null) && memoryService.refresh();
				boolean sessionRunning = false;
				if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
					sessionRunning = !orchestrator.getSelfDevSession().getIterations().isEmpty();
				}

				final boolean forceUpdate = memoryChanged || sessionRunning;

				Display.getDefault().asyncExec(() -> {
					if (!isDisposed()) {
						if (forceUpdate) {
							refreshData();
							updateUI();
							updateSessionStatus();
						}
						// Robust, high-performance real-time log refresh
						refreshLogs();
					}
				});
			}
		}, 2000, 2000);
	}

	private void updateSessionStatus() {
		if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
			eu.kalafatic.evolution.model.orchestration.SelfDevSession session = orchestrator.getSelfDevSession();
			sessionStatusLabel.setText("Session: " + session.getStatus().getName());

			int max = session.getMaxIterations();
			int current = session.getIterations().size();
			double progress = max > 0 ? (double) current / max * 100 : 0;
			sessionProgressLabel.setText(String.format("Progress: %.0f%%", progress));
		}
	}

	@Override
	public void setOrchestrator(Orchestrator orchestrator) {
		super.setOrchestrator(orchestrator);
		refreshData();
		updateUI();
	}

	@Override
	protected void refreshUI() {
		refreshData();
		updateUI();
		updateSessionStatus();
	}

	public void updateUIFromModel() {
		scheduleRefresh();
	}

	@Override
	public void dispose() {
		if (pollTimer != null) {
			pollTimer.cancel();
		}
		if (imageRegistry != null) {
			imageRegistry.dispose();
		}
		if (toolkit != null)
			toolkit.dispose();
		super.dispose();
	}
}
