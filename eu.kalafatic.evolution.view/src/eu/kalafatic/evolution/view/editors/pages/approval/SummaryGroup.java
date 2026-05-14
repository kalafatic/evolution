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

public class SummaryGroup extends AEvoGroup {
    private org.eclipse.swt.widgets.Text rationaleText;

    public SummaryGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
        refreshUI();
    }

    @Override
    protected void refreshUI() {
        updateUI(orchestrator);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "AI Rationale & Plan", 1, true);

        GUIFactory.INSTANCE.createLabel(group, "Rationale and Strategy:");
        rationaleText = new org.eclipse.swt.widgets.Text(group, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
        GridData rationaleGD = new GridData(GridData.FILL_HORIZONTAL);
        rationaleGD.heightHint = 80;
        rationaleText.setLayoutData(rationaleGD);
        rationaleText.setBackground(group.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    }

    private void updateUI(Orchestrator orchestrator) {
        if (orchestrator != null && orchestrator.getSelfDevSession() != null) {
            SelfDevSession session = orchestrator.getSelfDevSession();
            if (!session.getIterations().isEmpty()) {
                Iteration last = session.getIterations().get(session.getIterations().size() - 1);
                setTextSafe(rationaleText, last.getRationale() != null ? last.getRationale() : (session.getRationale() != null ? session.getRationale() : "No rationale provided."));
            } else {
                setTextSafe(rationaleText, session.getRationale() != null ? session.getRationale() : "No rationale provided.");
            }
        } else {
            setTextSafe(rationaleText, "No active session rationale.");
        }
    }
}
