package eu.kalafatic.evolution.selfdev.genome.hub;

import java.io.File;

import eu.kalafatic.evolution.selfdev.genome.core.GenomeArtifact;
import eu.kalafatic.evolution.selfdev.genome.core.MediatedPackageArtifact;
import eu.kalafatic.evolution.selfdev.genome.event.GenomeEvent;
import eu.kalafatic.evolution.selfdev.genome.event.GenomeEventBus;
import eu.kalafatic.evolution.selfdev.genome.mediation.MediatedPackageProcessor;
import eu.kalafatic.evolution.selfdev.genome.repository.GenomeRepository;
import eu.kalafatic.evolution.selfdev.genome.selfupgrade.SecondhandUpgradeEngine;

public class SelfDevGenomeHub {

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
}
