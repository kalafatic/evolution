package eu.kalafatic.evolution.forge.model;

public class ForgeFactory {
    public static final ForgeFactory eINSTANCE = new ForgeFactory();

    public ForgeSession createForgeSession() { return null; }
    public ForgeModel createForgeModel() { return null; }
    public ForgeDataset createForgeDataset() { return null; }
    public ForgeTrainingRun createForgeTrainingRun() { return null; }
    public ForgeSnapshot createForgeSnapshot() { return null; }
    public SubModel createSubModel() { return null; }
    public ModelConnection createModelConnection() { return null; }
    public ModelGenome createModelGenome() { return null; }
    public EvolutionSnapshot createEvolutionSnapshot() { return null; }
}
