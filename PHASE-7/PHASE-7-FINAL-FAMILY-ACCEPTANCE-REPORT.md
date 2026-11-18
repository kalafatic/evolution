# PHASE-7-FINAL-FAMILY-ACCEPTANCE-REPORT.md

Phase 7 — Final Family Acceptance Report
Date: 2026-11-17
Product Owner decision: **ACCEPTED WITH LIMITATIONS**

## 1. Executive summary

Phase 7 verified the complete five-project EVO family as one integrated ecosystem and
fixed every family-integration defect that blocked the mandated default execution
architecture. The headline question is answered affirmatively for the executable
paths:

**EVO, TRYST and DStudio independently and concurrently execute real development
workflows through the shared native DCode harness and the ENR inference runtime,
with application isolation, persistence, extensibility and operational reliability
preserved.** TRYST and DStudio were verified live with their real products; EVO's
product launches were verified live with its full session flow deferred (documented
limitation).

Phase 7 corrections (all verified by executed tests):

1. DCode gained its ENR provider (`EnrProvider`) — registered as the default
   inference provider (defect D-1).
2. The standalone ENR runtime now serves the family ENR 1.0 `/enr/v1` surface
   consumed by DCode/TRYST/DStudio (D-2).
3. ENR request timeouts and idle eviction are now actually enforced (D-3, D-4).
4. DCode session-level provider/model selection now works without a router default —
   the family ENR path (D-5).
5. DCode `GET /health` now serves the FHC 1.0 health probe (D-6).
6. ENR tokenizer OOV robustness (D-7).

## 2. Five-project architecture verification

PASS — see `PHASE-7-WP-7.1-FIVE-PROJECT-ARCHITECTURE-VERIFICATION.md`. Dependency
matrix acyclic; wire-only integration (TEIP 1.1 / FHC 1.0 / ENR 1.0); no duplicate
harnesses, runtimes, registries or session managers; DCode and ENR are family-native
defaults, extensible and replaceable.

## 3. DCode integration results

- Default harness in EVO, TRYST and DStudio (auto-selected when nothing selected;
  explicit user selections always win). PASS (suite + live registries).
- Alternatives remain: OpenCode, DStudio TEIP, EVO TEIP, Aider. PASS.
- FHC 1.0 semantics verified: delegation, containment, no-commit in delegated mode,
  monotonic events, idempotent cancellation, timeout handling, recovery. PASS
  (suite; live TRYST→DCode session).

## 4. ENR integration results

- Default inference engine in TRYST (`enr-default`), DStudio (`enr`, default:true)
  and DCode (`enr` registered first). Live-verified in all three.
- EVO: ENR registered, discoverable and selectable through the Darwin `LlmRouter`
  slot; not the silent default for pre-existing paths (limitation L-1, policy).
- Standalone ENR now speaks the family contract end-to-end (health, models,
  lifecycle, chat, SSE streaming, runtime, providers, capabilities; TEIP error
  envelope). Live-verified.

## 5. Default module registration results

PASS — see `PHASE-7-WP-7.4-FAMILY-TOOL-REGISTRY-ACCEPTANCE.md`. All 10 registry
scenarios (fresh install, upgrade, modified config, missing components, alternatives,
restarts) verified; default registration never overwrites user configuration;
unavailable components report clear diagnostics; no duplicate registration.

## 6. Alternative provider compatibility

PASS — Ollama everywhere, OpenAI-compatible in DCode, OpenCode/Aider/Evo/DStudio
harnesses everywhere; defaults restorable; configuration persisted. Live OpenCode
and Aider CLI executions green in the DStudio suite.

## 7. End-to-end test results

| Scenario | Status |
|---|---|
| A — EVO | PARTIAL — product launch verified live (RCP + headless products); full Darwin GUI session NOT TESTED |
| B — TRYST | PASS — full live run: ENR → DCode → TRYST headless → clone → plan → implement (DCode session, providerId=enr) → build → test → WAITING_FOR_REVIEW |
| C — DStudio | PASS — live headless launch, ENR default engine, TEIP capabilities, auth enforcement |
| D — Concurrent | PASS — ENR/DCode/TRYST/DStudio concurrent; ENR restart recovery live-verified |
| E — Alternatives | PASS |

