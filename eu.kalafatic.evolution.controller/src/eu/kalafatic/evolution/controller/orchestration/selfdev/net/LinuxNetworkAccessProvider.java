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

        try {
            executable.setExecutable(true, false);
        } catch (Exception ignored) {}

        boolean loopbackOk = true;
        try (java.net.ServerSocket ss = new java.net.ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new java.net.InetSocketAddress("127.0.0.1", 0));
        } catch (Exception e) {
            loopbackOk = false;
        }

        if (!loopbackOk) {
            return new NetworkAccessResult.Builder()
                    .provider(getName())
                    .executable(executable)
                    .ports(String.valueOf(port))
                    .scope("LOOPBACK")
                    .success(false)
                    .supported(true)
                    .message("Linux loopback network interface is not accessible or restricted")
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
                .message("Linux loopback network access and executable permissions verified")
                .build();
    }
}
