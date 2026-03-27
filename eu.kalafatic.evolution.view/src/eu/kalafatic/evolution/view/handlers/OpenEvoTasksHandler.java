package eu.kalafatic.evolution.view.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.ide.IDE;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;

public class OpenEvoTasksHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            for (Object element : ((IStructuredSelection) selection).toList()) {
                IFile fileToOpen = null;
                if (element instanceof IFile) {
                    fileToOpen = (IFile) element;
                } else if (element instanceof EvoProject) {
                    EvoProject ep = (EvoProject) element;
                    if (ep.eResource() != null) {
                        URI uri = ep.eResource().getURI();
                        if (uri.isPlatformResource()) {
                            String path = uri.toPlatformString(true);
                            IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
                            if (res instanceof IFile) {
                                fileToOpen = (IFile) res;
                            }
                        }
                    }
                }

                if (fileToOpen != null) {
                    try {
                        IDE.openEditor(page, fileToOpen, MultiPageEditor.ID);
                    } catch (PartInitException e) {
                        MessageDialog.openError(page.getWorkbenchWindow().getShell(), "Error", "Could not open Evo Editor: " + e.getMessage());
                    }
                }
            }
        }
        return null;
    }
}
