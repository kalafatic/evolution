package eu.kalafatic.evolution.view.editors.pages.mcpsettings;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.json.JSONObject;

public class McpRequestDialog extends Dialog {
    private Text methodText;
    private Text paramsText;
    private String method;
    private String params;

    public McpRequestDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        container.setLayout(new GridLayout(2, false));

        Label methodLabel = new Label(container, SWT.NONE);
        methodLabel.setText("Method:");
        methodText = new Text(container, SWT.BORDER);
        methodText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        methodText.setText("ping");

        Label paramsLabel = new Label(container, SWT.NONE);
        paramsLabel.setText("Parameters (JSON):");
        paramsLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
        paramsText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        gd.heightHint = 150;
        gd.widthHint = 400;
        paramsText.setLayoutData(gd);
        paramsText.setText("{}");

        return container;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Send MCP Request");
    }

    @Override
    protected Point getInitialSize() {
        return new Point(500, 350);
    }

    @Override
    protected void okPressed() {
        method = methodText.getText();
        params = paramsText.getText();
        try {
            new JSONObject(params);
            super.okPressed();
        } catch (Exception e) {
            methodText.setFocus();
            // Just basic validation, if it fails, don't close.
        }
    }

    public String getMethod() { return method; }
    public String getParams() { return params; }
}
