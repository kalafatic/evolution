package eu.kalafatic.evolution.forge.runtime.api;

import eu.kalafatic.evolution.forge.model.ForgeModel;
import eu.kalafatic.evolution.forge.runtime.impl.LinearExecutionEngine;
import eu.kalafatic.evolution.forge.runtime.impl.GraphExecutionEngine;

public class ExecutionEngineFactory {
    public static ExecutionEngine create(ForgeModel model) {
        if (model.getSubModels() == null || model.getSubModels().isEmpty()) {
            return new LinearExecutionEngine(model);
        }
        return new GraphExecutionEngine(model);
    }
}
