package eu.kalafatic.evolution.view;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.viewers.GraphViewer;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.LayoutStyles;
import org.eclipse.zest.layouts.algorithms.SpringLayoutAlgorithm;

public class EcoreZestView extends ViewPart {

    public static final String ID = "eu.kalafatic.evolution.view.ecoreZestView";
    private GraphViewer viewer;

    @Override
    public void createPartControl(Composite parent) {
        viewer = new GraphViewer(parent, SWT.NONE);
        viewer.setConnectionStyle(ZestStyles.CONNECTIONS_DIRECTED);
        viewer.setContentProvider(new EcoreContentProvider());
        viewer.setLabelProvider(new EcoreLabelProvider());
        viewer.setLayoutAlgorithm(new SpringLayoutAlgorithm(LayoutStyles.NO_LAYOUT_NODE_RESIZING));

        ResourceSet resourceSet = new ResourceSetImpl();
        URI fileURI = URI.createPlatformPluginURI("eu.kalafatic.evolution.model/model/evolution.ecore", true);
        Resource resource = resourceSet.getResource(fileURI, true);

        try {
            resource.load(Collections.EMPTY_MAP);
            EObject eObject = resource.getContents().get(0);
            viewer.setInput(eObject.eContents());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }
}
