package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link ModelParameters}.
 */
public class DefaultModelParameters implements ModelParameters {

    private static final long serialVersionUID = 1L;

    private final Map<String, Tensor> tensorMap = new LinkedHashMap<>();

    public DefaultModelParameters() {}

    public DefaultModelParameters(Map<String, Tensor> map) {
        if (map != null) {
            this.tensorMap.putAll(map);
        }
    }

    public void register(String canonicalName, Tensor tensor) {
        if (canonicalName == null || canonicalName.trim().isEmpty()) {
            throw new IllegalArgumentException("Canonical name cannot be null or empty");
        }
        if (tensor == null) {
            throw new IllegalArgumentException("Tensor cannot be null for name: " + canonicalName);
        }
        this.tensorMap.put(canonicalName, tensor);
    }

    @Override
    public Tensor get(String canonicalName) {
        return tensorMap.get(canonicalName);
    }

    @Override
    public boolean contains(String canonicalName) {
        return tensorMap.containsKey(canonicalName);
    }

    @Override
    public Set<String> names() {
        return Collections.unmodifiableSet(tensorMap.keySet());
    }

    @Override
    public Collection<Tensor> tensors() {
        return Collections.unmodifiableCollection(tensorMap.values());
    }

    @Override
    public int count() {
        return tensorMap.size();
    }

    @Override
    public long totalElements() {
        long sum = 0;
        for (Tensor t : tensorMap.values()) {
            if (t != null && t.getData() != null) {
                sum += t.getData().length;
            }
        }
        return sum;
    }
}
