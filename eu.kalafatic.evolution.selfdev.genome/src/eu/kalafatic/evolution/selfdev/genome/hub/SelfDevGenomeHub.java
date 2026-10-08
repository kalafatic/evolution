package eu.kalafatic.evolution.selfdev.genome.hub;

import java.io.File;

import eu.kalafatic.evolution.selfdev.genome.core.GenomeArtifact;
import eu.kalafatic.evolution.selfdev.genome.core.MediatedPackageArtifact;
import eu.kalafatic.evolution.selfdev.genome.event.GenomeEvent;
import eu.kalafatic.evolution.selfdev.genome.event.GenomeEventBus;
import eu.kalafatic.evolution.selfdev.genome.mediation.MediatedPackageProcessor;
import eu.kalafatic.evolution.selfdev.genome.milestone.GenomeGenerationProgressListener;
import eu.kalafatic.evolution.selfdev.genome.milestone.MilestoneGenerator;
import eu.kalafatic.evolution.selfdev.genome.repository.GenomeRepository;
import eu.kalafatic.evolution.selfdev.genome.repository.LocalGenomeRepository;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.SecondhandUpgradeEngine;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.UpgradeContext;
import eu.kalafatic.evolution.selfdev.genome.model.GenomeUpdateResult;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.UpgradePlan;

public class SelfDevGenomeHub {

    private static SelfDevGenomeHub instance;

    public static synchronized SelfDevGenomeHub getInstance() {
        if (instance == null) {
            LocalGenomeRepository repo = new LocalGenomeRepository();
            instance = new SelfDevGenomeHub(
                repo,
                new eu.kalafatic.evolution.selfdev.genome.event.DefaultGenomeEventBus(),
                new MediatedPackageProcessor(),
                new SecondhandUpgradeEngine(repo)
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

    public synchronized GenomeUpdateResult updateGenome(File root, String projectName, String version) {
        return updateGenome(root, projectName, version, null);
    }

    public synchronized GenomeUpdateResult updateGenome(File root, String projectName, String version, GenomeGenerationProgressListener progressListener) {
        MilestoneGenerator mg = new MilestoneGenerator();
        GenomeUpdateResult result = mg.generateMilestone(root, projectName, version, progressListener);

        if (eventBus != null) {
            eventBus.publish(new GenomeEvent(
                "GENOME_UPDATED",
                projectName != null ? projectName : (root != null ? root.getName() : "root"),
                result.toSummaryString()
            ));
        }

        return result;
    }

    public synchronized GenomeUpdateResult updateGenome(File root) {
        String name = (root != null) ? root.getName() : "EVO";
        return updateGenome(root, name, "v1.0.0", null);
    }

    public synchronized GenomeUpdateResult updateGenome(File root, GenomeGenerationProgressListener progressListener) {
        String name = (root != null) ? root.getName() : "EVO";
        return updateGenome(root, name, "v1.0.0", progressListener);
    }
}
