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
package eu.kalafatic.utils.db;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import eu.kalafatic.utils.dialogs.DialogUtils;

/**
 * The Class class DBUtils.
 * @author Petr Kalafatic
 * @project Gemini
 * @version 3.0.0
 */

@SuppressWarnings("serial")
public class DBUtils implements Serializable {

	/** The Constant DB_URL_PARAMETERS. */
	public static final String[] DB_URL_PARAMETERS = { "Connector", "Driver", "Host", "Port", "Database", "User", "Password" };

	/** The Constant DB_TORRENT_CATEGORIES. */
	public static final String[] DB_TORRENT_CATEGORIES = new String[] { "Video", "Audio", "Other" };

	/**
	 * The Enum enum EDBTorrents.
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	public static enum EDBTorrents {

		/** The CATEGORY. */
		CATEGORY(0, "category"),

		/** The NAME. */
		NAME(1, "name"),

		/** The DATE. */
		DATE(2, "date"),

		/** The SIZE. */
		SIZE(3, "size"),

		/** The SEEDS. */
		SEEDS(4, "seeds"),

		/** The PEERS. */
		PEERS(5, "peers"),

		/** The CONTENT. */
		CONTENT(6, "content");

		/** The column index. */
		public int columnIndex;

		/** The column name. */
		public String columnName;

		/**
		 * Instantiates a new eDB torrents.
		 * @param columnIndex the column index
		 * @param columnName the column name
		 */
		private EDBTorrents(int columnIndex, String columnName) {
			this.columnIndex = columnIndex;
			this.columnName = columnName;
		}
	}

	/** The INSTANCE. */
	private volatile static DBUtils INSTANCE;

	/**
	 * Gets the single instance of DBUtils.
	 * @return single instance of DBUtils
	 */
	public static DBUtils getInstance() {
		if (INSTANCE == null) {
			synchronized (DBUtils.class) {
				INSTANCE = new DBUtils();
			}
		}
		return INSTANCE;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * Execute.
	 * @param connection the connection
	 * @param query the query
	 * @param col the col
	 * @return the list
	 */
	public List<String> execute(Connection connection, String query, int col) {
		List<String> result = new ArrayList<String>();
		try {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			while (resultSet.next()) {
				for (int i = 1; i <= col; i++) {
					result.add(resultSet.getString(i));
				}
			}
		} catch (SQLException e) {
			DialogUtils.INSTANCE.showException(e);
		}
		return result;
	}

	// ---------------------------------------------------------------

	/**
	 * Checks if is unique.
	 * @param db the db
	 * @param dbName the db name
	 * @param colName the col name
	 * @param key the key
	 * @return true, if is unique
	 */
	public boolean isUnique(DB db, String dbName, String colName, String key) {
		boolean result = false;
		try {
			String query = "select count(" + colName + ") from " + dbName + " where " + colName + " like \"" + key + "\"";

			Class.forName(db.driver);
			Connection connection = DriverManager.getConnection(db.url);
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(query);
			resultSet.last();

			int rows = resultSet.getInt("count(" + colName + ")");
			result = rows > 0 ? false : true;

			connection.close();

		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
		return result;
	}

	// ---------------------------------------------------------------

	/**
	 * Checks if is available.
	 * @param db the db
	 * @return true, if is available
	 */
	public boolean isAvailable(DB db) {
		try {
			Class.forName(db.driver);
			Connection connection = DriverManager.getConnection(db.url);
			connection.close();
			DialogUtils.INSTANCE.info("Connection OK");
			return db.active = true;

		} catch (Exception e) {
			DialogUtils.INSTANCE.showException(e);
		}
		return db.active = false;
	}

	// ---------------------------------------------------------------

	/**
	 * Contains.
	 * @param list the list
	 * @param db the db
	 * @return true, if successful
	 */
	public boolean contains(List<DB> list, DB db) {
		try {
			for (DB element : list) {
				if (element.settings.get(DB_URL_PARAMETERS[4]).equals(db.settings.get(DB_URL_PARAMETERS[4]))) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	// ---------------------------------------------------------------
	// ---------------------------------------------------------------

	/**
	 * The Class class DB.
	 * @author Petr Kalafatic
	 * @project Gemini
	 * @version 3.0.0
	 */
	public class DB implements Serializable {

		/** The active. */
		public boolean active = true;

		/** The query. */
		public String query = "";

		/** The url. */
		public String url;

		/** The columns. */
		public int columns;

		/** The driver. */
		public String driver = "com.mysql.jdbc.Driver";

		/** The settings. */
		public Map<String, String> settings = new LinkedHashMap<String, String>() {
			{
				put(DB_URL_PARAMETERS[0], "jdbc:mysql");
				put(DB_URL_PARAMETERS[1], driver);
				// put(DB_URL_PARAMETERS[2], "kalafatic.eu");
				put(DB_URL_PARAMETERS[2], "localhost");
				put(DB_URL_PARAMETERS[3], "3306");
				put(DB_URL_PARAMETERS[4], "torrents");
				put(DB_URL_PARAMETERS[5], "reader");
				// put(DB_URL_PARAMETERS[6], "");
				put(DB_URL_PARAMETERS[6], "writer");
			}
		};

		/**
		 * Instantiates a new dB.
		 */
		public DB() {}

		/**
		 * Instantiates a new dB.
		 * @param query the query
		 */
		public DB(String query) {
			parseQuery(query);
		}

		/**
		 * Parses the query.
		 * @param query the query
		 */
		public void parseQuery(String query) {
			try {
				String[] split = query.split("-");
				this.columns = Integer.parseInt(split[0]);
				this.query = split[1];
			} catch (Exception e) {
				// TODO: handle exception
			}
		}

		/**
		 * Parses the url.
		 * @return the string
		 */
		public String parseURL() {
			return this.url = settings.get(DB_URL_PARAMETERS[0]) + "://" + settings.get(DB_URL_PARAMETERS[2]) + ":" + settings.get(DB_URL_PARAMETERS[3]) + "/" + settings.get(DB_URL_PARAMETERS[4])
					+ "?user=" + settings.get(DB_URL_PARAMETERS[5]) + "&password=" + settings.get(DB_URL_PARAMETERS[6]);
		}

		/**
		 * Settings as array.
		 * @return the string[]
		 */
		public String[] settingsAsArray() {
			return settings.values().toArray(new String[settings.size()]);
		}
	}

}
