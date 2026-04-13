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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolTip;
import org.eclipse.swt.widgets.TrayItem;

import eu.kalafatic.utils.lib.AppData;
import eu.kalafatic.utils.log.Log;
import eu.kalafatic.utils.preferences.ECorePreferences;

/**
 * The Class class AServerThread.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public abstract class AServerThread extends Thread implements IServerThread {

	/** The port. */
	protected int port;

	/** The isa. */
	protected InetSocketAddress isa;

	/** The server socket channel. */
	protected ServerSocketChannel serverSocketChannel;

	/** The server socket. */
	protected ServerSocket serverSocket;

	/** The message. */
	protected String message;

	/** The tool tip. */
	private ToolTip toolTip;

	/** The enabled. */
	protected boolean enabled = true;

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IServerThread#bind(eu.kalafatic.gemini .core.interfaces.IPreference)
	 */
	@Override
	public void bind(IPreference iPreference) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.configureBlocking(true);
		serverSocketChannel.socket().bind(isa);

		processMessages(iPreference, message);
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IServerThread#processMessages(eu. kalafatic.gemini.core.interfaces.IPreference, java.lang.String)
	 */
	@Override
	public void processMessages(final IPreference iPreference, final String message) {
		Log.log(ECorePreferences.MODULE, message);

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				try {
					TrayItem trayItem = AppData.getInstance().getTrayItem();

					if (trayItem.isDisposed()) {
						return;
					}
					if (trayItem.getToolTip().isVisible()) {
						sleep(2000);
					}
					createToolTip(iPreference, message, trayItem);

				} catch (Exception e) {
					Log.log(iPreference, e);
				}
			}
		});
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IServerThread#processEndOfThread( eu.kalafatic.gemini.core.interfaces.IPreference,
	 * java.nio.channels.ServerSocketChannel)
	 */
	@Override
	public void processEndOfThread(IPreference iPreference, ServerSocketChannel ssChannel) {
		try {
			if (ssChannel != null) {
				ssChannel.close();
				ssChannel = null;
			}
		} catch (Exception e) {
			Log.log(iPreference, e);
		}
	}

	// ---------------------------------------------------------------

	/*
	 * (non-Javadoc)
	 * @see eu.kalafatic.gemini.core.interfaces.IServerThread#processEndOfThread( eu.kalafatic.gemini.core.interfaces.IPreference,
	 * java.net.ServerSocket)
	 */
	@Override
	public void processEndOfThread(IPreference iPreference, ServerSocket sSocket) {
		try {
			if (sSocket != null) {
				sSocket.close();
				sSocket = null;
			}
		} catch (Exception e) {
			Log.log(iPreference, e);
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the tool tip.
	 * @param iPreference the i preference
	 * @param message the message
	 * @param trayItem the tray item
	 */
	private void createToolTip(IPreference iPreference, String message, TrayItem trayItem) {
		if (toolTip == null || toolTip.isDisposed()) {
			toolTip = new ToolTip(new Shell(Display.getDefault()), SWT.BALLOON | SWT.ICON_INFORMATION);
			toolTip.setAutoHide(true);
			trayItem.setToolTip(toolTip);
		}
		toolTip.setText(iPreference.getName() + " Info");
		toolTip.setMessage(message);
		toolTip.setVisible(true);
	}

	// ---------------------------------------------------------------

	/**
	 * Sets the enabled.
	 * @param enabled the new enabled
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
