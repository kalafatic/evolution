# EVO Architecture Consolidation & System Unification

**Document:** `docs/milestones/milestone_architecture_consolidation_090926.md`
**Date:** 2026-09-09
**Scope:** Entire EVO / AI Evolution project
**Type:** Architecture Analysis / Consolidation / Unification
**Implementation:** NONE
**Primary Goal:** Define one compact, coherent, robust architecture for the complete EVO platform.

---

# 1. Executive Summary

The EVO project has evolved through many independent development phases:

* AI orchestration
* Darwin evolution
* Forge / LLM training
* training-data preparation
* native EVO model format
* native inference
* GGUF / Ollama exporters
* Self-Dev
* Git-based model versioning
* Eclipse RCP UI
* Hugging Face integration
* EVO Codebase datasource
* reasoning LLM support
* external coding-agent / mutation concepts

The functionality is increasingly powerful, but the architecture has likely become **fragmented through incremental development**.

Different generations of the system appear to use different:

* naming conventions
* lifecycle models
* configuration approaches
* artifact conventions
* error handling
* state management
* UI patterns
* orchestration patterns
* directory structures
* protocols
* metadata models
* service boundaries
* persistence approaches
* logging/progress mechanisms
* terminology

As a result, individual modules may work correctly in isolation while the complete system lacks one consistent architectural language.

The objective of this milestone is therefore:

> **Do not implement new functionality. First establish one compact architectural model that explains the entire EVO system.**

The final architecture must make the following appear as parts of one system rather than separate applications:

```text
USER
  │
  ▼
EVO UI / COMMANDER
  │
  ▼
EVO REQUEST
  │
  ▼
ORCHESTRATION
  │
  ├── CHAT / INFERENCE
  ├── DARWIN
  ├── FORGE
  ├── SELF-DEV
  └── MUTATION
        │
        ▼
   DOMAIN SERVICES
        │
        ├── DATA
        ├── MODELS
        ├── TRAINING
        ├── EVALUATION
        ├── GIT
        └── RUNTIME
```

The architecture must be understandable without knowing the historical development sequence of the project.

---

# 2. Fundamental Architectural Principle

EVO should be treated as **one evolution system**, not a collection of loosely connected tools.

The central abstraction should be:

```text
EVO REQUEST
    ↓
ANALYZE
    ↓
PLAN
    ↓
EXECUTE
    ↓
EVALUATE
    ↓
EVOLVE
    ↓
ARTIFACT
```

Everything else is a specialized realization of this process.

For example:

### Chat

```text
Request
 → understand
 → infer
 → evaluate response
 → return result
```

### Darwin

```text
Request
 → understand
 → create trajectories
 → execute variants
 → evaluate fitness
 → select winner
```

### Forge

```text
Forge Request
 → analyze sources
 → prepare dataset
 → train
 → evaluate
 → optimize
 → produce .evo
```

### Self-Dev

```text
Self-Dev Request
 → analyze codebase
 → plan mutation
 → modify source
 → build
 → deploy
 → start
 → validate
 → select/rollback
```

These are not independent architectures.

They are different **evolution workflows**.

---

# 3. Architecture Audit Must Precede Any Refactoring

Before modifying code, perform a complete architecture inventory.

The audit must identify:

* modules
* packages
* OSGi bundles
* Eclipse plugins
* applications
* services
* controllers
* managers
* engines
* agents
* models
* DTOs
* configuration objects
* persistence
* artifact formats
* UI components
* background jobs
* external processes
* filesystem conventions
* protocols
* lifecycle/state machines
* duplicated responsibilities
* obsolete implementations
* compatibility layers
* transitional code
* abandoned concepts

The result must be a dependency and responsibility map.

For every significant class/module answer:

```text
WHAT IS IT?
WHO OWNS IT?
WHAT STATE DOES IT OWN?
WHAT DOES IT CONTROL?
WHAT DOES IT CALL?
WHAT CALLS IT?
WHAT ARTIFACT DOES IT PRODUCE?
WHAT ARTIFACT DOES IT CONSUME?
WHAT IS ITS LIFECYCLE?
IS IT STILL AUTHORITATIVE?
```

---

# 4. Identify Architectural Generations

The project must explicitly identify areas where multiple generations of the same concept coexist.

Typical examples to investigate:

* old/new orchestration
* old/new Forge flow
* old/new training pipeline
* old/new inference engine
* old/new model loading
* old/new dataset handling
* multiple configuration objects
* multiple model metadata formats
* multiple progress mechanisms
* multiple logging mechanisms
* multiple job/state models
* multiple Git abstractions
* multiple Self-Dev execution paths
* multiple UI-to-engine communication paths

The objective is not automatically to delete the older implementation.

First classify it:

```text
AUTHORITATIVE
ACTIVE
COMPATIBILITY
LEGACY
DUPLICATE
OBSOLETE
UNKNOWN
```

No module should remain `UNKNOWN` after this milestone.

---

# 5. Target Architectural Shape

The target architecture should be compact.

A recommended logical structure is:

```text
evo
│
├── core
│   ├── domain
│   ├── orchestration
│   ├── lifecycle
│   ├── events
│   ├── configuration
│   └── errors
│
├── intelligence
│   ├── llm
│   ├── agents
│   ├── reasoning
│   └── routing
│
├── evolution
│   ├── darwin
│   ├── optimization
│   └── evaluation
│
├── forge
│   ├── sources
│   ├── dataset
│   ├── training
│   ├── model
│   ├── validation
│   └── export
│
├── inference
│   └── native
│
├── selfdev
│   ├── codebase
│   ├── build
│   ├── deployment
│   ├── supervisor
│   └── runtime
│
├── git
│   └── evo-versioning
│
└── ui
    └── eclipse-rcp
```

This is a **logical architecture**, not a requirement to blindly rename existing bundles.

