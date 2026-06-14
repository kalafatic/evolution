package eu.kalafatic.forge.model;

import java.util.List;

public interface ForgeModel {
    String getId();
    void setId(String id);
    String getName();
    void setName(String name);
    String getType();
    void setType(String type);
    String getVersion();
    void setVersion(String version);

    List<SubModel> getSubModels();
    List<ModelConnection> getModelConnections();

    String getLineageId();
    void setLineageId(String lineageId);
    double getFitnessScore();
    void setFitnessScore(double fitnessScore);
    ModelStatus getStatus();
    void setStatus(ModelStatus status);

    ModelGenome getGenome();
    void setGenome(ModelGenome genome);
    List<EvolutionSnapshot> getEvolutionSnapshots();
}
