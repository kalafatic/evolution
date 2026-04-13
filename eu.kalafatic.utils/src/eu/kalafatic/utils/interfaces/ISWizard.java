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
package eu.kalafatic.utils.interfaces;

import org.eclipse.swt.widgets.Table;

/**
 * The Interface interface ISWizard.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public interface ISWizard extends ISWizardSettings {

	/** The FILE. */
	public final String FILE = "File";

	/** The DB. */
	public final String DB = "Database";

	/** The TORRENT. */
	public final String TORRENT = "Torrent";

	/**
	 * Creates the pages.
	 */
	void createPages();

	/**
	 * Adds the table.
	 * 
	 * @param source
	 *            the source
	 * @param table
	 *            the table
	 */
	void addTable(boolean source, Table table);

	/**
	 * Creates the jobs.
	 */
	void createJobs();
}
