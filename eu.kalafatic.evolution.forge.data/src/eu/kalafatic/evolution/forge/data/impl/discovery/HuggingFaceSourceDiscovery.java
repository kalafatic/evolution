package eu.kalafatic.evolution.forge.data.impl.discovery;

import eu.kalafatic.evolution.forge.data.api.discovery.DataSourceCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.TrainingDataSourceDiscovery;
import eu.kalafatic.evolution.forge.data.api.downloader.DataDownloader;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadRequest;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadResult;
import eu.kalafatic.evolution.forge.data.api.preference.TrainingDataPreferences;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSource;
import eu.kalafatic.evolution.forge.data.api.source.DatasetSourceConfig;
import eu.kalafatic.evolution.forge.data.impl.downloader.HuggingFaceDownloader;
import eu.kalafatic.evolution.forge.data.impl.source.HuggingFaceDatasetSource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Smart discovery implementation querying Hugging Face API dataset catalog and high-volume repository pools.
 * Dynamically expands similar dataset sources when initial search filters or single repositories exhaust early.
 */
public class HuggingFaceSourceDiscovery implements TrainingDataSourceDiscovery {

    private final DataDownloader downloader;

    public HuggingFaceSourceDiscovery() {
        this(new HuggingFaceDownloader());
    }

    public HuggingFaceSourceDiscovery(DataDownloader downloader) {
        this.downloader = downloader != null ? downloader : new HuggingFaceDownloader();
    }

    @Override
    public List<DataSourceCandidate> discover(TrainingDataPreferences preferences) {
        List<DataSourceCandidate> candidates = new ArrayList<>();
        Set<String> seenRepos = new HashSet<>();

        List<String> keywords = new ArrayList<>();
        if (preferences != null) {
            if (preferences.getPrimaryDomains() != null) {
                keywords.addAll(preferences.getPrimaryDomains());
            }
            if (preferences.getCapabilityObjective() != null && !preferences.getCapabilityObjective().isEmpty()) {
                keywords.add(preferences.getCapabilityObjective());
            }
        }

        if (keywords.isEmpty()) {
            keywords.add("text");
            keywords.add("chat");
        }

        // 1. Live Hugging Face dataset catalog search for each keyword
        for (String kw : keywords) {
            searchHuggingFaceCatalog(kw, candidates, seenRepos, preferences);
        }

        // 2. High-volume curated domain fallback pools
        addCuratedPool("chat", List.of("HuggingFaceH4/ultrachat_200k", "OpenAssistant/oasst1", "lmsys/lmsys-chat-1M", "allenai/tulu-v2-sft-mixture"), candidates, seenRepos, preferences);
        addCuratedPool("code", List.of("bigcode/the-stack", "iamtarun/python_code_instructions_18k_alpaca", "nickrosh/Evol-Instruct-Code-80k"), candidates, seenRepos, preferences);
        addCuratedPool("text", List.of("Salesforce/wikitext", "HuggingFaceFW/fineweb", "allenai/c4", "togethercomputer/RedPajama-Data-1T-Sample"), candidates, seenRepos, preferences);
        addCuratedPool("reasoning", List.of("gsm8k", "math_qa", "open-thoughts/OpenThoughts-114k"), candidates, seenRepos, preferences);

        return candidates;
    }

    private void searchHuggingFaceCatalog(String keyword, List<DataSourceCandidate> candidates, Set<String> seenRepos, TrainingDataPreferences preferences) {
        try {
            String url = "https://huggingface.co/api/datasets?search=" + java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) + "&limit=15";
            DownloadRequest req = new DownloadRequest(url);
            DownloadResult result = downloader.download(req);
            if (result.isSuccess()) {
                JSONArray array = new JSONArray(result.getContentText());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String id = obj.optString("id", null);
                    if (id != null && seenRepos.add(id)) {
                        DatasetSourceConfig cfg = new DatasetSourceConfig("HUGGING_FACE", id);
                        DatasetSource src = new HuggingFaceDatasetSource(cfg, downloader);
                        String lang = preferences != null ? preferences.getRequiredLanguage() : "en";
                        candidates.add(new DataSourceCandidate(id, "HUGGING_FACE", src, lang, List.of(keyword), 100_000_000L, 0.95 - (i * 0.02)));
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to curated pool if network search fails or is offline
        }
    }

    private void addCuratedPool(String domainTag, List<String> repos, List<DataSourceCandidate> candidates, Set<String> seenRepos, TrainingDataPreferences preferences) {
        for (String repo : repos) {
            if (seenRepos.add(repo)) {
                DatasetSourceConfig cfg = new DatasetSourceConfig("HUGGING_FACE", repo);
                if ("Salesforce/wikitext".equalsIgnoreCase(repo) || "wikitext".equalsIgnoreCase(repo)) {
                    cfg.setConfiguration("wikitext-103-v1");
                }
                DatasetSource src = new HuggingFaceDatasetSource(cfg, downloader);
                String lang = preferences != null ? preferences.getRequiredLanguage() : "en";
                candidates.add(new DataSourceCandidate(repo, "HUGGING_FACE", src, lang, List.of(domainTag), 150_000_000L, 0.85));
            }
        }
    }
}
