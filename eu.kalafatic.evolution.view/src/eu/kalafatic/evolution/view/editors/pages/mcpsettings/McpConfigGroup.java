package eu.kalafatic.evolution.view.editors.pages.mcpsettings;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.pages.McpSettingsPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class McpConfigGroup {
    private Composite group;
    private Text mcpUrlText;
    private Orchestrator orchestrator;
    private McpSettingsPage page;

    public McpConfigGroup(FormToolkit toolkit, Composite parent, Orchestrator orchestrator, McpSettingsPage page) {
        this.orchestrator = orchestrator;
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(parent, "MCP Configuration", 3, true);
        SWTFactory.createLabel(group, "Server URL:");
        mcpUrlText = SWTFactory.createText(group);
        mcpUrlText.addModifyListener(e -> {
            if (orchestrator != null) {
                orchestrator.setMcpServerUrl(mcpUrlText.getText());
                page.setDirty(true);
            }
        });

        Button testBtn = SWTFactory.createButton(group, "Test Connection");
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.testConnection(mcpUrlText.getText());
            }
        });
    }

    public void updateUI() {
        if (orchestrator != null) {
            mcpUrlText.setText(orchestrator.getMcpServerUrl() != null ? orchestrator.getMcpServerUrl() : "");
        }
    }

    public String getUrl() {
        return mcpUrlText.getText();
    }
}