Existing module boundaries must be reused where they are already healthy.

The objective is:

> Minimize architectural concepts, not maximize package count.

---

# 6. Core Domain Objects

The project should converge on a small set of canonical domain concepts.

The exact existing class names must first be audited.

Conceptually the system needs:

```text
EvoRequest
EvoSession
EvoJob
EvoRun
EvoResult
EvoArtifact
EvoModel
TrainingSource
TrainingDataset
ForgeRequest
ForgeRun
DarwinRun
SelfDevRun
EvaluationResult
GitRevision
```

There must not be five competing abstractions representing the same thing.

---

# 7. Request → Run → Artifact Model

A strong common model should be:

```text
REQUEST
   │
   ▼
RUN
   │
   ├── configuration
   ├── inputs
   ├── state
   ├── progress
   ├── logs
   ├── metrics
   ├── outputs
   └── provenance
          │
          ▼
       ARTIFACT
```

This should apply consistently to:

* inference
* Forge
* Darwin
* Self-Dev
* model export
* dataset preparation

A UI should not directly manage the lifecycle of a complex operation.

---

# 8. Lifecycle Unification

All long-running EVO operations should follow a common lifecycle vocabulary.

Recommended canonical states:

```text
CREATED
ANALYZING
PLANNING
PREPARING
READY
RUNNING
EVALUATING
FINALIZING
VALIDATING
COMPLETED
FAILED
CANCELLED
```

Specialized workflows may have additional internal states, but their externally visible lifecycle should map onto this common model.

There must be exactly one authoritative owner of state transitions per run.

For Darwin this is already conceptually aligned with:

```text
IterationManager = transition authority
EvolutionOrchestrator = executor
```

The same principle should be applied elsewhere.

No UI controller, background thread, trainer, supervisor or callback should silently become a second lifecycle authority.

---

# 9. Separation of Orchestration and Execution

A recurring architectural danger is mixing:

```text
WHAT SHOULD HAPPEN?
```

with:

```text
HOW IS IT EXECUTED?
```

The unified architecture must maintain:

```text
Planner / Orchestrator
        ↓
Execution Service
        ↓
Result
        ↓
Evaluator
```

For example:

### Forge

```text
ForgeOrchestrator
      ↓
SourceAnalyzer
      ↓
DatasetComposer
      ↓
Trainer
      ↓
Evaluator
      ↓
ModelFinalizer
```

### Self-Dev

```text
SelfDevOrchestrator
      ↓
Codebase Analysis
      ↓
Mutation Plan
      ↓
Execution
      ↓
Build
      ↓
Deployment
      ↓
Runtime Validation
```

No UI class should contain this workflow.

---

# 10. Canonical Forge Architecture

Forge must become one coherent pipeline.

```text
ForgeRequest
     │
     ├── Base Model
     │
     ├── Training Sources
     │
     ├── Objective
     │
     ├── Strategy
     │
     └── Budget
     │
     ▼
Source Analysis
     ▼
Normalization
     ▼
Global Deduplication
     ▼
Leakage Detection
     ▼
Dataset Composition
     ▼
Training Dataset (.evodata)
     ▼
Training
     ▼
Evaluation
     ▼
Optimization
     ▼
Native EVO Model (.evo)
     ▼
Validation
     ▼
Git EVO
```

The Forge UI must not implement this logic.

---

# 11. Training Source Architecture

All user-selectable training inputs must converge on one source abstraction.

Supported logical types include:

```text
HF_DATASET
LOCAL_FILE
LOCAL_DIRECTORY
EVODATA
GIT_REPOSITORY
EVO_CODEBASE
```

An existing `.evo` is different:

```text
EVO_MODEL
```

It is a **model base**, not training data.

Therefore:

```text
ForgeRequest
    ├── baseModel: optional EvoModel
    └── sources: List<TrainingSource>
```

This distinction must remain explicit throughout the system.

---

# 12. EVO Model as a First-Class Forge Input

An existing model:

```text
chat-v1.evo
```

may be used as:

```text
BASE MODEL
```

with new sources:

```text
chat-v1.evo
      +
English replay data
      +
EVO Codebase
      +
new documentation
      ↓
Forge
      ↓
chat-v2.evo
```

The original model must remain immutable.

The resulting model becomes a child:

```text
chat-v1.evo
     │
     └── chat-v2.evo
             │
             └── chat-v3.evo
```

The model lineage must be recorded.

Multiple `.evo` models must never be silently concatenated as if they were datasets.

Model merging is a separate operation requiring explicit compatibility checks.

---

# 13. `.evodata` Protocol

`.evodata` must have one canonical meaning:

> **EVO Training Dataset Artifact**

It is not a model.

It is not merely a ZIP file.

It is a versioned artifact with defined protocol semantics.

Conceptually:

```text
dataset.evodata
│
├── metadata.json
├── data.jsonl
├── val_data.jsonl
├── report.txt
└── protocol/version information
```

The protocol must define:

* format version
* dataset identity
* source provenance
* source revision
* content hashes
* record schema
* split information
* token accounting
* filtering statistics
* deduplication statistics
* validation statistics
* tokenizer information
* creation timestamp
* preparation configuration
* status

Truthful state must be enforced:

```text
PREPARING
READY
FAILED
```

Never expose an artifact as production-ready while it still contains placeholder or incomplete data.

---

# 14. `.evo` Protocol

`.evo` must be treated as the canonical native EVO model artifact.

It should define:

```text
format/version
architecture
tensor metadata
vocabulary
tokenizer
dimensions
layers
parameters
training provenance
parent model
dataset provenance
checksums
compatibility
```

The protocol must allow the native inference engine and exporters to determine whether the artifact is valid without relying on directory naming conventions.

The distinction must remain:

```text
.evodata = training data
.evo     = trained model
```

