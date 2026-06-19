# Runtime Event Bus

## Overview
The Runtime Event Bus is the "Spinal Cord" of the EVO platform. It provides a decoupled communication mechanism for bundles to publish and subscribe to structured signals.

## Event Model
Events are represented by the `RuntimeEvent` class, which includes:
- **Type**: Defined in `RuntimeEventType`.
- **SessionId**: Ensuring strict session isolation.
- **Source**: The component that emitted the event.
- **Payload**: The actual data (String, JSONObject, or POJO).
- **Metadata**: Key-value pairs for additional context.

## Core Event Types
- `FLOW_STARTED` / `FLOW_COMPLETED`: Lifecycle of an evolution task.
- `MODE_CHANGED`: Transitions between Local, Mediated, and Self-Dev modes.
- `TASK_STARTED` / `TASK_COMPLETED`: Individual agent task execution.
- `BRANCH_CREATED` / `WINNER_SELECTED`: Darwin engine milestones.
- `CONFIGURATION_UPDATED`: UI-driven setting changes.
- `SUPERVISOR_STATUS_CHANGED`: State transitions in the kernel.

## Session Isolation
The Event Bus is strictly session-scoped. A `RuntimeEventBus` instance is owned by a `SessionContext`.
- **Enforcement**: The bus throws an `IllegalArgumentException` if an event is published with a `sessionId` that doesn't match the bus's ID.
- **Benefit**: Prevents cross-talk and UI "ghosting" when multiple users or agents are active.

## Usage Patterns

### Publishing an Event
```java
eventBus.publish(new RuntimeEvent(
    RuntimeEventType.TASK_COMPLETED,
    sessionId,
    "MyComponent",
    "Task ID 123 finished"
));
```

### Subscribing to Events
Components register a `RuntimeEventListener`. In the UI, the `ConversationOutputController` listens for events to update the chat log and progress animations.

## Integration with Guidance
The `GuidanceEngine` (in the `creatic` bundle) analyzes the stream of events to provide real-time tips and "Cognitive Pressure" warnings to the user.

## Related
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [RuntimeEventBus.java](../../../eu.kalafatic.evolution.controller/src/eu/kalafatic/evolution/controller/workflow/RuntimeEventBus.java)
