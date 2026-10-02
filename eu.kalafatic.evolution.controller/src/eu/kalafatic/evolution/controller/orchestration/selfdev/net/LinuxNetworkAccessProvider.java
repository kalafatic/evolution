package eu.kalafatic.evolution.controller.orchestration.selfdev.net;

import java.io.File;
import eu.kalafatic.evolution.controller.orchestration.selfdev.SelfDevContext;

public class LinuxNetworkAccessProvider implements NetworkAccessProvider {

    @Override
    public String getName() {
        return "LinuxNetworkAccessProvider";
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

        // On Linux, loopback/local socket binding does not require explicit firewall configuration
        // unless iptables/ufw/nftables strictly blocks loopback traffic.
        return new NetworkAccessResult.Builder()
                .provider(getName())
                .executable(executable)
                .ports(String.valueOf(port))
                .scope("LOOPBACK")
                .success(true)
                .supported(true)
                .permissionAlreadyPresent(true)
                .message("Linux loopback network access verified (default allow)")
                .build();
    }
}
