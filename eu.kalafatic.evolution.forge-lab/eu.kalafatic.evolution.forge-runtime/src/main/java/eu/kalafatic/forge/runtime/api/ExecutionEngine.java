package eu.kalafatic.forge.runtime.api;

import eu.kalafatic.forge.math.api.Tensor;
import java.util.Map;

public interface ExecutionEngine {
    Map<String, Tensor> execute(Map<String, Tensor> inputs);
}
