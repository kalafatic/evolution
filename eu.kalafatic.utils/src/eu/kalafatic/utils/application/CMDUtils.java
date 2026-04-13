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
package eu.kalafatic.utils.application;

import org.eclipse.swt.widgets.Display;

import eu.kalafatic.utils.constants.FConstants;
import eu.kalafatic.utils.factories.CommandFactory;
import eu.kalafatic.utils.lib.EHandler;
import eu.kalafatic.utils.preferences.ECorePreferences;
import eu.kalafatic.utils.shedulers.CPUScheduler;

/**
 * The Class class CMDUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class CMDUtils {

	/** The INSTANCE. */
	private volatile static CMDUtils INSTANCE;

	/**
	 * Gets the single instance of CMDUtils.
	 * @return single instance of CMDUtils
	 */
	public static CMDUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (CMDUtils.class) {
				INSTANCE = new CMDUtils();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Run after start.
	 */
	public void runAfterStart() {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				runInternal();
				runRC();
				runBT();
				runCPU();
			}
		});
	}

	// ---------------------------------------------------------------

	/**
	 * Run internal.
	 */
	private void runInternal() {
		CommandFactory.INSTANCE.executeCommand(EHandler.RUN_INT_COMM.ID);
	}

	// ---------------------------------------------------------------

	/**
	 * Run rc.
	 */
	private void runRC() {
		boolean run = FConstants.PREFERENCES.getBoolean(ECorePreferences.RC_ENABLED.getName(), ((Boolean) ECorePreferences.RC_ENABLED.getDef()));

		if (run) {
			CommandFactory.INSTANCE.executeCommand(EHandler.RUN_RC.ID);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Run bt.
	 */
	private void runBT() {
		boolean run = FConstants.PREFERENCES.getBoolean(ECorePreferences.RUN_BT_AFTER_START.getName(), ((Boolean) ECorePreferences.RUN_BT_AFTER_START.getDef()));

		if (run) {
			CommandFactory.INSTANCE.executeCommand(EHandler.RUN_TORRENTS.ID);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Run cpu.
	 */
	private void runCPU() {
		boolean run = FConstants.PREFERENCES.getBoolean(ECorePreferences.CPU_MANAGEMENT.getName(), (Boolean) ECorePreferences.CPU_MANAGEMENT.getDef());

		if (run) {
			CPUScheduler.INSTANCE.start();
		}
	}
}
