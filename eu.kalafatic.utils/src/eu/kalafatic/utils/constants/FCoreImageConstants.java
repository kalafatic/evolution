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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import eu.kalafatic.utils.Activator;

/**
 * The Class class FCoreImageConstants.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public final class FCoreImageConstants {

	/** The shared images. */
	private static ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	// FILE STRUCTURE
	/** The Constant FOLDER_IMG. */
	public static final Image FOLDER_IMG = sharedImages.getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER).createImage();

	/** The Constant FILE_IMG. */
	public static final Image FILE_IMG = sharedImages.getImageDescriptor(ISharedImages.IMG_OBJ_FILE).createImage();

	/** The Constant DRIVE_IMG_1. */
	public static final Image DRIVE_IMG_1 = Activator.getImageDescriptor("icons/gemini/drive1.png").createImage();

	/** The Constant DRIVE_IMG_2. */
	public static final Image DRIVE_IMG_2 = Activator.getImageDescriptor("icons/gemini/drive2.png").createImage();

	/** The Constant INFO_IMG. */
	public static final Image INFO_IMG = sharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK).createImage();

	public static final Image PROJECT_IMG = Activator.getImageDescriptor("icons/project/project.gif").createImage();

	public static final Image SETTINGS_IMG = Activator.getImageDescriptor("icons/settings.png").createImage();

	public static final Image LIST_IMG = Activator.getImageDescriptor("icons/tree/list.gif").createImage();

	// ---------------------------------------------------------------

	// GEMINI
	/** The Constant TORRENT_IMG. */
	public static final Image TORRENT_IMG = Activator.getImageDescriptor("icons/gemini/t.jpg").createImage();

	/** The Constant GEMINI_IMG. */
	public static final Image GEMINI_IMG = Activator.getImageDescriptor("icons/branding/tray-16.PNG").createImage();

	/** The Constant ABOUT_DESC. */
	public static final ImageDescriptor ABOUT_DESC = Activator.getImageDescriptor("icons/about.png");

	// ---------------------------------------------------------------

	// NETWORK
	/** The Constant SERVER_IMG. */
	public static final Image SERVER_IMG = Activator.getImageDescriptor("icons/network/server.png").createImage();

	/** The Constant SERVER_ENABLED_IMG. */
	public static final Image SERVER_ENABLED_IMG = Activator.getImageDescriptor("icons/network/enable-server.png").createImage();

	/** The Constant SERVER_DISABLED_IMG. */
	public static final Image SERVER_DISABLED_IMG = Activator.getImageDescriptor("icons/network/disable-server.png").createImage();

	/** The Constant CONN_OK_IMG. */
	public static final Image CONN_OK_IMG = Activator.getImageDescriptor("icons/gemini/tray-icon16-16.jpg").createImage();

	/** The Constant CONN_ERR_IMG. */
	public static final Image CONN_ERR_IMG = Activator.getImageDescriptor("icons/gemini/tray-icon16-16-red.jpg").createImage();

	// ---------------------------------------------------------------

	// TOOLBAR
	/** The Constant PREFERENCES_DESC. */
	public static final ImageDescriptor PREFERENCES_DESC = Activator.getImageDescriptor("icons/toolbar/preferences.png");

	/** The Constant SESSION_IMG. */
	public static final Image SESSION_IMG = Activator.getImageDescriptor("icons/session.png").createImage();

	// ---------------------------------------------------------------

	// ACTIONS
	/** The Constant PENDING_IMG. */
	public static final Image PENDING_IMG = Activator.getImageDescriptor("icons/actions/pending.gif").createImage();

	/** The Constant UPDATE_IMG. */
	public static final Image UPDATE_IMG = Activator.getImageDescriptor("icons/actions/update1.png").createImage();

	/** The Constant ADD_DESC. */
	public static final ImageDescriptor ADD_DESC = Activator.getImageDescriptor("icons/actions/add_obj.gif");

	/** The Constant ADD_X_DESC. */
	public static final ImageDescriptor ADD_X_DESC = Activator.getImageDescriptor("icons/actions/add_exc.gif");

	/** The Constant OPEN_DESC. */
	public static final ImageDescriptor OPEN_DESC = Activator.getImageDescriptor("icons/actions/open.png");

	/** The Constant CLEAR_DESC. */
	public static final ImageDescriptor CLEAR_DESC = Activator.getImageDescriptor("icons/actions/clear.gif");

	/** The Constant DELETE_DESC. */
	public static final ImageDescriptor DELETE_DESC = Activator.getImageDescriptor("icons/actions/delete.gif");

	/** The Constant DELETE_OBJ_DESC. */
	public static final ImageDescriptor DELETE_OBJ_DESC = Activator.getImageDescriptor("icons/actions/delete_obj.gif");

	/** The Constant EXIT_DESC. */
	public static final ImageDescriptor EXIT_DESC = Activator.getImageDescriptor("icons/actions/exit.png");

	/** The Constant FORWARD_IMG. */
	public static final Image FORWARD_IMG = Activator.getImageDescriptor("icons/actions/forward.png").createImage();

	/** The Constant PAUSE_DESC. */
	public static final ImageDescriptor PAUSE_DESC = Activator.getImageDescriptor("icons/actions/pause.png");

	/** The Constant PLAY_IMG. */
	public static final Image PLAY_IMG = Activator.getImageDescriptor("icons/actions/play.png").createImage();

	/** The Constant REWIND_IMG. */
	public static final Image REWIND_IMG = Activator.getImageDescriptor("icons/actions/rewind.png").createImage();

	/** The Constant RELOAD_IMG. */
	public static final Image RELOAD_IMG = Activator.getImageDescriptor("icons/actions/reload.jpeg").createImage();

	/** The Constant STOP_IMG. */
	public static final Image STOP_IMG = Activator.getImageDescriptor("icons/actions/stop.png").createImage();

	/** The Constant HOME_IMG. */
	public static final Image HOME_IMG = Activator.getImageDescriptor("icons/actions/home.png").createImage();

	/** The Constant SAVE_ALL_DESC. */
	public static final ImageDescriptor SAVE_ALL_DESC = Activator.getImageDescriptor("icons/actions/save_all.gif");

	/** The Constant SELECT_DESC. */
	public static final ImageDescriptor SELECT_DESC = Activator.getImageDescriptor("icons/actions/select.gif");

	/** The Constant CHECK_DESC. */
	public static final ImageDescriptor CHECK_DESC = Activator.getImageDescriptor("icons/actions/check.gif");

	/** The Constant UP_NAV_DESC. */
	public static final ImageDescriptor UP_NAV_DESC = Activator.getImageDescriptor("icons/actions/up_nav.gif");

	/** The Constant UPLOAD_DESC. */
	public static final ImageDescriptor UPLOAD_DESC = Activator.getImageDescriptor("icons/actions/upload.gif");

	/** The Constant COLLAPSE_ALL_DESC. */
	public static final ImageDescriptor COLLAPSE_ALL_DESC = Activator.getImageDescriptor("icons/actions/collapse_all.gif");

	/** The Constant EXPAND_ALL_DESC. */
	public static final ImageDescriptor EXPAND_ALL_DESC = Activator.getImageDescriptor("icons/actions/expand_all.gif");

	// ---------------------------------------------------------------

	// FILES
	/** The Constant AUDIO_IMG. */
	public static final Image AUDIO_IMG = Activator.getImageDescriptor("icons/files/audio_icon.gif").createImage();

	/** The Constant EXCEL_IMG. */
	public static final Image EXCEL_IMG = Activator.getImageDescriptor("icons/files/excel_icon.gif").createImage();

	/** The Constant EXE_IMG. */
	public static final Image EXE_IMG = Activator.getImageDescriptor("icons/files/exe_icon.gif").createImage();

	/** The Constant IMAGE_IMG. */
	public static final Image IMAGE_IMG = Activator.getImageDescriptor("icons/files/image_icon.gif").createImage();

	/** The Constant MSI_IMG. */
	public static final Image MSI_IMG = Activator.getImageDescriptor("icons/files/msi_icon.gif").createImage();

	/** The Constant PDF_IMG. */
	public static final Image PDF_IMG = Activator.getImageDescriptor("icons/files/pdf_icon.gif").createImage();

	/** The Constant RAR_IMG. */
	public static final Image RAR_IMG = Activator.getImageDescriptor("icons/files/rar_icon.gif").createImage();

	/** The Constant TXT_IMG. */
	public static final Image TXT_IMG = Activator.getImageDescriptor("icons/files/txt_icon.png").createImage();

	/** The Constant VIDEO_IMG. */
	public static final Image VIDEO_IMG = Activator.getImageDescriptor("icons/files/video_icon.gif").createImage();

	/** The Constant WORD_IMG. */
	public static final Image WORD_IMG = Activator.getImageDescriptor("icons/files/word_icon.png").createImage();

	/** The Constant ZIP_IMG. */
	public static final Image ZIP_IMG = Activator.getImageDescriptor("icons/files/zip_icon.gif").createImage();

	// ---------------------------------------------------------------

	// FLAGS
	/** The Constant CS_IMG. */
	public static final Image CS_IMG = Activator.getImageDescriptor("icons/languages/flags/czech.gif").createImage();

	/** The Constant EN_IMG. */
	public static final Image EN_IMG = Activator.getImageDescriptor("icons/languages/flags/english.gif").createImage();

	/** The Constant DE_IMG. */
	public static final Image DE_IMG = Activator.getImageDescriptor("icons/languages/flags/german.gif").createImage();

	/** The Constant ZH_IMG. */
	public static final Image ZH_IMG = Activator.getImageDescriptor("icons/languages/flags/china.gif").createImage();

	/** The Constant RU_IMG. */
	public static final Image RU_IMG = Activator.getImageDescriptor("icons/languages/flags/russia.gif").createImage();

	/** The Constant ES_IMG. */
	public static final Image ES_IMG = Activator.getImageDescriptor("icons/languages/flags/spain.gif").createImage();

	// ---------------------------------------------------------------

	// OTHER
	/** The Constant SPLASH_IMG. */
	public static final Image SPLASH_IMG = Activator.getImageDescriptor("splash.bmp").createImage();

	/** The Constant CONSOLE_IMG. */
	public static final Image CONSOLE_IMG = Activator.getImageDescriptor("icons/console.png").createImage();

	/** The Constant TREE_IMG. */
	public static final Image TREE_IMG = Activator.getImageDescriptor("icons/tree.gif").createImage();

	/** The Constant WEB_IMG. */
	public static final Image WEB_IMG = Activator.getImageDescriptor("icons/toolbar/web.gif").createImage();

	/** The Constant ARROW_UP_LABEL_IMG. */
	public static final Image ARROW_UP_LABEL_IMG = Activator.getImageDescriptor("icons/actions/arrow_up_label.png").createImage();

	/** The Constant ARROW_DOWN_LABEL_IMG. */
	public static final Image ARROW_DOWN_LABEL_IMG = Activator.getImageDescriptor("icons/actions/arrow_down_label.png").createImage();

	/** The Constant ARROW_UP_IMG. */
	public static final Image ARROW_UP_IMG = Activator.getImageDescriptor("icons/actions/arrow_up.png").createImage();

	/** The Constant ARROW_DOWN_IMG. */
	public static final Image ARROW_DOWN_IMG = Activator.getImageDescriptor("icons/actions/arrow_down.png").createImage();

	/** The Constant PROGRESS_IMG_DESC. */
	public static final ImageDescriptor PROGRESS_IMG_DESC = Activator.getImageDescriptor("icons/actions/progress.gif");

	/** The Constant PROGRESS_IMG. */
	public static final Image PROGRESS_IMG = PROGRESS_IMG_DESC.createImage();
	// public ImageData[] PROGRESS_IMG_DATA =
	// loader.load("icons/progress.gif");
	/** The Constant WAIT_IMG. */
	public static final Image WAIT_IMG = Activator.getImageDescriptor("icons/actions/wait.png").createImage();

	// ---------------------------------------------------------------

	// TORRENT
	/** The Constant LOCK_IMG. */
	public static final Image LOCK_IMG = Activator.getImageDescriptor("icons/torrent/lock.gif").createImage();

	/** The Constant PIECES_IMG. */
	public static final Image PIECES_IMG = Activator.getImageDescriptor("icons/torrent/pieces.png").createImage();

	/** The Constant SIZE_IMG. */
	public static final Image SIZE_IMG = Activator.getImageDescriptor("icons/torrent/size.jpg").createImage();

	/** The Constant DATE_IMG. */
	public static final Image DATE_IMG = Activator.getImageDescriptor("icons/torrent/date.jpg").createImage();

	/** The Constant ENCODING_IMG. */
	public static final Image ENCODING_IMG = Activator.getImageDescriptor("icons/torrent/encoding.jpg").createImage();

	/** The Constant COMMENT_IMG. */
	public static final Image COMMENT_IMG = Activator.getImageDescriptor("icons/torrent/comment.jpg").createImage();

	/** The Constant URL_IMG. */
	public static final Image URL_IMG = Activator.getImageDescriptor("icons/torrent/link.png").createImage();

	/** The Constant CREATOR_IMG. */
	public static final Image CREATOR_IMG = Activator.getImageDescriptor("icons/torrent/creator.png").createImage();

}
