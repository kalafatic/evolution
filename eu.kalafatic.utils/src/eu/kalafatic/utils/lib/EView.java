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
 * The Enum enum EView.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public enum EView {

	/** The CONSOLE. */
	CONSOLE("eu.kalafatic.gemini.core.views.ConsoleView"),

	/** The PROGRESS. */
	PROGRESS("org.eclipse.ui.views.ProgressView"),

	ERRORS("org.eclipse.pde.runtime.LogView"),

	/** The WORKSPACE. */
	WORKSPACE("eu.kalafatic.gemini.core.views.WorkspaceView"),

	/** The FIL e_ navigator. */
	FILE_NAVIGATOR("eu.kalafatic.gemini.core.views.FileNavigatorView"),

	/** The SWARMS. */
	SWARMS("eu.kalafatic.gemini.bt.tracker.view.views.SwarmsView"),

	/** The SWARM s_ graph. */
	SWARMS_GRAPH("eu.kalafatic.gemini.bt.client.net.view.views.BTGraphView"),

	/** The SWARM s_ activity. */
	SWARMS_ACTIVITY("eu.kalafatic.gemini.bt.client.net.view.views.BTSwarmActivityView"),

	/** The SWARM s_ tree. */
	SWARMS_TREE("eu.kalafatic.gemini.bt.client.net.view.views.SwarmTreeView"),

	/** The TRACKER. */
	TRACKER("eu.kalafatic.gemini.bt.tracker.view.views.TrackerView"),

	/** The TORRENT s_ table. */
	TORRENTS_TABLE("eu.kalafatic.gemini.bt.client.view.views.TorrentsTableView"),

	/** The FINISHE d_ torrent s_ table. */
	FINISHED_TORRENTS_TABLE("eu.kalafatic.gemini.bt.client.view.views.FinishedTorrentsTableView"),

	/** The TORREN t_ detail. */
	TORRENT_DETAIL("eu.kalafatic.gemini.bt.client.view.views.TorrentDetailView"),

	/** The SESSIO n_ detail. */
	SESSION_DETAIL("eu.kalafatic.gemini.bt.client.net.view.views.SessionDetailView"),

	/** The SESSIO n_ property. */
	SESSION_PROPERTY("eu.kalafatic.gemini.bt.tracker.view.views.SessionPropertyView"),

	/** The VIDE o_ player. */
	VIDEO_PLAYER("eu.kalafatic.gemini.media.view.views.VideoPlayerView"),

	/** The BROWSER. */
	BROWSER("eu.kalafatic.gemini.webbrowser.view.views.WebBrowserView"),

	/** The BROWSE r_ navigator. */
	BROWSER_NAVIGATOR("eu.kalafatic.gemini.webbrowser.view.views.WebBrowserNavigator"),

	/** The PRE f_ algorithms. */
	PREF_ALGORITHMS("eu.kalafatic.gemini.bt.client.view.preferences.AlgorithmsPreferencePage"),

	/** The RC. */
	RC("eu.kalafatic.gemini.bt.client.net.rc.view.views.RCView"),

	/** The R c_ navigator. */
	RC_NAVIGATOR("eu.kalafatic.gemini.bt.client.net.rc.view.views.RCNavigator"),

	/** The STATISTICS. */
	STATISTICS("eu.kalafatic.gemini.stat.view.views.StatisticsView"),

	/** The STATISTIC s_ navigator. */
	STATISTICS_NAVIGATOR("eu.kalafatic.gemini.stat.view.views.StatTreeView"),

	;

	/** The ID. */
	public String ID;

	/**
	 * Instantiates a new e view.
	 * 
	 * @param index
	 *            the index
	 * @param id
	 *            the id
	 */
	private EView(String id) {
		this.ID = id;
	}
}
