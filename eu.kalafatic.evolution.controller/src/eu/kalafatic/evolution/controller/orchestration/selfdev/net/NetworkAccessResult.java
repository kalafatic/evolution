package eu.kalafatic.evolution.controller.orchestration.selfdev.net;

import java.io.File;

public class NetworkAccessResult {
    private final boolean success;
    private final boolean supported;
    private final boolean permissionAlreadyPresent;
    private final boolean permissionCreated;
    private final boolean permissionUpdated;
    private final boolean requiresElevation;
    private final boolean skipped;
    private final Throwable error;
    private final String provider;
    private final File executable;
    private final String ports;
    private final String scope;
    private final String direction;
    private final String ruleName;
    private final String message;

    private NetworkAccessResult(Builder builder) {
        this.success = builder.success;
        this.supported = builder.supported;
        this.permissionAlreadyPresent = builder.permissionAlreadyPresent;
        this.permissionCreated = builder.permissionCreated;
        this.permissionUpdated = builder.permissionUpdated;
        this.requiresElevation = builder.requiresElevation;
        this.skipped = builder.skipped;
        this.error = builder.error;
        this.provider = builder.provider;
        this.executable = builder.executable;
        this.ports = builder.ports;
        this.scope = builder.scope;
        this.direction = builder.direction;
        this.ruleName = builder.ruleName;
        this.message = builder.message;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isSupported() {
        return supported;
    }

    public boolean isPermissionAlreadyPresent() {
        return permissionAlreadyPresent;
    }

    public boolean isPermissionCreated() {
        return permissionCreated;
    }

    public boolean isPermissionUpdated() {
        return permissionUpdated;
    }

    public boolean isRequiresElevation() {
        return requiresElevation;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public Throwable getError() {
        return error;
    }

    public String getProvider() {
        return provider;
    }

    public File getExecutable() {
        return executable;
    }

    public String getPorts() {
        return ports;
    }

    public String getScope() {
        return scope;
    }

    public String getDirection() {
        return direction;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "NetworkAccessResult{" +
                "success=" + success +
                ", supported=" + supported +
                ", permissionAlreadyPresent=" + permissionAlreadyPresent +
                ", permissionCreated=" + permissionCreated +
                ", permissionUpdated=" + permissionUpdated +
                ", requiresElevation=" + requiresElevation +
                ", skipped=" + skipped +
                ", provider='" + provider + '\'' +
                ", executable=" + (executable != null ? executable.getAbsolutePath() : "null") +
                ", ports='" + ports + '\'' +
                ", scope='" + scope + '\'' +
                ", direction='" + direction + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", message='" + message + '\'' +
                '}';
    }

    public static class Builder {
        private boolean success = true;
        private boolean supported = true;
        private boolean permissionAlreadyPresent = false;
        private boolean permissionCreated = false;
        private boolean permissionUpdated = false;
        private boolean requiresElevation = false;
        private boolean skipped = false;
        private Throwable error;
        private String provider;
        private File executable;
        private String ports;
        private String scope = "LOOPBACK";
        private String direction = "INBOUND";
        private String ruleName;
        private String message;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder supported(boolean supported) {
            this.supported = supported;
            return this;
        }

        public Builder permissionAlreadyPresent(boolean permissionAlreadyPresent) {
            this.permissionAlreadyPresent = permissionAlreadyPresent;
            return this;
        }

        public Builder permissionCreated(boolean permissionCreated) {
            this.permissionCreated = permissionCreated;
            return this;
        }

        public Builder permissionUpdated(boolean permissionUpdated) {
            this.permissionUpdated = permissionUpdated;
            return this;
        }

        public Builder requiresElevation(boolean requiresElevation) {
            this.requiresElevation = requiresElevation;
            return this;
        }

        public Builder skipped(boolean skipped) {
            this.skipped = skipped;
            return this;
        }

        public Builder error(Throwable error) {
            this.error = error;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder executable(File executable) {
            this.executable = executable;
            return this;
        }

        public Builder ports(String ports) {
            this.ports = ports;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder direction(String direction) {
            this.direction = direction;
            return this;
        }

        public Builder ruleName(String ruleName) {
            this.ruleName = ruleName;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public NetworkAccessResult build() {
            return new NetworkAccessResult(this);
        }
    }
}
