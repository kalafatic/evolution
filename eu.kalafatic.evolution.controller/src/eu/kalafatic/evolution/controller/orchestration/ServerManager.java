package eu.kalafatic.evolution.controller.orchestration;

import java.io.IOException;

import src.eu.kalafatic.evolution.forge.controller.api.DatasetController;
import src.eu.kalafatic.evolution.forge.controller.api.ModelController;
import src.eu.kalafatic.evolution.forge.controller.api.SessionController;
import src.eu.kalafatic.evolution.forge.controller.api.SnapshotController;
import src.eu.kalafatic.evolution.forge.controller.api.TrainingController;
import src.eu.kalafatic.evolution.forge.controller.impl.DatasetControllerImpl;
import src.eu.kalafatic.evolution.forge.controller.impl.ModelControllerImpl;
import src.eu.kalafatic.evolution.forge.controller.impl.SessionControllerImpl;
import src.eu.kalafatic.evolution.forge.controller.impl.SnapshotControllerImpl;
import src.eu.kalafatic.evolution.forge.controller.impl.TrainingControllerImpl;

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
