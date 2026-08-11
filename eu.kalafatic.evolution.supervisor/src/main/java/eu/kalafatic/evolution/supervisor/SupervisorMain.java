package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;

public class SupervisorMain {
    private static NanoHTTPD server;
    private static NanoHTTPD controlServer;
    
    public static void main(String[] args) {
        System.out.println("=== EVO AI SUPERVISOR STARTING ===");

        String path = (args.length > 0) ? args[0] : ".";
        File baseDir = new File(path);

        System.out.println("[CONFIG] Base Directory: " + baseDir.getAbsolutePath());

        // ============================================================
        // START THE HTTP SERVERS FIRST - BEFORE THE MONITORING LOOP
        // ============================================================
        try {
            System.out.println("[HTTP] Initializing HTTP servers on port 8089 and 28080...");
            
            // Verify NanoHTTPD is in classpath
            Class.forName("fi.iki.elonen.NanoHTTPD");
            System.out.println("[HTTP] NanoHTTPD class found in classpath");
            
            // 1. Create and start the main supervisor API server
            server = new EVOSupervisorServer(8089, baseDir);
            server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("[HTTP] API Server started successfully on http://127.0.0.1:8089");

            // 2. Create and start the premium control dashboard server
            controlServer = new EVOSupervisorControlServer(28080, baseDir);
            controlServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            System.out.println("[HTTP] Control Dashboard started successfully on http://127.0.0.1:28080");

            System.out.println("[HTTP] Endpoints:");
            System.out.println("[HTTP]   GET /ping         - Health check");
            System.out.println("[HTTP]   GET /git-check    - Git availability");
            System.out.println("[HTTP]   GET /maven-check  - Maven availability");
            System.out.println("[HTTP]   POST /build       - Trigger build");
            System.out.println("[HTTP]   GET /export       - Export product");
            System.out.println("[HTTP]   POST /start-evo   - Start EVO");
            System.out.println("[HTTP]   POST /stop-evo    - Stop EVO");
            
            // Self-test: verify the server is actually responding
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) 
                    new java.net.URL("http://127.0.0.1:8089/ping").openConnection();
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.connect();
                int code = conn.getResponseCode();
                System.out.println("[HTTP] Self-test: /ping returned HTTP " + code);
                if (code == 200) {
                    System.out.println("[HTTP] Self-test: PASSED");
                } else {
                    System.err.println("[HTTP] Self-test: FAILED with code " + code);
                }
            } catch (Exception e) {
                System.err.println("[HTTP] Self-test: FAILED - " + e.getMessage());
                e.printStackTrace();
            }
            
        } catch (ClassNotFoundException e) {
            System.err.println("[HTTP] ERROR: NanoHTTPD class not found in classpath!");
            System.err.println("[HTTP] This means the shaded JAR does not include NanoHTTPD.");
            System.err.println("[HTTP] Please add the NanoHTTPD dependency to pom.xml:");
            System.err.println("[HTTP]   <dependency>");
            System.err.println("[HTTP]     <groupId>org.nanohttpd</groupId>");
            System.err.println("[HTTP]     <artifactId>nanohttpd</artifactId>");
            System.err.println("[HTTP]     <version>2.3.1</version>");
            System.err.println("[HTTP]   </dependency>");
            // Continue without HTTP server - file protocol still works
            System.err.println("[HTTP] Continuing with file-based protocol only.");
        } catch (Throwable t) {
            System.err.println("[HTTP] ERROR: Failed to start HTTP server: " + t.getMessage());
            t.printStackTrace();
            // Continue without HTTP server
            System.err.println("[HTTP] Continuing with file-based protocol only.");
        }

        // ============================================================
        // START THE MONITORING LOOP
        // ============================================================
        System.out.println("[SUPERVISOR] Starting monitoring loop...");
        SelfDevSupervisor supervisor = new SelfDevSupervisor(baseDir);
        supervisor.run();

        System.out.println("=== EVO AI SUPERVISOR FINISHED ===");
    }
    
    /**
     * NanoHTTPD server implementation for supervisor endpoints.
     */
    private static class EVOSupervisorServer extends NanoHTTPD {
        private final File baseDir;
        private final File runDir;
        private static volatile Process activeEvoProcess;
        
        public EVOSupervisorServer(int port, File baseDir) {
            // Explicitly bind to 127.0.0.1 to avoid Windows dual-stack issues
            super("127.0.0.1", port);
            this.baseDir = baseDir;
            this.runDir = new File(baseDir, "self-dev-run");
            System.out.println("[HTTP] EVOSupervisorServer created on port " + port + " bound to 127.0.0.1");
        }
        
        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            String method = session.getMethod().toString();
            
            System.out.println("[HTTP] " + method + " " + uri);
            
            try {
                // Parse query parameters
                Map<String, String> params = new HashMap<>();
                if (session.getMethod() == Method.GET) {
                    String query = session.getQueryParameterString();
                    if (query != null && !query.isEmpty()) {
                        for (String pair : query.split("&")) {
                            String[] parts = pair.split("=");
                            if (parts.length == 2) {
                                params.put(parts[0], parts[1]);
                            }
                        }
                    }
                }
                
                switch (uri) {
                    case "/ping":
                        return newFixedLengthResponse(Response.Status.OK, "application/json", 
                            "{\"status\":\"OK\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}");
                    
                    case "/git-check":
                        return handleGitCheck(session, params);
                    
                    case "/maven-check":
                        return handleMavenCheck(session, params);
                    
                    case "/build":
                        return handleBuild(session, params);
                    
                    case "/export":
                        return handleExport(session, params);
                    
                    case "/start-evo":
                        return handleStartEvo(session, params);
                    
                    case "/stop-evo":
                        return handleStopEvo(session, params);
                    
                    default:
                        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", 
                            "{\"status\":\"ERROR\",\"message\":\"404 Not Found: " + uri + "\"}");
                }
            } catch (Exception e) {
                System.err.println("[HTTP] Error handling " + uri + ": " + e.getMessage());
                e.printStackTrace();
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                    "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\",\"stackTrace\":\"" + sw.toString().replace("\"", "\\\"") + "\"}");
            }
        }
        
        private Response handleGitCheck(IHTTPSession session, Map<String, String> params) {
            try {
                String path = params.getOrDefault("path", baseDir.getAbsolutePath());
                System.out.println("[HTTP] Git check for path: " + path);
                
                ProcessBuilder pb = new ProcessBuilder("git", "--version");
                Process p = pb.start();
                boolean finished = p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                int exitCode;
                if (finished) {
                    exitCode = p.exitValue();
                } else {
                    p.destroyForcibly();
                    exitCode = -1;
                }
                
                if (exitCode == 0) {
                    return newFixedLengthResponse(Response.Status.OK, "application/json", 
                        "{\"status\":\"OK\",\"message\":\"Git is available\",\"path\":\"" + path + "\"}");
                } else {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                        "{\"status\":\"ERROR\",\"message\":\"Git not available\",\"exitCode\":" + exitCode + "}");
                }
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                    "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}");
            }
        }

        private Response handleMavenCheck(IHTTPSession session, Map<String, String> params) {
            try {
                String mvnCmd = System.getProperty("os.name").toLowerCase().contains("win") ? "mvn.cmd" : "mvn";
                System.out.println("[HTTP] Maven check using: " + mvnCmd);
                
                ProcessBuilder pb = new ProcessBuilder(mvnCmd, "-version");
                Process p = pb.start();
                boolean finished = p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                int exitCode;
                if (finished) {
                    exitCode = p.exitValue();
                } else {
                    p.destroyForcibly();
                    exitCode = -1;
                }
                
                if (exitCode == 0) {
                    return newFixedLengthResponse(Response.Status.OK, "application/json", 
                        "{\"status\":\"OK\",\"message\":\"Maven is available\",\"command\":\"" + mvnCmd + "\"}");
                } else {
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                        "{\"status\":\"ERROR\",\"message\":\"Maven not available\",\"exitCode\":" + exitCode + "}");
                }
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                    "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}");
            }
        }
        
        private Response handleBuild(IHTTPSession session, Map<String, String> params) {
            try {
                String path = params.getOrDefault("path", baseDir.getAbsolutePath());
                System.out.println("[HTTP] Build requested for path: " + path);
                
                // Ensure run directory exists
                if (!runDir.exists()) {
                    runDir.mkdirs();
                }
                
                // Write command.json to trigger build
                String command = "{\"action\":\"BUILD_AND_RUN\",\"iteration\":0}";
                File commandFile = new File(runDir, "command.json");
                java.nio.file.Files.write(commandFile.toPath(), 
                    command.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                
                System.out.println("[HTTP] Build command written to: " + commandFile.getAbsolutePath());
                
                return newFixedLengthResponse(Response.Status.OK, "application/json", 
                    "{\"status\":\"OK\",\"message\":\"Build command sent\",\"path\":\"" + path + "\"}");
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                    "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}");
            }
        }
        
        private Response handleExport(IHTTPSession session, Map<String, String> params) {
            try {
                String path = params.getOrDefault("path", baseDir.getAbsolutePath());
                System.out.println("[HTTP] Export requested for path: " + path);
                
                File exportDir = new File(baseDir, "export");
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }

                int copiedCount = 0;

                // 1. Copy EVO jars from workspace (sources) target directories recursively
                File sourcesDir = new File(path);
                if (sourcesDir.exists()) {
                    copiedCount += copyJars(sourcesDir, exportDir);
                }

                // 2. Copy supervisor jar from supervisor's bin or src/target
                File binDir = new File(baseDir, "bin");
                if (binDir.exists()) {
                    File[] binJars = binDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.startsWith("original-"));
                    if (binJars != null) {
                        for (File jar : binJars) {
                            try {
                                java.nio.file.Files.copy(jar.toPath(), new File(exportDir, jar.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                copiedCount++;
                                System.out.println("[HTTP] Exported supervisor jar from bin: " + jar.getName());
                            } catch (Exception ignored) {}
                        }
                    }
                } else {
                    File srcTargetDir = new File(baseDir, "src/target");
                    if (srcTargetDir.exists()) {
                        File[] targetJars = srcTargetDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.startsWith("original-"));
                        if (targetJars != null) {
                            for (File jar : targetJars) {
                                try {
                                    java.nio.file.Files.copy(jar.toPath(), new File(exportDir, jar.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                    copiedCount++;
                                    System.out.println("[HTTP] Exported supervisor jar from src/target: " + jar.getName());
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }

                return newFixedLengthResponse(Response.Status.OK, "application/json", 
                    "{\"status\":\"OK\",\"message\":\"Export completed. Exported " + copiedCount + " jars.\",\"path\":\"" + path + "\"}");
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                    "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}");
            }
        }
        
        private Response handleStartEvo(IHTTPSession session, Map<String, String> params) {
            try {
                String path = params.getOrDefault("path", baseDir.getAbsolutePath());
                System.out.println("[HTTP] Start EVO requested for path: " + path);
                
                // 1. Resolve exportDir
                File exportDir = new File(baseDir, "export");
                if (!exportDir.exists()) {
                    return newFixedLengthResponse(Response.Status.OK, "application/json",
                        "{\"status\":\"ERROR\",\"message\":\"No export folder found! Please run export first.\"}");
                }
                
                // 2. Find runnable jar
                File[] jars = exportDir.listFiles((dir, name) -> name.endsWith(".jar") && !name.startsWith("original-"));
                if (jars == null || jars.length == 0) {
                    return newFixedLengthResponse(Response.Status.OK, "application/json",
                        "{\"status\":\"ERROR\",\"message\":\"No jar files found in export folder!\"}");
                }

                File runnableJar = null;
                // Prioritize shaded/controller/servers/product jar that is not supervisor
                for (File jar : jars) {
                    String name = jar.getName().toLowerCase();
                    if (name.contains("supervisor")) {
                        continue;
                    }
                    if (name.contains("-shaded")) {
                        runnableJar = jar;
                        break;
                    }
                }
                if (runnableJar == null) {
                    for (File jar : jars) {
                        String name = jar.getName().toLowerCase();
                        if (!name.contains("supervisor")) {
                            runnableJar = jar;
                            break;
                        }
                    }
                }

                if (runnableJar == null) {
                    return newFixedLengthResponse(Response.Status.OK, "application/json",
                        "{\"status\":\"ERROR\",\"message\":\"No runnable EVO jar found in export folder.\"}");
                }

                // 3. Stop existing EVO process if running
                if (activeEvoProcess != null && activeEvoProcess.isAlive()) {
                    System.out.println("[HTTP] EVO is already running. Stopping it first...");
                    activeEvoProcess.destroyForcibly();
                    try {
                        activeEvoProcess.waitFor(2, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}
                }

                // 4. Start currently exported EVO application
                System.out.println("[HTTP] Launching exported EVO: " + runnableJar.getAbsolutePath());
                List<String> command = new ArrayList<>();
                command.add("java");
                command.add("-jar");
                command.add(runnableJar.getAbsolutePath());
                command.add("--mode=SELF_DEV");
                command.add("--variant=" + baseDir.getAbsolutePath());

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(exportDir);
                pb.redirectErrorStream(true);
                activeEvoProcess = pb.start();

                // Read console stream in a background thread to prevent deadlocks
                new Thread(() -> {
                    System.out.println("[HTTP] Reading EVO process stdout/stderr...");
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(activeEvoProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("[EVO] " + line);
                        }
                    } catch (Exception ignored) {}
                }).start();

                // Write command.json for backward compatibility or status file logging
                try {
                    if (!runDir.exists()) {
                        runDir.mkdirs();
                    }
                    String commandStr = "{\"action\":\"RESTART\",\"iteration\":0}";
                    File commandFile = new File(runDir, "command.json");
                    java.nio.file.Files.write(commandFile.toPath(),
                        commandStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                } catch (Exception ignored) {}
                
                return newFixedLengthResponse(Response.Status.OK, "application/json", 
                    "{\"status\":\"OK\",\"message\":\"SUCCESS: Started product " + runnableJar.getName() + "\",\"path\":\"" + path + "\"}");
            } catch (Exception e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                    "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}");
            }
        }
        
        private Response handleStopEvo(IHTTPSession session, Map<String, String> params) {
            try {
                System.out.println("[HTTP] Stop EVO requested");
                
                if (!runDir.exists()) {
                    runDir.mkdirs();
                }
                
                String control = "{\"pause\":false,\"forceAction\":\"STOP\"}";
                File controlFile = new File(runDir, "control.json");
                java.nio.file.Files.write(controlFile.toPath(), 
                    control.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                
                if (activeEvoProcess != null && activeEvoProcess.isAlive()) {
                    System.out.println("[HTTP] Forcibly destroying active EVO process...");
                    activeEvoProcess.destroyForcibly();
                    try {
                        activeEvoProcess.waitFor(2, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {}
                    activeEvoProcess = null;
                    return newFixedLengthResponse(Response.Status.OK, "application/json",
                        "{\"status\":\"OK\",\"message\":\"SUCCESS: Stopped running product.\"}");
                }

                activeEvoProcess = null;
                return newFixedLengthResponse(Response.Status.OK, "application/json", 
                    "{\"status\":\"OK\",\"message\":\"READY (Stopped) - Product was not running.\"}");
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", 
                    "{\"status\":\"ERROR\",\"message\":\"" + e.getMessage() + "\"}");
            }
        }

        private static int copyJars(File dir, File exportDir) {
            int count = 0;
            if (dir.isDirectory()) {
                if (dir.getName().equals("target")) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (f.isFile() && f.getName().endsWith(".jar")) {
                                try {
                                    java.nio.file.Files.copy(f.toPath(), new File(exportDir, f.getName()).toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                                    count++;
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    return count;
                }
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) {
                            String name = f.getName();
                            if (!name.equals(".git") && !name.equals("self-dev-run") && !name.equals(".settings") && !name.equals(".metadata") && !name.equals("bin")) {
                                count += copyJars(f, exportDir);
                            }
                        }
                    }
                }
            }
            return count;
        }
    }
}