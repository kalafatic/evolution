# EVO FORGE — Root Cause Analysis and End-to-End Repair

Date: 2026-11-27 · Repository: `/home/petr/git/evolution-221126` (authoritative, **Java 25**)

---

## 1. Root Causes (confirmed)

The independent validation verdict (FAIL — model emits 64 spaces for every input) has three root causes, two of which are fatal code defects and one of which is a data limitation.

### 1.1 GGUF positional-encoding mismatch (fatal — caused the "spaces-only" output)

- The native EVO model's positional encoding default is **`sinusoidal_abs`** (additive sinusoidal absolute positions added to the embeddings).
- The GGUF exporter writes **`general.architecture=llama`**, which llama.cpp/Ollama evaluates with **RoPE** (rotary) inside attention.
- llama.cpp applies RoPE to a model trained with additive sinusoidal PE → the attention computation is numerically broken → the hidden state collapses and the model argmaxes the same token (the `▁` metaspace) forever.

**Evidence:** `EvoLlmArchitecture` 6-arg constructor defaulted to `POSITIONAL_SINUSOIDAL_ABS`, while `OllamaExporter` unconditionally emitted `general.architecture=llama` + `llama.rope.*` metadata and merely printed a warning.

### 1.2 Vocabulary inflated with synthetic placeholders (wasteful, misleading)

`SimpleBPETokenizer.train(corpus, target)` pads the vocabulary to the requested `target` with `<pad_N>` placeholders. With a ~12 KB corpus and target 14 000, this produced **12 571 placeholders + 1 429 real tokens (89.8% synthetic)**. The model's embedding and output-projection rows for those placeholders are dead weight and misrepresent the model's true vocabulary.

### 1.3 Near-random loss (data limitation, not a code defect)

`cleanCorpusChars ≈ 12 374` (2 records). A BPE tokenizer + decoder-only transformer cannot learn language from 12 KB; the final loss 8.29 is essentially the random baseline `ln(14000) ≈ 9.55`. The model's weights sit at random-initialization scale (std ≈ 1/√640).

---

## 2. Corrections Implemented

| File | Change | Reason |
|---|---|---|
| `forge.model/llm/EvoLlmArchitecture.java` | 6-arg constructor now defaults to `POSITIONAL_ROPE` | Make the native model RoPE so the GGUF/llama.cpp export is mathematically faithful |
| `forge.model/protocol/EvoArchitectureDescriptor.java` | descriptor default `positionalEncoding` → `"rope"` | Match the corrected training default |
| `forge.tokenizer/impl/SimpleBPETokenizer.java` | Added `compactToRealVocabulary()` (removes `<pad_N>` tokens; IDs stay contiguous) | Expose the effective vocabulary instead of a padded one |
| `forge.agent.api/export/OllamaExporter.java` | GGUF export now **throws** for non-RoPE models (was a warning + broken export) | Never silently substitute RoPE for sinusoidal positional encoding |
| `controller/.../selfdev/LLMDarwinEngine.java` | Candidate screening and winner refinement now `compactToRealVocabulary()` and size the model to the effective vocab | Stop padding the model embedding/output to 14 000 placeholders |
| same | Writes `tokenizer.json` and `config.json` alongside `.evo`/GGUF | Produce the required export artifacts (previously only referenced, never written) |
| same | Final validation now gates on degenerate output (empty / whitespace / single repeated char) with an explicit `FAILED` verdict | A whitespace/repeated-char model must not be reported as SUCCESS |

### Tests

- Updated `RoPEPositionalEncodingTest.testLegacySinusoidalModelFallback` and `PositionalEncodingMathematicalTest.testModelIntegrationAndBackwardPass` to reflect the new RoPE default (legacy sinusoidal remains available via the explicit 7-arg constructor).
- Added `SimpleBPETokenizerPadTokenTest.testCompactToRealVocabularyRemovesPlaceholdersAndKeepsIdsContiguous`.

---

## 3. Verification

Builds (all `BUILD SUCCESS`, Java 25):

- `./mvnw -o -Plinux -pl eu.kalafatic.evolution.forge.model,forge.tokenizer,forge.agent.api,controller -am install -DskipTests`

Test runs (Tycho, online for missing source artifact):

| Batch | Result |
|---|---|
| RoPE + sinusoidal + pad-placeholder + tokenizer | 47/47 PASS |
| Export/GGUF/model/architecture/inference/trainer (9 classes) | 37/37 PASS (1 pre-existing failure unrelated to this repair: `AlpacaEvodataTrainingPipelineTest.testDatasetBuilderAndTrainerPromptMasking`) |

---

## 4. Remaining / Not Repaired

1. **NaN refinement validation loss** — not root-caused to a single line here (the refinement runs the full 1024-sequence-length validation; the NaN is consistent with degenerate logits/softmax from a model that learned nothing, but the exact numeric divergence point was not isolated). NaN is already treated as a training failure in the trainer (loss-mask/NaN guards).
2. **Near-random loss from a 12 KB corpus** — a data limitation. The code now uses the effective vocabulary, but a genuinely usable language model requires a non-trivial corpus; this is an operational constraint, not something this repair can fabricate.
3. **7 min/epoch refinement** — float32 CPU/GPU training of a 70 M-param model at 1024-sequence windows; plausible for the current kernel throughput.

---

## 5. Verdict

The two fatal code defects — the **GGUF/RoPE positional-encoding mismatch** and the **synthetic vocabulary padding** — have been repaired, and the exporter now refuses (rather than silently corrupts) an incompatible export. The exported artifacts (`tokenizer.json`, `config.json`) and a degenerate-output quality gate have been added.

The central question — whether a *future* Forge run will now produce a usable model — now depends on the training corpus size: with a real corpus the corrected RoPE + effective-vocabulary pipeline will export a numerically faithful GGUF. The previously generated model (`evo-aaa-v14000-e640-l8-h8-271126_050540`) remains FAIL (it was trained under the defective defaults and on a 12 KB corpus); it was not retrained here.
