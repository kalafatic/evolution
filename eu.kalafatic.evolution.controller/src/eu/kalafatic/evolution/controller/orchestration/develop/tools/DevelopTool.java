package eu.kalafatic.evolution.controller.orchestration.develop.tools;

import eu.kalafatic.evolution.controller.orchestration.develop.DevelopPermissions;
import eu.kalafatic.evolution.controller.orchestration.develop.DevelopSession;

/**
 * Base interface for coding agent tools with permission checks.
 */
public interface DevelopTool {

    String getName();

    String getDescription();

    boolean isAllowed(DevelopPermissions permissions);

    ToolResult execute(DevelopSession session, ToolInput input) throws Exception;

    class ToolInput {
        private final String payload;

        public ToolInput(String payload) {
            this.payload = payload != null ? payload : "";
        }

        public String getPayload() {
            return payload;
        }
    }

    class ToolResult {
        private final boolean success;
        private final String output;
        private final String error;

        public ToolResult(boolean success, String output, String error) {
            this.success = success;
            this.output = output != null ? output : "";
            this.error = error != null ? error : "";
        }

        public boolean isSuccess() {
            return success;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }
    }
}
