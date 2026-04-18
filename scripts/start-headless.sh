#!/bin/bash
# Evolution Platform - Headless Starter

PORT=${1:-8080}

# Build classpath from target jars and local libs
CP="eu.kalafatic.evolution.controller/target/eu.kalafatic.evolution.controller-2.6.1-SNAPSHOT.jar"
CP="$CP:eu.kalafatic.evolution.model/target/eu.kalafatic.evolution.model-2.6.1-SNAPSHOT.jar"
CP="$CP:eu.kalafatic.utils/target/eu.kalafatic.utils-2.6.1-SNAPSHOT.jar"

# Add bundled libs from controller
for jar in eu.kalafatic.evolution.controller/lib/*.jar; do
    CP="$CP:$jar"
done

# Resolve core OSGi/EMF dependencies from Tycho cache for standalone execution
TYCHO_CACHE="$HOME/.m2/repository/.cache/tycho"
CORE_REPO="$TYCHO_CACHE/https/ca.mirrors.cicku.me/eclipse/releases/2025-12/202512101000/plugins"

EMF_ECORE=$(find "$CORE_REPO" -name "org.eclipse.emf.ecore_*.jar" | head -n 1)
EMF_COMMON=$(find "$CORE_REPO" -name "org.eclipse.emf.common_*.jar" | head -n 1)
OSGI=$(find "$CORE_REPO" -name "org.eclipse.osgi_*.jar" | head -n 1)

CP="$CP:$EMF_ECORE:$EMF_COMMON:$OSGI"

echo "Starting Evolution Headless Server on port $PORT..."
java -cp "$CP" eu.kalafatic.evolution.controller.orchestration.EvolutionServer "$PORT"
