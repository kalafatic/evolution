package eu.kalafatic.evolution.view.editors;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;

public class OrchestratorEditorInput implements IEditorInput {

    private Orchestrator orchestrator;

    public OrchestratorEditorInput(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    public Orchestrator getOrchestrator() {
        return orchestrator;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return null;
    }

    @Override
    public String getName() {
        return orchestrator.getName();
    }

    @Override
    public IPersistableElement getPersistable() {
        return null;
    }

    @Override
    public String getToolTipText() {
        return "Orchestration: " + orchestrator.getName();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isInstance(orchestrator)) {
            return adapter.cast(orchestrator);
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        OrchestratorEditorInput other = (OrchestratorEditorInput) obj;
        return orchestrator.equals(other.orchestrator);
    }

    @Override
    public int hashCode() {
        return orchestrator.hashCode();
    }
}
