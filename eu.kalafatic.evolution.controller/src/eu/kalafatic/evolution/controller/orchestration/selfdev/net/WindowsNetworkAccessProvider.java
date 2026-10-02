package eu.kalafatic.evolution.controller.orchestration.selfdev.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
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
        String ruleName = "EVO RCP Self-Dev (" + ruleHash + ")";
        String portsStr = port > 0 ? String.valueOf(port) : "ANY";

        // Cleanup stale rules for non-existent executables periodically
        cleanupStaleRulesQuietly();

        // Check if rule already exists for this executable and rule name
        boolean ruleExists = checkRuleExists(ruleName, canonicalPath);
        if (ruleExists) {
            return new NetworkAccessResult.Builder()
                    .provider(getName())
                    .executable(canonicalExe)
                    .ports(portsStr)
                    .ruleName(ruleName)
                    .scope("LOOPBACK_LAN")
                    .direction("INBOUND")
                    .success(true)
                    .supported(true)
                    .permissionAlreadyPresent(true)
                    .message("Windows Firewall rule already present and verified for: " + canonicalPath)
                    .build();
        }

        // Rule does not exist -> create rule
        CommandResult createRes = createRule(ruleName, canonicalPath);
        if (createRes.exitCode != 0) {
            boolean requiresElevation = isElevationError(createRes.stderr + " " + createRes.stdout);
            String errMsg = "Failed to create Windows Firewall rule (exitCode=" + createRes.exitCode + "): " +
                    (createRes.stderr.trim().isEmpty() ? createRes.stdout.trim() : createRes.stderr.trim());
            return new NetworkAccessResult.Builder()
                    .provider(getName())
                    .executable(canonicalExe)
                    .ports(portsStr)
                    .ruleName(ruleName)
                    .scope("LOOPBACK_LAN")
                    .direction("INBOUND")
                    .success(false)
                    .supported(true)
                    .requiresElevation(requiresElevation)
                    .error(new RuntimeException(errMsg))
                    .message(errMsg)
                    .build();
        }

        // Verify rule exists after creation
        boolean verified = checkRuleExists(ruleName, canonicalPath);
        if (!verified) {
            return new NetworkAccessResult.Builder()
                    .provider(getName())
                    .executable(canonicalExe)
                    .ports(portsStr)
                    .ruleName(ruleName)
                    .scope("LOOPBACK_LAN")
                    .direction("INBOUND")
                    .success(false)
                    .supported(true)
                    .error(new RuntimeException("Windows Firewall rule creation command succeeded but verification failed"))
                    .message("Verification failed after rule creation command for: " + ruleName)
                    .build();
        }

        return new NetworkAccessResult.Builder()
                .provider(getName())
                .executable(canonicalExe)
                .ports(portsStr)
                .ruleName(ruleName)
                .scope("LOOPBACK_LAN")
                .direction("INBOUND")
                .success(true)
                .supported(true)
                .permissionCreated(true)
                .message("Windows Firewall rule successfully created and verified for: " + canonicalPath)
                .build();
    }

    private boolean checkRuleExists(String ruleName, String exePath) {
        // Try PowerShell Get-NetFirewallRule first
        String[] psCmd = new String[]{
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                "Get-NetFirewallRule -DisplayName '" + escapePsString(ruleName) + "' -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Name"
        };
        CommandResult res = commandRunner.runCommand(psCmd);
        if (res.exitCode == 0 && res.stdout.trim().length() > 0) {
            return true;
        }

        // Fallback to netsh
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
            return true;
        }

        return false;
    }

    private CommandResult createRule(String ruleName, String exePath) {
        // Try PowerShell New-NetFirewallRule first
        String psScript = "New-NetFirewallRule -DisplayName '" + escapePsString(ruleName) + "' " +
                "-Direction Inbound -Action Allow -Program '" + escapePsString(exePath) + "' " +
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

        // Fallback to netsh
        String[] netshCmd = new String[]{
                "netsh.exe",
                "advfirewall",
                "firewall",
                "add",
                "rule",
                "name=" + ruleName,
                "dir=in",
                "action=allow",
                "program=" + exePath,
                "enable=yes"
        };
        return commandRunner.runCommand(netshCmd);
    }

    private void cleanupStaleRulesQuietly() {
        try {
            // Find all firewall rules with DisplayName starting with "EVO RCP Self-Dev"
            String psScript = "Get-NetFirewallRule -DisplayName 'EVO RCP Self-Dev*' -ErrorAction SilentlyContinue | " +
                    "Get-NetFirewallApplicationFilter | " +
                    "Select-Object Program, @{N='RuleName';E={(Get-NetFirewallRule -AssociatedApplicationFilter $_).DisplayName}}";
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

            // Lines contain output formatted by PowerShell
            // If any program path no longer exists on disk, remove its rule
            String[] lines = res.stdout.split("\r?\n");
            for (String line : lines) {
                if (line.contains(":\\") && line.contains("evo.exe")) {
                    String pathCandidate = extractFilePath(line);
                    if (pathCandidate != null && !new File(pathCandidate).exists()) {
                        // Remove rule for missing executable
                        String removeScript = "Remove-NetFirewallRule -Program '" + escapePsString(pathCandidate) + "' -ErrorAction SilentlyContinue";
                        commandRunner.runCommand(new String[]{
                                "powershell.exe",
                                "-NoProfile",
                                "-NonInteractive",
                                "-Command",
                                removeScript
                        });
                    }
                }
            }
        } catch (Exception ignored) {
            // Ignore cleanup failures
        }
    }

    private static String extractFilePath(String line) {
        int idx = line.indexOf(":\\");
        if (idx > 0) {
            int start = idx - 1;
            int end = line.toLowerCase().indexOf(".exe", start);
            if (end > start) {
                return line.substring(start, end + 4).trim();
            }
        }
        return null;
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
