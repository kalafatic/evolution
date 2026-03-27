package eu.kalafatic.evolution.view.provider;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.view.editors.OrchestratorEditorInput;

public class OrchestrationEditorActionProvider extends CommonActionProvider {

    private Action refreshAction;
    private Action deleteAction;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        makeActions();
    }

    private void makeActions() {
        refreshAction = new Action("Refresh") {
            @Override
            public void run() {
                getActionSite().getStructuredViewer().refresh();
            }
        };
        refreshAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED));

        deleteAction = new Action("Delete") {
            @Override
            public void run() {
                ISelection selection = getContext().getSelection();
                if (selection instanceof IStructuredSelection) {
                    for (Object obj : ((IStructuredSelection) selection).toList()) {
                        if (obj instanceof IResource) {
                            try {
                                ((IResource) obj).delete(true, null);
                            } catch (org.eclipse.core.runtime.CoreException e) {
                                e.printStackTrace();
                            }
                        } else if (obj instanceof EObject) {
                            EObject eobj = (EObject) obj;
                            if (eobj.eContainer() != null) {
                                org.eclipse.emf.edit.command.DeleteCommand.create(
                                    org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain.getEditingDomainFor(eobj),
                                    eobj).execute();
                            }
                        }
                    }
                    getActionSite().getStructuredViewer().refresh();
                }
            }
        };
        deleteAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
    }

    @Override
    public void fillContextMenu(IMenuManager menu) {
        ISelection selection = getContext().getSelection();
        if (selection instanceof IStructuredSelection) {
            final Object firstElement = ((IStructuredSelection) selection).getFirstElement();
            Action openAction = createOpenAction(firstElement);
            if (openAction != null) {
                menu.insertAfter(ICommonMenuConstants.GROUP_OPEN, openAction);
            }
        }
        menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, deleteAction);
        menu.appendToGroup(ICommonMenuConstants.GROUP_BUILD, refreshAction);
        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
    }

    private Action createOpenAction(final Object element) {
        if (element instanceof IFile) {
            return new Action("Open File") {
                @Override
                public void run() {
                    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
                    try {
                        IDE.openEditor(page, (IFile) element);
                    } catch (PartInitException e) {
                        e.printStackTrace();
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
                            IResource res = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
                            if (res instanceof IFile) {
                                try {
                                    IDE.openEditor(page, (IFile) res);
                                } catch (PartInitException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            };
        } else if (element instanceof Orchestrator) {
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
        }
        return null;
    }

    private void openOrchestrator(Orchestrator orchestrator) {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        try {
            page.openEditor(new OrchestratorEditorInput(orchestrator), eu.kalafatic.evolution.view.editors.MultiPageEditor.ID);
        } catch (PartInitException e) {
            MessageDialog.openError(page.getWorkbenchWindow().getShell(), "Error", "Could not open MultiPageEditor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void fillActionBars(org.eclipse.ui.IActionBars actionBars) {
        Action openAction = new Action("Open") {
            @Override
            public void run() {
                ISelection selection = getContext().getSelection();
                if (selection instanceof IStructuredSelection) {
                    Object firstElement = ((IStructuredSelection) selection).getFirstElement();
                    Action dynamicOpen = createOpenAction(firstElement);
                    if (dynamicOpen != null) {
                        dynamicOpen.run();
                    }
                }
            }
        };
        actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAction);
        actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);
        actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteAction);
        actionBars.updateActionBars();
    }
}
