package eu.kalafatic.evolution.view.editors;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
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
import eu.kalafatic.evolution.view.editors.pages.PeerReviewPage;
import eu.kalafatic.evolution.view.editors.pages.PreviewPage;
import eu.kalafatic.evolution.view.editors.pages.PropertiesPage;
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

    private Orchestrator orchestrator;
    private TaskContext currentContext;
    private boolean isDirty = false;
    private ResourceSet resourceSet;
    private Resource resource;
    private EditorResourceChangeListener resourceListener;
    private EditorSelectionListener selectionListener;

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

            Display.getDefault().asyncExec(() -> {
                if (!getContainer().isDisposed()) {
                    refreshPages();
                }
            });
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
                resource.save(Collections.EMPTY_MAP);
                setDirty(false);
            } catch (IOException e) {
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
        if (aiChatPage != null) aiChatPage.updateUI();
        if (propertiesPage != null) propertiesPage.updatePropertiesInfo();
        if (toolsPage != null) toolsPage.updateUIFromModel();
        if (taskStackPage != null) taskStackPage.updateUIFromModel();
        if (testsPage != null) testsPage.updateUIFromModel();
        if (iterationPage != null) iterationPage.updateUIFromModel();
        if (peerReviewPage != null) peerReviewPage.refreshUI();
        if (mcpSettingsPage != null) mcpSettingsPage.updateMcpInfo();
        if (contextPage != null) contextPage.refreshUI();
    }

    public void showApprovalPage() {
        setActivePageByControl(approvalPage);
    }

    public void showAiChatPage() {
        setActivePageByControl(aiChatPage);
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
}
