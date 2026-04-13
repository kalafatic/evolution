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

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;

/**
 * The Interface interface IServerThread.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public interface IServerThread {

	/**
	 * Inits the.
	 */
	void init();

	/**
	 * Bind.
	 * 
	 * @param iPreference
	 *            the i preference
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	void bind(IPreference iPreference) throws IOException;

	/**
	 * Process messages.
	 * 
	 * @param iPreference
	 *            the i preference
	 * @param message
	 *            the message
	 */
	void processMessages(IPreference iPreference, final String message);

	/**
	 * Process end of thread.
	 * 
	 * @param iPreference
	 *            the i preference
	 * @param ssChannel
	 *            the ss channel
	 */
	void processEndOfThread(IPreference iPreference,
			ServerSocketChannel ssChannel);

	/**
	 * Process end of thread.
	 * 
	 * @param iPreference
	 *            the i preference
	 * @param sSocket
	 *            the s socket
	 */
	void processEndOfThread(IPreference iPreference, ServerSocket sSocket);

}
