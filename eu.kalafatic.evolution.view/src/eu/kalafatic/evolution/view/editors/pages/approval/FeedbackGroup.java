package eu.kalafatic.evolution.view.editors.pages.approval;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class FeedbackGroup extends AEvoGroup {
    private Scale ratingScale;
    private Text commentsText;

    public FeedbackGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(editor, orchestrator);
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "User Feedback & Satisfaction", 2, true);

        SWTFactory.createLabel(group, "Rating (1-5):");
        ratingScale = new Scale(group, SWT.HORIZONTAL);
        ratingScale.setMinimum(1);
        ratingScale.setMaximum(5);
        ratingScale.setIncrement(1);
        ratingScale.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        ratingScale.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                if (orchestrator != null && orchestrator.getSelfDevSession() != null && !orchestrator.getSelfDevSession().getIterations().isEmpty()) {
                    Iteration last = orchestrator.getSelfDevSession().getIterations().get(orchestrator.getSelfDevSession().getIterations().size() - 1);
                    last.setRating(ratingScale.getSelection());
                    editor.setDirty(true);
                }
            }
        });

        SWTFactory.createLabel(group, "Comments:");
        commentsText = toolkit.createText(group, "", SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
        GridData commentsGD = new GridData(GridData.FILL_HORIZONTAL);
        commentsGD.heightHint = 60;
        commentsText.setLayoutData(commentsGD);
        commentsText.addModifyListener(e -> {
            if (orchestrator != null && orchestrator.getSelfDevSession() != null && !orchestrator.getSelfDevSession().getIterations().isEmpty()) {
                Iteration last = orchestrator.getSelfDevSession().getIterations().get(orchestrator.getSelfDevSession().getIterations().size() - 1);
                last.setComments(commentsText.getText());
                editor.setDirty(true);
            }
        });
    }

    @Override
    public void refreshUI() {
        if (orchestrator != null && orchestrator.getSelfDevSession() != null && !orchestrator.getSelfDevSession().getIterations().isEmpty()) {
            Iteration last = orchestrator.getSelfDevSession().getIterations().get(orchestrator.getSelfDevSession().getIterations().size() - 1);
            ratingScale.setSelection(last.getRating() > 0 ? last.getRating() : 3);
            commentsText.setText(last.getComments() != null ? last.getComments() : "");
        }
    }
}
