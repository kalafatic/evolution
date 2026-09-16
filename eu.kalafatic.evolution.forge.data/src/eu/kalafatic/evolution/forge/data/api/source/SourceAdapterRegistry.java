package eu.kalafatic.evolution.forge.data.api.source;

import eu.kalafatic.evolution.forge.data.impl.source.adapters.CSVAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.EVODataAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.FolderAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.GitAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.HuggingFaceAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.JSONAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.JSONLAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.OASST1Adapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.ParquetAdapter;
import eu.kalafatic.evolution.forge.data.impl.source.adapters.TextAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry and factory for resolving format-specific DatasetSourceAdapter instances for dataset selection items.
 */
public class SourceAdapterRegistry {

    private final List<DatasetSourceAdapter> adapters = new ArrayList<>();

    public SourceAdapterRegistry() {}

    public static SourceAdapterRegistry createDefaultRegistry() {
        SourceAdapterRegistry registry = new SourceAdapterRegistry();
        registry.registerAdapter(new EVODataAdapter());
        registry.registerAdapter(new OASST1Adapter());
        registry.registerAdapter(new JSONLAdapter());
        registry.registerAdapter(new JSONAdapter());
        registry.registerAdapter(new CSVAdapter());
        registry.registerAdapter(new ParquetAdapter());
        registry.registerAdapter(new HuggingFaceAdapter());
        registry.registerAdapter(new GitAdapter(registry));
        registry.registerAdapter(new TextAdapter());
        registry.registerAdapter(new FolderAdapter(registry));
        return registry;
    }

    public void registerAdapter(DatasetSourceAdapter adapter) {
        if (adapter != null) {
            adapters.add(adapter);
        }
    }

    public DatasetSourceAdapter findAdapter(DatasetItem item) {
        if (item == null || item.getPath() == null || item.getPath().trim().isEmpty()) {
            return null;
        }
        for (DatasetSourceAdapter adapter : adapters) {
            if (adapter.supports(item)) {
                return adapter;
            }
        }
        return null;
    }

    public List<DatasetSourceAdapter> getAdapters() {
        return Collections.unmodifiableList(adapters);
    }
}
