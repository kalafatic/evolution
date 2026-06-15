package eu.kalafatic.evolution.forge.trainer.impl;

import eu.kalafatic.evolution.forge.trainer.api.Trainer;
import eu.kalafatic.evolution.forge.controller.service.EvolutionService;

public class EvolutionaryTrainer implements Trainer {
    private final EvolutionService evolutionService;
    private final String sessionId;
    private final String modelId;

    public EvolutionaryTrainer(EvolutionService evolutionService, String sessionId, String modelId) {
        this.evolutionService = evolutionService;
        this.sessionId = sessionId;
        this.modelId = modelId;
    }

    @Override
    public void train(int epochs) {
        for (int i = 0; i < epochs; i++) {
            step();
            if (i % 10 == 0) {
                evaluateFitness();
                evolve();
            }
        }
    }

    @Override
    public void step() {
        // Forward/Backward pass
    }

    @Override
    public void evolve() {
        evolutionService.evolve(sessionId, modelId);
    }

    @Override
    public void evaluateFitness() {
        // Compute fitness score based on loss
    }
}
