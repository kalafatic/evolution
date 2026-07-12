# Proposed Improvements for Self-Evo Forging & Integration

The Self-Evo Forging pipeline is a core component of the closed-loop self-development kernel, transitioning from custom codebase crawling to model training and local GGUF registration. Below are key proposed architectural and operational improvements to elevate the robustness, speed, and intelligence of the model generation process.

---

## 1. Dynamic Quantization Support (GGUF Conversion)
### Current Limitation
The current export logic serializes raw float parameters and writes a lightweight placeholder GGUF. During actual conversion, raw weights need to be quantized.
### Proposed Improvement
- Integrate a pure Java or JNI-based **llama.cpp quantization wrapper** (e.g., executing `llama-quantize` or equivalent sub-processes).
- Expose quantization level properties (`Q4_K_M`, `Q8_0`, `F16`) directly in the `ForgeSession` EMF model, allowing developers to configure the accuracy-to-footprint trade-off from the SWT Properties UI.

---

## 2. Parameter-Efficient Fine-Tuning (PEFT/LoRA) Integration
### Current Limitation
The codebase relies on training a full miniature `EvoLlmModel` from scratch, which is highly resource-intensive and struggles with context window size limits.
### Proposed Improvement
- Implement **LoRA (Low-Rank Adaptation)** injection into the attention blocks of the base model (e.g., `llama3.2:3b`).
- Keep the base model frozen and only train rank-decomposition matrices ($W_0 + B \times A$), drastically reducing the training parameter count by $>99\%$, allowing rapid local learning on a CPU/GPU.

---

## 3. Automated Validation Feedback Loops (RLHF/Self-Play)
### Current Limitation
Once the model is registered, it is assumed complete and selected by the Orchestrator without further deterministic checks on code quality or compilation safety.
### Proposed Improvement
- **Validation Stage (Stage 6)**: Before registering the model in Ollama, pass it through a programmatic benchmark suite within the `ValidatorAgent`.
- **Automated Self-Correction**: If the model fails to generate valid syntax, trigger an automatic re-evaluation loop to synthesize balanced fine-tuning samples or adjust temperature parameters dynamically.

---

## 4. Real-time Metrics Visualizations & Loss Curves
### Current Limitation
The training progress is reported as simple percentages, and the detailed stats do not show the convergence behavior of the model.
### Proposed Improvement
- Expose a WebSocket telemetry endpoint or register model progress listeners on `EvoLlmTrainer`.
- Transmit real-time epoch, perplexity, and training loss metrics to the `ForgePage` SWT Browser to render live SVG/HTML5 charts of the training convergence.

---

## 5. Multi-Model Base Registry & Fallback Policies
### Current Limitation
The pipeline defaults to `llama3.2:3b` and uses simple fallback checks on `/api/tags` if that model is missing.
### Proposed Improvement
- Provide a robust base model selector pre-populated with tiny-yet-capable models (e.g., `qwen2.5:1.5b-instruct`, `phi3:3.8b`, or `llama3.2:1b`).
- Implement automatic local model pulling via Ollama's `/api/pull` with visual progress reporting inside the Eclipse SWT view.
