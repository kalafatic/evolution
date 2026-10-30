package eu.kalafatic.evolution.view.editors.pages;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.editors.pages.peerreview.*;

public class PeerReviewPage extends AEvoPage {

    private FileTreeGroup fileTreeGroup;
    private DiffViewerGroup diffViewerGroup;
    private CommentsGroup commentsGroup;
    private SashForm sashForm;

    private Adapter modelAdapter = new EContentAdapter() {
        @Override
        public void notifyChanged(Notification notification) {
            super.notifyChanged(notification);
            if (notification.isTouch()) return;
            scheduleRefresh();
        }
    };

    public PeerReviewPage(Composite parent, MultiPageEditor editor, Orchestrator orchestrator) {
        super(parent, editor, orchestrator);
        createControl();
    }

    private void createControl() {
        Composite comp = toolkit.createComposite(this);
        comp.setLayout(new FillLayout());

        sashForm = new SashForm(comp, SWT.HORIZONTAL | SWT.SMOOTH);

        fileTreeGroup = new FileTreeGroup(toolkit, sashForm, editor, orchestrator);
        diffViewerGroup = new DiffViewerGroup(toolkit, sashForm, editor, orchestrator);
        commentsGroup = new CommentsGroup(toolkit, sashForm, editor, orchestrator);

        sashForm.setWeights(new int[]{25, 50, 25});

        fileTreeGroup.getTreeViewer().addSelectionChangedListener(event -> {
            org.eclipse.jface.viewers.IStructuredSelection selection = (org.eclipse.jface.viewers.IStructuredSelection) event.getSelection();
            Object firstElement = selection.getFirstElement();
            if (firstElement instanceof String) {
                diffViewerGroup.setFilePath((String) firstElement);
            }
        });

        this.setContent(comp);
        this.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT));

        initializeData();
    }

    private void initializeData() {
        try {
            java.io.File projectRoot = getProjectRoot();
            if (projectRoot != null) {
                java.util.List<String> changedFiles = eu.kalafatic.evolution.controller.review.service.PeerReviewService.getInstance().getChangedFiles(projectRoot);
                fileTreeGroup.getTreeViewer().setInput(changedFiles);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setOrchestrator(Orchestrator orchestrator) {
        if (this.orchestrator != null) this.orchestrator.eAdapters().remove(modelAdapter);
        super.setOrchestrator(orchestrator);
        if (this.orchestrator != null) this.orchestrator.eAdapters().add(modelAdapter);
    }

    public void notifyLineSelected(String filePath, int lineNum) {
        commentsGroup.setSelectedLine(filePath, lineNum);
    }

    @Override
    protected void refreshUI() {
        if (isDisposed()) return;
        fileTreeGroup.updateUI();
        diffViewerGroup.updateUI();
        commentsGroup.updateUI();
        this.reflow(true);
    }

    public void updateUI() {
        scheduleRefresh();
    }

    @Override
    public void dispose() {
        if (orchestrator != null) orchestrator.eAdapters().remove(modelAdapter);
        super.dispose();
    }
}
