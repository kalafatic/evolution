package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.Bundle;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Test;
import eu.kalafatic.evolution.model.orchestration.TestStatus;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.tests.iterative.ITestListener;
import eu.kalafatic.evolution.tests.iterative.IterativeDevelopmentTest;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.factories.SWTFactory;

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
    private Browser iterativeBrowser;
    private List<TestRow> testRows = new ArrayList<>();
    private IterativeDevelopmentTest iterativeTest;
    private List<Class<?>> discoveredTestClasses = new ArrayList<>();

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
        Button executeBtn;
        TableItem item;

        TestRow(Test test, Button executeBtn, TableItem item) {
            this.test = test;
            this.executeBtn = executeBtn;
            this.item = item;
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
        toolkit = new FormToolkit(getDisplay());
        SashForm mainSash = new SashForm(this, SWT.VERTICAL);
        mainSash.setLayoutData(new GridData(GridData.FILL_BOTH));

        testsScrolled = new SharedScrolledComposite(mainSash, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER) {
        };
        testsScrolled.setExpandHorizontal(true);
        testsScrolled.setExpandVertical(true);

        testsContent = toolkit.createComposite(testsScrolled, SWT.NONE);
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

        discoverTests();

        createPredefinedTestsGroup(testsContent);
        createIterativeDevelopmentGroup(testsContent);

        java.util.Map<String, List<Test>> groupedBy = new java.util.HashMap<>();
        for (Test test : orchestrator.getTests()) {
            String type = test.getType() != null ? test.getType() : "General";
            if (!"Predefined".equals(type)) {
                groupedBy.computeIfAbsent(type, k -> new java.util.ArrayList<>()).add(test);
            }
        }

        for (String type : groupedBy.keySet()) {
            createTestTableGroup(testsContent, type + " Tests", groupedBy.get(type), false);
        }

        Button addBtn = toolkit.createButton(testsContent, "Add Test", SWT.PUSH);
        addBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addNewTest();
            }
        });

        testsContent.layout(true, true);
        testsScrolled.reflow(true);

        isUpdating = false;
    }

    private void createTestTableGroup(Composite parent, String title, List<Test> tests, boolean expanded) {
        Composite groupContainer = SWTFactory.createExpandableGroup(toolkit, parent, title, 1, expanded);

        Composite tableComposite = toolkit.createComposite(groupContainer);
        tableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        TableColumnLayout layout = new TableColumnLayout();
        tableComposite.setLayout(layout);

        Table table = toolkit.createTable(tableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        addColumn(table, layout, "Name", 150, 30);
        addColumn(table, layout, "Path", 250, 40);
        addColumn(table, layout, "Status", 100, 15);
        addColumn(table, layout, "Execute", 100, 15);

        for (final Test test : tests) {
            final TableItem item = new TableItem(table, SWT.NONE);
            updateTableItem(item, test);

            TableEditor execEditor = new TableEditor(table);
            Button execBtn = toolkit.createButton(table, "Execute", SWT.PUSH);
            execBtn.pack();
            execEditor.minimumWidth = execBtn.getSize().x;
            execEditor.horizontalAlignment = SWT.CENTER;
            execEditor.setEditor(execBtn, item, 3);
            execBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    executeTest(test);
                }
            });
            testRows.add(new TestRow(test, execBtn, item));
        }

        GridData groupGd = (GridData) groupContainer.getLayoutData();
        groupGd.heightHint = 200;
    }

    private void addColumn(Table table, TableColumnLayout layout, String text, int width, int weight) {
        TableColumn col = new TableColumn(table, SWT.NONE);
        col.setText(text);
        layout.setColumnData(col, new ColumnWeightData(weight, width, true));
    }

    private void updateTableItem(TableItem item, Test test) {
        item.setText(0, test.getName() != null ? test.getName() : "New Test");
        item.setText(1, test.getPath() != null ? test.getPath() : "");
        item.setText(2, test.getStatus().toString());
    }

    private void discoverTests() {
        if (!discoveredTestClasses.isEmpty()) return;
        Bundle bundle = Platform.getBundle("eu.kalafatic.evolution.tests");
        if (bundle != null) {
            java.util.Enumeration<java.net.URL> entries = bundle.findEntries("/", "*Test.class", true);
            if (entries != null) {
                while (entries.hasMoreElements()) {
                    java.net.URL url = entries.nextElement();
                    String path = url.getPath();
                    String className = path.replace("/", ".");
                    if (className.endsWith(".class")) className = className.substring(0, className.length() - 6);
                    if (className.startsWith(".")) className = className.substring(1);

                    String[] prefixes = {"bin.", "target.classes.", "target.test-classes."};
                    for (String pref : prefixes) {
                        if (className.contains(pref)) {
                            className = className.substring(className.indexOf(pref) + pref.length());
                        }
                    }

                    try {
                        Class<?> clazz = bundle.loadClass(className);
                        if (!clazz.isInterface() && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                            discoveredTestClasses.add(clazz);
                        }
                    } catch (Exception e) {}
                }
            }
        }
        if (discoveredTestClasses.isEmpty()) {
            try {
                discoveredTestClasses.add(bundle.loadClass("eu.kalafatic.evolution.tests.iterative.IterativeDevelopmentTest"));
            } catch (Exception e) {}
        }
    }

    private void createPredefinedTestsGroup(Composite parent) {
        Composite container = SWTFactory.createExpandableGroup(toolkit, parent, "Predefined Tests", 1, true);

        Composite tableComposite = toolkit.createComposite(container);
        tableComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
        TableColumnLayout layout = new TableColumnLayout();
        tableComposite.setLayout(layout);

        final Table table = toolkit.createTable(tableComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        addColumn(table, layout, "Sel", 40, 5);
        addColumn(table, layout, "Name", 150, 25);
        addColumn(table, layout, "Path", 250, 40);
        addColumn(table, layout, "Status", 100, 15);
        addColumn(table, layout, "Actions", 150, 15);

        for (Class<?> testClass : discoveredTestClasses) {
            String name = testClass.getSimpleName();
            Test existing = null;
            for (Test t : orchestrator.getTests()) {
                if (name.equals(t.getName()) && "Predefined".equals(t.getType())) {
                    existing = t;
                    break;
                }
            }

            if (existing == null) {
                existing = OrchestrationFactory.eINSTANCE.createTest();
                existing.setName(name);
                existing.setType("Predefined");
                existing.setStatus(TestStatus.PENDING);
                isUpdating = true;
                orchestrator.getTests().add(existing);
                isUpdating = false;
            }

            final Test finalTest = existing;
            final TableItem item = new TableItem(table, SWT.NONE);
            item.setText(1, finalTest.getName());
            item.setText(2, finalTest.getPath() != null ? finalTest.getPath() : "");
            item.setText(3, finalTest.getStatus().toString());

            TableEditor selEditor = new TableEditor(table);
            final Button radio = new Button(table, SWT.RADIO);
            radio.setSelection(finalTest.isSelected());
            radio.pack();
            selEditor.minimumWidth = radio.getSize().x;
            selEditor.horizontalAlignment = SWT.CENTER;
            selEditor.setEditor(radio, item, 0);
            radio.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (radio.getSelection()) handleTestSelection(finalTest);
                }
            });

            TableEditor actionEditor = new TableEditor(table);
            Composite actionComp = toolkit.createComposite(table);
            GridLayout actionLayout = new GridLayout(2, false);
            actionLayout.marginHeight = 0; actionLayout.marginWidth = 0;
            actionComp.setLayout(actionLayout);

            Button editBtn = toolkit.createButton(actionComp, "Edit", SWT.PUSH);
            editBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                   // Placeholder for edit dialog
                }
            });

            Button execBtn = toolkit.createButton(actionComp, "Execute", SWT.PUSH);
            execBtn.setEnabled(finalTest.isSelected());
            execBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    executeTest(finalTest);
                }
            });

            actionComp.pack();
            actionEditor.minimumWidth = actionComp.getSize().x;
            actionEditor.setEditor(actionComp, item, 4);

            testRows.add(new TestRow(finalTest, execBtn, item));
        }

        GridData containerGd = (GridData) container.getLayoutData();
        containerGd.heightHint = 250;
    }

    private void addNewTest() {
        discoverTests();
        ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new LabelProvider() {
            @Override
            public String getText(Object element) {
                return ((Class<?>) element).getSimpleName();
            }
        });
        dialog.setTitle("Select Test to Add");
        dialog.setMessage("Select a test from the available test modules:");
        dialog.setElements(discoveredTestClasses.toArray());

        if (dialog.open() == org.eclipse.jface.window.Window.OK) {
            Object[] result = dialog.getResult();
            for (Object obj : result) {
                Class<?> testClass = (Class<?>) obj;
                String name = testClass.getSimpleName();

                boolean exists = false;
                for (Test t : orchestrator.getTests()) {
                    if (name.equals(t.getName())) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    Test newTest = OrchestrationFactory.eINSTANCE.createTest();
                    newTest.setName(name);
                    newTest.setType("General");
                    newTest.setStatus(TestStatus.PENDING);
                    orchestrator.getTests().add(newTest);
                    editor.setDirty(true);
                }
            }
            updateUIFromModel();
        }
    }

    private void handleTestSelection(Test selected) {
        if (isUpdating) return;
        isUpdating = true;
        for (Test t : orchestrator.getTests()) {
            t.setSelected(t == selected);
        }
        updateUIFromModel();
        isUpdating = false;
        editor.setDirty(true);
    }

    private void executeTest(Test test) {
        test.setStatus(TestStatus.RUNNING);
        refreshBrowser();
        for (TestRow row : testRows) {
            if (row.test == test) {
                row.item.setText(2, TestStatus.RUNNING.toString()); // Update status column
                break;
            }
        }

        if ("IterativeDevelopmentTest".equals(test.getName()) && iterativeBrowser != null && !iterativeBrowser.isDisposed()) {
            runIterativeSimulation(iterativeBrowser, null, test);
        } else {
            Display.getDefault().timerExec(2000, () -> {
                if (test.eContainer() != null) {
                    TestStatus status = Math.random() > 0.3 ? TestStatus.PASSED : TestStatus.FAILED;
                    test.setStatus(status);
                    for (TestRow row : testRows) {
                        if (row.test == test) {
                            row.item.setText(2, status.toString());
                            break;
                        }
                    }
                    refreshBrowser();
                }
            });
        }
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

    private void runIterativeSimulation(Browser browser, Button runBtn, Test testModel) {
        if (iterativeTest != null) {
            iterativeTest.stop();
        }

        iterativeTest = new IterativeDevelopmentTest(new ITestListener() {
            @Override
            public void stepStarted(String step) {
                Display.getDefault().asyncExec(() -> {
                    if (browser != null && !browser.isDisposed()) browser.execute("setNodeStatus('" + step + "', 'active');");
                });
            }

            @Override
            public void stepSuccess(String step) {
                Display.getDefault().asyncExec(() -> {
                    if (browser != null && !browser.isDisposed()) browser.execute("setNodeStatus('" + step + "', 'success');");
                    if ("refine".equals(step)) {
                        if (testModel != null) {
                            testModel.setStatus(TestStatus.PASSED);
                            updateStatusInTable(testModel);
                        }
                        if (runBtn != null) runBtn.setEnabled(true);
                        refreshBrowser();
                    }
                });
            }

            @Override
            public void stepFailed(String step) {
                Display.getDefault().asyncExec(() -> {
                    if (browser != null && !browser.isDisposed()) {
                        browser.execute("setNodeStatus('" + step + "', 'failed');");
                        if (runBtn != null) runBtn.setEnabled(true);
                    }
                    if (testModel != null) {
                        testModel.setStatus(TestStatus.FAILED);
                        updateStatusInTable(testModel);
                        refreshBrowser();
                    }
                });
            }

            @Override
            public void stepSkipped(String step) {
                Display.getDefault().asyncExec(() -> {
                    if (browser != null && !browser.isDisposed()) browser.execute("setNodeStatus('" + step + "', 'skipped');");
                });
            }

            @Override
            public void transitionActive(String edgeId) {
                Display.getDefault().asyncExec(() -> {
                    if (browser != null && !browser.isDisposed()) {
                        browser.execute("setEdgeStatus('" + edgeId + "', 'active');");
                        Display.getDefault().timerExec(500, () -> {
                            if (browser != null && !browser.isDisposed()) browser.execute("setEdgeStatus('" + edgeId + "', '');");
                        });
                    }
                });
            }

            @Override
            public void reset() {
                Display.getDefault().asyncExec(() -> {
                    if (browser != null && !browser.isDisposed()) {
                        browser.execute("resetDiagram();");
                        if (runBtn != null) runBtn.setEnabled(false);
                    }
                });
            }
        });

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

    private void createIterativeDevelopmentGroup(Composite parent) {
        Composite container = SWTFactory.createExpandableGroup(toolkit, parent, "Iterative Development Lifecycle", 1, false);
        container.setLayout(new GridLayout(2, false));

        Button runBtn = toolkit.createButton(container, "Run Lifecycle Simulation", SWT.PUSH);
        GridData btnGd = new GridData();
        btnGd.widthHint = 180;
        runBtn.setLayoutData(btnGd);

        iterativeBrowser = new Browser(container, SWT.NONE);
        GridData browserGD = new GridData(GridData.FILL_BOTH);
        browserGD.heightHint = 250;
        browserGD.horizontalSpan = 2;
        iterativeBrowser.setLayoutData(browserGD);
        iterativeBrowser.setText(getIterativeHtmlTemplate());

        runBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                runIterativeSimulation(iterativeBrowser, runBtn, null);
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
                + ".node.skipped { fill: #f8fafc; stroke: #94a3b8; stroke-dasharray: 4; }"
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
        if (toolkit != null) {
            toolkit.dispose();
        }
        super.dispose();
    }
}
