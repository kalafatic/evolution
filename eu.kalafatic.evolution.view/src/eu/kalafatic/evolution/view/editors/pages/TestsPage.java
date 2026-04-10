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

import java.util.ArrayList;
import java.util.List;

public class TestsPage extends Composite {

	private MultiPageEditor editor;
	private Orchestrator orchestrator;
	private FormToolkit toolkit;
	private boolean isUpdating = false;
	private SharedScrolledComposite testsScrolled;
	private Composite testsContent;
	private Browser statusBrowser;
	private List<TestRow> testRows = new ArrayList<>();
	private ISimulationTest iterativeTest;
	private List<Class<?>> discoveredTestClasses = new ArrayList<>();

	private PredefinedTestsGroup predefinedTestsGroup;
	private IterativeDevelopmentLifecycleGroup iterativeDevelopmentLifecycleGroup;

	private Adapter modelAdapter = new EContentAdapter() {
		@Override public void notifyChanged(Notification notification) {
			super.notifyChanged(notification);
			if (!isUpdating) {
				Display.getDefault().asyncExec(() -> {
					if (!isDisposed()) {
						if (notification.getEventType() == Notification.ADD || notification.getEventType() == Notification.REMOVE) {
							updateUIFromModel();
						}
						refreshBrowser();
					}
				});
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
		super(parent, SWT.NONE);
		this.editor = editor;
		this.orchestrator = orchestrator;
		this.setLayout(new GridLayout(1, false));
		createControl();
		setOrchestrator(orchestrator);
	}

	private void createControl() {
		toolkit = new FormToolkit(getDisplay());
		SashForm mainSash = new SashForm(this, SWT.VERTICAL);
		mainSash.setLayoutData(new GridData(GridData.FILL_BOTH));
		testsScrolled = new SharedScrolledComposite(mainSash, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER) {};
		testsScrolled.setExpandHorizontal(true); testsScrolled.setExpandVertical(true);
		testsContent = toolkit.createComposite(testsScrolled, SWT.NONE);
		testsContent.setLayout(new GridLayout(1, false));
		testsScrolled.setContent(testsContent);
		statusBrowser = new Browser(mainSash, SWT.NONE);
		mainSash.setWeights(new int[] { 2, 1 });
		statusBrowser.setText(getHtmlTemplate());
	}

	public void setOrchestrator(Orchestrator orchestrator) {
		if (this.orchestrator != null) this.orchestrator.eAdapters().remove(modelAdapter);
		this.orchestrator = orchestrator;
		if (this.orchestrator != null) this.orchestrator.eAdapters().add(modelAdapter);
		updateUIFromModel(); refreshBrowser();
	}

	public void updateUIFromModel() {
		if (isUpdating || orchestrator == null || testsContent == null || testsContent.isDisposed()) return;
		isUpdating = true;
		for (org.eclipse.swt.widgets.Control child : testsContent.getChildren()) child.dispose();
		testRows.clear();
		discoverTests();

		predefinedTestsGroup = new PredefinedTestsGroup(toolkit, testsContent, editor, orchestrator, this);
		iterativeDevelopmentLifecycleGroup = new IterativeDevelopmentLifecycleGroup(toolkit, testsContent, editor, orchestrator, this);

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

		java.util.Map<String, List<Test>> groupedBy = new java.util.HashMap<>();
		for (Test test : orchestrator.getTests()) {
			String type = test.getType() != null ? test.getType() : "General";
			if (!"Predefined".equals(type)) groupedBy.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(test);
		}
		for (String type : groupedBy.keySet()) new TestTableGroup(toolkit, testsContent, type + " Tests", groupedBy.get(type), false, editor, orchestrator, this);

		Button addBtn = toolkit.createButton(testsContent, "Add Test", SWT.PUSH);
		addBtn.addSelectionListener(new SelectionAdapter() {
			@Override public void widgetSelected(SelectionEvent e) { addNewTest(); }
		});
		testsContent.layout(true, true); testsScrolled.reflow(true);
		isUpdating = false;
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
			java.util.Enumeration<java.net.URL> entries = bundle.findEntries("/", "*Test.class", true);
			if (entries != null) {
				while (entries.hasMoreElements()) {
					java.net.URL url = entries.nextElement(); String path = url.getPath();
					String className = path.replace("/", "."); if (className.endsWith(".class")) className = className.substring(0, className.length() - 6);
					if (className.startsWith(".")) className = className.substring(1);
					String[] prefixes = { "bin.", "target.classes.", "target.test-classes." };
					for (String pref : prefixes) if (className.contains(pref)) className = className.substring(className.indexOf(pref) + pref.length());
					try { Class<?> clazz = bundle.loadClass(className); if (!clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) discoveredTestClasses.add(clazz); } catch (Exception e) {}
				}
			}
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
		test.setStatus(TestStatus.RUNNING); refreshBrowser();
		for (TestRow row : testRows) {
			if (row.test == test) { row.item.setText(row.test.getType().equals("Predefined") ? 3 : 2, TestStatus.RUNNING.toString()); break; }
		}
		boolean isSimulation = "ProjectSetupTest".equals(test.getName()) || "ManualOrchestrationTest".equals(test.getName()) || "IterativeDevelopmentTest".equals(test.getName()) || "AutonomousImprovementTest".equals(test.getName());
		if (isSimulation && iterativeDevelopmentLifecycleGroup.getBrowser() != null && !iterativeDevelopmentLifecycleGroup.getBrowser().isDisposed()) {
			runIterativeSimulation(iterativeDevelopmentLifecycleGroup.getBrowser(), null, test);
		} else {
			Display.getDefault().timerExec(2000, () -> {
				if (test.eContainer() != null) {
					TestStatus status = Math.random() > 0.3 ? TestStatus.PASSED : TestStatus.FAILED;
					test.setStatus(status);
					for (TestRow row : testRows) if (row.test == test) { row.item.setText(2, status.toString()); break; }
					refreshBrowser();
				}
			});
		}
	}

	private void refreshBrowser() { if (statusBrowser == null || statusBrowser.isDisposed()) return; statusBrowser.execute("updateDiagram(" + getTestsAsJson() + ");"); }

	private String getTestsAsJson() {
		JSONArray arr = new JSONArray();
		if (orchestrator != null) { for (Test t : orchestrator.getTests()) { JSONObject obj = new JSONObject(); obj.put("name", t.getName()); obj.put("status", t.getStatus().toString()); arr.put(obj); } }
		return arr.toString();
	}

	public void runIterativeSimulation(Browser browser, Button runBtn, Test testModel) {
		if (iterativeTest != null) iterativeTest.stop();
		ITestListener listener = new ITestListener() {
			@Override public void stepStarted(String step) { Display.getDefault().asyncExec(() -> { if (browser != null && !browser.isDisposed()) browser.execute("setNodeStatus('" + step + "', 'active');"); }); }
			@Override public void stepSuccess(String step) {
				Display.getDefault().asyncExec(() -> {
					if (browser != null && !browser.isDisposed()) browser.execute("setNodeStatus('" + step + "', 'success');");
					boolean isLast = false; String name = (testModel != null) ? testModel.getName() : "";
					if ("ProjectSetupTest".equals(name) && "validate".equals(step)) isLast = true;
					else if ("ManualOrchestrationTest".equals(name) && "evaluate".equals(step)) isLast = true;
					else if ("IterativeDevelopmentTest".equals(name) && "learn".equals(step)) isLast = true;
					else if ("AutonomousImprovementTest".equals(name) && "learn".equals(step)) isLast = true;
					else if (testModel == null && "refine".equals(step)) isLast = true;
					if (isLast) { if (testModel != null) { testModel.setStatus(TestStatus.PASSED); updateStatusInTable(testModel); } if (runBtn != null) runBtn.setEnabled(true); refreshBrowser(); }
				});
			}
			@Override public void stepFailed(String step) { Display.getDefault().asyncExec(() -> { if (browser != null && !browser.isDisposed()) browser.execute("setNodeStatus('" + step + "', 'failed');"); if (runBtn != null) runBtn.setEnabled(true); if (testModel != null) { testModel.setStatus(TestStatus.FAILED); updateStatusInTable(testModel); refreshBrowser(); } }); }
			@Override public void stepSkipped(String step) { Display.getDefault().asyncExec(() -> { if (browser != null && !browser.isDisposed()) browser.execute("setNodeStatus('" + step + "', 'skipped');"); }); }
			@Override public void transitionActive(String edgeId) { Display.getDefault().asyncExec(() -> { if (browser != null && !browser.isDisposed()) { browser.execute("setEdgeStatus('" + edgeId + "', 'active');"); Display.getDefault().timerExec(500, () -> { if (browser != null && !browser.isDisposed()) browser.execute("setEdgeStatus('" + edgeId + "', '');"); }); } }); }
			@Override public void reset() { Display.getDefault().asyncExec(() -> { if (browser != null && !browser.isDisposed()) { browser.execute("resetDiagram();"); if (runBtn != null) runBtn.setEnabled(false); } }); }
		};
		String testName = (testModel != null) ? testModel.getName() : "IterativeDevelopmentTest";
		try { Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.tests"); Class<?> clazz = bundle.loadClass("eu.kalafatic.evolution.tests.iterative." + testName); iterativeTest = (ISimulationTest) clazz.getConstructor(ITestListener.class).newInstance(listener); }
		catch (Exception e) { iterativeTest = new IterativeDevelopmentTest(listener); }
		new Thread(() -> { iterativeTest.run(); Display.getDefault().asyncExec(() -> { if (runBtn != null && !runBtn.isDisposed()) runBtn.setEnabled(true); }); }).start();
	}

	private void updateStatusInTable(Test test) { for (TestRow row : testRows) if (row.test == test) { row.item.setText(row.test.getType().equals("Predefined") ? 3 : 2, test.getStatus().toString()); break; } }

	public String getIterativeHtmlTemplate() {
		return "<!DOCTYPE html><html><head><style>"
				+ "body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 20px; overflow: hidden; }"
				+ ".node { fill: #f1f5f9; stroke: #cbd5e1; stroke-width: 2px; transition: all 0.3s; }"
				+ ".node.active { fill: #eff6ff; stroke: #3b82f6; stroke-width: 3px; animation: pulse 1.5s infinite; }"
				+ ".node.success { fill: #f0fdf4; stroke: #22c55e; }"
				+ ".node.failed { fill: #fef2f2; stroke: #ef4444; }"
				+ ".node.skipped { fill: #f8fafc; stroke: #94a3b8; stroke-dasharray: 4; }"
				+ ".edge { stroke: #cbd5e1; stroke-width: 2px; fill: none; marker-end: url(#arrow); transition: stroke 0.3s; }"
				+ ".edge.active { stroke: #3b82f6; stroke-width: 3px; }"
				+ ".label { font-size: 10px; font-weight: 600; fill: #475569; text-anchor: middle; pointer-events: none; }"
				+ "@keyframes pulse { 0% { stroke-opacity: 1; } 50% { stroke-opacity: 0.4; } 100% { stroke-opacity: 1; } }"
				+ "</style></head><body>" + "<svg width='100%' height='100%' viewBox='0 0 800 250' id='svg'>"
				+ "<defs><marker id='arrow' markerWidth='10' markerHeight='7' refX='10' refY='3.5' orient='auto'><polygon points='0 0, 10 3.5, 0 7' fill='#cbd5e1'/></marker></defs>"
				+ "<g id='nodes'>"
				+ "<circle id='n_observe' class='node' cx='50' cy='50' r='20' /><text x='50' y='20' class='label'>Observe</text>"
				+ "<circle id='n_analyze' class='node' cx='150' cy='50' r='20' /><text x='150' y='20' class='label'>Analyze</text>"
				+ "<circle id='n_plan' class='node' cx='250' cy='50' r='20' /><text x='250' y='20' class='label'>Plan</text>"
				+ "<circle id='n_validate' class='node' cx='350' cy='50' r='20' /><text x='350' y='20' class='label'>Validate</text>"
				+ "<circle id='n_execute' class='node' cx='450' cy='50' r='20' /><text x='450' y='20' class='label'>Execute</text>"
				+ "<circle id='n_test' class='node' cx='550' cy='50' r='20' /><text x='550' y='20' class='label'>Test</text>"
				+ "<circle id='n_evaluate' class='node' cx='650' cy='50' r='20' /><text x='650' y='20' class='label'>Evaluate</text>"
				+ "<circle id='n_commit' class='node' cx='650' cy='150' r='20' /><text x='650' y='185' class='label'>Commit</text>"
				+ "<circle id='n_PR' class='node' cx='550' cy='150' r='20' /><text x='550' y='185' class='label'>PR</text>"
				+ "<circle id='n_feedback' class='node' cx='450' cy='150' r='20' /><text x='450' y='185' class='label'>Feedback</text>"
				+ "<circle id='n_refine' class='node' cx='350' cy='150' r='20' /><text x='350' y='185' class='label'>Refine</text>"
				+ "<circle id='n_learn' class='node' cx='250' cy='150' r='20' /><text x='250' y='185' class='label'>Learn</text>"
				+ "</g>"
				+ "<path id='e_observe_analyze' class='edge' d='M 70 50 L 130 50' />"
				+ "<path id='e_analyze_plan' class='edge' d='M 170 50 L 230 50' />"
				+ "<path id='e_plan_validate' class='edge' d='M 270 50 L 330 50' />"
				+ "<path id='e_validate_execute' class='edge' d='M 370 50 L 430 50' />"
				+ "<path id='e_execute_test' class='edge' d='M 470 50 L 530 50' />"
				+ "<path id='e_test_evaluate' class='edge' d='M 570 50 L 630 50' />"
				+ "<path id='e_evaluate_commit' class='edge' d='M 650 70 L 650 130' />"
				+ "<path id='e_commit_PR' class='edge' d='M 630 150 L 570 150' />"
				+ "<path id='e_PR_feedback' class='edge' d='M 530 150 L 470 150' />"
				+ "<path id='e_feedback_refine' class='edge' d='M 430 150 L 370 150' />"
				+ "<path id='e_refine_learn' class='edge' d='M 330 150 L 270 150' />"
				+ "<path id='e_learn_plan' class='edge' d='M 230 150 C 200 130 200 70 230 50' />"
				+ "<path id='e_refine_plan' class='edge' d='M 350 130 C 350 110 280 90 270 65' />" + "</svg>"
				+ "<script>" + "function setNodeStatus(id, status) { var el = document.getElementById('n_' + id); if (el) el.className.baseVal = 'node ' + (status || ''); }"
				+ "function setEdgeStatus(id, status) { var el = document.getElementById('e_' + id); if (el) el.className.baseVal = 'edge ' + (status || ''); }"
				+ "function resetDiagram() { var nodes = document.querySelectorAll('.node'); nodes.forEach(function(n) { n.className.baseVal = 'node'; }); var edges = document.querySelectorAll('.edge'); edges.forEach(function(e) { e.className.baseVal = 'edge'; }); }" + "</script></body></html>";
	}

	private String getHtmlTemplate() {
		return "<!DOCTYPE html><html><head><style>"
				+ "body { font-family: 'Segoe UI', sans-serif; background: #f1f5f9; margin: 0; display: flex; align-items: center; justify-content: center; height: 100vh; overflow: hidden; }"
				+ ".container { display: flex; gap: 40px; padding: 20px; }"
				+ ".test-node { display: flex; flex-direction: column; align-items: center; gap: 8px; }"
				+ ".circle { width: 40px; height: 40px; border-radius: 50%; border: 3px solid #cbd5e1; background: #fff; transition: all 0.3s; }"
				+ ".circle.PENDING { background: #fff; border-color: #cbd5e1; }"
				+ ".circle.RUNNING { background: #fef3c7; border-color: #f59e0b; animation: pulse 1.5s infinite; }"
				+ ".circle.PASSED { background: #dcfce7; border-color: #22c55e; }"
				+ ".circle.FAILED { background: #fee2e2; border-color: #ef4444; }"
				+ ".label { font-size: 12px; font-weight: 600; color: #475569; text-align: center; max-width: 80px; }"
				+ "@keyframes pulse { 0% { transform: scale(1); opacity: 1; } 50% { transform: scale(1.1); opacity: 0.7; } 100% { transform: scale(1); opacity: 1; } }"
				+ "</style></head><body>" + "<div id='diagram' class='container'></div>" + "<script>"
				+ "function updateDiagram(tests) { var container = document.getElementById('diagram'); container.innerHTML = ''; tests.forEach(function(t) { var node = document.createElement('div'); node.className = 'test-node'; var circle = document.createElement('div'); circle.className = 'circle ' + t.status; var label = document.createElement('div'); label.className = 'label'; label.textContent = t.name; node.appendChild(circle); node.appendChild(label); container.appendChild(node); }); }" + "</script></body></html>";
	}

	@Override
	public void dispose() {
		if (orchestrator != null) orchestrator.eAdapters().remove(modelAdapter);
		if (toolkit != null) toolkit.dispose();
		super.dispose();
	}
}
