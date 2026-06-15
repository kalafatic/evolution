package eu.kalafatic.evolution.forge.runtime.api;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import java.util.Map;

public interface Node {
    String getId();
    String getType();
    Tensor forward(Map<String, Tensor> inputs);
}
