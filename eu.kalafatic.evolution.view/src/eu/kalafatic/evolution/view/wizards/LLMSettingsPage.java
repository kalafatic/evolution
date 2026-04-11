package eu.kalafatic.evolution.view.wizards;

import java.util.HashMap;
import java.util.Map;

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
    private Text modelText, tempText;
    private Button skipCheck;
    
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

        if (orchestrator != null) {
            aiModeCombo.select(orchestrator.getAiMode().getValue());
            String remoteModel = orchestrator.getRemoteModel();

            // Set default to deepseek if not configured
            if (remoteModel == null || remoteModel.isEmpty()) {
                remoteModel = "deepseek";
                orchestrator.setRemoteModel(remoteModel);
            }

            if (remoteModel != null) {
                int index = aiRemoteCombo.indexOf(remoteModel);
                if (index >= 0) aiRemoteCombo.select(index);
            }
            boolean remoteVisible = orchestrator.getAiMode() == AiMode.HYBRID || orchestrator.getAiMode() == AiMode.REMOTE;
            remoteLabel.setVisible(remoteVisible);
            aiRemoteCombo.setVisible(remoteVisible);
        } else {
            aiModeCombo.select(0);
            remoteLabel.setVisible(false);
            aiRemoteCombo.setVisible(false);
        }
        final Group groupLinks = SWTFactory.createGroup(container, "Setup", 2);
        
        aiModeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (orchestrator != null) {
                	AiMode aiMode = AiMode.get(aiModeCombo.getSelectionIndex());
                    orchestrator.setAiMode(aiMode);
                            
                    boolean remoteVisible = aiMode == AiMode.HYBRID || aiMode == AiMode.REMOTE;
                    remoteLabel.setVisible(remoteVisible);
                    aiRemoteCombo.setVisible(remoteVisible);
                    
                    
                    groupLinks.setVisible(remoteVisible);
                    
                    groupMode.layout(true, true);
                    groupLinks.layout(true, true);
                }
            }
        });

        new Label(groupLinks, SWT.NONE).setText("LLM Model:");
        modelText = new Text(groupLinks, SWT.BORDER);
        modelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        modelText.setText("gpt-4o");

        new Label(groupLinks, SWT.NONE).setText("Temperature:");
        tempText = new Text(groupLinks, SWT.BORDER);
        tempText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tempText.setText("1.0");

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
                    modelText.setText(tempOrch.getOllama().getModel());
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
                    modelText.setText(tempOrch.getLlm().getModel());
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
        if (visible && orchestrator != null && orchestrator.getOllama() != null) {
            String ollamaModel = orchestrator.getOllama().getModel();
            if (ollamaModel != null && !ollamaModel.isEmpty() && (modelText.getText().equals("gpt-4o") || modelText.getText().isEmpty())) {
                modelText.setText(ollamaModel);
            }
        }
    }

    public String getLlmModel() { return modelText.getText(); }
    public String getTemperature() { return tempText.getText(); }
    public boolean isSkipped() { return skipCheck.getSelection(); }
}
