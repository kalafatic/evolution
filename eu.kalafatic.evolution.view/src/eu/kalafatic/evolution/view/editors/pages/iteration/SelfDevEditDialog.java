package eu.kalafatic.evolution.view.editors.pages.iteration;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
import eu.kalafatic.evolution.view.editors.pages.DevelopmentPage;

public class SelfDevEditDialog extends Dialog {
    private SelfDevSession session;
    private DevelopmentPage page;

    private Text initialRequestText;
    private Text maxIterationsText;
    private Text rationaleText;

    public SelfDevEditDialog(Shell parentShell, SelfDevSession session, DevelopmentPage page) {
        super(parentShell);
        this.session = session;
        this.page = page;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Edit Self-Dev Session");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(2, false));
        GridData gridData = new GridData();
        gridData.widthHint = 600;
        container.setLayoutData(gridData);

        createLabel(container, "Initial Request:");
        initialRequestText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData gdRequest = new GridData(GridData.FILL_BOTH);
        gdRequest.heightHint = 100;
        initialRequestText.setLayoutData(gdRequest);
        initialRequestText.setText(session.getInitialRequest() != null ? session.getInitialRequest() : "");

        createLabel(container, "Max Iterations:");
        maxIterationsText = createText(container, String.valueOf(session.getMaxIterations()));

        createLabel(container, "Rationale:");
        rationaleText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData gdRationale = new GridData(GridData.FILL_BOTH);
        gdRationale.heightHint = 100;
        rationaleText.setLayoutData(gdRationale);
        rationaleText.setText(session.getRationale() != null ? session.getRationale() : "");

        return container;
    }

    private void createLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
    }

    private Text createText(Composite parent, String initialValue) {
        Text text = new Text(parent, SWT.BORDER);
        text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        text.setText(initialValue);
        return text;
    }

    @Override
    protected void okPressed() {
        String maxIterStr = maxIterationsText.getText();
        try {
            int maxIter = Integer.parseInt(maxIterStr);
            session.setMaxIterations(maxIter);
        } catch (NumberFormatException e) {
            org.eclipse.jface.dialogs.MessageDialog.openError(getShell(), "Invalid Input", "Max Iterations must be a valid integer.");
            return;
        }

        session.setInitialRequest(initialRequestText.getText());
        session.setRationale(rationaleText.getText());

        page.setDirty(true);
        super.okPressed();
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}
