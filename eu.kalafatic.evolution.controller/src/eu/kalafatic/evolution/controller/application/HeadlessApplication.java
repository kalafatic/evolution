package eu.kalafatic.evolution.controller.application;

import java.io.Console;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import eu.kalafatic.evolution.controller.orchestration.ServerManager;

/**
 * Headless application that starts the Evolution Server.
 * <p>
 * Family UX Standard v1.0: when command arguments are given (e.g. {@code /help},
 * {@code /status}), they are dispatched once and the application exits. When no
 * command arguments are given and a console is available (interactive TTY),
 * an interactive {@code evo>} REPL runs until {@code /exit} or EOF. Without a
 * console the behavior is unchanged: server + wait (daemon).
 */
public class HeadlessApplication implements IApplication {

    @Override
    public Object start(IApplicationContext context) throws Exception {
        String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        int port = eu.kalafatic.evolution.controller.resource.PortRegistry.EVO_HTTP_PRIMARY.getDefaultPort();
        List<String> otherArgs = new ArrayList<>();

        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if ("--port".equals(args[i]) && i + 1 < args.length) {
                    try {
                        port = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException ignored) {}
                } else if (args[i] != null && args[i].startsWith("--port=")) {
                    try {
                        port = Integer.parseInt(args[i].substring("--port=".length()));
                    } catch (NumberFormatException ignored) {}
                } else if (args[i] != null) {
                    otherArgs.add(args[i]);
                }
            }
        }

        System.out.println("Evolution Headless Server starting on port " + port + "...");
        // Register the local update repository (<install>/updates) when present.
        eu.kalafatic.utils.eclipse.P2Utils.registerLocalUpdateRepository();
        // Web server startup follows the persistent configuration:
        //   webServer.enabled=false  -> backend runs without any web server (DISABLED)
        //   webServer.autoStart=false -> backend runs, server startable via /start or the RCP menu
        // Defaults keep the historical behavior: the server starts automatically.
        boolean enabled = ServerManager.getInstance().isWebServerEnabled();
        boolean autoStart = ServerManager.getInstance().isWebServerAutoStart();
        if (!enabled) {
            ServerManager.getInstance().applyWebServerDisabledState();
            System.out.println("Evolution Headless: web server disabled (webServer.enabled=false). "
                    + "Backend services run without a web interface.");
        } else if (!autoStart) {
            System.out.println("Evolution Headless: web server auto-start disabled "
                    + "(webServer.autoStart=false). Start it with /start.");
        } else {
            ServerManager.getInstance().start(port);
            System.out.println("Evolution Headless Server started on port " + port + ".");
        }

        // 1. One-shot command dispatch (non-interactive): /help, /status, ...
        List<String> commandArgs = new ArrayList<>();
        for (String arg : otherArgs) {
            if (arg != null && (arg.startsWith("/") || HeadlessCli.isCommandArgument(arg))) {
                commandArgs.add(arg);
            }
        }
        if (!commandArgs.isEmpty()) {
            HeadlessCli cli = new HeadlessCli(port);
            cli.dispatch(String.join(" ", commandArgs), System.out);
            return IApplication.EXIT_OK;
        }

        // 2. Interactive REPL when a console is available
        Console console = System.console();
        if (console != null) {
            runRepl(console, port);
            return IApplication.EXIT_OK;
        }

        // 3. Non-TTY daemon behavior (unchanged): server + wait
        System.out.println("No console detected — running as daemon. Press Ctrl+C to stop.");
        synchronized (this) {
            while (true) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        return IApplication.EXIT_OK;
    }

    private void runRepl(Console console, int port) {
        HeadlessCli cli = new HeadlessCli(port);
        console.printf("%s%n", HeadlessCli.BANNER);
        while (true) {
            String line = console.readLine(HeadlessCli.PROMPT);
            if (line == null) {
                // EOF (Ctrl+D)
                console.printf("bye%n");
                break;
            }
            if (!cli.dispatch(line, System.out)) {
                console.printf("bye%n");
                break;
            }
        }
    }

    @Override
    public void stop() {
        ServerManager.getInstance().stop();
        synchronized (this) {
            notifyAll();
        }
    }
}
