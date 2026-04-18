# Headless Execution & Build Guide

## Build Automation
To automate the full Tycho build process, use the provided build script:
```bash
./scripts/build.sh
```
This script runs `mvn clean verify -DskipTests=true` to regenerate all OSGi bundles and the P2 repository.

## Headless Server
The platform includes an embedded REST server (`EvolutionServer`) for remote control and headless orchestration.

### Start Command
To start the headless server:
```bash
./scripts/start-headless.sh [PORT]
```
- **Default Port:** 8080
- **Dependencies:** The script automatically resolves core EMF and OSGi dependencies from the Tycho cache.

### API Interaction
Once the server is running, you can interact with it via REST:

**Check Server Status:**
```bash
curl http://localhost:8080/server/status
```

**Create an Orchestration Task:**
```bash
curl -X POST http://localhost:8080/task -d '{"prompt": "mode: chat - Summarize the project structure", "projectRoot": "."}'
```

## Testing
Comprehensive tests for the headless API and mode routing are located in:
- `eu.kalafatic.evolution.controller.tests.EvolutionServerTest`
- `eu.kalafatic.evolution.controller.tests.ModeRouterTest`
