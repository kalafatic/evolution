package eu.kalafatic.evolution.controller.discovery;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.kalafatic.evolution.model.orchestration.NetworkEntry;
import eu.kalafatic.evolution.model.orchestration.OrchestrationFactory;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

/**
 * Discovery service that scans the codebase for hardcoded addresses and collects them into the model.
 * @evo:20:A reason=network-discovery
 */
public class NetworkDiscoveryService {

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[a-zA-Z0-9\\.\\-:]+)");

    public void discoverAndSync(Orchestrator orchestrator, File rootDir) {
        if (orchestrator == null || rootDir == null || !rootDir.exists()) return;

        Set<String> discoveredUrls = new HashSet<>();
        scanDirectory(rootDir, discoveredUrls);

        // Sync with model
        List<NetworkEntry> existing = new ArrayList<>(orchestrator.getNetworkEntries());

        for (String urlStr : discoveredUrls) {
            if (isAlreadyPresent(existing, urlStr)) continue;

            try {
                URI uri = new URI(urlStr);
                NetworkEntry entry = OrchestrationFactory.eINSTANCE.createNetworkEntry();
                entry.setAddress(urlStr);
                entry.setHost(uri.getHost());
                entry.setPort(uri.getPort() != -1 ? uri.getPort() : (urlStr.startsWith("https") ? 443 : 80));
                entry.setType("Discovered");
                entry.setNote("Found in source code analysis");

                orchestrator.getNetworkEntries().add(entry);
            } catch (Exception e) {
                // Ignore invalid URIs
            }
        }
    }

    private boolean isAlreadyPresent(List<NetworkEntry> existing, String url) {
        return existing.stream().anyMatch(e -> url.equals(entryToUrl(e)));
    }

    private String entryToUrl(NetworkEntry e) {
        if (e.getAddress() != null) return e.getAddress();
        return (e.getPort() == 443 ? "https://" : "http://") + e.getHost() + ":" + e.getPort();
    }

    private void scanDirectory(File dir, Set<String> urls) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                if (f.getName().equals("target") || f.getName().equals(".git") || f.getName().equals("bin")) continue;
                scanDirectory(f, urls);
            } else if (f.getName().endsWith(".java") || f.getName().endsWith(".properties") || f.getName().endsWith(".xml") || f.getName().endsWith(".json")) {
                scanFile(f, urls);
            }
        }
    }

    private void scanFile(File f, Set<String> urls) {
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = URL_PATTERN.matcher(line);
                while (m.find()) {
                    String url = m.group(1);
                    // Filter out common false positives or generic schemas
                    if (url.length() < 12) continue;
                    if (url.contains("example.com")) continue;
                    if (url.contains("schema.omg.org")) continue;
                    if (url.contains("www.w3.org")) continue;
                    urls.add(url);
                }
            }
        } catch (Exception e) {
            // Skip unreadable files
        }
    }
}
