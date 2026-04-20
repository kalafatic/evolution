package eu.kalafatic.evolution.view.editors;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.MultiPageEditorPart;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.model.orchestration.Task;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.listeners.EditorResourceChangeListener;
import eu.kalafatic.evolution.view.editors.listeners.EditorSelectionListener;
import eu.kalafatic.evolution.view.editors.pages.AiChatPage;
import eu.kalafatic.evolution.view.editors.pages.AiFlowPage;
import eu.kalafatic.evolution.view.editors.pages.ApprovalPage;
import eu.kalafatic.evolution.view.editors.pages.BrowserPage;
import eu.kalafatic.evolution.view.editors.pages.ContextPage;
import eu.kalafatic.evolution.view.editors.pages.GraphPage;
import eu.kalafatic.evolution.view.editors.pages.IterationPage;
import eu.kalafatic.evolution.view.editors.pages.McpSettingsPage;
import eu.kalafatic.evolution.view.editors.compare.ResourceCompareInput.StringElement;
import eu.kalafatic.evolution.view.editors.pages.ComparePage;
import eu.kalafatic.evolution.view.editors.pages.PeerReviewPage;
import eu.kalafatic.evolution.view.editors.pages.PreviewPage;
import eu.kalafatic.evolution.view.editors.pages.PropertiesPage;
import eu.kalafatic.evolution.view.editors.pages.ServerPage;
import eu.kalafatic.evolution.view.editors.pages.TaskStackPage;
import eu.kalafatic.evolution.view.editors.pages.TestsPage;
import eu.kalafatic.evolution.view.editors.pages.ToolsPage;

public class MultiPageEditor extends MultiPageEditorPart {

    public static final String ID = "eu.kalafatic.evolution.view.editors.MultiPageEditor";
    private TextEditor textEditor;
    private AiChatPage aiChatPage;
    private PropertiesPage propertiesPage;
    private McpSettingsPage mcpSettingsPage;
    private PreviewPage previewPage;
    private GraphPage graphPage;
    private BrowserPage browserPage;
    private AiFlowPage aiFlowPage;
    private ApprovalPage approvalPage;
    private ToolsPage toolsPage;
    private TestsPage testsPage;
    private IterationPage iterationPage;
    private TaskStackPage taskStackPage;
    private ContextPage contextPage;
    private PeerReviewPage peerReviewPage;
    private ComparePage comparePage;
    private ServerPage serverPage;

    private Orchestrator orchestrator;
    private TaskContext currentContext;
    private boolean isDirty = false;
    private ResourceSet resourceSet;
    private Resource resource;
    private EditorResourceChangeListener resourceListener;
    private EditorSelectionListener selectionListener;
    
    private Color lightGreen, lightRed, lightOrange;
    
    private AtomicBoolean refreshScheduled = new AtomicBoolean(false);

    private Adapter modelAdapter = new EContentAdapter() {
        @Override
        public void notifyChanged(Notification notification) {
            super.notifyChanged(notification);
            if (notification.isTouch()) return;

            if (notification.getEventType() == Notification.SET ||
                notification.getEventType() == Notification.ADD ||
                notification.getEventType() == Notification.REMOVE) {
                setDirty(true);
            }
                       
            if (refreshScheduled.compareAndSet(false, true)) {
                Display.getDefault().asyncExec(() -> {
                    refreshScheduled.set(false);

                    if (!getContainer().isDisposed()) {
                        refreshPages();
                    }
                });
            }
        }
    };

    public MultiPageEditor() {
        super();
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
    }

    @Override
    protected void createPages() {
        loadModel();
        try {
        	
        	this.lightGreen = new Color(Display.getDefault(), 220, 255, 220);
            this.lightRed = new Color(Display.getDefault(), 255, 220, 220);
            this.lightOrange = new Color(Display.getDefault(), 255, 240, 200);
            
            if (orchestrator != null) {
                aiChatPage = AiChatPageFactory.createAiChatPage(this, orchestrator);

                textEditor = new TextEditor();
                int index = addPage(textEditor, getEditorInput());
                setPageText(index, "Editor");

                propertiesPage = PropertiesPageFactory.createPropertiesPage(this, orchestrator);
                mcpSettingsPage = McpSettingsPageFactory.createMcpSettingsPage(this, orchestrator);
                previewPage = PreviewPageFactory.createPreviewPage(this, orchestrator);
                browserPage = BrowserPageFactory.createBrowserPage(this, orchestrator);
                aiFlowPage = AiFlowPageFactory.createAiFlowPage(this, orchestrator);
                approvalPage = ApprovalPageFactory.createApprovalPage(this, orchestrator);
                toolsPage = ToolsPageFactory.createToolsPage(this, orchestrator);
                testsPage = TestsPageFactory.createTestsPage(this, orchestrator);
                iterationPage = IterationPageFactory.createIterationPage(this, orchestrator);
                contextPage = ContextPageFactory.createContextPage(this, orchestrator);
                peerReviewPage = PeerReviewPageFactory.createPeerReviewPage(this, orchestrator);
                taskStackPage = TaskStackPageFactory.createTaskStackPage(this, orchestrator);
                graphPage = GraphPageFactory.createGraphPage(this, orchestrator);
                comparePage = ComparePageFactory.createComparePage(this, orchestrator);
                serverPage = ServerPageFactory.createServerPage(this, orchestrator);
            } else {
                Composite placeholder = new Composite(getContainer(), SWT.NONE);
                placeholder.setLayout(new FillLayout());
                Label label = new Label(placeholder, SWT.CENTER);
                label.setText("No Orchestrator Loaded. Please ensure the file contains at least one Orchestration.");
                int index = addPage(placeholder);
                setPageText(index, "Error");
            }
        } catch (PartInitException e) {
            ErrorDialog.openError(getSite().getShell(), "Error creating pages", null, e.getStatus());
        }

        resourceListener = new EditorResourceChangeListener(this);
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener);

