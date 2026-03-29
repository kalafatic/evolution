package eu.kalafatic.evolution.view.editors.pages;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ScalableFigure;
import org.eclipse.draw2d.Viewport;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.ui.actions.ZoomComboContributionItem;
import org.eclipse.gef.ui.actions.ZoomInAction;
import org.eclipse.gef.ui.actions.ZoomOutAction;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.GridLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.HorizontalTreeLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.RadialLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;
import org.eclipse.zest.layouts.algorithms.TreeLayoutAlgorithm;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.view.editors.MultiPageEditor;
import eu.kalafatic.evolution.view.provider.OrchestrationGraphContentProvider;
import eu.kalafatic.evolution.view.provider.OrchestrationGraphLabelProvider;

public class GraphPage extends Composite {
    private IWorkbenchSite site;
    private Orchestrator orchestrator;
    private GraphViewer viewer;
    private ZoomManager zoomManager;
    private Adapter modelAdapter = new EContentAdapter() {
        @Override
        public void notifyChanged(Notification notification) {
            super.notifyChanged(notification);
            Display.getDefault().asyncExec(() -> {
                if (!isDisposed()) {
                    refreshViewer();
                }
            });
        }
    };

    public GraphPage(Composite parent, MultiPageEditor editor, IWorkbenchSite site, Orchestrator orchestrator) {
        super(parent, SWT.NONE);
        this.site = site;
        setOrchestrator(orchestrator);
        createControl();
    }

    private void createControl() {
        this.setLayout(new GridLayout(1, false));

        // Toolbar on top
        Composite tbComp = new Composite(this, SWT.NONE);
        tbComp.setLayout(new FillLayout());
        tbComp.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        viewer = new GraphViewer(this, SWT.NONE);
        viewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        viewer.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
        viewer.setContentProvider(new OrchestrationGraphContentProvider());
        viewer.setLabelProvider(new OrchestrationGraphLabelProvider());
        viewer.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING));

        setupZoomSupport();
        createGraphToolbar(tbComp);

        viewer.setInput(orchestrator != null ? new Object[] { orchestrator } : new Object[0]);
    }

    private void setupZoomSupport() {
        if (viewer.getGraphControl() == null) return;
        IFigure contents = viewer.getGraphControl().getContents();
        Viewport viewport = viewer.getGraphControl().getViewport();
        zoomManager = new ZoomManager((ScalableFigure) contents, viewport);
        zoomManager.setZoomLevels(new double[] { 0.1, 0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0 });
        List<String> zoomContribs = new ArrayList<>();
        zoomContribs.add(ZoomManager.FIT_ALL);
        zoomContribs.add(ZoomManager.FIT_HEIGHT);
        zoomContribs.add(ZoomManager.FIT_WIDTH);
        zoomManager.setZoomLevelContributions(zoomContribs);
    }

    private void createGraphToolbar(Composite parent) {
        ToolBarManager mgr = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
        mgr.createControl(parent);
        mgr.add(new Action("Refresh") { @Override public void run() { refreshViewer(); } });
        mgr.add(new Separator());
        mgr.add(new Action("Tree") { @Override public void run() { viewer.setLayoutAlgorithm(new TreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true); } });
        mgr.add(new Action("Horizontal") { @Override public void run() { viewer.setLayoutAlgorithm(new HorizontalTreeLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true); } });
        mgr.add(new Action("Spring") { @Override public void run() { viewer.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true); } });
        mgr.add(new Action("Radial") { @Override public void run() { viewer.setLayoutAlgorithm(new RadialLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true); } });
        mgr.add(new Action("Grid") { @Override public void run() { viewer.setLayoutAlgorithm(new GridLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING), true); } });
        mgr.add(new Separator());
        if (zoomManager != null) {
            mgr.add(new ZoomComboContributionItem(site.getPage()));
            mgr.add(new ZoomInAction(zoomManager));
            mgr.add(new ZoomOutAction(zoomManager));
            mgr.add(new Action("Fit") { @Override public void run() { zoomManager.setZoomAsText(ZoomManager.FIT_ALL); } });
        }
        mgr.update(true);
    }

    public void refreshViewer() {
        Display.getDefault().asyncExec(() -> {
            if (viewer != null && !viewer.getControl().isDisposed()) {
                viewer.refresh();
                viewer.applyLayout();
            }
        });
    }

    public void setOrchestrator(Orchestrator orchestrator) {
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().remove(modelAdapter);
        }
        this.orchestrator = orchestrator;
        if (this.orchestrator != null) {
            this.orchestrator.eAdapters().add(modelAdapter);
        }
        if (viewer != null && !viewer.getControl().isDisposed()) {
            viewer.setInput(orchestrator != null ? new Object[] { orchestrator } : new Object[0]);
        }
    }

    @Override
    public void dispose() {
        if (orchestrator != null) {
            orchestrator.eAdapters().remove(modelAdapter);
        }
        super.dispose();
    }
}
