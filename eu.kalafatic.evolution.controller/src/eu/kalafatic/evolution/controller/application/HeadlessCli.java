package eu.kalafatic.evolution.controller.application;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.harness.ConnectedApplicationDescriptor;
import eu.kalafatic.evolution.controller.harness.ConnectedApplicationRegistry;
import eu.kalafatic.evolution.controller.log.Log;
import eu.kalafatic.evolution.controller.manager.OllamaManager;
import eu.kalafatic.evolution.controller.manager.OllamaModel;
import eu.kalafatic.evolution.controller.manager.OllamaService;
import eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager;
import eu.kalafatic.evolution.controller.orchestration.ServerEntry;
import eu.kalafatic.evolution.controller.orchestration.ServerManager;
import eu.kalafatic.evolution.controller.providers.AiProviders;
import eu.kalafatic.evolution.controller.providers.ProviderConfig;
import eu.kalafatic.evolution.controller.resource.PortAllocationManager;
import eu.kalafatic.evolution.controller.resource.PortConfiguration;

/**
 * Headless EVO command-line interface (Family UX Standard v1.0).
 * <p>
 * A single command table (name, syntax, short help, topic help, handler)
 * drives both dispatch and the generated help output. Slash is optional and
 * command names are case-insensitive. Commands without a real backing
 * capability are intentionally not registered (no invented surfaces).
 */
public class HeadlessCli {

    /** Interactive REPL prompt. */
    public static final String PROMPT = "evo> ";

    /** Banner printed when the interactive REPL starts. */
    public static final String BANNER = "Evolution Headless CLI.\n"
            + "Type /help for the command list. /exit or Ctrl+D to leave.";

    private final int port;

    public HeadlessCli(int port) {
        this.port = port;
    }

    // ---------------------------------------------------------------
    // Command table (authoritative for dispatch AND help output)
    // ---------------------------------------------------------------

    @FunctionalInterface
    private interface Handler {
        void run(HeadlessCli cli, String[] tokens, PrintStream out) throws Exception;
    }

    private static final class Command {
        final String name;
        final String syntax;
        final String shortHelp;
        final String topicHelp;
        final Handler handler;

        Command(String name, String syntax, String shortHelp, String topicHelp, Handler handler) {
            this.name = name;
            this.syntax = syntax;
            this.shortHelp = shortHelp;
            this.topicHelp = topicHelp;
            this.handler = handler;
        }
    }

