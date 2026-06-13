# PACKAGE CONTEXT

## Directory: eu.kalafatic.evolution.selfdev.genome/src/eu/kalafatic/evolution/selfdev/genome/hub/

## Domain: general

## Components
* `SelfDevGenomeHub.java`: package eu.kalafatic.evolution.selfdev.genome.hub; import java.io.File; import eu.kalafatic.evolution.selfdev.genome.core.GenomeArtifact; import eu.kalafatic.evolution.selfdev.genome.core.MediatedPackageArtifact; import eu.kalafatic.evolution.selfdev.genome.event.GenomeEvent; import eu.kalafatic.evolution.selfdev.genome.event.GenomeEventBus; import eu.kalafatic.evolution.selfdev.genome.mediation.MediatedPackageProcessor; import eu.kalafatic.evolution.selfdev.genome.repository.GenomeRepository; import eu.kalafatic.evolution.selfdev.genome.selfupgrade.SecondhandUpgradeEngine; import eu.kalafatic.evolution.selfdev.genome.selfupgrade.UpgradeContext; import eu.kalafatic.evolution.selfdev.genome.selfupgrade.UpgradePlan; public class SelfDevGenomeHub { private final GenomeRepository repository; private final GenomeEventBus eventBus; private final MediatedPackageProcessor processor; private final SecondhandUpgradeEngine upgradeEngine; public SelfDevGenomeHub( GenomeRepository repository, GenomeEventBus eventBus, MediatedPackageProcessor processor,
