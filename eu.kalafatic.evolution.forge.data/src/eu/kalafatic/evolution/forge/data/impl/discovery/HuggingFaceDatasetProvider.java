package eu.kalafatic.evolution.forge.data.impl.discovery;

import eu.kalafatic.evolution.forge.data.api.discovery.DatasetCandidate;
import eu.kalafatic.evolution.forge.data.api.discovery.DatasetCompatibilityEvaluator;
import eu.kalafatic.evolution.forge.data.api.discovery.DatasetSearchRequest;
import eu.kalafatic.evolution.forge.data.api.discovery.RemoteDatasetProvider;
import eu.kalafatic.evolution.forge.data.api.downloader.DataDownloader;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadRequest;
import eu.kalafatic.evolution.forge.data.api.downloader.DownloadResult;
import eu.kalafatic.evolution.forge.data.impl.downloader.HuggingFaceDownloader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hugging Face implementation of RemoteDatasetProvider.
 * Queries official Hugging Face catalog endpoints and constructs evaluated DatasetCandidate objects.
 */
public class HuggingFaceDatasetProvider implements RemoteDatasetProvider {

    private final DataDownloader downloader;
    private final DatasetCompatibilityEvaluator evaluator;

    public HuggingFaceDatasetProvider() {
        this(new HuggingFaceDownloader());
    }

    public HuggingFaceDatasetProvider(DataDownloader downloader) {
        this.downloader = downloader != null ? downloader : new HuggingFaceDownloader();
        this.evaluator = new DatasetCompatibilityEvaluator();
    }

    @Override
    public String getProviderName() {
        return "Hugging Face";
    }

    @Override
    public List<DatasetCandidate> search(DatasetSearchRequest request) throws Exception {
        List<DatasetCandidate> candidates = new ArrayList<>();
        if (request == null) return candidates;

        Set<String> seenRepos = new HashSet<>();

        List<String> queries = new ArrayList<>();
        if (!request.getDatasetName().isEmpty() && !request.getDatasetName().equalsIgnoreCase("wikitext")) {
            queries.add(request.getDatasetName());
        }
        if (!request.getDomain().isEmpty()) {
            queries.add(request.getDomain());
        }
        if (!request.getTask().isEmpty()) {
            queries.add(request.getTask().toLowerCase());
        }
        for (String kw : request.getKeywords()) {
            if (!queries.contains(kw)) queries.add(kw);
        }
        if (queries.isEmpty()) {
            queries.add("text");
            queries.add("instruction");
        }

        int limitPerQuery = Math.max(5, request.getLimit() / Math.max(1, queries.size()));

        for (String q : queries) {
            try {
                String encodedQ = URLEncoder.encode(q, StandardCharsets.UTF_8);
                String url = "https://huggingface.co/api/datasets?search=" + encodedQ + "&limit=" + limitPerQuery + "&full=true";
                DownloadRequest req = new DownloadRequest(url);
                DownloadResult res = downloader.download(req);

                if (res.isSuccess() && res.getContentText() != null) {
                    JSONArray arr = new JSONArray(res.getContentText());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String repoId = obj.optString("id", null);
                        if (repoId == null || repoId.trim().isEmpty() || !seenRepos.add(repoId)) {
                            continue;
                        }

                        String lowerRepo = repoId.toLowerCase();
                        if (lowerRepo.contains("tokenizer") || lowerRepo.contains("distilgpt2") || lowerRepo.contains("gguf") || lowerRepo.contains("lora")) {
                            continue;
                        }

                        DatasetCandidate candidate = parseHfJsonObject(obj, repoId, request);
                        evaluator.evaluate(candidate, request);
                        candidates.add(candidate);
                    }
                }
            } catch (Exception ignored) {
                // Network failures handling gracefully per query
            }
        }

        candidates.sort((c1, c2) -> Integer.compare(c2.getCompatibilityScore(), c1.getCompatibilityScore()));

        return candidates;
    }

    @Override
    public DatasetCandidate getMetadata(String datasetId) throws Exception {
        if (datasetId == null || datasetId.trim().isEmpty()) return null;
        String repoId = datasetId.startsWith("huggingface:") ? datasetId.split(":")[1] : datasetId;

        String encodedRepo = URLEncoder.encode(repoId, StandardCharsets.UTF_8);
        String url = "https://huggingface.co/api/datasets/" + encodedRepo;
        DownloadRequest req = new DownloadRequest(url);
        DownloadResult res = downloader.download(req);

        if (res.isSuccess() && res.getContentText() != null) {
            JSONObject obj = new JSONObject(res.getContentText());
            DatasetSearchRequest dummyReq = new DatasetSearchRequest.Builder().datasetName(repoId).build();
            DatasetCandidate candidate = parseHfJsonObject(obj, repoId, dummyReq);
            evaluator.evaluate(candidate, dummyReq);
            return candidate;
        }
        return null;
    }

    private DatasetCandidate parseHfJsonObject(JSONObject obj, String repoId, DatasetSearchRequest request) {
        String author = repoId.contains("/") ? repoId.split("/")[0] : "HuggingFace";
        DatasetCandidate candidate = new DatasetCandidate("Hugging Face", repoId, "main");
        candidate.setAuthor(author);
        candidate.setDescription(obj.optString("description", "Hugging Face dataset candidate: " + repoId));

        String license = "unknown";
        if (obj.has("cardData")) {
            JSONObject card = obj.optJSONObject("cardData");
            if (card != null && card.has("license")) {
                license = card.optString("license", "unknown");
            }
        }
        candidate.setLicense(license);

        List<String> tags = new ArrayList<>();
        JSONArray tagsArr = obj.optJSONArray("tags");
        if (tagsArr != null) {
            for (int i = 0; i < tagsArr.length(); i++) {
                tags.add(tagsArr.getString(i));
            }
        }
        candidate.setTags(tags);

        candidate.setTask(request.getTask());
        candidate.setFormat(request.getFormat());
        candidate.setLanguage(request.getLanguage());

        long sizeBytes = extractRealMetadataBytes(obj);
        candidate.setSizeBytes(sizeBytes);

        candidate.setSplits(List.of("train"));

        List<String> schema = new ArrayList<>();
        if ("INSTRUCTION".equalsIgnoreCase(request.getFormat())) {
            schema.addAll(List.of("instruction", "input", "output", "prompt", "response"));
        } else {
            schema.add("text");
        }
        candidate.setSchema(schema);

        return candidate;
    }

    private long extractRealMetadataBytes(JSONObject repoObj) {
        if (repoObj == null) return 0L;
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
        return 0L;
    }
}
