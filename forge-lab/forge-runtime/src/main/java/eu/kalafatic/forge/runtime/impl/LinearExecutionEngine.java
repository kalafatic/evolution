package eu.kalafatic.forge.runtime.impl;

import eu.kalafatic.forge.math.api.Tensor;
import eu.kalafatic.forge.model.ForgeModel;
import eu.kalafatic.forge.runtime.api.ExecutionEngine;
import java.util.Map;
import java.util.HashMap;

public class LinearExecutionEngine implements ExecutionEngine {
    private final ForgeModel model;

    public LinearExecutionEngine(ForgeModel model) {
        this.model = model;
    }

    @Override
    public Map<String, Tensor> execute(Map<String, Tensor> inputs) {
        // Legacy linear execution logic
        return new HashMap<>();
    }
}
