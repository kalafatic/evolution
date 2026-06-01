# Use Case: Mediated Execution

## Scenario
A user wants to make a high-risk architectural change in `MEDIATED` mode.

## 1. Intent Analysis
- **User Prompt**: "Change the event bus from synchronous to asynchronous across the whole platform."
- **IntentExpansionEngine**: Detects `ARCHITECTURE`, `REFACTORING`, and `ANALYSIS`.
- **EPS Calculation**:
    - Ambiguity: Medium.
    - Risk: High (Cross-cutting architectural change).
    - Complexity: High.
    - **EPS Score**: ~0.85 (Above 0.60 threshold).

## 2. Orchestration Routing
- Since EPS >= 0.60, `DarwinFlow` executes the full evolutionary machine.

## 3. Evolutionary Pipeline
1. **MUTATION**: `DarwinEngine` generates 3 variants:
    - `v1`: Using `CompletableFuture`.
    - `v2`: Using a custom `ExecutorService`.
    - `v3`: Using a third-party reactive library.
2. **USER REVIEW (MEDIATED)**: The kernel pauses. The user sees all three proposals.
    - User selects `v1` but asks for it to be refined to use a specific thread pool.
3. **EXPLORATION**: The kernel executes `v1` (with refinement) in a separate branch.
4. **EVALUATION**: The kernel runs performance benchmarks and static analysis on the `v1` branch.
5. **FINAL APPROVAL**: User reviews the evaluation results and performance metrics.
6. **COMMIT**: User approves, and `v1` is merged into `main`.

## Key Takeaway
Mediated execution ensures that the human remains the ultimate authority for high-impact decisions, while the kernel handles the heavy lifting of exploration and evaluation.
