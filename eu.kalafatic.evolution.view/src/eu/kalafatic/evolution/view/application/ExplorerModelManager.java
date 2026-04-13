///*******************************************************************************
// * Copyright (c) 2010, Petr Kalafatic (gemini@kalafatic.eu).
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the GNU GPL Version 3
// * which accompanies this distribution, and is available at
// * http://www.gnu.org/licenses/gpl.txt
// *
// * Contributors:
// *     Petr Kalafatic - initial API and implementation
// ******************************************************************************/
//package eu.kalafatic.evolution.view.application;
//
//import java.io.IOException;
//import java.net.InetAddress;
//import java.util.Collections;
//
//import org.eclipse.emf.common.util.URI;
//import org.eclipse.emf.ecore.EClass;
//import org.eclipse.emf.ecore.EFactory;
//import org.eclipse.emf.ecore.EObject;
//import org.eclipse.emf.ecore.EPackage;
//import org.eclipse.emf.ecore.resource.Resource;
//import org.eclipse.emf.ecore.resource.ResourceSet;
//import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
//import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
//
//import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
//import eu.kalafatic.evolution.model.orchestration.Orchestrator;
//import eu.kalafatic.utils.model.AModelManager;
//import eu.kalafatic.utils.model.ModelUtils;
//
///**
// * The Class class ExplorerModelManager.
// * @author Petr Kalafatic
// * @project Gemini
// * @version 3.0.0
// */
//public class ExplorerModelManager extends AModelManager {
//
//	/** The MODE l_ name. */
//	private final String MODEL_NAME = "Model.explorer";
//
//	/** The INSTANCE. */
//	private volatile static ExplorerModelManager INSTANCE;
//
//	/**
//	 * Instantiates a new explorer model manager.
//	 */
//	public ExplorerModelManager() {
//		initModel();
//	}
//
//	/**
//	 * Gets the single instance of ExplorerModelManager.
//	 * @return single instance of ExplorerModelManager
//	 */
//	public static ExplorerModelManager getInstance() {
//		if (INSTANCE == null) {
//			synchronized (ExplorerModelManager.class) {
//				INSTANCE = new ExplorerModelManager();
//			}
//		}
//		return INSTANCE;
//	}
//
//	// ---------------------------------------------------------------
//	// ---------------------------------------------------------------
//
//	/**
//	 * Inits the model.
//	 */
//	@Override
//	public void initModel() {
//		try {
//			// String models =
//			// PREFERENCES.get(ECorePreferences.MODELS_LOC.getName(),
//			// (String) ECorePreferences.MODELS_LOC.getDef());
//
//			String models = "./models";
//
//			super.initModel(models, "Explorer", MODEL_NAME);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
//
//	// ---------------------------------------------------------------
//
//	/**
//	 * Creates the model.
//	 */
//	@Override
//	public void createModel() {
//		try {
//			ResourceSetImpl resourceSet = new ResourceSetImpl();
//			// Register the appropriate resource factory to handle all file extensions.
//			resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
//			
//			URI fileURI = URI.createPlatformPluginURI("eu.kalafatic.evolution.model/model/evolution.ecore", true);
//		    resource = resourceSet.getResource(fileURI, true);
//		    
//	        resource.load(Collections.EMPTY_MAP);
//	        EPackage ePackage = (EPackage) resource.getContents().get(0);
//	        
//	        // Create an instance of the model to be edited
//	        EFactory factory = ePackage.getEFactoryInstance();
//	        EClass orchestratorClass = (EClass) ePackage.getEClassifier("Orchestrator");
//
//			model = factory.create(orchestratorClass);
//			resource.getContents().add(getModel());
//			resource.save(ModelUtils.SAVE_OPTIONS);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
////	
////	// Load model and create an instance to edit
////    ResourceSet resourceSet = editingDomain.getResourceSet();
////    URI fileURI = URI.createPlatformPluginURI("eu.kalafatic.evolution.model/model/evolution.ecore", true);
////    Resource resource = resourceSet.getResource(fileURI, true);
////
////    try {
////        resource.load(Collections.EMPTY_MAP);
////        EPackage ePackage = (EPackage) resource.getContents().get(0);
////
////        // Create an instance of the model to be edited
////        EFactory factory = ePackage.getEFactoryInstance();
////        EClass orchestratorClass = (EClass) ePackage.getEClassifier("Orchestrator");
////        rootObject = factory.create(orchestratorClass);
////
////        // Create and set Git
////        EClass gitClass = (EClass) ePackage.getEClassifier("Git");
////        EObject gitObject = factory.create(gitClass);
////        gitObject.eSet(gitClass.getEStructuralFeature("repositoryUrl"), "https://github.com/example/repo.git");
////        rootObject.eSet(orchestratorClass.getEStructuralFeature("git"), gitObject);
////
////        // Create and set Maven
////		EClass mavenClass = (EClass) ePackage.getEClassifier("Maven");
////		EObject mavenObject = factory.create(mavenClass);
////		rootObject.eSet(orchestratorClass.getEStructuralFeature("maven"), mavenObject);
////
////		// Create and set LLM
////		EClass llmClass = (EClass) ePackage.getEClassifier("LLM");
////		EObject llmObject = factory.create(llmClass);
////		rootObject.eSet(orchestratorClass.getEStructuralFeature("llm"), llmObject);
////
////		// Create and set Compiler
////		EClass compilerClass = (EClass) ePackage.getEClassifier("Compiler");
////		EObject compilerObject = factory.create(compilerClass);
////		rootObject.eSet(orchestratorClass.getEStructuralFeature("compiler"), compilerObject);
////
////		// Create and set Ollama
////		if (rootObject instanceof Orchestrator) {
////			((Orchestrator) rootObject).setOllama(OrchestrationFactory.eINSTANCE.createOllama());
////			((Orchestrator) rootObject).setAiChat(OrchestrationFactory.eINSTANCE.createAiChat());
////		}
////
////        viewer.setInput(rootObject);
////        viewer.expandAll();
////
////    } catch (IOException e) {
////        e.printStackTrace();
////    }
//
//	// ---------------------------------------------------------------
//
//	/**
//	 * Sets the up explorer.
//	 */
//	@Override
//	public void setUpModel() {
////		try {
////			Device gateway = Utils.getGateway();
////
////			Device localHost = ExplorerFactory.eINSTANCE.createDevice();
////			localHost.setIp(InetAddress.getLocalHost().getHostAddress());
////			localHost.setHost(InetAddress.getLocalHost().getCanonicalHostName());
////
////			Port defaultPort=ExplorerFactory.eINSTANCE.createPort();
////			defaultPort.setNumber(80);
////
////			localHost.getOpenPorts().put(80, defaultPort);
////
////			localHost.getChildren().put(gateway.getHost(), gateway);
////			getModel().getChildren().put(localHost.getHost(), localHost);
////		} catch (Exception e) {
////			e.printStackTrace();
////		}
//	}
//
//	// ---------------------------------------------------------------
//
//	/**
//	 * Gets the explorer.
//	 * @return the explorer
//	 */
//	@Override
//	public Orchestrator getModel() {
//		return (Orchestrator) model;
//	}
//
//
//
//}
