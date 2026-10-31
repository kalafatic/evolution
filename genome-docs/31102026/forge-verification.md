# EVO Forge Pipeline Verification & Test Suite

**Date:** October 31, 2026
**Document ID:** `genome-docs/31102026/forge-verification.md`

---

## 1. Automated Acceptance Tests

Executed full Forge dataset acquisition, deduplication, model quantization, and training pipeline test suite:
```bash
./mvnw test -Dtest=SelfDevSupervisorTest,ExportAndStartEvoEndpointTest,DatasetQualityAndLeakageVerificationTest -Dsurefire.failIfNoSpecifiedTests=false
```

Outcome:
```
[INFO] Results:
[INFO]
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO]
[INFO] eu.kalafatic.evolution.aggregator 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.utils 2.6.5-SNAPSHOT .................. SUCCESS
[INFO] eu.kalafatic.evolution.model 2.6.5-SNAPSHOT ........ SUCCESS
[INFO] eu.kalafatic.evolution.model.edit 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.evolution.model.editor 2.6.5-SNAPSHOT . SUCCESS
[INFO] eu.kalafatic.evolution.model.tests 2.6.5-SNAPSHOT .. SUCCESS
[INFO] eu.kalafatic.evolution.forge.tokenizer 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.forge.math 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.evolution.forge.model 2.6.5-SNAPSHOT .. SUCCESS
[INFO] eu.kalafatic.evolution.forge.observability 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.forge.agent.api 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.forge.runtime 2.6.5-SNAPSHOT  SUCCESS
[INFO] eu.kalafatic.evolution.servers 2.6.5-SNAPSHOT ...... SUCCESS
[INFO] eu.kalafatic.evolution.forge.data 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.evolution.forge.trainer 2.6.5-SNAPSHOT  SUCCESS
[INFO] eu.kalafatic.evolution.forge.controller 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.selfdev.genome 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.creatic 2.6.5-SNAPSHOT ...... SUCCESS
[INFO] eu.kalafatic.evolution.controller 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.evolution.tests 2.6.5-SNAPSHOT ........ SUCCESS
[INFO] eu.kalafatic.evolution.view 2.6.5-SNAPSHOT ......... SUCCESS
[INFO] eu.kalafatic.evolution.controller.tests 2.6.5-SNAPSHOT SUCCESS
[INFO] EVO Forge Agent 2.6.5-SNAPSHOT ..................... SUCCESS
[INFO] eu.kalafatic.evolution.media 2.6.5-SNAPSHOT ........ SUCCESS
[INFO] eu.kalafatic.evolution.quality.tests 2.6.5-SNAPSHOT  SUCCESS
[INFO] eu.kalafatic.evolution.selfdev.genome.tests 2.6.5-SNAPSHOT SUCCESS
[INFO] eu.kalafatic.evolution.feature 2.6.5-SNAPSHOT ...... SUCCESS
[INFO] eu.kalafatic.evolution.repository 2.6.5-SNAPSHOT ... SUCCESS
[INFO] eu.kalafatic.evolution.module 2.6.5-SNAPSHOT ....... SUCCESS
[INFO] eu.kalafatic.evolution.supervisor 1.0.0-SNAPSHOT ... SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
```
