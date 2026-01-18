package eu.kalafatic.evolution.controller;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.kalafatic.evolution.view.PropertiesView;

public class SaveCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IViewPart view = page.findView(PropertiesView.ID);
                if (view instanceof PropertiesView) {
                    EObject rootObject = ((PropertiesView) view).getRootObject();
                    if (rootObject != null) {
                        saveProperties(rootObject, HandlerUtil.getActiveShell(event));
                    }
                }
            }
        }
        return null;
    }

    private void saveProperties(EObject properties, Shell shell) {
        EObject git = (EObject) properties.eGet(properties.eClass().getEStructuralFeature("git"));
        String localPath = (String) git.eGet(git.eClass().getEStructuralFeature("localPath"));
        String filePath = localPath + "/orchestrator.xml";

        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xml", new XMIResourceFactoryImpl());
        ResourceSet resourceSet = new ResourceSetImpl();
        URI fileURI = URI.createFileURI(filePath);
        Resource resource = resourceSet.createResource(fileURI);
        resource.getContents().add(properties);

        try {
            resource.save(Collections.EMPTY_MAP);
            System.out.println("Properties saved to " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
