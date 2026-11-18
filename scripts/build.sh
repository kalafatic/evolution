#!/bin/bash
# Evolution Platform - Build Script

set -euo pipefail

SCRIPT_DIR="$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"
cd "${SCRIPT_DIR}/.."

JAVA_MAJOR="$(java -version 2>&1 | awk -F'"' '/version/ {print $2; exit}' | cut -d. -f1)"
if [[ "${JAVA_MAJOR}" != "21" && -d /usr/lib/jvm/java-21-openjdk-amd64 ]]; then
    export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
    export PATH="${JAVA_HOME}/bin:${PATH}"
    JAVA_MAJOR="$(java -version 2>&1 | awk -F'"' '/version/ {print $2; exit}' | cut -d. -f1)"
fi
if [[ "${JAVA_MAJOR}" != "21" ]]; then
    echo "ERROR: JDK 21 is required. Set JAVA_HOME to a JDK 21 installation." >&2
    exit 1
fi

echo "Starting Evolution Platform build..."
echo "JAVA_HOME=${JAVA_HOME:-<unset>}"
echo "Java: $(java -version 2>&1 | grep -m1 version)"

if [[ -x ./mvnw ]]; then
    MVN=./mvnw
else
    MVN=mvn
fi

"${MVN}" -B clean verify -DskipTests=true -Plinux

echo "Build successful! Products are under eu.kalafatic.evolution.repository/target/products."
