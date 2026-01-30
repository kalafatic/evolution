package eu.kalafatic.evolution.view.views;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutAlgorithm;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.*;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ZoomComboContributionItem;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.jface.action.Separator;
import java.util.ArrayList;
import java.util.List;

public class OrchestrationZestView extends ViewPart implements ISelectionListener {

    public static final String ID = "eu.kalafatic.evolution.view.orchestrationZestView";
    private GraphViewer viewer;
    private Orchestrator currentOrchestrator;
    private ZoomManager zoomManager;

    private Adapter modelAdapter = new AdapterImpl() {
        @Override
        public void notifyChanged(Notification notification) {
            refreshViewer();
        }
    };

    @Override
    public void createPartControl(Composite parent) {
        viewer = new GraphViewer(parent, SWT.NONE);
        viewer.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
        viewer.setContentProvider(new OrchestrationGraphContentProvider());
        viewer.setLabelProvider(new OrchestrationGraphLabelProvider());
        viewer.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING));

        getSite().getPage().addSelectionListener(this);

        setupZoomSupport();
        createToolbar();
    }

    private void setupZoomSupport() {
        IFigure contents = viewer.getGraphControl().getContents();
        Viewport viewport = viewer.getGraphControl().getViewport();
        zoomManager = new ZoomManager(contents, viewport);

        double[] zoomLevels = { 0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0 };
        zoomManager.setZoomLevels(zoomLevels);

        List<String> zoomContributions = new ArrayList<>();
        zoomContributions.add(ZoomManager.FIT_ALL);
        zoomContributions.add(ZoomManager.FIT_HEIGHT);
        zoomContributions.add(ZoomManager.FIT_WIDTH);
        zoomManager.setZoomLevelContributions(zoomContributions);
    }

    private void createToolbar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();

        mgr.add(new Action("Refresh") {
            @Override
            public void run() {
                refreshViewer();
            }
        });

        mgr.add(new Action("Tree Layout") {
            @Override
            public void run() {
                viewer.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
            }
        });

        mgr.add(new Action("Spring Layout") {
            @Override
            public void run() {
                viewer.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
            }
        });

        mgr.add(new Action("Radial Layout") {
            @Override
            public void run() {
                viewer.setLayoutAlgorithm(new RadialLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
            }
        });

        mgr.add(new Action("Horizontal Tree") {
            @Override
            public void run() {
                viewer.setLayoutAlgorithm(new HorizontalTreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true);
            }
        });

        mgr.add(new Separator());

        if (zoomManager != null) {
            mgr.add(new ZoomComboContributionItem(getViewSite().getPage()));
            mgr.add(new ZoomInAction(zoomManager));
            mgr.add(new ZoomOutAction(zoomManager));

            mgr.add(new Action("Fit to Page") {
                @Override
                public void run() {
                    zoomManager.setZoomAsText(ZoomManager.FIT_ALL);
                }
            });
        }
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if (selection instanceof IStructuredSelection) {
            Object first = ((IStructuredSelection) selection).getFirstElement();
            if (first instanceof Orchestrator) {
                updateInput((Orchestrator) first);
            }
        }
    }

    private void updateInput(Orchestrator orchestrator) {
        if (currentOrchestrator != null) {
            currentOrchestrator.eAdapters().remove(modelAdapter);
        }
        currentOrchestrator = orchestrator;
        if (currentOrchestrator != null) {
            currentOrchestrator.eAdapters().add(modelAdapter);
            viewer.setInput(new Object[] { currentOrchestrator });
        }
    }

    public void refreshViewer() {
        Display.getDefault().asyncExec(() -> {
            if (viewer != null && !viewer.getControl().isDisposed()) {
                viewer.refresh();
                viewer.applyLayout();
            }
        });
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == ZoomManager.class) {
            return adapter.cast(zoomManager);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void dispose() {
        getSite().getPage().removeSelectionListener(this);
        if (currentOrchestrator != null) {
            currentOrchestrator.eAdapters().remove(modelAdapter);
        }
        super.dispose();
    }
}
