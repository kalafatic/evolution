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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Smart, multi-page, target-driven Hugging Face dataset discovery implementation.
 * Performs real-time API queries with pagination, multi-level progressive query expansion,
 * metadata size extraction (without fake constants), and cross-round deduplication.
 */
public class HuggingFaceSourceDiscovery implements TrainingDataSourceDiscovery {

    private final DataDownloader downloader;
    private final Set<String> globalSeenRepos = new HashSet<>();
    private int discoveryRound = 0;

    private static final Map<String, List<String>> SYNONYMS = Map.of(
            "chat", List.of("dialogue", "conversation", "instruction", "sft", "assistant", "multi-turn"),
            "code", List.of("coding", "programming", "software", "python", "javascript", "developer", "github"),
            "text", List.of("corpus", "articles", "fineweb", "wikitext", "documents", "c4"),
            "reasoning", List.of("math", "logic", "chain-of-thought", "gsm8k", "problem-solving", "cot"),
            "evolution", List.of("genetics", "darwin", "biology", "natural-selection", "mutation")
    );

    public HuggingFaceSourceDiscovery() {
        this(new HuggingFaceDownloader());
    }

    public HuggingFaceSourceDiscovery(DataDownloader downloader) {
        this.downloader = downloader != null ? downloader : new HuggingFaceDownloader();
    }

    public void resetDiscoveryState() {
        globalSeenRepos.clear();
        discoveryRound = 0;
    }

    @Override
    public List<DataSourceCandidate> discover(TrainingDataPreferences preferences) {
        return discoverNextBatch(preferences, discoveryRound++);
    }

    public List<DataSourceCandidate> discoverNextBatch(TrainingDataPreferences preferences, int round) {
        List<DataSourceCandidate> candidates = new ArrayList<>();
        Set<String> keywords = new LinkedHashSet<>();

        List<String> baseKeywords = extractBaseKeywords(preferences);

        // Progressive Query Expansion Levels:
        // Level 0 (round 0): Exact user intent / primary domains
        // Level 1 (round 1): Domain synonyms and technical terms
        // Level 2 (round 2): Related concepts and broader topics
        // Level 3+ (round 3+): Broader fallbacks and paginated queries (p=round)
        if (round == 0) {
            keywords.addAll(baseKeywords);
        } else if (round == 1) {
            for (String kw : baseKeywords) {
                keywords.add(kw);
                List<String> syns = SYNONYMS.get(kw.toLowerCase());
                if (syns != null) {
                    keywords.addAll(syns.subList(0, Math.min(3, syns.size())));
                }
            }
        } else if (round == 2) {
            for (String kw : baseKeywords) {
                List<String> syns = SYNONYMS.get(kw.toLowerCase());
                if (syns != null) {
                    keywords.addAll(syns);
                }
            }
            keywords.add("dataset");
            keywords.add("instruction");
        } else {
            keywords.addAll(baseKeywords);
            keywords.add("text");
            keywords.add("chat");
            keywords.add("code");
            keywords.add("reasoning");
        }

        int page = round / 2; // Paginate API results across multiple search rounds

        // 1. Live Hugging Face catalog search with pagination
        for (String kw : keywords) {
            searchHuggingFaceCatalog(kw, page, candidates, globalSeenRepos, preferences, round);
        }

        // 2. Curated domain pools fallback if catalog search yields few new candidates
        if (candidates.size() < 3) {
            addCuratedPool("chat", List.of("HuggingFaceH4/ultrachat_200k", "OpenAssistant/oasst1", "lmsys/lmsys-chat-1M", "allenai/tulu-v2-sft-mixture"), candidates, globalSeenRepos, preferences);
            addCuratedPool("code", List.of("bigcode/the-stack", "iamtarun/python_code_instructions_18k_alpaca", "nickrosh/Evol-Instruct-Code-80k"), candidates, globalSeenRepos, preferences);
            addCuratedPool("text", List.of("Salesforce/wikitext", "HuggingFaceFW/fineweb", "allenai/c4", "togethercomputer/RedPajama-Data-1T-Sample"), candidates, globalSeenRepos, preferences);
            addCuratedPool("reasoning", List.of("gsm8k", "math_qa", "open-thoughts/OpenThoughts-114k"), candidates, globalSeenRepos, preferences);
        }

        return candidates;
    }

