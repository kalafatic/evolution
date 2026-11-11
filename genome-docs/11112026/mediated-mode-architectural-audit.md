# MEDIATED MODE ARCHITECTURAL AUDIT & DEFECT REPORT

**Date:** 2026-11-11
**Status:** AUDITED & APPROVED FOR IMPLEMENTATION
**Subsystem:** `eu.kalafatic.evolution.controller` / `eu.kalafatic.evolution.view` (Darwin Mediated Mode)

---

## 1. Executive Summary & Intended Architecture

### What Mediated Mode IS:
- **Local AI Prompt Engineering and Context Preparation Engine**: Uses local Darwin intelligence to convert imperfect/raw user requests into high-fidelity engineering specifications.
- **Intelligent Context Distillation**: Discovers architecture, traces dependencies, and selects high-signal context (source files, configuration, tests, documentation) while excluding binaries and noise.
- **Self-Contained Packaging**: Combines improved specifications, instructions, project context, and file manifests into a downloadable, portable ZIP archive for manual user submission to external frontier AI models (ChatGPT, Claude, Gemini, etc.).
- **Strictly Read-Only & Privacy-Guaranteed**: Never alters target project files. Stores all outputs/temporary archives outside the target. Performs no automatic cloud network calls.

### What Mediated Mode IS NOT:
- NOT an autonomous coding engine.
- NOT a replacement for Developer Mode or Self-Dev Mode.
- NOT an automatic external cloud API proxy or transmission service.
- NOT a simple text rewrite or grammar paraphrasing tool.

---

## 2. Documentation Analysis

1. **`docs/GENERAL_MEDIATED_MODE.md`**:
   - Outlines initial concept: bridging target reality to external LLM via `TargetScanner`, `SemanticExtractor`, `ContextCurator`, and `PromptSynthesizer`.
   - Identified deficiency: Lacked explicit structure for external AI package, file manifests, and strict read-only guarantees.
2. **`docs/USE_CASE_MEDIATED_EXECUTION.md`**:
   - Outdated conceptual example that assumed mediated mode mutated branches and benchmarked variants in-place.
   - Contradicts the core purpose: Mediated Mode does NOT execute code changes in the target project.
3. **`docs/architecture/mediated-mode-evolution-architecture.md`**:
   - Describes dual-genome evolution (Genome A: Prompt, Genome B: Package).
   - Defect: The implementation did not enforce the explicit ZIP archive structure (`PROJECT_CONTEXT/`, `REQUIREMENTS/`, etc.) and lacked file manifest rationale.

---

## 3. Defect Report & Root Cause Analysis

### Defect 1: Target Path Resolution Failure & Fallback Risk
- **Component:** `MediatedEngine.java` / `ADarwinEngine.java` / `OrchestratorServiceImpl.java`
- **Actual Behavior:** `MediatedEngine` did not override target resolution; it inherited `ADarwinEngine` behavior which relied on `context.getProjectRoot()`. When users selected an external target via `MediatedTargetDialog`, stored in `ChatSession.getTargetPath()`, it could be ignored or fallback silently to working dir / workspace root.
- **Expected Behavior:** `ChatSession.getTargetPath()` must be the primary target source. Git repositories, Eclipse projects, and arbitrary filesystem directories must be supported. Inaccessible or invalid paths must fail fast with clear actionable errors without silent fallbacks.
- **Root Cause:** Missing target path resolution in `MediatedEngine`.

### Defect 2: Lack of Structured Technical Specification Transformation
- **Component:** `PromptSynthesizer.java` / `MediatedEngine.java` / `IterationManager.java`
- **Actual Behavior:** Raw user requests were passed directly or minimally reformatted into generic analysis prompts without deep technical expansion.
- **Expected Behavior:** Local Darwin reasoning transforms raw requests into formal specifications: Objective, Scope, Implicit Requirements, Technical Constraints, Affected Areas, Risks, and Acceptance Criteria.
- **Root Cause:** Absence of a dedicated technical specification reasoning engine.

### Defect 3: Missing Context Selection Rationales & File Manifest
- **Component:** `ContextCurator.java` / `MediatedExportManager.java`
- **Actual Behavior:** Files were curated as a raw list of paths without per-file categorization or reasons for inclusion.
- **Expected Behavior:** Every selected file must have an explicit role and reason for inclusion documented in `FILE_MANIFEST.json`.
- **Root Cause:** Curation logic returned only `List<String>` without selection metadata.

### Defect 4: Non-Compliant ZIP Package Layout
- **Component:** `MediatedExportManager.java`
- **Actual Behavior:** Generated legacy layout (`prompt.md`, `affected-files/`, `architecture/`) rather than the specified self-explanatory structure.
- **Expected Behavior:** Standard structure:
  - `README.md`
  - `PROMPT.md`
  - `PROJECT_CONTEXT/` (`ARCHITECTURE.md`, `PROJECT_OVERVIEW.md`, `DEPENDENCIES.md`, `RELEVANT_FILES/`, `FILE_MANIFEST.json`)
  - `REQUIREMENTS/` (`ORIGINAL_REQUEST.md`, `IMPROVED_SPECIFICATION.md`, `ACCEPTANCE_CRITERIA.md`)
  - `DOCUMENTATION/`
- **Root Cause:** `MediatedExportManager` only had legacy unified export methods.

### Defect 5: Target Read-Only Invariance
- **Component:** `MediatedExportManager.java` / `IterationManager.java`
- **Actual Behavior:** When `outputPath` was null, output was written to `projectRoot/src/main/resources` or `projectRoot`, modifying the target project!
- **Expected Behavior:** Outputs must never be placed inside the target project. Use user output path, system temp, or dedicated external export directory.
