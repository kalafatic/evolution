package eu.kalafatic.evolution.controller.manager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;


public class OllamaConfigManager {

    public enum OS { WINDOWS, MACOS, LINUX, UNKNOWN }

    public static class OllamaDefaults {
        public final String modelPath;
        public final String logPath;
        public final String binPath;
        public final String apiUrl; // Added URL field
        public final Map<String, String> envVars;

        public OllamaDefaults(String modelPath, String logPath, String binPath, String host, String port) {
            this.modelPath = resolveHome(modelPath);
            this.logPath = resolveHome(logPath);
            this.binPath = binPath;
            this.apiUrl = String.format("http://%s:%s", host, port);
            
            this.envVars = new HashMap<>();
            this.envVars.put("OLLAMA_HOST", host + ":" + port);
            this.envVars.put("OLLAMA_MODELS", this.modelPath);
        }

        private String resolveHome(String path) {
            if (path.startsWith("~")) {
                return System.getProperty("user.home") + path.substring(1);
            }
            return path;
        }
    }

    public static OS getOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) return OS.WINDOWS;
        if (osName.contains("mac")) return OS.MACOS;
        if (osName.contains("nix") || osName.contains("nux")) return OS.LINUX;
        return OS.UNKNOWN;
    }

    public static OllamaDefaults getDefaults() {
        // Defaulting to 127.0.0.1 and 11434
        String defaultHost = "127.0.0.1";
        String defaultPort = "11434";

        OS os = getOS();
        if (os == OS.UNKNOWN) os = OS.LINUX; // Default to Ubuntu/Linux

        switch (os) {
            case WINDOWS:
                String localAppData = System.getenv("LOCALAPPDATA");
                return new OllamaDefaults(
                    "~/.ollama/models",
                    localAppData + "\\Ollama\\server.log",
                    localAppData + "\\Programs\\Ollama\\ollama.exe",
                    defaultHost, defaultPort
                );
            case MACOS:
                return new OllamaDefaults(
                    "~/.ollama/models",
                    "~/.ollama/logs/server.log",
                    "/usr/local/bin/ollama",
                    defaultHost, defaultPort
                );
            case LINUX:
            default: // Default: Ubuntu
                return new OllamaDefaults(
                    "/usr/share/ollama/.ollama/models",
                    "/var/log/ollama.log",
                    "/usr/local/bin/ollama",
                    defaultHost, defaultPort
                );
        }
    }

    public static void main(String[] args) {
        OllamaDefaults config = getDefaults();
        System.out.println("--- Ollama Configuration ---");
        System.out.println("OS: " + getOS());
        System.out.println("API URL: " + config.apiUrl); // http://127.0.0.1:11434
        System.out.println("Generate Endpoint: " + config.apiUrl + "/api/generate");
        System.out.println("Chat Endpoint: " + config.apiUrl + "/api/chat");
    }
    
    
    public static List<OllamaModel> loadModels() {
    	 OllamaDefaults config = getDefaults();    	 
    	 
	    List<OllamaModel> result = new ArrayList<>();

	    try {
	    	
	        URL url = new URL(config.apiUrl + "/api/tags");
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	        conn.setRequestMethod("GET");

	        BufferedReader reader = new BufferedReader(
	            new InputStreamReader(conn.getInputStream())
	        );

	        StringBuilder json = new StringBuilder();
	        String line;

	        while ((line = reader.readLine()) != null) {
	            json.append(line);
	        }

	        reader.close();

	        JSONObject obj = new JSONObject(json.toString());
	        JSONArray models = obj.getJSONArray("models");

	        for (int i = 0; i < models.length(); i++) {
	            JSONObject m = models.getJSONObject(i);

	            String name = m.getString("name");
	            long size = m.optLong("size", 0);

	            result.add(new OllamaModel(name, size));
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return result;
	}
}