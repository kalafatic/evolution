package eu.kalafatic.evolution.forge.controller.service.impl;

import eu.kalafatic.evolution.forge.controller.service.EvolutionService;
import eu.kalafatic.evolution.forge.controller.service.EvolutionPolicyEngine;
import eu.kalafatic.evolution.forge.controller.repository.ForgeRepository;
import eu.kalafatic.evolution.forge.model.*;
import java.util.ArrayList;

import eu.kalafatic.evolution.forge.controller.service.EvolutionPolicy;

public class EvolutionServiceImpl implements EvolutionService {
    private final EvolutionPolicyEngine policyEngine;
    private final ForgeRepository repository;
    private final EvolutionPolicy evolutionPolicy = new EvolutionPolicy();

    public EvolutionServiceImpl(EvolutionPolicyEngine policyEngine, ForgeRepository repository) {
        this.policyEngine = policyEngine;
        this.repository = repository;
    }

    @Override
    public String addSubModel(String sessionId, String modelId, String type, String config) {
        ForgeSession session = repository.load(sessionId);
        ForgeModel model = findModel(session, modelId);

        SubModel subModel = ForgeFactory.eINSTANCE.createSubModel();
        subModel.setId("sm-" + System.currentTimeMillis());
        subModel.setType(SubModelType.valueOf(type));
        subModel.setConfig(config);

        model.getSubModels().add(subModel);

        createSnapshot(model, "Added submodel " + subModel.getId());
        repository.save(session);

        return subModel.getId();
    }

    @Override
    public void removeSubModel(String sessionId, String modelId, String subModelId) {
        ForgeSession session = repository.load(sessionId);
        ForgeModel model = findModel(session, modelId);

        model.getSubModels().removeIf(sm -> sm.getId().equals(subModelId));
        model.getModelConnections().removeIf(c -> c.getFromSubModelId().equals(subModelId) || c.getToSubModelId().equals(subModelId));

        createSnapshot(model, "Removed submodel " + subModelId);
        repository.save(session);
    }

    @Override
    public void connectSubModels(String sessionId, String modelId, String fromId, String toId, String connectionType) {
        if (!policyEngine.isCompatible(fromId, toId, connectionType)) return;

        ForgeSession session = repository.load(sessionId);
        ForgeModel model = findModel(session, modelId);

        ModelConnection conn = ForgeFactory.eINSTANCE.createModelConnection();
        conn.setFromSubModelId(fromId);
        conn.setToSubModelId(toId);
        conn.setConnectionType(ConnectionType.valueOf(connectionType));

        model.getModelConnections().add(conn);

        createSnapshot(model, "Connected " + fromId + " to " + toId);
        repository.save(session);
    }

    @Override
    public void disconnectSubModels(String sessionId, String modelId, String fromId, String toId) {
        ForgeSession session = repository.load(sessionId);
        ForgeModel model = findModel(session, modelId);

        model.getModelConnections().removeIf(c -> c.getFromSubModelId().equals(fromId) && c.getToSubModelId().equals(toId));

        createSnapshot(model, "Disconnected " + fromId + " from " + toId);
        repository.save(session);
    }

    @Override
    public void replaceSubModel(String sessionId, String modelId, String oldSubModelId, String newType, String newConfig) {
        // Implementation logic
    }

    @Override
    public void freezeSubModel(String sessionId, String modelId, String subModelId) {
        // Implementation logic
    }

    @Override
    public void unfreezeSubModel(String sessionId, String modelId, String subModelId) {
        // Implementation logic
    }

    @Override
    public void evolve(String sessionId, String modelId) {
        ForgeSession session = repository.load(sessionId);
        ForgeModel model = findModel(session, modelId);

        // Gating logic
        if (!evolutionPolicy.isMutationAllowed(100, model.getFitnessScore())) {
             return;
        }

        // Complex evolution logic producing new snapshot
        createSnapshot(model, "Evolution cycle triggered");
        evolutionPolicy.recordMutation();
        repository.save(session);
    }

    @Override
    public void rollback(String sessionId, String modelId, String snapshotId) {
        // Rollback logic
    }

    private void createSnapshot(ForgeModel model, String change) {
        EvolutionSnapshot snapshot = ForgeFactory.eINSTANCE.createEvolutionSnapshot();
        snapshot.setId("snap-" + System.currentTimeMillis());
        snapshot.setTimestamp(System.currentTimeMillis());
        snapshot.setFullGraphState(change); // Simplification for now

        // Identity and Lineage management
        String parentId = model.getEvolutionSnapshots().isEmpty() ? null :
                          model.getEvolutionSnapshots().get(model.getEvolutionSnapshots().size() - 1).getId();
        snapshot.setParentSnapshotId(parentId);
        snapshot.setGeneration(model.getGenerationIndex());

        if (change.contains("Evolution cycle triggered")) {
            model.setGenerationIndex(model.getGenerationIndex() + 1);
            snapshot.setGeneration(model.getGenerationIndex());
        }

        model.getEvolutionSnapshots().add(snapshot);

        if (model.getGenome() == null) {
            model.setGenome(ForgeFactory.eINSTANCE.createModelGenome());
        }
        String history = model.getGenome().getMutationHistory();
        model.getGenome().setMutationHistory((history == null ? "" : history + "\n") + change);
    }

    private ForgeModel findModel(ForgeSession session, String modelId) {
        if (session.getActiveModel() != null && session.getActiveModel().getId().equals(modelId)) {
            return session.getActiveModel();
        }
        return null; // Search other models if needed
    }
}
