package eu.kalafatic.evolution.view.handlers;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class OpenSourceHandler {
    public static void open(String relativePath) {
        if (relativePath == null) return;

        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();

        for (IProject project : projects) {
            IFile file = project.getFile(new Path(relativePath));
            if (file.exists()) {
                try {
                    IDE.openEditor(page, file);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
