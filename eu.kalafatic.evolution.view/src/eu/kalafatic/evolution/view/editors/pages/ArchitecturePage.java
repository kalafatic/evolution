package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.orchestration.design.ComponentRecord;
import eu.kalafatic.evolution.controller.orchestration.design.DesignModel;
import eu.kalafatic.evolution.controller.orchestration.design.RelationshipRecord;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.architecture.DesignRenderer;

/**
 * @evo:19:A reason=dynamic-architecture-page
 */
public class ArchitecturePage extends Composite {
    private Browser browser;
    private Orchestrator orchestrator;
    private MultiPageEditor editor;
    private DesignRenderer renderer = new DesignRenderer();
    private long lastRefresh = 0;
    private static final long REFRESH_DEBOUNCE = 500;

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
        this.setLayout(new FillLayout());

        this.browser = new Browser(this, SWT.NONE);

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

    private void scheduleRefresh() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh < REFRESH_DEBOUNCE) return;
        lastRefresh = now;
        refreshBrowser();
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
