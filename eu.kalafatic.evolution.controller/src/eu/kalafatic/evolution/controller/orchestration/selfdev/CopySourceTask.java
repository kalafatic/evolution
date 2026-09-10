package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class CopySourceTask extends AbstractSelfDevTask {
    private final SourceProvider sourceProvider;

    public CopySourceTask(String id) {
        super(id, "Copy Codebase Task (" + id + ")");
        this.sourceProvider = new GitSourceProvider();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        return sourceProvider.fetchSource(context.getProjectRoot(), context.getSourceDirectory());
    }
}
