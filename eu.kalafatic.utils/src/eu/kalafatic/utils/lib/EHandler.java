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
package eu.kalafatic.utils.lib;

/**
 * The Enum enum EHandler.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum EHandler {

	/** The RU n_ rc. */
	RUN_RC(0,
			"eu.kalafatic.gemini.bt.client.net.rc.controller.handlers.RunRCHandler"),

	/** The RU n_ torrents. */
	RUN_TORRENTS(1,
			"eu.kalafatic.gemini.bt.client.controller.handlers.RunTorrentsHandler"),

	/** The RU n_ torrent s_ net. */
	RUN_TORRENTS_NET(2,
			"eu.kalafatic.gemini.bt.client.net.controller.handlers.RunTorrentsHandler"),

	/** The INI t_ torrent s_ model. */
	INIT_TORRENTS_MODEL(3,
			"eu.kalafatic.gemini.bt.client.controller.handlers.InitTorrentModelHandler"),

	/** The AD d_ torrent. */
	ADD_TORRENT(4,
			"eu.kalafatic.gemini.bt.client.controller.handlers.AddTorrentHandler"),

	/** The CREAT e_ torrent. */
	CREATE_TORRENT(5,
			"eu.kalafatic.gemini.bt.tm.controller.handlers.CreateTorrentHandler"),

	/** The OPEN. */
	OPEN(6, "eu.kalafatic.gemini.bt.client.controller.handlers.OpenHandler"),

	/** The RU n_ in t_ comm. */
	RUN_INT_COMM(7,
			"eu.kalafatic.gemini.core.handlers.RunInternalCommunicationHandler"),

	;

	/** The index. */
	public int index;

	/** The ID. */
	public String ID;

	/**
	 * Instantiates a new e handler.
	 * 
	 * @param index
	 *            the index
	 * @param id
	 *            the id
	 */
	private EHandler(int index, String id) {
		this.index = index;
		this.ID = id;
	}

}
