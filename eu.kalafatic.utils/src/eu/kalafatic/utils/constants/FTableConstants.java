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
package eu.kalafatic.utils.constants;

import org.eclipse.swt.SWT;

/**
 * The Class class FTableConstants.
 * 
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public final class FTableConstants {

	/** The Constant TAB_STYLE_1. */
	public static final int TAB_STYLE_1 = SWT.SINGLE | SWT.BORDER
			| SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION
			| SWT.HIDE_SELECTION;

	/** The Constant TAB_STYLE_2. */
	public static final int TAB_STYLE_2 = SWT.BORDER | SWT.CHECK | SWT.MULTI
			| SWT.FULL_SELECTION;

	/** The Constant TAB_STYLE_3. */
	public static final int TAB_STYLE_3 = SWT.BORDER | SWT.CHECK | SWT.H_SCROLL
			| SWT.V_SCROLL | SWT.FULL_SELECTION;

	/** The Constant VIEWER_DATA_TYPE. */
	public static final String VIEWER_DATA_TYPE = "type";

	/** The Constant VIEWER_DATA_ITEM. */
	public static final String VIEWER_DATA_ITEM = "treeItem";

	// Set the table column property names
	/** The Constant COUNT_COL. */
	public static final String COUNT_COL = "Number";

	/** The Constant NAME_COL. */
	public static final String NAME_COL = "Name";

	/** The Constant STATUS_COL. */
	public static final String STATUS_COL = "Status";

	/** The Constant SIZE_COL. */
	public static final String SIZE_COL = "Size";

	/** The Constant DOWNLOADED_COL. */
	public static final String DOWNLOADED_COL = "Downloaded";

	/** The Constant COMPLETED_COL. */
	public static final String COMPLETED_COL = "% Completed";

	/** The Constant DWNLD_SPEED_COL. */
	public static final String DWNLD_SPEED_COL = "Dwnld speed";

	/** The Constant UPLD_SPEED_COL. */
	public static final String UPLD_SPEED_COL = "Upld speed";

	// Set column names
	/** The Constant TORRENTS_COL_NAMES. */
	public static final String[] TORRENTS_COL_NAMES = new String[] { NAME_COL,
			SIZE_COL, DOWNLOADED_COL, COMPLETED_COL, DWNLD_SPEED_COL,
			UPLD_SPEED_COL };

	// torrent details table
	/** The VALU e_ col. */
	public static String VALUE_COL = "Value";

	/** The DESCRIPTIO n_ col. */
	public static String DESCRIPTION_COL = "Description";

	// Set column names
	/** The TORREN t_ detail s_ co l_ names. */
	public String[] TORRENT_DETAILS_COL_NAMES = new String[] { NAME_COL,
			VALUE_COL, DESCRIPTION_COL };

	/** The NAM e_ row. */
	public static String NAME_ROW = "Name";

	/** The TRACKE r_ row. */
	public static String TRACKER_ROW = "Tracker";

	/** The BITMA p_ row. */
	public static String BITMAP_ROW = "Bitmap";

	// Set column names
	/** The TORREN t_ detail s_ ro w_ names. */
	public String[] TORRENT_DETAILS_ROW_NAMES = new String[] { NAME_ROW,
			TRACKER_ROW, BITMAP_ROW };

	// client detail table
	// Set column names
	/** The CLIENT t_ detail s_ co l_ names. */
	public String[] CLIENTT_DETAILS_COL_NAMES = new String[] { NAME_COL,
			VALUE_COL, DESCRIPTION_COL };

	/** The PROTOCO l_ row. */
	public static String PROTOCOL_ROW = "Protocol";

	/** The HOS t_ row. */
	public static String HOST_ROW = "Host";

	/** The POR t_ row. */
	public static String PORT_ROW = "Port";

	/** The AP p_ row. */
	public static String APP_ROW = "Application";

	/** The DESCRIPTIO n_ row. */
	public static String DESCRIPTION_ROW = "Description";

	// Set column names
	/** The CLIEN t_ detail s_ ro w_ names. */
	public String[] CLIENT_DETAILS_ROW_NAMES = new String[] { PROTOCOL_ROW,
			HOST_ROW, PORT_ROW, APP_ROW, BITMAP_ROW, DESCRIPTION_ROW };

}
