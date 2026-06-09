package eu.kalafatic.evolution.selfdev.genome.selfupgrade;

import java.util.List;

import eu.kalafatic.evolution.selfdev.genome.core.GenomeArtifact;
import eu.kalafatic.evolution.selfdev.genome.repository.GenomeRepository;

public class SecondhandUpgradeEngine {

    private final GenomeRepository repository;

    public SecondhandUpgradeEngine(GenomeRepository repository) {
        this.repository = repository;
    }

    public List<UpgradeProposal> process(GenomeArtifact artifact, ProjectContext context) {

        Insight insight = analyze(artifact);

        Mapping mapping = map(insight, context);

        return generateProposals(mapping);
    }

    private Insight analyze(GenomeArtifact artifact) {
        return new Insight(artifact.getTopic(), artifact.getType().name());
    }

    private Mapping map(Insight insight, ProjectContext context) {
        return new Mapping(insight, context);
    }

    private List<UpgradeProposal> generateProposals(Mapping mapping) {
        return List.of(
                new UpgradeProposal("optimize-context-selection"),
                new UpgradeProposal("improve-darwin-evaluation-loop")
        );
    }
}
