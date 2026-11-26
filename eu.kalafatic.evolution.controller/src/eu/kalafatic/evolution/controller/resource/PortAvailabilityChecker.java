package eu.kalafatic.evolution.controller.resource;

import java.io.BufferedReader;
import java.io.IOException;
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
                sb.append("Occupant process not accessible from the current user account")
                  .append(" (it may belong to root or another user; client connections are")
                  .append(" never reported as occupants).");
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

        String message = "Port " + port + " occupied for " + serviceName
                + (pid != null ? " by PID " + pid : " (LISTEN socket detected, owner not accessible to this user)");
        return new ConflictReport(true, port, serviceName, pid, pName, cmdLine, message);
    }

    /**
     * Attempts to find the PID using the port across Windows, Linux, and macOS.
     * Only LISTEN sockets are ever considered: client connections must never be
     * misattributed as the occupant.
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
        // 1. Authoritative and root-free: the kernel socket table. Parsing
        //    /proc/net/tcp{,6} never requires elevated privileges and only
        //    LISTEN (st=0A) sockets are matched, so clients of the service
        //    (e.g. a browser tab) can never be misattributed as the occupant.
        //    The socket inode is resolved to a PID via /proc/<pid>/fd; for
        //    sockets owned by another user (e.g. root) the PID stays unknown
        //    and the report states that honestly instead of blaming a client.
        String inode = findListenSocketInode(port);
        if (inode != null) {
            Long pid = findPidBySocketInode(inode);
            if (pid != null) {
                return pid;
            }
        }

        // 2. lsof with an explicit LISTEN filter (best effort).
        try {
            Process process = new ProcessBuilder("lsof", "-t", "-iTCP:" + port, "-sTCP:LISTEN").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        try {
                            return Long.parseLong(line);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // 3. ss with LISTEN filtering (pid= appears only for readable processes).
        try {
            Process process = new ProcessBuilder("ss", "-lptn", "sport = :" + port).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                Pattern pattern = Pattern.compile("pid=(\\d+)");
                while ((line = reader.readLine()) != null) {
                    if (!line.contains("LISTEN")) continue;
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        return Long.parseLong(m.group(1));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Deliberately no unfiltered lsof/fuser fallback: those report every
        // process with ANY connection on the port (including plain clients),
        // which previously caused false conflict attributions (e.g. Firefox
        // was reported as the occupant of a port that a root-owned service
        // was actually listening on).
        return null;
    }

    /** Returns the socket inode of a LISTEN socket on the given port, or null. */
    private static String findListenSocketInode(int port) {
        for (String table : new String[]{"/proc/net/tcp", "/proc/net/tcp6"}) {
            try (BufferedReader reader = java.nio.file.Files.newBufferedReader(java.nio.file.Paths.get(table))) {
                String line = reader.readLine(); // header
                while ((line = reader.readLine()) != null) {
                    String[] f = line.trim().split("\\s+");
                    // Canonical row: sl local_address rem_address st tx rx tr uid timeout inode ...
                    // (older kernels list retrnsmt/tm columns; the inode is at index 9 in
                    //  the stable layout used by the kernel ABI documentation.)
                    if (f.length < 10) continue;
                    if (!"0A".equalsIgnoreCase(f[3])) continue; // LISTEN only
                    String local = f[1];
                    int idx = local.lastIndexOf(':');
                    if (idx < 0) continue;
                    try {
                        if (Integer.parseInt(local.substring(idx + 1), 16) == port) {
                            return f[9];
                        }
                    } catch (NumberFormatException ignored) {}
                }
            } catch (IOException ignored) {
            }
        }
        return null;
    }

    /** Resolves a socket inode to the owning PID via /proc/<pid>/fd (readable processes only). */
    private static Long findPidBySocketInode(String inode) {
        String target = "socket:[" + inode + "]";
        try (java.nio.file.DirectoryStream<java.nio.file.Path> procs =
                     java.nio.file.Files.newDirectoryStream(java.nio.file.Paths.get("/proc"))) {
            for (java.nio.file.Path p : procs) {
                String name = p.getFileName().toString();
                if (name.isEmpty() || !Character.isDigit(name.charAt(0))) continue;
                try (java.nio.file.DirectoryStream<java.nio.file.Path> fds =
                             java.nio.file.Files.newDirectoryStream(p.resolve("fd"))) {
                    for (java.nio.file.Path fd : fds) {
                        try {
                            if (target.equals(java.nio.file.Files.readSymbolicLink(fd).toString())) {
                                return Long.parseLong(name);
                            }
                        } catch (IOException ignored) {}
                    }
                } catch (IOException ignored) {
                    // Unreadable /proc/<pid>/fd: process belongs to another user.
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
