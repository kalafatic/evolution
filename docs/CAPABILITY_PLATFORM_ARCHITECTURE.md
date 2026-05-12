# Capability Platform Architecture

## Overview

The AI Kernel has evolved into a modular Capability and Plugin Platform. This architecture decouples core orchestration logic from specific implementation details, allowing for safer experimentation and easier extension.

## Core Concepts

### 1. Capability (`ICapability`)
The universal extension model. Every major subsystem is now a Capability.
- `capabilityId`: Unique identifier.
- `lifecycle`: initialize, start, stop.
- `contracts`: Explicit interfaces the capability implements.
- `health`: Stability, latency, and status telemetry.

### 2. CapabilityRegistry
The central runtime catalog for all capabilities.
- Registration and discovery.
- Contract-based resolution.
- Lifecycle management.

### 3. Capability Contracts
Explicit interfaces for subsystem interaction.
- `IEvaluationContract`: Signal production (Maven, etc.).
- `IResolverContract`: Decision authority.
- `ISchedulingContract`: Resource and budget management.
- `IMutationContract`: Darwin variant generation.
- `IWorkspaceContract`: Semantic memory and artifact management.
- `ITraceContract`: Cognitive diagnostics.

## Component Map

| Capability | Contract | Implementation |
|---|---|---|
| Scheduling | `ISchedulingContract` | `KernelScheduler` |
| Resolver | `IResolverContract` | `ActivationResolver` |
| Evaluator | `IEvaluationContract` | `Evaluator` |
| Workspace | `IWorkspaceContract` | `SemanticWorkspace` |
| Mutation | `IMutationContract` | `DarwinEngine` |
| Trace | `ITraceContract` | `CognitiveTrace` |

## Lifecycle Diagram

```text
[Registry.register] -> [Capability.initialize]
        ↓
[Registry.startAll] -> [Capability.start]
        ↓
[Runtime Execution] -> [Capability.getHealth]
        ↓
[Registry.stopAll]  -> [Capability.stop]
```

## Migration Strategy

### For New Evaluators
1. Create a class implementing `ICapability` and `IEvaluationContract`.
2. Register the instance in `KernelFactory` or `IterationManager`.
3. Orchestration flows will automatically discover it via `CapabilityRegistry`.

### For New Resolver Policies
- Policies are still used by the `ActivationResolver` capability.
- To replace the entire resolver, implement `IResolverContract` and register it.
