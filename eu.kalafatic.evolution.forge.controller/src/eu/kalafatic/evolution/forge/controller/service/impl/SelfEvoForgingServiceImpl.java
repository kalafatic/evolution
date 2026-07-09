package eu.kalafatic.evolution.forge.controller.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;
import eu.kalafatic.evolution.forge.controller.service.OllamaService;
import eu.kalafatic.evolution.forge.controller.service.SelfEvoForgingService;
import eu.kalafatic.evolution.forge.data.impl.DatasetBuilder;
import eu.kalafatic.evolution.forge.data.impl.MarkdownCleaner;
import eu.kalafatic.evolution.forge.data.impl.MarkdownLoader;
import eu.kalafatic.evolution.forge.data.impl.VocabularyBuilder;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;

public class SelfEvoForgingServiceImpl implements SelfEvoForgingService {
	
	public static final Integer MCP_PORT = 38080;
	public static final String MCP_ADDRESS = "localhost:" + MCP_PORT;
	public static final String MCP_URL = "http://"+MCP_ADDRESS+"/mcp";
	
    private final Map<String, ForgingStats> sessionStats = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public void startForging(String sessionId, Path projectPath) throws Exception {
        updateStats(sessionId, new ForgingStats("STARTING", 0, 0, 0, 0, 0.0, "0", ""));

        executor.submit(() -> {
            long timestamp = System.currentTimeMillis();
            Path runFolder = projectPath.resolve("dist/forging-" + sessionId + "-" + timestamp);
            Path logFile = runFolder.resolve("forging.log");
            try {
                Files.createDirectories(runFolder);

                JSONObject infoJson = new JSONObject();
                infoJson.put("sessionId", sessionId);
                infoJson.put("modelType", "SELF_EVO");
                infoJson.put("projectPath", projectPath.toAbsolutePath().toString());
                infoJson.put("timestamp", timestamp);
                infoJson.put("startTime", new java.util.Date(timestamp).toString());
                Files.writeString(runFolder.resolve("session_info.json"), infoJson.toString(4));

                logToFile(logFile, "Starting forging session: " + sessionId + " at " + infoJson.getString("startTime"));

                // REAL PIPELINE IMPLEMENTATION
                MarkdownLoader loader = new MarkdownLoader();
                MarkdownCleaner cleaner = new MarkdownCleaner();
                
                updateStats(sessionId, new ForgingStats("SCANNING", 10, 0, 0, 0, 0.0, "0", runFolder.toAbsolutePath().toString()));
                logToFile(logFile, "Stage: SCANNING");
                String corpus = loader.loadFromDirectory(projectPath);
                String cleanCorpus = cleaner.clean(corpus);
                logToFile(logFile, "Scanning complete. Corpus length: " + corpus.length() + " chars, Cleaned length: " + cleanCorpus.length() + " chars.");

                JSONObject stage1 = new JSONObject();
                stage1.put("stage", "SCANNING");
                stage1.put("rawCorpusLength", corpus.length());
                stage1.put("cleanCorpusLength", cleanCorpus.length());
                stage1.put("sample", cleanCorpus.substring(0, Math.min(1000, cleanCorpus.length())));
                Files.writeString(runFolder.resolve("stage_1_scanner_result.json"), stage1.toString(4));
                
                updateStats(sessionId, new ForgingStats("ENHANCING", 30, 0, 0, 0, 0.0, "0", runFolder.toAbsolutePath().toString()));
                logToFile(logFile, "Stage: ENHANCING");
                SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
                tokenizer.train(cleanCorpus, 4096);
                List<Integer> allTokens = tokenizer.encode(cleanCorpus);
                logToFile(logFile, "Tokenization complete. Vocabulary size: " + tokenizer.getVocabSize() + ", Total tokens: " + allTokens.size());
                
                VocabularyBuilder vocabBuilder = new VocabularyBuilder();
                // Map of tokens for vocab builder (simplified)
                Map<String, Integer> vocab = vocabBuilder.buildVocabulary(List.of(cleanCorpus.split("\\s+")), 1);
                
                DatasetBuilder datasetBuilder = new DatasetBuilder();
                List<DatasetBuilder.Sample> samples = datasetBuilder.buildSlidingWindow(allTokens, 16, 8);
                logToFile(logFile, "Dataset builder complete. Generated " + samples.size() + " training samples.");

                JSONObject stage2 = new JSONObject();
                stage2.put("stage", "ENHANCING");
                stage2.put("vocabSize", tokenizer.getVocabSize());
                stage2.put("totalTokens", allTokens.size());
                stage2.put("samplesGenerated", samples.size());
                JSONArray tokenSample = new JSONArray();
                for (int i = 0; i < Math.min(100, allTokens.size()); i++) {
                    tokenSample.put(allTokens.get(i));
                }
                stage2.put("tokenSample", tokenSample);
                Files.writeString(runFolder.resolve("stage_2_enhancer_result.json"), stage2.toString(4));
                
                updateStats(sessionId, new ForgingStats("TRAINING", 60, 0, 0, samples.size(), 0.0, "1/1", runFolder.toAbsolutePath().toString()));
                logToFile(logFile, "Stage: TRAINING. Training EvoLlmModel with sliding window samples...");
                EvoLlmModel model = new EvoLlmModel(tokenizer.getVocabSize(), 128, 4, 2, 512, 16);
                EvoLlmTrainer trainer = new EvoLlmTrainer(model);
                trainer.train(samples, 1);
                logToFile(logFile, "Training complete.");

                JSONObject stage3 = new JSONObject();
                stage3.put("stage", "TRAINING");
                stage3.put("samplesTrained", samples.size());
                stage3.put("epochs", 1);
                JSONObject arch = new JSONObject();
                arch.put("vocabSize", tokenizer.getVocabSize());
                arch.put("hiddenSize", 128);
                arch.put("attentionHeads", 4);
                arch.put("layers", 2);
                stage3.put("architecture", arch);
                Files.writeString(runFolder.resolve("stage_3_trainer_result.json"), stage3.toString(4));
                
                updateStats(sessionId, new ForgingStats("EXPORTING", 80, 0, 0, samples.size(), 0.0, "1/1", runFolder.toAbsolutePath().toString()));
                logToFile(logFile, "Stage: EXPORTING. Exporting model LoRA adapters...");
                OllamaExporter exporter = new OllamaExporter();
                Path exportPath = projectPath.resolve("dist/evo-" + sessionId);
                String modelName = "evo-" + sessionId;
                exporter.export(modelName, exportPath, model);
                logToFile(logFile, "Export complete. Model output written to: " + exportPath.toAbsolutePath().toString());

                JSONObject stage4 = new JSONObject();
                stage4.put("stage", "EXPORTING");
                stage4.put("modelName", modelName);
                stage4.put("exportPath", exportPath.toAbsolutePath().toString());
                stage4.put("success", true);
                Files.writeString(runFolder.resolve("stage_4_exporter_result.json"), stage4.toString(4));

                updateStats(sessionId, new ForgingStats("EXPORT_GGUF", 90, 0, 0, samples.size(), 0.0, "OLLAMA", runFolder.toAbsolutePath().toString()));
                logToFile(logFile, "Stage: EXPORT_GGUF. Registering model in Ollama...");
               
                // For 'SELF_EVO' interactive demo consistency, ensure we register the model as 'evo'
                String targetName = "evo";
                Path modelfilePath = exportPath.resolve("Modelfile");
                if (Files.exists(modelfilePath)) {
                    try {
                        String modelfileContent = Files.readString(modelfilePath);
                        String ollamaUrl = "http://localhost:11434";
                        boolean pingOk = pingOllama(ollamaUrl);
                        boolean registered = false;
                        String baseModelUsed = "llama3.2:3b";

                        if (pingOk) {
                            String availableModel = getFirstAvailableModel(ollamaUrl);
                            if (availableModel != null && !availableModel.equals("llama3.2:3b")) {
                                baseModelUsed = availableModel;
                                logToFile(logFile, "[EXPORT_GGUF] Rewriting FROM in Modelfile from llama3.2:3b to " + availableModel);
                                modelfileContent = modelfileContent.replaceAll("(?m)^FROM\\s+llama3.2:3b", "FROM " + availableModel);
                                Files.writeString(modelfilePath, modelfileContent);
                            }
                            logToFile(logFile, "[EXPORT_GGUF] Registering model in Ollama as '" + targetName + "'...");
                            createModel(ollamaUrl, targetName, modelfileContent);
                            logToFile(logFile, "[EXPORT_GGUF] Model registered successfully.");
                            registered = true;
                        } else {
                            logToFile(logFile, "[EXPORT_GGUF] Ollama is not running on " + ollamaUrl + ", skipping model registration.");
                        }

                        JSONObject stage5 = new JSONObject();
                        stage5.put("stage", "OLLAMA_REGISTRATION");
                        stage5.put("targetModel", targetName);
                        stage5.put("ollamaOnline", pingOk);
                        stage5.put("baseModelUsed", baseModelUsed);
                        stage5.put("registrationSuccess", registered);
                        Files.writeString(runFolder.resolve("stage_5_registration_result.json"), stage5.toString(4));
                    } catch (Exception ex) {
                        logToFile(logFile, "[EXPORT_GGUF] Ollama registration failed (non-blocking): " + ex.getMessage());

                        JSONObject stage5 = new JSONObject();
                        stage5.put("stage", "OLLAMA_REGISTRATION");
                        stage5.put("targetModel", targetName);
                        stage5.put("error", ex.getMessage());
                        stage5.put("registrationSuccess", false);
                        Files.writeString(runFolder.resolve("stage_5_registration_result.json"), stage5.toString(4));
                    }
                }
                
                logToFile(logFile, "Stage: COMPLETE. Forging process completed successfully!");
                updateStats(sessionId, new ForgingStats("COMPLETE", 100, 0, 0, samples.size(), 0.0, "DONE", runFolder.toAbsolutePath().toString()));

            } catch (Exception e) {
                logToFile(logFile, "Stage: ERROR. Forging process failed: " + e.getMessage());
                try {
                    JSONObject errorObj = new JSONObject();
                    errorObj.put("sessionId", sessionId);
                    errorObj.put("error", e.getMessage());
                    java.io.StringWriter sw = new java.io.StringWriter();
                    e.printStackTrace(new java.io.PrintWriter(sw));
                    errorObj.put("stackTrace", sw.toString());
                    Files.writeString(runFolder.resolve("error_result.json"), errorObj.toString(4));
                } catch (Exception ex) {}
                e.printStackTrace();
                updateStats(sessionId, new ForgingStats("ERROR", 0, 0, 0, 0, 0.0, "ERR", runFolder != null ? runFolder.toAbsolutePath().toString() : ""));
            }
        });
    }
    
