package eu.kalafatic.evolution.view.editors.pages.approval;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.utils.factories.GUIFactory;
import eu.kalafatic.evolution.view.editors.pages.ApprovalPage;

public class FeedbackGroup extends AEvoGroup {
    private ApprovalPage page;
    private Scale ratingScale;
    private Text commentsText;

    public FeedbackGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, ApprovalPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "User Feedback & Satisfaction", 1, true);

        Composite ratingComp = GUIFactory.INSTANCE.createComposite(group, 2);
        GUIFactory.INSTANCE.createLabel(ratingComp, "Rating (1-10):");
        ratingScale = new Scale(ratingComp, SWT.HORIZONTAL);
        ratingScale.setMinimum(1);
        ratingScale.setMaximum(10);
        ratingScale.setIncrement(1);
        ratingScale.setSelection(5);
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

        Composite feedbackBox = GUIFactory.INSTANCE.createComposite(group, 1);
        GUIFactory.INSTANCE.createLabel(feedbackBox, "Comments:");
        commentsText = toolkit.createText(feedbackBox, "", SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
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

        Button submitBtn = GUIFactory.INSTANCE.createButton(group, "Submit Feedback");
        submitBtn.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                if (page != null) {
                    page.submitFeedback(ratingScale.getSelection(), commentsText.getText());
                }
            }
        });
    }

    @Override
    public void refreshUI() {
        if (orchestrator != null && orchestrator.getSelfDevSession() != null && !orchestrator.getSelfDevSession().getIterations().isEmpty()) {
            Iteration last = orchestrator.getSelfDevSession().getIterations().get(orchestrator.getSelfDevSession().getIterations().size() - 1);
            int rating = last.getRating() > 0 ? last.getRating() : 5;
            if (ratingScale.getSelection() != rating) {
                ratingScale.setSelection(rating);
            }
            setTextSafe(commentsText, last.getComments());
        }
    }
}
