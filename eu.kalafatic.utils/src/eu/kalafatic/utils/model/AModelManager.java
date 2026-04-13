/*******************************************************************************
 * Copyright (c) 2010, Petr Kalafatic (gemini@kalafatic.eu).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU GPL Version 3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.txt
 *
 * Contributors:
 *     Petr Kalafatic - initial API and implementation
 ******************************************************************************/
package eu.kalafatic.utils.model;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.resources.ProjectExplorer;

import eu.kalafatic.utils.application.AppUtils;

/**
 * The Class class AModelManager.
 * @author Petr Kalafatic
 * @version 3.0.0
 * @project Gemini
 */
public abstract class AModelManager implements IModelManager {

	/** The model file. */
	protected File modelFile;

	/** The model uri. */
	protected URI modelURI;

	/** The resource. */
	protected Resource resource;

	/** The model. */
	protected Object model;

	private IProject iProject;

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.explorer.controller.model.IModelManager#initModel(java.lang.String, java.lang.String)
	 */
	@Override
	public void initModel(String path, String projectName, String modelName) {
		try {
			modelFile = new File(path.concat(File.separator).concat(modelName));
			modelURI = URI.createURI("file:/" + modelFile.getAbsolutePath());
			if (modelFile.exists()) {
				openModel();
			} else {
				createModel();
			}
			setUpModel();

			ModelUtils.doSave(getModel());
			setUpProject(projectName);
			IFile file = getProject().getFolder("Model").getFile(modelName);
			if (!file.exists()) {
				file.createLink(modelFile.toURI(), IResource.FORCE, new NullProgressMonitor());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.explorer.controller.model.IModelManager#openModel()
	 */
	@Override
	public void openModel() throws Exception {
		ResourceSetImpl resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry().getFactory(modelURI);
		resource = resourceSet.getResource(modelURI, true);
		resource.load(ModelUtils.SAVE_OPTIONS);
		model = resource.getContents().get(0);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.explorer.controller.model.IModelManager#setUpProject(java.lang.String)
	 */
	@Override
	public void setUpProject(String projectName) {
		try {
			iProject = AppUtils.INSTANCE.createProject(projectName);

			assert (iProject != null);

			if (!iProject.getFolder("Model").exists()) {
				iProject.getFolder("Model").create(IResource.FORCE, true, new NullProgressMonitor());
			}
			final ProjectExplorer projectExplorer = (ProjectExplorer) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView("org.eclipse.ui.navigator.ProjectExplorer");

			assert (projectExplorer != null);

			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					projectExplorer.getCommonViewer().expandAll();
					projectExplorer.selectReveal(new StructuredSelection(iProject));
					projectExplorer.getCommonViewer().refresh();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	public IProject getProject() {
		return iProject;
	}
}
