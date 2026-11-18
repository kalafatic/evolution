# PERFORMANCE AND RELIABILITY

## 1. Live latency measurements (HTTP end-to-end, running EVO, 20 samples each)

| Operation | p50 | p95 | min | max |
|---|---|---|---|---|
| context_get (8000-token budget) | 32.8 ms | 38.2 ms | 26.0 ms | 236.7 ms |
| context_get (400-token budget) | 28.3 ms | 35.0 ms | 24.7 ms | 37.1 ms |
| memory_store | 18.9 ms | 22.3 ms | 17.9 ms | 30.2 ms |
| memory_search | 16.5 ms | 28.2 ms | 15.0 ms | 33.9 ms |
| conversation_history_get | 18.1 ms | 21.3 ms | 13.0 ms | 21.7 ms |
| session_snapshot | 21.3 ms | 24.2 ms | 19.7 ms | 24.2 ms |
| decision_search | 16.4 ms | 22.8 ms | 15.6 ms | 28.3 ms |
| session_search | 15.9 ms | 17.0 ms | 14.9 ms | 17.1 ms |

Architecture-document targets (context_get < 200 ms p95 warm; memory_search
< 50 ms p95): met with margin.

## 2. Reliability checks

- Clean startup: repeated (5+ times across verification); log shows the full
  server sequence finishing without unhandled exceptions.
- Port conflict resilience: 28080 occupied by an external process — detected
  by PortAvailabilityChecker, logged, primary marked FAILED, all other
  servers (28180, 28280, supervisor) started normally. Deterministic ports
  preserved (no silent shifts).
- Shutdown/restart: EVO killed and restarted repeatedly; all persistent
  cognitive data (memory, decisions, preferences, sessions) re-read after
  restart.
- Runtime log: exactly one expected exception line (the 28080 conflict
  IOException); no SEVERE/ERROR noise otherwise across hours of requests.
- Memory/CPU: ~700 MB RSS, ~2.8% CPU during idle with servers up.
- Background workers: consolidation executor is a single bounded daemon
  thread; no unbounded queues (inspection).
- Database connections: try-with-resources everywhere in cognitive services;
  WAL + busy_timeout as established.
- No duplicate service registrations: singletons + the single 28280 server
  instance (one listener verified).
- Controlled interruption tests (DB reopen mid-write) run only against
  isolated test databases; the development DB was never used as a test
  target.

## 3. Remaining reliability notes

- No formal soak test or connection-leak profiler run (documented gap).
- Supervisor (28480) has no authentication — pre-existing, out of scope.
