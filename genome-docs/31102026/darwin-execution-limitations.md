# Darwin Execution Limitations & Future Scope

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/darwin-execution-limitations.md`

---

## 1. Technical Scope & Known Boundaries

1. **Hardware Dependent Acceleration**: Full native CUDA acceleration for `.evo` transformer models requires available NVIDIA hardware with CUDA driver (`libcuda.so` / `nvcuda.dll`); on CPU-only hosts, execution cleanly falls back to `CPU_HOST_FALLBACK` mode.
2. **External LLM Provider Interruption**: In scenarios where external providers (e.g., remote Ollama or custom HTTP endpoints) do not support socket-level request cancellation, Darwin guards against late responses by checking cancellation tokens before applying mutations to session memory.
3. **Workspace Locking**: Parallel variant evaluation requires dedicated isolated worktree directories; environments lacking Git CLI capability fall back to directory copy mechanisms.
