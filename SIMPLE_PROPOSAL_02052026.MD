# FIX PROMPT: Simple Chat Flow Hardening

**Context:** Analysis (SIMPLE_ANALYSIS_02052026.MD) shows the greeting detection is working but can be further optimized to avoid any accidental fallthrough to the LLM.

**Task for AI Coder:**
Harden the fast-path for conversational greetings.

**Requirements:**
1. **Regex Expansion:** In `EvolutionOrchestrator.handle()`, expand the greeting regex to cover more variants (e.g., "how are you", "who are you").
2. **Early Exit:** Ensure that if a greeting is detected, no `ConversationState` loading or context analysis happens before the greeting is returned. Move the greeting check to the very top of the `handle()` method.
3. **Stateless Response:** For simple greetings, do not trigger the `ConversationState.save()` logic unless a specific user goal is already active.
