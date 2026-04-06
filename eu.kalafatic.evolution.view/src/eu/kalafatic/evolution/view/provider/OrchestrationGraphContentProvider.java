package eu.kalafatic.evolution.view.provider;

import eu.kalafatic.evolution.model.orchestration.Agent;
import eu.kalafatic.evolution.model.orchestration.Iteration;
import eu.kalafatic.evolution.model.orchestration.NeuronAI;
import eu.kalafatic.evolution.model.orchestration.Orchestrator;
import eu.kalafatic.evolution.model.orchestration.SelfDevSession;
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
            connections.addAll(o.getTests());
            if (o.getGit() != null) {
                connections.add(o.getGit());
            }
            if (o.getMaven() != null) {
                connections.add(o.getMaven());
            }
            if (o.getLlm() != null) {
                connections.add(o.getLlm());
            }
            if (o.getCompiler() != null) {
                connections.add(o.getCompiler());
            }
            if (o.getOllama() != null) {
                connections.add(o.getOllama());
            }
            if (o.getAiChat() != null) {
                connections.add(o.getAiChat());
            }
            if (o.getNeuronAI() != null) {
                connections.add(o.getNeuronAI());
            }
            if (o.getSelfDevSession() != null) {
                connections.add(o.getSelfDevSession());
            }
            if (o.getDatabase() != null) {
                connections.add(o.getDatabase());
            }
            if (o.getFileConfig() != null) {
                connections.add(o.getFileConfig());
            }
            if (o.getEclipse() != null) {
                connections.add(o.getEclipse());
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
            List<Object> connections = new ArrayList<>();
            connections.addAll(a.getTasks());
            connections.addAll(a.getRules());
            return connections.toArray();
        } else if (entity instanceof SelfDevSession) {
            SelfDevSession s = (SelfDevSession) entity;
            return s.getIterations().toArray();
        } else if (entity instanceof Iteration) {
            Iteration i = (Iteration) entity;
            List<Object> connections = new ArrayList<>();
            connections.addAll(i.getTasks());
            if (i.getEvaluationResult() != null) {
                connections.add(i.getEvaluationResult());
            }
            return connections.toArray();
        }
        return new Object[0];
    }
}
