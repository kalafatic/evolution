package eu.kalafatic.evolution.view.editors.pages.aichat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class InputGroup {
    private Composite group;
    private Label promptLabel;
    private Text inputText;
    private AiChatPage page;

    public InputGroup(Composite parent, AiChatPage page) {
        this.page = page;
        createControl(parent);
    }

    private void createControl(Composite parent) {
        group = new Composite(parent, SWT.NONE);
        group.setLayout(new GridLayout(3, false));
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setVisible(false);
        ((GridData) group.getLayoutData()).exclude = true;

        promptLabel = new Label(group, SWT.NONE);
        promptLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        inputText = new Text(group, SWT.BORDER);
        GridData textGd = new GridData(GridData.FILL_HORIZONTAL);
        textGd.widthHint = 200;
        inputText.setLayoutData(textGd);

        Button submitButton = SWTFactory.createButton(group, "Submit");
        submitButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.provideInput(inputText.getText());
                inputText.setText("");
            }
        });
    }

    public void show(String message) {
        if (group.isDisposed()) return;
        promptLabel.setText(message);
        group.setVisible(true);
        ((GridData) group.getLayoutData()).exclude = false;
        inputText.setFocus();
    }

    public void hide() {
        if (group.isDisposed()) return;
        group.setVisible(false);
        ((GridData) group.getLayoutData()).exclude = true;
    }
}
