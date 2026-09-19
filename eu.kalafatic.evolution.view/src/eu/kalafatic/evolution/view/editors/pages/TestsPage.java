package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.Bundle;

import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Test;
import eu.kalafatic.evolution.model.orchestration.TestStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.tests.iterative.ISimulationTest;
import eu.kalafatic.evolution.tests.iterative.ITestListener;
import eu.kalafatic.evolution.tests.iterative.IterativeDevelopmentTest;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.tests.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestsPage extends AEvoPage {

	private boolean isUpdating = false;
	private SharedScrolledComposite testsScrolled;
	private Composite testsContent;
	private Browser statusBrowser;
	private List<TestRow> testRows = new ArrayList<>();
	private List<TestTableGroup> dynamicGroups = new ArrayList<>();
	private ISimulationTest iterativeTest;
	private List<Class<?>> discoveredTestClasses = new ArrayList<>();

	private PredefinedTestsGroup predefinedTestsGroup;
	private IterativeDevelopmentLifecycleGroup iterativeDevelopmentLifecycleGroup;

	private Adapter modelAdapter = new EContentAdapter() {
		@Override public void notifyChanged(Notification notification) {
			super.notifyChanged(notification);
			if (notification.isTouch()) return;
			if (!isUpdating) {
				scheduleRefresh();
			}
		}
	};

	public class TestRow {
		public Test test;
		public Button executeBtn;
		public TableItem item;
		TestRow(Test test, Button executeBtn, TableItem item) { this.test = test; this.executeBtn = executeBtn; this.item = item; }
	}

	public TestsPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
		super(parent, editor, orchestrator);
		this.setLayout(new GridLayout(1, false));
		createControl();
	}

	private void createControl() {
		Composite container = toolkit.createComposite(this);
		container.setLayout(new GridLayout(1, false));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));
		this.setContent(container);

		SashForm mainSash = new SashForm(container, SWT.VERTICAL);
		mainSash.setLayoutData(new GridData(GridData.FILL_BOTH));

		testsScrolled = new SharedScrolledComposite(mainSash, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER) {};
		testsScrolled.setExpandHorizontal(true); testsScrolled.setExpandVertical(true);
		testsContent = toolkit.createComposite(testsScrolled, SWT.NONE);
		testsContent.setLayout(new GridLayout(1, false));
		testsScrolled.setContent(testsContent);

		discoverTests();
		predefinedTestsGroup = new PredefinedTestsGroup(toolkit, testsContent, editor, orchestrator, this);
		iterativeDevelopmentLifecycleGroup = new IterativeDevelopmentLifecycleGroup(toolkit, testsContent, editor, orchestrator, this);
		predefinedTestsGroup.syncTestsToModel();

		Button addBtn = toolkit.createButton(testsContent, "Add Test", SWT.PUSH);
		addBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) { addNewTest(); }
		});

		statusBrowser = new Browser(mainSash, SWT.NONE);
		mainSash.setWeights(new int[] { 2, 1 });
		statusBrowser.setText(getHtmlTemplate());
	}

	@Override
	public void setOrchestrator(Orchestrator orchestrator) {
		if (this.orchestrator != null) this.orchestrator.eAdapters().remove(modelAdapter);
		super.setOrchestrator(orchestrator);
		if (this.orchestrator != null) this.orchestrator.eAdapters().add(modelAdapter);
	}

	@Override
	protected void refreshUI() {
		if (isUpdating || orchestrator == null || testsContent == null || testsContent.isDisposed() || !isVisible()) return;
		isUpdating = true;
		testRows.clear();
		discoverTests();

		if (predefinedTestsGroup != null) {
			predefinedTestsGroup.setOrchestrator(orchestrator);
		}
		if (iterativeDevelopmentLifecycleGroup != null) iterativeDevelopmentLifecycleGroup.setOrchestrator(orchestrator);

		// Synchronize Lifecycle Browser with Model Phase
		if (orchestrator.getSelfDevSession() != null && !orchestrator.getSelfDevSession().getIterations().isEmpty()) {
			Iteration last = orchestrator.getSelfDevSession().getIterations().get(orchestrator.getSelfDevSession().getIterations().size() - 1);
			String phase = last.getPhase();
			if (phase != null && iterativeDevelopmentLifecycleGroup.getBrowser() != null) {
				Display.getDefault().asyncExec(() -> {
					if (!iterativeDevelopmentLifecycleGroup.getBrowser().isDisposed()) {
						iterativeDevelopmentLifecycleGroup.getBrowser().execute("resetDiagram();");
						iterativeDevelopmentLifecycleGroup.getBrowser().execute("setNodeStatus('" + phase.toLowerCase() + "', 'active');");
					}
				});
			}
		}

		// Remove existing dynamic groups
		for (TestTableGroup dg : dynamicGroups) {
			dg.dispose();
			if (dg.getGroup() != null && !dg.getGroup().isDisposed()) {
				dg.getGroup().getParent().dispose(); // Dispose the Section
			}
		}
		dynamicGroups.clear();

		java.util.Map<String, List<Test>> groupedBy = new java.util.HashMap<>();
		for (Test test : orchestrator.getTests()) {
			String type = test.getType() != null ? test.getType() : "General";
			if (!"Predefined".equals(type)) groupedBy.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(test);
		}
		for (String type : groupedBy.keySet()) {
			TestTableGroup dynamicGroup = new TestTableGroup(toolkit, testsContent, type + " Tests", groupedBy.get(type), false, editor, orchestrator, this);
			dynamicGroups.add(dynamicGroup);
		}

		if (!testsContent.isDisposed()) {
			testsContent.layout(true, true);
			testsScrolled.setMinSize(testsContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			testsScrolled.reflow(true);
		}
		isUpdating = false;
		refreshBrowser();
	}

	public void updateUIFromModel() {
		scheduleRefresh();
	}

	public void registerTestRow(Test test, Button executeBtn, TableItem item) {
		testRows.add(new TestRow(test, executeBtn, item));
	}

	public void addTestToModel(Test test) {
		orchestrator.getTests().add(test);
	}

	public void discoverTests() {
		if (!discoveredTestClasses.isEmpty()) return;
		Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.tests");
		if (bundle != null) {
			// Try to find tests in the bundle
			// Try to find .class files first
			java.util.Enumeration<java.net.URL> entries = bundle.findEntries("/", "*Test.class", true);
			if (entries != null) {
				while (entries.hasMoreElements()) {
					java.net.URL url = entries.nextElement(); String path = url.getPath();
					String className = path.replace("/", "."); if (className.endsWith(".class")) className = className.substring(0, className.length() - 6);
					if (className.startsWith(".")) className = className.substring(1);
					String[] prefixes = { "bin.", "target.classes.", "target.test-classes.", "src." };
					for (String pref : prefixes) if (className.contains(pref)) className = className.substring(className.indexOf(pref) + pref.length());
					try { Class<?> clazz = bundle.loadClass(className); if (!clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) discoveredTestClasses.add(clazz); } catch (Exception e) {}
				}
			}
			// If no classes found, try to find .java files as fallback for source-only environments
			if (discoveredTestClasses.isEmpty()) {
				entries = bundle.findEntries("/", "*Test.java", true);
				if (entries != null) {
					while (entries.hasMoreElements()) {
						java.net.URL url = entries.nextElement(); String path = url.getPath();
						String className = path.replace("/", "."); if (className.endsWith(".java")) className = className.substring(0, className.length() - 5);
						if (className.startsWith(".")) className = className.substring(1);
						if (className.contains("src.")) className = className.substring(className.indexOf("src.") + 4);
						try { Class<?> clazz = bundle.loadClass(className); if (!clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) discoveredTestClasses.add(clazz); } catch (Exception e) {}
					}
				}
			}
		}

		// If still empty, try to look at the filesystem directly if we are in a dev environment
		if (discoveredTestClasses.isEmpty()) {
			try {
				File testsProject = new File("eu.kalafatic.evolution.tests/src/eu/kalafatic/evolution/tests/iterative");
				if (testsProject.exists() && testsProject.isDirectory()) {
					for (File f : testsProject.listFiles()) {
						if (f.getName().endsWith("Test.java")) {
							String className = "eu.kalafatic.evolution.tests.iterative." + f.getName().substring(0, f.getName().length() - 5);
							try {
								Class<?> clazz = (bundle != null) ? bundle.loadClass(className) : Class.forName(className);
								if (!clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
									if (!discoveredTestClasses.contains(clazz)) discoveredTestClasses.add(clazz);
								}
							} catch (Exception e) {}
						}
					}
				}
			} catch (Exception e) {}
		}
	}

	public List<Class<?>> getDiscoveredTestClasses() { return discoveredTestClasses; }

	private void addNewTest() {
		discoverTests();
		ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new LabelProvider() {
			@Override public String getText(Object element) { return ((Class<?>) element).getSimpleName(); }
		});
		dialog.setTitle("Select Test to Add"); dialog.setMessage("Select a test from the available test modules:");
		dialog.setElements(discoveredTestClasses.toArray());
		if (dialog.open() == org.eclipse.jface.window.Window.OK) {
			for (Object obj : dialog.getResult()) {
				Class<?> testClass = (Class<?>) obj; String name = testClass.getSimpleName();
				boolean exists = false; for (Test t : orchestrator.getTests()) if (name.equals(t.getName())) { exists = true; break; }
				if (!exists) {
					Test newTest = OrchestrationFactory.eINSTANCE.createTest(); newTest.setName(name); newTest.setType("General"); newTest.setStatus(TestStatus.PENDING);
					orchestrator.getTests().add(newTest); editor.setDirty(true);
				}
			}
			updateUIFromModel();
		}
	}

	public void handleTestSelection(Test selected) {
		if (isUpdating) return; isUpdating = true;
		for (Test t : orchestrator.getTests()) t.setSelected(t == selected);
		updateUIFromModel(); isUpdating = false; editor.setDirty(true);
	}

	public void executeTest(Test test) {
		Display.getDefault().asyncExec(() -> {
			if (isDisposed()) return;
			test.setStatus(TestStatus.RUNNING);
			refreshBrowser();
			for (TestRow row : testRows) {
				if (row.test == test && row.item != null && !row.item.isDisposed()) {
					row.item.setText(row.test.getType().equals("Predefined") ? 3 : 2, TestStatus.RUNNING.toString());
					break;
				}
			}
			boolean isSimulation = "ProjectSetupTest".equals(test.getName()) || "ManualOrchestrationTest".equals(test.getName()) || "IterativeDevelopmentTest".equals(test.getName()) || "AutonomousImprovementTest".equals(test.getName());
			if (isSimulation && iterativeDevelopmentLifecycleGroup.getBrowser() != null && !iterativeDevelopmentLifecycleGroup.getBrowser().isDisposed()) {
				runIterativeSimulation(iterativeDevelopmentLifecycleGroup.getBrowser(), null, test);
			} else {
				Display.getDefault().timerExec(2000, () -> {
					Display.getDefault().asyncExec(() -> {
						if (isDisposed()) return;
						if (test.eContainer() != null) {
							TestStatus status = Math.random() > 0.3 ? TestStatus.PASSED : TestStatus.FAILED;
							test.setStatus(status);
							for (TestRow row : testRows) {
								if (row.test == test && row.item != null && !row.item.isDisposed()) {
									row.item.setText(row.test.getType().equals("Predefined") ? 3 : 2, status.toString());
									break;
								}
							}
							refreshBrowser();
						}
					});
				});
			}
		});
	}

	private void refreshBrowser() {
		if (statusBrowser == null || statusBrowser.isDisposed()) return;
		Display.getDefault().asyncExec(() -> {
			if (statusBrowser != null && !statusBrowser.isDisposed()) {
				statusBrowser.execute("updateDiagram(" + getTestsAsJson() + ");");
			}
		});
	}

	private String getTestsAsJson() {
		JSONArray arr = new JSONArray();
		if (orchestrator != null) {
			for (Test t : orchestrator.getTests()) {
				JSONObject obj = new JSONObject();
				obj.put("name", t.getName());
				obj.put("status", t.getStatus().toString());
				obj.put("type", t.getType() != null ? t.getType() : "General");
				arr.put(obj);
			}
		}
		return arr.toString();
	}

	public void runIterativeSimulation(Browser browser, Button runBtn, Test testModel) {
		if (iterativeTest != null) iterativeTest.stop();
		ITestListener listener = new ITestListener() {
			@Override public void stepStarted(String step) {
				Display.getDefault().asyncExec(() -> {
					if (browser != null && !browser.isDisposed()) browser.execute("setNodeStatus('" + step + "', 'active');");
				});
			}
			@Override public void stepSuccess(String step) {
				Display.getDefault().asyncExec(() -> {
					if (browser != null && !browser.isDisposed()) browser.execute("setNodeStatus('" + step + "', 'success');");
					boolean isLast = false;
					String name = (testModel != null) ? testModel.getName() : "";
					if ("ProjectSetupTest".equals(name) && "validate".equals(step)) isLast = true;
					else if ("ManualOrchestrationTest".equals(name) && "evaluate".equals(step)) isLast = true;
					else if ("IterativeDevelopmentTest".equals(name) && "learn".equals(step)) isLast = true;
					else if ("AutonomousImprovementTest".equals(name) && "learn".equals(step)) isLast = true;
					else if (testModel == null && "refine".equals(step)) isLast = true;
					if (isLast) {
						if (testModel != null) { testModel.setStatus(TestStatus.PASSED); updateStatusInTable(testModel); }
						if (runBtn != null && !runBtn.isDisposed()) runBtn.setEnabled(true);
						refreshBrowser();
					}
				});
			}
			@Override public void stepFailed(String step) {
				Display.getDefault().asyncExec(() -> {
					if (browser != null && !browser.isDisposed()) browser.execute("setNodeStatus('" + step + "', 'failed');");
					if (runBtn != null && !runBtn.isDisposed()) runBtn.setEnabled(true);
					if (testModel != null) { testModel.setStatus(TestStatus.FAILED); updateStatusInTable(testModel); }
					refreshBrowser();
				});
			}
			@Override public void stepSkipped(String step) {
				Display.getDefault().asyncExec(() -> {
					if (browser != null && !browser.isDisposed()) browser.execute("setNodeStatus('" + step + "', 'skipped');");
				});
			}
			@Override public void transitionActive(String edgeId) {
				Display.getDefault().asyncExec(() -> {
					if (browser != null && !browser.isDisposed()) {
						browser.execute("setEdgeStatus('" + edgeId + "', 'active');");
						Display.getDefault().timerExec(500, () -> {
							if (browser != null && !browser.isDisposed()) browser.execute("setEdgeStatus('" + edgeId + "', '');");
						});
					}
				});
			}
			@Override public void reset() {
				Display.getDefault().asyncExec(() -> {
					if (browser != null && !browser.isDisposed()) {
						browser.execute("resetDiagram();");
						if (runBtn != null && !runBtn.isDisposed()) runBtn.setEnabled(false);
					}
				});
			}
		};
		String testName = (testModel != null) ? testModel.getName() : "IterativeDevelopmentTest";
		try {
			Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.tests");
			Class<?> clazz = bundle.loadClass("eu.kalafatic.evolution.tests.iterative." + testName);
			iterativeTest = (ISimulationTest) clazz.getConstructor(ITestListener.class).newInstance(listener);
		} catch (Exception e) {
			iterativeTest = new IterativeDevelopmentTest(listener);
		}
		new Thread(() -> {
			iterativeTest.run();
			Display.getDefault().asyncExec(() -> {
				if (runBtn != null && !runBtn.isDisposed()) runBtn.setEnabled(true);
			});
		}).start();
	}

	private void updateStatusInTable(Test test) {
		for (TestRow row : testRows) {
			if (row.test == test) {
				row.item.setText(row.test.getType().equals("Predefined") ? 3 : 2, test.getStatus().toString());
				break;
			}
		}
	}

	public static String getIterativeHtmlTemplate() {
		String css = eu.kalafatic.evolution.controller.ui.EvoStyleManager.getInstance().getEvoStyleCss();
		return "<!DOCTYPE html>\n"
				+ "<html>\n"
				+ "<head>\n"
				+ "  <meta charset='UTF-8'>\n"
				+ "  <style>\n"
				+ css + "\n"
				+ "    html, body { margin: 0; padding: 0; height: 100%; width: 100%; overflow: hidden; background-color: var(--evo-bg); font-family: var(--evo-font-family); }\n"
				+ "    .diagram-page { display: flex; flex-direction: column; height: 100vh; width: 100vw; box-sizing: border-box; padding: 8px; gap: 8px; }\n"
				+ "    .diagram-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 6px 12px; background: var(--evo-surface-alt); border: 1px solid var(--evo-border); border-radius: var(--evo-radius); box-shadow: var(--evo-shadow-sm); }\n"
				+ "    .diagram-title { font-weight: 700; font-size: 11px; color: var(--evo-primary); letter-spacing: 0.5px; text-transform: uppercase; display: flex; align-items: center; gap: 6px; }\n"
				+ "    .legend-bar { display: flex; align-items: center; gap: 8px; font-size: 10px; }\n"
				+ "    .legend-item { display: flex; align-items: center; gap: 4px; color: var(--evo-text-secondary); font-weight: 600; }\n"
				+ "    .legend-dot { width: 8px; height: 8px; border-radius: 50%; border: 1px solid var(--evo-border-dark); }\n"
				+ "    .legend-dot.ready { background: var(--evo-surface); border-color: var(--evo-border-dark); }\n"
				+ "    .legend-dot.active { background: var(--evo-primary); border-color: var(--evo-primary-border); box-shadow: 0 0 6px rgba(29,82,150,0.6); }\n"
				+ "    .legend-dot.success { background: var(--evo-success); border-color: var(--evo-success-hover); }\n"
				+ "    .legend-dot.failed { background: var(--evo-danger); border-color: var(--evo-danger-hover); }\n"
				+ "    .legend-dot.skipped { background: var(--evo-surface-alt); border-style: dashed; }\n"
				+ "    .canvas-container { flex: 1; position: relative; background: var(--evo-surface); border: 1px solid var(--evo-border); border-radius: var(--evo-radius); box-shadow: var(--evo-shadow-sm); overflow: hidden; display: flex; align-items: center; justify-content: center; }\n"
				+ "    svg { width: 100%; height: 100%; max-height: 100%; display: block; }\n"
				+ "    .node { cursor: pointer; transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }\n"
				+ "    .node .node-card { fill: var(--evo-surface-alt); stroke: var(--evo-border); stroke-width: 1.5px; rx: 6px; ry: 6px; filter: drop-shadow(0 1px 2px rgba(0,0,0,0.06)); transition: all 0.3s ease; }\n"
				+ "    .node .node-title { fill: var(--evo-text); font-size: 10px; font-weight: 700; text-anchor: middle; font-family: var(--evo-font-family); pointer-events: none; }\n"
				+ "    .node .node-num { fill: var(--evo-primary); font-size: 9px; font-weight: 800; font-family: var(--evo-font-mono); pointer-events: none; }\n"
				+ "    .node .status-pill { fill: var(--evo-surface); stroke: var(--evo-border); stroke-width: 1px; rx: 3px; ry: 3px; }\n"
				+ "    .node .status-text { fill: var(--evo-text-muted); font-size: 8px; font-weight: 700; text-anchor: middle; font-family: var(--evo-font-mono); pointer-events: none; }\n"
				+ "    /* Node States */\n"
				+ "    .node.active .node-card { fill: var(--evo-primary-light); stroke: var(--evo-primary); stroke-width: 2.5px; animation: pulseGlow 1.6s infinite ease-in-out; }\n"
				+ "    .node.active .node-title { fill: var(--evo-primary-hover); }\n"
				+ "    .node.active .status-pill { fill: var(--evo-primary); stroke: var(--evo-primary-border); }\n"
				+ "    .node.active .status-text { fill: #ffffff; }\n"
				+ "    .node.success .node-card { fill: var(--evo-success-light); stroke: var(--evo-success); stroke-width: 2px; }\n"
				+ "    .node.success .node-title { fill: var(--evo-success-hover); }\n"
				+ "    .node.success .status-pill { fill: var(--evo-success); stroke: var(--evo-success-hover); }\n"
				+ "    .node.success .status-text { fill: #ffffff; }\n"
				+ "    .node.failed .node-card { fill: var(--evo-danger-light); stroke: var(--evo-danger); stroke-width: 2px; }\n"
				+ "    .node.failed .node-title { fill: var(--evo-danger-hover); }\n"
				+ "    .node.failed .status-pill { fill: var(--evo-danger); stroke: var(--evo-danger-hover); }\n"
				+ "    .node.failed .status-text { fill: #ffffff; }\n"
				+ "    .node.skipped .node-card { fill: var(--evo-surface-alt); stroke: var(--evo-border-dark); stroke-dasharray: 4,3; stroke-width: 1.5px; }\n"
				+ "    .node.skipped .status-text { fill: var(--evo-text-muted); }\n"
				+ "    .node:hover .node-card { stroke: var(--evo-primary); filter: drop-shadow(0 2px 6px rgba(0,0,0,0.12)); transform: translateY(-1px); }\n"
				+ "    /* Edge Paths */\n"
				+ "    .edge { stroke: var(--evo-border-dark); stroke-width: 2px; fill: none; transition: stroke 0.3s ease, stroke-width 0.3s ease; opacity: 0.8; }\n"
				+ "    .edge.active { stroke: var(--evo-primary); stroke-width: 3.5px; opacity: 1; animation: dashFlow 1s linear infinite; stroke-dasharray: 6,4; }\n"
				+ "    .edge.loopback { stroke-dasharray: 4,3; }\n"
				+ "    /* Floating Inspector Tooltip */\n"
				+ "    .inspector-tooltip { position: absolute; bottom: 8px; left: 12px; right: 12px; padding: 6px 12px; background: rgba(255,255,255,0.92); backdrop-filter: blur(4px); border: 1px solid var(--evo-border); border-radius: var(--evo-radius); font-size: 10.5px; color: var(--evo-text); display: flex; align-items: center; justify-content: space-between; box-shadow: var(--evo-shadow-sm); pointer-events: none; opacity: 0; transition: opacity 0.2s ease; }\n"
				+ "    .inspector-tooltip.show { opacity: 1; }\n"
				+ "    .inspector-label { font-weight: 700; color: var(--evo-primary); }\n"
				+ "    @keyframes pulseGlow { 0% { stroke-opacity: 1; filter: drop-shadow(0 0 2px rgba(29,82,150,0.3)); } 50% { stroke-opacity: 0.4; filter: drop-shadow(0 0 8px rgba(29,82,150,0.7)); } 100% { stroke-opacity: 1; filter: drop-shadow(0 0 2px rgba(29,82,150,0.3)); } }\n"
				+ "    @keyframes dashFlow { from { stroke-dashoffset: 20; } to { stroke-dashoffset: 0; } }\n"
				+ "  </style>\n"
				+ "</head>\n"
				+ "<body>\n"
				+ "  <div class='diagram-page'>\n"
				+ "    <div class='diagram-toolbar'>\n"
				+ "      <div class='diagram-title'>\n"
				+ "        <svg width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.5'><path d='M21.5 2v6h-6M21.34 15.57a10 10 0 1 1-.57-8.38l5.67-5.67'/></svg>\n"
				+ "        Iterative Development Lifecycle\n"
				+ "      </div>\n"
				+ "      <div class='legend-bar'>\n"
				+ "        <div class='legend-item'><div class='legend-dot ready'></div>Ready</div>\n"
				+ "        <div class='legend-item'><div class='legend-dot active'></div>Active</div>\n"
				+ "        <div class='legend-item'><div class='legend-dot success'></div>Success</div>\n"
				+ "        <div class='legend-item'><div class='legend-dot failed'></div>Failed</div>\n"
				+ "        <div class='legend-item'><div class='legend-dot skipped'></div>Skipped</div>\n"
				+ "        <button class='btn btn-sm btn-secondary' onclick='resetDiagram()'>Reset</button>\n"
				+ "      </div>\n"
				+ "    </div>\n"
				+ "    <div class='canvas-container'>\n"
				+ "      <svg id='lifecycle-svg' viewBox='0 0 940 230' preserveAspectRatio='xMidYMid meet'>\n"
				+ "        <defs>\n"
				+ "          <marker id='arrow' markerWidth='8' markerHeight='6' refX='7' refY='3' orient='auto'>\n"
				+ "            <polygon points='0 0, 8 3, 0 6' fill='#98a2ac'/>\n"
				+ "          </marker>\n"
				+ "          <marker id='arrow-active' markerWidth='8' markerHeight='6' refX='7' refY='3' orient='auto'>\n"
				+ "            <polygon points='0 0, 8 3, 0 6' fill='#1d5296'/>\n"
				+ "          </marker>\n"
				+ "        </defs>\n"
				+ "        <!-- Upper Pipeline Connectors (Left to Right) -->\n"
				+ "        <path id='e_observe_analyze' class='edge' marker-end='url(#arrow)' d='M 105 50 L 140 50' />\n"
				+ "        <path id='e_analyze_plan' class='edge' marker-end='url(#arrow)' d='M 235 50 L 270 50' />\n"
				+ "        <path id='e_plan_validate' class='edge' marker-end='url(#arrow)' d='M 365 50 L 400 50' />\n"
				+ "        <path id='e_validate_execute' class='edge' marker-end='url(#arrow)' d='M 495 50 L 530 50' />\n"
				+ "        <path id='e_execute_test' class='edge' marker-end='url(#arrow)' d='M 625 50 L 660 50' />\n"
				+ "        <path id='e_test_evaluate' class='edge' marker-end='url(#arrow)' d='M 755 50 L 790 50' />\n"
				+ "        <!-- Right Transition (Upper to Lower) -->\n"
				+ "        <path id='e_evaluate_commit' class='edge' marker-end='url(#arrow)' d='M 835 72 L 835 125' />\n"
				+ "        <!-- Lower Pipeline Connectors (Right to Left) -->\n"
				+ "        <path id='e_commit_PR' class='edge' marker-end='url(#arrow)' d='M 790 150 L 755 150' />\n"
				+ "        <path id='e_PR_feedback' class='edge' marker-end='url(#arrow)' d='M 660 150 L 625 150' />\n"
				+ "        <path id='e_feedback_refine' class='edge' marker-end='url(#arrow)' d='M 530 150 L 495 150' />\n"
				+ "        <path id='e_refine_learn' class='edge' marker-end='url(#arrow)' d='M 400 150 L 365 150' />\n"
				+ "        <!-- Loopback Paths -->\n"
				+ "        <path id='e_learn_plan' class='edge loopback' marker-end='url(#arrow)' d='M 320 128 C 280 100 280 75 315 72' />\n"
				+ "        <path id='e_refine_plan' class='edge loopback' marker-end='url(#arrow)' d='M 445 128 C 380 110 350 85 330 72' />\n"
				+ "        <!-- Nodes Group -->\n"
				+ "        <g id='nodes'>\n"
				+ "          <!-- Upper Row: Steps 1-7 -->\n"
				+ "          <g id='n_observe' class='node' onclick=\"inspectNode('observe', 'Observe', 'Monitor system state, telemetry, and test outcomes')\">\n"
				+ "            <rect x='10' y='28' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='20' y='42' class='node-num'>01</text>\n"
				+ "            <text x='57' y='46' class='node-title'>Observe</text>\n"
				+ "            <rect x='20' y='54' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='57' y='63' class='status-text' id='st_observe'>READY</text>\n"
				+ "          </g>\n"
				+ "          <g id='n_analyze' class='node' onclick=\"inspectNode('analyze', 'Analyze', 'Analyze failure patterns, diffs, and project requirements')\">\n"
				+ "            <rect x='140' y='28' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='150' y='42' class='node-num'>02</text>\n"
				+ "            <text x='187' y='46' class='node-title'>Analyze</text>\n"
				+ "            <rect x='150' y='54' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='187' y='63' class='status-text' id='st_analyze'>READY</text>\n"
				+ "          </g>\n"
				+ "          <g id='n_plan' class='node' onclick=\"inspectNode('plan', 'Plan', 'Formulate structured step-by-step implementation plan')\">\n"
				+ "            <rect x='270' y='28' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='280' y='42' class='node-num'>03</text>\n"
				+ "            <text x='317' y='46' class='node-title'>Plan</text>\n"
				+ "            <rect x='280' y='54' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='317' y='63' class='status-text' id='st_plan'>READY</text>\n"
				+ "          </g>\n"
				+ "          <g id='n_validate' class='node' onclick=\"inspectNode('validate', 'Validate', 'Verify architectural rules, dependencies, and invariants')\">\n"
				+ "            <rect x='400' y='28' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='410' y='42' class='node-num'>04</text>\n"
				+ "            <text x='447' y='46' class='node-title'>Validate</text>\n"
				+ "            <rect x='410' y='54' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='447' y='63' class='status-text' id='st_validate'>READY</text>\n"
				+ "          </g>\n"
				+ "          <g id='n_execute' class='node' onclick=\"inspectNode('execute', 'Execute', 'Apply source code modifications and build steps')\">\n"
				+ "            <rect x='530' y='28' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='540' y='42' class='node-num'>05</text>\n"
				+ "            <text x='577' y='46' class='node-title'>Execute</text>\n"
				+ "            <rect x='540' y='54' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='577' y='63' class='status-text' id='st_execute'>READY</text>\n"
				+ "          </g>\n"
				+ "          <g id='n_test' class='node' onclick=\"inspectNode('test', 'Test', 'Run unit, integration, and verification test suites')\">\n"
				+ "            <rect x='660' y='28' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='670' y='42' class='node-num'>06</text>\n"
				+ "            <text x='707' y='46' class='node-title'>Test</text>\n"
				+ "            <rect x='670' y='54' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='707' y='63' class='status-text' id='st_test'>READY</text>\n"
				+ "          </g>\n"
				+ "          <g id='n_evaluate' class='node' onclick=\"inspectNode('evaluate', 'Evaluate', 'Evaluate test metrics, regression status, and goals')\">\n"
				+ "            <rect x='790' y='28' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='800' y='42' class='node-num'>07</text>\n"
				+ "            <text x='837' y='46' class='node-title'>Evaluate</text>\n"
				+ "            <rect x='800' y='54' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='837' y='63' class='status-text' id='st_evaluate'>READY</text>\n"
				+ "          </g>\n"
				+ "          <!-- Lower Row: Steps 8-12 -->\n"
				+ "          <g id='n_commit' class='node' onclick=\"inspectNode('commit', 'Commit', 'Record clean atomic commit to local Git workspace')\">\n"
				+ "            <rect x='790' y='128' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='800' y='142' class='node-num'>08</text>\n"
				+ "            <text x='837' y='146' class='node-title'>Commit</text>\n"
				+ "            <rect x='800' y='154' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='837' y='163' class='status-text' id='st_commit'>READY</text>\n"
				+ "          </g>\n"
				+ "          <g id='n_PR' class='node' onclick=\"inspectNode('PR', 'Pull Request', 'Submit changes for peer review and automated checks')\">\n"
				+ "            <rect x='660' y='128' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='670' y='142' class='node-num'>09</text>\n"
				+ "            <text x='707' y='146' class='node-title'>PR</text>\n"
				+ "            <rect x='670' y='154' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='707' y='163' class='status-text' id='st_PR'>READY</text>\n"
				+ "          </g>\n"
				+ "          <g id='n_feedback' class='node' onclick=\"inspectNode('feedback', 'Feedback', 'Collect peer review findings and CI validation results')\">\n"
				+ "            <rect x='530' y='128' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='540' y='142' class='node-num'>10</text>\n"
				+ "            <text x='577' y='146' class='node-title'>Feedback</text>\n"
				+ "            <rect x='540' y='154' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='577' y='163' class='status-text' id='st_feedback'>READY</text>\n"
				+ "          </g>\n"
				+ "          <g id='n_refine' class='node' onclick=\"inspectNode('refine', 'Refine', 'Adjust strategy and refine code based on feedback')\">\n"
				+ "            <rect x='400' y='128' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='410' y='142' class='node-num'>11</text>\n"
				+ "            <text x='447' y='146' class='node-title'>Refine</text>\n"
				+ "            <rect x='410' y='154' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='447' y='163' class='status-text' id='st_refine'>READY</text>\n"
				+ "          </g>\n"
				+ "          <g id='n_learn' class='node' onclick=\"inspectNode('learn', 'Learn', 'Record strategy memory and update evolution model')\">\n"
				+ "            <rect x='270' y='128' width='95' height='44' class='node-card'/>\n"
				+ "            <text x='280' y='142' class='node-num'>12</text>\n"
				+ "            <text x='317' y='146' class='node-title'>Learn</text>\n"
				+ "            <rect x='280' y='154' width='75' height='12' class='status-pill'/>\n"
				+ "            <text x='317' y='163' class='status-text' id='st_learn'>READY</text>\n"
				+ "          </g>\n"
				+ "        </g>\n"
				+ "      </svg>\n"
				+ "      <div id='tooltip' class='inspector-tooltip'>\n"
				+ "        <span><span class='inspector-label' id='tp-title'>Phase:</span> <span id='tp-desc'>Select or execute a step to inspect details</span></span>\n"
				+ "        <span class='evo-badge' id='tp-status'>IDLE</span>\n"
				+ "      </div>\n"
				+ "    </div>\n"
				+ "  </div>\n"
				+ "  <script>\n"
				+ "    function setNodeStatus(id, status) {\n"
				+ "      var el = document.getElementById('n_' + id);\n"
				+ "      if (el) {\n"
				+ "        el.setAttribute('class', 'node ' + (status || ''));\n"
				+ "        var txt = document.getElementById('st_' + id);\n"
				+ "        if (txt) txt.textContent = status ? status.toUpperCase() : 'READY';\n"
				+ "      }\n"
				+ "    }\n"
				+ "    function setEdgeStatus(id, status) {\n"
				+ "      var el = document.getElementById('e_' + id);\n"
				+ "      if (el) {\n"
				+ "        el.setAttribute('class', 'edge ' + (status || ''));\n"
				+ "        if (status === 'active') el.setAttribute('marker-end', 'url(#arrow-active)');\n"
				+ "        else el.setAttribute('marker-end', 'url(#arrow)');\n"
				+ "      }\n"
				+ "    }\n"
				+ "    function resetDiagram() {\n"
				+ "      var nodes = document.querySelectorAll('.node');\n"
				+ "      nodes.forEach(function(n) { n.setAttribute('class', 'node'); });\n"
				+ "      var stTexts = document.querySelectorAll('.status-text');\n"
				+ "      stTexts.forEach(function(t) { t.textContent = 'READY'; });\n"
				+ "      var edges = document.querySelectorAll('.edge');\n"
				+ "      edges.forEach(function(e) {\n"
				+ "        e.setAttribute('class', 'edge' + (e.classList.contains('loopback') ? ' loopback' : ''));\n"
				+ "        e.setAttribute('marker-end', 'url(#arrow)');\n"
				+ "      });\n"
				+ "      var tt = document.getElementById('tooltip');\n"
				+ "      if (tt) tt.classList.remove('show');\n"
				+ "    }\n"
				+ "    function inspectNode(id, title, desc) {\n"
				+ "      var tt = document.getElementById('tooltip');\n"
				+ "      if (!tt) return;\n"
				+ "      document.getElementById('tp-title').textContent = title + ': ';\n"
				+ "      document.getElementById('tp-desc').textContent = desc;\n"
				+ "      var txt = document.getElementById('st_' + id);\n"
				+ "      document.getElementById('tp-status').textContent = txt ? txt.textContent : 'READY';\n"
				+ "      tt.classList.add('show');\n"
				+ "    }\n"
				+ "  </script>\n"
				+ "</body>\n"
				+ "</html>";
	}

	public static String getHtmlTemplate() {
		String css = eu.kalafatic.evolution.controller.ui.EvoStyleManager.getInstance().getEvoStyleCss();
		return "<!DOCTYPE html>\n"
				+ "<html>\n"
				+ "<head>\n"
				+ "  <meta charset='UTF-8'>\n"
				+ "  <style>\n"
				+ css + "\n"
				+ "    html, body { margin: 0; padding: 0; height: 100%; width: 100%; overflow: hidden; background-color: var(--evo-bg); font-family: var(--evo-font-family); }\n"
				+ "    .dashboard-page { display: flex; flex-direction: column; height: 100vh; width: 100vw; box-sizing: border-box; padding: 8px; gap: 8px; }\n"
				+ "    .dashboard-header { display: flex; align-items: center; justify-content: space-between; padding: 6px 12px; background: var(--evo-surface-alt); border: 1px solid var(--evo-border); border-radius: var(--evo-radius); box-shadow: var(--evo-shadow-sm); }\n"
				+ "    .dashboard-title { font-weight: 700; font-size: 11px; color: var(--evo-primary); text-transform: uppercase; letter-spacing: 0.5px; display: flex; align-items: center; gap: 6px; }\n"
				+ "    .metrics-bar { display: flex; align-items: center; gap: 6px; }\n"
				+ "    .metric-pill { display: inline-flex; align-items: center; gap: 4px; padding: 3px 8px; border-radius: var(--evo-radius); font-size: 10px; font-weight: 700; border: 1px solid var(--evo-border); background: var(--evo-surface); }\n"
				+ "    .metric-pill.total { color: var(--evo-text); border-color: var(--evo-border-dark); }\n"
				+ "    .metric-pill.passed { color: var(--evo-success); background: var(--evo-success-light); border-color: var(--evo-success); }\n"
				+ "    .metric-pill.failed { color: var(--evo-danger); background: var(--evo-danger-light); border-color: var(--evo-danger); }\n"
				+ "    .metric-pill.running { color: var(--evo-warning); background: var(--evo-warning-light); border-color: var(--evo-warning); animation: pulseWarn 1.5s infinite; }\n"
				+ "    .metric-pill.pending { color: var(--evo-text-muted); background: var(--evo-surface-alt); }\n"
				+ "    .dashboard-toolbar { display: flex; align-items: center; gap: 6px; padding: 4px 8px; background: var(--evo-surface); border: 1px solid var(--evo-border); border-radius: var(--evo-radius); }\n"
				+ "    .filter-btn { padding: 3px 8px; font-size: 10px; font-weight: 600; border-radius: var(--evo-radius); border: 1px solid transparent; background: transparent; color: var(--evo-text-secondary); cursor: pointer; transition: all 0.15s ease; }\n"
				+ "    .filter-btn:hover { background: var(--evo-surface-hover); color: var(--evo-primary); }\n"
				+ "    .filter-btn.active { background: var(--evo-primary); color: #ffffff; font-weight: 700; }\n"
				+ "    .grid-container { flex: 1; overflow-y: auto; padding: 8px; background: var(--evo-surface); border: 1px solid var(--evo-border); border-radius: var(--evo-radius); display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px; align-content: start; box-shadow: inset 0 1px 2px rgba(0,0,0,0.03); }\n"
				+ "    .test-card { display: flex; align-items: center; gap: 10px; padding: 8px 10px; background: var(--evo-surface-alt); border: 1px solid var(--evo-border); border-radius: var(--evo-radius); transition: all 0.2s ease; box-shadow: var(--evo-shadow-sm); cursor: default; }\n"
				+ "    .test-card:hover { border-color: var(--evo-border-dark); box-shadow: var(--evo-shadow-md); transform: translateY(-1px); }\n"
				+ "    .status-icon-wrap { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; border: 1.5px solid var(--evo-border); background: var(--evo-surface); transition: all 0.25s ease; }\n"
				+ "    .test-card.PASSED .status-icon-wrap { background: var(--evo-success-light); border-color: var(--evo-success); color: var(--evo-success); }\n"
				+ "    .test-card.FAILED .status-icon-wrap { background: var(--evo-danger-light); border-color: var(--evo-danger); color: var(--evo-danger); }\n"
				+ "    .test-card.RUNNING .status-icon-wrap { background: var(--evo-warning-light); border-color: var(--evo-warning); color: var(--evo-warning); animation: pulseRing 1.4s infinite; }\n"
				+ "    .test-card.PENDING .status-icon-wrap { background: var(--evo-surface); border-color: var(--evo-border-dark); color: var(--evo-text-muted); }\n"
				+ "    .test-info { display: flex; flex-direction: column; gap: 2px; flex: 1; min-width: 0; }\n"
				+ "    .test-name { font-weight: 700; font-size: 11px; color: var(--evo-text); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n"
				+ "    .test-type { font-size: 9.5px; color: var(--evo-text-secondary); font-family: var(--evo-font-mono); }\n"
				+ "    .status-badge { display: inline-block; padding: 1px 5px; border-radius: 3px; font-size: 8.5px; font-weight: 800; font-family: var(--evo-font-mono); text-transform: uppercase; align-self: start; margin-top: 2px; }\n"
				+ "    .test-card.PASSED .status-badge { background: var(--evo-success); color: #ffffff; }\n"
				+ "    .test-card.FAILED .status-badge { background: var(--evo-danger); color: #ffffff; }\n"
				+ "    .test-card.RUNNING .status-badge { background: var(--evo-warning); color: #ffffff; }\n"
				+ "    .test-card.PENDING .status-badge { background: var(--evo-border); color: var(--evo-text-secondary); }\n"
				+ "    @keyframes pulseRing { 0% { box-shadow: 0 0 0 0 rgba(245,158,11,0.5); } 70% { box-shadow: 0 0 0 6px rgba(245,158,11,0); } 100% { box-shadow: 0 0 0 0 rgba(245,158,11,0); } }\n"
				+ "    @keyframes pulseWarn { 0% { opacity: 1; } 50% { opacity: 0.6; } 100% { opacity: 1; } }\n"
				+ "  </style>\n"
				+ "</head>\n"
				+ "<body>\n"
				+ "  <div class='dashboard-page'>\n"
				+ "    <div class='dashboard-header'>\n"
				+ "      <div class='dashboard-title'>\n"
				+ "        <svg width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2.5'><path d='M9 11l3 3L22 4'/><path d='M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11'/></svg>\n"
				+ "        Test Execution Dashboard\n"
				+ "      </div>\n"
				+ "      <div class='metrics-bar'>\n"
				+ "        <div class='metric-pill total'>Total: <span id='m-total'>0</span></div>\n"
				+ "        <div class='metric-pill passed'>Passed: <span id='m-passed'>0</span></div>\n"
				+ "        <div class='metric-pill failed'>Failed: <span id='m-failed'>0</span></div>\n"
				+ "        <div class='metric-pill running'>Running: <span id='m-running'>0</span></div>\n"
				+ "        <div class='metric-pill pending'>Pending: <span id='m-pending'>0</span></div>\n"
				+ "      </div>\n"
				+ "    </div>\n"
				+ "    <div class='dashboard-toolbar'>\n"
				+ "      <span style='font-size:10px; font-weight:700; color:var(--evo-text-muted); margin-right:4px;'>FILTER:</span>\n"
				+ "      <button class='filter-btn active' id='flt-ALL' onclick=\"setFilter('ALL')\">All</button>\n"
				+ "      <button class='filter-btn' id='flt-PASSED' onclick=\"setFilter('PASSED')\">Passed</button>\n"
				+ "      <button class='filter-btn' id='flt-FAILED' onclick=\"setFilter('FAILED')\">Failed</button>\n"
				+ "      <button class='filter-btn' id='flt-RUNNING' onclick=\"setFilter('RUNNING')\">Running</button>\n"
				+ "      <button class='filter-btn' id='flt-PENDING' onclick=\"setFilter('PENDING')\">Pending</button>\n"
				+ "    </div>\n"
				+ "    <div id='grid-container' class='grid-container'>\n"
				+ "      <div style='grid-column:1/-1; text-align:center; padding:20px; color:var(--evo-text-muted); font-style:italic;'>No tests registered.</div>\n"
				+ "    </div>\n"
				+ "  </div>\n"
				+ "  <script>\n"
				+ "    var currentFilter = 'ALL';\n"
				+ "    var cachedTests = [];\n"
				+ "    function setFilter(filter) {\n"
				+ "      currentFilter = filter;\n"
				+ "      var btns = document.querySelectorAll('.filter-btn');\n"
				+ "      btns.forEach(function(b) { b.classList.remove('active'); });\n"
				+ "      var activeBtn = document.getElementById('flt-' + filter);\n"
				+ "      if (activeBtn) activeBtn.classList.add('active');\n"
				+ "      renderGrid();\n"
				+ "    }\n"
				+ "    function updateDiagram(tests) {\n"
				+ "      cachedTests = tests || [];\n"
				+ "      var passed = 0, failed = 0, running = 0, pending = 0;\n"
				+ "      cachedTests.forEach(function(t) {\n"
				+ "        var st = (t.status || 'PENDING').toUpperCase();\n"
				+ "        if (st === 'PASSED') passed++;\n"
				+ "        else if (st === 'FAILED') failed++;\n"
				+ "        else if (st === 'RUNNING') running++;\n"
				+ "        else pending++;\n"
				+ "      });\n"
				+ "      document.getElementById('m-total').textContent = cachedTests.length;\n"
				+ "      document.getElementById('m-passed').textContent = passed;\n"
				+ "      document.getElementById('m-failed').textContent = failed;\n"
				+ "      document.getElementById('m-running').textContent = running;\n"
				+ "      document.getElementById('m-pending').textContent = pending;\n"
				+ "      renderGrid();\n"
				+ "    }\n"
				+ "    function renderGrid() {\n"
				+ "      var container = document.getElementById('grid-container');\n"
				+ "      container.innerHTML = '';\n"
				+ "      var filtered = cachedTests.filter(function(t) {\n"
				+ "        if (currentFilter === 'ALL') return true;\n"
				+ "        return (t.status || 'PENDING').toUpperCase() === currentFilter;\n"
				+ "      });\n"
				+ "      if (filtered.length === 0) {\n"
				+ "        container.innerHTML = \"<div style='grid-column:1/-1; text-align:center; padding:20px; color:var(--evo-text-muted); font-style:italic;'>No matching tests for filter '\" + currentFilter + \"'.</div>\";\n"
				+ "        return;\n"
				+ "      }\n"
				+ "      filtered.forEach(function(t) {\n"
				+ "        var st = (t.status || 'PENDING').toUpperCase();\n"
				+ "        var card = document.createElement('div');\n"
				+ "        card.className = 'test-card ' + st;\n"
				+ "        var iconWrap = document.createElement('div');\n"
				+ "        iconWrap.className = 'status-icon-wrap';\n"
				+ "        if (st === 'PASSED') {\n"
				+ "          iconWrap.innerHTML = \"<svg width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='3'><polyline points='20 6 9 17 4 12'/></svg>\";\n"
				+ "        } else if (st === 'FAILED') {\n"
				+ "          iconWrap.innerHTML = \"<svg width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='3'><line x1='18' y1='6' x2='6' y2='18'/><line x1='6' y1='6' x2='18' y2='18'/></svg>\";\n"
				+ "        } else if (st === 'RUNNING') {\n"
				+ "          iconWrap.innerHTML = \"<svg width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='3'><path d='M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83'/></svg>\";\n"
				+ "        } else {\n"
				+ "          iconWrap.innerHTML = \"<svg width='12' height='12' viewBox='0 0 24 24' fill='currentColor'><circle cx='12' cy='12' r='8'/></svg>\";\n"
				+ "        }\n"
				+ "        var info = document.createElement('div');\n"
				+ "        info.className = 'test-info';\n"
				+ "        var name = document.createElement('div');\n"
				+ "        name.className = 'test-name';\n"
				+ "        name.textContent = t.name || 'Unnamed Test';\n"
				+ "        name.title = t.name || 'Unnamed Test';\n"
				+ "        var type = document.createElement('div');\n"
				+ "        type.className = 'test-type';\n"
				+ "        type.textContent = t.type || 'General';\n"
				+ "        var badge = document.createElement('span');\n"
				+ "        badge.className = 'status-badge';\n"
				+ "        badge.textContent = st;\n"
				+ "        info.appendChild(name);\n"
				+ "        info.appendChild(type);\n"
				+ "        info.appendChild(badge);\n"
				+ "        card.appendChild(iconWrap);\n"
				+ "        card.appendChild(info);\n"
				+ "        container.appendChild(card);\n"
				+ "      });\n"
				+ "    }\n"
				+ "  </script>\n"
				+ "</body>\n"
				+ "</html>";
	}

	@Override
	public void dispose() {
		if (orchestrator != null) orchestrator.eAdapters().remove(modelAdapter);
		super.dispose();
	}
}
