package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.DesignExporter;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.mediation.analysis.MetadataGenerator;
import eu.kalafatic.evolution.controller.orchestration.design.DesignRenderer;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * @evo:19:A reason=dynamic-architecture-page
 */
public class ArchitecturePage extends Composite {
    private Browser browser;
    private Orchestrator orchestrator;
    private MultiPageEditor editor;
    private DesignRenderer renderer = new DesignRenderer();
    private Runnable refreshRunnable = this::refreshBrowser;

    private Adapter modelAdapter = new EContentAdapter() {
        @Override
        public void notifyChanged(Notification notification) {
            super.notifyChanged(notification);
            if (notification.isTouch()) return;
            scheduleRefresh();
        }
    };

    public ArchitecturePage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.NONE);
        this.editor = editor;
        this.setLayout(new GridLayout(1, false));

        createControlPanel();

        this.browser = new Browser(this, SWT.NONE);
        this.browser.setLayoutData(new GridData(GridData.FILL_BOTH));

        setOrchestrator(orchestrator);
        refreshBrowser();
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().remove(modelAdapter);
        }
        this.orchestrator = orchestrator;
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().add(modelAdapter);
        }
    }

    public void scheduleRefresh() {
        Display.getDefault().asyncExec(() -> {
            if (isDisposed()) return;
            Display.getDefault().timerExec(-1, refreshRunnable); // Cancel previous
            Display.getDefault().timerExec(500, refreshRunnable); // Debounce
        });
    }

    private void createControlPanel() {
        Composite panel = new Composite(this, SWT.NONE);
        panel.setLayout(new GridLayout(2, false));
        panel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button exportBtn = new Button(panel, SWT.PUSH);
        exportBtn.setText("Export Architecture (HTML)");
        exportBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleExport();
            }
        });

        Button saveBtn = new Button(panel, SWT.PUSH);
        saveBtn.setText("Save Design Model (JSON)");
        saveBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleSaveModel();
            }
        });

        Group metaGroup = new Group(panel, SWT.NONE);
        metaGroup.setText("Metadata");
        metaGroup.setLayout(new GridLayout(1, false));
        metaGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Button generateMetaBtn = new Button(metaGroup, SWT.PUSH);
        generateMetaBtn.setText("Generate AI Metadata");
        generateMetaBtn.setToolTipText("Generate .ai.json sidecar files for this project");
        generateMetaBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                handleGenerateMetadata();
            }
        });
    }

    private void handleGenerateMetadata() {
        if (editor == null) return;
        org.eclipse.ui.IEditorInput input = editor.getEditorInput();
        if (input instanceof org.eclipse.ui.IFileEditorInput) {
            org.eclipse.core.resources.IProject project = ((org.eclipse.ui.IFileEditorInput) input).getFile().getProject();
            java.io.File root = project.getLocation().toFile();

            MetadataGenerator generator = new MetadataGenerator();
            generator.generate(root);

            MessageBox box = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
            box.setText("Metadata Generation");
            box.setMessage("AI Metadata generation completed for: " + project.getName());
            box.open();
        }
    }

    private void handleExport() {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { "*.html" });
        dialog.setFileName("architecture.html");
        String path = dialog.open();
        if (path != null) {
            try {
                DesignModel model = extractModel();
                eu.kalafatic.evolution.controller.orchestration.TaskContext context = new eu.kalafatic.evolution.controller.orchestration.TaskContext(orchestrator, null);
                DesignExporter.exportToHtml(renderer.render(model), new java.io.File(path), context);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void handleSaveModel() {
        FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
        dialog.setFilterExtensions(new String[] { "*.json" });
        dialog.setFileName("design_model.json");
        String path = dialog.open();
        if (path != null) {
            try {
                eu.kalafatic.evolution.controller.orchestration.TaskContext context = new eu.kalafatic.evolution.controller.orchestration.TaskContext(orchestrator, null);
                DesignExporter.saveModelAsJson(orchestrator.getSharedMemory(), new java.io.File(path), context);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void refreshBrowser() {
        if (browser == null || browser.isDisposed()) return;
        Display.getDefault().asyncExec(() -> {
            if (browser == null || browser.isDisposed()) return;
            DesignModel model = extractModel();
            browser.setText(renderer.render(model));
        });
    }

    private DesignModel extractModel() {
        DesignModel model = new DesignModel();
        if (orchestrator == null) return model;

        String jsonStr = orchestrator.getSharedMemory();
        if (jsonStr == null || jsonStr.isEmpty() || !jsonStr.trim().startsWith("{")) {
            return createDefaultModel();
        }

        try {
            JSONObject root = new JSONObject(jsonStr);
            if (root.has("architecture")) {
                JSONObject arch = root.getJSONObject("architecture");
                model.setName(arch.optString("name", "Evolution Architecture"));

                if (arch.has("components")) {
                    JSONArray comps = arch.getJSONArray("components");
                    for (int i = 0; i < comps.length(); i++) {
                        JSONObject c = comps.getJSONObject(i);
                        ComponentRecord rec = new ComponentRecord();
                        rec.setName(c.getString("name"));
                        rec.setType(c.optString("type", "Component"));
                        rec.setX(c.optInt("x", 50 + (i * 200) % 800));
                        rec.setY(c.optInt("y", 50 + (i / 4) * 150));
                        model.getComponents().add(rec);
                    }
                }

                if (arch.has("relationships")) {
                    JSONArray rels = arch.getJSONArray("relationships");
                    for (int i = 0; i < rels.length(); i++) {
                        JSONObject r = rels.getJSONObject(i);
                        RelationshipRecord rec = new RelationshipRecord();
                        rec.setFrom(r.getString("from"));
                        rec.setTo(r.getString("to"));
                        rec.setType(r.optString("type", "link"));
                        model.getRelationships().add(rec);
                    }
                }
                return model;
            }
        } catch (Exception e) {
            // Fallback to default
        }
        return createDefaultModel();
    }

    private DesignModel createDefaultModel() {
        DesignModel model = new DesignModel();

        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            eu.kalafatic.evolution.model.orchestration.SelfDevSession session = orchestrator.getSelfDevSession();
            model.setName("Self-Development Session: " + (session.getId() != null ? session.getId() : "Active"));

            int i = 0;
            for (eu.kalafatic.evolution.model.orchestration.Iteration iter : session.getIterations()) {
                ComponentRecord comp = new ComponentRecord();
                comp.setName(iter.getId());
                comp.setType("Step");
                comp.setX(100 + (i * 250) % 750);
                comp.setY(100 + (i / 3) * 200);
                model.getComponents().add(comp);

                if (i > 0) {
                    RelationshipRecord rel = new RelationshipRecord();
                    rel.setFrom(session.getIterations().get(i - 1).getId());
                    rel.setTo(iter.getId());
                    rel.setType("evolves");
                    model.getRelationships().add(rel);
                }
                i++;
            }
            if (model.getComponents().isEmpty()) {
                ComponentRecord comp = new ComponentRecord();
                comp.setName("Session Active");
                comp.setType("Status");
                comp.setX(100); comp.setY(100);
                model.getComponents().add(comp);
            }
            return model;
        }

        if (orchestrator != null && !orchestrator.getTasks().isEmpty()) {
            model.setName("Active Session Tasks");
            int i = 0;
            boolean hasClassTasks = orchestrator.getTasks().stream()
                .anyMatch(t -> t.getName().toLowerCase().contains("class") || t.getName().toLowerCase().contains("interface"));

            for (eu.kalafatic.evolution.model.orchestration.Task task : orchestrator.getTasks()) {
                if (!hasClassTasks || task.getName().toLowerCase().contains("class") || task.getName().toLowerCase().contains("interface")) {
                    ComponentRecord comp = new ComponentRecord();
                    String name = task.getName().replace("Create", "").replace("create", "").replace("class", "").replace("java", "").replace(".", "").trim();
                    if (name.isEmpty()) name = task.getId() != null ? task.getId() : "Task" + i;
                    comp.setName(name);
                    comp.setType(hasClassTasks ? "Class" : "Task");
                    comp.setX(100 + (i * 220) % 660);
                    comp.setY(100 + (i / 3) * 180);

                    if (task.getDescription() != null) {
                        String desc = task.getDescription();
                        if (desc.contains("method") || desc.contains("(")) {
                            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\w+)\\s*\\(").matcher(desc);
                            while (m.find()) {
                                if (!m.group(1).equalsIgnoreCase("create")) comp.getMethods().add(m.group(1) + "()");
                            }
                        }
                    }

                    model.getComponents().add(comp);
                    i++;
                }
            }
            if (!model.getComponents().isEmpty()) return model;
        }

        model.setName("Darwinian Evolution (Default)");

        ComponentRecord engine = new ComponentRecord();
        engine.setName("DarwinEngine"); engine.setType("Engine"); engine.setX(100); engine.setY(100);

        ComponentRecord mdm = new ComponentRecord();
        mdm.setName("DesignModel"); mdm.setType("Data"); mdm.setX(400); mdm.setY(100);

        RelationshipRecord rel = new RelationshipRecord();
        rel.setFrom("DarwinEngine"); rel.setTo("DesignModel"); rel.setType("manages");

        model.getComponents().add(engine);
        model.getComponents().add(mdm);
        model.getRelationships().add(rel);

        return model;
    }

    @Override
    public void dispose() {
        if (orchestrator != null) {
            orchestrator.eAdapters().remove(modelAdapter);
        }
        super.dispose();
    }
}
