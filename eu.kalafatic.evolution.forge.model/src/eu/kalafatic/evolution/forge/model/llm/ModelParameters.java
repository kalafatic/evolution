package eu.kalafatic.evolution.forge.model.llm;

import eu.kalafatic.evolution.forge.math.api.Tensor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

/**
 * First-class canonical tensor and parameter registry for EVO Native models.
 */
public interface ModelParameters extends Serializable {

    /**
     * Retrieves the parameter tensor associated with the given canonical tensor name.
     */
    Tensor get(String canonicalName);

    /**
     * Checks if a tensor with the specified canonical name exists.
     */
    boolean contains(String canonicalName);

    /**
     * Returns an unmodifiable set of canonical tensor names.
     */
    Set<String> names();

    /**
     * Returns an unmodifiable collection of parameter tensors.
     */
    Collection<Tensor> tensors();

    /**
     * Returns the total number of parameter tensors.
     */
    int count();

    /**
     * Returns the total sum of parameter elements (scalar values) across all tensors.
     */
    long totalElements();
}