---

# 15. EVO Model Lineage

Model evolution must be explicit.

Example:

```text
chat-v1.evo
    │
    ├── source: general English
    └── Forge run F001
          │
          ▼
chat-v2.evo
    │
    ├── parent: chat-v1
    ├── source: EVO Codebase
    └── Forge run F002
          │
          ▼
chat-v3.evo
```

Every model should be able to answer:

```text
What is my parent?
What data trained me?
Which Forge run created me?
Which configuration was used?
Which tokenizer?
Which architecture?
Which Git revision?
Which evaluation?
Which hardware?
Which random seed?
```

---

# 16. Data Composition

Multiple sources must be composable.

Example:

```text
SOURCE A
English conversational corpus

SOURCE B
EVO Codebase

SOURCE C
EVO documentation

SOURCE D
existing .evodata
```

The system must not simply concatenate them.

The composition stage must consider:

* source type
* domain
* language
* quality
* duplication
* estimated tokens
* requested token budget
* desired capabilities
* training strategy
* model capacity
* validation requirements

The result should be a deliberate training mixture.

---

# 17. Catastrophic Forgetting

Continual Forge must explicitly account for preservation of existing capabilities.

For:

```text
chat-v1.evo + EVO Codebase
```

the system must not automatically assume that training only on EVO Codebase is correct.

Where appropriate:

```text
existing capability data
+
new domain data
```

should be mixed.

Evaluation must compare:

```text
parent model
vs
child model
```

on both:

```text
general capability
domain capability
```

This is part of the Forge architecture, not an optional UI feature.

---

# 18. EVO Codebase Datasource

The complete EVO Git repository is a special high-value source.

It must be treated as a structured knowledge source, not merely a directory of text files.

Relevant categories include:

```text
JAVA_SOURCE
JAVA_TEST
XML
OSGI
ECLIPSE_RCP
TYCHO
MAVEN
MARKDOWN
PDF
JSON
YAML
PROPERTIES
SHELL
CONFIGURATION
RESOURCE
```

Build outputs, generated files, caches, temporary files, models, datasets, logs and other irrelevant artifacts must have explicit exclusion rules.

The source should preserve relationships such as:

```text
Java class
   ↕
XML contribution
   ↕
plugin.xml
   ↕
OSGi metadata
   ↕
Tycho build
   ↕
documentation
   ↕
tests
```

This makes EVO Codebase materially different from a generic text directory.

---

# 19. Dataset Preparation Boundary

The architecture must maintain a clean separation:

```text
SOURCE
  ↓
SOURCE READER
  ↓
NORMALIZER
  ↓
QUALITY / DEDUP
  ↓
COMPOSER
  ↓
EVODATA
  ↓
TRAINER
```

Training must consume a stable dataset artifact rather than knowing how every source type works.

The trainer must not contain:

* Hugging Face downloading logic
* PDF parsing
* Git traversal
* Java source classification
* directory scanning
* dataset deduplication

This is one of the most important architectural boundaries.

---

# 20. Native Inference Boundary

Native inference must consume:

```text
.evo
```

through a canonical model-loading protocol.

Conceptually:

```text
EvoModelLoader
      ↓
Validated EvoModel
      ↓
NativeInferenceEngine
```

Inference must not know:

* where the model was trained
* which UI selected it
* which Forge dialog created it
* whether it came from Git
* whether it came from Self-Dev

Those are provenance concerns.

Inference needs only a valid model.

---

# 21. Export Architecture

Exporters should consume the canonical model representation:

```text
EVO MODEL
   │
   ├── Native EVO
   ├── GGUF
   ├── Ollama
   ├── llama.cpp
   └── future formats
```

Exporters must not independently reconstruct the model from training directories.

There must be one canonical source of model truth.

---

# 22. Evaluation Architecture

Evaluation must be a first-class subsystem.

Conceptually:

```text
EvaluationRequest
     ↓
Benchmark/Test Set
     ↓
Model Execution
     ↓
Metrics
     ↓
EvaluationResult
```

Evaluation must support at least:

```text
training loss
validation loss
overfitting indicators
generation quality
repetition
stability
general capability
domain capability
native inference smoke test
```

For continued training:

```text
parent evaluation
        ↓
child evaluation
        ↓
regression analysis
```

A model should not become `READY` merely because training completed.

---

# 23. Darwin Integration

Darwin should be a general optimization mechanism above specialized engines.

It should not duplicate Forge internals.

Correct conceptual relation:

```text
Darwin
  ↓
chooses/optimizes
  ↓
Forge configuration
```

rather than:

```text
Darwin
  ↓
contains another Forge
```

Possible Darwin variables:

```text
dataset composition
learning rate
batch configuration
training budget
sequence length
model configuration
evaluation weighting
```

The architecture must prevent Darwin from becoming a second orchestration framework.

---

# 24. LLM / Reasoning Architecture

LLM providers must converge on one abstraction.

The system must distinguish:

```text
REQUEST
REASONING
FINAL RESPONSE
```

A reasoning model must never accidentally expose internal reasoning as the final answer.

Conceptually:

```text
LLM Response
    │
    ├── reasoning
    │
    └── final
```

The UI may display reasoning separately as a dedicated thinking bubble, while the final answer remains semantically separate.

This protocol should be provider-independent.

---

# 25. Chat / Commander UI

The Eclipse multipage page currently known as:

```text
AI CHAT
```

should be treated architecturally as the main EVO command/inference interface rather than as a simple chatbot.

The UI should conceptually provide:

```text
COMMAND / REQUEST
        ↓
EVO
        ↓
RESULT
```

The exact final label can be decided separately, but the architecture must not depend on the page name.

The UI must remain thin.

It should:

* collect user input
* select relevant options
* start a request
* display state
* display progress
* display reasoning separately
* display results
* display artifacts
* allow cancellation
* display errors

