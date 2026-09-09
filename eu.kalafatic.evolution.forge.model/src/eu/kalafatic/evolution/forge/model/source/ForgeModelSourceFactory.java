package eu.kalafatic.evolution.forge.model.source;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.llm.EvoModelArtifact;
import eu.kalafatic.evolution.forge.model.target.ForgeTarget;
import eu.kalafatic.evolution.forge.model.target.ForgeTargetDetector;
import eu.kalafatic.evolution.forge.model.target.ForgeTargetType;
import eu.kalafatic.evolution.forge.model.target.ForgeTargetValidator;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;

import java.util.Collections;
import java.util.Map;

public class ForgeModelSourceFactory {

    public static ForgeModelSource createSource(String targetPath) {
        ForgeTarget target = ForgeTargetDetector.detect(targetPath);
        return createSource(target);
    }

    public static ForgeModelSource createSource(ForgeTarget target) {
        if (target == null) {
            target = new ForgeTarget("", ForgeTargetType.TRAINING_DATA, false, "Target is null");
        }

        if (target.getType() == ForgeTargetType.EVO_MODEL || target.getType() == ForgeTargetType.EVO_WORKSPACE) {
            ForgeTargetValidator.validateForForging(target);
            return new ExistingEvoModelSource(target);
        } else {
            return new NewForgeModelSource(target);
        }
    }

    private static class ExistingEvoModelSource implements ForgeModelSource {
        private final ForgeTarget target;
        private final EvoModelArtifact artifact;

        public ExistingEvoModelSource(ForgeTarget target) {
            this.target = target;
            this.artifact = target.getArtifact();
        }

        @Override
        public ForgeTarget getForgeTarget() {
            return target;
        }

        @Override
        public ForgeTargetType getType() {
            return target.getType();
        }

        @Override
        public String getForgeMode() {
            return target.getType() == ForgeTargetType.EVO_MODEL ? "FROM_EVO_MODEL" : "FROM_EVO_WORKSPACE";
        }

        @Override
        public String getParentIdentifier() {
            if (artifact != null && artifact.getModelName() != null && !artifact.getModelName().isEmpty()) {
                return artifact.getModelName();
            }
            return target.getDisplayName();
        }

        @Override
        public EvoLlmModel createOrRestoreModel(int vocabSize, int hiddenSize, int heads, int layers, int dff, int maxSeqLen) {
            if (artifact != null) {
                return artifact.createModel();
            }
            return new EvoLlmModel(vocabSize, hiddenSize, heads, layers, dff, maxSeqLen);
        }

        @Override
        public SimpleBPETokenizer getTokenizer(String newCorpus, int targetVocabSize) {
            SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
            if (artifact != null && artifact.getTokenizerVocab() != null && !artifact.getTokenizerVocab().isEmpty()) {
                tokenizer.setVocabulary(artifact.getTokenizerVocab());
            } else {
                tokenizer.train(newCorpus, targetVocabSize);
            }
            return tokenizer;
        }

        @Override
        public Map<String, Integer> getVocabulary() {
            if (artifact != null && artifact.getTokenizerVocab() != null) {
                return artifact.getTokenizerVocab();
            }
            return Collections.emptyMap();
        }

        @Override
        public boolean isPretrained() {
            return true;
        }
    }

    private static class NewForgeModelSource implements ForgeModelSource {
        private final ForgeTarget target;

        public NewForgeModelSource(ForgeTarget target) {
            this.target = target;
        }

        @Override
        public ForgeTarget getForgeTarget() {
            return target;
        }

        @Override
        public ForgeTargetType getType() {
            return ForgeTargetType.TRAINING_DATA;
        }

        @Override
        public String getForgeMode() {
            return "NEW_FORGE";
        }

        @Override
        public String getParentIdentifier() {
            return null;
        }

        @Override
        public EvoLlmModel createOrRestoreModel(int vocabSize, int hiddenSize, int heads, int layers, int dff, int maxSeqLen) {
            return new EvoLlmModel(vocabSize, hiddenSize, heads, layers, dff, maxSeqLen);
        }

        @Override
        public SimpleBPETokenizer getTokenizer(String newCorpus, int targetVocabSize) {
            SimpleBPETokenizer tokenizer = new SimpleBPETokenizer();
            tokenizer.train(newCorpus, targetVocabSize);
            return tokenizer;
        }

        @Override
        public Map<String, Integer> getVocabulary() {
            return Collections.emptyMap();
        }

        @Override
        public boolean isPretrained() {
            return false;
        }
    }
}
