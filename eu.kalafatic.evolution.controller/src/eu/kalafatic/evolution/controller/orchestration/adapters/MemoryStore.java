package eu.kalafatic.evolution.controller.orchestration.adapters;
import eu.kalafatic.evolution.controller.orchestration.enums.RealityLevel;
import eu.kalafatic.evolution.controller.orchestration.enums.EvolutionPhase;
import eu.kalafatic.evolution.controller.orchestration.engines.DarwinEngine;

import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationMemoryService;
import eu.kalafatic.evolution.controller.orchestration.selfdev.EvolutionTree;
import eu.kalafatic.evolution.controller.orchestration.selfdev.IterationRecord;

public class MemoryStore {
    private final IterationMemoryService memoryService;

    public MemoryStore(IterationMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    public EvolutionTree getEvolutionTree() {
        return memoryService.getEvolutionTree();
    }

    public void saveRecord(IterationRecord record) {
        memoryService.saveRecord(record);
    }

    public void flush() {
        memoryService.flush();
    }
}
