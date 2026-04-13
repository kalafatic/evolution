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
package eu.kalafatic.utils.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.misc.StatusUtil;
import org.eclipse.ui.statushandlers.StatusManager;

import eu.kalafatic.utils.Activator;
import eu.kalafatic.utils.constants.FCoreImageConstants;
import eu.kalafatic.utils.lib.EExt;

/**
 * The Class class ImageUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
@SuppressWarnings("restriction")
public class ImageUtils {

	/** The image loader. */
	private static ImageLoader imageLoader = new ImageLoader();;

	/** The image data array. */
	private static ImageData[] imageDataArray;

	/** The animated. */
	private static Image[] animated;

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the file extension image.
	 * @param fileName the file name
	 * @return the file extension image
	 */
	public static Image getFileExtensionImage(String fileName) {
		if (fileName.endsWith(".txt")) {
			return FCoreImageConstants.TXT_IMG;
		} else if (fileName.endsWith(".pdf")) {
			return FCoreImageConstants.PDF_IMG;
		} else if (fileName.endsWith(".doc")) {
			return FCoreImageConstants.WORD_IMG;
		} else if (fileName.endsWith(".avi")) {
			return FCoreImageConstants.VIDEO_IMG;
		} else if (fileName.endsWith(".txt")) {
			return FCoreImageConstants.TXT_IMG;
		} else if (fileName.endsWith(".mp3")) {
			return FCoreImageConstants.AUDIO_IMG;
		} else if (fileName.endsWith(".rar")) {
			return FCoreImageConstants.RAR_IMG;
		} else if (fileName.endsWith(".zip")) {
			return FCoreImageConstants.ZIP_IMG;
		} else if (fileName.endsWith(".exe")) {
			return FCoreImageConstants.EXE_IMG;
		} else if (fileName.endsWith(".msi")) {
			return FCoreImageConstants.MSI_IMG;
		} else if (fileName.endsWith(EExt.TORRENT.ext)) {
			return FCoreImageConstants.TORRENT_IMG;
		}

		return FCoreImageConstants.FILE_IMG;
	}

	// ---------------------------------------------------------------

	/**
	 * Load gif.
	 * @param path the path
	 * @return the image[]
	 */
	public static Image[] loadGif(String path) {
		File imageFile = Activator.getImageFile(path);
		imageDataArray = imageLoader.load(imageFile.getAbsolutePath());
		animated = new Image[imageDataArray.length];

		for (int i = 0; i < imageDataArray.length; i++) {
			animated[i] = new Image(Display.getDefault(), imageDataArray[i]);
		}
		return animated;
	}

	// ---------------------------------------------------------------

	/**
	 * Load image.
	 * @param path the path
	 * @return the image
	 */
	public static Image loadImage(String path) {
		Image image = null;
		if (path != null) {
			InputStream input = null;
			try {
				input = new BufferedInputStream(new FileInputStream(path));
				image = new Image(Display.getDefault(), input);
			} catch (SWTException e) {
				StatusManager.getManager().handle(StatusUtil.newStatus(WorkbenchPlugin.PI_WORKBENCH, e));
			} catch (IOException e) {
				StatusManager.getManager().handle(StatusUtil.newStatus(WorkbenchPlugin.PI_WORKBENCH, e));
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						// he's done for
					}
				}
			}
		}
		return image;
	}

	// ---------------------------------------------------------------

	/**
	 * Resize.
	 * @param image the image
	 * @param width the width
	 * @param height the height
	 * @return the image
	 */
	@SuppressWarnings("unused")
	private static Image resize(Image image, int width, int height) {
		Image scaled = new Image(Display.getDefault(), width, height);
		GC gc = new GC(scaled);
		gc.setAntialias(SWT.ON);
		gc.setInterpolation(SWT.HIGH);
		gc.drawImage(image, 0, 0, image.getBounds().width, image.getBounds().height, 0, 0, width, height);
		gc.dispose();
		image.dispose(); // don't forget about me!
		return scaled;
	}
}