It should not contain orchestration logic.

---

# 26. Forge UI

Forge configuration should be centralized.

The UI should expose a coherent configuration model rather than independent dialogs each maintaining their own interpretation of settings.

Conceptually:

```text
Forge Settings
│
├── Base Model
├── Sources
├── Objective
├── Strategy
├── Dataset Budget
├── Model Configuration
├── Training Configuration
├── Evaluation
└── Output
```

The `Select Target` concept should select the appropriate artifact/source, while Forge Settings should define how that target participates in the operation.

The UI must not duplicate source interpretation logic.

---

# 27. AUTO Strategy

The preferred user experience is:

```text
SELECT SOURCES
      ↓
AUTO
      ↓
EVO decides how to process them
```

Advanced configuration should override AUTO when required.

AUTO should be able to determine:

* source classification
* source compatibility
* data extraction strategy
* filtering
* deduplication
* composition
* training strategy
* base model usage
* preservation strategy
* evaluation strategy

The important architectural rule is:

> AUTO is a planning strategy, not a collection of hidden UI heuristics.

Its decisions belong to the orchestration/planning layer.

---

# 28. Configuration Unification

The audit must identify every configuration mechanism.

Examples to investigate:

```text
preferences
properties
JSON
XML
environment variables
system properties
hard-coded constants
dialog state
workspace state
command-line arguments
database state
```

The project should establish a clear hierarchy:

```text
DEFAULTS
   ↓
GLOBAL CONFIG
   ↓
PROJECT CONFIG
   ↓
JOB CONFIG
   ↓
EXPLICIT USER OVERRIDE
```

Configuration must not be silently duplicated across modules.

Every important parameter must have one canonical representation.

---

# 29. Filesystem Architecture

The project should establish canonical directory semantics.

Suggested conceptual structure:

```text
<evo-home>/
│
├── models/
│   ├── native/
│   ├── exports/
│   └── registry/
│
├── datasets/
│   ├── sources/
│   ├── evodata/
│   └── registry/
│
├── forge/
│   ├── runs/
│   ├── checkpoints/
│   ├── evaluation/
│   └── temporary/
│
├── darwin/
│   ├── runs/
│   └── iterations/
│
├── selfdev/
│   ├── runs/
│   ├── workspaces/
│   └── runtime/
│
├── git/
│   └── model-history/
│
├── logs/
│
└── cache/
```

This is a target semantic structure.

Existing paths must be audited and mapped before any physical migration.

The project must not contain multiple undocumented meanings for directories such as:

```text
runtime
output
data
models
forge-output
workspace
temp
cache
```

---

# 30. Artifact Ownership

Every generated artifact must have one owner.

Examples:

```text
.evodatа
    owned by Dataset/Preparation subsystem

.evo
    owned by Model/Forge subsystem

GGUF
    owned by Export subsystem

Forge Run
    owned by Forge orchestration

Darwin Run
    owned by Darwin orchestration

Self-Dev Run
    owned by Self-Dev orchestration
```

Other modules may reference artifacts but must not silently mutate them.

---

# 31. Immutability

Completed artifacts should be immutable.

Especially:

```text
.evodata
.evo
ForgeRun
EvaluationResult
Git model revision
```

A new operation creates a new artifact/version.

For example:

```text
chat-v1.evo
       ↓
Forge
       ↓
chat-v2.evo
```

not:

```text
chat-v1.evo
       ↓
overwrite
```

This is essential for reproducibility and rollback.

---

# 32. Provenance

Every significant operation should record provenance.

Minimum conceptual chain:

```text
SOURCE
   ↓
DATASET
   ↓
FORGE RUN
   ↓
MODEL
   ↓
EVALUATION
   ↓
GIT REVISION
```

For Self-Dev:

```text
GIT REVISION
   ↓
SELFDEV RUN
   ↓
MUTATION
   ↓
BUILD
   ↓
DEPLOYMENT
   ↓
RUNTIME
   ↓
VALIDATION
```

The system should be able to reconstruct why an artifact exists.

---

# 33. Git EVO

Git integration must be treated as model versioning rather than a special file copy.

Conceptually:

```text
Model
+
Manifest
+
Provenance
+
Hash
+
Parent
+
Evaluation
=
Git EVO revision
```

Git should preserve model lineage.

Large binary models may require an appropriate large-file strategy.

The existing `GitManager` should remain the low-level Git abstraction.

A model-aware layer should sit above it rather than duplicating Git functionality.

---

# 34. Self-Dev Architecture

Self-Dev must follow the same architectural principles.

The complete lifecycle is:

```text
SOURCE REPOSITORY
       ↓
ANALYZE
       ↓
PLAN
       ↓
MUTATE
       ↓
COMPILE
       ↓
BUILD
       ↓
DEPLOY
       ↓
START SUPERVISOR
       ↓
COMMAND RUNNING SUPERVISOR
       ↓
BUILD / DEPLOY / START EVO
       ↓
VALIDATE
       ↓
ACCEPT / ROLLBACK
```

The lifecycle must be explicit.

A successful compilation is not equivalent to a successful Self-Dev operation.

The final acceptance criterion is a functioning EVO runtime.

---

# 35. Supervisor Boundary

The supervisor must be treated as a runtime control plane.

It should not become a second application architecture.

Conceptually:

```text
SelfDev Controller
       ↓
Supervisor
       ↓
EVO Runtime
```

The supervisor should provide controlled operations such as:

```text
BUILD
DEPLOY
START
STOP
RESTART
STATUS
VALIDATE
```

The protocol must be explicit and versioned.

---

# 36. Self-Dev Artifact Paths

Self-Dev should distinguish clearly between:

```text
SOURCE
BUILD OUTPUT
DEPLOYMENT
RUNTIME
LOGS
VALIDATION
```

