#!/bin/bash
# Evolution Platform - Build Script

set -e

echo "Starting Evolution Platform build..."

# Clean and verify using Tycho
mvn clean verify -DskipTests=true

echo "Build successful! Artifacts generated in target directories."
