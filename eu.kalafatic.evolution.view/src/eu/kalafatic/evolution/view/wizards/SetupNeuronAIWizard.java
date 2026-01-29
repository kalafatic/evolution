package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import org.eclipse.swt.layout.GridData;

public class SetupNeuronAIWizard extends Wizard implements INewWizard {
    private Orchestrator orchestrator;
    private SetupNeuronAIPage page;

    public SetupNeuronAIWizard() {
        setWindowTitle("Setup Neuron AI");
    }

    public SetupNeuronAIWizard(Orchestrator orchestrator) {
        this();
        this.orchestrator = orchestrator;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        if (orchestrator == null && selection != null && !selection.isEmpty()) {
            Object first = selection.getFirstElement();
            if (first instanceof Orchestrator) {
                orchestrator = (Orchestrator) first;
            }
        }
    }

    @Override
    public void addPages() {
        page = new SetupNeuronAIPage();
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        if (orchestrator != null) {
            NeuronAI neuronAI = orchestrator.getNeuronAI();
            if (neuronAI == null) {
                neuronAI = OrchestrationFactory.eINSTANCE.createNeuronAI();
                orchestrator.setNeuronAI(neuronAI);
            }
            neuronAI.setUrl(page.getUrl());
            neuronAI.setModel(page.getModel());
        }
        return true;
    }

    private class SetupNeuronAIPage extends WizardPage {
        private Text urlText;
        private Text modelText;

        protected SetupNeuronAIPage() {
            super("SetupNeuronAIPage");
            setTitle("Setup Neuron AI");
            setDescription("Enter Neuron AI URL and model name.");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));

            new Label(container, SWT.NONE).setText("URL:");
            urlText = new Text(container, SWT.BORDER);
            urlText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            String currentUrl = (orchestrator != null && orchestrator.getNeuronAI() != null) ? orchestrator.getNeuronAI().getUrl() : "http://localhost:8080/neuron";
            urlText.setText(currentUrl);

            new Label(container, SWT.NONE).setText("Model:");
            modelText = new Text(container, SWT.BORDER);
            modelText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            String currentModel = (orchestrator != null && orchestrator.getNeuronAI() != null) ? orchestrator.getNeuronAI().getModel() : "default-neuron-model";
            modelText.setText(currentModel);

            setControl(container);
        }

        public String getUrl() { return urlText.getText(); }
        public String getModel() { return modelText.getText(); }
    }
}
