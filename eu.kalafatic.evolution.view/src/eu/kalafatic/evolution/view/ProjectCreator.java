package eu.kalafatic.evolution.view;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.resources.ProjectExplorer;

public class ProjectCreator {

    public static void createAndShowProject(String projectName) {
        IProgressMonitor monitor = new NullProgressMonitor();

        try {
            // 1. Get workspace root
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

            // 2. Get or define the project
            IProject project = root.getProject(projectName);

            if (project.exists()) {
                System.out.println("Project already exists: " + projectName);
                selectAndReveal(project);
                return;
            }

            // 3. Create project description (plain project, no nature by default)
            IProjectDescription desc = project.getWorkspace().newProjectDescription(projectName);
            // Optional: add natures if needed, e.g. Java nature
            // desc.setNatureIds(new String[] { JavaCore.NATURE_ID });

            // 4. Create & open the project
            project.create(desc, monitor);
            project.open(monitor);

            System.out.println("Created project: " + projectName);

            // 5. Make it visible & selected in Project Explorer
            selectAndReveal(project);

        } catch (CoreException ex) {
            ex.printStackTrace();
            // In real code → show error dialog
        }
    }
    

    /**
     * Selects the project in Project Explorer and reveals it.
     */
    public static void selectAndReveal(Object element) {
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) return;

        IWorkbenchPage page = window.getActivePage();
        if (page == null) return;

        // Find Project Explorer view
        ProjectExplorer explorer = (ProjectExplorer) page.findView("org.eclipse.ui.navigator.ProjectExplorer");
        if (explorer == null) {
            // Fallback: try to show it first
            try {
                explorer = (ProjectExplorer) page.showView("org.eclipse.ui.navigator.ProjectExplorer");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        if (explorer != null) {
            // Set selection
            ISelection selection = new StructuredSelection(element);
            ISelectionProvider provider = explorer.getSite().getSelectionProvider();
            provider.setSelection(selection);

            // Reveal the element (scroll to it)
            explorer.getCommonViewer().reveal(element);
        }
    }

    // Example usage – call this from your handler / button / etc.
    public static void mainExample() {
        createAndShowProject("MyNewCustomProject");
    }
}