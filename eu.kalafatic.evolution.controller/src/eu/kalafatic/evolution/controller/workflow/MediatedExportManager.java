package eu.kalafatic.evolution.controller.workflow;

import eu.kalafatic.evolution.controller.orchestration.TaskContext;
import java.io.File;

public class MediatedExportManager {
    private final TaskContext context;

    public MediatedExportManager(TaskContext context) {
        this.context = context;
    }

    public void notifyExportReady(File zipFile) {
        RuntimeEvent event = new RuntimeEvent(
            RuntimeEventType.EXPORT_READY,
            context.getSessionId(),
            "MediatedExportManager",
            zipFile.getAbsolutePath()
        );
        RuntimeEventBus.getInstance().publish(event);
    }
}
