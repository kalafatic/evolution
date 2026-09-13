package eu.kalafatic.evolution.controller.orchestration.selfdev;

public class CopySourceTask extends AbstractSelfDevTask {
    private final SourceProvider sourceProvider;

    public CopySourceTask(String id) {
        super(id, "Copy Codebase Task (" + id + ")");
        this.sourceProvider = new GitSourceProvider();
    }

    @Override
    protected TaskResult run(SelfDevContext context) throws Exception {
        TaskResult res = sourceProvider.fetchSource(context.getProjectRoot(), context.getSourceDirectory());
        if (res.isSuccess() && context != null) {
            context.discoverAndRepairModulePaths();
        }
        return res;
    }
}
