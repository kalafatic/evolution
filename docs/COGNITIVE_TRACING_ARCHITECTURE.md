# Cognitive Tracing Architecture

## Vision
The Cognitive Tracing architecture evolves the platform from simple event-emitting orchestration to a causally explainable reasoning infrastructure. It allows the kernel to explain *why* decisions were made, *why* failures occurred, and *why* specific paths were taken.

## Core Components

### 1. CognitiveTrace
Represents a complete causal chain of reasoning events for a single orchestration lifecycle. It tracks:
- Intent expansion paths
- Clarification decisions
- Branch mutations
- Evaluation signals
- Scheduling decisions
- Resolver outcomes
- Workspace retrievals and memory decay
- State transitions

### 2. CausalNode
A single reasoning event inside the trace graph.
- `nodeId`: Unique identifier
- `nodeType`: e.g., `INTENT_EXPANSION`, `SCHEDULING`, `RESOLVER_POLICY`, `MUTATION`
- `sourceComponent`: The component that emitted the node
- `inputReferences` / `outputReferences`: Linkage to previous/next artifacts or decisions
- `confidence`: The degree of certainty in the decision
- `rationale`: Human-readable explanation of the reasoning

### 3. IntrospectionEngine
The central diagnostics layer responsible for:
- Reconstructing reasoning chains from raw nodes
- Generating causal summaries for humans
- Identifying "noisy" components (high activity, low confidence)
- Compressing long traces by collapsing redundant events

### 4. FailurePropagationAnalyzer
Identifies the root cause of failures and detects cascading instability.
- `identifyRootCauses()`: Distinguishes between primary failures and secondary effects.
- `detectCascadingInstability()`: Flags traces where confidence is consistently degrading across the chain.

### 5. ReplayEngine
Supports deterministic replay of reasoning cycles by walking the `CognitiveTrace` and simulating the original kernel execution path.

## Integration Points

- **IterationManager**: Records all state transitions and triggers memory decay tracing.
- **DarwinFlow**: Records variant generation (mutation) and integrates with the `ActivationResolver`.
- **IntentExpansionEngine**: Records intent hypotheses and confidence rationales.
- **KernelScheduler**: Records scheduling decisions, variants selected, and backpressure adjustments.
- **ActivationResolver**: Records the evaluation of specific resolver policies and the final activation decision.
- **SemanticWorkspace**: Records artifact retrieval and the pruning of stale artifacts during decay.

## Replay Workflow
1. Export a `CognitiveTrace` to JSON.
2. Load the JSON into the `ReplayEngine`.
3. The engine simulates the `STATE_TRANSITION` events, allowing developers to inspect the exact kernel state at each reasoning step.

## Health Metrics
Tracing enables the calculation of `CognitiveHealth` metrics:
- **Ambiguity Pressure**: Measure of unresolved intent dimensions.
- **Mutation Stability**: Ratio of successful vs. failed variants.
- **Signal Noise Ratio**: Variance in evaluator signals for the same variant.
- **Resolver Confidence Consistency**: Stability of confidence scores across multiple policies.