    private static final List<Command> COMMANDS = List.of(
            new Command("help", "/help [topic]",
                    "Show command help (optionally per topic)",
                    "help\n"
                            + "  Usage: /help [topic]\n\n"
                            + "  Lists all available commands. With a topic (e.g. /help status),\n"
                            + "  prints the detailed help of that command.",
                    HeadlessCli::cmdHelp),
            new Command("status", "/status",
                    "Show the EVO server status",
                    "status\n"
                            + "  Usage: /status\n\n"
                            + "  Queries GET http://127.0.0.1:<port>/server/status. When the\n"
                            + "  server is unreachable, reports the local ServerManager state.",
                    HeadlessCli::cmdStatus),
            new Command("web", "/web",
                    "Print the web interface URL",
                    "web\n"
                            + "  Usage: /web\n\n"
                            + "  Prints the URL of the EVO web interface (Intelligence Portal).",
                    HeadlessCli::cmdWeb),
            new Command("terminal", "/terminal",
                    "Open a system terminal in the workspace directory",
                    "terminal\n"
                            + "  Usage: /terminal\n\n"
                            + "  Spawns a system terminal (x-terminal-emulator, gnome-terminal,\n"
                            + "  konsole or xterm) in the workspace directory (fallback: user home).",
                    HeadlessCli::cmdTerminal),
            new Command("logs", "/logs show",
                    "Tail the EVO log file",
                    "logs\n"
                            + "  Usage: /logs show\n\n"
                            + "  Prints the last lines of the EVO log file (evo.log).",
                    HeadlessCli::cmdLogs),
            new Command("sessions", "/sessions",
                    "List forge sessions (GET /forge/sessions)",
                    "sessions\n"
                            + "  Usage: /sessions\n\n"
                            + "  Lists the forge sessions from GET /forge/sessions; when the server\n"
                            + "  is unreachable, falls back to the in-process ForgeSessionManager.",
                    HeadlessCli::cmdSessions),
            new Command("models", "/models",
                    "List EVO models (local Ollama models + provider defaults)",
                    "models\n"
                            + "  Usage: /models\n\n"
                            + "  Lists the local Ollama models reported by GET /server/status\n"
                            + "  (name, size) and the default models of the configured LLM\n"
                            + "  providers (AiProviders). When the server is unreachable,\n"
                            + "  queries the local Ollama service directly.",
                    HeadlessCli::cmdModels),
            new Command("providers", "/providers",
                    "List LLM providers and connected coding harness providers",
                    "providers\n"
                            + "  Usage: /providers\n\n"
                            + "  Lists the configured LLM providers (AiProviders: name, url,\n"
                            + "  local, default model — API keys are never printed) and the\n"
                            + "  connected coding-harness applications of the\n"
                            + "  ConnectedApplicationRegistry.",
                    HeadlessCli::cmdProviders),
            new Command("agents", "/agents",
                    "List connected coding agents (harness adapters)",
                    "agents\n"
                            + "  Usage: /agents\n\n"
                            + "  Lists the connected coding-harness agents of the\n"
                            + "  ConnectedApplicationRegistry (name, type, enabled, status,\n"
                            + "  capabilities, selected). These are the family coding agents\n"
                            + "  available to Darwin sessions.",
                    HeadlessCli::cmdAgents),
            new Command("config", "/config",
                    "Show the authoritative EVO port/service configuration",
                    "config\n"
                            + "  Usage: /config\n\n"
                            + "  Prints the PortAllocationManager service configuration and the\n"
                            + "  current ServerManager server states.",
                    HeadlessCli::cmdConfig),
            new Command("stop", "/stop",
                    "Stop all EVO servers (graceful)",
                    "stop\n"
                            + "  Usage: /stop\n\n"
                            + "  Gracefully stops all EVO servers via ServerManager.stopAll().",
                    HeadlessCli::cmdStop),
            new Command("start", "/start",
                    "Start the primary EVO HTTP server",
                    "start\n"
                            + "  Usage: /start\n\n"
                            + "  Starts the primary EVO HTTP server when it is not already running.",
                    HeadlessCli::cmdStart),
            new Command("restart", "/restart",
                    "Restart the primary EVO HTTP server",
                    "restart\n"
                            + "  Usage: /restart\n\n"
                            + "  Stops and restarts the primary EVO HTTP server.",
                    HeadlessCli::cmdRestart),
            new Command("exit", "/exit",
                    "Exit the interactive CLI (REPL only)",
                    "exit\n"
                            + "  Usage: /exit\n\n"
                            + "  Leaves the interactive command line. Only valid inside the REPL.",
                    (cli, tokens, out) -> {
                        // handled by dispatch(); never invoked directly
                    }));

    // ---------------------------------------------------------------
    // Dispatch
    // ---------------------------------------------------------------

    /**
     * Dispatches one command line.
     *
     * @return {@code false} when the REPL should terminate ({@code /exit})
     */
    public boolean dispatch(String line, PrintStream out) {
        String input = (line == null) ? "" : line.trim();
        if (input.isEmpty()) {
            return true;
        }
        String[] tokens = input.split("\\s+");
        String name = normalize(tokens[0]);
        if ("exit".equals(name)) {
            return false;
        }
        Command command = find(name);
        if (command == null) {
            out.println("Unknown command '" + tokens[0] + "'. " + suggest(name));
            return true;
        }
        try {
            command.handler.run(this, tokens, out);
        } catch (Exception e) {
            out.println("Error: " + e.getMessage());
        }
        return true;
    }

    /** True when the argument (with or without leading slash) is a command. */
    public static boolean isCommandArgument(String arg) {
        if (arg == null || arg.isBlank()) {
            return false;
        }
        String name = normalize(arg);
        if (name.isEmpty()) {
            return false;
        }
        return find(name) != null;
    }

