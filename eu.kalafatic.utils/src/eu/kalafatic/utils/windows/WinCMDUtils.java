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
package eu.kalafatic.utils.windows;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The Class class WinCMDUtils.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class WinCMDUtils {

	/** The INSTANCE. */
	private volatile static WinCMDUtils INSTANCE;

	/**
	 * Gets the single instance of WinCMDUtils.
	 * 
	 * @return single instance of WinCMDUtils
	 */
	public static WinCMDUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (WinCMDUtils.class) {
				INSTANCE = new WinCMDUtils();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Process command.
	 * 
	 * @param var
	 *            the var
	 * @return the process
	 */
	public Process processCommand(String... var) {
		ProcessBuilder processBuilder = new ProcessBuilder(var);
		Process p = null;
		try {
			p = processBuilder.start();
			p.waitFor();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return p;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the process output.
	 * 
	 * @param p
	 *            the p
	 * @return the process output
	 */
	public String getProcessOutput(Process p) {
		BufferedReader stdInput = null;	
		StringBuffer out=new StringBuffer();
		try {			
			stdInput = new BufferedReader(new InputStreamReader(
					p.getInputStream()));			

			// read the output from the command
			String s = null;		
			
			while ((s = stdInput.readLine()) != null) {
				out.append(s);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				stdInput.close();				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return out.toString();
	}

	// ---------------------------------------------------------------

	/**
	 * Prints the process output.
	 * 
	 * @param p
	 *            the p
	 */
	public void printProcessOutput(Process p) {
		BufferedReader stdInput = null;		
		BufferedReader stdError = null;
		try {			
			stdInput = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			
			stdError = new BufferedReader(new InputStreamReader(
					p.getErrorStream()));

			// read the output from the command
			String s = null;
			
			System.out.println("Here is the standard input of the command:\n");
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
			}

			// read any errors from the attempted command
			System.out
					.println("Here is the standard error of the command (if any):\n");

			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				stdInput.close();
				stdError.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
