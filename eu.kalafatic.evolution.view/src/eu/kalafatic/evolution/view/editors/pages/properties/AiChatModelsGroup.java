package eu.kalafatic.evolution.view.editors.pages.properties;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.FormToolkit;
import eu.kalafatic.evolution.controller.manager.ProjectModelManager;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.model.orchestration.AIProvider;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.AEvoGroup;
import eu.kalafatic.evolution.view.editors.pages.PropertiesPage;
import eu.kalafatic.utils.factories.GUIFactory;

public class AiChatModelsGroup extends AEvoGroup {
    private Text localModelText, hybridModelText;
    private Combo remoteModelCombo;
    private PropertiesPage page;
    private ControlDecoration localModelDecorator, hybridModelDecorator, remoteModelDecorator;

    public AiChatModelsGroup(FormToolkit toolkit, Composite parent, MultiPageEditor editor, Orchestrator orchestrator, PropertiesPage page) {
        super(editor, orchestrator);
        this.page = page;
        createControl(toolkit, parent);
    }

    private void createControl(FormToolkit toolkit, Composite parent) {
        group = GUIFactory.INSTANCE.createExpandableGroup(toolkit, parent, "AI Chat Models (per Mode)", 3, false);
        GUIFactory.INSTANCE.createLabel(group, "Local Model:");
        localModelText = GUIFactory.INSTANCE.createText(group);
        GUIFactory.INSTANCE.createEditButton(group, localModelText);
        GUIFactory.INSTANCE.createLabel(group, "Hybrid Model:");
        hybridModelText = GUIFactory.INSTANCE.createText(group);
        GUIFactory.INSTANCE.createEditButton(group, hybridModelText);
        GUIFactory.INSTANCE.createLabel(group, "Remote Model:");
        remoteModelCombo = GUIFactory.INSTANCE.createCombo(group);
        remoteModelCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
        for (String providerName : AiProviders.PROVIDERS.keySet()) {
            remoteModelCombo.add(providerName);
        }
        remoteModelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String provider = remoteModelCombo.getText();
                ProviderConfig config = AiProviders.PROVIDERS.get(provider);
                if (config != null) {
                    page.updateAiChatUrl(config.getEndpointUrl());
                }
                page.syncModelWithUI();
            }
        });

        localModelDecorator = new ControlDecoration(localModelText, SWT.TOP | SWT.LEFT);
        localModelDecorator.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        localModelDecorator.hide();

        hybridModelDecorator = new ControlDecoration(hybridModelText, SWT.TOP | SWT.LEFT);
        hybridModelDecorator.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        hybridModelDecorator.hide();

        remoteModelDecorator = new ControlDecoration(remoteModelCombo, SWT.TOP | SWT.LEFT);
        remoteModelDecorator.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION).getImage());
        remoteModelDecorator.hide();
    }

    @Override
    protected void refreshUI() {
        if (orchestrator != null) {
            String localModel = orchestrator.getLocalModel() != null ? orchestrator.getLocalModel() : "";
            String hybridModel = orchestrator.getHybridModel() != null ? orchestrator.getHybridModel() : "";
            String remoteModel = orchestrator.getRemoteModel() != null ? orchestrator.getRemoteModel() : "";

            setTextSafe(localModelText, localModel);
            setTextSafe(hybridModelText, hybridModel);
            if (orchestrator.getRemoteModel() != null) {
                selectSafe(remoteModelCombo, remoteModel);
            }

            // Verify local/hybrid models
            new Thread(() -> {
                java.util.List<AIProvider> allModels = ProjectModelManager.getInstance().getAllModels(orchestrator);
                boolean ollamaOnline = allModels.stream().noneMatch(m -> "Ollama".equals(m.getName()) && "ERR".equals(m.getState()));

                Display.getDefault().asyncExec(() -> {
                    if (localModelText.isDisposed()) return;

                    if (ollamaOnline) {
                        verifyOllamaModel(localModel, localModelDecorator, allModels);
                        verifyOllamaModel(hybridModel, hybridModelDecorator, allModels);
                    } else {
                        localModelDecorator.setDescriptionText("Ollama server offline");
                        localModelDecorator.show();
                        hybridModelDecorator.setDescriptionText("Ollama server offline");
                        hybridModelDecorator.show();
                    }
                });
            }).start();

            // Verify remote model token
            eu.kalafatic.evolution.controller.security.TokenSecurityService.ResolvedProvider resolved =
                eu.kalafatic.evolution.controller.security.TokenSecurityService.getInstance().resolve(orchestrator, remoteModel);
            if (resolved != null && (resolved.token == null || resolved.token.isEmpty() || "YOUR_API_KEY".equals(resolved.token))) {
                remoteModelDecorator.setDescriptionText("API Token missing for this model");
                remoteModelDecorator.show();
            } else {
                remoteModelDecorator.hide();
            }
        }
    }

    private void verifyOllamaModel(String modelName, ControlDecoration decorator, java.util.List<AIProvider> models) {
        if (modelName == null || modelName.isEmpty()) {
            decorator.hide();
            return;
        }
        boolean found = models.stream().anyMatch(m -> m.isLocal() && m.getName().equalsIgnoreCase(modelName) && "OK".equals(m.getState()));
        if (!found) {
            decorator.setDescriptionText("Model not found in Ollama");
            decorator.show();
        } else {
            decorator.hide();
        }
    }

    @Override
    public void updateModel() {
        if (orchestrator != null) {
            ProjectModelManager.getInstance().updateLocalModel(orchestrator, localModelText.getText());
            ProjectModelManager.getInstance().updateHybridModel(orchestrator, hybridModelText.getText());
            ProjectModelManager.getInstance().updateRemoteModel(orchestrator, remoteModelCombo.getText());
        }
    }

    @Override
    public Text[] getTextFields() {
        return new Text[] { localModelText, hybridModelText };
    }
}
