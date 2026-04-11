package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.PropertiesPage;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class AiChatModelsGroup extends AEvoGroup {
    private Text localModelText, hybridModelText;
    private Combo remoteModelCombo;
    private PropertiesPage page;

    public AiChatModelsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, PropertiesPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = SWTFactory.createExpandableGroup(toolkit, parent, "AI Chat Models (per Mode)", 3, false);
        SWTFactory.createLabel(group, "Local Model:");
        localModelText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, localModelText);
        SWTFactory.createLabel(group, "Hybrid Model:");
        hybridModelText = SWTFactory.createText(group);
        SWTFactory.createEditButton(group, hybridModelText);
        SWTFactory.createLabel(group, "Remote Model:");
        remoteModelCombo = SWTFactory.createCombo(group);
        remoteModelCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
        remoteModelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String providerName = remoteModelCombo.getText();
                AIProvider provider = orchestrator.getAiProviders().stream()
                        .filter(p -> p.getName().equalsIgnoreCase(providerName))
                        .findFirst().orElse(null);
                if (provider != null) {
                    page.updateAiChatUrl(provider.getUrl());
                }
                page.syncModelWithUI();
            }
        });
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            AiProviders.initializeProviders(orchestrator);
            String current = remoteModelCombo.getText();
            remoteModelCombo.removeAll();
            for (AIProvider provider : orchestrator.getAiProviders()) {
                remoteModelCombo.add(provider.getName());
            }
            localModelText.setText(orchestrator.getLocalModel() != null ? orchestrator.getLocalModel() : "");
            hybridModelText.setText(orchestrator.getHybridModel() != null ? orchestrator.getHybridModel() : "");
            if (orchestrator.getRemoteModel() != null) {
                int index = remoteModelCombo.indexOf(orchestrator.getRemoteModel());
                if (index >= 0) remoteModelCombo.select(index);
            }
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator != null) {
            orchestrator.setLocalModel(localModelText.getText());
            orchestrator.setHybridModel(hybridModelText.getText());
            orchestrator.setRemoteModel(remoteModelCombo.getText());
        }
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { localModelText, hybridModelText };
    }
}
