package eu.kalafatic.evolution.forge.model;

public interface ModelGenome {
    String getMutationHistory();
    void setMutationHistory(String mutationHistory);
    String getStructuralChanges();
    void setStructuralChanges(String structuralChanges);
    String getFitnessHistory();
    void setFitnessHistory(String fitnessHistory);
}
