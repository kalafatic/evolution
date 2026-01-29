package eu.kalafatic.evolution.view;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.Task;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.zest.core.viewers.IGraphEntityContentProvider;

import java.util.ArrayList;
import java.util.List;

public class OrchestrationGraphContentProvider extends ArrayContentProvider implements IGraphEntityContentProvider {

    @Override
    public Object[] getConnectedTo(Object entity) {
        if (entity instanceof Orchestrator) {
            Orchestrator o = (Orchestrator) entity;
            List<Object> connections = new ArrayList<>();
            connections.addAll(o.getAgents());
            connections.addAll(o.getTasks());
            if (o.getNeuronAI() != null) {
                connections.add(o.getNeuronAI());
            }
            return connections.toArray();
        } else if (entity instanceof Task) {
            Task t = (Task) entity;
            List<Object> connections = new ArrayList<>();
            connections.addAll(t.getSubTasks());
            connections.addAll(t.getNext());
            return connections.toArray();
        } else if (entity instanceof Agent) {
            Agent a = (Agent) entity;
            return a.getTasks().toArray();
        }
        return new Object[0];
    }
}
