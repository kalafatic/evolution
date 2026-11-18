package eu.kalafatic.evolution.controller.harness;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.controller.resource.EvoPath;
import eu.kalafatic.evolution.controller.resource.ResourceManager;
import eu.kalafatic.utils.log.Log;

/**
 * Product-local registry of connected coding applications in EVO (Phase 3, A-14).
 *
 * <p>Manages application descriptors, adapter registration, connection configuration,
 * connection testing, connection status and application selection. Descriptors and the
 * selected application id are persisted as JSON in the EVO data directory, resolved through
 * the central ResourceManager (EVO AGENTS.md §7); when persistence is unavailable the
 * registry degrades to in-memory operation. This is not a cross-product shared service.</p>
 */
public class ConnectedApplicationRegistry {

    private static final ConnectedApplicationRegistry INSTANCE = new ConnectedApplicationRegistry();

    private final Map<String, CodingHarnessAdapter> adapters = new ConcurrentHashMap<>();
    private final Map<String, ConnectedApplicationDescriptor> descriptors = new ConcurrentHashMap<>();
    private volatile String selectedApplicationId;

    private ConnectedApplicationRegistry() {
        loadFromPersistence();
    }

    public static ConnectedApplicationRegistry getInstance() {
        return INSTANCE;
    }

    public void registerAdapter(CodingHarnessAdapter adapter) {
        if (adapter == null) return;
        adapters.put(adapter.getApplicationId(), adapter);

        ConnectedApplicationDescriptor descriptor = descriptors.computeIfAbsent(
                adapter.getApplicationId(),
                id -> new ConnectedApplicationDescriptor(
                        id,
                        adapter.getApplicationName(),
                        adapter.getApplicationType(),
                        adapter.getCapabilities(),
                        true,
                        ConnectedApplicationDescriptor.ApplicationStatus.UNKNOWN,
                        "",
                        new LinkedHashMap<>(),
                        Instant.now(),
                        Instant.now()
                )
        );
        descriptor.setName(adapter.getApplicationName());
        descriptor.setApplicationType(adapter.getApplicationType());
        descriptor.setCapabilities(adapter.getCapabilities());
        persist();
    }

    public void unregisterApplication(String applicationId) {
        adapters.remove(applicationId);
        descriptors.remove(applicationId);
        if (applicationId != null && applicationId.equals(selectedApplicationId)) {
            selectedApplicationId = null;
        }
        persist();
    }

    public List<ConnectedApplicationDescriptor> listApplications() {
        List<ConnectedApplicationDescriptor> list = new ArrayList<>(descriptors.values());
        list.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return list;
    }

    public Optional<ConnectedApplicationDescriptor> findApplication(String applicationId) {
        return Optional.ofNullable(descriptors.get(applicationId));
    }

    public Optional<CodingHarnessAdapter> findAdapter(String applicationId) {
        return Optional.ofNullable(adapters.get(applicationId));
    }

    public void selectApplication(String applicationId) {
        this.selectedApplicationId = (applicationId == null || applicationId.isBlank()) ? null : applicationId;
        persist();
    }

    public Optional<String> getSelectedApplicationId() {
        return Optional.ofNullable(selectedApplicationId);
    }

    /**
     * Returns the selected adapter when it is enabled and available; falls back to the
     * registered DCode adapter (Phase 6 default family harness), then OpenCode, when no
     * explicit selection exists (keeps Developer Mode usable out of the box).
     */
    public Optional<CodingHarnessAdapter> findSelectedAdapter() {
        CodingHarnessAdapter adapter = null;
        if (selectedApplicationId != null) {
            adapter = adapters.get(selectedApplicationId);
        } else {
            // Deterministic fallback: prefer the DCode adapter, then OpenCode
            for (CodingHarnessAdapter candidate : adapters.values()) {
                if ("DCODE".equalsIgnoreCase(candidate.getApplicationType())) {
                    adapter = candidate;
                    break;
                }
            }
            if (adapter == null) {
                for (CodingHarnessAdapter candidate : adapters.values()) {
                    if ("OPENCODE".equalsIgnoreCase(candidate.getApplicationType())) {
                        adapter = candidate;
                        break;
                    }
                }
            }
        }
        if (adapter == null) return Optional.empty();
        ConnectedApplicationDescriptor descriptor = descriptors.get(adapter.getApplicationId());
        if (descriptor != null && !descriptor.isEnabled()) return Optional.empty();
        if (!adapter.isAvailable()) return Optional.empty();
        return Optional.of(adapter);
    }

    public CodingHarnessAdapter.ConnectionTestResult testConnection(String applicationId) {
        CodingHarnessAdapter adapter = adapters.get(applicationId);
        if (adapter == null) {
            return CodingHarnessAdapter.ConnectionTestResult.error("Unknown application: " + applicationId);
        }
        CodingHarnessAdapter.ConnectionTestResult result = adapter.testConnection();
        ConnectedApplicationDescriptor descriptor = descriptors.get(applicationId);
        if (descriptor != null) {
            descriptor.setStatus(result.success()
                    ? ConnectedApplicationDescriptor.ApplicationStatus.CONNECTED
                    : ConnectedApplicationDescriptor.ApplicationStatus.DISCONNECTED);
            descriptor.setStatusMessage(result.message());
            persist();
        }
        return result;
    }

