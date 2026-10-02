package eu.kalafatic.evolution.controller.orchestration.selfdev.net;

import java.io.File;
import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;

public class NetworkAccessManager {

    private static final NetworkAccessManager INSTANCE = new NetworkAccessManager();

    private NetworkAccessProvider provider;

    private NetworkAccessManager() {
        this.provider = detectProvider();
    }

    public static NetworkAccessManager getInstance() {
        return INSTANCE;
    }

    public synchronized void setProvider(NetworkAccessProvider provider) {
        this.provider = provider != null ? provider : detectProvider();
    }

    public NetworkAccessProvider getProvider() {
        return provider;
    }

    private NetworkAccessProvider detectProvider() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        if (osName.contains("win")) {
            return new WindowsNetworkAccessProvider();
        } else if (osName.contains("linux") || osName.contains("unix")) {
            return new LinuxNetworkAccessProvider();
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            return new MacNetworkAccessProvider();
        } else {
            return new UnsupportedNetworkAccessProvider();
        }
    }

    public NetworkAccessResult prepareNetworkAccess(File executable, int port, SelfDevContext context) {
        NetworkAccessProvider currentProvider = getProvider();
        String osName = System.getProperty("os.name", "Unknown");

        File canonicalExe = executable;
        if (executable != null) {
            try {
                canonicalExe = executable.getCanonicalFile();
            } catch (Exception ignored) {
                canonicalExe = executable.getAbsoluteFile();
            }
        }

        String execPathStr = canonicalExe != null ? canonicalExe.getAbsolutePath() : "null";
        String workDirStr = canonicalExe != null && canonicalExe.getParentFile() != null ? canonicalExe.getParentFile().getAbsolutePath() : "null";
        String endpointsStr = "127.0.0.1:" + (port > 0 ? port : "8787");

        logInfo("[NETWORK]\nPreparing network access for EVO RCP\n" +
                "OS=" + osName + "\n" +
                "Provider=" + currentProvider.getName() + "\n" +
                "Executable=" + (executable != null ? executable.getAbsolutePath() : "null") + "\n" +
                "CanonicalExecutable=" + execPathStr + "\n" +
                "WorkingDirectory=" + workDirStr + "\n" +
                "RequiredEndpoints=" + endpointsStr + "\n" +
                "Direction=INBOUND\n" +
                "Scope=LOOPBACK_LAN");

        NetworkAccessResult result = currentProvider.prepareNetworkAccess(canonicalExe, port, context);

        if (result.isSuccess()) {
            logInfo("[NETWORK]\nVerification:\n" +
                    "ruleExists=" + (result.isPermissionAlreadyPresent() || result.isPermissionCreated() || result.isPermissionUpdated()) + "\n" +
                    "enabled=true\n" +
                    "action=ALLOW\n" +
                    "ruleName=" + (result.getRuleName() != null ? result.getRuleName() : "N/A") + "\n" +
                    "Network access preparation SUCCESS: " + result.getMessage());
        } else {
            logError("[NETWORK]\nNetwork access preparation FAILED\n" +
                    "reason=" + result.getMessage() + "\n" +
                    "requiresElevation=" + result.isRequiresElevation() + "\n" +
                    "ruleName=" + (result.getRuleName() != null ? result.getRuleName() : "N/A"));
        }

        return result;
    }

    private void logInfo(String msg) {
        try {
            Log.log(msg);
        } catch (Throwable ignored) {}
        System.out.println(msg);
    }

    private void logError(String msg) {
        try {
            Log.log("[ERROR] " + msg);
        } catch (Throwable ignored) {}
        System.err.println(msg);
    }
}
