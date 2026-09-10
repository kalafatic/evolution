# Configuration Precedence and Canonical Owners

**Document Identifier**: `docs/architecture/configuration.md`
**Date**: September 9, 2026

---

## 1. Canonical Configuration Hierarchy

Configuration settings across the EVO platform adhere to a strict 5-tier precedence hierarchy:

```text
       ┌────────────────────────┐
       │ EXPLICIT USER OVERRIDE │ (Highest Precedence)
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │     JOB / RUN CONFIG   │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │     PROJECT CONFIG     │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │     GLOBAL CONFIG      │
       └───────────┬────────────┘
                   │
                   ▼
       ┌────────────────────────┐
       │        DEFAULTS        │ (Lowest Precedence)
       └────────────────────────┘
```

---

## 2. Configuration Mechanism Mapping

| Setting Category | Storage Mechanism | Canonical Owner | Overridden By |
| :--- | :--- | :--- | :--- |
| **System / Platform** | `eclipse.ini`, `plugin_customization.ini` | OSGi Framework / Workspace | Environment Variables / System Properties |
| **UI Preferences** | Eclipse `IDialogSettings` / `RuntimeProjection` | `eu.kalafatic.evolution.view` | Form Field Entries |
| **Forge Training State** | `ForgeSession` / `uiState` | `ForgeSessionManager` | `ForgeSettingsDialog` explicitly passed params |
| **LLM Provider API Keys** | Encrypted `TokenSecurityService` | `ProviderConfig` | Session-level task overrides |
| **Self-Dev Supervisor** | `application.json` / `SupervisorConfiguration` | `SelfDevSupervisor` | Command-line HTTP `/start` arguments |