    public void updateConfiguration(String applicationId, Map<String, String> configuration) {
        CodingHarnessAdapter adapter = adapters.get(applicationId);
        if (adapter == null) {
            throw new IllegalArgumentException("Unknown application: " + applicationId);
        }
        if (configuration == null) return;
        adapter.updateConnectionConfiguration(configuration);
        ConnectedApplicationDescriptor descriptor = descriptors.get(applicationId);
        if (descriptor != null) {
            descriptor.setConnectionConfiguration(new LinkedHashMap<>(configuration));
            persist();
        }
    }

    public void setApplicationEnabled(String applicationId, boolean enabled) {
        ConnectedApplicationDescriptor descriptor = descriptors.get(applicationId);
        if (descriptor == null) {
            throw new IllegalArgumentException("Unknown application: " + applicationId);
        }
        descriptor.setEnabled(enabled);
        persist();
    }

    public void clearAll() {
        adapters.clear();
        descriptors.clear();
        selectedApplicationId = null;
        persist();
    }

    // ---------------------------------------------------------------------
    // Persistence (JSON under the EVO data directory, via ResourceManager)
    // ---------------------------------------------------------------------

    private Path persistenceFile() {
        try {
            Path root = ResourceManager.getInstance().getPath(EvoPath.EVO_ROOT);
            return root.resolve("data").resolve("connected-applications.json");
        } catch (Throwable t) {
            return null;
        }
    }

    private synchronized void persist() {
        Path file = persistenceFile();
        if (file == null) return;
        try {
            JSONArray apps = new JSONArray();
            for (ConnectedApplicationDescriptor d : descriptors.values()) {
                JSONObject obj = new JSONObject();
                obj.put("id", d.getId());
                obj.put("name", d.getName());
                obj.put("applicationType", d.getApplicationType());
                obj.put("capabilities", new JSONArray(d.getCapabilities()));
                obj.put("enabled", d.isEnabled());
                obj.put("status", d.getStatus().name());
                obj.put("statusMessage", d.getStatusMessage());
                obj.put("connectionConfiguration", new JSONObject(d.getConnectionConfiguration()));
                obj.put("createdAt", d.getCreatedAt().toString());
                obj.put("updatedAt", d.getUpdatedAt().toString());
                apps.put(obj);
            }
            JSONObject root = new JSONObject();
            root.put("selectedApplicationId", selectedApplicationId == null ? JSONObject.NULL : selectedApplicationId);
            root.put("applications", apps);
            Files.createDirectories(file.getParent());
            Files.writeString(file, root.toString(2), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log("Failed to persist connected-application registry: " + e.getMessage());
        }
    }

    private synchronized void loadFromPersistence() {
        Path file = persistenceFile();
        if (file == null || !Files.exists(file)) return;
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            JSONObject root = new JSONObject(content);
            if (root.has("selectedApplicationId") && !root.isNull("selectedApplicationId")) {
                this.selectedApplicationId = root.getString("selectedApplicationId");
            }
            JSONArray apps = root.optJSONArray("applications");
            if (apps != null) {
                for (int i = 0; i < apps.length(); i++) {
                    JSONObject obj = apps.getJSONObject(i);
                    List<String> capabilities = new ArrayList<>();
                    JSONArray caps = obj.optJSONArray("capabilities");
                    if (caps != null) {
                        for (int c = 0; c < caps.length(); c++) {
                            capabilities.add(caps.getString(c));
                        }
                    }
                    Map<String, String> config = new LinkedHashMap<>();
                    JSONObject cfg = obj.optJSONObject("connectionConfiguration");
                    if (cfg != null) {
                        java.util.Iterator<?> keys = cfg.keys();
                        while (keys.hasNext()) {
                            String key = String.valueOf(keys.next());
                            config.put(key, cfg.optString(key));
                        }
                    }
                    ConnectedApplicationDescriptor descriptor = new ConnectedApplicationDescriptor(
                            obj.getString("id"),
                            obj.optString("name"),
                            obj.optString("applicationType"),
                            capabilities,
                            obj.optBoolean("enabled", true),
                            safeStatus(obj.optString("status")),
                            obj.optString("statusMessage"),
                            config,
                            safeInstant(obj.optString("createdAt")),
                            safeInstant(obj.optString("updatedAt"))
                    );
                    descriptors.put(descriptor.getId(), descriptor);
                }
            }
        } catch (Throwable e) {
            log("Failed to load connected-application registry: " + e.getMessage());
        }
    }

    private static ConnectedApplicationDescriptor.ApplicationStatus safeStatus(String status) {
        try {
            return status != null ? ConnectedApplicationDescriptor.ApplicationStatus.valueOf(status)
                    : ConnectedApplicationDescriptor.ApplicationStatus.UNKNOWN;
        } catch (IllegalArgumentException e) {
            return ConnectedApplicationDescriptor.ApplicationStatus.UNKNOWN;
        }
    }

    private static Instant safeInstant(String value) {
        try {
            return value != null ? Instant.parse(value) : Instant.now();
        } catch (Throwable t) {
            return Instant.now();
        }
    }

    private static void log(String message) {
        try {
            Log.log("[HARNESS-REGISTRY] " + message);
        } catch (Throwable ignored) {}
    }
}