    /**
   * Creates a new model in Ollama from a Modelfile content.
   * @param modelName The name of the model to create.
   * @param modelfileContent The content of the Modelfile.
   * @return The status response from Ollama.
   */
  public String createModel(String baseUrl, String modelName, String modelfileContent) throws Exception {
      String createUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/create";

      JSONObject jsonObject = new JSONObject();
      jsonObject.put("model", modelName);
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

      //refreshModels();
      return response.body();
  }

  private boolean pingOllama(String baseUrl) {
      try {
          HttpRequest request = HttpRequest.newBuilder()
                  .uri(URI.create(baseUrl))
                  .timeout(Duration.ofSeconds(2))
                  .GET()
                  .build();
          HttpResponse<Void> response = createClient().send(request, HttpResponse.BodyHandlers.discarding());
          return response.statusCode() == 200;
      } catch (Exception e) {
          return false;
      }
  }

  private String getFirstAvailableModel(String baseUrl) {
      try {
          String tagsUrl = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/tags";
          HttpRequest request = HttpRequest.newBuilder()
                  .uri(URI.create(tagsUrl))
                  .timeout(Duration.ofSeconds(5))
                  .GET()
                  .build();

          HttpResponse<String> response = createClient().send(request, HttpResponse.BodyHandlers.ofString());
          if (response.statusCode() == 200) {
              JSONObject obj = new JSONObject(response.body());
              JSONArray models = obj.getJSONArray("models");
              if (models.length() > 0) {
                  // See if llama3.2:3b is in the list
                  for (int i = 0; i < models.length(); i++) {
                      JSONObject m = models.getJSONObject(i);
                      String name = m.getString("name");
                      if (name.contains("llama3.2:3b")) {
                          return "llama3.2:3b";
                      }
                  }
                  // Otherwise, return the first one available
                  return models.getJSONObject(0).getString("name");
              }
          }
      } catch (Exception e) {
          // Ignore, default to llama3.2:3b
      }
      return "llama3.2:3b";
  }
  
