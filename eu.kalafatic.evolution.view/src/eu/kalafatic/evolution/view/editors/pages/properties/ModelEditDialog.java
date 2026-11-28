package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.controller.manager.ModelEditingService;
import eu.kalafatic.evolution.controller.manager.ModelIdentity;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Editor for user model metadata and for safe, explicit renaming.
 *
 * <p>The dialog clearly separates the display name from the technical identity,
 * the runtime name and the artifact filename. Display changes never rename the
 * underlying runtime model.</p>
 */
public class ModelEditDialog extends Dialog {

    private final Orchestrator orchestrator;
    private final ModelRow row;
    private final ModelIdentity identity;
    private final boolean renameFocus;

    private Text displayNameText;
    private Text descriptionText;
    private Text tagsText;
    private Text aliasesText;
    private Text endpointText;
    private Button renameArtifactCheckbox;
    private Text artifactNameText;
    private Label renameHint;

    private String resultMessage = "";
    private boolean saved;

    public ModelEditDialog(Shell parentShell, Orchestrator orchestrator, ModelRow row, boolean renameFocus) {
        super(parentShell);
        this.orchestrator = orchestrator;
        this.row = row;
        this.identity = row.getIdentity();
        this.renameFocus = renameFocus;
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    public boolean isSaved() {
        return saved;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        area.setLayout(new GridLayout(2, false));
        ((GridLayout) area.getLayout()).marginWidth = 12;
        ((GridLayout) area.getLayout()).marginHeight = 12;

        addReadOnly(area, "Display / technical identity", identity.getTechnicalId());
        addReadOnly(area, "Canonical identity", identity.getCanonicalId());
        addReadOnly(area, "Runtime name", identity.getRuntimeName());
        addReadOnly(area, "Provider", row.getProviderLabel());
        addReadOnly(area, "Type", row.getTypeLabel());
        addReadOnly(area, "Status", row.getStatus());
        addReadOnly(area, "Registration", row.getRegistrationState());
        addReadOnly(area, "Endpoint", identity.getEndpoint());
        addReadOnly(area, "Artifact", row.getArtifactPath() != null
                ? row.getArtifactPath().toString() : "(none)");
        addReadOnly(area, "Ownership", row.isArtifactOwned()
                ? "EVO-owned artifact" : (row.hasArtifact() ? "External / retained" : "No artifact"));

        addSeparator(area);

        Label displayLabel = new Label(area, SWT.NONE);
        displayLabel.setText("Display name:");
        displayNameText = new Text(area, SWT.BORDER);
        displayNameText.setText(row.getDisplayName());
        displayNameText.setLayoutData(fill());

        Label descriptionLabel = new Label(area, SWT.NONE);
        descriptionLabel.setText("Description:");
        descriptionText = new Text(area, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        descriptionText.setText(row.getDescription());
        GridData descriptionData = fill();
        descriptionData.heightHint = 60;
        descriptionText.setLayoutData(descriptionData);

        Label tagsLabel = new Label(area, SWT.NONE);
        tagsLabel.setText("Tags / categories:");
        tagsText = new Text(area, SWT.BORDER);
        tagsText.setText(String.join(", ", row.getTags()));
        tagsText.setLayoutData(fill());

        Label aliasesLabel = new Label(area, SWT.NONE);
        aliasesLabel.setText("Aliases (optional, comma separated):");
        aliasesText = new Text(area, SWT.BORDER);
        aliasesText.setLayoutData(fill());

        if (identity.getKind() == eu.kalafatic.evolution.controller.manager.ModelIdentity.Kind.REMOTE) {
            Label endpointLabel = new Label(area, SWT.NONE);
            endpointLabel.setText("Provider endpoint:");
            endpointText = new Text(area, SWT.BORDER);
            endpointText.setText(identity.getEndpoint());
            endpointText.setLayoutData(fill());
        }

        addSeparator(area);

        Composite renameGroup = new Composite(area, SWT.NONE);
        GridLayout renameLayout = new GridLayout(1, false);
        renameLayout.marginWidth = 0;
        renameLayout.marginHeight = 0;
        renameGroup.setLayout(renameLayout);
        renameGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));

        Label renameTitle = new Label(renameGroup, SWT.NONE);
        renameTitle.setText("Physical artifact rename");
        renameTitle.setFont(org.eclipse.jface.resource.JFaceResources.getBannerFont());

        boolean artifactSupported = row.getIdentity().isArtifactBacked()
                && row.getArtifactPath() != null && row.isArtifactOwned();

        renameArtifactCheckbox = new Button(renameGroup, SWT.CHECK);
        renameArtifactCheckbox.setText("Rename the physical artifact (EVO Native / GGUF only)");
        renameArtifactCheckbox.setSelection(renameFocus && artifactSupported);
        renameArtifactCheckbox.setEnabled(artifactSupported);

        Composite artifactRow = new Composite(renameGroup, SWT.NONE);
        artifactRow.setLayout(new GridLayout(2, false));
        artifactRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        Label artifactLabel = new Label(artifactRow, SWT.NONE);
        artifactLabel.setText("New artifact file name:");
        artifactNameText = new Text(artifactRow, SWT.BORDER);
        artifactNameText.setLayoutData(fill());
        if (row.getArtifactPath() != null) {
            artifactNameText.setText(row.getArtifactPath().getFileName().toString());
        }
        artifactNameText.setEnabled(renameArtifactCheckbox.getSelection());
        renameArtifactCheckbox.addSelectionListener(
                new org.eclipse.swt.events.SelectionAdapter() {
                    @Override
                    public void widgetSelected(org.eclipse.swt.events.SelectionEvent e) {
                        artifactNameText.setEnabled(renameArtifactCheckbox.getSelection());
                    }
                });

        renameHint = new Label(renameGroup, SWT.WRAP);
        renameHint.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        if (row.getIdentity().isOllama()) {
            renameHint.setText("Ollama runtime rename is not supported by the Ollama API. "
                    + "Changing the display name here does not rename the runtime model '"
                    + row.getIdentity().getRuntimeName() + "'.");
        } else if (!artifactSupported) {
            renameHint.setText("The artifact is not owned by EVO, so only display metadata can be changed.");
        } else {
            renameHint.setText("Renaming moves the owned artifact and updates every EVO reference to it.");
        }

        Composite info = new Composite(area, SWT.NONE);
        info.setLayout(new GridLayout(1, false));
        info.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        Label infoLabel = new Label(info, SWT.WRAP);
        infoLabel.setText("The canonical identity and runtime model are preserved unless a physical "
                + "artifact rename is explicitly selected.");
        infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        return area;
    }

