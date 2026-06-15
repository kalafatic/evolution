package eu.kalafatic.evolution.forge.runtime.api;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import java.util.Map;

public interface ExecutionEngine {
    Map<String, Tensor> execute(Map<String, Tensor> inputs);
}
