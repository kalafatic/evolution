package eu.kalafatic.evolution.forge.data.impl.discovery;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceDiscovery;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Discovery implementation querying Hugging Face repository presets matching user preference domains.
 */
public class HuggingFaceSourceDiscovery implements TrainingDataSourceDiscovery {

    @Override
    public List<DataSourceCandidate> discover(TrainingDataPreferences preferences) {
        List<DataSourceCandidate> candidates = new ArrayList<>();
        List<String> domains = preferences != null ? preferences.getPrimaryDomains() : List.of();

        if (domains.isEmpty()) {
            DatasetSourceConfig cfg = new DatasetSourceConfig("HUGGING_FACE", "Salesforce/wikitext");
            cfg.setConfiguration("wikitext-103-v1");
            DatasetSource src = new HuggingFaceDatasetSource(cfg);
            candidates.add(new DataSourceCandidate("Salesforce/wikitext", "HUGGING_FACE", src, "en", List.of("text", "general"), 100_000_000L, 0.8));
            return candidates;
        }

        for (String domain : domains) {
            String repo = resolveRepoForDomain(domain);
            DatasetSourceConfig cfg = new DatasetSourceConfig("HUGGING_FACE", repo);
            DatasetSource src = new HuggingFaceDatasetSource(cfg);
            candidates.add(new DataSourceCandidate(repo, "HUGGING_FACE", src, preferences.getRequiredLanguage(), List.of(domain), 50_000_000L, 0.9));
        }

        return candidates;
    }

    private String resolveRepoForDomain(String domain) {
        String lower = domain.toLowerCase();
        if (lower.contains("prog") || lower.contains("code")) return "bigcode/the-stack";
        if (lower.contains("evolut") || lower.contains("bio")) return "Salesforce/wikitext";
        if (lower.contains("ai") || lower.contains("reason")) return "HuggingFaceH4/ultrachat_200k";
        return "Salesforce/wikitext";
    }
}
