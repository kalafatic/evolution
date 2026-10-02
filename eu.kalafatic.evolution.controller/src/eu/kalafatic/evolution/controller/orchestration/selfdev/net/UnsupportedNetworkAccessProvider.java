package eu.kalafatic.evolution.controller.orchestration.selfdev.net;

import java.io.File;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;

public class UnsupportedNetworkAccessProvider implements NetworkAccessProvider {

    @Override
    public String getName() {
        return "UnsupportedNetworkAccessProvider";
    }

    @Override
    public NetworkAccessResult prepareNetworkAccess(File executable, int port, SelfDevContext context) {
        return new NetworkAccessResult.Builder()
                .provider(getName())
                .executable(executable)
                .ports(String.valueOf(port))
                .success(true)
                .supported(false)
                .skipped(true)
                .message("Network access provider unsupported for current platform; skipping firewall preparation")
                .build();
    }
}
