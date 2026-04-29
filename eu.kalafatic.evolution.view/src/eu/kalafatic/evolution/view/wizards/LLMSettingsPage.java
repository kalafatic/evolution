package eu.kalafatic.evolution.view.wizards;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.model.orchestration.AiMode;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.factories.SWTFactory;

public class LLMSettingsPage extends AWizardPage {
    private Combo modelCombo;
    private Text tempText;
    private Button skipCheck;
    private ControlDecoration modelDecorator;
    
    private Orchestrator orchestrator;
    private StyledText requestText;
    private StyledText responseText;
    private Label ollamaStatusLabel;
    private Label modelStatusLabel;
    private Label statusLabel;
    private ProgressBar progressBar;
    private Composite approvalComposite;
    private Label approvalLabel;
    private Button approveButton;
    private Button rejectButton;
    private TaskContext currentContext;
    private OllamaService ollamaService;
    private Map<String, String> threads = new HashMap<>();
    private Map<String, StyleRange[]> threadStyles = new HashMap<>();
    private String currentThread = "Default";
    private Combo threadCombo;
    private Combo aiModeCombo;    
    private Combo aiRemoteCombo;

    // Colors and Fonts
    private Color colorUser;
    private Color colorEvolution;
    private Color colorPlanner;
    private Color colorArchitect;
    private Color colorJavaDev;
    private Color colorTester;
    private Color colorReviewer;
    private Color colorError;
    private Font chatFont;

