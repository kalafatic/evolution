package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Test;
import eu.kalafatic.evolution.model.orchestration.TestStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import java.util.ArrayList;
import java.util.List;

public class TestsPage extends Composite {

    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private boolean isUpdating = false;
    private ScrolledComposite testsScrolled;
    private Composite testsContent;
    private Browser statusBrowser;
    private List<TestRow> testRows = new ArrayList<>();

    private Adapter modelAdapter = new EContentAdapter() {
        @Override
        public void notifyChanged(Notification notification) {
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

    private class TestRow {
        Test test;
        Text nameText;
        Text pathText;
        Button executeBtn;

        TestRow(Test test, Text nameText, Text pathText, Button executeBtn) {
            this.test = test;
            this.nameText = nameText;
            this.pathText = pathText;
            this.executeBtn = executeBtn;
        }
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
        SashForm mainSash = new SashForm(this, SWT.VERTICAL);
        mainSash.setLayoutData(new GridData(GridData.FILL_BOTH));

        testsScrolled = new ScrolledComposite(mainSash, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        testsScrolled.setExpandHorizontal(true);
        testsScrolled.setExpandVertical(true);

        testsContent = new Composite(testsScrolled, SWT.NONE);
        testsContent.setLayout(new GridLayout(1, false));
        testsScrolled.setContent(testsContent);

        statusBrowser = new Browser(mainSash, SWT.NONE);

        mainSash.setWeights(new int[] { 2, 1 });

        statusBrowser.setText(getHtmlTemplate());
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().remove(modelAdapter);
        }
        this.orchestrator = orchestrator;
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().add(modelAdapter);
        }
        updateUIFromModel();
        refreshBrowser();
    }

    private void updateUIFromModel() {
        if (isUpdating || orchestrator == null || testsContent == null || testsContent.isDisposed()) return;
        isUpdating = true;

        for (org.eclipse.swt.widgets.Control child : testsContent.getChildren()) {
            child.dispose();
        }
        testRows.clear();

        // Use a SashForm to allow maximizable groups
        SashForm groupsSash = new SashForm(testsContent, SWT.VERTICAL);
        groupsSash.setLayoutData(new GridData(GridData.FILL_BOTH));

        createIterativeDevelopmentGroup(groupsSash);

        java.util.Map<String, List<Test>> groupedBy = new java.util.HashMap<>();
        for (Test test : orchestrator.getTests()) {
            String type = test.getType() != null ? test.getType() : "General";
            groupedBy.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(test);
        }

        ModifyListener ml = e -> {
            if (!isUpdating) {
                updateModelFromFields();
                editor.setDirty(true);
            }
        };

        for (String type : groupedBy.keySet()) {
            Group group = SWTFactory.createMaximizableGroup(groupsSash, type + " Tests", 3);
            for (Test test : groupedBy.get(type)) {
                SWTFactory.createLabel(group, test.getName() != null ? test.getName() : "New Test");
                Text pathText = SWTFactory.createText(group);
                pathText.setText(test.getPath() != null ? test.getPath() : "");
                pathText.addModifyListener(ml);

                Button execBtn = SWTFactory.createButton(group, "Execute");
                execBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
                    @Override
                    public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                        executeTest(test);
                    }
                });

                testRows.add(new TestRow(test, null, pathText, execBtn));
            }
        }

        Button addBtn = SWTFactory.createButton(testsContent, "Add Test");
        addBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                addNewTest();
            }
        });

        testsContent.layout(true, true);
        testsScrolled.setMinSize(testsContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        isUpdating = false;
    }

    private void addNewTest() {
        Test newTest = OrchestrationFactory.eINSTANCE.createTest();
        newTest.setName("New Test " + (orchestrator.getTests().size() + 1));
        newTest.setStatus(TestStatus.PENDING);
        orchestrator.getTests().add(newTest);
        editor.setDirty(true);
    }

    private void updateModelFromFields() {
        if (orchestrator == null) return;
        for (TestRow row : testRows) {
            row.test.setPath(row.pathText.getText());
        }
    }

    private void executeTest(Test test) {
        test.setStatus(TestStatus.RUNNING);
        refreshBrowser();

        // Mock execution
        Display.getDefault().timerExec(2000, () -> {
            if (test.eContainer() != null) {
                test.setStatus(Math.random() > 0.3 ? TestStatus.PASSED : TestStatus.FAILED);
                refreshBrowser();
            }
        });
    }

    private void refreshBrowser() {
        if (statusBrowser == null || statusBrowser.isDisposed()) return;
        String json = getTestsAsJson();
        statusBrowser.execute("updateDiagram(" + json + ");");
    }

    private String getTestsAsJson() {
        JSONArray arr = new JSONArray();
        if (orchestrator != null) {
            for (Test t : orchestrator.getTests()) {
                JSONObject obj = new JSONObject();
                obj.put("name", t.getName());
                obj.put("status", t.getStatus().toString());
                arr.put(obj);
            }
        }
        return arr.toString();
    }

    private void runIterativeSimulation(Browser browser, Button runBtn) {
        runBtn.setEnabled(false);
        browser.execute("resetDiagram();");
        simulateStep(browser, runBtn, "prompt", 0);
    }

    private void simulateStep(Browser browser, Button runBtn, String step, int iterationCount) {
        if (browser == null || browser.isDisposed()) return;

        browser.execute("setNodeStatus('" + step + "', 'active');");

        int delay = 1000;
        Display.getDefault().timerExec(delay, () -> {
            if (browser.isDisposed()) return;

            String nextStep = null;
            String edgeId = null;

            // Mark current node success before moving on
            browser.execute("setNodeStatus('" + step + "', 'success');");

            switch (step) {
                case "prompt": nextStep = "plan"; edgeId = "prompt_plan"; break;
                case "plan": nextStep = "implement"; edgeId = "plan_implement"; break;
                case "implement": nextStep = "compile"; edgeId = "implement_compile"; break;
                case "compile":
                    if (Math.random() < 0.1) {
                        browser.execute("setNodeStatus('compile', 'failed');");
                        runBtn.setEnabled(true);
                        return;
                    }
                    nextStep = "test"; edgeId = "compile_test"; break;
                case "test":
                    if (Math.random() < 0.15) {
                        browser.execute("setNodeStatus('test', 'failed');");
                        runBtn.setEnabled(true);
                        return;
                    }
                    nextStep = "evaluate"; edgeId = "test_evaluate"; break;
                case "evaluate": nextStep = "iterate"; edgeId = "evaluate_iterate"; break;
                case "iterate":
                    if (iterationCount < 1 && Math.random() < 0.4) {
                        // Loop back to plan
                        browser.execute("setEdgeStatus('iterate_plan', 'active');");
                        Display.getDefault().timerExec(500, () -> {
                            if (!browser.isDisposed()) {
                                browser.execute("setEdgeStatus('iterate_plan', '');");
                                simulateStep(browser, runBtn, "plan", iterationCount + 1);
                            }
                        });
                        return;
                    } else {
                        nextStep = "commit"; edgeId = "iterate_commit";
                    }
                    break;
                case "commit": nextStep = "PR"; edgeId = "commit_PR"; break;
                case "PR": nextStep = "feedback"; edgeId = "PR_feedback"; break;
                case "feedback": nextStep = "refine"; edgeId = "feedback_refine"; break;
                case "refine":
                    runBtn.setEnabled(true);
                    return;
            }

            if (nextStep != null) {
                if (edgeId != null) {
                    browser.execute("setEdgeStatus('" + edgeId + "', 'active');");
                    final String finalEdgeId = edgeId;
                    final String finalNextStep = nextStep;
                    Display.getDefault().timerExec(500, () -> {
                        if (!browser.isDisposed()) {
                            browser.execute("setEdgeStatus('" + finalEdgeId + "', '');");
                            simulateStep(browser, runBtn, finalNextStep, iterationCount);
                        }
                    });
                } else {
                    simulateStep(browser, runBtn, nextStep, iterationCount);
                }
            }
        });
    }

    private void createIterativeDevelopmentGroup(Composite parent) {
        Group group = SWTFactory.createMaximizableGroup(parent, "Iterative Development Lifecycle", 1);
        group.setLayout(new GridLayout(2, false));

        Button runBtn = SWTFactory.createButton(group, "Run Lifecycle Simulation", 180);

        Browser iterativeBrowser = new Browser(group, SWT.NONE);
        GridData browserGD = new GridData(GridData.FILL_BOTH);
        browserGD.heightHint = 250;
        browserGD.horizontalSpan = 2;
        iterativeBrowser.setLayoutData(browserGD);
        iterativeBrowser.setText(getIterativeHtmlTemplate());

        runBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                runIterativeSimulation(iterativeBrowser, runBtn);
            }
        });
    }

    private String getIterativeHtmlTemplate() {
        return "<!DOCTYPE html><html><head><style>"
                + "body { font-family: 'Segoe UI', sans-serif; background: #f8fafc; margin: 0; padding: 20px; overflow: hidden; }"
                + ".node { fill: #f1f5f9; stroke: #cbd5e1; stroke-width: 2px; transition: all 0.3s; }"
                + ".node.active { fill: #eff6ff; stroke: #3b82f6; stroke-width: 3px; animation: pulse 1.5s infinite; }"
                + ".node.success { fill: #f0fdf4; stroke: #22c55e; }"
                + ".node.failed { fill: #fef2f2; stroke: #ef4444; }"
                + ".edge { stroke: #cbd5e1; stroke-width: 2px; fill: none; marker-end: url(#arrow); transition: stroke 0.3s; }"
                + ".edge.active { stroke: #3b82f6; stroke-width: 3px; }"
                + ".label { font-size: 10px; font-weight: 600; fill: #475569; text-anchor: middle; pointer-events: none; }"
                + "@keyframes pulse { 0% { stroke-opacity: 1; } 50% { stroke-opacity: 0.4; } 100% { stroke-opacity: 1; } }"
                + "</style></head><body>"
                + "<svg width='100%' height='100%' viewBox='0 0 800 250' id='svg'>"
                + "<defs><marker id='arrow' markerWidth='10' markerHeight='7' refX='10' refY='3.5' orient='auto'><polygon points='0 0, 10 3.5, 0 7' fill='#cbd5e1'/></marker></defs>"
                // Nodes
                + "<g id='nodes'>"
                + "<circle id='n_prompt' class='node' cx='50' cy='50' r='20' /><text x='50' y='85' class='label'>Prompt</text>"
                + "<circle id='n_plan' class='node' cx='150' cy='50' r='20' /><text x='150' y='85' class='label'>Plan</text>"
                + "<circle id='n_implement' class='node' cx='250' cy='50' r='20' /><text x='250' y='85' class='label'>Implement</text>"
                + "<circle id='n_compile' class='node' cx='350' cy='50' r='20' /><text x='350' y='85' class='label'>Compile</text>"
                + "<circle id='n_test' class='node' cx='450' cy='50' r='20' /><text x='450' y='85' class='label'>Test</text>"
                + "<circle id='n_evaluate' class='node' cx='550' cy='50' r='20' /><text x='550' y='85' class='label'>Evaluate</text>"
                + "<circle id='n_iterate' class='node' cx='550' cy='150' r='20' /><text x='550' y='185' class='label'>Iterate</text>"
                + "<circle id='n_commit' class='node' cx='450' cy='150' r='20' /><text x='450' y='185' class='label'>Commit</text>"
                + "<circle id='n_PR' class='node' cx='350' cy='150' r='20' /><text x='350' y='185' class='label'>PR</text>"
                + "<circle id='n_feedback' class='node' cx='250' cy='150' r='20' /><text x='250' y='185' class='label'>Feedback</text>"
                + "<circle id='n_refine' class='node' cx='150' cy='150' r='20' /><text x='150' y='185' class='label'>Refine</text>"
                + "</g>"
                // Edges
                + "<path id='e_prompt_plan' class='edge' d='M 70 50 L 130 50' />"
                + "<path id='e_plan_implement' class='edge' d='M 170 50 L 230 50' />"
                + "<path id='e_implement_compile' class='edge' d='M 270 50 L 330 50' />"
                + "<path id='e_compile_test' class='edge' d='M 370 50 L 430 50' />"
                + "<path id='e_test_evaluate' class='edge' d='M 470 50 L 530 50' />"
                + "<path id='e_evaluate_iterate' class='edge' d='M 550 70 L 550 130' />"
                + "<path id='e_iterate_plan' class='edge' d='M 530 150 C 400 220 200 220 150 70' />" // Loop back to plan
                + "<path id='e_iterate_commit' class='edge' d='M 530 150 L 470 150' />"
                + "<path id='e_commit_PR' class='edge' d='M 430 150 L 370 150' />"
                + "<path id='e_PR_feedback' class='edge' d='M 330 150 L 270 150' />"
                + "<path id='e_feedback_refine' class='edge' d='M 230 150 L 170 150' />"
                + "</svg>"
                + "<script>"
                + "function setNodeStatus(id, status) {"
                + "  var el = document.getElementById('n_' + id);"
                + "  if (el) el.className.baseVal = 'node ' + (status || '');"
                + "}"
                + "function setEdgeStatus(id, status) {"
                + "  var el = document.getElementById('e_' + id);"
                + "  if (el) el.className.baseVal = 'edge ' + (status || '');"
                + "}"
                + "function resetDiagram() {"
                + "  var nodes = document.querySelectorAll('.node');"
                + "  nodes.forEach(function(n) { n.className.baseVal = 'node'; });"
                + "  var edges = document.querySelectorAll('.edge');"
                + "  edges.forEach(function(e) { e.className.baseVal = 'edge'; });"
                + "}"
                + "</script></body></html>";
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
                + "</style></head><body>"
                + "<div id='diagram' class='container'></div>"
                + "<script>"
                + "function updateDiagram(tests) {"
                + "  var container = document.getElementById('diagram');"
                + "  container.innerHTML = '';"
                + "  tests.forEach(function(t) {"
                + "    var node = document.createElement('div');"
                + "    node.className = 'test-node';"
                + "    var circle = document.createElement('div');"
                + "    circle.className = 'circle ' + t.status;"
                + "    var label = document.createElement('div');"
                + "    label.className = 'label';"
                + "    label.textContent = t.name;"
                + "    node.appendChild(circle);"
                + "    node.appendChild(label);"
                + "    container.appendChild(node);"
                + "  });"
                + "}"
                + "</script></body></html>";
    }

    @Override
    public void dispose() {
        if (orchestrator != null) {
            orchestrator.eAdapters().remove(modelAdapter);
        }
        super.dispose();
    }
}