Do not mix these.

A generated RCP executable such as:

```text
evo.exe
```

must have a deterministic relationship to:

```text
product definition
configuration
features
plugins
artifacts
launcher
runtime
```

A blank RCP window must be treated as a deployment/runtime validation failure, not as a successful build.

---

# 37. Cross-Platform Runtime

Windows and Linux paths must be represented semantically.

Avoid architectural assumptions such as:

```text
C:\...
/home/...
```

inside domain logic.

Paths should be resolved by runtime/environment-specific adapters.

The logical artifact structure remains identical.

---

# 38. Logging

The project should have one canonical structured logging concept.

All long-running operations should be able to emit:

```text
timestamp
runId
stage
severity
component
message
progress
artifact
```

Example:

```text
[FORGE][run=F123][TRAINING][62%]
Training epoch 4/8
```

The exact format may differ internally, but semantics must remain consistent.

UI logs should be projections of system events/logs, not a separate logging system.

---

# 39. Progress

Progress must be stage-based.

Example:

```text
ANALYZING        10%
PREPARING        25%
TRAINING         55%
EVALUATING       75%
FINALIZING       90%
VALIDATING       100%
```

Modules should not invent incompatible meanings for `progress=100`.

A run reaching 100% must correspond to a completed lifecycle.

---

# 40. Error Model

Errors must distinguish:

```text
USER_ERROR
CONFIGURATION_ERROR
INPUT_ERROR
COMPATIBILITY_ERROR
DATA_ERROR
TRAINING_ERROR
MODEL_ERROR
BUILD_ERROR
DEPLOYMENT_ERROR
RUNTIME_ERROR
VALIDATION_ERROR
SYSTEM_ERROR
```

Errors should preserve:

```text
runId
stage
component
cause
artifact
recoverability
```

The UI should display the meaningful failure stage rather than a generic:

```text
Operation failed
```

---

# 41. Cancellation

All long-running workflows must support cancellation consistently.

Cancellation must propagate:

```text
UI
 ↓
Run
 ↓
Orchestrator
 ↓
Executor
 ↓
External process
```

Cancellation must result in:

```text
CANCELLED
```

rather than an accidental:

```text
FAILED
```

unless the cancellation itself failed.

---

# 42. External Processes

Ollama, llama.cpp, Maven, Tycho, Git, Java launchers and other external processes must be accessed through dedicated adapters.

Domain logic must not directly execute shell commands.

Conceptually:

```text
Domain Service
     ↓
Platform Adapter
     ↓
External Process
```

This is especially important for Self-Dev and model export.

---

# 43. OSGi / Eclipse RCP Boundaries

The Eclipse RCP layer should remain a presentation/application layer.

It must not become the owner of domain logic.

Desired direction:

```text
RCP UI
  ↓
Application Service
  ↓
Domain
  ↓
Infrastructure
```

Avoid:

```text
View
 ↓
Manager
 ↓
random utility
 ↓
another UI controller
 ↓
static singleton
```

OSGi services should represent stable module boundaries.

---

# 44. Static State and Session Isolation

The audit must identify all:

* static mutable fields
* global caches
* singleton session objects
* static executors
* static model references
* static configuration
* global lifecycle state

Particular attention is required for Darwin, Forge and Self-Dev.

A user/session/run must not inherit state from another run.

The preferred model is:

```text
EvoSession
   └── EvoRun
        └── RunContext
```

with explicit ownership.

---

# 45. Event Architecture

Cross-module communication should use a controlled event model.

Examples:

```text
RunStarted
StageChanged
ProgressChanged
ArtifactCreated
EvaluationCompleted
RunCompleted
RunFailed
RunCancelled
```

Events must not replace direct service calls everywhere.

Use:

```text
direct calls = commands / ownership
events       = notifications / observation
```

This distinction prevents an event-driven architecture from becoming another source of hidden control flow.

---

# 46. Naming Unification

The audit must identify inconsistent terminology.

Examples requiring one canonical vocabulary:

```text
Forge
Training
Generation
Model
Artifact
Dataset
Source
Target
Run
Job
Session
Iteration
Variant
Branch
Trajectory
Agent
Engine
Controller
Manager
Service
Provider
Executor
Orchestrator
```

Avoid having several names for the same concept.

Especially investigate classes named:

```text
*Manager
*Controller
*Engine
*Service
*Processor
*Handler
```

which may actually perform the same responsibility.

Naming must reflect architectural responsibility.

---

# 47. Manager / Controller / Engine / Service Rules

The project should define simple semantics.

### Controller

Coordinates external/application requests.

### Service

Provides a domain capability.

### Engine

Performs a specialized computational operation.

### Orchestrator

Owns a workflow.

### Executor

Executes a planned operation.

### Manager

Should only be used when it genuinely manages a resource/lifecycle.

Avoid generic `Manager` classes that become uncontrolled god objects.

---

# 48. Agents

Agents should represent reasoning capabilities, not hidden application orchestration.

For example:

```text
PlannerAgent
CriticAgent
AnalyticAgent
ValidatorAgent
RepairAgent
FinalResponseAgent
```

They should not secretly own:

* global application state
* UI state
* filesystem lifecycle
* model registry
* Git lifecycle

Agents operate through explicit services and contexts.

---

# 49. Model Registry

There should be one conceptual model registry.

It should know:

```text
model identity
path
format
architecture
hash
parent
status
provenance
evaluation
compatibility
```

It must distinguish:

```text
DISCOVERED
VALID
INVALID
TRAINING
READY
ARCHIVED
```

The registry must not become a second copy of the model itself.

---

# 50. Dataset Registry

Likewise:

```text
Dataset identity
Source
Revision
Hash
Format
Size
Token count
Language
Domain
Quality
Split
Status
```

