package eu.kalafatic.forge.runtime.impl;

import eu.kalafatic.forge.math.api.Tensor;
import eu.kalafatic.forge.model.ForgeModel;
import eu.kalafatic.forge.model.SubModel;
import eu.kalafatic.forge.model.ModelConnection;
import eu.kalafatic.forge.runtime.api.ExecutionEngine;
import java.util.*;

public class GraphExecutionEngine implements ExecutionEngine {
    private final ForgeModel model;

    public GraphExecutionEngine(ForgeModel model) {
        this.model = model;
    }

    @Override
    public Map<String, Tensor> execute(Map<String, Tensor> inputs) {
        List<SubModel> executionOrder = computeTopologicalOrder();
        Map<String, Tensor> state = new HashMap<>(inputs);

        for (SubModel node : executionOrder) {
            Map<String, Tensor> nodeInputs = collectInputs(node, state);
            Tensor output = executeNode(node, nodeInputs);
            state.put(node.getId(), output);
        }

        return state;
    }

    private List<SubModel> computeTopologicalOrder() {
        Map<String, List<String>> adjacency = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, SubModel> nodeMap = new HashMap<>();

        for (SubModel sm : model.getSubModels()) {
            nodeMap.put(sm.getId(), sm);
            inDegree.put(sm.getId(), 0);
            adjacency.put(sm.getId(), new ArrayList<>());
        }

        for (ModelConnection conn : model.getModelConnections()) {
            adjacency.get(conn.getFromSubModelId()).add(conn.getToSubModelId());
            inDegree.put(conn.getToSubModelId(), inDegree.get(conn.getToSubModelId()) + 1);
        }

        Queue<String> queue = new LinkedList<>();
        for (String id : inDegree.keySet()) {
            if (inDegree.get(id) == 0) queue.add(id);
        }

        List<SubModel> order = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            order.add(nodeMap.get(id));

            for (String neighbor : adjacency.get(id)) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) queue.add(neighbor);
            }
        }

        if (order.size() != model.getSubModels().size()) {
            throw new IllegalStateException("Cycle detected in model graph");
        }

        return order;
    }

    private Map<String, Tensor> collectInputs(SubModel node, Map<String, Tensor> state) {
        Map<String, Tensor> inputs = new HashMap<>();
        for (ModelConnection conn : model.getModelConnections()) {
            if (conn.getToSubModelId().equals(node.getId())) {
                inputs.put(conn.getFromSubModelId(), state.get(conn.getFromSubModelId()));
            }
        }
        return inputs;
    }

    private Tensor executeNode(SubModel node, Map<String, Tensor> inputs) {
        // Deterministic node execution logic
        return null;
    }
}