    public LLMSettingsPage() {
        super("LLMSettingsPage");
        setTitle("LLM Settings");
        setDescription("Configure LLM model and parameters.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));        
        
        final Group groupMode = SWTFactory.createGroup(container, "Mode", 4);

        SWTFactory.createLabel(groupMode, "AI Mode:");
        aiModeCombo = new Combo(groupMode, SWT.DROP_DOWN | SWT.READ_ONLY);
        GridData gd = new GridData();
		gd.widthHint = 100;
        aiModeCombo.setLayoutData(gd);
        
        for (AiMode mode : AiMode.values()) {
            aiModeCombo.add(mode.getName());
        }

        Label remoteLabel = SWTFactory.createLabel(groupMode, "AI Remote:");
  
        aiRemoteCombo = new Combo(groupMode, SWT.DROP_DOWN | SWT.READ_ONLY);
        aiRemoteCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (String providerName : AiProviders.PROVIDERS.keySet()) {
            aiRemoteCombo.add(providerName);
        }
        aiRemoteCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (orchestrator != null) {
                    orchestrator.setRemoteModel(aiRemoteCombo.getText());
                    
                }
            }
        });

        final Group groupLinks = SWTFactory.createGroup(container, "Setup", 2);
        
        new Label(groupLinks, SWT.NONE).setText("LLM Model:");
        modelCombo = new Combo(groupLinks, SWT.DROP_DOWN | SWT.READ_ONLY);
        modelCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        aiModeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (orchestrator != null) {
                	AiMode aiMode = AiMode.get(aiModeCombo.getSelectionIndex());
                    orchestrator.setAiMode(aiMode);

                    // Update models based on mode
                    java.util.List<String> models = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance().getLlmModels(orchestrator, aiMode);
                    String current = modelCombo.getText();
                    modelCombo.removeAll(); // clear
                    for (String m : models) {
                        modelCombo.add(m);
                    }

                    // Restore current if still exists
                    for (int i = 0; i < modelCombo.getItemCount(); i++) {
                        if (modelCombo.getItem(i).equals(current)) {
                            modelCombo.select(i);
                            break;
                        }
                    }

                    if (modelCombo.getText().isEmpty() && modelCombo.getItemCount() > 0) {
                        modelCombo.select(0);
                    }
                            
                    boolean remoteVisible = aiMode != null && (aiMode.getValue() == AiMode.HYBRID_VALUE || aiMode.getValue() == AiMode.REMOTE_VALUE);
                    remoteLabel.setVisible(remoteVisible);
                    aiRemoteCombo.setVisible(remoteVisible);
                    
                    groupLinks.setVisible(true);
                    groupLinks.setEnabled(aiMode != null && aiMode.getValue() != AiMode.LOCAL_VALUE);
                    
                    groupMode.layout(true, true);
                    groupLinks.layout(true, true);
                    container.layout(true, true);
                }
            }
        });

        if (orchestrator != null) {
            aiModeCombo.select(orchestrator.getAiMode().getValue());
            String remoteModel = orchestrator.getRemoteModel();

            if (remoteModel != null) {
                int index = aiRemoteCombo.indexOf(remoteModel);
                if (index >= 0) aiRemoteCombo.select(index);
            }

            AiMode aiMode = orchestrator.getAiMode();
            boolean remoteVisible = aiMode.getValue() == AiMode.HYBRID_VALUE || aiMode.getValue() == AiMode.REMOTE_VALUE;
            remoteLabel.setVisible(remoteVisible);
            aiRemoteCombo.setVisible(remoteVisible);

            // Initial population of model combo
            java.util.List<String> models = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance().getLlmModels(orchestrator, aiMode);
            for (String m : models) {
                modelCombo.add(m);
            }
            if (modelCombo.getItemCount() > 0) {
                modelCombo.select(0);
            }
            groupLinks.setEnabled(aiMode.getValue() != AiMode.LOCAL_VALUE);
        } else {
            aiModeCombo.select(0);
            remoteLabel.setVisible(false);
            aiRemoteCombo.setVisible(false);
            groupLinks.setEnabled(false);
        }

        modelDecorator = new ControlDecoration(modelCombo, SWT.TOP | SWT.LEFT);
        modelDecorator.setImage(FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage());
        modelDecorator.setDescriptionText("Model name is required. Use 'Setup LLM...' link to configure.");
        modelDecorator.setShowOnlyOnFocus(false);

        modelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                validateModel();
            }
        });

        new Label(groupLinks, SWT.NONE).setText("Temperature:");
        tempText = new Text(groupLinks, SWT.BORDER);
        tempText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Link pullModelLink = new Link(groupLinks, SWT.NONE);
        pullModelLink.setText("<a>Setup/Pull Ollama Model...</a>");
        pullModelLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
        pullModelLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;
                Orchestrator tempOrch = factory.createOrchestrator();
                tempOrch.setOllama(factory.createOllama());
                SetupOllamaModelWizard wizard = new SetupOllamaModelWizard(tempOrch);
                WizardDialog dialog = new WizardDialog(getShell(), wizard);
                if (dialog.open() == WizardDialog.OK) {
                    setModelComboText(tempOrch.getOllama().getModel());
                }
            }
        });

        Link setupLink = new Link(groupLinks, SWT.NONE);
        setupLink.setText("<a>Setup LLM...</a>");
        setupLink.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
        setupLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;
                Orchestrator tempOrch = factory.createOrchestrator();
                tempOrch.setLlm(factory.createLLM());
                tempOrch.setAiChat(factory.createAiChat());

                SetupLLMWizard wizard = new SetupLLMWizard(tempOrch);
                WizardDialog dialog = new WizardDialog(getShell(), wizard);
                if (dialog.open() == WizardDialog.OK) {
                    setModelComboText(tempOrch.getLlm().getModel());
                    tempText.setText(String.valueOf(tempOrch.getLlm().getTemperature()));
                }
            }
        });

        skipCheck = new Button(container, SWT.CHECK);
        skipCheck.setText("Skip this step and setup later");
        skipCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, true, false, 2, 1));

        setControl(container);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible && orchestrator != null) {
            if (modelCombo.getItemCount() == 0) {
                AiMode aiMode = orchestrator.getAiMode();
                java.util.List<String> models = eu.kalafatic.evolution.controller.manager.ProjectModelManager.getInstance().getLlmModels(orchestrator, aiMode);
                for (String m : models) {
                    modelCombo.add(m);
                }
            }

            if (orchestrator.getOllama() != null) {
                String ollamaModel = orchestrator.getOllama().getModel();
                if (ollamaModel != null && !ollamaModel.isEmpty() && (modelCombo.getText().equals("gpt-4o") || modelCombo.getText().isEmpty())) {
                    setModelComboText(ollamaModel);
                }
            }

            if (modelCombo.getText().isEmpty()) {
                setModelComboText("gpt-4o");
            }
        }
    }

    private void setModelComboText(String model) {
        if (model == null || model.isEmpty()) return;
        int index = modelCombo.indexOf(model);
        if (index == -1) {
            modelCombo.add(model);
            index = modelCombo.indexOf(model);
        }
        modelCombo.select(index);
        validateModel();
    }

    private void validateModel() {
        if (modelCombo.getText().isEmpty()) {
            modelDecorator.show();
        } else {
            modelDecorator.hide();
        }
    }

    public String getLlmModel() { return modelCombo.getText(); }
    public String getTemperature() { return tempText.getText(); }
    public boolean isSkipped() { return skipCheck.getSelection(); }
}
