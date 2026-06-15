package eu.kalafatic.evolution.forge.model;

import java.util.List;

public interface ForgeSession {
    String getId();
    void setId(String id);
    String getName();
    void setName(String name);
    ForgeModel getActiveModel();
    void setActiveModel(ForgeModel model);
    List<ForgeDataset> getDatasets();
    List<ForgeTrainingRun> getTrainingRuns();
    List<ForgeSnapshot> getSnapshots();
}
