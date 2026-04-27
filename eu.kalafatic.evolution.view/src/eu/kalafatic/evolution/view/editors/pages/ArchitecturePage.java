package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

/**
 * @evo:18:A reason=architecture-page
 */
public class ArchitecturePage extends Composite {
    private Browser browser;
    private Orchestrator orchestrator;
    private MultiPageEditor editor;

    private Adapter modelAdapter = new EContentAdapter() {
        @Override
        public void notifyChanged(Notification notification) {
            super.notifyChanged(notification);
            refreshBrowser();
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

    private void refreshBrowser() {
        if (browser == null || browser.isDisposed()) return;
        Display.getDefault().asyncExec(() -> {
            if (browser == null || browser.isDisposed()) return;
            browser.setText(getHtmlTemplate());
        });
    }

    private String getHtmlTemplate() {
        return "<!DOCTYPE html><html><head>"
                + "<style>"
                + "body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px; line-height: 1.6; color: #334155; background-color: #f8fafc; }"
                + ".container { max-width: 900px; margin: auto; background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1); }"
                + "h1 { color: #1e293b; border-bottom: 2px solid #e2e8f0; padding-bottom: 10px; margin-top: 0; }"
                + "h2 { color: #334155; margin-top: 30px; font-size: 1.5em; border-left: 4px solid #3b82f6; padding-left: 15px; }"
                + "p { margin-bottom: 20px; }"
                + "ul { padding-left: 20px; }"
                + "li { margin-bottom: 8px; }"
                + "pre { background-color: #1e293b; color: #f8fafc; padding: 20px; border-radius: 8px; overflow-x: auto; font-family: 'Consolas', 'Monaco', monospace; font-size: 0.95em; }"
                + ".diagram-box { background-color: #eff6ff; border: 1px solid #bfdbfe; padding: 20px; border-radius: 8px; font-family: monospace; white-space: pre; color: #1e40af; }"
                + ".json-key { color: #93c5fd; }"
                + ".json-string { color: #facc15; }"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<h1>Evolution Architecture: Darwinian Design</h1>"
                + "<h2>Overview</h2>"
                + "<p>The \"Darwinian Design\" architecture focuses on a design-first approach where the system evolves by operating on its own Design Model. "
                + "It utilizes a DarwinEngine to manage iterative cycles (generate &rarr; evaluate &rarr; select &rarr; mutate) specifically targeting the architectural blueprint. "
                + "This ensures that the system doesn't just evolve code, but improves its structural logic and scalability over time. "
                + "For a single developer, this provides maximum clarity by keeping the high-level design as the source of truth, while specialized agents "
                + "(Architect, Coder, Evaluator) handle the realization. It's simple, maintainable, and designed for continuous self-improvement.</p>"

                + "<h2>Core Use Cases</h2>"
                + "<ul>"
                + "<li><strong>Define System Architecture:</strong> Declarative modeling of components and relationships.</li>"
                + "<li><strong>Evolve Design via Darwinian Loop:</strong> Autonomous refinement of the architecture model.</li>"
                + "<li><strong>Generate UML Diagrams:</strong> Automatic visualization from the Design Model.</li>"
                + "<li><strong>Generate Boilerplate Code:</strong> Model-to-code transformation for rapid development.</li>"
                + "<li><strong>Evaluate Design Quality:</strong> Automated assessment of modularity and scalability.</li>"
                + "</ul>"

                + "<h2>Simple Class Diagram</h2>"
                + "<div class='diagram-box'>"
                + "[DarwinEngine] -- manages --> [DesignModel]\n"
                + "[ArchitectAgent] -- refines --> [DesignModel]\n"
                + "[CoderAgent] -- implements --> [DesignModel]\n"
                + "[Evaluator] -- scores --> [DesignModel]\n"
                + "[DesignModel] -- represents --> [Architecture]"
                + "</div>"

                + "<h2>Minimal JSON Design Model</h2>"
                + "<pre>"
                + "{\n"
                + "  <span class='json-key'>\"system\"</span>: <span class='json-string'>\"EvolutionPlatform\"</span>,\n"
                + "  <span class='json-key'>\"version\"</span>: <span class='json-string'>\"2.0\"</span>,\n"
                + "  <span class='json-key'>\"components\"</span>: [\n"
                + "    { <span class='json-key'>\"name\"</span>: <span class='json-string'>\"DarwinEngine\"</span>, <span class='json-key'>\"type\"</span>: <span class='json-string'>\"Engine\"</span> },\n"
                + "    { <span class='json-key'>\"name\"</span>: <span class='json-string'>\"DesignModel\"</span>, <span class='json-key'>\"type\"</span>: <span class='json-string'>\"Data\"</span> }\n"
                + "  ],\n"
                + "  <span class='json-key'>\"relationships\"</span>: [\n"
                + "    { <span class='json-key'>\"from\"</span>: <span class='json-string'>\"DarwinEngine\"</span>, <span class='json-key'>\"to\"</span>: <span class='json-string'>\"DesignModel\"</span>, <span class='json-key'>\"type\"</span>: <span class='json-string'>\"manages\"</span> }\n"
                + "  ]\n"
                + "}"
                + "</pre>"
                + "</div>"
                + "</body></html>";
    }

    @Override
    public void dispose() {
        if (orchestrator != null) {
            orchestrator.eAdapters().remove(modelAdapter);
        }
        super.dispose();
    }
}
