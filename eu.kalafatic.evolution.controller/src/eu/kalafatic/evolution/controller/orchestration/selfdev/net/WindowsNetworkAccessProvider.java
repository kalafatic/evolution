package eu.kalafatic.evolution.controller.orchestration.selfdev.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;

public class WindowsNetworkAccessProvider implements NetworkAccessProvider {

    public interface CommandRunner {
        CommandResult runCommand(String[] command);
    }

    public static class CommandResult {
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        public CommandResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout != null ? stdout : "";
            this.stderr = stderr != null ? stderr : "";
        }
    }

    private final CommandRunner commandRunner;

    public WindowsNetworkAccessProvider() {
        this(new DefaultCommandRunner());
    }

    public WindowsNetworkAccessProvider(CommandRunner commandRunner) {
        this.commandRunner = commandRunner != null ? commandRunner : new DefaultCommandRunner();
    }

    @Override
    public String getName() {
        return "WindowsNetworkAccessProvider";
    }

    @Override
    public NetworkAccessResult prepareNetworkAccess(File executable, int port, SelfDevContext context) {
        if (executable == null || !executable.exists()) {
            return new NetworkAccessResult.Builder()
                    .provider(getName())
                    .executable(executable)
                    .success(false)
                    .message("Executable file does not exist")
                    .build();
        }

        File canonicalExe;
        try {
            canonicalExe = executable.getCanonicalFile();
        } catch (Exception e) {
            canonicalExe = executable.getAbsoluteFile();
        }

        String canonicalPath = canonicalExe.getAbsolutePath();
        String ruleHash = computeHash(canonicalPath);
        String inboundRuleName = "EVO-SelfDev-Inbound-" + ruleHash;
        String outboundRuleName = "EVO-SelfDev-Outbound-" + ruleHash;
        String portsStr = port > 0 ? String.valueOf(port) : "ANY";

        // Cleanup stale rules for non-existent executables periodically
        cleanupStaleRulesQuietly();

        // Ensure executable file permission mode
        ensureExecutablePermission(canonicalExe);

        // Check if both Inbound and Outbound rules exist and are verified for this executable
        boolean inboundExists = checkRuleExists(inboundRuleName, "Inbound", canonicalPath);
        boolean outboundExists = checkRuleExists(outboundRuleName, "Outbound", canonicalPath);

        if (inboundExists && outboundExists) {
            return new NetworkAccessResult.Builder()
                    .provider(getName())
                    .executable(canonicalExe)
                    .ports(portsStr)
                    .ruleName(inboundRuleName + ";" + outboundRuleName)
                    .scope("LOOPBACK_LAN")
                    .direction("INBOUND_OUTBOUND")
                    .success(true)
                    .supported(true)
                    .permissionAlreadyPresent(true)
                    .message("Windows Firewall Inbound and Outbound rules verified for: " + canonicalPath)
                    .build();
        }

        // Create Inbound rule if missing
        if (!inboundExists) {
            CommandResult createRes = createRule(inboundRuleName, "EVO RCP Self-Dev Inbound (" + ruleHash + ")", "Inbound", canonicalPath);
            if (createRes.exitCode != 0) {
                boolean requiresElevation = isElevationError(createRes.stderr + " " + createRes.stdout);
                String errMsg = "Failed to create Windows Firewall Inbound rule (exitCode=" + createRes.exitCode + "): " +
                        (createRes.stderr.trim().isEmpty() ? createRes.stdout.trim() : createRes.stderr.trim());

                return new NetworkAccessResult.Builder()
                        .provider(getName())
                        .executable(canonicalExe)
                        .ports(portsStr)
                        .ruleName(inboundRuleName)
                        .scope("LOOPBACK_LAN")
                        .direction("INBOUND")
                        .success(false)
                        .supported(true)
                        .requiresElevation(requiresElevation)
                        .error(new RuntimeException(errMsg))
                        .message("Windows Firewall rule creation failed due to " + (requiresElevation ? "administrator elevation requirement" : "command failure") + ": " + errMsg)
                        .build();
            }
        }

        // Create Outbound rule if missing
        if (!outboundExists) {
            CommandResult createRes = createRule(outboundRuleName, "EVO RCP Self-Dev Outbound (" + ruleHash + ")", "Outbound", canonicalPath);
            if (createRes.exitCode != 0) {
                boolean requiresElevation = isElevationError(createRes.stderr + " " + createRes.stdout);
                String errMsg = "Failed to create Windows Firewall Outbound rule (exitCode=" + createRes.exitCode + "): " +
                        (createRes.stderr.trim().isEmpty() ? createRes.stdout.trim() : createRes.stderr.trim());

                return new NetworkAccessResult.Builder()
                        .provider(getName())
                        .executable(canonicalExe)
                        .ports(portsStr)
                        .ruleName(outboundRuleName)
                        .scope("LOOPBACK_LAN")
                        .direction("OUTBOUND")
                        .success(false)
                        .supported(true)
                        .requiresElevation(requiresElevation)
                        .error(new RuntimeException(errMsg))
                        .message("Windows Firewall Outbound rule creation failed due to " + (requiresElevation ? "administrator elevation requirement" : "command failure") + ": " + errMsg)
                        .build();
            }
        }

        // Verify both rules exist post-creation
        boolean inboundVerified = checkRuleExists(inboundRuleName, "Inbound", canonicalPath);
        boolean outboundVerified = checkRuleExists(outboundRuleName, "Outbound", canonicalPath);

        if (!inboundVerified || !outboundVerified) {
            return new NetworkAccessResult.Builder()
                    .provider(getName())
                    .executable(canonicalExe)
                    .ports(portsStr)
                    .ruleName(inboundRuleName + ";" + outboundRuleName)
                    .scope("LOOPBACK_LAN")
                    .direction("INBOUND_OUTBOUND")
                    .success(false)
                    .supported(true)
                    .error(new RuntimeException("Windows Firewall rule creation commands succeeded but post-creation property verification failed"))
                    .message("Verification failed after Windows Firewall rule creation for: " + canonicalPath)
                    .build();
        }

        return new NetworkAccessResult.Builder()
                .provider(getName())
                .executable(canonicalExe)
                .ports(portsStr)
                .ruleName(inboundRuleName + ";" + outboundRuleName)
                .scope("LOOPBACK_LAN")
                .direction("INBOUND_OUTBOUND")
                .success(true)
                .supported(true)
                .permissionCreated(true)
                .message("Windows Firewall Inbound and Outbound rules successfully created and verified for: " + canonicalPath)
                .build();
    }

    private boolean checkRuleExists(String ruleName, String expectedDirection, String exePath) {
        String psScript = "Get-NetFirewallRule -Name '" + escapePsString(ruleName) + "' -ErrorAction SilentlyContinue | ForEach-Object { " +
                "$app = $_ | Get-NetFirewallApplicationFilter; " +
                "[PSCustomObject]@{ Name=$_.Name; Enabled=[string]$_.Enabled; Action=[string]$_.Action; Direction=[string]$_.Direction; Program=$app.Program } " +
                "} | ConvertTo-Json -Compress";

        String[] psCmd = new String[]{
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                psScript
        };
        CommandResult res = commandRunner.runCommand(psCmd);
        if (res.exitCode == 0 && !res.stdout.trim().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(res.stdout.trim());
                if (root.isArray() && root.size() > 0) {
                    root = root.get(0);
                }
                String name = root.has("Name") ? root.get("Name").asText() : "";
                String enabled = root.has("Enabled") ? root.get("Enabled").asText() : "";
                String action = root.has("Action") ? root.get("Action").asText() : "";
                String direction = root.has("Direction") ? root.get("Direction").asText() : "";
                String program = root.has("Program") ? root.get("Program").asText() : "";

                boolean nameMatches = name.equalsIgnoreCase(ruleName);
                boolean enabledMatches = "True".equalsIgnoreCase(enabled) || "1".equals(enabled);
                boolean actionMatches = "Allow".equalsIgnoreCase(action) || "2".equals(action);
                boolean dirMatches = direction.equalsIgnoreCase(expectedDirection) || direction.contains(expectedDirection);
                boolean progMatches = program.equalsIgnoreCase(exePath) || sameFile(program, exePath);

                if (nameMatches && enabledMatches && actionMatches && dirMatches && progMatches) {
                    return true;
                }
            } catch (Exception ignored) {
                // Fallback to substring match if JSON parsing fails on older shell output
                if (res.stdout.contains(ruleName) && res.stdout.contains(exePath)) {
                    return true;
                }
            }
        }

        // Netsh fallback
        String[] netshCmd = new String[]{
                "netsh.exe",
                "advfirewall",
                "firewall",
                "show",
                "rule",
                "name=" + ruleName
        };
        CommandResult netshRes = commandRunner.runCommand(netshCmd);
        if (netshRes.exitCode == 0 && !netshRes.stdout.contains("No rules match")) {
            String out = netshRes.stdout;
            boolean enabledOk = out.contains("Enabled:") && out.contains("Yes");
            boolean actionOk = out.contains("Action:") && out.contains("Allow");
            boolean progOk = out.toLowerCase().contains(exePath.toLowerCase());
            return enabledOk && actionOk && progOk;
        }

        return false;
    }

    private CommandResult createRule(String ruleName, String displayName, String direction, String exePath) {
        String psScript = "New-NetFirewallRule -Name '" + escapePsString(ruleName) + "' " +
                "-DisplayName '" + escapePsString(displayName) + "' " +
                "-Direction " + direction + " -Action Allow -Program '" + escapePsString(exePath) + "' " +
                "-Profile Any -Enabled True -ErrorAction Stop";
        String[] psCmd = new String[]{
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                psScript
        };
        CommandResult res = commandRunner.runCommand(psCmd);
        if (res.exitCode == 0) {
            return res;
        }

        String netshDir = "Inbound".equalsIgnoreCase(direction) ? "in" : "out";
        String[] netshCmd = new String[]{
                "netsh.exe",
                "advfirewall",
                "firewall",
                "add",
                "rule",
                "name=" + ruleName,
                "dir=" + netshDir,
                "action=allow",
                "program=" + exePath,
                "enable=yes"
        };
        return commandRunner.runCommand(netshCmd);
    }

    private void cleanupStaleRulesQuietly() {
        try {
            String psScript = "Get-NetFirewallRule -Name 'EVO-SelfDev-*' -ErrorAction SilentlyContinue | ForEach-Object { " +
                    "$app = $_ | Get-NetFirewallApplicationFilter; " +
                    "[PSCustomObject]@{ Name=$_.Name; Program=$app.Program } " +
                    "} | ConvertTo-Json -Compress";

            String[] psCmd = new String[]{
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-Command",
                    psScript
            };
            CommandResult res = commandRunner.runCommand(psCmd);
            if (res.exitCode != 0 || res.stdout.trim().isEmpty()) {
                return;
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(res.stdout.trim());
            List<JsonNode> nodes = new ArrayList<>();
            if (root.isArray()) {
                root.forEach(nodes::add);
            } else if (root.isObject()) {
                nodes.add(root);
            }

            for (JsonNode node : nodes) {
                String name = node.has("Name") ? node.get("Name").asText() : "";
                String program = node.has("Program") ? node.get("Program").asText() : "";

                if (!name.isEmpty() && !program.isEmpty()) {
                    File progFile = new File(program);
                    if (!progFile.exists()) {
                        String removeScript = "Remove-NetFirewallRule -Name '" + escapePsString(name) + "' -ErrorAction SilentlyContinue";
                        commandRunner.runCommand(new String[]{
                                "powershell.exe",
                                "-NoProfile",
                                "-NonInteractive",
                                "-Command",
                                removeScript
                        });
                        commandRunner.runCommand(new String[]{
                                "netsh.exe",
                                "advfirewall",
                                "firewall",
                                "delete",
                                "rule",
                                "name=" + name
                        });
                    }
                }
            }
        } catch (Exception ignored) {
            // Quietly ignore cleanup errors
        }
    }

    private static boolean sameFile(String path1, String path2) {
        if (path1 == null || path2 == null) return false;
        try {
            return new File(path1).getCanonicalPath().equalsIgnoreCase(new File(path2).getCanonicalPath());
        } catch (Exception e) {
            return path1.equalsIgnoreCase(path2);
        }
    }

    private static void ensureExecutablePermission(File executable) {
        if (executable == null || !executable.exists()) return;
        try {
            if (!executable.canExecute()) {
                executable.setExecutable(true, false);
            }
        } catch (Exception ignored) {}
    }

    private static boolean isElevationError(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        return lower.contains("access is denied") ||
                lower.contains("run as administrator") ||
                lower.contains("requires elevation") ||
                lower.contains("administrator privileges") ||
                lower.contains("requested operation requires elevation");
    }

    private static String escapePsString(String str) {
        if (str == null) return "";
        return str.replace("'", "''");
    }

    private static String computeHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) { // Use first 6 bytes (12 hex chars) for concise stable identifier
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(Math.abs(input.hashCode()));
        }
    }

    public static class DefaultCommandRunner implements CommandRunner {
        @Override
        public CommandResult runCommand(String[] command) {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                Process proc = pb.start();

                StringBuilder stdout = new StringBuilder();
                StringBuilder stderr = new StringBuilder();

                Thread stdoutThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stdout.append(line).append("\n");
                        }
                    } catch (Exception ignored) {}
                });

                Thread stderrThread = new Thread(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            stderr.append(line).append("\n");
                        }
                    } catch (Exception ignored) {}
                });

                stdoutThread.start();
                stderrThread.start();

                boolean finished = proc.waitFor(15, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) {
                    proc.destroyForcibly();
                    return new CommandResult(-1, stdout.toString(), "Command timed out after 15 seconds");
                }

                stdoutThread.join(2000);
                stderrThread.join(2000);

                return new CommandResult(proc.exitValue(), stdout.toString(), stderr.toString());
            } catch (Exception e) {
                return new CommandResult(-1, "", e.getMessage());
            }
        }
    }
}