        selectionListener = new EditorSelectionListener(this);
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(selectionListener);
    }

    private void loadModel() {
        OrchestrationPackage.eINSTANCE.eClass();
        OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;

        IEditorInput input = getEditorInput();
        if (input instanceof IFileEditorInput) {
            setPartName(((IFileEditorInput) input).getFile().getProject().getName());
            URI uri = URI.createPlatformResourceURI(((IFileEditorInput) input).getFile().getFullPath().toString(), true);
            resource = resourceSet.getResource(uri, true);
            if (resource != null && !resource.getContents().isEmpty()) {
                Object root = resource.getContents().get(0);
                if (root instanceof Orchestrator) {
                    orchestrator = (Orchestrator) root;
                } else if (root instanceof EvoProject) {
                    EvoProject project = (EvoProject) root;
                    if (!project.getOrchestrations().isEmpty()) {
                        orchestrator = project.getOrchestrations().get(0);
                    }
                }
            }
        } else if (input instanceof OrchestratorEditorInput) {
            orchestrator = ((OrchestratorEditorInput) input).getOrchestrator();
            resource = orchestrator.eResource();
        }

        if (orchestrator != null) {
            orchestrator.eAdapters().add(modelAdapter);
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        if (resource != null) {
            try {
                org.eclipse.ui.actions.WorkspaceModifyOperation operation = new org.eclipse.ui.actions.WorkspaceModifyOperation() {
                    @Override
                    protected void execute(IProgressMonitor monitor) throws org.eclipse.core.runtime.CoreException, java.lang.reflect.InvocationTargetException {
                        try {
                            resource.save(Collections.EMPTY_MAP);
                        } catch (IOException e) {
                            throw new org.eclipse.core.runtime.CoreException(new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.IStatus.ERROR, "eu.kalafatic.evolution.view", e.getMessage(), e));
                        }
                    }
                };
                operation.run(monitor);
                setDirty(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (textEditor != null) {
            textEditor.doSave(monitor);
        }
    }

    @Override
    public void doSaveAs() {
        IEditorPart editor = getEditor(1);
        if (editor != null) {
            editor.doSaveAs();
            setPageText(1, editor.getTitle());
            setInput(editor.getEditorInput());
        }
    }

    @Override
    public boolean isSaveAsAllowed() { return true; }

    @Override
    public void setFocus() {
        int index = getActivePage();
        if (index != -1) getControl(index).setFocus();
    }

    public Object getActivePageInstance() {
        int index = getActivePage();
        if (index != -1) return getControl(index);
        return null;
    }

    @Override
    public void dispose() {
        if (resourceListener != null) ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
        if (selectionListener != null) getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
        if (orchestrator != null) orchestrator.eAdapters().remove(modelAdapter);
        super.dispose();
    }

    @Override
    public boolean isDirty() { return isDirty || (textEditor != null && textEditor.isDirty()); }

    public void setDirty(boolean dirty) {
        if (this.isDirty != dirty) {
            this.isDirty = dirty;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().remove(modelAdapter);
        }
        this.orchestrator = orchestrator;
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().add(modelAdapter);
        }

        if (aiChatPage != null) aiChatPage.setOrchestrator(orchestrator);
        if (propertiesPage != null) propertiesPage.setOrchestrator(orchestrator);
        if (mcpSettingsPage != null) mcpSettingsPage.setOrchestrator(orchestrator);
        if (previewPage != null) previewPage.setOrchestrator(orchestrator);
        if (graphPage != null) graphPage.setOrchestrator(orchestrator);
        if (browserPage != null) browserPage.setOrchestrator(orchestrator);
        if (aiFlowPage != null) aiFlowPage.setOrchestrator(orchestrator);
        if (approvalPage != null) approvalPage.setOrchestrator(orchestrator);
        if (toolsPage != null) toolsPage.setOrchestrator(orchestrator);
        if (testsPage != null) testsPage.setOrchestrator(orchestrator);
        if (iterationPage != null) iterationPage.setOrchestrator(orchestrator);
        if (peerReviewPage != null) peerReviewPage.setOrchestrator(orchestrator);
        if (taskStackPage != null) taskStackPage.setOrchestrator(orchestrator);
        if (contextPage != null) contextPage.setOrchestrator(orchestrator);
        if (serverPage != null) serverPage.setOrchestrator(orchestrator);
    }

    public void reloadModel() {
        loadModel();
        if (orchestrator != null) {
            setOrchestrator(orchestrator);
        }
    }

    public void setCurrentContext(TaskContext context) {
        this.currentContext = context;
    }

    public TaskContext getCurrentContext() {
        return currentContext;
    }

    public void refreshPages() {
        if (orchestrator == null) return;
        if (aiChatPage != null) aiChatPage.scheduleRefresh();
        if (propertiesPage != null) propertiesPage.scheduleRefresh();
        if (toolsPage != null) toolsPage.scheduleRefresh();
        if (taskStackPage != null) taskStackPage.scheduleRefresh();
        if (testsPage != null) testsPage.scheduleRefresh();
        if (iterationPage != null) iterationPage.scheduleRefresh();
        if (peerReviewPage != null) peerReviewPage.scheduleRefresh();
        if (mcpSettingsPage != null) mcpSettingsPage.scheduleRefresh();
        if (contextPage != null) contextPage.refreshUI(); // TODO: refactor ContextPage if needed
        if (serverPage != null) serverPage.scheduleRefresh();
        if (approvalPage != null) approvalPage.scheduleRefresh();
        refreshComparePage();
    }

    private void refreshComparePage() {
        if (comparePage == null || orchestrator == null || getEditorInput() == null) return;
        if (!(getEditorInput() instanceof IFileEditorInput)) return;

        IFile file = ((IFileEditorInput) getEditorInput()).getFile();
        updateComparePage(file);
    }

    public void showComparePage(IFile file) {
        if (comparePage == null) return;
        updateComparePage(file);
        setActivePageByControl(comparePage);
    }

    private void updateComparePage(IFile file) {
        Job job = new Job("Fetching Git content") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                java.io.File workingDir = file.getProject().getLocation().toFile();
                String relativePath = file.getProjectRelativePath().toString();
                try {
                    eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider vcs = new eu.kalafatic.evolution.controller.vcs.GitVersionControlProvider();
                    String headContent = vcs.getFileContent(workingDir, "HEAD", relativePath);
                    if (headContent != null) {
                        Display.getDefault().asyncExec(() -> {
                            if (!comparePage.isDisposed()) {
                                comparePage.setInput(file, new StringElement(headContent, file.getName(), file.getFileExtension()), "Local File", "Git HEAD");
                            }
                        });
                    }
                } catch (Exception e) {
                    // Probably not a git repo or file not in git
                }
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    public void showApprovalPage() {
        setActivePageByControl(approvalPage);
    }

    public void showAiChatPage() {
        setActivePageByControl(aiChatPage);
    }

    public void runTaskInChat(Task task) {
        if (aiChatPage != null) {
            showAiChatPage();
            aiChatPage.runTask(task);
        }
    }

    public void openTaskResult(Task task) {
        if (aiChatPage != null) {
            showAiChatPage();
            // If the task has an ID, we could try to switch to that thread
            if (task.getId() != null) {
                aiChatPage.switchThread(task.getId());
            }
        }
    }

    private void setActivePageByControl(Control control) {
        for (int i = 0; i < getPageCount(); i++) {
            if (getControl(i) == control) {
                setActivePage(i);
                break;
            }
        }
    }

    public void selectNode(Object element) {
        if (graphPage != null) {
            graphPage.selectNode(element);
        }
    }

    @Override
    protected void pageChange(int newPageIndex) {
        super.pageChange(newPageIndex);
        Control control = getControl(newPageIndex);
        if (control == previewPage && previewPage != null) {
            previewPage.sortWords();
        } else if (control == comparePage && comparePage != null) {
            refreshComparePage();
        }
    }

    public void gotoMarker(IMarker marker) {
        setActivePage(1);
        IDE.gotoMarker(textEditor, marker);
    }

    @Override
    public Composite getContainer() {
        return super.getContainer();
    }

    @Override
    public int addPage(Control control) {
        return super.addPage(control);
    }

    @Override
    public int addPage(IEditorPart editor, IEditorInput input) throws PartInitException {
        return super.addPage(editor, input);
    }

    @Override
    public void setPageText(int index, String text) {
        super.setPageText(index, text);
    }

	public Color getLightGreen() {
		return lightGreen;
	}

	public void setLightGreen(Color lightGreen) {
		this.lightGreen = lightGreen;
	}

	public Color getLightRed() {
		return lightRed;
	}

	public void setLightRed(Color lightRed) {
		this.lightRed = lightRed;
	}

	public Color getLightOrange() {
		return lightOrange;
	}

	public void setLightOrange(Color lightOrange) {
		this.lightOrange = lightOrange;
	}
}
