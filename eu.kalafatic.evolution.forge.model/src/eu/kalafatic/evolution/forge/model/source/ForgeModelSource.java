package eu.kalafatic.evolution.forge.model.source;

import eu.kalafatic.evolution.forge.model.llm.EvoLlmModel;
import eu.kalafatic.evolution.forge.model.target.ForgeTarget;
import eu.kalafatic.evolution.forge.model.target.ForgeTargetType;
import eu.kalafatic.evolution.forge.tokenizer.impl.SimpleBPETokenizer;

import java.util.Map;

public interface ForgeModelSource {

    ForgeTarget getForgeTarget();

    ForgeTargetType getType();

    String getForgeMode();

    String getParentIdentifier();

    EvoLlmModel createOrRestoreModel(int vocabSize, int hiddenSize, int heads, int layers, int dff, int maxSeqLen);

    SimpleBPETokenizer getTokenizer(String newCorpus, int targetVocabSize);

    Map<String, Integer> getVocabulary();

    boolean isPretrained();
}