    private void addReadOnly(Composite area, String label, String value) {
        Label l = new Label(area, SWT.NONE);
        l.setText(label + ":");
        Text t = new Text(area, SWT.BORDER | SWT.READ_ONLY);
        t.setText(value != null ? value : "");
        t.setLayoutData(fill());
        t.setBackground(area.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
    }

    private void addSeparator(Composite area) {
        Label separator = new Label(area, SWT.HORIZONTAL | SWT.SEPARATOR);
        separator.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    }

    private GridData fill() {
        return new GridData(SWT.FILL, SWT.CENTER, true, false);
    }

    @Override
    protected void okPressed() {
        ModelEditingService service = ModelEditingService.getInstance();
        ModelEditingService.MetadataUpdate update = new ModelEditingService.MetadataUpdate()
                .displayName(displayNameText.getText())
                .description(descriptionText.getText())
                .tags(split(tagsText.getText()))
                .aliases(split(aliasesText.getText()));

        ModelEditingService.RenameResult metadataResult =
                service.saveMetadata(orchestrator, row.getProvider(), update);
        if (!metadataResult.isSuccess()) {
            MessageDialog.openError(getShell(), "Edit Model", metadataResult.getMessage());
            return;
        }

        StringBuilder message = new StringBuilder(metadataResult.getMessage());

        if (endpointText != null && !endpointText.isDisposed()) {
            String endpoint = endpointText.getText() != null ? endpointText.getText().trim() : "";
            if (!endpoint.isEmpty() && !endpoint.equals(identity.getEndpoint())) {
                java.util.List<eu.kalafatic.evolution.model.orchestration.AIProvider> registrations =
                        eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance()
                                .findRegisteredProviders(orchestrator, identity);
                for (eu.kalafatic.evolution.model.orchestration.AIProvider registration : registrations) {
                    registration.setUrl(endpoint);
                }
                if (!registrations.isEmpty()) {
                    message.append(" Provider endpoint updated for ")
                            .append(registrations.size()).append(" registration(s).");
                }
            }
        }

        if (renameArtifactCheckbox.getSelection()) {
            String artifactName = artifactNameText.getText();
            ModelEditingService.RenameResult renameResult =
                    service.renameArtifact(orchestrator, row.getProvider(), artifactName);
            if (!renameResult.isSuccess()) {
                MessageDialog.openError(getShell(), "Rename Artifact", renameResult.getMessage());
                return;
            }
            message.append(' ').append(renameResult.getMessage());
        }
        resultMessage = message.toString();

        saved = true;
        super.okPressed();
    }

    private static java.util.List<String> split(String value) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (value == null) {
            return result;
        }
        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(renameFocus ? "Rename Model" : "Edit Model");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, renameFocus ? "Rename" : "Save", true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
}