`.evodata` artifacts should be discoverable through one canonical dataset model.

---

# 51. Compatibility

Before combining artifacts, the system must perform explicit compatibility checks.

For model continuation:

```text
architecture
dimensions
vocabulary
tokenizer
tensor format
protocol version
```

For dataset:

```text
format
schema
tokenizer compatibility
encoding
split
metadata
```

For exporters:

```text
supported architecture
supported tensor types
supported tokenizer
supported protocol
```

Incompatible operations must fail before expensive execution.

---

# 52. Preflight

Every major operation should have a preflight phase.

Example:

```text
REQUEST
  ↓
PREFLIGHT
  ↓
EXECUTION
```

Preflight should verify:

* inputs
* paths
* permissions
* disk space
* memory
* GPU/VRAM
* model compatibility
* dataset validity
* configuration
* external tools
* output location

This is particularly important for Forge and Self-Dev.

---

# 53. Cache and Temporary Data

The audit must distinguish:

```text
CACHE
TEMPORARY
CHECKPOINT
ARTIFACT
OUTPUT
```

They must never be semantically interchangeable.

A cache can be deleted.

A completed artifact cannot.

A checkpoint may be resumable.

A temporary file has no durable identity.

---

# 54. Resume / Recovery

Long operations should be resumable where technically appropriate.

The run must record its stage and artifacts.

For example:

```text
Dataset preparation completed
Training interrupted
```

should not necessarily require re-downloading and rebuilding the dataset.

Likewise:

```text
training completed
evaluation interrupted
```

should allow evaluation to resume.

---

# 55. No Fake Success

A major architectural requirement:

> EVO must never report successful completion when only an intermediate stage succeeded.

Examples:

```text
downloaded ≠ dataset READY
compiled ≠ application READY
trained ≠ model READY
exported ≠ exported model VALID
started process ≠ working EVO
```

Every operation needs final validation.

---

# 56. End-to-End Definitions of Done

### Dataset

```text
source analyzed
+
data extracted
+
filtered
+
deduplicated
+
split
+
token accounting valid
+
artifact validated
=
READY
```

### Model

```text
training completed
+
weights valid
+
metadata valid
+
model protocol valid
+
native load successful
+
inference smoke test successful
=
READY
```

### Self-Dev

```text
source mutation
+
compile
+
build
+
deployment
+
runtime startup
+
EVO functionality
+
validation
=
SUCCESS
```

---

# 57. UI Consistency

All EVO UI components should use common visual and interaction conventions.

The audit should identify differences in:

* dialogs
* buttons
* status messages
* progress
* error display
* configuration forms
* artifact selectors
* logs
* validation indicators
* lifecycle labels
* terminology

The target is not visual uniformity for its own sake.

The goal is that a user feels they are operating **one EVO system**.

---

# 58. Artifact Selection

The project should converge on one semantic artifact selector.

It must distinguish:

```text
DATASET
EVODATA
MODEL
EVO MODEL
DIRECTORY
GIT REPOSITORY
```

The UI must never depend on ambiguous assumptions such as:

```text
"target directory"
```

when the selected object is actually:

```text
model artifact
dataset artifact
repository
```

---

# 59. Source vs Target Terminology

The project should explicitly distinguish:

```text
SOURCE
```

from:

```text
TARGET
```

A source provides input material.

A target describes what an operation acts upon.

For Forge:

```text
Base Model = target initialization state
Training Sources = data inputs
Output Model = resulting artifact
```

This removes ambiguity from the current `Select Target` terminology.

---

# 60. Recommended Unified Forge Request

Conceptually:

```text
ForgeRequest
│
├── baseModel
│
├── sources[]
│
├── objective
│
├── strategy
│
├── datasetBudget
├── modelConfiguration
├── trainingConfiguration
├── evaluationConfiguration
│
└── outputConfiguration
```

This should become the central semantic object for Forge.

The UI, CLI and future APIs should all ultimately produce this object.

---

# 61. Recommended Unified Self-Dev Request

Conceptually:

```text
SelfDevRequest
│
├── repository
├── objective
├── mutationPlan
├── buildConfiguration
├── deploymentConfiguration
├── runtimeConfiguration
└── validationConfiguration
```

Again:

```text
UI
CLI
Supervisor
future API
```

should converge on the same semantic request.

---

# 62. Protocol Inventory

Create an explicit inventory of every protocol currently present:

```text
.evo
.evodata
LLM responses
reasoning responses
Forge job state
Darwin state
Self-Dev state
Supervisor commands
Git EVO metadata
model metadata
dataset metadata
events
progress
external process communication
```

For each protocol document:

```text
version
schema
owner
producer
consumer
lifecycle
compatibility
error semantics
```

No undocumented protocol should remain.

---

# 63. Versioning Rules

Every persistent protocol should have explicit versioning.

At minimum:

```text
artifact format version
schema version
run/provenance version
```

Versioning must be independent from:

```text
application version
```

because an application update must not silently redefine an old artifact.

---

# 64. Backward Compatibility

The consolidation must classify compatibility requirements.

For every old artifact/protocol:

```text
READ
WRITE
MIGRATE
DEPRECATE
DROP
```

Do not create compatibility code indefinitely.

Compatibility must have a defined boundary.

---

# 65. Dependency Direction

The architecture should follow a mostly one-way dependency flow:

```text
UI
 ↓
Application / Orchestration
 ↓
Domain
 ↓
Infrastructure
```

and:

```text
Forge
Darwin
Self-Dev
Inference
```

should depend on shared core abstractions rather than on each other unnecessarily.

Avoid:

```text
Forge → UI
UI → Forge
Forge → SelfDev
SelfDev → UI
Inference → Forge UI
```

Such cycles indicate architectural leakage.

---

# 66. Module Dependency Audit

Produce a dependency graph and identify:

