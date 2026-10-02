package eu.kalafatic.evolution.controller.orchestration.selfdev.net;

import java.io.File;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;

public interface NetworkAccessProvider {
    String getName();
    NetworkAccessResult prepareNetworkAccess(File executable, int port, SelfDevContext context);
}
