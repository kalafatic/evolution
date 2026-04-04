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
