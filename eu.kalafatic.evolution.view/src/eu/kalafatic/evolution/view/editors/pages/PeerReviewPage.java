package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.peerreview.*;

public class PeerReviewPage extends SharedScrolledComposite {
    private MultiPageEditor editor;
    private Orchestrator orchestrator;
    private FormToolkit toolkit;

    private FileTreeGroup fileTreeGroup;
    private DiffViewerGroup diffViewerGroup;
    private CommentsGroup commentsGroup;

    private Adapter modelAdapter = new EContentAdapter() {
        @Override
        public void notifyChanged(Notification notification) {
            super.notifyChanged(notification);
            refreshUI();
        }
    };

    public PeerReviewPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        this.editor = editor;
        this.orchestrator = orchestrator;
        this.setExpandHorizontal(true);
        this.setExpandVertical(true);
        this.toolkit = new FormToolkit(parent.getDisplay());
        createControl();
        setOrchestrator(orchestrator);
    }

    private void createControl() {
        Composite comp = toolkit.createComposite(this);
        comp.setLayout(new GridLayout(3, false)); // 3 panels: Left (Tree), Center (Diff), Right (Comments)

        fileTreeGroup = new FileTreeGroup(toolkit, comp, editor, orchestrator);
        diffViewerGroup = new DiffViewerGroup(toolkit, comp, editor, orchestrator);
        commentsGroup = new CommentsGroup(toolkit, comp, editor, orchestrator);

        fileTreeGroup.getTreeViewer().addSelectionChangedListener(event -> {
            org.eclipse.jface.viewers.IStructuredSelection selection = (org.eclipse.jface.viewers.IStructuredSelection) event.getSelection();
            Object firstElement = selection.getFirstElement();
            if (firstElement instanceof java.io.File && ((java.io.File) firstElement).isFile()) {
                diffViewerGroup.setFile((java.io.File) firstElement);
            }
        });

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        initializeData();
    }

    private void initializeData() {
        try {
            org.eclipse.core.resources.IProject project = null;
            if (editor.getEditorInput() instanceof org.eclipse.ui.IFileEditorInput) {
                project = ((org.eclipse.ui.IFileEditorInput) editor.getEditorInput()).getFile().getProject();
            }
            if (project != null) {
                java.io.File projectRoot = project.getLocation().toFile();
                fileTreeGroup.getTreeViewer().setInput(new java.io.File[] { projectRoot });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        if (this.orchestrator != null) this.orchestrator.eAdapters().remove(modelAdapter);
        this.orchestrator = orchestrator;
        if (this.orchestrator != null) this.orchestrator.eAdapters().add(modelAdapter);
        refreshUI();
    }

    public void refreshUI() {
        if (isDisposed()) return;
        Display.getDefault().asyncExec(() -> {
            if (isDisposed()) return;
            fileTreeGroup.updateUI();
            diffViewerGroup.updateUI();
            commentsGroup.updateUI();
            this.reflow(true);
        });
    }

    @Override
    public void dispose() {
        if (orchestrator != null) orchestrator.eAdapters().remove(modelAdapter);
        if (toolkit != null) toolkit.dispose();
        super.dispose();
    }
}
