# EVO Native Inference Engine Architecture

## 1. Executive Summary
The native **EVO Inference Engine** (`eu.kalafatic.evolution.forge.model.inference`) is the authoritative, zero-dependency primary runtime for EVO models (`EvoLlmModel`).

It executes Transformer model forward passes directly from native model weights without invoking `llama.cpp`, `Ollama`, or depending on GGUF format binaries.

---

## 2. Core Architecture

```
                                 +-----------------------+
                                 |     EvoLlmModel       |
                                 | (Authoritative Source |
                                 |      of Truth)        |
                                 +-----------+-----------+
                                             |
                                             v
                              +--------------+--------------+
                              |   EvoInferenceEngine        |
                              | (Interface / Core Abstr.)   |
                              +--------------+--------------+
                                             |
                                             v
                              +--------------+--------------+
                              | ReferenceEvoInferenceEngine |
                              | (Native Primary Runtime)    |
                              +--------------+--------------+
                                             |
                     +-----------------------+-----------------------+
                     |                                               |
                     v                                               v
        +------------+------------+                     +------------+------------+
        |  Forward Pass / Logits  |                     |  Interoperability Targets   |
        |  (Shared Math Core)     |                     |  (Secondary Export Formats)|
        +------------+------------+                     +------------+------------+
                     |                                               |
                     v                                               v
        +------------+------------+                     +------------+------------+
        |  Sampling & Generation  |                     | GGUFExporter -> llama.cpp  |
        | (Greedy, Top-P, Top-K)  |                     |              -> Ollama    |
        +-------------------------+                     +-------------------------+
```

---

## 3. Key Components & Specifications

### 3.1 `EvoLlmModel` & `EvoLlmArchitecture`
`EvoLlmModel` holds the structural model architecture (`vocabSize`, `dModel`, `numHeads`, `numBlocks`, `dff`, `maxSeqLen`) and parameter tensors:
- Embedding matrix: `[vocabSize, dModel]`
- Positional Encoding: `[maxSeqLen, dModel]`
- Transformer blocks: RMSNorm weights, Q/K/V/O projections, SwiGLU FFN (W1, W2, W3)
- Final output RMSNorm and LM Head: `[dModel, vocabSize]`

### 3.2 `EvoInferenceEngine` & `ReferenceEvoInferenceEngine`
- **`forward(EvoLlmModel model, int[] inputIds)`**: Executes the forward pass directly from native tensors and returns unnormalized raw logits `[seqLen, vocabSize]`.
- **`generate(EvoLlmModel model, InferenceRequest request, Tokenizer tokenizer)`**: Runs token generation in a decoupled, iterative loop.
- **`validateModel(EvoLlmModel model)`**: Validates model architecture dimensions and weight shapes deterministically via `EvoModelValidator`.

### 3.3 Sampling Subsystem
Generation logic is strictly separated from matrix forward math:
- **Greedy / Deterministic Mode**: Enabled when `temperature == 0.0f`.
- **Temperature & Softmax**: Scales raw logits prior to exponentiation.
- **Top-K & Top-P (Nucleus)**: Filters out low-probability tails.
- **Repeat Penalty**: Applies penalization on previously generated tokens.
- **EOS & Stop Tokens**: Terminates generation upon encountering `</s>` (token 2), EOS (token 3), or custom stop token IDs.

---

## 4. Training vs. Inference Mathematical Alignment
The native inference engine uses the exact same model forward pass mathematics as the training pipeline (`EvoLlmModel.forward`). For identical models and input tokens:

$$\text{Logits}_{\text{training}} = \text{Logits}_{\text{inference}}$$

Numerical equivalence is enforced by tests within floating-point tolerance ($10^{-6}$).

---

## 5. Darwin Evolutionary Engine Compatibility
`ReferenceEvoInferenceEngine` is stateless with respect to the model being evaluated. Arbitrary Darwin candidate models can be evaluated independently without static global state or cross-candidate weight leakage:

```java
InferenceResult resA = inferenceEngine.generate(candidateA, request, tokenizer);
InferenceResult resB = inferenceEngine.generate(candidateB, request, tokenizer);
```

---

## 6. GGUF and External Runtimes
- **Primary Source of Truth**: Native EVO (`EvoLlmModel`, `.evo` packaged artifacts).
- **Secondary Interoperability**: GGUF export, `llama.cpp`, and `Ollama` serve purely as optional export formats and are never invoked for internal EVO inference or Darwin evaluation.
