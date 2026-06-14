package eu.kalafatic.forge.runtime.api;

import eu.kalafatic.forge.model.ForgeModel;
import eu.kalafatic.forge.runtime.impl.LinearExecutionEngine;
import eu.kalafatic.forge.runtime.impl.GraphExecutionEngine;

public class ExecutionEngineFactory {
    public static ExecutionEngine create(ForgeModel model) {
        if (model.getSubModels() == null || model.getSubModels().isEmpty()) {
            return new LinearExecutionEngine(model);
        }
        return new GraphExecutionEngine(model);
    }
}