    private static String normalize(String token) {
        String t = (token == null) ? "" : token.trim().toLowerCase();
        while (t.startsWith("/")) {
            t = t.substring(1);
        }
        return t;
    }

    private static Command find(String name) {
        for (Command command : COMMANDS) {
            if (command.name.equals(name)) {
                return command;
            }
        }
        return null;
    }

    private static String suggest(String name) {
        List<String> matches = new ArrayList<>();
        for (Command command : COMMANDS) {
            if (command.name.startsWith(name)) {
                matches.add(command.name);
            }
        }
        if (matches.isEmpty()) {
            List<Command> byDistance = new ArrayList<>(COMMANDS);
            byDistance.sort(Comparator
                    .comparingInt((Command c) -> levenshtein(name, c.name))
                    .thenComparing(c -> c.name));
            int best = byDistance.isEmpty() ? Integer.MAX_VALUE : levenshtein(name, byDistance.get(0).name);
            if (best <= 2) {
                for (Command c : byDistance) {
                    if (levenshtein(name, c.name) == best) {
                        matches.add(c.name);
                    }
                }
            }
        }
        if (matches.isEmpty()) {
            return "Type /help to list the available commands.";
        }
        StringBuilder sb = new StringBuilder("Did you mean:");
        int shown = 0;
        for (String match : matches) {
            if (shown >= 3) {
                break;
            }
            sb.append(" /").append(match);
            shown++;
        }
        return sb.toString();
    }

