package eu.kalafatic.evolution.view.editors.pages.approval;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.utils.factories.GUIFactory;

import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class SelfDevSessionGroup extends AEvoGroup {
    private Label sessionIdLabel, statusLabel, iterationsLabel, branchLabel;

    public SelfDevSessionGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
        refreshUI();
    }

    @Override
    protected void refreshUI() {
        updateUI(orchestrator);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "Self-Dev Session Details", 2, true);

        GUIFactory.INSTANCE.createLabel(group, "Session ID:");
        sessionIdLabel = toolkit.createLabel(group, "");
        sessionIdLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "Status:");
        statusLabel = toolkit.createLabel(group, "");
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "Iterations:");
        iterationsLabel = toolkit.createLabel(group, "");
        iterationsLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        GUIFactory.INSTANCE.createLabel(group, "Git Branch:");
        branchLabel = toolkit.createLabel(group, "");
        branchLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    private void updateUI(Orchestrator orchestrator) {
        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            SelfDevSession session = orchestrator.getSelfDevSession();
            sessionIdLabel.setText(session.getId() != null ? session.getId() : "N/A");
            statusLabel.setText(session.getStatus() != null ? session.getStatus().toString() : "N/A");
            iterationsLabel.setText(String.valueOf(session.getIterations().size()) + " / " + session.getMaxIterations());

            if (!session.getIterations().isEmpty()) {
                Iteration last = session.getIterations().get(session.getIterations().size() - 1);
                branchLabel.setText(last.getBranchName() != null ? last.getBranchName() : "N/A");
            } else {
                branchLabel.setText("N/A");
            }
        } else {
            sessionIdLabel.setText("No active session");
            statusLabel.setText("N/A");
            iterationsLabel.setText("0 / 0");
            branchLabel.setText("N/A");
        }
    }
}
