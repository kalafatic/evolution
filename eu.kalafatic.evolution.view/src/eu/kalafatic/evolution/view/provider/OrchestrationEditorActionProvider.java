package eu.kalafatic.evolution.view.provider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
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
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.OrchestratorEditorInput;

public class OrchestrationEditorActionProvider extends CommonActionProvider {

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        ISelection selection = getContext().getSelection();
        if (selection instanceof IStructuredSelection) {
            final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
            Action openAction = createOpenAction(firstElement);
            if (openAction != null) {
                menu.add(openAction);
            }
        }
    }

    private Action createOpenAction(final Object element) {
        if (element instanceof Orchestrator) {
            return new Action("Open Orchestration") {
                @Override
                public void run() {
                    openOrchestrator((Orchestrator) element);
                }
            };
        } else if (element instanceof Agent || element instanceof Task) {
            return new Action("Open Parent Orchestration") {
                @Override
                public void run() {
                    EObject current = (EObject) element;
                    while (current != null && !(current instanceof Orchestrator)) {
                        current = current.eContainer();
                    }
                    if (current instanceof Orchestrator) {
                        openOrchestrator((Orchestrator) current);
                    }
                }
            };
        } else if (element instanceof EvoProject) {
            return new Action("Open Evo Project") {
                @Override
                public void run() {
                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    EvoProject ep = (EvoProject) element;
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
        return null;
    }

    private void openOrchestrator(Orchestrator orchestrator) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            page.openEditor(new OrchestratorEditorInput(orchestrator), eu.kalafatic.evolution.view.editors.MultiPageEditor.ID);
        } catch (PartInitException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fillActionBars(org.eclipse.ui.IActionBars actionBars) {
        ISelection selection = getContext().getSelection();
        if (selection instanceof IStructuredSelection) {
            final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
            Action openAction = createOpenAction(firstElement);
            if (openAction != null) {
                actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
            }
        }
    }
}
