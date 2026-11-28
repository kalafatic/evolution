package eu.kalafatic.evolution.controller.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        public enum Classification {
            /** Another EVO server answered on the port (X-EVO-Application: evolution). */
            EVO_INSTANCE,
            /** A different HTTP application answered (server/header/title detected). */
            OTHER_APPLICATION,
            /** The LISTEN socket belongs to a process we could identify, but no HTTP identity. */
            IDENTIFIED_PROCESS,
            /** A LISTEN socket exists but its owner cannot be inspected by this user. */
            UNKNOWN_OWNER
        }

        private final boolean inUse;
        private final int port;
        private final String affectedService;
        private final Long processId;
        private final String processName;
        private final String commandLine;
        private final boolean processIdVerified;
        private final String ownerUid;
        private final String httpServer;
        private final String httpContentType;
        private final String httpApplication;
        private final String httpTitle;
        private final String diagnosticMessage;

        public ConflictReport(boolean inUse, int port, String affectedService, Long processId,
                              String processName, String commandLine, boolean processIdVerified, String ownerUid,
                              String httpServer, String httpContentType, String httpApplication,
                              String httpTitle, String diagnosticMessage) {
            this.inUse = inUse;
            this.port = port;
            this.affectedService = affectedService;
            this.processId = processId;
            this.processName = processName;
            this.commandLine = commandLine;
            this.processIdVerified = processIdVerified;
            this.ownerUid = ownerUid;
            this.httpServer = httpServer;
            this.httpContentType = httpContentType;
            this.httpApplication = httpApplication;
            this.httpTitle = httpTitle;
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

        public Optional<String> getOwnerUid() {
            return Optional.ofNullable(ownerUid);
        }

        public Optional<String> getHttpServer() {
            return Optional.ofNullable(httpServer);
        }

        public Optional<String> getHttpContentType() {
            return Optional.ofNullable(httpContentType);
        }

        public Optional<String> getHttpApplication() {
            return Optional.ofNullable(httpApplication);
        }

        public Optional<String> getHttpTitle() {
            return Optional.ofNullable(httpTitle);
        }

        public Classification getClassification() {
            if (httpApplication != null && "evolution".equalsIgnoreCase(httpApplication.trim())) {
                return Classification.EVO_INSTANCE;
            }
            // EVO-family surfaces that predate the identity header are still recognisable
            // by their document title (e.g. "Log On to Evolution", "EVO MCP Platform
            // Interface", "ENR — Neural Runtime").
            if (httpTitle != null) {
                String t = httpTitle.toLowerCase();
                if (t.contains("evolution") || t.matches(".*\\bevo\\b.*")
                        || t.matches(".*\\benr\\b.*") || t.contains("neural runtime")) {
                    return Classification.EVO_INSTANCE;
                }
            }
            if (httpServer != null || httpTitle != null) {
                return Classification.OTHER_APPLICATION;
            }
            // A JSON endpoint without a conflicting identity may well be an EVO surface
            // (e.g. the ENR API); report it as an identified process, not as "other app".
            return processId != null ? Classification.IDENTIFIED_PROCESS : Classification.UNKNOWN_OWNER;
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
                sb.append(processIdVerified ? "Occupied by PID: " : "Candidate process PID (command-line match, socket owner not inspectable): ").append(processId);
                if (processName != null && !processName.isEmpty()) {
                    sb.append(" (").append(processName).append(")");
                }
                if (ownerUid != null && !ownerUid.isEmpty()) {
                    sb.append(" owner uid=").append(ownerUid);
                }
                if (commandLine != null && !commandLine.isEmpty()) {
                    sb.append(" Command: ").append(commandLine);
                }
            } else {
                sb.append("Occupant process not accessible from the current user account");
                if (ownerUid != null && !ownerUid.isEmpty()) {
                    sb.append(" (LISTEN socket owner uid=").append(ownerUid).append(")");
                }
                sb.append("; it may belong to root or another user. Client connections are");
                sb.append(" never reported as occupants.");
            }
            if (httpServer != null || httpContentType != null || httpTitle != null) {
                sb.append("\nHTTP identity: server='").append(httpServer == null ? "(none)" : httpServer)
                  .append("' content-type='").append(httpContentType == null ? "(none)" : httpContentType)
                  .append("' application='").append(httpApplication == null ? "(none)" : httpApplication)
                  .append("' title='").append(httpTitle == null ? "(none)" : httpTitle).append("'");
            }
            sb.append("\nClassification: ").append(getClassification())
              .append(getClassification() == Classification.EVO_INSTANCE
                      ? " (another EVO instance is already running)"
                      : " (not an EVO server we can verify)");
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
            return new ConflictReport(false, port, serviceName, null, null, null, false, null,
                    null, null, null, null, "Port is available");
        }

        // Port is occupied - inspect OS process details (LISTEN sockets only).
        Long pid = null;
        String pName = null;
        String cmdLine = null;
        String ownerUid = null;
        boolean pidVerified = false;

        try {
            String[] listen = findListenSocketInfo(port);
            if (listen != null) {
                ownerUid = listen[1];
                if (listen[0] != null) {
                    pid = findPidBySocketInode(listen[0]);
                    pidVerified = (pid != null);
                }
            }
            if (pid == null) {
                // Preserve the cross-platform fallbacks (lsof / ss) when the kernel
                // socket-inode lookup could not resolve the owner.
                pid = discoverProcessIdUsingPort(port);
                pidVerified = (pid != null);
            }
            if (pid == null) {
                // Root/other-user processes are not inspectable via /proc/<pid>/fd,
                // but `ps` command lines are world-readable. Use the command-line port
                // declaration as an explicitly marked *candidate*, never as a verified
                // socket owner.
                pid = findCandidatePidByCommandLine(port);
            }
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

        // Identify the application that answers on the port (best effort, short timeout).
        HttpIdentity http = probeHttpIdentity(port);

        StringBuilder message = new StringBuilder();
        message.append("Port ").append(port).append(" occupied for ").append(serviceName);
        if (pid != null) {
            message.append(" by PID ").append(pid);
        } else {
            message.append(" (LISTEN socket detected, owner not accessible to this user");
            if (ownerUid != null && !ownerUid.isEmpty()) {
                message.append(", socket uid=").append(ownerUid);
            }
            message.append(")");
        }
        if (http.server != null || http.contentType != null) {
            message.append("; HTTP server='").append(http.server == null ? "(none)" : http.server)
                   .append("' type='").append(http.contentType == null ? "(none)" : http.contentType).append("'");
        }
        return new ConflictReport(true, port, serviceName, pid, pName, cmdLine, pidVerified, ownerUid,
                http.server, http.contentType, http.application, http.title, message.toString());
    }

    /** Minimal HTTP identity discovered on a conflicted port. */
    private static final class HttpIdentity {
        String server;
        String contentType;
        String application;
        String title;
    }

    /**
     * Best-effort HTTP identity probe. Distinguishes the EVO server
     * ({@code X-EVO-Application: evolution}) from an unrelated HTTP server that
     * happens to occupy the same port and would otherwise return HTTP 200.
     */
    private static HttpIdentity probeHttpIdentity(int port) {
        HttpIdentity id = new HttpIdentity();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 400);
            socket.setSoTimeout(700);
            OutputStream out = socket.getOutputStream();
            out.write(("GET / HTTP/1.1\r\nHost: 127.0.0.1:" + port
                    + "\r\nConnection: close\r\nUser-Agent: EVO-PortDiagnostics\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.flush();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.ISO_8859_1));
            String statusLine = reader.readLine();
            if (statusLine == null || !statusLine.startsWith("HTTP/")) {
                return id; // not HTTP
            }
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                int idx = line.indexOf(':');
                if (idx <= 0) continue;
                String name = line.substring(0, idx).trim().toLowerCase();
                String value = line.substring(idx + 1).trim();
                switch (name) {
                    case "server": id.server = value; break;
                    case "content-type": id.contentType = value; break;
                    case "x-evo-application": id.application = value; break;
                    default: break;
                }
            }
            if (id.contentType != null && id.contentType.toLowerCase().contains("html")) {
                char[] buf = new char[4096];
                int n = reader.read(buf);
                if (n > 0) {
                    String body = new String(buf, 0, n);
                    Matcher m = Pattern.compile("<title[^>]*>(.*?)</title>",
                            Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(body);
                    if (m.find()) {
                        id.title = m.group(1).replaceAll("\\s+", " ").trim();
                        if (id.title.length() > 80) {
                            id.title = id.title.substring(0, 80) + "…";
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // No HTTP identity available (non-HTTP service, access denied, timeout).
        }
        return id;
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
        String[] listen = findListenSocketInfo(port);
        String inode = (listen != null) ? listen[0] : null;
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

    /**
     * Last-resort candidate detection for a port declared on a process command line
     * ({@code --port N}, {@code --port=N} or {@code -p N}). This is intentionally a
     * *candidate* only: a client connection such as {@code host:port} must never be
     * treated as the listener, so only explicit listen-port declarations match.
     */
    private static Long findCandidatePidByCommandLine(int port) {
        try {
            Process process = new ProcessBuilder("ps", "-eo", "pid=,args=").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                Pattern pattern = Pattern.compile("(?:--port[= ]|-p )" + port + "(?:\\s|$)");
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    int sp = line.indexOf(' ');
                    if (sp <= 0) continue;
                    String pidText = line.substring(0, sp).trim();
                    String args = line.substring(sp + 1);
                    if (pattern.matcher(args).find()) {
                        try {
                            return Long.parseLong(pidText);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Returns {@code [inode, uid]} of a LISTEN socket on the given port, or null.
     * The uid is reported even when the owning process is not inspectable, which
     * distinguishes "root/other user" from "no listener at all".
     */
    private static String[] findListenSocketInfo(int port) {
        for (String table : new String[]{"/proc/net/tcp", "/proc/net/tcp6"}) {
            try (BufferedReader reader = java.nio.file.Files.newBufferedReader(java.nio.file.Paths.get(table))) {
                String line = reader.readLine(); // header
                while ((line = reader.readLine()) != null) {
                    String[] f = line.trim().split("\\s+");
                    // Canonical row: sl local_address rem_address st tx rx tr uid timeout inode ...
                    if (f.length < 10) continue;
                    if (!"0A".equalsIgnoreCase(f[3])) continue; // LISTEN only
                    String local = f[1];
                    int idx = local.lastIndexOf(':');
                    if (idx < 0) continue;
                    try {
                        if (Integer.parseInt(local.substring(idx + 1), 16) == port) {
                            return new String[]{f[9], f[7]};
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
