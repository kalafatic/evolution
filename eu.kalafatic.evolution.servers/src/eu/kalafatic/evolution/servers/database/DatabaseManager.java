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
        return DriverManager.getConnection(URL);
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
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(sessionsTable);
        }
    }

    private void insertDefaultAdmin(Connection conn) throws SQLException {
        String checkAdmin = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
        try (PreparedStatement pstmt = conn.prepareStatement(checkAdmin);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next() && rs.getInt(1) == 0) {
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
