package eu.kalafatic.evolution.view.editors.pages.server;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.ServerSettings;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.tools.AToolGroup;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class SettingsGroup extends AToolGroup {

    private Text portText;
    private Button autoStartCheck;
    private Runnable onStateChange;

    public SettingsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, Color successColor, Runnable onStateChange) {
        super(editor, orchestrator, successColor);
        this.onStateChange = onStateChange;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        Section section = toolkit.createSection(parent, Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Server Settings");
        section.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        group = toolkit.createComposite(section);
        group.setLayout(new GridLayout(2, false));
        section.setClient(group);

        toolkit.createLabel(group, "Server Port:");
        portText = toolkit.createText(group, "88080");
        portText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        toolkit.createLabel(group, "Auto-start with UI:");
        autoStartCheck = toolkit.createButton(group, "", SWT.CHECK);

        Composite btnComp = toolkit.createComposite(group);
        btnComp.setLayout(new GridLayout(3, false));
        btnComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        Button startBtn = SWTFactory.createButton(btnComp, "Start");
        startBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    ServerManager.getInstance().start(Integer.parseInt(portText.getText()));
                    if (onStateChange != null) onStateChange.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        Button stopBtn = SWTFactory.createButton(btnComp, "Stop");
        stopBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                ServerManager.getInstance().stop();
                if (onStateChange != null) onStateChange.run();
            }
        });

        Button restartBtn = SWTFactory.createButton(btnComp, "Restart");
        restartBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    ServerManager.getInstance().restart();
                    if (onStateChange != null) onStateChange.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void refreshUI() {
        if (orchestrator == null) return;
        ServerSettings settings = orchestrator.getServerSettings();
        if (settings != null) {
            portText.setText(String.valueOf(settings.getPort()));
            autoStartCheck.setSelection(settings.isAutoStart());
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator == null) return;
        ServerSettings settings = orchestrator.getServerSettings();
        if (settings == null) {
            settings = OrchestrationFactory.eINSTANCE.createServerSettings();
            orchestrator.setServerSettings(settings);
        }
        try {
            settings.setPort(Integer.parseInt(portText.getText()));
        } catch (NumberFormatException e) {
            // keep old value
        }
        settings.setAutoStart(autoStartCheck.getSelection());
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { portText };
    }

    @Override
    protected String getTestStatus() {
        return null;
    }

    @Override
    protected void clearTestStatus() {
    }
}
