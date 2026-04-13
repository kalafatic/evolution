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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.channels.FileChannel;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import eu.kalafatic.utils.constants.FTextConstants;
import eu.kalafatic.utils.dialogs.DialogUtils;
import eu.kalafatic.utils.lib.EExt;
import eu.kalafatic.utils.lib.EMimeType;

/**
 * The Class class FileUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class FileUtils {

	/** The Constant CHUNK. */
	public static final long CHUNK = 16 * 1024 * 1024; /* 16 Mb */

	/** The Constant NEW_LINE. */
	public static final String NEW_LINE = "\r\n"; /* 16 Mb */

	/** The INSTANCE. */
	private volatile static FileUtils INSTANCE;

	/**
	 * Gets the single instance of FileUtils.
	 * @return single instance of FileUtils
	 */
	public static FileUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (FileUtils.class) {
				INSTANCE = new FileUtils();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the text from file.
	 * @param path the path
	 * @param appendEndOfLine the append end of line
	 * @return the text from file
	 */
	public String getTextFromFile(String path, boolean appendEndOfLine) {
		return getTextFromFile(new File(path), appendEndOfLine);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the text from file.
	 * @param file the file
	 * @param appendEndOfLine the append end of line
	 * @return the text from file
	 */
	public String getTextFromFile(File file, boolean appendEndOfLine) {
		StringBuffer sb = new StringBuffer();
		try {
			String line = null;
			BufferedReader reader = new BufferedReader(new FileReader(file));

			while ((line = reader.readLine()) != null) {
				sb.append(line);

				if (appendEndOfLine) {
					sb.append(NEW_LINE);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the file.
	 * @param name the name
	 * @param path the path
	 * @param content the content
	 * @return the file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public File createFile(String name, String path, String content) throws IOException {
		File file = new File(path.concat(File.separator).concat(name));
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		try {
			writer.write(content);
			return file;
		} finally {
			writer.flush();
			writer.close();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Removes the file job.
	 * @param srcFolder the src folder
	 * @return the job
	 */
	public Job removeFileJob(final File srcFolder) {
		String jobName = "Removing : " + srcFolder.getName();

		return new Job(jobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					removeFile(srcFolder);
					return Status.OK_STATUS;
				} catch (Exception e) {
					e.printStackTrace();
					return Status.CANCEL_STATUS;
				}
			}

			private void removeFile(File srcFile) {
				if (srcFile.isDirectory()) {
					for (File file : srcFile.listFiles()) {
						removeFile(file);
					}
					srcFile.delete();
				} else {
					srcFile.delete();
				}
			}
		};
	}

	// ---------------------------------------------------------------

	/**
	 * Move file job.
	 * @param srcFileName the src file name
	 * @param destFolder the dest folder
	 * @return the job
	 */
	public Job moveFileJob(final String srcFileName, final File destFolder) {
		String jobName = "Moving file : " + new File(srcFileName).getName() + "  to  " + destFolder.getPath();

		return new Job(jobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					moveFile(srcFileName, destFolder, monitor);
					return Status.OK_STATUS;
				} catch (Exception e) {
					e.printStackTrace();
					return Status.CANCEL_STATUS;
				}
			}
		};
	}

	// ---------------------------------------------------------------

	/**
	 * Move file.
	 * @param srcPath the src path
	 * @param destFolder the dest folder
	 * @param monitor the monitor
	 * @throws Exception the exception
	 */
	private void moveFile(String srcPath, File destFolder, IProgressMonitor monitor) throws Exception {
		try {
			File srcFile = new File(srcPath);
			monitor.beginTask("Moving is in progress ...", 100);

			if (!moveFile(srcFile, destFolder)) {
				throw new OperationCanceledException();
			}
		} finally {
			monitor.done();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Copy file job.
	 * @param srcFileName the src file name
	 * @param destFileName the dest file name
	 * @return the job
	 */
	public Job copyFileJob(final String srcFileName, final String destFileName) {
		String jobName = "Copying file : " + new File(srcFileName).getName() + "  to  " + new File(destFileName).getParent();

		return new Job(jobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					copyFile(srcFileName, destFileName, monitor);
					return Status.OK_STATUS;
				} catch (Exception e) {
					e.printStackTrace();
					return Status.CANCEL_STATUS;
				}
			}
		};
	}

	// ---------------------------------------------------------------

	/**
	 * Copy file.
	 * @param srcFileName the src file name
	 * @param destFileName the dest file name
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	public boolean copyFile(String srcFileName, String destFileName) throws Exception {
		File srcFile = new File(srcFileName);
		File destFile = new File(destFileName);

		if (isSrcDestFileOK(srcFile, destFile)) {
			return copyFile(srcFile, destFile, null);
		}
		return false;
	}

	// ---------------------------------------------------------------

	/**
	 * Copy file.
	 * @param srcFileName the src file name
	 * @param destFileName the dest file name
	 * @param monitor the monitor
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	public boolean copyFile(String srcFileName, String destFileName, IProgressMonitor monitor) throws Exception {
		File srcFile = new File(srcFileName);
		File destFile = new File(destFileName);

		if (isSrcDestFileOK(srcFile, destFile)) {
			return copyFile(srcFile, destFile, monitor);
		}
		return false;
	}

	// ---------------------------------------------------------------

	/**
	 * Copy file.
	 * @param srcFile the src file
	 * @param destFile the dest file
	 * @param monitor the monitor
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	public boolean copyFile(File srcFile, File destFile, IProgressMonitor monitor) throws Exception {
		FileChannel inChannel = new FileInputStream(srcFile).getChannel();
		FileChannel outChannel = new FileOutputStream(destFile).getChannel();
		try {
			long position = 0, size = inChannel.size();
			long total = size / CHUNK;
			monitor.beginTask("Copying is in progress ...", (int) (total));

			while (position < size) {

				if (monitor.isCanceled()) {
					throw new OperationCanceledException();
				}
				position += inChannel.transferTo(position, CHUNK, outChannel);
				monitor.worked(1);
			}
			return true;
		} finally {
			if (inChannel != null) {
				inChannel.close();
			}
			if (outChannel != null) {
				outChannel.close();
			}
			monitor.done();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Move file.
	 * @param srcFileName the src file name
	 * @param destFileName the dest file name
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	public boolean moveFile(String srcFileName, String destFileName) throws Exception {
		File srcFile = new File(srcFileName);
		File destFile = new File(destFileName);

		if (isSrcDestFileOK(srcFile, destFile)) {
			return moveFile(srcFile, destFile);
		}
		return false;
	}

	// ---------------------------------------------------------------

	/**
	 * Move file.
	 * @param srcFile the src file
	 * @param destFolder the dest folder
	 * @return true, if successful
	 * @throws Exception the exception
	 */
	public boolean moveFile(File srcFile, File destFolder) throws Exception {
		// Move file to new directory
		return srcFile.renameTo(new File(destFolder, srcFile.getName()));
	}

	// ---------------------------------------------------------------

	/**
	 * Checks if is src dest file ok.
	 * @param srcFile the src file
	 * @param destFile the dest file
	 * @return true, if is src dest file ok
	 * @throws Exception the exception
	 */
	public boolean isSrcDestFileOK(File srcFile, File destFile) throws Exception {
		if (!srcFile.exists()) {
			DialogUtils.INSTANCE.error("File doesn't exists : " + srcFile.getName());
			return false;
		}
		if (!destFile.exists()) {
			if (!destFile.createNewFile()) {
				DialogUtils.INSTANCE.error("Can't create file : " + destFile.getName());
				return false;
			}
		}
		return true;
	}

	// ---------------------------------------------------------------

	/**
	 * Open file.
	 * @param isFile the is file
	 * @param args the args
	 * @return the string
	 */
	public String openFile(boolean isFile, Object... args) {
		Shell shell = new Shell(Display.getDefault());
		String filterPath = (args.length > 0) ? (String) args[0] : null;
		String result = null;

		if (isFile) {
			FileDialog dialog = new FileDialog(shell);
			dialog.setFilterPath(filterPath);
			result = dialog.open();
		} else {
			DirectoryDialog dialog = new DirectoryDialog(shell);
			dialog.setFilterPath(null);
			result = dialog.open();
		}
		if (result == null) {
			DialogUtils.INSTANCE.error("Nothing selected");
		}
		return result;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the bytes from file.
	 * @param path the path
	 * @return the bytes from file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public byte[] getBytesFromFile(String path) throws IOException {
		return getBytesFromFile(new File(path));
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the bytes from file.
	 * @param file the file
	 * @return the bytes from file
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();

		if (length > Integer.MAX_VALUE) {
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file " + file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

	// ---------------------------------------------------------------

	/**
	 * Write file.
	 * @param filename the filename
	 * @param text the text
	 */
	public void writeFile(String filename, String text) {
		try {
			writeFile(new File(filename), text.getBytes(FTextConstants.UTF8));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Write file.
	 * @param file the file
	 * @param content the content
	 */
	public void writeFile(File file, byte[] content) {
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(content);
			fos.close();

		} catch (IOException e) {}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the mime type.
	 * @param file the file
	 * @return the mime type
	 */
	public EMimeType getMimeType(File file) {
		return getMimeType(file.getName());
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the mime type.
	 * @param fileName the file name
	 * @return the mime type
	 */
	public EMimeType getMimeType(String fileName) {
		// Extension without the dot
		EExt ext = getExtension(fileName);
		if (ext != null) {
			return ext.mime;
		}
		return null;
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the extension.
	 * @param file the file
	 * @return the extension
	 */
	public EExt getExtension(File file) {
		return getExtension(file.getAbsolutePath());
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the extension.
	 * @param fileName the file name
	 * @return the extension
	 */
	public EExt getExtension(String fileName) {
		// Extension without the dot
		String ext;
		int dotPlace = fileName.lastIndexOf('.');
		// Get the extension
		if (dotPlace >= 0) {
			ext = fileName.substring(dotPlace);
		} else {
			// No extension
			ext = "";
		}
		// Return the extension
		return EExt.getExtension(ext);
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the segments.
	 * @param filePath the file path
	 * @return the segments
	 */
	public static String[] getSegments(String filePath) {
		String pathRegex = "[/\\\\]";
		return filePath.split(pathRegex, -1);
	}
}
