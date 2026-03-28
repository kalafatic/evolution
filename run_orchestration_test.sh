#!/bin/bash

# Setup colors
GREEN='\033[0;32m'
NC='\033[0m'

echo -e "${GREEN}Preparing Evolution Orchestration Test...${NC}"

# 1. Install the model bundle to local Maven repo
echo "Installing model bundle..."
mvn -f eu.kalafatic.evolution.model/pom.xml install -Dtycho.mode=maven -DskipTests

# 2. Compile the controller bundle
echo "Compiling controller bundle..."
mvn -f eu.kalafatic.evolution.controller/pom.xml compile -Dtycho.mode=maven

# 3. Run the ExampleMain verification scenario
echo -e "${GREEN}Running Orchestration Scenario...${NC}"
mvn -f eu.kalafatic.evolution.controller/pom.xml exec:java \
    -Dexec.mainClass="eu.kalafatic.evolution.controller.orchestration.ExampleMain" \
    -Dtycho.mode=maven \
    -Dexec.cleanupDaemonThreads=false
