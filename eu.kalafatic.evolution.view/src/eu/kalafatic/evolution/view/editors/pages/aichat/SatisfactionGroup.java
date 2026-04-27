package eu.kalafatic.evolution.view.editors.pages.aichat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;

public class SatisfactionGroup extends AEvoGroup {
    private Scale satisfactionScale;
    private Text satisfactionCommentsText;
    private AiChatPage page;

    public SatisfactionGroup(Composite parent, MultiPageEditor editor, Orchestrator orchestrator, AiChatPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(parent);
    }

    @Override
    protected void refreshUI() {
        // No dynamic model data to refresh
    }

    private void createControl(Composite parent) {
        group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(2, false));
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setVisible(false);
        ((GridData) group.getLayoutData()).exclude = true;

        Label satLabel = new Label(group, SWT.NONE);
        satLabel.setText("Rate Session (1-5):");
        satisfactionScale = new Scale(group, SWT.HORIZONTAL);
        satisfactionScale.setMinimum(1);
        satisfactionScale.setMaximum(5);
        satisfactionScale.setSelection(3);
        satisfactionScale.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Label commentLabel = new Label(group, SWT.NONE);
        commentLabel.setText("Session Feedback:");
        satisfactionCommentsText = new Text(group, SWT.BORDER | SWT.SINGLE);
        satisfactionCommentsText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite buttonBox = new Composite(group, SWT.NONE);
        GridLayout bbLayout = new GridLayout(2, true);
        bbLayout.marginWidth = 0;
        bbLayout.marginHeight = 0;
        buttonBox.setLayout(bbLayout);
        GridData bbGD = new GridData(GridData.FILL_HORIZONTAL);
        bbGD.horizontalSpan = 2;
        buttonBox.setLayoutData(bbGD);

        Button submitSatButton = SWTFactory.createButton(buttonBox, "Submit Feedback", 120);
        submitSatButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        submitSatButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.submitFeedback(satisfactionScale.getSelection(), satisfactionCommentsText.getText());
            }
        });

        Button peerReviewBtn = SWTFactory.createButton(buttonBox, "Peer Review", 120);
        peerReviewBtn.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        peerReviewBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.handleReview();
            }
        });
    }

    public void setVisible(boolean visible) {
        if (group.isDisposed()) return;
        group.setVisible(visible);
        ((GridData) group.getLayoutData()).exclude = !visible;
        if (visible) {
            page.expandFeedbackSection();
        }
        page.updateFeedbackVisibility();
    }

    public boolean isVisible() {
        return group != null && !group.isDisposed() && group.getVisible();
    }
}
