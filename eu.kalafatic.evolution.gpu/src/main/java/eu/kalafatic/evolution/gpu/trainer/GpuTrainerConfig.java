package eu.kalafatic.evolution.gpu.trainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;

/**
 * Isolated configuration model for GPU training process.
 */
public class GpuTrainerConfig {

    private String modelName = "evo_gpu_model";
    private String workDir = "gpu-work";
    private String outputDir = "gpu-output";
    private int epochs = 3;
    private int batchSize = 4;
    private float learningRate = 0.001f;
    private int vocabSize = 1000;
    private int embedDim = 64;
    private int numLayers = 2;
    private int numHeads = 4;
    private boolean preferCuda = true;

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getWorkDir() { return workDir; }
    public void setWorkDir(String workDir) { this.workDir = workDir; }

    public String getOutputDir() { return outputDir; }
    public void setOutputDir(String outputDir) { this.outputDir = outputDir; }

    public int getEpochs() { return epochs; }
    public void setEpochs(int epochs) { this.epochs = epochs; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }

    public float getLearningRate() { return learningRate; }
    public void setLearningRate(float learningRate) { this.learningRate = learningRate; }

    public int getVocabSize() { return vocabSize; }
    public void setVocabSize(int vocabSize) { this.vocabSize = vocabSize; }

    public int getEmbedDim() { return embedDim; }
    public void setEmbedDim(int embedDim) { this.embedDim = embedDim; }

    public int getNumLayers() { return numLayers; }
    public void setNumLayers(int numLayers) { this.numLayers = numLayers; }

    public int getNumHeads() { return numHeads; }
    public void setNumHeads(int numHeads) { this.numHeads = numHeads; }

    public boolean isPreferCuda() { return preferCuda; }
    public void setPreferCuda(boolean preferCuda) { this.preferCuda = preferCuda; }

    public static GpuTrainerConfig load(File configFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(configFile, GpuTrainerConfig.class);
    }

    public void save(File configFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(configFile, this);
    }
}
