package eu.kalafatic.evolution.selfdev.genome.hub;

import java.io.File;

import eu.kalafatic.evolution.selfdev.genome.core.GenomeArtifact;
import eu.kalafatic.evolution.selfdev.genome.core.MediatedPackageArtifact;
import eu.kalafatic.evolution.selfdev.genome.event.GenomeEvent;
import eu.kalafatic.evolution.selfdev.genome.mediation.MediatedPackageProcessor;
import eu.kalafatic.evolution.selfdev.genome.repository.GenomeRepository;
import eu.kalafatic.evolution.selfdev.genome.repository.LocalGenomeRepository;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.SecondhandUpgradeEngine;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.UpgradeContext;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.UpgradePlan;



public class SelfDevGenomeHub {

    private static SelfDevGenomeHub instance;

    public static synchronized SelfDevGenomeHub getInstance() {
        if (instance == null) {
            eu.kalafatic.evolution.selfdev.genome.repository.LocalGenomeRepository repo = new eu.kalafatic.evolution.selfdev.genome.repository.LocalGenomeRepository();
            instance = new SelfDevGenomeHub(
                new LocalGenomeRepository(),
                new GenomeEventBus(),
                new MediatedPackageProcessor(),
                new SecondhandUpgradeEngine()
            );
        }
        return instance;
    }

    private final GenomeRepository repository;
    private final GenomeEventBus eventBus;
    private final MediatedPackageProcessor processor;
    private final SecondhandUpgradeEngine upgradeEngine;

    public SelfDevGenomeHub(
            GenomeRepository repository,
            GenomeEventBus eventBus,
            MediatedPackageProcessor processor,
            SecondhandUpgradeEngine upgradeEngine
    ) {
        this.repository = repository;
        this.eventBus = eventBus;
        this.processor = processor;
        this.upgradeEngine = upgradeEngine;
    }

    public GenomeArtifact uploadMediated(File zipFile, String sourceProject) {

        MediatedPackageArtifact artifact = processor.process(zipFile);

        artifact.setSourceProject(sourceProject);

        repository.save(artifact);

        eventBus.publish(new GenomeEvent(
                "NEW_MEDIATED_PACKAGE",
                artifact.getId(),
                artifact.getTopic()
            ));

        return artifact;
    }

    public GenomeArtifact uploadDiscovery(GenomeArtifact artifact) {

        repository.save(artifact);

        eventBus.publish(new GenomeEvent(
                "NEW_DISCOVERY",
                artifact.getId(),
                artifact.getTopic()
            ));

        return artifact;
    }

    public UpgradePlan generateUpgradePlan(UpgradeContext context) {
        return upgradeEngine.compile(context);
    }

    public GenomeRepository getRepository() {
        return repository;
    }

    public GenomeEventBus getEventBus() {
        return eventBus;
    }

    public MediatedPackageProcessor getProcessor() {
        return processor;
    }

    public SecondhandUpgradeEngine getUpgradeEngine() {
        return upgradeEngine;
    }

    public void updateGenome(File root, String projectName, String version) {
        eu.kalafatic.evolution.selfdev.genome.milestone.MilestoneGenerator mg = new eu.kalafatic.evolution.selfdev.genome.milestone.MilestoneGenerator();
        mg.generateMilestone(root, projectName, version);
    }
}
