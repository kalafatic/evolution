package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class LLMSettingsPage extends WizardPage {
    private Text modelText, tempText;
    private Button skipCheck;

    public LLMSettingsPage() {
        super("LLMSettingsPage");
        setTitle("LLM Settings");
        setDescription("Configure LLM model and parameters.");
    }

    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        new Label(container, SWT.NONE).setText("LLM Model:");
        modelText = new Text(container, SWT.BORDER);
        modelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        modelText.setText("gpt-4o");

        new Label(container, SWT.NONE).setText("Temperature:");
        tempText = new Text(container, SWT.BORDER);
        tempText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        tempText.setText("0.7");

        Link pullModelLink = new Link(container, SWT.NONE);
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

        Link setupLink = new Link(container, SWT.NONE);
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

    public String getLlmModel() { return modelText.getText(); }
    public String getTemperature() { return tempText.getText(); }
    public boolean isSkipped() { return skipCheck.getSelection(); }
}
