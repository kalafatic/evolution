package eu.kalafatic.evolution.forge.controller.service.impl;

import eu.kalafatic.evolution.forge.controller.service.SelfEvoForgingService;
import eu.kalafatic.evolution.forge.data.impl.*;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;
import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.trainer.impl.llm.EvoLlmTrainer;
import eu.kalafatic.evolution.forge.agent.export.OllamaExporter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SelfEvoForgingServiceImpl implements SelfEvoForgingService {
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

                updateStats(sessionId, new ForgingStats("EXPORTING", 90, 0, 0, samples.size(), 0.0, "DONE"));
                OllamaExporter exporter = new OllamaExporter();
                exporter.export("evo-" + sessionId, Paths.get("dist/evo-" + sessionId), model);

                updateStats(sessionId, new ForgingStats("COMPLETE", 100, 0, 0, samples.size(), 0.0, "DONE"));

            } catch (Exception e) {
                e.printStackTrace();
                updateStats(sessionId, new ForgingStats("ERROR", 0, 0, 0, 0, 0.0, "ERR"));
            }
        });
    }

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
