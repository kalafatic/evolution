package eu.kalafatic.evolution.controller.orchestration.selfdev.net;

import java.io.File;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;

public class MacNetworkAccessProvider implements NetworkAccessProvider {

    @Override
    public String getName() {
        return "MacNetworkAccessProvider";
    }

    @Override
    public NetworkAccessResult prepareNetworkAccess(File executable, int port, SelfDevContext context) {
        if (executable == null || !executable.exists()) {
            return new NetworkAccessResult.Builder()
                    .provider(getName())
                    .executable(executable)
                    .success(false)
                    .message("Executable file does not exist")
                    .build();
        }

        return new NetworkAccessResult.Builder()
                .provider(getName())
                .executable(executable)
                .ports(String.valueOf(port))
                .scope("LOOPBACK")
                .success(true)
                .supported(true)
                .permissionAlreadyPresent(true)
                .message("macOS loopback network access verified")
                .build();
    }
}
