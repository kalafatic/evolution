package eu.kalafatic.evolution.view.provider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.OrchestratorEditorInput;

public class OrchestrationEditorActionProvider extends CommonActionProvider {

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
    }

    @Override
    public void fillActionBars(org.eclipse.ui.IActionBars actionBars) {
        ISelection selection = getContext().getSelection();
        if (selection instanceof IStructuredSelection) {
            final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
            Action openAction = null;

            if (firstElement instanceof Orchestrator) {
                openAction = new Action("Open Orchestration") {
                    @Override
                    public void run() {
                        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                        try {
                            page.openEditor(new OrchestratorEditorInput((Orchestrator) firstElement), "eu.kalafatic.evolution.view.editors.MultiPageEditor");
                        } catch (PartInitException e) {
                            e.printStackTrace();
                        }
                    }
                };
            } else if (firstElement instanceof EvoProject) {
                openAction = new Action("Open Evo Project") {
                    @Override
                    public void run() {
                        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                        EvoProject ep = (EvoProject) firstElement;
                        if (ep.eResource() != null) {
                            URI uri = ep.eResource().getURI();
                            if (uri.isPlatformResource()) {
                                String path = uri.toPlatformString(true);
                                IResource res = org.eclipse.core.resources.ResourcesPlugin.getWorkspace().getRoot().findMember(path);
                                if (res instanceof IFile) {
                                    try {
                                        org.eclipse.ui.ide.IDE.openEditor(page, (IFile) res);
                                    } catch (PartInitException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                };
            }

            if (openAction != null) {
                actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
            }
        }
    }
}
