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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import eu.kalafatic.utils.Activator;

/**
 * The Class class FUIConstants.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public final class FUIConstants {

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
	public static final Image DRIVE_IMG_1 = Activator.getImageDescriptor("icons/drive1.png").createImage();

	/** The Constant DRIVE_IMG_2. */
	public static final Image DRIVE_IMG_2 = Activator.getImageDescriptor("icons/drive2.png").createImage();

	/** The Constant MAINTAIN_IMG. */
	public static final Image MAINTAIN_IMG = Activator.getImageDescriptor("icons/maintain.png").createImage();

	/** The Constant PROJECT_IMG. */
	public static final Image PROJECT_IMG = Activator.getImageDescriptor("icons/project/project.gif").createImage();

	/** The Constant SETTINGS_IMG. */
	public static final Image SETTINGS_IMG = Activator.getImageDescriptor("icons/settings.png").createImage();

	/** The Constant TREE_IMG. */
	public static final Image TREE_IMG = Activator.getImageDescriptor("icons/tree/tree.png").createImage();

	/** The Constant LIST_IMG. */
	public static final Image LIST_IMG = Activator.getImageDescriptor("icons/tree/list.gif").createImage();

	/** The Constant ELEMENTS_IMG. */
	public static final Image ELEMENTS_IMG = Activator.getImageDescriptor("icons/tree/elements.gif").createImage();

	/** The Constant FRAME_IMG. */
	public static final Image FRAME_IMG = Activator.getImageDescriptor("icons/tree/frame.png").createImage();

	// public static final Image SERVER_IMG = Activator.getImageDescriptor(
	// "icons/server.png").createImage();

	/** The Constant COLLAPSE_ALL_DESC. */
	public static final ImageDescriptor COLLAPSE_ALL_DESC = Activator.getImageDescriptor("icons/tree/collapse_all.gif");

	/** The Constant EXPAND_ALL_DESC. */
	public static final ImageDescriptor EXPAND_ALL_DESC = Activator.getImageDescriptor("icons/tree/expand_all.gif");

	/** The Constant APP_SIZE. */
	public final static Point APP_SIZE = new Point(800, 600);

	/** The Constant BLOCK_COLS. */
	public static final int BLOCK_COLS = 3;

	/** The Constant COMBO_WIDTH. */
	public final static int COMBO_WIDTH = 50;

	/** The Constant CHECKBOX_WIDTH. */
	public final static int CHECKBOX_WIDTH = 120;

	/** The Constant LABEL_WIDTH. */
	public final static int LABEL_WIDTH = 150;

	/** The Constant LABEL_HEIGHT. */
	public final static int LABEL_HEIGHT = 18;

	/** The Constant TEXT_WIDTH. */
	public final static int TEXT_WIDTH = 150;

	/** The Constant LONG_TEXT_WIDTH. */
	public final static int LONG_TEXT_WIDTH = 200;

	/** The Constant TEXT_HEIGHT. */
	public final static int TEXT_HEIGHT = 18;

	/** The Constant TABLE_HEIGHT. */
	public final static int TABLE_HEIGHT = 30;

	/** The Constant DIALOG_WIDTH. */
	public final static int DIALOG_WIDTH = 400;

	/** The Constant DIALOG_HEIGHT. */
	public final static int DIALOG_HEIGHT = 200;

	/** The Constant CONTROL_WIDTH. */
	public static final int CONTROL_WIDTH = 180;

	/** The Constant BUTTON_WIDTH. */
	public static final int BUTTON_WIDTH = 90;

	/** The Constant BUTTON_HEIGHT. */
	public static final int BUTTON_HEIGHT = 20;

	/** The Constant WIDGET_HEIGHT. */
	public static final int WIDGET_HEIGHT = 40;

	/** The Constant WIDGET_HEIGHT. */
	public static final int SCROLL_WIDTH = 13;
	// /** The Constant SECTION_STYLE. */
	// public final static int SECTION_STYLE = Section.TITLE_BAR | Section.TWISTIE
	// | Section.COMPACT;

	// ---------------------------------------------------------------

	/** The Constant SCROLL_BOTH. */
	public final static int SCROLL_BOTH = SWT.V_SCROLL | SWT.H_SCROLL;

	/** The Constant BORDER_SCROLL_BOTH. */
	public final static int BORDER_SCROLL_BOTH = SWT.BORDER | SCROLL_BOTH;

	/** The Constant FULL_BORDER_SCROLL_BOTH. */
	public final static int FULL_BORDER_SCROLL_BOTH = SWT.MULTI | SWT.FULL_SELECTION | BORDER_SCROLL_BOTH;

	// ---------------------------------------------------------------

	/** The Constant TAB_STYLE_1. */
	public final static int TAB_STYLE_1 = SWT.SINGLE | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | BORDER_SCROLL_BOTH;

	/** The Constant TAB_STYLE_2. */
	public final static int TAB_STYLE_2 = SWT.BORDER | SWT.CHECK | SWT.MULTI | SWT.FULL_SELECTION;

	// ---------------------------------------------------------------

	/** The Constant TABLE_STYLE_0. */
	public final static int TABLE_STYLE_0 = SWT.READ_ONLY | FULL_BORDER_SCROLL_BOTH;

	/** The Constant TABLE_STYLE_1. */
	public final static int TABLE_STYLE_1 = SWT.CHECK | TABLE_STYLE_0;

	/** The Constant TABLE_STYLE_2. */
	public final static int TABLE_STYLE_2 = SWT.APPLICATION_MODAL | TABLE_STYLE_1;

	// ---------------------------------------------------------------

	/** The Constant TREE_STYLE_0. */
	public final static int TREE_STYLE_0 = SWT.SHADOW_ETCHED_OUT | FULL_BORDER_SCROLL_BOTH;

	/** The Constant TREE_STYLE_1. */
	public final static int TREE_STYLE_1 = SWT.CHECK | TREE_STYLE_0;

	// ---------------------------------------------------------------

	/** The Constant RED. */
	public final static Color RED = Display.getDefault().getSystemColor(SWT.COLOR_RED);

	/** The Constant LIGHT_RED. */
	public final static Color LIGHT_RED = new Color(Display.getDefault(), 255, 240, 240);

	/** The Constant WHITE. */
	public final static Color WHITE = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);

	/** The Constant SILVER. */
	public final static Color SILVER = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);

	/** The Constant BLUE. */
	public final static Color BLUE = Display.getDefault().getSystemColor(SWT.COLOR_BLUE);

	/** The Constant DARK_BLUE. */
	public final static Color DARK_BLUE = Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);

	/** The Constant LIGHT_BLUE. */
	public final static Color LIGHT_BLUE = new Color(Display.getDefault(), 240, 240, 255);

	/** The Constant GREEN. */
	public final static Color GREEN = Display.getDefault().getSystemColor(SWT.COLOR_GREEN);

	/** The Constant LIGHT_GREEN. */
	public final static Color LIGHT_GREEN = new Color(Display.getDefault(), 240, 255, 240);

	/** The Constant DARK_GREEN. */
	public final static Color DARK_GREEN = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);

	// public final static Color DARK_GREEN =
	// Display.getDefault().getSystemColor(
	// SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW);

	/** The Constant YELLOW. */
	public final static Color YELLOW = Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);

	/** The Constant GRADIENT. */
	public final static Color GRADIENT = Display.getDefault().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT);

	/** The Constant CYAN. */
	public final static Color CYAN = Display.getDefault().getSystemColor(SWT.COLOR_CYAN);

	/** The Constant GRAY. */
	public final static Color GRAY = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);

	/** The Constant DARK_GRAY. */
	public final static Color DARK_GRAY = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);

	/** The Constant BLACK. */
	public final static Color BLACK = Display.getDefault().getSystemColor(SWT.COLOR_BLACK);

	/** The Constant SAND_COLOR. */
	public final static Color SAND_COLOR = new Color(Display.getDefault(), 255, 200, 50);

	/** The Constant GRASS_COLOR. */
	public final static Color GRASS_COLOR = new Color(Display.getDefault(), 230, 255, 220);

	/** The Constant ORANGE_COLOR. */
	public final static Color ORANGE_COLOR = new Color(Display.getDefault(), 255, 100, 50);

	/** The Constant NORMAL_FONT. */
	public final static Font NORMAL_FONT = new Font(Display.getDefault(), "Normal", 8, SWT.NORMAL);

	/** The Constant BOLD_FONT. */
	public final static Font BOLD_FONT = new Font(Display.getDefault(), "Arial", 8, SWT.BOLD);

	/** The Constant BOLD_6_FONT. */
	public final static Font BOLD_6_FONT = new Font(Display.getDefault(), "Courier", 10, SWT.NORMAL);

	/** The Constant PROGRESS_FONT. */
	public final static Font PROGRESS_FONT = new Font(Display.getDefault(), "Arial", 8, SWT.BOLD);

	/** The Constant ARIAL_8_FONT. */
	public final static Font ARIAL_8_FONT = new Font(Display.getDefault(), "Arial", 8, SWT.NORMAL);

	/** The Constant INDEXES_FONT. */
	public final static Font INDEXES_FONT = new Font(Display.getDefault(), "Normal", 6, SWT.ITALIC);

	/** The Constant PERCENT_FONT. */
	public final static Font PERCENT_FONT = new Font(Display.getDefault(), "Normal", 7, SWT.ITALIC);

	/** The Constant COURIER_FONT. */
	public final static Font COURIER_FONT = new Font(Display.getDefault(), "Courier New", 10, SWT.NORMAL);

	/** The Constant KB. */
	public final static long KB = 1024;

	/** The Constant MB. */
	public final static long MB = KB * KB;

	/** The Constant GB. */
	public final static long GB = KB * MB;

	/** The Constant S. */
	public final static long S = 1000;

	/** The Constant M. */
	public final static long M = 60 * S;

	/** The Constant H. */
	public final static long H = 60 * M;

}
