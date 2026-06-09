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
import eu.kalafatic.evolution.controller.agents.MetadataAgent;
import eu.kalafatic.evolution.controller.orchestration.design.DesignRenderer;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * @evo:19:A reason=dynamic-architecture-page
 */
public class ArchitecturePage extends AEvoPage {
    private Browser browser;
    private DesignRenderer renderer = new DesignRenderer();
    private Runnable refreshRunnable = this::refreshBrowser;

    public ArchitecturePage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        this.setLayout(new GridLayout(1, false));

        createControlPanel();

        this.browser = new Browser(this, SWT.NONE);
        this.browser.setLayoutData(new GridData(GridData.FILL_BOTH));

        hookContextMenu();
        refreshBrowser();
    }

    @Override
    public void scheduleRefresh() {
        super.scheduleRefresh();
    }

    private void createControlPanel() {
        Composite toolbarComp = new Composite(this, SWT.NONE);
        toolbarComp.setLayout(new org.eclipse.swt.layout.FillLayout());
        toolbarComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        org.eclipse.jface.action.ToolBarManager mgr = new org.eclipse.jface.action.ToolBarManager(SWT.FLAT | SWT.RIGHT);
        mgr.createControl(toolbarComp);

        mgr.add(new org.eclipse.jface.action.Action("Refresh") { @Override public void run() { scheduleRefresh(); } });
        mgr.add(new org.eclipse.jface.action.Separator());
        mgr.add(new org.eclipse.jface.action.Action("Zoom In") { @Override public void run() { if (browser != null) browser.execute("document.body.style.zoom = (parseFloat(document.body.style.zoom || 1) + 0.1);"); } });
        mgr.add(new org.eclipse.jface.action.Action("Zoom Out") { @Override public void run() { if (browser != null) browser.execute("document.body.style.zoom = (parseFloat(document.body.style.zoom || 1) - 0.1);"); } });
        mgr.add(new org.eclipse.jface.action.Action("Reset Zoom") { @Override public void run() { if (browser != null) browser.execute("document.body.style.zoom = 1.0;"); } });
        mgr.add(new org.eclipse.jface.action.Separator());
        mgr.add(new org.eclipse.jface.action.Action("Export HTML") { @Override public void run() { handleExport(); } });
        mgr.add(new org.eclipse.jface.action.Action("Save JSON") { @Override public void run() { handleSaveModel(); } });
        mgr.add(new org.eclipse.jface.action.Separator());
        mgr.add(new org.eclipse.jface.action.Action("Generate Metadata") { @Override public void run() { handleGenerateMetadata(); } });

        mgr.update(true);
    }

    private void hookContextMenu() {
        org.eclipse.jface.action.MenuManager menuMgr = new org.eclipse.jface.action.MenuManager("#PopupMenu");
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(manager -> fillContextMenu(manager));
        org.eclipse.swt.widgets.Menu menu = menuMgr.createContextMenu(browser);
        browser.setMenu(menu);
    }

    private void fillContextMenu(org.eclipse.jface.action.IMenuManager manager) {
        manager.add(new org.eclipse.jface.action.Action("Refresh") { @Override public void run() { scheduleRefresh(); } });
        manager.add(new org.eclipse.jface.action.Separator());
        manager.add(new org.eclipse.jface.action.Action("Zoom In") { @Override public void run() { if (browser != null) browser.execute("document.body.style.zoom = (parseFloat(document.body.style.zoom || 1) + 0.1);"); } });
        manager.add(new org.eclipse.jface.action.Action("Zoom Out") { @Override public void run() { if (browser != null) browser.execute("document.body.style.zoom = (parseFloat(document.body.style.zoom || 1) - 0.1);"); } });
        manager.add(new org.eclipse.jface.action.Action("Reset Zoom") { @Override public void run() { if (browser != null) browser.execute("document.body.style.zoom = 1.0;"); } });
        manager.add(new org.eclipse.jface.action.Separator());
        manager.add(new org.eclipse.jface.action.Action("Export Architecture (HTML)") { @Override public void run() { handleExport(); } });
        manager.add(new org.eclipse.jface.action.Action("Save Design Model (JSON)") { @Override public void run() { handleSaveModel(); } });
    }

    private void handleGenerateMetadata() {
        if (editor == null) return;
        org.eclipse.ui.IEditorInput input = editor.getEditorInput();
        if (input instanceof org.eclipse.ui.IFileEditorInput) {
            org.eclipse.core.resources.IProject project = ((org.eclipse.ui.IFileEditorInput) input).getFile().getProject();
            java.io.File root = project.getLocation().toFile();

            org.eclipse.core.runtime.jobs.Job job = new org.eclipse.core.runtime.jobs.Job("Generating AI Metadata") {
                @Override
                protected org.eclipse.core.runtime.IStatus run(org.eclipse.core.runtime.IProgressMonitor monitor) {
                    try {
                        MetadataAgent generator = new MetadataAgent();
                        generator.generate(root, monitor);

                        Display.getDefault().asyncExec(() -> {
                            if (!getShell().isDisposed()) {
                                MessageBox box = new MessageBox(getShell(), SWT.ICON_INFORMATION | SWT.OK);
                                box.setText("Metadata Generation");
                                box.setMessage("AI Metadata generation completed for: " + project.getName());
                                box.open();
                            }
                        });
                        return org.eclipse.core.runtime.Status.OK_STATUS;
                    } catch (Exception e) {
                        return new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.IStatus.ERROR, "eu.kalafatic.evolution.view", "Failed to generate metadata", e);
                    }
                }
            };
            job.schedule();
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

        model.setName(orchestrator.getName() != null ? orchestrator.getName() : "Evolution Architecture");

        int i = 0;

        // 1. Static Configuration Components
        if (orchestrator.getGit() != null) {
            ComponentRecord git = new ComponentRecord();
            git.setName("Git: " + (orchestrator.getGit().getBranch() != null ? orchestrator.getGit().getBranch() : "master"));
            git.setType("VCS");
            git.setX(50); git.setY(50);
            if (orchestrator.getGit().getRepositoryUrl() != null) git.getProperties().add("URL: " + orchestrator.getGit().getRepositoryUrl());
            model.getComponents().add(git);
        }

        if (orchestrator.getOllama() != null) {
            ComponentRecord ollama = new ComponentRecord();
            ollama.setName("Ollama: " + (orchestrator.getOllama().getModel() != null ? orchestrator.getOllama().getModel() : "local"));
            ollama.setType("LLM Provider");
            ollama.setX(300); ollama.setY(50);
            if (orchestrator.getOllama().getUrl() != null) ollama.getProperties().add("URL: " + orchestrator.getOllama().getUrl());
            model.getComponents().add(ollama);
        }

        if (orchestrator.getLlm() != null) {
            ComponentRecord llm = new ComponentRecord();
            llm.setName("LLM: " + (orchestrator.getLlm().getModel() != null ? orchestrator.getLlm().getModel() : "gpt-4o"));
            llm.setType("Model");
            llm.setX(550); llm.setY(50);
            llm.getProperties().add("Temp: " + orchestrator.getLlm().getTemperature());
            model.getComponents().add(llm);
        }

        i = 2; // Offset for agents/tasks

        // 2. Agents as components
        for (eu.kalafatic.evolution.model.orchestration.Agent agent : orchestrator.getAgents()) {
            ComponentRecord rec = new ComponentRecord();
            rec.setName(agent.getId());
            rec.setType(agent.getType() != null ? agent.getType() : "Agent");
            rec.setX(50 + (i * 220) % 880);
            rec.setY(250 + (i / 4) * 200);
            model.getComponents().add(rec);
            i++;
        }

        // 3. Tasks as components
        for (eu.kalafatic.evolution.model.orchestration.Task task : orchestrator.getTasks()) {
            ComponentRecord rec = new ComponentRecord();
            String taskName = task.getName() != null ? task.getName() : (task.getId() != null ? task.getId() : "Task " + task.hashCode());
            rec.setName(taskName);
            rec.setType("Task");
            rec.setX(50 + (i * 220) % 880);
            rec.setY(250 + (i / 4) * 200);

            if (task.getStatus() != null) {
                rec.getProperties().add("Status: " + task.getStatus().toString());
            }
            if (task.getRating() > 0) rec.getProperties().add("Rating: " + task.getRating());

            model.getComponents().add(rec);

            // Relationships from task hierarchy/flow
            for (eu.kalafatic.evolution.model.orchestration.Task next : task.getNext()) {
                RelationshipRecord rel = new RelationshipRecord();
                rel.setFrom(taskName);
                String nextName = next.getName() != null ? next.getName() : (next.getId() != null ? next.getId() : "Task " + next.hashCode());
                rel.setTo(nextName);
                rel.setType("next");
                model.getRelationships().add(rel);
            }
            i++;
        }

        // 4. Iterations if present
        if (orchestrator.getSelfDevSession() != null) {
            i = 0;
            for (eu.kalafatic.evolution.model.orchestration.Iteration iter : orchestrator.getSelfDevSession().getIterations()) {
                ComponentRecord rec = new ComponentRecord();
                String iterName = iter.getId() != null ? iter.getId() : "Iteration " + iter.hashCode();
                rec.setName(iterName);
                rec.setType("Iteration");
                rec.setX(50 + (i * 220) % 880);
                rec.setY(250 + (i / 4) * 200);
                if (iter.getPhase() != null) rec.getProperties().add("Phase: " + iter.getPhase());
                if (iter.getStatus() != null) rec.getProperties().add("Status: " + iter.getStatus());
                model.getComponents().add(rec);
                i++;
            }
        }

        // 4. Shared Memory elements if they look like components
        String sharedMemory = orchestrator.getSharedMemory();
        if (sharedMemory != null && sharedMemory.startsWith("{")) {
            try {
                JSONObject json = new JSONObject(sharedMemory);
                if (json.has("components")) {
                    JSONArray comps = json.getJSONArray("components");
                    for (int j = 0; j < comps.length(); j++) {
                        JSONObject c = comps.getJSONObject(j);
                        ComponentRecord rec = new ComponentRecord();
                        rec.setName(c.optString("name", "Unknown"));
                        rec.setType(c.optString("type", "Component"));
                        rec.setX(c.optInt("x", 500));
                        rec.setY(c.optInt("y", 500));
                        model.getComponents().add(rec);
                    }
                }
            } catch (Exception e) {
                // Ignore parsing errors for shared memory
            }
        }

        if (model.getComponents().isEmpty()) {
            return createDefaultModel();
        }

        return model;
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
    protected void refreshUI() {
        refreshBrowser();
    }
}
