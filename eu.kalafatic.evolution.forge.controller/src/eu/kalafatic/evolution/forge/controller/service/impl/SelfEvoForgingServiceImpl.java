package eu.kalafatic.evolution.forge.controller.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.forge.controller.api.ForgeJob;
import eu.kalafatic.evolution.forge.controller.api.ForgeJob.ModelObjective;
import eu.kalafatic.evolution.forge.controller.service.ForgeOrchestrator;
import eu.kalafatic.evolution.forge.controller.service.SelfEvoForgingService;

public class SelfEvoForgingServiceImpl implements SelfEvoForgingService {

    public static final Integer MCP_PORT = 38080;
    public static final String MCP_ADDRESS = "localhost:" + MCP_PORT;
    public static final String MCP_URL = "http://" + MCP_ADDRESS + "/mcp";

    private final Map<String, ForgingStats> sessionStats = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ForgeOrchestrator forgeOrchestrator = new ForgeOrchestratorImpl();

    private JSONObject getUiStateViaReflection(String sessionId) {
        try {
            Class<?> clazz = Class.forName("eu.kalafatic.evolution.controller.orchestration.ForgeSessionManager");
            Object manager = clazz.getMethod("getInstance").invoke(null);
            Object jsonResult = clazz.getMethod("getUiState", String.class).invoke(manager, sessionId);
            return new JSONObject(jsonResult.toString());
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    @Override
    public void startForging(String sessionId, Path projectPath, List<String> dataSources) throws Exception {
        updateStats(sessionId, new ForgingStats("STARTING", 0, 0, 0, 0, 0.0, "0", ""));

        executor.submit(() -> {
            try {
                JSONObject uiState = getUiStateViaReflection(sessionId);

                ForgeJob job = new ForgeJob(sessionId);

                long minBytes = uiState.optLong("targetUsableBytes", uiState.optLong("minimumUsableBytes", 524_288_000L));
                job.setRequestedMinimumUsableBytes(minBytes);

                // Populate objective
                String objStr = uiState.optString("objective", "AUTO").toUpperCase();
                try {
                    job.setObjective(ModelObjective.valueOf(objStr));
                } catch (Exception e) {
                    job.setObjective(ModelObjective.AUTO);
                }

                // Populate sources
                List<String> activeSources = new ArrayList<>();
                JSONArray datasetsArr = null;
                if (uiState.has("datasets")) {
                    Object dsObj = uiState.get("datasets");
                    if (dsObj instanceof JSONArray) {
                        datasetsArr = (JSONArray) dsObj;
                    } else if (dsObj instanceof String && !((String) dsObj).trim().isEmpty()) {
                        try {
                            datasetsArr = new JSONArray((String) dsObj);
                        } catch (Exception ex) {}
                    }
                }

                if (datasetsArr != null && datasetsArr.length() > 0) {
                    for (int i = 0; i < datasetsArr.length(); i++) {
                        JSONObject dsItem = datasetsArr.optJSONObject(i);
                        if (dsItem != null && dsItem.optBoolean("checked", true)) {
                            String pStr = dsItem.optString("path", "").trim();
                            if (!pStr.isEmpty()) {
                                activeSources.add(pStr);
                            }
                        }
                    }
                }

                if (activeSources.isEmpty() && dataSources != null && !dataSources.isEmpty()) {
                    activeSources.addAll(dataSources);
                }

                if (activeSources.isEmpty()) {
                    String codebase = getCodebasePathViaReflection();
                    if (codebase != null) {
                        activeSources.add(codebase);
                    } else {
                        activeSources.add(System.getProperty("user.dir"));
                    }
                }

                job.setSourcePaths(activeSources);

                // Populate model hyperparameters
                double lr = uiState.optDouble("lr", 0.01);
                int epochs = uiState.optInt("epochs", 1);
                String modelSizeName = uiState.optString("modelSize", "SMALL").toUpperCase();
                int hiddenSize = uiState.optInt("hidden_size", 512);
                int layers = uiState.optInt("layers", 8);
                int heads = uiState.optInt("heads", 8);
                int dff = hiddenSize * 4;
                int maxSeqLen = 1024;

                int[] resolvedParams = new int[] { hiddenSize, layers, heads, dff, maxSeqLen };
                resolveModelSizePreset(modelSizeName, resolvedParams);

                job.getModelConfig().setModelSize(modelSizeName);
                job.getModelConfig().setHiddenSize(resolvedParams[0]);
                job.getModelConfig().setLayers(resolvedParams[1]);
                job.getModelConfig().setHeads(resolvedParams[2]);
                job.getModelConfig().setDff(resolvedParams[3]);
                job.getModelConfig().setMaxSeqLen(resolvedParams[4]);

                job.getTrainingConfig().setLearningRate(lr);
                job.getTrainingConfig().setEpochs(epochs);

                // Delegate complete job execution to the authoritative ForgeOrchestrator
                forgeOrchestrator.executeJob(job, projectPath);

                String runFolderStr = job.getRunDirectory() != null ? job.getRunDirectory().toAbsolutePath().toString() : "";
                updateStats(sessionId, new ForgingStats("COMPLETE", 100, job.getSourcePaths().size(), job.getSourceProfiles().size(), 100, 0.05, "DONE", runFolderStr));

            } catch (Exception e) {
                e.printStackTrace();
                updateStats(sessionId, new ForgingStats("ERROR", 0, 0, 0, 0, 0.0, "ERR", ""));
            }
        });
    }

    public String createModel(String baseUrl, String modelName, String modelfileContent) throws Exception {
        String createUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/create";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", modelName);
        jsonObject.put("modelfile", modelfileContent);
        jsonObject.put("stream", false);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(createUrl))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                .build();

        HttpResponse<String> response = createClient().send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama create model error: " + response.statusCode() + " - " + response.body());
        }
        return response.body();
    }

    private HttpClient createClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    private void updateStats(String sessionId, ForgingStats stats) {
        sessionStats.put(sessionId, stats);
    }

    @Override
    public ForgingStats getStats(String sessionId) {
        return sessionStats.getOrDefault(sessionId, new ForgingStats("IDLE", 0, 0, 0, 0, 0.0, "0", ""));
    }

    @Override
    public void stopForging(String sessionId) {
        sessionStats.remove(sessionId);
    }

    private String getCodebasePathViaReflection() {
        try {
            Class<?> clazz = Class.forName("eu.kalafatic.evolution.controller.manager.ProjectModelManager");
            return (String) clazz.getMethod("getCodebasePath").invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void resolveModelSizePreset(String modelSizeName, int[] targetParams) {
        try {
            Class<?> enumClass = Class.forName("eu.kalafatic.evolution.controller.manager.ModelSizePreset$Size");
            Object[] enumConstants = enumClass.getEnumConstants();
            Object matchedEnum = null;
            if (enumConstants != null) {
                for (Object constant : enumConstants) {
                    String name = (String) enumClass.getMethod("name").invoke(constant);
                    String displayName = (String) enumClass.getMethod("getDisplayName").invoke(constant);
                    if (name.equalsIgnoreCase(modelSizeName) ||
                        modelSizeName.toUpperCase().contains(name.toUpperCase()) ||
                        (displayName != null && (displayName.equalsIgnoreCase(modelSizeName) || displayName.toUpperCase().contains(modelSizeName.toUpperCase()) || modelSizeName.toUpperCase().contains(displayName.toUpperCase())))) {
                        matchedEnum = constant;
                        break;
                    }
                }
            }
            if (matchedEnum != null) {
                int dModel = (Integer) enumClass.getMethod("getDModel").invoke(matchedEnum);
                int numBlocks = (Integer) enumClass.getMethod("getNumBlocks").invoke(matchedEnum);
                int numHeads = (Integer) enumClass.getMethod("getNumHeads").invoke(matchedEnum);
                int dff = (Integer) enumClass.getMethod("getDff").invoke(matchedEnum);
                int maxSeqLen = (Integer) enumClass.getMethod("getMaxSeqLen").invoke(matchedEnum);

                if (dModel > 0) targetParams[0] = dModel;
                if (numBlocks > 0) targetParams[1] = numBlocks;
                if (numHeads > 0) targetParams[2] = numHeads;
                if (dff > 0) targetParams[3] = dff;
                if (maxSeqLen > 0) targetParams[4] = maxSeqLen;
            }
        } catch (Exception e) {
            if (modelSizeName.contains("NANO")) {
                targetParams[0] = 128; targetParams[1] = 3; targetParams[2] = 4; targetParams[3] = 256; targetParams[4] = 128;
            } else if (modelSizeName.contains("MICRO")) {
                targetParams[0] = 256; targetParams[1] = 4; targetParams[2] = 8; targetParams[3] = 512; targetParams[4] = 256;
            } else if (modelSizeName.contains("TINY")) {
                targetParams[0] = 384; targetParams[1] = 6; targetParams[2] = 8; targetParams[3] = 1024; targetParams[4] = 512;
            } else if (modelSizeName.contains("SMALL")) {
                targetParams[0] = 512; targetParams[1] = 8; targetParams[2] = 8; targetParams[3] = 2048; targetParams[4] = 1024;
            } else if (modelSizeName.contains("MEDIUM")) {
                targetParams[0] = 768; targetParams[1] = 12; targetParams[2] = 12; targetParams[3] = 3072; targetParams[4] = 2048;
            } else if (modelSizeName.contains("LARGE")) {
                targetParams[0] = 1024; targetParams[1] = 16; targetParams[2] = 16; targetParams[3] = 4096; targetParams[4] = 4096;
            }
        }
    }
}
