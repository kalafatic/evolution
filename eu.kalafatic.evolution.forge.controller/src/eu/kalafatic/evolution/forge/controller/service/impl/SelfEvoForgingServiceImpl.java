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
        updateStats(sessionId, new ForgingStats("STARTING", 0, 0, 0, 0, 0.0, "0"));

        executor.submit(() -> {
            try {
                // REAL PIPELINE IMPLEMENTATION
                MarkdownLoader loader = new MarkdownLoader();
                MarkdownCleaner cleaner = new MarkdownCleaner();
                
                updateStats(sessionId, new ForgingStats("SCANNING", 10, 0, 0, 0, 0.0, "0"));
                String corpus = loader.loadFromDirectory(projectPath);
                String cleanCorpus = cleaner.clean(corpus);
                
                updateStats(sessionId, new ForgingStats("ENHANCING", 30, 0, 0, 0, 0.0, "0"));
                SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
                tokenizer.train(cleanCorpus, 4096);
                List<Integer> allTokens = tokenizer.encode(cleanCorpus);
                
                VocabularyBuilder vocabBuilder = new VocabularyBuilder();
                // Map of tokens for vocab builder (simplified)
                Map<String, Integer> vocab = vocabBuilder.buildVocabulary(List.of(cleanCorpus.split("\\s+")), 1);
                
                DatasetBuilder datasetBuilder = new DatasetBuilder();
                List<DatasetBuilder.Sample> samples = datasetBuilder.buildSlidingWindow(allTokens, 16, 8);
                
                updateStats(sessionId, new ForgingStats("TRAINING", 60, 0, 0, samples.size(), 0.0, "1/1"));
                EvoLlmModel model = new EvoLlmModel(tokenizer.getVocabSize(), 128, 4, 2, 512, 16);
                EvoLlmTrainer trainer = new EvoLlmTrainer(model);
                trainer.train(samples, 1);
                
                updateStats(sessionId, new ForgingStats("EXPORTING", 80, 0, 0, samples.size(), 0.0, "1/1"));
                OllamaExporter exporter = new OllamaExporter();
                Path exportPath = Paths.get("dist/evo-" + sessionId);
                String modelName = "evo-" + sessionId;
                exporter.export(modelName, exportPath, model);

                updateStats(sessionId, new ForgingStats("EXPORT_GGUF", 90, 0, 0, samples.size(), 0.0, "OLLAMA"));
               
                // For 'SELF_EVO' interactive demo consistency, ensure we register the model as 'evo'
                String targetName = "evo";
                Path modelfilePath = exportPath.resolve("Modelfile");
                if (Files.exists(modelfilePath)) {
                    String modelfileContent = Files.readString(modelfilePath);
                    createModel("http://localhost:11434", modelName, modelfileContent);
                }
                
                updateStats(sessionId, new ForgingStats("COMPLETE", 100, 0, 0, samples.size(), 0.0, "DONE"));

            } catch (Exception e) {
                e.printStackTrace();
                updateStats(sessionId, new ForgingStats("ERROR", 0, 0, 0, 0, 0.0, "ERR"));
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
              .timeout(Duration.ofMinutes(5))
              .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
              .build();

      HttpResponse<String> response = createClient().send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
          throw new RuntimeException("Ollama create model error: " + response.statusCode() + " - " + response.body());
      }

      //refreshModels();
      return response.body();
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

    private void updateStats(String sessionId, ForgingStats stats) {
        sessionStats.put(sessionId, stats);
    }

    @Override
    public ForgingStats getStats(String sessionId) {
        return sessionStats.getOrDefault(sessionId, new ForgingStats("IDLE", 0, 0, 0, 0, 0.0, "0"));
    }

    @Override
    public void stopForging(String sessionId) {
        sessionStats.remove(sessionId);
    }
}
