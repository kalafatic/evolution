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

public class SatisfactionGroup {
    private Composite group;
    private Scale satisfactionScale;
    private Text satisfactionCommentsText;
    private AiChatPage page;

    public SatisfactionGroup(Composite parent, AiChatPage page) {
        this.page = page;
        createControl(parent);
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

        Button submitSatButton = SWTFactory.createButton(group, "Submit Feedback", 150);
        GridData submitSatGD = new GridData();
        submitSatGD.horizontalSpan = 2;
        submitSatButton.setLayoutData(submitSatGD);
        submitSatButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.submitFeedback(satisfactionScale.getSelection(), satisfactionCommentsText.getText());
            }
        });
    }

    public void setVisible(boolean visible) {
        if (group.isDisposed()) return;
        group.setVisible(visible);
        ((GridData) group.getLayoutData()).exclude = !visible;
    }
}
