package eu.kalafatic.evolution.controller.application;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import eu.kalafatic.evolution.controller.orchestration.ServerManager;

/**
 * Headless application that starts the Evolution Server.
 */
public class HeadlessApplication implements IApplication {

    @Override
    public Object start(IApplicationContext context) throws Exception {
        String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        int port = 48080;

        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                port = Integer.parseInt(args[++i]);
            }
        }

        System.out.println("Evolution Headless Server starting on port " + port + "...");
        ServerManager.getInstance().start(port);
        System.out.println("Evolution Headless Server started. Press Ctrl+C to stop.");

        // Keep the application running
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

    @Override
    public void stop() {
        ServerManager.getInstance().stop();
        synchronized (this) {
            notifyAll();
        }
    }
}
