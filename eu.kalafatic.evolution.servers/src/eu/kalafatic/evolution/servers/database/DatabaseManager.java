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
            seedDefaultData(conn);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        try (Statement stmt = conn.createStatement()) {
            // High concurrency SQLite optimization
            stmt.execute("PRAGMA busy_timeout = 30000;");
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
            stmt.execute("PRAGMA cache_size = -2000;");
        } catch (SQLException e) {
            System.err.println("Warning: Failed to set PRAGMAs: " + e.getMessage());
        }
        return conn;
    }

    private void createTables(Connection conn) throws SQLException {
        String usersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "display_name TEXT," +
                "password_hash TEXT NOT NULL," +
                "role TEXT NOT NULL," +
                "enabled BOOLEAN NOT NULL DEFAULT 1," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT," +
                "last_login_at TEXT," +
                "description TEXT" +
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
                "status TEXT DEFAULT 'ACTIVE'," +
                "session_type TEXT DEFAULT 'SYSTEM'," +
                "ended_at TEXT," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");";

        String rolesTable = "CREATE TABLE IF NOT EXISTS roles (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE NOT NULL," +
                "description TEXT," +
                "system_role BOOLEAN NOT NULL DEFAULT 0," +
                "enabled BOOLEAN NOT NULL DEFAULT 1" +
                ");";

        String permissionsTable = "CREATE TABLE IF NOT EXISTS permissions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "permission_key TEXT UNIQUE NOT NULL," +
                "name TEXT NOT NULL," +
                "description TEXT," +
                "resource_type TEXT NOT NULL," +
                "action TEXT NOT NULL" +
                ");";

        String userRolesTable = "CREATE TABLE IF NOT EXISTS user_roles (" +
                "user_id INTEGER NOT NULL," +
                "role_id INTEGER NOT NULL," +
                "assigned_at TEXT NOT NULL," +
                "assigned_by INTEGER," +
                "PRIMARY KEY (user_id, role_id)," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE," +
                "FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE" +
                ");";

        String rolePermissionsTable = "CREATE TABLE IF NOT EXISTS role_permissions (" +
                "role_id INTEGER NOT NULL," +
                "permission_id INTEGER NOT NULL," +
                "assigned_at TEXT NOT NULL," +
                "assigned_by INTEGER," +
                "PRIMARY KEY (role_id, permission_id)," +
                "FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE," +
                "FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE" +
                ");";

        String resourcesTable = "CREATE TABLE IF NOT EXISTS resources (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "resource_type TEXT NOT NULL," +
                "resource_key TEXT NOT NULL," +
                "display_name TEXT NOT NULL," +
                "description TEXT," +
                "enabled BOOLEAN NOT NULL DEFAULT 1," +
                "metadata TEXT," +
                "UNIQUE(resource_type, resource_key)" +
                ");";

        String resourceAccessTable = "CREATE TABLE IF NOT EXISTS resource_access (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "principal_type TEXT NOT NULL," + // USER, ROLE
                "principal_id INTEGER NOT NULL," +
                "resource_id INTEGER NOT NULL," +
                "action TEXT NOT NULL," + // READ, EXECUTE, MANAGE
                "effect TEXT NOT NULL," + // ALLOW, DENY
                "created_at TEXT NOT NULL," +
                "created_by INTEGER," +
                "FOREIGN KEY (resource_id) REFERENCES resources(id) ON DELETE CASCADE" +
                ");";

        String auditLogTable = "CREATE TABLE IF NOT EXISTS audit_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp TEXT NOT NULL," +
                "actor_user_id INTEGER," +
                "actor_username TEXT," +
                "action TEXT NOT NULL," +
                "resource_type TEXT," +
                "resource_id TEXT," +
                "outcome TEXT NOT NULL," +
                "details TEXT" +
                ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(usersTable);
            stmt.execute(sessionsTable);
            stmt.execute(rolesTable);
            stmt.execute(permissionsTable);
            stmt.execute(userRolesTable);
            stmt.execute(rolePermissionsTable);
            stmt.execute(resourcesTable);
            stmt.execute(resourceAccessTable);
            stmt.execute(auditLogTable);
            createCognitiveTables(stmt);

            // Migrations for existing tables if needed
            try { stmt.execute("ALTER TABLE users ADD COLUMN display_name TEXT;"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN updated_at TEXT;"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN last_login_at TEXT;"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN description TEXT;"); } catch (SQLException e) {}

            try { stmt.execute("ALTER TABLE sessions ADD COLUMN workflow_type TEXT;"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE sessions ADD COLUMN metadata TEXT;"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE sessions ADD COLUMN status TEXT DEFAULT 'ACTIVE';"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE sessions ADD COLUMN session_type TEXT DEFAULT 'SYSTEM';"); } catch (SQLException e) {}
            try { stmt.execute("ALTER TABLE sessions ADD COLUMN ended_at TEXT;"); } catch (SQLException e) {}
        }
    }

    /**
     * Cognitive Services schema (additive, idempotent). FTS tables are optional:
     * if FTS5 is unavailable the virtual tables are skipped and services fall
     * back to LIKE-based search.
     */
    private void createCognitiveTables(Statement stmt) throws SQLException {
        // Phase 1: application identity and access
        stmt.execute("CREATE TABLE IF NOT EXISTS applications (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "application_id TEXT UNIQUE NOT NULL," +
                "name TEXT NOT NULL," +
                "kind TEXT," +
                "owner_user_id INTEGER," +
                "enabled BOOLEAN NOT NULL DEFAULT 1," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT," +
                "description TEXT," +
                "FOREIGN KEY (owner_user_id) REFERENCES users(id)" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS application_tokens (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "application_id INTEGER NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "token_hash TEXT UNIQUE NOT NULL," +
                "scopes_json TEXT NOT NULL," +
                "revoked BOOLEAN NOT NULL DEFAULT 0," +
                "issued_at TEXT NOT NULL," +
                "expires_at TEXT," +
                "last_used_at TEXT," +
                "description TEXT," +
                "FOREIGN KEY (application_id) REFERENCES applications(id) ON DELETE CASCADE," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");");

        // Phase 1: validation policy
        stmt.execute("CREATE TABLE IF NOT EXISTS validation_policy_config (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "rule_id TEXT UNIQUE NOT NULL," +
                "category TEXT NOT NULL," +
                "description TEXT," +
                "mode TEXT NOT NULL DEFAULT 'DISABLED'," +
                "enabled BOOLEAN NOT NULL DEFAULT 1," +
                "severity TEXT NOT NULL DEFAULT 'WARNING'," +
                "security_fixed BOOLEAN NOT NULL DEFAULT 0," +
                "default_mode TEXT NOT NULL DEFAULT 'DISABLED'," +
                "warnings INTEGER NOT NULL DEFAULT 0," +
                "errors INTEGER NOT NULL DEFAULT 0," +
                "last_result TEXT," +
                "last_check_at TEXT," +
                "updated_at TEXT NOT NULL," +
                "updated_by TEXT" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS validation_policy_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp TEXT NOT NULL," +
                "rule_id TEXT NOT NULL," +
                "category TEXT," +
                "old_mode TEXT," +
                "new_mode TEXT," +
                "changed_by TEXT," +
                "change_reason TEXT" +
                ");");

        // Phase 2: memory index and versions
        stmt.execute("CREATE TABLE IF NOT EXISTS memory_index (" +
                "entry_id TEXT PRIMARY KEY," +
                "content TEXT NOT NULL," +
                "scope TEXT," +
                "type TEXT," +
                "project_ref TEXT," +
                "user_ref TEXT," +
                "application_ref TEXT," +
                "confidence REAL," +
                "validity TEXT," +
                "updated_at INTEGER" +
                ");");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_memory_scope_type ON memory_index (scope, type);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_memory_project ON memory_index (project_ref);");

        // Phase 3: sessions, conversations, snapshots
        stmt.execute("CREATE TABLE IF NOT EXISTS cognitive_sessions (" +
                "id TEXT PRIMARY KEY," +
                "legacy_alias TEXT," +
                "kind TEXT NOT NULL DEFAULT 'CONVERSATION'," +
                "owner_user_id TEXT," +
                "application_id TEXT," +
                "project_ref TEXT," +
                "status TEXT NOT NULL DEFAULT 'ACTIVE'," +
                "started_at INTEGER NOT NULL," +
                "last_activity_at INTEGER," +
                "ended_at INTEGER," +
                "parent_session_id TEXT," +
                "metadata TEXT" +
                ");");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_cog_sessions_owner ON cognitive_sessions (owner_user_id);");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_cog_sessions_project ON cognitive_sessions (project_ref);");
        stmt.execute("CREATE TABLE IF NOT EXISTS session_relationships (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "from_session_id TEXT NOT NULL," +
                "to_session_id TEXT NOT NULL," +
                "kind TEXT NOT NULL," +
                "created_at INTEGER NOT NULL" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS session_snapshots (" +
                "id TEXT PRIMARY KEY," +
                "session_id TEXT NOT NULL," +
                "kind TEXT NOT NULL," +
                "schema_version TEXT," +
                "ref TEXT," +
                "created_at INTEGER NOT NULL," +
                "created_by TEXT" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS conversations (" +
                "id TEXT PRIMARY KEY," +
                "session_id TEXT," +
                "title TEXT," +
                "created_at INTEGER NOT NULL," +
                "file_ref TEXT" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS conversation_messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "conversation_id TEXT NOT NULL," +
                "sequence INTEGER NOT NULL," +
                "role TEXT NOT NULL," +
                "agent_type TEXT," +
                "tool_name TEXT," +
                "content TEXT NOT NULL," +
                "tokens INTEGER," +
                "timestamp INTEGER NOT NULL," +
                "metadata TEXT" +
                ");");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_conv_messages ON conversation_messages (conversation_id, sequence);");

        // Phase 4/8: preferences, profiles, project knowledge, context snapshots
        stmt.execute("CREATE TABLE IF NOT EXISTS user_profiles (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "display_name TEXT," +
                "locale TEXT," +
                "timezone TEXT," +
                "hints_json TEXT," +
                "updated_at TEXT," +
                "FOREIGN KEY (user_id) REFERENCES users(id)" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS user_preferences (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "scope TEXT NOT NULL DEFAULT 'USER'," +
                "project_ref TEXT," +
                "key TEXT NOT NULL," +
                "value_json TEXT NOT NULL," +
                "source TEXT NOT NULL DEFAULT 'EXPLICIT'," +
                "confidence REAL NOT NULL DEFAULT 1.0," +
                "status TEXT NOT NULL DEFAULT 'ACTIVE'," +
                "expires_at TEXT," +
                "updated_at TEXT NOT NULL" +
                ");");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_prefs_user ON user_preferences (user_id, scope);");
        stmt.execute("CREATE TABLE IF NOT EXISTS project_knowledge (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_key TEXT UNIQUE NOT NULL," +
                "project_name TEXT," +
                "root_path TEXT," +
                "last_scanned_at INTEGER" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS project_components (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_knowledge_id INTEGER NOT NULL," +
                "component_path TEXT," +
                "component_type TEXT," +
                "summary TEXT," +
                "tags_json TEXT," +
                "criticality TEXT" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS architectural_decisions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_knowledge_id INTEGER NOT NULL," +
                "decision_id TEXT," +
                "title TEXT," +
                "context TEXT," +
                "decision TEXT," +
                "rationale TEXT," +
                "status TEXT NOT NULL DEFAULT 'ACCEPTED'," +
                "superseded_by TEXT," +
                "created_at INTEGER," +
                "decided_at INTEGER" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS known_issues (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_knowledge_id INTEGER NOT NULL," +
                "title TEXT," +
                "description TEXT," +
                "severity TEXT," +
                "status TEXT NOT NULL DEFAULT 'OPEN'," +
                "workaround TEXT" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS implementation_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_knowledge_id INTEGER NOT NULL," +
                "summary TEXT," +
                "change_summary TEXT," +
                "commit_ref TEXT," +
                "session_id TEXT," +
                "timestamp INTEGER" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS build_results (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_knowledge_id INTEGER NOT NULL," +
                "command TEXT," +
                "success BOOLEAN NOT NULL DEFAULT 0," +
                "duration_ms INTEGER," +
                "summary TEXT," +
                "session_id TEXT," +
                "timestamp INTEGER" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS test_results (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_knowledge_id INTEGER NOT NULL," +
                "suite TEXT," +
                "run INTEGER," +
                "failures INTEGER," +
                "errors INTEGER," +
                "skipped INTEGER," +
                "pass_rate REAL," +
                "failing_tests_json TEXT," +
                "build_result_id INTEGER," +
                "timestamp INTEGER" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS repository_snapshots (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "project_knowledge_id INTEGER NOT NULL," +
                "snapshot_ref TEXT," +
                "root_path TEXT," +
                "file_count INTEGER," +
                "created_at INTEGER" +
                ");");
        stmt.execute("CREATE TABLE IF NOT EXISTS context_snapshots (" +
                "id TEXT PRIMARY KEY," +
                "context_id TEXT," +
                "schema_version TEXT," +
                "package_ref TEXT," +
                "requested_by TEXT," +
                "created_at INTEGER" +
                ");");

        // FTS indexes (optional capability)
        try {
            stmt.execute("CREATE VIRTUAL TABLE IF NOT EXISTS conversation_messages_fts USING fts5(content, content='conversation_messages', content_rowid='id');");
        } catch (SQLException e) {
            System.err.println("DatabaseManager: FTS5 unavailable for conversation_messages_fts, LIKE search fallback active: " + e.getMessage());
        }
        try {
            stmt.execute("CREATE VIRTUAL TABLE IF NOT EXISTS memory_fts USING fts5(content, content='memory_index', content_rowid='rowid');");
        } catch (SQLException e) {
            System.err.println("DatabaseManager: FTS5 unavailable for memory_fts, LIKE search fallback active: " + e.getMessage());
        }
    }

    /**
     * Whether the FTS5 extension is available for full-text search.
     */
    public boolean isFtsAvailable() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name='conversation_messages_fts'")) {
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private void seedDefaultData(Connection conn) throws SQLException {
        // 1. Seed System Roles
        String[][] defaultRoles = {
            {"ADMIN", "Full system administrative access", "1"},
            {"OPERATOR", "System operations and task execution", "1"},
            {"DEVELOPER", "Developer capabilities and process access", "1"},
            {"USER", "Standard regular user access", "1"},
            {"READ_ONLY", "Read-only inspection access", "1"}
        };

        String insertRoleSql = "INSERT OR IGNORE INTO roles (name, description, system_role, enabled) VALUES (?, ?, ?, 1)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertRoleSql)) {
            for (String[] r : defaultRoles) {
                pstmt.setString(1, r[0]);
                pstmt.setString(2, r[1]);
                pstmt.setBoolean(3, "1".equals(r[2]));
                pstmt.executeUpdate();
            }
        }

        // 2. Seed Default Core Permissions
        String[][] defaultPermissions = {
            {"users.read", "View Users", "Permission to view user accounts", "USER", "READ"},
            {"users.manage", "Manage Users", "Permission to create, edit, deactivate users", "USER", "MANAGE"},
            {"sessions.read", "View Sessions", "Permission to view active/historical sessions", "SESSION", "READ"},
            {"sessions.terminate", "Terminate Sessions", "Permission to terminate active sessions", "SESSION", "TERMINATE"},
            {"permissions.read", "View Permissions", "Permission to view roles and policies", "PERMISSION", "READ"},
            {"permissions.manage", "Manage Permissions", "Permission to update roles, permissions, access policies", "PERMISSION", "MANAGE"},
            {"mcp.tools.list", "List MCP Tools", "Permission to discover MCP tools", "MCP_TOOL", "READ"},
            {"mcp.tools.execute", "Execute MCP Tools", "Permission to execute MCP tools", "MCP_TOOL", "EXECUTE"},
            {"agents.list", "List AI Agents", "Permission to list AI agents", "AGENT", "READ"},
            {"agents.execute", "Execute AI Agents", "Permission to execute AI agents", "AGENT", "EXECUTE"},
            {"darwin.simple_chat", "Darwin Simple Chat", "Access to Darwin simple chat mode", "DARWIN_MODE", "EXECUTE"},
            {"darwin.assisted_coding", "Darwin Assisted Coding", "Access to Darwin assisted coding mode", "DARWIN_MODE", "EXECUTE"},
            {"darwin.self_dev", "Darwin Self Dev", "Access to Darwin self development mode", "DARWIN_MODE", "EXECUTE"},
            {"processes.read", "View Processes", "Permission to view EVO processes and functionalities", "PROCESS", "READ"},
            {"processes.execute", "Execute Processes", "Permission to trigger EVO processes and functionalities", "PROCESS", "EXECUTE"},
            {"configuration.read", "View System Configuration", "Permission to view system configurations", "CONFIGURATION", "READ"},
            {"configuration.manage", "Manage System Configuration", "Permission to modify system configurations", "CONFIGURATION", "MANAGE"}
        };

        String insertPermSql = "INSERT OR IGNORE INTO permissions (permission_key, name, description, resource_type, action) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertPermSql)) {
            for (String[] p : defaultPermissions) {
                pstmt.setString(1, p[0]);
                pstmt.setString(2, p[1]);
                pstmt.setString(3, p[2]);
                pstmt.setString(4, p[3]);
                pstmt.setString(5, p[4]);
                pstmt.executeUpdate();
            }
        }

        // Grant all permissions to ADMIN role
        String assignAdminPerms = "INSERT OR IGNORE INTO role_permissions (role_id, permission_id, assigned_at) " +
                "SELECT r.id, p.id, ? FROM roles r CROSS JOIN permissions p WHERE r.name = 'ADMIN'";
        try (PreparedStatement pstmt = conn.prepareStatement(assignAdminPerms)) {
            pstmt.setString(1, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            pstmt.executeUpdate();
        }

        // 3. Seed Default Admin User
        String checkAdmin = "SELECT id FROM users WHERE username = 'admin'";
        long adminUserId = -1;
        try (PreparedStatement pstmt = conn.prepareStatement(checkAdmin);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                adminUserId = rs.getLong("id");
                String updateAdmin = "UPDATE users SET password_hash = ?, role = 'ADMIN', enabled = ? WHERE username = 'admin'";
                try (PreparedStatement updatePstmt = conn.prepareStatement(updateAdmin)) {
                    updatePstmt.setString(1, BCryptUtils.hashPassword("admin"));
                    updatePstmt.setBoolean(2, true);
                    updatePstmt.executeUpdate();
                }
            } else {
                String insertAdmin = "INSERT INTO users (username, display_name, password_hash, role, enabled, created_at, description) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement insertPstmt = conn.prepareStatement(insertAdmin, Statement.RETURN_GENERATED_KEYS)) {
                    insertPstmt.setString(1, "admin");
                    insertPstmt.setString(2, "Administrator");
                    insertPstmt.setString(3, BCryptUtils.hashPassword("admin"));
                    insertPstmt.setString(4, "ADMIN");
                    insertPstmt.setBoolean(5, true);
                    insertPstmt.setString(6, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    insertPstmt.setString(7, "Default System Administrator");
                    insertPstmt.executeUpdate();
                    try (ResultSet keys = insertPstmt.getGeneratedKeys()) {
                        if (keys.next()) adminUserId = keys.getLong(1);
                    }
                }
            }
        }

        // Assign ADMIN role to admin user in user_roles
        if (adminUserId != -1) {
            String assignUserRole = "INSERT OR IGNORE INTO user_roles (user_id, role_id, assigned_at) " +
                    "SELECT ?, id, ? FROM roles WHERE name = 'ADMIN'";
            try (PreparedStatement pstmt = conn.prepareStatement(assignUserRole)) {
                pstmt.setLong(1, adminUserId);
                pstmt.setString(2, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                pstmt.executeUpdate();
            }
        }
    }
}
