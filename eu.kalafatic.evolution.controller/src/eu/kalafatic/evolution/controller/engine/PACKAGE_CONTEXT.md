# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/engine/

## Domain: general

## Components
* `NeuronEngine.java`: package eu.kalafatic.evolution.controller.engine; import eu.kalafatic.evolution.model.orchestration.NeuronType; import java.util.Random; import java.util.stream.Collectors; import java.util.Arrays; public class NeuronEngine { public String runModel(NeuronType type, String modelName, String prompt) { switch (type) { case MLP: return runMLP(modelName, prompt); case CNN: return runCNN(modelName, prompt); case RNN: return runRNN(modelName, prompt); case LSTM: return runLSTM(modelName, prompt); case TRANSFORMER: return runTransformer(modelName, prompt); default: return "Unknown model type: " + type;
