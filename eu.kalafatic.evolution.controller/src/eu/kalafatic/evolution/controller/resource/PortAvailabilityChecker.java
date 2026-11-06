package eu.kalafatic.evolution.controller.resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kalafatic.evolution.controller.log.Log;

/**
 * Socket availability verification and cross-platform process collision inspection.
 * <p>
 * EVO Collision Policy:
 * 1. Detect conflict before starting the service.
 * 2. Identify conflicting port and affected EVO service.
 * 3. Inspect conflicting process where OS permits (PID, process name, command line).
 * 4. Never automatically terminate another process.
 * 5. Never silently move EVO to another port.
 */
public class PortAvailabilityChecker {

    public static class ConflictReport {
        private final boolean inUse;
        private final int port;
        private final String affectedService;
        private final Long processId;
        private final String processName;
        private final String commandLine;
        private final String diagnosticMessage;

        public ConflictReport(boolean inUse, int port, String affectedService, Long processId,
                              String processName, String commandLine, String diagnosticMessage) {
            this.inUse = inUse;
            this.port = port;
            this.affectedService = affectedService;
            this.processId = processId;
            this.processName = processName;
            this.commandLine = commandLine;
            this.diagnosticMessage = diagnosticMessage;
        }

        public boolean isInUse() {
            return inUse;
        }

        public int getPort() {
            return port;
        }

        public String getAffectedService() {
            return affectedService;
        }

        public Optional<Long> getProcessId() {
            return Optional.ofNullable(processId);
        }

        public Optional<String> getProcessName() {
            return Optional.ofNullable(processName);
        }

        public Optional<String> getCommandLine() {
            return Optional.ofNullable(commandLine);
        }

        public String getDiagnosticMessage() {
            return diagnosticMessage;
        }

        @Override
        public String toString() {
            if (!inUse) {
                return "Port " + port + " is available for " + affectedService;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("PORT CONFLICT DETECTED on port ").append(port)
              .append(" for service [").append(affectedService).append("]. ");
            if (processId != null) {
                sb.append("Occupied by PID: ").append(processId);
                if (processName != null && !processName.isEmpty()) {
                    sb.append(" (").append(processName).append(")");
                }
                if (commandLine != null && !commandLine.isEmpty()) {
                    sb.append(" Command: ").append(commandLine);
                }
            } else {
                sb.append("Process details unavailable or restricted by OS.");
            }
            sb.append("\nRemedy: Stop or reconfigure the conflicting process, or free port ").append(port)
              .append(" before launching EVO. EVO ports are deterministic and will not silently shift.");
            return sb.toString();
        }
    }

    /**
     * Checks if a port is currently available on the local interface (127.0.0.1 and wildcard).
     *
     * @param port The port number to test.
     * @return true if socket can be bound, false if occupied.
     */
    public static boolean isPortAvailable(int port) {
        if (port < 1 || port > 65535) {
            return false;
        }
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress("127.0.0.1", port));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Performs a comprehensive conflict check and diagnoses the occupying process if unavailable.
     *
     * @param port The port to check.
     * @param serviceName The affected EVO service name.
     * @return Detailed ConflictReport.
     */
    public static ConflictReport inspectPort(int port, String serviceName) {
        if (isPortAvailable(port)) {
            return new ConflictReport(false, port, serviceName, null, null, null, "Port is available");
        }

        // Port is occupied - inspect OS process details
        Long pid = null;
        String pName = null;
        String cmdLine = null;

        try {
            pid = discoverProcessIdUsingPort(port);
            if (pid != null && pid > 0) {
                Optional<ProcessHandle> handle = ProcessHandle.of(pid);
                if (handle.isPresent()) {
                    ProcessHandle.Info info = handle.get().info();
                    pName = info.command().orElse(null);
                    cmdLine = info.commandLine().orElse(null);
                }
            }
        } catch (Exception e) {
            Log.log("[PortAvailabilityChecker] Warning: Failed to query process details for port " + port + ": " + e.getMessage());
        }

        String message = "Port " + port + " occupied for " + serviceName + (pid != null ? " by PID " + pid : "");
        return new ConflictReport(true, port, serviceName, pid, pName, cmdLine, message);
    }

    /**
     * Attempts to find the PID using the port across Windows, Linux, and macOS.
     */
    public static Long discoverProcessIdUsingPort(int port) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return findPidOnWindows(port);
        } else {
            return findPidOnUnix(port);
        }
    }

    private static Long findPidOnWindows(int port) {
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", "netstat -ano | findstr :" + port).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                Pattern pattern = Pattern.compile("LISTENING\\s+(\\d+)");
                while ((line = reader.readLine()) != null) {
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        return Long.parseLong(m.group(1));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Long findPidOnUnix(int port) {
        // Try lsof first
        try {
            Process process = new ProcessBuilder("lsof", "-t", "-i:" + port).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    return Long.parseLong(line.trim());
                }
            }
        } catch (Exception ignored) {
        }

        // Try fuser as fallback
        try {
            Process process = new ProcessBuilder("fuser", port + "/tcp").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    String clean = line.trim().replaceAll("[^0-9]", "");
                    if (!clean.isEmpty()) {
                        return Long.parseLong(clean);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Try ss fallback
        try {
            Process process = new ProcessBuilder("ss", "-lptn", "sport = :" + port).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                Pattern pattern = Pattern.compile("pid=(\\d+)");
                while ((line = reader.readLine()) != null) {
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        return Long.parseLong(m.group(1));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
