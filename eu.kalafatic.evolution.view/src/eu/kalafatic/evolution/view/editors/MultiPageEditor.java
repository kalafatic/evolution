package eu.kalafatic.evolution.view.editors;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.MultiPageEditorPart;
import eu.kalafatic.evolution.model.orchestration.EvoProject;
import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.OrchestrationPackage;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.listeners.EditorResourceChangeListener;
import eu.kalafatic.evolution.view.editors.listeners.EditorSelectionListener;
import eu.kalafatic.evolution.view.editors.pages.*;
import eu.kalafatic.evolution.view.editors.pages.factories.*;

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
    private Orchestrator orchestrator;
    private TaskContext currentContext;
    private boolean isDirty = false;
    private ResourceSet resourceSet;
    private Resource resource;
    private EditorResourceChangeListener resourceListener;
    private EditorSelectionListener selectionListener;

    public MultiPageEditor() {
        super();
        resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("evo", new XMIResourceFactoryImpl());
    }
    
    @Override
    public void createPartControl(Composite parent) {
        ScrolledComposite scrolled = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);

        Composite container = new Composite(scrolled, SWT.NONE);
        container.setLayout(new FillLayout());
        scrolled.setContent(container);

        super.createPartControl(container);

        container.addControlListener(new ControlAdapter() {
            @Override
            public void controlResized(ControlEvent e) {
                scrolled.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            }
        });
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
    	// This forces the EMF registry to load your specific implementation
    	OrchestrationPackage.eINSTANCE.eClass(); 
    	// Then try the cast/access again
    	OrchestrationFactory factory = OrchestrationFactory.eINSTANCE;
    	
        IEditorInput input = getEditorInput();
        if (input instanceof IFileEditorInput) {
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

    @Override
    public void dispose() {
        if (resourceListener != null) ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
        if (selectionListener != null) getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(selectionListener);
        super.dispose();
    }

    @Override
    public boolean isDirty() { return isDirty || super.isDirty(); }

    public void setDirty(boolean dirty) {
        if (this.isDirty != dirty) {
            this.isDirty = dirty;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
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

    public void showApprovalPage() {
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            Control control = getControl(i);
            if (control == approvalPage) {
                setActivePage(i);
                break;
            }
        }
    }

    public void showAiChatPage() {
        int pageCount = getPageCount();
        for (int i = 0; i < pageCount; i++) {
            Control control = getControl(i);
            if (control == aiChatPage) {
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
        if (control == aiChatPage && aiChatPage != null) {
            aiChatPage.setOrchestrator(orchestrator);
        } else if (control == propertiesPage && propertiesPage != null) {
            propertiesPage.updatePropertiesInfo();
        } else if (control == toolsPage && toolsPage != null) {
            toolsPage.updateUIFromModel();
        } else if (control == testsPage && testsPage != null) {
            testsPage.setOrchestrator(orchestrator);
        } else if (control == previewPage && previewPage != null) {
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