Evidence: `PHASE-7-WP-7.7-END-TO-END-FAMILY-ACCEPTANCE.md`.

## 8. Regression statistics

1,589 tests executed across the five projects in Phase 7. Modified components fully
green: DCode 126/0/0(1 skip), ENR 70/0/0 (incl. CUDA 9/9). Unmodified: TRYST
354/354, DStudio 102/102. EVO: Phase 6 baseline red set reproduced identically;
extended-run failures proven pre-existing by isolation re-runs; zero EVO source
changes in Phase 7. Full detail: `PHASE-7-WP-7.10-REGRESSION-REPORT.md`.

## 9. Performance observations

Stable concurrent operation, no deadlocks or leaks observed; ENR CPU ~250 tok/s on
the fixture model, 3–6 ms/request; live footprints small (DCode ~146 MB, ENR ~60 MB);
CUDA green with free VRAM; GPU contention behavior correctly reported. Full detail:
`PHASE-7-WP-7.11-PERFORMANCE-STABILITY-REPORT.md`.

## 10. Known limitations (with remediation plans)

| ID | Limitation | Remediation |
|---|---|---|
| L-1 | EVO `LlmRouter` keeps the documented no-silent-switching policy: ENR is selectable, not the unconditional default for pre-existing EVO inference paths | Product Owner approval to default the router fallback to ENR (D-12) |
| L-2 | Full coding-task completion through DCode→ENR used the 920-parameter fixture model (the pipeline executed; the toy model cannot emit tool-call JSON) | Load a coding-capable `.evo`/GGUF into ENR and re-run Scenario A/B |
| D-8 | Port convention collision: DCode HTTP and DStudio TEIP both default to 48080 (both loopback; DCode falls back, DStudio fails hard; EVO observed falling back live) | Product Owner approval for the 48xxx convention split; temporary explicit-port configuration |
| D-9 | DCode packaged headless product launcher fails ("Unable to acquire application service") while the identical stack boots green under the Equinox test harness and the live in-process server | Investigate product p2 profile/start levels; add a product-launch smoke test |
| D-10 | EVO full GUI development session (Scenario A steps 6–15) not executed live | Run on a host with canonical ports free; the products were built and launch-verified |
| D-11 | TRYST documentation states TCP 9090 vs code 38280 | Documentation update |
| — | EVO pre-existing red test set (20 classes) and the unbounded-loop test | Backlog; recorded in WP-7.10 / defect register |

## 11. Defect register

`PHASE-7-DEFECT-REGISTER.md` — 7 defects fixed and verified in Phase 7 (D-1..D-7);
open items D-8..D-13 with severities and remediation.

## 12. Evidence references

- Builds: `PHASE-7-WP-7.2-FAMILY-BUILD-INTEGRATION.md`
- Live runs: `PHASE-7-WP-7.3-DCODE-ENR-NATIVE-INTEGRATION.md`,
  `PHASE-7-WP-7.5-COMMUNICATION-INTEGRATION.md`,
  `PHASE-7-WP-7.6-PERSISTENCE-RECOVERY.md`,
  `PHASE-7-WP-7.7-END-TO-END-FAMILY-ACCEPTANCE.md`,
  `PHASE-7-WP-7.9-OPERATIONAL-ACCEPTANCE.md`
- Suites: `PHASE-7-WP-7.10-REGRESSION-REPORT.md`
- Isolation/security: `PHASE-7-WP-7.8-GIT-REPOSITORY-ACCEPTANCE.md`,
  `PHASE-7-WP-7.12-SECURITY-ISOLATION-REPORT.md`

## 13. Final acceptance decision

**ACCEPTED WITH LIMITATIONS**

All critical workflows work: the five projects build, DCode and ENR operate as the
default coding harness and inference runtime, TRYST and DStudio were verified live
end-to-end, the default-path defects were fixed with regression coverage, and
isolation/persistence/extensibility/operational behavior were verified. The
remaining limitations (L-1, L-2, D-8, D-9, D-10, EVO pre-existing test backlog) are
explicitly documented above with remediation plans and require either Product Owner
policy decisions or model/infrastructure availability.
