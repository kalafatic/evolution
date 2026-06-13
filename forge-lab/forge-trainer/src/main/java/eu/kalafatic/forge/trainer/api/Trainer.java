package eu.kalafatic.forge.trainer.api;

public interface Trainer {
    void train(int epochs);
    void step();
}
