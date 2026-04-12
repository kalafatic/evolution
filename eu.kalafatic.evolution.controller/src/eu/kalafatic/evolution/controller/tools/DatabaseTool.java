package eu.kalafatic.evolution.controller.tools;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import eu.kalafatic.evolution.model.orchestration.Database;

/**
 * Tool for database operations and connectivity testing.
 */
public class DatabaseTool implements ITool {
    @Override
    public String getName() {
        return "DatabaseTool";
    }

    @Override
    public String execute(String command, File workingDir, TaskContext context) throws Exception {
        Database dbSettings = context.getOrchestrator().getDatabase();
        if (dbSettings == null) {
            throw new Exception("Database settings not configured in the orchestrator.");
        }

        if ("TEST_CONNECTION".equalsIgnoreCase(command)) {
            String url = dbSettings.getUrl();
            String user = dbSettings.getUsername();
            String password = dbSettings.getPassword();
            String driver = dbSettings.getDriver();

            if (url == null || url.isEmpty()) {
                throw new Exception("Database URL is missing.");
            }

            if (driver != null && !driver.isEmpty()) {
                try {
                    Class.forName(driver);
                } catch (ClassNotFoundException e) {
                    throw new Exception("Database driver class not found: " + driver);
                }
            }

            try (Connection conn = DriverManager.getConnection(url, user, password)) {
                if (conn != null && !conn.isClosed()) {
                    return "SUCCESS: Connected to " + url;
                } else {
                    throw new Exception("Failed to establish connection to " + url);
                }
            } catch (Exception e) {
                throw new Exception("Database connection failed: " + e.getMessage(), e);
            }
        }

        throw new Exception("Unsupported command for DatabaseTool: " + command);
    }
}
