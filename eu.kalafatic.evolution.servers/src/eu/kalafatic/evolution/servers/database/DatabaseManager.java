package eu.kalafatic.evolution.servers.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import eu.kalafatic.evolution.servers.security.BCryptUtils;

public class DatabaseManager {
    private static final String DB_DIR = "data";
    private static final String DB_FILE = DB_DIR + "/evolution.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Failed to load SQLite JDBC driver");
            e.printStackTrace();
        }
    }

    public DatabaseManager() {
        initialize();
    }

    private void initialize() {
        File dir = new File(DB_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (Connection conn = getConnection()) {
            createTables(conn);
            insertDefaultAdmin(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            // High concurrency SQLite optimization
            stmt.execute("PRAGMA busy_timeout = 30000;"); // Increase to 30 seconds
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
            stmt.execute("PRAGMA cache_size = -2000;");   // 2MB cache
        } catch (SQLException e) {
            System.err.println("Warning: Failed to set PRAGMAs: " + e.getMessage());
        }
        return conn;
    }

    private void createTables(Connection conn) throws SQLException {
        String usersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "enabled BOOLEAN NOT NULL DEFAULT 1," +
                "created_at TEXT NOT NULL" +
                ");";

        String sessionsTable = "CREATE TABLE IF NOT EXISTS sessions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "session_id TEXT UNIQUE NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "created_at TEXT NOT NULL," +
                "last_access TEXT NOT NULL," +
                "client_ip TEXT," +
                "workflow_type TEXT," +
                "metadata TEXT," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(sessionsTable);

            // Migration: Check if columns exist and add them if not
            try {
                stmt.execute("ALTER TABLE sessions ADD COLUMN workflow_type TEXT;");
            } catch (SQLException e) { /* Column already exists */ }
            try {
                stmt.execute("ALTER TABLE sessions ADD COLUMN metadata TEXT;");
            } catch (SQLException e) { /* Column already exists */ }
        }
    }

    private void insertDefaultAdmin(Connection conn) throws SQLException {
        String checkAdmin = "SELECT id FROM users WHERE username = 'admin'";
        try (PreparedStatement pstmt = conn.prepareStatement(checkAdmin);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                // Ensure admin is enabled and has correct password
                String updateAdmin = "UPDATE users SET password_hash = ?, enabled = ? WHERE username = 'admin'";
                try (PreparedStatement updatePstmt = conn.prepareStatement(updateAdmin)) {
                    updatePstmt.setString(1, BCryptUtils.hashPassword("admin"));
                    updatePstmt.setBoolean(2, true);
                    updatePstmt.executeUpdate();
                }
            } else {
                String insertAdmin = "INSERT INTO users (username, password_hash, role, enabled, created_at) " +
                        "VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement insertPstmt = conn.prepareStatement(insertAdmin)) {
                    insertPstmt.setString(1, "admin");
                    insertPstmt.setString(2, BCryptUtils.hashPassword("admin"));
                    insertPstmt.setString(3, "ADMIN");
                    insertPstmt.setBoolean(4, true);
                    insertPstmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    insertPstmt.executeUpdate();
                }
            }
        }
    }
}
