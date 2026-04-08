package eu.kalafatic.evolution.view.editors.pages.approval;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class SummaryGroup {
    private Composite group;
    private Label sessionIdLabel, statusLabel, iterationsLabel, branchLabel, rationaleLabel;

    public SummaryGroup(FormToolkit toolkit, Composite parent, Orchestrator orchestrator) {
        createControl(toolkit, parent);
        updateUI(orchestrator);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(parent, "Approval Summary", 2, true);

        SWTFactory.createLabel(group, "Session ID:");
        sessionIdLabel = toolkit.createLabel(group, "");
        sessionIdLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        SWTFactory.createLabel(group, "Status:");
        statusLabel = toolkit.createLabel(group, "");
        statusLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        SWTFactory.createLabel(group, "Iterations:");
        iterationsLabel = toolkit.createLabel(group, "");
        iterationsLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        SWTFactory.createLabel(group, "Git Branch:");
        branchLabel = toolkit.createLabel(group, "");
        branchLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        SWTFactory.createLabel(group, "AI Rationale:");
        rationaleLabel = toolkit.createLabel(group, "", SWT.WRAP);
        GridData rationaleGD = new GridData(GridData.FILL_HORIZONTAL);
        rationaleGD.heightHint = 40;
        rationaleLabel.setLayoutData(rationaleGD);
    }

    public void updateUI(Orchestrator orchestrator) {
        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            SelfDevSession session = orchestrator.getSelfDevSession();
            sessionIdLabel.setText(session.getId() != null ? session.getId() : "N/A");
            statusLabel.setText(session.getStatus() != null ? session.getStatus().toString() : "N/A");
            iterationsLabel.setText(String.valueOf(session.getIterations().size()) + " / " + session.getMaxIterations());

            if (!session.getIterations().isEmpty()) {
                Iteration last = session.getIterations().get(session.getIterations().size() - 1);
                branchLabel.setText(last.getBranchName() != null ? last.getBranchName() : "N/A");
                rationaleLabel.setText(last.getRationale() != null ? last.getRationale() : (session.getRationale() != null ? session.getRationale() : "No rationale provided."));
            } else {
                branchLabel.setText("N/A");
                rationaleLabel.setText("N/A");
            }
        } else {
            sessionIdLabel.setText("No active session");
            statusLabel.setText("N/A");
            iterationsLabel.setText("0 / 0");
            branchLabel.setText("N/A");
            rationaleLabel.setText("N/A");
        }
    }
}
