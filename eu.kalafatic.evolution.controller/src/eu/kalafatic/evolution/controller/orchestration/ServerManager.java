package eu.kalafatic.evolution.controller.orchestration;

import java.io.IOException;

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
