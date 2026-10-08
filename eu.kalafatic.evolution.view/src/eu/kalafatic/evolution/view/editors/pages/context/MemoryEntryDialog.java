package eu.kalafatic.evolution.view.editors.pages.context;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.controller.memory.MemoryEntry;
import eu.kalafatic.evolution.controller.memory.MemoryImportance;
import eu.kalafatic.evolution.controller.memory.MemoryScope;
import eu.kalafatic.evolution.controller.memory.MemorySource;
import eu.kalafatic.evolution.controller.memory.MemoryType;

public class MemoryEntryDialog extends Dialog {

    private MemoryEntry entry;
    private Text contentText;
    private Combo scopeCombo;
    private Combo typeCombo;
    private Combo sourceCombo;
    private Combo importanceCombo;
    private Text confidenceText;

    public MemoryEntryDialog(Shell parentShell, MemoryEntry entry) {
        super(parentShell);
        this.entry = entry != null ? entry : new MemoryEntry();
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText(entry.getContent() == null || entry.getContent().trim().isEmpty() ? "Add Memory Entry" : "Edit Memory Entry");
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("Content:");
        contentText = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        GridData contentGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        contentGd.heightHint = 100;
        contentGd.widthHint = 400;
        contentText.setLayoutData(contentGd);
        contentText.setText(entry.getContent() != null ? entry.getContent() : "");

        new Label(container, SWT.NONE).setText("Scope:");
        scopeCombo = new Combo(container, SWT.READ_ONLY);
        scopeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (MemoryScope scope : MemoryScope.values()) {
            scopeCombo.add(scope.name());
        }
        scopeCombo.select(entry.getScope() != null ? entry.getScope().ordinal() : 0);

        new Label(container, SWT.NONE).setText("Type:");
        typeCombo = new Combo(container, SWT.READ_ONLY);
        typeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (MemoryType type : MemoryType.values()) {
            typeCombo.add(type.name());
        }
        typeCombo.select(entry.getType() != null ? entry.getType().ordinal() : 0);

        new Label(container, SWT.NONE).setText("Source:");
        sourceCombo = new Combo(container, SWT.READ_ONLY);
        sourceCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (MemorySource src : MemorySource.values()) {
            sourceCombo.add(src.name());
        }
        sourceCombo.select(entry.getSource() != null ? entry.getSource().ordinal() : 0);

        new Label(container, SWT.NONE).setText("Importance:");
        importanceCombo = new Combo(container, SWT.READ_ONLY);
        importanceCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        for (MemoryImportance imp : MemoryImportance.values()) {
            importanceCombo.add(imp.name());
        }
        importanceCombo.select(entry.getImportance() != null ? entry.getImportance().ordinal() : MemoryImportance.HIGH.ordinal());

        new Label(container, SWT.NONE).setText("Confidence (0.0 - 1.0):");
        confidenceText = new Text(container, SWT.BORDER);
        confidenceText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        confidenceText.setText(String.valueOf(entry.getConfidence()));

        return area;
    }

    @Override
    protected void okPressed() {
        if (contentText.getText().trim().isEmpty()) {
            contentText.setFocus();
            return;
        }

        entry.setContent(contentText.getText().trim());
        entry.setScope(MemoryScope.values()[scopeCombo.getSelectionIndex()]);
        entry.setType(MemoryType.values()[typeCombo.getSelectionIndex()]);
        entry.setSource(MemorySource.values()[sourceCombo.getSelectionIndex()]);
        entry.setImportance(MemoryImportance.values()[importanceCombo.getSelectionIndex()]);

        try {
            double conf = Double.parseDouble(confidenceText.getText().trim());
            if (conf >= 0.0 && conf <= 1.0) {
                entry.setConfidence(conf);
            }
        } catch (NumberFormatException ignored) {}

        super.okPressed();
    }

    public MemoryEntry getEntry() {
        return entry;
    }
}