    private static int levenshtein(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int corner = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int upper = costs[j];
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                costs[j] = Math.min(Math.min(costs[j - 1] + 1, costs[j] + 1), corner + cost);
                corner = upper;
            }
        }
        return costs[b.length()];
    }

    // ---------------------------------------------------------------
    // Command handlers
    // ---------------------------------------------------------------

    private void cmdHelp(String[] tokens, PrintStream out) {
        if (tokens.length > 1) {
            String topic = normalize(tokens[1]);
            Command command = find(topic);
            if (command == null) {
                out.println("No help for topic '" + tokens[1] + "'. " + suggest(topic));
                return;
            }
            out.println(command.topicHelp);
            return;
        }
        out.println(BANNER);
        out.println();
        out.println("Commands (slash optional, lowercase):");
        for (Command command : COMMANDS) {
            out.println(String.format("  %-18s %s", command.syntax, command.shortHelp));
        }
        out.println();
        out.println("Type /help <topic> for detailed help on a command.");
    }

    private void cmdStatus(String[] tokens, PrintStream out) {
        String body = httpGet("/server/status");
        if (body != null) {
            out.println("GET http://127.0.0.1:" + port + "/server/status");
            out.println(body.trim());
            return;
        }
        out.println("Server unreachable on port " + port + " — local ServerManager state:");
        for (ServerEntry entry : ServerManager.getInstance().getServerEntries()) {
            out.println(String.format("  %-16s %-7s : %s", entry.getServiceId(),
                    entry.getPort(), entry.getState()));
        }
    }

    private void cmdWeb(String[] tokens, PrintStream out) {
        out.println("Web interface: http://127.0.0.1:" + port + "/dashboard.html");
        out.println("Server root:   http://127.0.0.1:" + port + "/");
    }

    private void cmdTerminal(String[] tokens, PrintStream out) {
        String directory = null;
        try {
            File wsRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile();
            if (wsRoot.isDirectory()) {
                directory = wsRoot.getAbsolutePath();
            }
        } catch (Throwable t) {
            // No Eclipse workspace in this runtime — fall through
        }
        if (directory == null) {
            directory = System.getProperty("user.dir");
        }
        if (directory == null || !new File(directory).isDirectory()) {
            directory = System.getProperty("user.home", ".");
        }
        String[] command = terminalCommand(directory);
        if (command == null) {
            out.println("No supported terminal emulator found on this system "
                    + "(tried x-terminal-emulator, gnome-terminal, konsole, xterm).");
            return;
        }
        try {
            new ProcessBuilder(command).start();
            out.println("Terminal opened in " + directory);
        } catch (IOException e) {
            out.println("Failed to open terminal: " + e.getMessage());
        }
    }

    private void cmdLogs(String[] tokens, PrintStream out) {
        if (tokens.length < 2 || !"show".equals(normalize(tokens[1]))) {
            out.println("Usage: /logs show");
            return;
        }
        String logFile = Log.getLogFile();
        File file = new File(logFile);
        if (!file.exists()) {
            out.println("EVO log file not found: " + file.getAbsolutePath());
            out.println("EVO logs are written to " + file.getAbsolutePath() + " once the file logger initializes.");
            return;
        }
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            int start = Math.max(0, lines.size() - 40);
            out.println("Log file: " + file.getAbsolutePath() + " (last " + (lines.size() - start) + " of " + lines.size() + " lines)");
            for (int i = start; i < lines.size(); i++) {
                out.println(lines.get(i));
            }
        } catch (IOException e) {
            out.println("Failed to read log file: " + e.getMessage());
        }
    }

    private void cmdSessions(String[] tokens, PrintStream out) {
        String body = httpGet("/forge/sessions");
        if (body != null) {
            out.println("GET http://127.0.0.1:" + port + "/forge/sessions");
            out.println(body.trim());
            return;
        }
        List<eu.kalafatic.evolution.model.orchestration.ForgeSession> sessions =
                ForgeSessionManager.getInstance().getSessions();
        if (sessions == null || sessions.isEmpty()) {
            out.println("No forge sessions registered (in-process fallback).");
            return;
        }
        out.println("Forge sessions (in-process fallback):");
        for (eu.kalafatic.evolution.model.orchestration.ForgeSession session : sessions) {
            out.println(String.format("  %-24s %-12s %s", session.getName(),
                    session.getSessionId(), session.getStatus()));
        }
    }

    private void cmdModels(String[] tokens, PrintStream out) {
        String body = httpGet("/server/status");
        if (body != null) {
            try {
                JSONObject status = new JSONObject(body);
                JSONObject ollama = status.optJSONObject("ollama");
                out.println("Local Ollama models (from GET /server/status):");
                if (ollama == null) {
                    out.println("  (no Ollama section in the server status)");
                } else {
                    out.println("  online: " + ollama.optBoolean("online"));
                    JSONArray models = ollama.optJSONArray("models");
                    if (models == null || models.length() == 0) {
                        out.println("  (no models reported)");
                    }
                    if (models != null) {
                        for (int i = 0; i < models.length(); i++) {
                            JSONObject model = models.getJSONObject(i);
                            out.println("  - " + model.optString("name")
                                    + "  (" + model.optLong("size") + " bytes)");
                        }
                    }
                }
            } catch (Exception e) {
                out.println("Could not parse /server/status: " + e.getMessage());
            }
        } else {
            out.println("Server unreachable — querying local Ollama "
                    + "(http://localhost:11434):");
            OllamaService service = OllamaManager.getInstance()
                    .getService("http://localhost:11434");
            try {
                if (!service.ping()) {
                    out.println("  (Ollama not reachable)");
                }
                List<OllamaModel> models = service.loadModels();
                if (models.isEmpty()) {
                    out.println("  (no local models)");
                }
                for (OllamaModel model : models) {
                    out.println("  - " + model.getName() + "  ("
                            + model.getSize() + " bytes)");
                }
            } catch (Exception e) {
                out.println("  Failed to query local models: " + e.getMessage());
            }
        }
        out.println();
        out.println("Provider default models (AiProviders):");
        for (ProviderConfig cfg : AiProviders.PROVIDERS.values()) {
            out.println("  " + cfg.getName() + " -> "
                    + (cfg.getDefaultModel() == null ? "(none)" : cfg.getDefaultModel()));
        }
    }

    private void cmdProviders(String[] tokens, PrintStream out) {
        out.println("Configured LLM providers (AiProviders):");
        for (ProviderConfig cfg : AiProviders.PROVIDERS.values()) {
            out.println(String.format("  %-12s url=%-52s local=%-5s default=%s",
                    cfg.getName(), cfg.getUrl(), cfg.isLocal(),
                    cfg.getDefaultModel() == null ? "(none)" : cfg.getDefaultModel()));
        }
        out.println();
        ConnectedApplicationRegistry registry = ConnectedApplicationRegistry.getInstance();
        List<ConnectedApplicationDescriptor> apps = registry.listApplications();
        out.println("Connected coding-harness providers (ConnectedApplicationRegistry):");
        if (apps.isEmpty()) {
            out.println("  (none connected)");
            return;
        }
        String selected = registry.getSelectedApplicationId().orElse("");
        for (ConnectedApplicationDescriptor app : apps) {
            out.println(String.format(
                    "  %-24s %-12s enabled=%-5s status=%-12s%s",
                    app.getName(), app.getApplicationType(), app.isEnabled(),
                    app.getStatus(),
                    app.getId().equals(selected) ? "  [selected]" : ""));
        }
    }

    private void cmdAgents(String[] tokens, PrintStream out) {
        ConnectedApplicationRegistry registry = ConnectedApplicationRegistry.getInstance();
        List<ConnectedApplicationDescriptor> apps = registry.listApplications();
        if (apps.isEmpty()) {
            out.println("No connected coding agents "
                    + "(ConnectedApplicationRegistry is empty).");
            return;
        }
        String selected = registry.getSelectedApplicationId().orElse("");
        out.println("Connected coding agents:");
        for (ConnectedApplicationDescriptor app : apps) {
            out.println(String.format(
                    "  %-24s %-12s enabled=%-5s status=%-12s capabilities=%s%s",
                    app.getName(), app.getApplicationType(), app.isEnabled(),
                    app.getStatus(), app.getCapabilities(),
                    app.getId().equals(selected) ? "  [selected]" : ""));
        }
    }

    private void cmdConfig(String[] tokens, PrintStream out) {
        out.println("Port allocation (PortAllocationManager):");
        for (PortConfiguration cfg : PortAllocationManager.getInstance().getAllConfigurations()) {
            out.println(String.format("  %-18s %-40s %d%s", cfg.getServiceId(),
                    cfg.getServiceName(), cfg.getAssignedPort(),
                    cfg.isEnabled() ? "" : " (disabled)"));
        }
        out.println();
        out.println("ServerManager state:");
        for (ServerEntry entry : ServerManager.getInstance().getServerEntries()) {
            out.println(String.format("  %-16s %-7s : %s", entry.getServiceId(),
                    entry.getPort(), entry.getState()));
        }
    }

    private void cmdStop(String[] tokens, PrintStream out) {
        ServerManager.getInstance().stopAll();
        out.println("All EVO servers stopped.");
    }

    private void cmdStart(String[] tokens, PrintStream out) {
        ServerManager serverManager = ServerManager.getInstance();
        if (serverManager.isRunning(port)) {
            out.println("Primary EVO server already running on port " + port + ".");
            return;
        }
        try {
            serverManager.start(port);
            out.println("Primary EVO server started on port " + port + ".");
        } catch (IOException e) {
            out.println("Failed to start primary EVO server on port " + port + ": " + e.getMessage());
        }
    }

    private void cmdRestart(String[] tokens, PrintStream out) {
        try {
            ServerManager.getInstance().restart(port);
            out.println("Primary EVO server restarted on port " + port + ".");
        } catch (IOException e) {
            out.println("Failed to restart primary EVO server on port " + port + ": " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Support helpers
    // ---------------------------------------------------------------

    private String httpGet(String path) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder(
                    URI.create("http://127.0.0.1:" + port + path))
                    .timeout(Duration.ofSeconds(3)).GET().build();
            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String[] terminalCommand(String directory) {
        if (hasCommand("x-terminal-emulator")) {
            return new String[]{"x-terminal-emulator", "--working-directory=" + directory};
        }
        if (hasCommand("gnome-terminal")) {
            return new String[]{"gnome-terminal", "--working-directory=" + directory};
        }
        if (hasCommand("konsole")) {
            return new String[]{"konsole", "--workdir", directory};
        }
        if (hasCommand("xterm")) {
            return new String[]{"xterm", "-e", "cd " + shellQuote(directory) + " && $SHELL"};
        }
        return null;
    }

    private static boolean hasCommand(String command) {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(":")) {
            if (Files.isExecutable(new File(dir, command).toPath())) {
                return true;
            }
        }
        return false;
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