```text
cycles
high fan-in
high fan-out
god modules
god services
UI leakage
infrastructure leakage
duplicate utilities
duplicate models
```

Each module should have:

```text
one clear purpose
one public boundary
minimal dependencies
stable contracts
```

---

# 67. God Object Detection

Special attention must be given to classes that simultaneously:

* parse configuration
* manipulate UI
* start threads
* call LLMs
* manipulate files
* execute Git
* train models
* manage state
* emit logs
* update progress

Such classes are historical symptoms of architectural growth.

They should be marked for responsibility decomposition in a later implementation milestone.

This milestone only defines the desired boundary.

---

# 68. Duplicate Utility Detection

Search for multiple implementations of:

```text
file handling
JSON
hashing
process execution
path handling
logging
progress
model metadata
configuration
Git
HTTP
LLM calls
serialization
validation
```

The target architecture should have canonical infrastructure services.

---

# 69. Security / Safety Boundary

External commands and filesystem operations must be explicit.

Particular attention:

```text
Self-Dev
Git
build commands
deployment
model import
dataset extraction
external URLs
```

No component should execute arbitrary commands simply because it received a string from an LLM.

LLM-generated intent must become a validated structured command before execution.

---

# 70. LLM as Planner, Not Authority

The architectural rule should be:

```text
LLM proposes
EVO validates
EVO executes
EVO evaluates
```

Never:

```text
LLM directly controls filesystem/process/system state
```

This principle should apply equally to:

* Darwin
* Forge
* Self-Dev
* mutation
* coding agents

---

# 71. Mutation Architecture

The future `mutation` component should fit into the same model.

Conceptually:

```text
EVO
 ↓
Mutation Request
 ↓
Repository Workspace
 ↓
LLM Planning
 ↓
Controlled Mutation
 ↓
Diff
 ↓
Build
 ↓
Test
 ↓
Validation
 ↓
Commit / Rollback
```

It should not become an independent competing Self-Dev framework.

---

# 72. Runtime / Application Architecture

The Eclipse product, native inference and supervisor must be treated as runtime environments.

The logical relationship is:

```text
EVO DOMAIN
     │
     ├── Eclipse RCP application
     ├── headless runtime
     ├── native inference
     └── supervisor-controlled runtime
```

The domain must not depend on a particular launcher.

---

# 73. Testing Architecture

Testing must be aligned with architectural layers.

### Unit

Domain logic.

### Contract

Artifact/protocol compatibility.

### Integration

Subsystem boundaries.

### End-to-End

Complete workflows.

Critical end-to-end scenarios:

```text
source → evodata
evodata → forge → evo
evo → native inference
evo → exporter
evo + new source → child evo
repo → selfdev → build → runtime
git → model lineage
reasoning LLM → thinking + final
```

---

# 74. Golden Test Artifacts

Define small canonical test artifacts:

```text
minimal.evodata
minimal.evo
minimal codebase
minimal Forge request
minimal SelfDev project
minimal reasoning response
```

They become protocol regression fixtures.

This prevents every test from constructing its own incompatible fake representation.

---

# 75. Documentation Architecture

Documentation should follow the same conceptual model.

Recommended:

```text
docs/
├── architecture/
│   ├── overview.md
│   ├── modules.md
│   ├── lifecycle.md
│   ├── artifacts.md
│   ├── protocols.md
│   └── dependencies.md
│
├── protocols/
│   ├── evo-format.md
│   ├── evodata-format.md
│   ├── forge.md
│   ├── supervisor.md
│   └── git-evo.md
│
├── milestones/
│
└── operations/
```

The architecture documentation becomes the reference point for future development.

---

# 76. Canonical End-to-End EVO Model

After consolidation, the whole system should be explainable with this diagram:

```text
                         ┌────────────────────┐
                         │      EVO UI        │
                         │  COMMANDER / RCP   │
                         └─────────┬──────────┘
                                   │
                                   ▼
                         ┌────────────────────┐
                         │    EVO REQUEST     │
                         └─────────┬──────────┘
                                   │
                                   ▼
                         ┌────────────────────┐
                         │  ORCHESTRATION     │
                         │   PLAN / EXECUTE   │
                         └─────────┬──────────┘
                                   │
            ┌──────────────────────┼──────────────────────┐
            │                      │                      │
            ▼                      ▼                      ▼
       ┌─────────┐            ┌─────────┐            ┌─────────┐
       │  CHAT   │            │ DARWIN  │            │  FORGE  │
       └────┬────┘            └────┬────┘            └────┬────┘
            │                      │                      │
            │                      │                 ┌────┴────┐
            │                      │                 │ SOURCES │
            │                      │                 └────┬────┘
            │                      │                      │
            │                      │                 ┌────▼────┐
            │                      │                 │ EVODATA  │
            │                      │                 └────┬────┘
            │                      │                      │
            │                      │                 ┌────▼────┐
            │                      │                 │ TRAINING │
            │                      │                 └────┬────┘
            │                      │                      │
            │                      │                 ┌────▼────┐
            │                      │                 │   EVO    │
            │                      │                 │  MODEL   │
            │                      │                 └────┬────┘
            │                      │                      │
            └──────────────┬───────┴──────────────────────┘
                           │
                           ▼
                  ┌───────────────────┐
                  │    EVALUATION     │
                  └─────────┬─────────┘
                            │
                            ▼
                  ┌───────────────────┐
                  │  ARTIFACT / GIT   │
                  └───────────────────┘


              SELF-DEV / MUTATION
                       │
                       ▼
                ┌───────────────┐
                │   CODEBASE    │
                └───────┬───────┘
                        ▼
                  PLAN / MUTATE
                        ▼
                     BUILD
                        ▼
                    DEPLOY
                        ▼
                     START
                        ▼
                   VALIDATE
                        ▼
                 ACCEPT / ROLLBACK
```

