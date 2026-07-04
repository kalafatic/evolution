package eu.kalafatic.evolution.view.editors.pages.mcpsettings;

import java.util.LinkedHashMap;
import org.eclipse.swt.widgets.Shell;
import eu.kalafatic.evolution.model.orchestration.NetworkEntry;
import eu.kalafatic.utils.dialogs.DynamicField;
import eu.kalafatic.utils.dialogs.DynamicMapDialog;

public class McpServerDialog extends DynamicMapDialog {
    private NetworkEntry entry;

    private static final String HOST = "host";
    private static final String ADDRESS = "address";
    private static final String PORT = "port";
    private static final String PATH = "path";
    private static final String NOTE = "note";

    public McpServerDialog(Shell parentShell, NetworkEntry entry) {
        super(parentShell, createFields(entry));
        this.entry = entry;
        setTitle(entry.getHost() == null ? "Add MCP Server" : "Edit MCP Server: " + entry.getHost());
    }

    private static LinkedHashMap<String, DynamicField> createFields(NetworkEntry entry) {
        LinkedHashMap<String, DynamicField> fields = new LinkedHashMap<>();
        fields.put(HOST, new DynamicField("Host Name:", DynamicField.TYPE_TEXT, entry.getHost()));
        fields.put(ADDRESS, new DynamicField("Address:", DynamicField.TYPE_TEXT, entry.getAddress()));
        fields.put(PORT, new DynamicField("Port:", DynamicField.TYPE_TEXT, String.valueOf(entry.getPort())));
        fields.put(PATH, new DynamicField("Path:", DynamicField.TYPE_TEXT, entry.getPath()));
        fields.put(NOTE, new DynamicField("Note:", DynamicField.TYPE_TEXT, entry.getNote()));
        return fields;
    }

    @Override
    protected void okPressed() {
        if (!validate()) return;
        saveValues();
        entry.setHost(getString(HOST));
        entry.setAddress(getString(ADDRESS));
        try {
            entry.setPort(Integer.parseInt(getString(PORT)));
        } catch (NumberFormatException e) {
            entry.setPort(0);
        }
        entry.setPath(getString(PATH));
        entry.setNote(getString(NOTE));
        entry.setType("MCP");
        super.okPressed();
    }
}
