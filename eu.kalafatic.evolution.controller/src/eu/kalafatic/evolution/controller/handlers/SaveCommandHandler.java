package eu.kalafatic.evolution.controller.handlers;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class SaveCommandHandler extends AbstractOrchestratorHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Orchestrator orchestrator = getOrchestrator(event);
        if (orchestrator != null) {
            saveProperties(orchestrator, HandlerUtil.getActiveShell(event));
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
