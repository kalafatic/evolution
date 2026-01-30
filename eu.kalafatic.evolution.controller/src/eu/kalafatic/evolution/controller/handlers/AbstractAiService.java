package eu.kalafatic.evolution.controller.handlers;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;

public abstract class AbstractAiService implements IAiService {

    protected HttpClient getClient(String proxyUrl) {
        HttpClient.Builder builder = HttpClient.newBuilder();
        if (proxyUrl != null && !proxyUrl.isEmpty()) {
            try {
                URI proxyUri = URI.create(proxyUrl);
                builder.proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return builder.build();
    }
}
