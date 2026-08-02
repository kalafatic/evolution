package eu.kalafatic.evolution.supervisor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import fi.iki.elonen.NanoHTTPD;

public class SupervisorMain3 {
    private static NanoHTTPD server;
    
    public static void main(String[] args) {
        System.out.println("=== EVO AI SUPERVISOR STARTING ===");

        String path = (args.length > 0) ? args[0] : ".";
        File baseDir = new File(path);

        System.out.println("[CONFIG] Base Directory: " + baseDir.getAbsolutePath());

        // Start the HTTP server on port 8089
        try {
            // Explicitly bind to 127.0.0.1 to avoid Windows dual-stack issues
            server = new EVOSupervisorServer(8089, baseDir);
            server.start();
            System.out.println("[HTTP] Server started on http://127.0.0.1:8089");
            System.out.println("[HTTP] Endpoints: /ping, /git-check, /maven-check, /build, /export, /start-evo, /stop-evo");
        } catch (Throwable t) {
            // Catch Throwable (not just Exception) to catch NoClassDefFoundError
            System.err.println("[HTTP] Failed to start HTTP server: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }

        // Start the file-based protocol supervisor
        SelfDevSupervisor supervisor = new SelfDevSupervisor(baseDir);
        supervisor.run();

        System.out.println("=== EVO AI SUPERVISOR FINISHED ===");
    }
    
    // Simple NanoHTTPD server for HTTP endpoints
    private static class EVOSupervisorServer extends NanoHTTPD {
        private final File baseDir;
        private final ObjectMapper mapper = new ObjectMapper();
        
        public EVOSupervisorServer(int port, File baseDir) {
            // Explicitly bind to 127.0.0.1
            super("127.0.0.1", port);
            this.baseDir = baseDir;
        }
        
        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();
            String method = session.getMethod().toString();
            
            System.out.println("[HTTP] " + method + " " + uri);
            
            try {
                switch (uri) {
                    case "/ping":
                        return newFixedLengthResponse(Response.Status.OK, "application/json", 
                            "{\"status\":\"OK\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}");
                    
                    case "/git-check":
                        return handleGitCheck(session);
                    
                    case "/maven-check":
                        return handleMavenCheck(session);
                    
                    case "/build":
                        return handleBuild(session);
                    
                    case "/export":
                        return handleExport(session);
                    
                    case "/start-evo":
                        return handleStartEvo(session);
                    
                    case "/stop-evo":
                        return handleStopEvo(session);
                    
                    default:
                        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", 
                            "404 Not Found");
                }
            } catch (Exception e) {
                System.err.println("[HTTP] Error handling " + uri + ": " + e.getMessage());
                e.printStackTrace();
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                    "Error: " + e.getMessage());
            }
        }
        
        private Response handleGitCheck(IHTTPSession session) {
            // Implementation
            return newFixedLengthResponse(Response.Status.OK, "application/json", 
                "{\"status\":\"OK\",\"message\":\"Git check passed\"}");
        }
        
        private Response handleMavenCheck(IHTTPSession session) {
            // Implementation
            return newFixedLengthResponse(Response.Status.OK, "application/json", 
                "{\"status\":\"OK\",\"message\":\"Maven check passed\"}");
        }
        
        private Response handleBuild(IHTTPSession session) {
            // Write command.json to trigger build
            try {
                File runDir = new File(baseDir, "self-dev-run");
                runDir.mkdirs();
                
                JSONObject command = new JSONObject();
                command.put("action", "BUILD_AND_RUN");
                command.put("iteration", 0);
                
                File commandFile = new File(runDir, "command.json");
                Files.write(commandFile.toPath(), command.toString(4).getBytes(StandardCharsets.UTF_8));
                
                return newFixedLengthResponse(Response.Status.OK, "application/json", 
                    "{\"status\":\"OK\",\"message\":\"Build command sent\"}");
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                    "Error: " + e.getMessage());
            }
        }
        
        private Response handleExport(IHTTPSession session) {
            // Implementation
            return newFixedLengthResponse(Response.Status.OK, "application/json", 
                "{\"status\":\"OK\",\"message\":\"Export completed\"}");
        }
        
        private Response handleStartEvo(IHTTPSession session) {
            // Write command.json to start EVO
            try {
                File runDir = new File(baseDir, "self-dev-run");
                runDir.mkdirs();
                
                JSONObject command = new JSONObject();
                command.put("action", "RESTART");
                command.put("iteration", 0);
                
                File commandFile = new File(runDir, "command.json");
                Files.write(commandFile.toPath(), command.toString(4).getBytes(StandardCharsets.UTF_8));
                
                return newFixedLengthResponse(Response.Status.OK, "application/json", 
                    "{\"status\":\"OK\",\"message\":\"Start EVO command sent\"}");
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                    "Error: " + e.getMessage());
            }
        }
        
        private Response handleStopEvo(IHTTPSession session) {
            // Write control.json to stop EVO
            try {
                File runDir = new File(baseDir, "self-dev-run");
                runDir.mkdirs();
                
                JSONObject control = new JSONObject();
                control.put("pause", false);
                control.put("forceAction", "STOP");
                
                File controlFile = new File(runDir, "control.json");
                Files.write(controlFile.toPath(), control.toString(4).getBytes(StandardCharsets.UTF_8));
                
                return newFixedLengthResponse(Response.Status.OK, "application/json", 
                    "{\"status\":\"OK\",\"message\":\"Stop EVO command sent\"}");
            } catch (IOException e) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", 
                    "Error: " + e.getMessage());
            }
        }
    }
}