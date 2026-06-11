# PACKAGE CONTEXT

## Directory: git/evolution/eu.kalafatic.utils/src/eu/kalafatic/utils/db/

## Domain: general

## Components
* `DBUtils.java`: package eu.kalafatic.utils.db; import java.io.Serializable; import java.sql.Connection; import java.sql.DriverManager; import java.sql.ResultSet; import java.sql.SQLException; import java.sql.Statement; import java.util.ArrayList; import java.util.LinkedHashMap; import java.util.List; import java.util.Map; import eu.kalafatic.utils.dialogs.DialogUtils; @SuppressWarnings("serial") public class DBUtils implements Serializable { public static final String[] DB_URL_PARAMETERS = { "Connector", "Driver", "Host", "Port", "Database", "User", "Password" }; public static final String[] DB_TORRENT_CATEGORIES = new String[] { "Video", "Audio", "Other" }; public static enum EDBTorrents {
