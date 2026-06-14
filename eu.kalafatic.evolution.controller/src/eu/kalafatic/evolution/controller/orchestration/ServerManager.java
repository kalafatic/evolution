package eu.kalafatic.evolution.controller.orchestration;

import java.io.IOException;
import eu.kalafatic.forge.controller.api.SessionController;
import eu.kalafatic.forge.controller.api.ModelController;
import eu.kalafatic.forge.controller.api.DatasetController;
import eu.kalafatic.forge.controller.api.TrainingController;
import eu.kalafatic.forge.controller.api.SnapshotController;
import eu.kalafatic.forge.controller.impl.SessionControllerImpl;
import eu.kalafatic.forge.controller.impl.ModelControllerImpl;
import eu.kalafatic.forge.controller.impl.DatasetControllerImpl;
import eu.kalafatic.forge.controller.impl.TrainingControllerImpl;
import eu.kalafatic.forge.controller.impl.SnapshotControllerImpl;

/**
 * Singleton manager for EvolutionServer lifecycle.
 */
public class ServerManager {
    private static ServerManager instance;
    private EvolutionServer server;
    private int currentPort = 48080;

    private ServerManager() {}

    public static synchronized ServerManager getInstance() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    public synchronized void start(int port) throws IOException {
        if (server != null) {
            stop();
        }
        this.currentPort = port;
        server = new EvolutionServer(port);

        // Wire Controllers
        SessionController sc = new SessionControllerImpl(null);
        ModelController mc = new ModelControllerImpl(null);
        DatasetController dc = new DatasetControllerImpl(null);
        TrainingController tc = new TrainingControllerImpl(null);
        SnapshotController snc = new SnapshotControllerImpl(null);
        server.setForgeControllers(sc, mc, dc, tc, snc);

        server.startServer();
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    public synchronized void restart() throws IOException {
        stop();
        start(currentPort);
    }

    public synchronized boolean isRunning() {
        return server != null && server.isAlive();
    }

    public synchronized int getPort() {
        return currentPort;
    }
}