---

# 77. Target Architectural Invariants

After consolidation the following invariants should hold.

### I1 — One authoritative lifecycle owner

A run has exactly one state authority.

### I2 — UI never owns domain orchestration

UI starts and observes operations.

### I3 — Artifacts have canonical formats

`.evodata` and `.evo` have explicit protocols.

### I4 — Model and dataset are different concepts

`.evo` is model state.

`.evodata` is training data.

### I5 — Sources are composable

Multiple heterogeneous sources can participate in one Forge operation.

### I6 — Base model is explicit

Existing `.evo` is a base model, not a dataset.

### I7 — Completed artifacts are immutable

New evolution produces new versions.

### I8 — Provenance is first-class

Every model can explain its origin.

### I9 — LLM proposes, EVO validates

LLM output is never direct system authority.

### I10 — Every operation has final validation

Intermediate success is not final success.

### I11 — One canonical configuration model

No hidden duplicate configuration.

### I12 — One canonical terminology

Same concept = same name.

### I13 — One canonical artifact ownership model

No competing producers.

### I14 — External processes are adapters

Domain code does not directly manage platform commands.

### I15 — No hidden global mutable state

Runs and sessions are isolated.

---

# 78. Consolidation Process

The implementation work following this milestone should happen in phases.

## Phase 1 — Architecture Discovery

Produce:

```text
module map
package map
dependency graph
class responsibility map
artifact map
configuration map
filesystem map
protocol map
UI map
lifecycle map
```

No refactoring yet.

---

## Phase 2 — Classification

Every relevant component receives:

```text
KEEP
MERGE
MOVE
REPLACE
DEPRECATE
REMOVE
```

and:

```text
AUTHORITATIVE
COMPATIBILITY
LEGACY
OBSOLETE
```

---

## Phase 3 — Canonical Contracts

Define:

```text
EvoRequest
EvoRun
EvoArtifact
TrainingSource
ForgeRequest
ForgeRun
EvoModel
TrainingDataset
EvaluationResult
SelfDevRun
```

only where equivalent existing objects do not already exist.

Do not create duplicate classes merely because the proposed name is cleaner.

---

# 78. Consolidation Process (Continued)

## Phase 4 — Boundary Stabilization

Stabilize:

```text
UI
Application
Domain
Infrastructure
```

and:

```text
Forge
Darwin
Inference
Self-Dev
Git
```

interfaces.

---

## Phase 5 — Remove Duplication

Only after boundaries are stable:

```text
merge duplicate services
remove obsolete managers
remove duplicate protocols
remove duplicate configuration
remove duplicate utilities
remove dead code
```

---

## Phase 6 — Artifact/Protocol Consolidation

Make:

```text
.evodata
.evo
Forge metadata
Git EVO
Supervisor protocol
```

authoritative.

---

## Phase 7 — UI Consolidation

Only after backend semantics are stable:

```text
dialogs
selectors
progress
logs
errors
settings
artifact views
```

should be aligned.

---

## Phase 8 — End-to-End Validation

Validate the complete system rather than isolated modules.

Required paths:

```text
HF/local source
      ↓
EVODATA
      ↓
Forge
      ↓
EVO
      ↓
native inference
```

and:

```text
EVO model
      +
new source
      ↓
continued Forge
      ↓
child EVO model
      ↓
evaluation
```

and:

```text
EVO Git repo
      ↓
Self-Dev
      ↓
build
      ↓
deploy
      ↓
start
      ↓
runtime validation
```

---

# 79. Definition of Architectural Completion

This milestone is complete when an engineer who did not participate in the historical development can understand the project by reading:

```text
architecture/overview.md
architecture/modules.md
architecture/lifecycle.md
protocols/evo-format.md
protocols/evodata-format.md
```

and then navigate the source tree without discovering contradictory architectural concepts.

The project should be explainable in a few statements:

> EVO receives a request.

> EVO plans and executes an evolution workflow.

> Data sources become normalized `.evodata` artifacts.

> Forge trains or continues an EVO model.

> `.evo` is the canonical native model artifact.

> Inference consumes `.evo`.

> Darwin optimizes decisions rather than duplicating subsystems.

> Self-Dev evolves the EVO codebase through controlled build/deploy/runtime validation.

> Git preserves source and model lineage.

> UI observes and controls these workflows but does not own their domain logic.

---

# 80. Final Architectural Objective

The desired result is not a large enterprise architecture.

It is the opposite.

The objective is:

```text
FEWER CONCEPTS
FEWER OWNERS
FEWER PROTOCOLS
FEWER DUPLICATES
FEWER SPECIAL CASES
CLEARER BOUNDARIES
EXPLICIT LIFECYCLES
IMMUTABLE ARTIFACTS
REPRODUCIBLE RUNS
```

The architecture should feel like one system:

```text
                  EVO
                   │
          ┌────────┴────────┐
          │                 │
       EVOLUTION         ARTIFACTS
          │                 │
    ┌─────┼─────┐       ┌───┴────┐
    │     │     │       │        │
  DARWIN FORGE SELFDEV  EVODATA  EVO
    │     │     │                  │
    └─────┴─────┴──────────────────┘
                   │
               EVALUATION
                   │
                GIT EVO
```

The key architectural idea is:

> **EVO should not contain independent “Forge architecture”, “Darwin architecture”, “Self-Dev architecture”, “Inference architecture” and “UI architecture”. It should contain one EVO architecture with specialized capabilities.**

This milestone therefore establishes the architectural baseline against which all future features, refactoring and Jules tasks must be evaluated.

No new subsystem should be introduced unless its responsibility cannot be expressed cleanly within this model.

No existing subsystem should remain merely because it was historically implemented that way.

The target is a **compact, modular, internally consistent and evolvable EVO architecture**.
