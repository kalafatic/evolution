package eu.kalafatic.evolution.view.editors.pages.properties;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Confirmation dialog for bulk model removal. It makes the distinction between
 * runtime deregistration and physical artifact deletion explicit.
 */
public class ModelDeleteDialog extends Dialog {

    private final List<ModelRow> rows;

    private Button deregisterRuntimeCheckbox;
    private Button deleteArtifactsCheckbox;
    private Button deleteSharedArtifactsCheckbox;

    private boolean deregisterRuntime = true;
    private boolean deleteArtifacts;
    private boolean deleteSharedArtifacts;

    public ModelDeleteDialog(Shell parentShell, List<ModelRow> rows) {
        super(parentShell);
        this.rows = rows;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        area.setLayout(new GridLayout(1, false));
        ((GridLayout) area.getLayout()).marginWidth = 12;
        ((GridLayout) area.getLayout()).marginHeight = 12;

        Label heading = new Label(area, SWT.WRAP);
        heading.setText("Remove " + rows.size() + " selected model(s)?");
        heading.setFont(org.eclipse.jface.resource.JFaceResources.getBannerFont());
        heading.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Text names = new Text(area, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL);
        StringBuilder builder = new StringBuilder();
        for (ModelRow row : rows) {
            builder.append("• ").append(row.getDisplayName())
                    .append("  [").append(row.getTechnicalId()).append("]")
                    .append("  — ").append(row.getProviderLabel())
                    .append(" / ").append(row.getRegistrationState());
            if (row.hasOwnedArtifacts()) {
                builder.append(" / ").append(row.getOwnedArtifactCount()).append(" owned artifact(s)");
            }
            builder.append('\n');
        }
        names.setText(builder.toString());
        GridData namesData = new GridData(SWT.FILL, SWT.FILL, true, true);
        namesData.heightHint = 140;
        names.setLayoutData(namesData);

        Label warning = new Label(area, SWT.WRAP);
        warning.setText("Runtime deregistration and physical file deletion are separate operations. "
                + "Physical artifacts are deleted only when explicitly selected below and only when "
                + "they are owned by EVO. Shared/external artifacts are always retained.");
        warning.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        boolean hasOllama = rows.stream().anyMatch(r -> r.getIdentity().isOllama());
        boolean hasOwnedArtifact = rows.stream().anyMatch(ModelRow::hasOwnedArtifacts);
        boolean hasSharedArtifact = rows.stream().anyMatch(r -> {
            if (r.getArtifactPath() == null) {
                return false;
            }
            String name = r.getArtifactPath().getFileName().toString().toLowerCase();
            return name.equals("evo.gguf") || name.equals("evo.evo");
        });

        deregisterRuntimeCheckbox = new Button(area, SWT.CHECK);
        deregisterRuntimeCheckbox.setText("Deregister Ollama runtime registration (recommended)");
        deregisterRuntimeCheckbox.setSelection(hasOllama);
        deregisterRuntimeCheckbox.setEnabled(hasOllama);

        deleteArtifactsCheckbox = new Button(area, SWT.CHECK);
        deleteArtifactsCheckbox.setText("Also delete EVO-owned model artifacts from disk (recommended)");
        deleteArtifactsCheckbox.setEnabled(hasOwnedArtifact);
        // Deleting a registered model but leaving its artifact behind makes the
        // model immediately reappear through discovery. Preselect owned artifact
        // deletion so a confirmed removal actually removes the model. Shared
        // generic artifacts and external files remain protected below.
        deleteArtifactsCheckbox.setSelection(hasOwnedArtifact);

        deleteSharedArtifactsCheckbox = new Button(area, SWT.CHECK);
        deleteSharedArtifactsCheckbox.setText(
                "Also delete shared generic artifacts such as evo.gguf (not recommended)");
        deleteSharedArtifactsCheckbox.setEnabled(hasOwnedArtifact && hasSharedArtifact);
        deleteSharedArtifactsCheckbox.setSelection(false);

        deleteArtifactsCheckbox.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter() {
            @Override
            public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                deleteSharedArtifactsCheckbox.setEnabled(
                        deleteArtifactsCheckbox.getSelection() && hasSharedArtifact);
                if (!deleteArtifactsCheckbox.getSelection()) {
                    deleteSharedArtifactsCheckbox.setSelection(false);
                }
            }
        });

        Label hint = new Label(area, SWT.WRAP);
        hint.setText("If the runtime is unavailable, runtime deregistration is reported as unverified "
                + "and the overall result is partial success rather than success.");
        hint.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        return area;
    }

    @Override
    protected void okPressed() {
        deregisterRuntime = deregisterRuntimeCheckbox.getSelection();
        deleteArtifacts = deleteArtifactsCheckbox.getSelection();
        deleteSharedArtifacts = deleteSharedArtifactsCheckbox.getSelection();
        super.okPressed();
    }

    public boolean isDeregisterRuntime() {
        return deregisterRuntime;
    }

    public boolean isDeleteArtifacts() {
        return deleteArtifacts;
    }

    public boolean isDeleteSharedArtifacts() {
        return deleteSharedArtifacts;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Remove Models");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, "Remove", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}
