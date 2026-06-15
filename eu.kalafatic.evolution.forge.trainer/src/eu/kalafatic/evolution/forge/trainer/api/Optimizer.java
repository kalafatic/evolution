package eu.kalafatic.evolution.forge.trainer.api;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import java.util.Map;

public interface Optimizer {
    void update(Map<String, Tensor> parameters, Map<String, Tensor> gradients);
}
