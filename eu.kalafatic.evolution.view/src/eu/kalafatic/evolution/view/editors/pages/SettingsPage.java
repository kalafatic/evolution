package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.settings.NetworkSettingsGroup;

/**
 * Page for system settings and network discovery.
 * @evo:21:A reason=settings-page
 */
public class SettingsPage extends AEvoPage {

    private NetworkSettingsGroup networkGroup;

    public SettingsPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        createControl();
    }

    private void createControl() {
        Composite comp = toolkit.createComposite(this);
        comp.setLayout(new GridLayout(1, false));

        networkGroup = new NetworkSettingsGroup(toolkit, comp, editor, orchestrator, this);

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    @Override
    protected void refreshUI() {
        if (networkGroup != null) {
            networkGroup.updateUI();
        }
    }

    @Override
    public void setOrchestrator(Orchestrator orchestrator) {
        super.setOrchestrator(orchestrator);
        if (networkGroup != null) {
            networkGroup.setOrchestrator(orchestrator);
        }
    }
}