  private HttpClient createClient() {
      return HttpClient.newBuilder()
              .connectTimeout(Duration.ofSeconds(10))
              .followRedirects(HttpClient.Redirect.NORMAL)
              .build();
  }

  
  /**
   * Forces a refresh of the models list.
   */
//  public List<OllamaModel> refreshModels() {
//      List<OllamaModel> result = new ArrayList<>();
//      try {
//          String tagsUrl = this.baseUrl + (this.baseUrl.endsWith("/") ? "" : "/") + "api/tags";
//          HttpRequest request = HttpRequest.newBuilder()
//                  .uri(URI.create(tagsUrl))
//                  .timeout(Duration.ofSeconds(10))
//                  .GET()
//                  .build();
//
//          HttpResponse<String> response = createClient().send(request, HttpResponse.BodyHandlers.ofString());
//          if (response.statusCode() == 200) {
//              JSONObject obj = new JSONObject(response.body());
//              JSONArray models = obj.getJSONArray("models");
//              for (int i = 0; i < models.length(); i++) {
//                  JSONObject m = models.getJSONObject(i);
//                  String name = m.getString("name");
//                  long size = m.optLong("size", 0);
//                  result.add(new OllamaModel(name, size));
//              }
//              this.cachedModels = Collections.unmodifiableList(result);
//              this.lastModelRefresh = System.currentTimeMillis();
//          }
//      } catch (Exception e) {
//          // silent fail or return empty
//      }
//      return result;
//  }

  private void logToFile(Path logFile, String msg) {
      try {
          String formatted = String.format("[%s] %s\n", java.time.Instant.now().toString(), msg);
          Files.writeString(logFile, formatted, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
          System.out.println(msg);
      } catch (Exception e) {
          e.printStackTrace();
      }
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
}
