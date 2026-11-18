package eu.kalafatic.evolution.forge.model.inference;

import eu.kalafatic.evolution.forge.math.api.Tensor;
import java.util.ArrayList;
import java.util.List;

/**
 * Clean abstraction for Key-Value caching during Transformer inference.
 * Scoped per request / generation run to prevent cross-candidate state leakage.
 */
public class KVCache {
    public static class LayerKVCache {
        private Tensor keyCache;
        private Tensor valueCache;

        public Tensor getKeyCache() {
            return keyCache;
        }

        public void setKeyCache(Tensor keyCache) {
            this.keyCache = keyCache;
        }

        public Tensor getValueCache() {
            return valueCache;
        }

        public void setValueCache(Tensor valueCache) {
            this.valueCache = valueCache;
        }

        public void clear() {
            this.keyCache = null;
            this.valueCache = null;
        }
    }

    private final List<LayerKVCache> layerCaches;

    public KVCache(int numLayers) {
        this.layerCaches = new ArrayList<>();
        for (int i = 0; i < numLayers; i++) {
            this.layerCaches.add(new LayerKVCache());
        }
    }

    public LayerKVCache getLayerCache(int layerIndex) {
        if (layerIndex < 0 || layerIndex >= layerCaches.size()) {
            throw new IndexOutOfBoundsException("Invalid layer index: " + layerIndex);
        }
        return layerCaches.get(layerIndex);
    }

    /**
     * Returns the number of token positions already stored in the cache,
     * i.e. the position offset of the next token to be processed.
     */
    public int getCachedSequenceLength() {
        for (LayerKVCache layerCache : layerCaches) {
            if (layerCache.getKeyCache() != null) {
                return (int) layerCache.getKeyCache().getShape()[0];
            }
        }
        return 0;
    }

    public void clear() {
        for (LayerKVCache layerCache : layerCaches) {
            layerCache.clear();
        }
    }
}
