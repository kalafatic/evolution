package eu.kalafatic.evolution.view.editors.pages.factories;

import org.eclipse.ui.PartInitException;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.McpSettingsPage;

public class McpSettingsPageFactory {
    public static McpSettingsPage createMcpSettingsPage(MultiPageEditor editor, Orchestrator orchestrator) throws PartInitException {
        McpSettingsPage page = new McpSettingsPage(editor.getContainer(), editor, orchestrator);
        int index = editor.addPage(page);
        editor.setPageText(index, "MCP Settings");
        return page;
    }
}