    private List<String> extractBaseKeywords(TrainingDataPreferences preferences) {
        List<String> keywords = new ArrayList<>();
        if (preferences != null) {
            if (preferences.getPrimaryDomains() != null) {
                keywords.addAll(preferences.getPrimaryDomains());
            }
            if (preferences.getCapabilityObjective() != null && !preferences.getCapabilityObjective().trim().isEmpty()) {
                keywords.add(preferences.getCapabilityObjective().trim());
            }
            if (preferences.getTopics() != null) {
                keywords.addAll(preferences.getTopics());
            }
        }
        if (keywords.isEmpty()) {
            keywords.add("text");
            keywords.add("chat");
        }
        return keywords;
    }

    private void searchHuggingFaceCatalog(String keyword, int page, List<DataSourceCandidate> candidates, Set<String> seenRepos, TrainingDataPreferences preferences, int round) {
        try {
            String encodedKw = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = "https://huggingface.co/api/datasets?search=" + encodedKw + "&limit=20&full=true&p=" + page;
            DownloadRequest req = new DownloadRequest(url);
            DownloadResult result = downloader.download(req);

            if (result.isSuccess()) {
                JSONArray array = new JSONArray(result.getContentText());
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String id = obj.optString("id", null);
                    if (id == null || id.trim().isEmpty() || !seenRepos.add(id)) {
                        continue;
                    }

                    // Extract authentic size metadata if available, otherwise 0L (unknown until streamed)
                    long realMetadataBytes = extractRealMetadataBytes(obj);

                    DatasetSourceConfig cfg = new DatasetSourceConfig("HUGGING_FACE", id);
                    DatasetSource src = new HuggingFaceDatasetSource(cfg, downloader);
                    String lang = preferences != null && preferences.getRequiredLanguage() != null ? preferences.getRequiredLanguage() : "en";

                    double baseRelevance = 0.95 - (round * 0.05) - (i * 0.01);
                    double relevance = Math.max(0.3, baseRelevance);

                    candidates.add(new DataSourceCandidate(
                            id,
                            "HUGGING_FACE",
                            src,
                            lang,
                            List.of(keyword),
                            realMetadataBytes,
                            relevance
                    ));
                }
            }
        } catch (Exception ignored) {
            // Network failures or offline mode handle gracefully by continuing
        }
    }

    private long extractRealMetadataBytes(JSONObject repoObj) {
        if (repoObj == null) return 0L;

        // Inspect cardData or metadata payload if present
        if (repoObj.has("cardData")) {
            JSONObject cardData = repoObj.optJSONObject("cardData");
            if (cardData != null && cardData.has("dataset_info")) {
                Object info = cardData.get("dataset_info");
                if (info instanceof JSONObject infoObj && infoObj.has("dataset_size")) {
                    return infoObj.optLong("dataset_size", 0L);
                } else if (info instanceof JSONArray arr && arr.length() > 0) {
                    JSONObject first = arr.optJSONObject(0);
                    if (first != null && first.has("dataset_size")) {
                        return first.optLong("dataset_size", 0L);
                    }
                }
            }
        }

        // Check if downloads indicator gives an approximate scale or return 0L
        return 0L;
    }

    private void addCuratedPool(String domainTag, List<String> repos, List<DataSourceCandidate> candidates, Set<String> seenRepos, TrainingDataPreferences preferences) {
        for (String repo : repos) {
            if (seenRepos.add(repo)) {
                DatasetSourceConfig cfg = new DatasetSourceConfig("HUGGING_FACE", repo);
                if ("Salesforce/wikitext".equalsIgnoreCase(repo) || "wikitext".equalsIgnoreCase(repo)) {
                    cfg.setConfiguration("wikitext-103-v1");
                }
                DatasetSource src = new HuggingFaceDatasetSource(cfg, downloader);
                String lang = preferences != null && preferences.getRequiredLanguage() != null ? preferences.getRequiredLanguage() : "en";
                candidates.add(new DataSourceCandidate(repo, "HUGGING_FACE", src, lang, List.of(domainTag), 0L, 0.80));
            }
        }
    }
}
