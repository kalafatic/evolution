package eu.kalafatic.evolution.view.wizards;

import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import eu.kalafatic.evolution.model.orchestration.*;
import java.util.Collections;
import org.eclipse.swt.layout.GridData;

public class NewEvoProjectWizard extends Wizard implements INewWizard {
    private NewEvoProjectPage page;

    public NewEvoProjectWizard() {
        setWindowTitle("New Evo Project");
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
    }

    @Override
    public void addPages() {
        page = new NewEvoProjectPage();
        addPage(page);
    }

    @Override
    public boolean performFinish() {
        final String projectName = page.getProjectName();
        final String fileName = page.getFileName();

        try {
            IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            if (!project.exists()) {
                project.create(null);
            }
            if (!project.isOpen()) {
                project.open(null);
            }

            String filePath = project.getLocation().append(fileName).toOSString();
            ResourceSet resSet = new ResourceSetImpl();
            resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
            resSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());

            URI fileURI = URI.createFileURI(filePath);
            Resource resource = resSet.createResource(fileURI);

            OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;

            EvoProject evoProject = factory.createEvoProject();
            evoProject.setName(projectName);

            Orchestrator orchestrator = factory.createOrchestrator();
            orchestrator.setName("Initial Orchestration");
            orchestrator.setId("orch1");

            // Default Git
            Git git = factory.createGit();
            git.setRepositoryUrl("https://github.com/kalafatic/evolution.git");
            git.setBranch("main");
            git.setUsername("admin");
            git.setLocalPath("./repo");
            orchestrator.setGit(git);

            // Default Ollama
            Ollama ollama = factory.createOllama();
            ollama.setUrl("http://localhost:11434");
            ollama.setModel("llama3");
            ollama.setPath("/usr/bin/ollama");
            orchestrator.setOllama(ollama);

            // Default LLM
            LLM llm = factory.createLLM();
            llm.setModel("gpt-4");
            llm.setTemperature(0.7f);
            orchestrator.setLlm(llm);

            // Default Maven
            Maven maven = factory.createMaven();
            maven.getGoals().add("clean");
            maven.getGoals().add("install");
            orchestrator.setMaven(maven);

            // Default AiChat
            AiChat aiChat = factory.createAiChat();
            aiChat.setUrl("http://localhost:8080/ai");
            aiChat.setToken("ENTER_TOKEN_HERE");
            aiChat.setPrompt("You are a helpful assistant.");
            orchestrator.setAiChat(aiChat);

            evoProject.getOrchestrations().add(orchestrator);
            resource.getContents().add(evoProject);

            resource.save(Collections.EMPTY_MAP);
            project.refreshLocal(IProject.DEPTH_INFINITE, null);

        } catch (Exception e) {
            MessageDialog.openError(getShell(), "Error", "Could not create project: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private class NewEvoProjectPage extends WizardPage {
        private Text projectNameText;
        private Text fileNameText;

        protected NewEvoProjectPage() {
            super("NewEvoProjectPage");
            setTitle("Evo Project Details");
            setDescription("Enter the name for your new Evo project and the configuration file name.");
        }

        @Override
        public void createControl(Composite parent) {
            Composite container = new Composite(parent, SWT.NONE);
            container.setLayout(new GridLayout(2, false));

            new Label(container, SWT.NONE).setText("Project Name:");
            projectNameText = new Text(container, SWT.BORDER);
            projectNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            projectNameText.setText("MyEvoProject");

            new Label(container, SWT.NONE).setText("Config File Name (.xml):");
            fileNameText = new Text(container, SWT.BORDER);
            fileNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            fileNameText.setText("evo_config.xml");

            setControl(container);
        }

        public String getProjectName() {
            return projectNameText.getText();
        }

        public String getFileName() {
            return fileNameText.getText();
        }
    }
}
