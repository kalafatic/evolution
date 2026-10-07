package eu.kalafatic.evolution.supervisor.ui;

import java.io.File;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class SupervisorRcpApplication implements IApplication {

    private SupervisorUiWindow uiWindow;

    @Override
    public Object start(IApplicationContext context) throws Exception {
        String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
        if (args == null) {
            args = new String[0];
        }

        File baseDir = new File(".");
        int supervisorPort = 8089;
        int controlPort = 28080;
        boolean debug = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("--port=")) {
                try { supervisorPort = Integer.parseInt(arg.substring(7).trim()); } catch (Exception ignored) {}
            } else if ("--port".equals(arg) && i + 1 < args.length) {
                try { supervisorPort = Integer.parseInt(args[++i].trim()); } catch (Exception ignored) {}
            } else if (arg.startsWith("--control-port=")) {
                try { controlPort = Integer.parseInt(arg.substring(15).trim()); } catch (Exception ignored) {}
            } else if ("--control-port".equals(arg) && i + 1 < args.length) {
                try { controlPort = Integer.parseInt(args[++i].trim()); } catch (Exception ignored) {}
            } else if ("--debug".equalsIgnoreCase(arg)) {
                debug = true;
            } else if (!arg.startsWith("-")) {
                baseDir = new File(arg);
            }
        }

        uiWindow = new SupervisorUiWindow(baseDir, supervisorPort, controlPort, debug);
        uiWindow.open(args);

        return IApplication.EXIT_OK;
    }

    @Override
    public void stop() {
        if (uiWindow != null) {
            uiWindow.stopSupervisorLogic();
        }
    }
}
