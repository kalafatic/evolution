package eu.kalafatic.evolution.view.editors.pages.development;

import java.util.LinkedHashMap;
import org.eclipse.swt.widgets.Shell;
import eu.kalafatic.evolution.view.editors.pages.DevelopmentPage.SelfDevRow;
import eu.kalafatic.utils.dialogs.DynamicField;
import eu.kalafatic.utils.dialogs.DynamicMapDialog;

public class RowEditDialog extends DynamicMapDialog {
    private SelfDevRow row;

    private static final String NAME = "name";
    private static final String PATH = "path";
    private static final String STATUS = "status";

    public RowEditDialog(Shell parentShell, SelfDevRow row) {
        super(parentShell, createFields(row));
        this.row = row;
        setTitle("Edit Row: " + row.name);
    }

    private static LinkedHashMap<String, DynamicField> createFields(SelfDevRow row) {
        LinkedHashMap<String, DynamicField> fields = new LinkedHashMap<>();
        fields.put(NAME, new DynamicField("Name:", DynamicField.TYPE_TEXT, row.name));

        int pathFlags = DynamicField.TYPE_TEXT;
        if (SelfDevRow.GIT_CHECK.equals(row.name)) {
            // Git could be a directory if it's a local path
            pathFlags |= DynamicField.DIRECTORY;
        } else if (SelfDevRow.COPY_SOURCE.equals(row.name) ||
                   SelfDevRow.BUILD_PROJECT.equals(row.name) ||
                   SelfDevRow.EXPORT_PRODUCT.equals(row.name)) {
            pathFlags |= DynamicField.DIRECTORY;
        } else if (SelfDevRow.SUPERVISOR_LOOP.equals(row.name)) {
            pathFlags |= DynamicField.FILE;
        }

        fields.put(PATH, new DynamicField("Path/URL:", pathFlags, row.path));
        fields.put(STATUS, new DynamicField("Status:", DynamicField.TYPE_TEXT, row.status));
        return fields;
    }

    @Override
    protected void okPressed() {
        if (!validate()) return;
        saveValues();
        row.name = getString(NAME);
        row.path = getString(PATH);
        row.status = getString(STATUS);
        super.okPressed();
    }
}
