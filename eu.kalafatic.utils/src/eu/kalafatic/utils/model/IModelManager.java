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

import org.eclipse.emf.ecore.EObject;

/**
 * The Interface interface IModelManager.
 * @author Petr Kalafatic
 * @version 3.0.0
 * @project Gemini
 */
public interface IModelManager {

	/**
	 * Creates the model.
	 */
	void createModel();

	/**
	 * Inits the model.
	 */
	void initModel();

	/**
	 * Inits the model.
	 * @param path the path
	 * @param modelName the model name
	 */
	void initModel(String path, String projectName, String modelName);

	/**
	 * Sets the up model.
	 */
	void setUpModel();

	/**
	 * Sets the up project.
	 * @param modelName the new up project
	 */
	void setUpProject(String projectName);

	/**
	 * Open model.
	 * @throws Exception the exception
	 */
	void openModel() throws Exception;

	/**
	 * Gets the model.
	 * @return the model
	 */
	EObject getModel();

}
