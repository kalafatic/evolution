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
package eu.kalafatic.utils.log;

import static eu.kalafatic.utils.constants.FConstants.PREFERENCES;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.eclipse.core.runtime.Path;

import eu.kalafatic.utils.constants.FConstants;
import eu.kalafatic.utils.interfaces.ILog;
import eu.kalafatic.utils.lib.ELogType;
import eu.kalafatic.utils.model.LogElement;
import eu.kalafatic.utils.preferences.ECorePreferences;

/**
 * The Class class LoggerUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */
public class LoggerUtils {

	/** The CHEC k_ after. */
	private final int CHECK_AFTER = PREFERENCES.getInt(ECorePreferences.CHECK_AFTER.getName(), (Integer) ECorePreferences.CHECK_AFTER.getDef());

	/** The LO g_ size. */
	private final int LOG_SIZE = PREFERENCES.getInt(ECorePreferences.LOG_SIZE.getName(), (Integer) ECorePreferences.LOG_SIZE.getDef()) * 1024;

	/** The check counter. */
	private int checkCounter = 0;

	/** The simple date format. */
	private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH.mm.ss");

	/** The log map. */
	private final Map<String, LogGroup> logMap = new HashMap<String, LogGroup>();

	/** The INSTANCE. */
	private volatile static LoggerUtils INSTANCE;

	/**
	 * Gets the single instance of LoggerUtils.
	 * @return single instance of LoggerUtils
	 */
	public static LoggerUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (LoggerUtils.class) {
				INSTANCE = new LoggerUtils();
				INSTANCE.initPool();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * The Class class LogGroup.
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	class LogGroup {

		/** The logger. */
		public Logger logger;

		/** The handler. */
		public FileHandler handler;

		/** The file. */
		public File file;

		/**
		 * Instantiates a new log group.
		 * @param logger the logger
		 * @param handler the handler
		 * @param file the file
		 */
		public LogGroup(Logger logger, FileHandler handler, File file) {
			this.logger = logger;
			this.handler = handler;
			this.file = file;
		}
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Gets the logger.
	 * @param name the name
	 * @return the logger
	 */
	public Logger getLogger(String name) {
		if (logMap.get(name) == null) {
			createLogger(name);
		}
		if ((checkCounter++) > CHECK_AFTER) {
			checkSize(logMap.get(name));
			checkCounter = 0;
		}
		return logMap.get(name).logger;
	}

	// ---------------------------------------------------------------

	/**
	 * Creates the logger.
	 * @param name the name
	 */
	private void createLogger(String name) {
		try {
			// Create an appending file handler
			boolean append = true;
			File file = null;
			FileHandler handler = null;

			String location = PREFERENCES.get(ECorePreferences.LOGS_LOC.getName(), (String) ECorePreferences.LOGS_LOC.getDef());

			String logType = PREFERENCES.get(ECorePreferences.LOG_TYPE.getName(), (String) ECorePreferences.LOG_TYPE.getDef());

			if (!(new File(location).exists())) {
				new File(location).mkdirs();
			}

			if (logType.equals(ELogType.TXT.getValue())) {
				file = new File(location.concat(File.separator).concat(name + ".txt"));
				// handler = new FileHandler(file.getAbsolutePath(), append);
				handler = new FileHandler(file.getAbsolutePath(), LOG_SIZE, 1, append);
				handler.setFormatter(new SimpleFormatter());

			} else if (logType.equals(ELogType.HTML.getValue())) {
				file = new File(location.concat(File.separator).concat(name + ".html"));
				// handler = new FileHandler(file.getAbsolutePath(), append);
				handler = new FileHandler(file.getAbsolutePath(), LOG_SIZE, 1, append);
				handler.setFormatter(new LogHtmlFormatter());

			} else if (logType.equals(ELogType.XML.getValue())) {
				file = new File(location.concat(File.separator).concat(name + ".xml"));
				// handler = new FileHandler(file.getAbsolutePath(), append);
				handler = new FileHandler(file.getAbsolutePath(), LOG_SIZE, 1, append);
				handler.setFormatter(new SimpleFormatter());
			}

			// Add to the desired logger
			Logger logger = Logger.getLogger(name);
			logger.addHandler(handler);

			logMap.put(name, new LogGroup(logger, handler, file));

		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Inits the pool.
	 */
	public void initPool() {
		try {
			while (FConstants.LOGS.size() < ILog.MAX_LOGS) {
				FConstants.LOGS.add(new LogElement());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Gets the log element.
	 * @param type the type
	 * @param name the name
	 * @param msg the msg
	 * @return the log element
	 */
	public synchronized LogElement getLogElement(String type, String name, String msg) {
		LogElement logElement = null;
		try {
			logElement = FConstants.LOGS.remove(FConstants.LOGS.size() - 1);

			if (logElement == null) {
				logElement = new LogElement();
			}
			FConstants.LOGS.add(0, logElement);

			String date = simpleDateFormat.format(System.currentTimeMillis());
			String fullClassName = Thread.currentThread().getStackTrace()[4].getClassName();
			String className = fullClassName.substring(fullClassName.lastIndexOf(".") + 1);
			String methodName = Thread.currentThread().getStackTrace()[4].getMethodName();
			int lineNumber = Thread.currentThread().getStackTrace()[4].getLineNumber();
			String trace = className + "." + methodName + "():" + lineNumber;

			logElement.setType(type);
			logElement.setDate(date);
			logElement.setName(name);
			logElement.setMsg(msg);
			logElement.setTrace(trace);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return logElement;
	}

	// ---------------------------------------------------------------

	/**
	 * Close before delete.
	 */
	public void closeBeforeDelete() {
		for (LogGroup logGroup : logMap.values()) {
			if (logGroup.handler != null) {
				logGroup.handler.close();
				logGroup.handler = null;
				logGroup.logger = null;
			}
		}
		logMap.clear();
	}

	// ---------------------------------------------------------------

	/**
	 * Check size.
	 * @param logGroup the log group
	 */
	private void checkSize(LogGroup logGroup) {
		if (logGroup.file.length() > LOG_SIZE) {
			try {
				long bytesToClear = logGroup.file.length() - LOG_SIZE;
				deleteLineFromFile(logGroup, bytesToClear);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// ---------------------------------------------------------------

	/**
	 * Delete line from file.
	 * @param logGroup the log group
	 * @param bytesToClear the bytes to clear
	 * @return the long
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private long deleteLineFromFile(LogGroup logGroup, long bytesToClear) throws IOException {
		Path path = (Path) new Path(logGroup.file.getAbsolutePath()).removeLastSegments(1);
		File temp = new File(path.toString().concat(File.separator).concat("x"));

		// Read in a file & process line by line
		Writer output = new BufferedWriter(new FileWriter(temp));

		String newline = System.getProperty("line.separator");

		FileInputStream in = new FileInputStream(logGroup.file);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String strLine;
		long deleted = 0;

		while ((strLine = br.readLine()) != null) {
			if ((deleted > bytesToClear)) {
				// Write non deleted lines to file
				output.write(strLine);
				output.write(newline);
			} else {
				deleted += strLine.length();
			}
		}
		br.close();
		output.close();

		Formatter formatter = logGroup.handler.getFormatter();
		logGroup.handler.close();

		String absolutePath = logGroup.file.getAbsolutePath();
		File newFile = new File(absolutePath);

		FileHandler handler = new FileHandler(absolutePath, false);
		handler.setFormatter(formatter);
		logGroup.handler = handler;
		logGroup.logger.addHandler(handler);
		logGroup.file = newFile;

		return deleted;
	}
}
