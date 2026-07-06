package eu.kalafatic.evolution.view.editors.pages.mcpsettings;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.McpSettingsPage;
import eu.kalafatic.utils.factories.GUIFactory;

public class McpConfigGroup extends AEvoGroup {
    private Text mcpUrlText;
    private Button mcpEnabledBtn;
    private Text mcpPortText;
    private org.eclipse.swt.widgets.Label statusLabel;
    private McpSettingsPage page;

    public McpConfigGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, McpSettingsPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "MCP Configuration", 2, true);

        mcpEnabledBtn = toolkit.createButton(group, "Enable Local MCP Server", org.eclipse.swt.SWT.CHECK);
        mcpEnabledBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (orchestrator != null && orchestrator.getServerSettings() != null) {
                    orchestrator.getServerSettings().setMcpEnabled(mcpEnabledBtn.getSelection());
                    page.setDirty(true);
                }
            }
        });
        GUIFactory.INSTANCE.createLabel(group, "");

        GUIFactory.INSTANCE.createLabel(group, "Local MCP Port:");
        mcpPortText = GUIFactory.INSTANCE.createText(group);
        mcpPortText.addModifyListener(e -> {
            if (orchestrator != null && orchestrator.getServerSettings() != null) {
                try {
                    orchestrator.getServerSettings().setMcpPort(Integer.parseInt(mcpPortText.getText()));
                    page.setDirty(true);
                } catch (NumberFormatException ex) {}
            }
        });

        GUIFactory.INSTANCE.createLabel(group, "Remote Server URL:");
        mcpUrlText = GUIFactory.INSTANCE.createText(group);
        mcpUrlText.addModifyListener(e -> {
            if (orchestrator != null) {
                orchestrator.setMcpServerUrl(mcpUrlText.getText());
                page.setDirty(true);
            }
        });

        Button testBtn = GUIFactory.INSTANCE.createButton(group, "Test Connection", 150);
        testBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.testConnection(mcpUrlText.getText());
            }
        });

        Button requestBtn = GUIFactory.INSTANCE.createButton(group, "Test Request", 150);
        requestBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                page.openRequestDialog(mcpUrlText.getText());
            }
        });

        GUIFactory.INSTANCE.createLabel(group, "Status:");
        statusLabel = GUIFactory.INSTANCE.createLabel(group, "Unknown");
    }

    public void setStatus(boolean success, String message) {
        if (group == null || group.isDisposed()) return;
        org.eclipse.swt.widgets.Display.getDefault().asyncExec(() -> {
            if (group.isDisposed()) return;
            statusLabel.setText(message != null ? message : (success ? "Connected" : "Disconnected"));
            group.setBackground(success ? lightGreen : lightRed);
            // Also set background for child labels/controls if needed, but group background usually propagates
        });
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            setTextSafe(mcpUrlText, orchestrator.getMcpServerUrl());
            if (orchestrator.getServerSettings() != null) {
                mcpEnabledBtn.setSelection(orchestrator.getServerSettings().isMcpEnabled());
                setTextSafe(mcpPortText, String.valueOf(orchestrator.getServerSettings().getMcpPort()));
            }
        }
    }

    public String getUrl() {
        return mcpUrlText.getText();
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { mcpUrlText };
    }
}
