# Use Case: Simple Task Execution

## Scenario
A user wants to create a simple utility class in a Java project.

## 1. Intent Analysis
- **User Prompt**: "Create a StringUtils class with a capitalize method in package com.example.util"
- **IntentService**: Detects `IMPLEMENTATION` intent.
- **EPS Calculation**:
    - Ambiguity: Low (Specific class and method mentioned).
    - Risk: Low (New file creation).
    - Complexity: Low.
    - **EPS Score**: ~0.20 (Below 0.25 threshold).

## 2. Orchestration Routing
- Since EPS < 0.25, `DarwinFlow` delegates to `AtomicFlow`.

## 3. Execution Pipeline
1. **PLAN**: `AtomicFlow` generates a single-step plan to write the file.
2. **EXECUTE**: `TaskExecutor` calls the LLM to generate the Java code and writes it to the disk.
3. **VERIFY**: `ValidatorAgent` checks if the file exists and contains valid Java syntax.
4. **DONE**: Kernel transitions to `DONE` and returns the final response.

## Key Takeaway
For low-pressure tasks, the system avoids the overhead of Darwinian branching and executes directly while still maintaining the kernel's supervision and verification.
