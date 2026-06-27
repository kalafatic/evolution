package eu.kalafatic.evolution.controller.orchestration;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.kalafatic.evolution.forge.controller.api.DatasetController;
import eu.kalafatic.evolution.forge.controller.api.ModelController;
import eu.kalafatic.evolution.forge.controller.api.SessionController;
import eu.kalafatic.evolution.forge.controller.api.SnapshotController;
import eu.kalafatic.evolution.forge.controller.api.TrainingController;
import eu.kalafatic.evolution.forge.controller.impl.DatasetControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.ModelControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.SessionControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.SnapshotControllerImpl;
import eu.kalafatic.evolution.forge.controller.impl.TrainingControllerImpl;

/**
 * Singleton manager for multi-port EvolutionServer lifecycle.
 */
public class ServerManager {
    private static ServerManager instance;
    private final Map<Integer, EvolutionServer> activeServers = new ConcurrentHashMap<>();
    private int primaryPort = 48080;

    private ServerManager() {}

    public static synchronized ServerManager getInstance() {
        if (instance == null) {
            instance = new ServerManager();
        }
        return instance;
    }

    public synchronized void start(int port) throws IOException {
        if (activeServers.containsKey(port)) {
            stop(port);
        }
        
        EvolutionServer server = new EvolutionServer(port);

        // Wire Controllers
        SessionController sc = new SessionControllerImpl(null);
        ModelController mc = new ModelControllerImpl(null);
        DatasetController dc = new DatasetControllerImpl(null);
        TrainingController tc = new TrainingControllerImpl(null);
        SnapshotController snc = new SnapshotControllerImpl(null);
        server.setForgeControllers(sc, mc, dc, tc, snc);

        server.startServer();
        activeServers.put(port, server);
        this.primaryPort = port;
    }

    public synchronized void stop(int port) {
        EvolutionServer server = activeServers.get(port);
        if (server != null) {
            server.stop();
            activeServers.remove(port);
        }
    }

    public synchronized void stop() {
        stop(primaryPort);
    }

    public synchronized void stopAll() {
        for (Integer port : new java.util.HashSet<>(activeServers.keySet())) {
            stop(port);
        }
    }

    public synchronized void restart(int port) throws IOException {
        stop(port);
        start(port);
    }

    public synchronized boolean isRunning(int port) {
        EvolutionServer server = activeServers.get(port);
        return server != null && server.isAlive();
    }

    public synchronized boolean isAnyRunning() {
        return !activeServers.isEmpty();
    }

    public int getPort() {
        return primaryPort;
    }
    
    public Map<Integer, Boolean> getServerStatuses() {
        Map<Integer, Boolean> statuses = new HashMap<>();
        for (Map.Entry<Integer, EvolutionServer> entry : activeServers.entrySet()) {
            statuses.put(entry.getKey(), entry.getValue().isAlive());
        }
        return statuses;
    }
}
