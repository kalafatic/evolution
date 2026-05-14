package eu.kalafatic.evolution.view.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * Dialog for selecting a mediated analysis target.
 */
public class MediatedTargetDialog extends Dialog {
    private Text pathText;
    private Combo typeCombo;
    private String selectedPath;
    private String selectedType;
    private String initialPath;
    private String initialType;

    public MediatedTargetDialog(Shell parentShell) {
        super(parentShell);
    }

    public void setInitialPath(String path) { this.initialPath = path; }
    public void setInitialType(String type) { this.initialType = type; }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(3, false));

        Label pathLabel = new Label(container, SWT.NONE);
        pathLabel.setText("Target Path:");

        pathText = new Text(container, SWT.BORDER);
        pathText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (initialPath != null) pathText.setText(initialPath);

        Button browseButton = new Button(container, SWT.PUSH);
        browseButton.setText("Browse...");
        browseButton.addListener(SWT.Selection, event -> {
            DirectoryDialog dialog = new DirectoryDialog(getShell());
            String path = dialog.open();
            if (path != null) pathText.setText(path);
        });

        Label typeLabel = new Label(container, SWT.NONE);
        typeLabel.setText("Target Type:");

        typeCombo = new Combo(container, SWT.READ_ONLY);
        typeCombo.setItems(new String[] {"Project", "Folder", "PDF", "HTML", "Markdown"});
        typeCombo.select(0);
        if (initialType != null) {
            for (int i = 0; i < typeCombo.getItemCount(); i++) {
                if (typeCombo.getItem(i).equalsIgnoreCase(initialType)) {
                    typeCombo.select(i);
                    break;
                }
            }
        }
        typeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        return area;
    }

    @Override
    protected void okPressed() {
        selectedPath = pathText.getText();
        selectedType = typeCombo.getText();
        super.okPressed();
    }

    public String getSelectedPath() { return selectedPath; }
    public String getSelectedType() { return selectedType; }
}
